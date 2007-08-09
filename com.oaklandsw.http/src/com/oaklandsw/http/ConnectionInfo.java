/*
 * Copyright 2007 Oakland Software Incorporated. All rights Reserved.
 */
package com.oaklandsw.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.oaklandsw.util.LogUtils;
import com.oaklandsw.util.Util;

import edu.emory.mathcs.backport.java.util.concurrent.ArrayBlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.BlockingQueue;

class ConnectionInfo
{
    private static final Log        _log       = LogUtils.makeLogger();

    private static final Log        _connLog   = LogFactory
                                                       .getLog(HttpConnection.CONN_LOG);

    static final int                START_SIZE = 10;

    // The concatenation of host/port and if a specific
    // proxy host/port was specified, that is added as well.
    // This is used to tell where the connection is going.
    // If the proxy was not specified on a per-connection basis,
    // it is not included here; the proxy incarnation mechanism
    // is used to deal with that.
    String                          _connectionKey;

    // The number of urlcons allowed on this connection before the
    // server closes it. This starts off being zero (unlimited), then
    // if a connection is closed by the server (using a Connection: close header
    // the number of urlcons sent at that time will be recorded here.
    // This is used to help with pipelining to make sure it
    // does not put more than that many urlcons on a connection.
    int                             _observedMaxUrlCons;

    // The total number of connections that have been created,
    // this includes any that are presently assigned, and any
    // that are in the pool
    // int _totalActiveConnections;

    // The total number of connections ever created.
    int                             _totalEverCreated;

    // The available connections that are not assigned.
    protected BlockingQueue         _availableConnections;

    // The currently assigned connections
    protected List                  _assignedConnections;

    // Used to keep track of the time this is no longer used,
    // we discard these only after some interval. We want to keep
    // them in the short term so we don't lose information
    // if we reconnect to the same host/port
    long                            _usedTime;

    protected GlobalState           _globalState;
    protected HttpConnectionManager _connManager;

    ConnectionInfo(HttpConnectionManager connManager,
            String connectionKey)
    {
        _connectionKey = connectionKey;
        _availableConnections = new ArrayBlockingQueue(START_SIZE);
        _assignedConnections = new ArrayList();
        _connManager = connManager;
        _globalState = _connManager._globalState;
    }

    void addNewConnection(HttpConnection conn)
    {
        _assignedConnections.add(conn);
        _usedTime = System.currentTimeMillis();
        conn._connectionInfo = this;
        conn._connManager = _connManager;
        _totalEverCreated++;
    }

    // Returns a connection to the connection pool
    // Assumed to be synchronized on the HttpConnectionManager
    void connectionAvail(HttpConnection conn)
    {
        _assignedConnections.remove(conn);
        if (_availableConnections.remainingCapacity() == 0)
        {
            _log.debug("Expanding _availableConnections");
            BlockingQueue q = new ArrayBlockingQueue(_availableConnections
                    .size() * 2);
            q.addAll(_availableConnections);
            _availableConnections = q;
        }

        if (!_availableConnections.offer(conn))
            Util.impossible("BlockingQueue overflow: " + _availableConnections);
        bugCheckCounts();
    }

    private void bugCheckCounts()
    {
        if (_availableConnections.size() < 0 || _assignedConnections.size() < 0)
        {
            Util.impossible("connection count underflow\n" + dump(0));
        }
    }

    boolean isConnectionAvail(HttpConnection conn)
    {
        return _availableConnections.contains(conn);
    }

    // Remove a connection that is in the available pool
    // Assumed to be synchronized on the HttpConnectionManager
    void removeAvailConnection(HttpConnection conn)
    {
        _availableConnections.remove(conn);
        bugCheckCounts();
    }

    // Remove a connection that is currently assigned
    // Assumed to be synchronized on the HttpConnectionManager
    void removeAssignedConnection(HttpConnection conn)
    {
        _assignedConnections.remove(conn);
        bugCheckCounts();
    }

    // Returns a connection that can be used for this URL connection,
    // delinks the returned connection
    // Assumed to be synchronized on the HttpConnectionManager
    HttpConnection getMatchingConnection(HttpURLConnection urlCon)
        throws InterruptedException
    {
        // Only pay attention to this option if there is a limit on the max
        // number of connections, for obvious reasons
        if ((urlCon._pipeliningOptions & HttpURLConnection.PIPE_MAX_CONNECTIONS) != 0
            && _globalState._maxConns >= 0)
        {
            if (getActiveConnectionCount() < _globalState._maxConns)
            {
                if (_log.isDebugEnabled())
                {
                    _log.debug("getMatch - force new connection - limit: "
                        + _globalState._maxConns);
                }

                // Force it to create a connection
                return null;
            }
            // Use the next available connection, since the available
            // connections are a queue this should work in round robin fashion
        }

        // Fast path
        HttpConnection conn = (HttpConnection)_availableConnections.peek();
        if (conn == null)
            return null;

        assign:
        {
            if (checkMatch(conn, urlCon))
            {
                if (conn.isOpen())
                {
                    // Dequeues the conn from available connections
                    conn = (HttpConnection)_availableConnections.poll();
                    if (_availableConnections.size() < 0)
                        Util.impossible("avail conn underflow: " + dump(0));
                    break assign;
                }
                if (_connLog.isDebugEnabled())
                    _connLog.debug("getMatching - found closed connection");
            }

            // Slow path, check them all
            Iterator it = _availableConnections.iterator();
            while (it.hasNext())
            {
                conn = (HttpConnection)it.next();

                boolean matched = checkMatch(conn, urlCon);

                // This is possible because a connection can close during
                // pipelined reading while it is still in the pool.
                if (!conn.isOpen())
                {
                    if (_connLog.isDebugEnabled())
                    {
                        _connLog.debug("getMatching - removing closed "
                            + "connection from available pool:"
                            + conn);
                    }

                    it.remove();
                    if (_availableConnections.size() < 0)
                        Util.impossible("avail conn underflow: " + dump(0));
                    continue;
                }

                if (matched)
                {
                    // Remove because it's assigned
                    it.remove();
                    if (_availableConnections.size() < 0)
                        Util.impossible("avail conn underflow: " + dump(0));
                    break assign;
                }
            }

            // No matching connection found
            conn = null;

        } // assign:

        if (conn != null)
            _assignedConnections.add(conn);

        return conn;
    }

    static final int MATCHED = 1;

    private int checkCredential(HttpConnection conn,
                                HttpURLConnection urlCon,
                                UserCredential cred,
                                int proxyOrNormal)
    {
        if (conn._credential[proxyOrNormal] != null)
        {
            if (cred == null)
                return -3;
            if (!cred.getKey().equals(conn._credential[proxyOrNormal].getKey()))
                return -4;
        }
        return MATCHED;
    }

    // Returns the limit of the number of urlCons we will put
    // on this connection
    int getPipelineConnectionLimit(HttpURLConnection urlCon)
    {
        int limit;

        // Limit is either observed or specified by the user
        if (urlCon._connectionRequestLimit == 0)
        {
            limit = _observedMaxUrlCons;
        }
        else if (_observedMaxUrlCons == 0)
        {
            limit = urlCon._connectionRequestLimit;
        }
        else
        {
            limit = Math.min(urlCon._connectionRequestLimit,
                             _observedMaxUrlCons);
        }
        return limit;
    }

    // Called only for a pipelined connection to determine if
    // it needs to flush for reasons related to to the total connection limit
    // or the pipeline depth. This is called after the data has been
    // written to the connection.
    boolean needsFlush(HttpConnection conn, HttpURLConnection urlCon)
    {
        // User requested depth
        if (urlCon._pipeliningMaxDepth > 0
            && conn.getPipelinedUrlConCount() == urlCon._pipeliningMaxDepth)
        {
            return true;
        }

        if (getPipelineConnectionLimit(urlCon) == conn._totalReqUrlConCount)
            return true;
        return false;
    }

    // See if this connection has a matching credential if required or
    // other criteria
    private boolean checkMatch(HttpConnection conn, HttpURLConnection urlCon)
        throws InterruptedException
    {
        int matched = 0;

        check:
        {
            int limit;
            if (urlCon.isPipelining())
            {
                limit = getPipelineConnectionLimit(urlCon);
            }
            else
            {
                // Not pipelining, it's only specified by the user
                limit = urlCon._connectionRequestLimit;
            }

            // Don't allow on connections over the limit
            // Need the check here because the connection may be open after
            // all of the requests are sent and before all of the replies
            // are received in the pipeline case
            if (limit != 0 && conn._totalReqUrlConCount >= limit)
            {
                if (conn._totalReqUrlConCount == conn._totalResUrlConCount)
                {
                    // If the connection has no outstanding activity, close it
                    // and show it as a match, the caller will finish disposing
                    // if it and look for another connection
                    conn.close();
                    matched = 2;
                }
                else
                {
                    // The connection can't be used but has outstanding
                    // activity, just skip it
                    matched = -12;
                }
                break check;
            }

            int pipelinedCount = conn.getPipelinedUrlConCount();

            if (urlCon.isPipelining())
            {
                // User requested depth
                if (urlCon._pipeliningMaxDepth > 0
                    && pipelinedCount >= urlCon._pipeliningMaxDepth)
                {
                    matched = -10;
                    break check;
                }
            }
            else
            {
                // A connection with pipeline I/O outstanding can only be used
                // by a urlCon requesting pipelining.
                if (pipelinedCount > 0)
                {
                    matched = -1;
                    break check;
                }
            }

            if (conn._credential[HttpURLConnection.AUTH_NORMAL] == null
                && conn._credential[HttpURLConnection.AUTH_PROXY] == null)
            {
                matched = MATCHED;
                break check;
            }

            if (!UserCredential
                    .useConnectionAuthentication(conn._authProtocol[HttpURLConnection.AUTH_NORMAL])
                && !UserCredential
                        .useConnectionAuthentication(conn._authProtocol[HttpURLConnection.AUTH_PROXY]))
            {
                matched = MATCHED;
                break check;
            }

            HttpUserAgent ua = urlCon.getUserAgent();
            if (ua == null)
            {
                matched = -2;
                break check;
            }

            UserCredential cred;
            if (conn._credential[HttpURLConnection.AUTH_NORMAL] != null)
            {
                cred = (UserCredential)ua
                        .getCredential(null,
                                       urlCon.getUrlString(),
                                       conn._authProtocol[HttpURLConnection.AUTH_NORMAL]);

                matched = checkCredential(conn,
                                          urlCon,
                                          cred,
                                          HttpURLConnection.AUTH_NORMAL);
                if (matched < 0)
                    break check;
            }

            if (conn._credential[HttpURLConnection.AUTH_PROXY] != null)
            {
                cred = (UserCredential)ua
                        .getProxyCredential(null,
                                            urlCon.getUrlString(),
                                            conn._authProtocol[HttpURLConnection.AUTH_PROXY]);
                matched = checkCredential(conn,
                                          urlCon,
                                          cred,
                                          HttpURLConnection.AUTH_PROXY);
                if (matched < 0)
                    break check;
            }
            matched = MATCHED;
        }

        if (_log.isDebugEnabled() || _connLog.isDebugEnabled())
        {
            String str = "checking for match: result: "
                + matched
                + " conn: "
                + conn
                + " url: "
                + urlCon;

            _log.debug(str);

            if (_connLog.isDebugEnabled() && matched < MATCHED)
            {
                _connLog.debug("match failed: "
                    + str
                    + " pipelining: "
                    + urlCon.isPipelining()
                    + " conn: \n"
                    + conn.dump(0));
            }
        }

        return matched >= MATCHED;
    }

    // Assumed to be synchronized on the HttpConnectionManager
    int getActiveConnectionCount()
    {
        return _assignedConnections.size() + _availableConnections.size();
    }

    void flushAllAvailConnections() throws IOException
    {
        Iterator it = _availableConnections.iterator();
        while (it.hasNext())
        {
            HttpConnection conn = (HttpConnection)it.next();
            conn.unconditionalFlush();
        }
    }

    // Assumed to be synchronized on the HttpConnectionManager
    void closeAllConnections(boolean immediate) throws InterruptedException
    {
        _connLog.debug("closeAllConnections - immediate: " + immediate);

        HttpConnection conn;

        Iterator it = _availableConnections.iterator();
        while (it.hasNext())
        {
            conn = (HttpConnection)it.next();
            conn.close(immediate);
            it.remove();
        }

        _connLog.debug("closeAllConnections - finished avail conns");

        // Kill everything right now
        if (immediate)
        {
            it = _assignedConnections.iterator();
            while (it.hasNext())
            {
                conn = (HttpConnection)it.next();
                conn.close(immediate);
                it.remove();
            }
            _connLog.debug("closeAllConnections - finished assigned conns");
        }
    }

    public String dump(int indent)
    {
        StringBuffer sb = new StringBuffer();

        sb.append("Connection: "
            + _connectionKey
            + "\n  Total connections created: "
            + _totalEverCreated
            + "\n  Avail connections:         "
            + _availableConnections.size()
            + "\n  Assigned connections:      "
            + _assignedConnections.size()
            + "\n  Observed max urlcons before close: "
            + _observedMaxUrlCons);
        sb.append("\n");

        sb.append("Assigned connections: \n");
        Iterator it = _assignedConnections.iterator();
        while (it.hasNext())
        {
            HttpConnection conn = (HttpConnection)it.next();
            sb.append("    " + conn.toString());
            sb.append("\n");
            sb.append(conn.dump(6));
        }

        sb.append("Available connections: \n");
        it = _availableConnections.iterator();
        while (it.hasNext())
        {
            HttpConnection conn = (HttpConnection)it.next();
            if (conn == null)
            {
                sb.append("Error - null conn");
                continue;
            }

            sb.append("    " + conn.toString());
            sb.append("\n");
            sb.append(conn.dump(6));
        }
        return sb.toString();
    }

}
