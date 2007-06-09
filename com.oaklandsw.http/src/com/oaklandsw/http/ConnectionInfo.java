/*
 * Copyright 2007 Oakland Software Incorporated. All rights Reserved.
 */
package com.oaklandsw.http;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;

import com.oaklandsw.util.LogUtils;
import com.oaklandsw.util.Util;

import edu.emory.mathcs.backport.java.util.concurrent.ArrayBlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.BlockingQueue;

class ConnectionInfo
{
    private static final Log        _log       = LogUtils.makeLogger();

    static final int                START_SIZE = 10;

    // The concatenation of host/port and if a specific
    // proxy host/port was specified, that is added as well.
    // This is used to tell where the connection is going.
    // If the proxy was not specified on a per-connection basis,
    // it is not included here; the proxy incarnation mechanism
    // is used to deal with that.
    String                          _connectionKey;

    // The number that is incremented whenever the global
    // proxy state changes (ie, all connections are reset)
    // This is used to make sure that older connections opened
    // before the reset are closed.
    int                             _proxyIncarnation;

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

    // The available connections that are not assigned. We don't keep
    // specific track of connections that are assigned.
    protected BlockingQueue         _availableConnections;

    // For diagnositic purposes
    protected List                  _assignedConnections;

    protected HttpConnectionManager _connManager;

    ConnectionInfo(HttpConnectionManager connManager,
            String connectionKey,
            int proxyIncarnation)
    {
        _connectionKey = connectionKey;
        _proxyIncarnation = proxyIncarnation;
        _availableConnections = new ArrayBlockingQueue(START_SIZE);
        _assignedConnections = new ArrayList();
        _connManager = connManager;
    }

    void addNewConnection(HttpConnection conn)
    {
        _assignedConnections.add(conn);
        conn._connectionInfo = this;
        conn._connManager = _connManager;
        _totalEverCreated++;
    }

    // Returns a connection to the connection pool
    // Assumed to be synchronized on the HttpConnectionManager
    void connectionAvail(HttpConnection conn)
    {
        _assignedConnections.remove(conn);

        // Don't allow a closed connection to be returned
        if (!conn.isOpen())
            Util.impossible("Cannot return a closed connection:" + conn);

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

        if (getActiveConnectionCount() < 0)
        {
            Util.impossible("totalActiveConnections underflow\n" + dump(0));
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
    }

    // Remove a connection that is currently assigned
    // Assumed to be synchronized on the HttpConnectionManager
    void removeAssignedConnection(HttpConnection conn)
    {
        _assignedConnections.remove(conn);
    }

    // Returns a connection that can be used for this URL connection,
    // delinks the returned connection
    // Assumed to be synchronized on the HttpConnectionManager
    HttpConnection getMatchingConnection(HttpURLConnection urlCon)
    {
        // Only pay attention to this option if there is a limit on the max
        // number of connections, for obvious reasons
        if ((urlCon._pipeliningOptions & HttpURLConnection.PIPE_MAX_CONNECTIONS) != 0
            && _connManager._maxConns >= 0)
        {
            if (getActiveConnectionCount() < _connManager._maxConns)
            {
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
                    conn = (HttpConnection)_availableConnections.poll();
                    break assign;
                }
                if (_log.isDebugEnabled())
                    _log.debug("getMatching - found closed connection");
            }

            // Slow path, check them all (we already checked the first)
            Iterator it = _availableConnections.iterator();
            while (it.hasNext())
            {
                conn = (HttpConnection)it.next();

                // This is possible because a connection can close during
                // pipelined reading while it is still in the pool.
                if (!conn.isOpen())
                {
                    if (_log.isDebugEnabled())
                    {
                        _log.debug("getMatching - removing closed "
                            + "connection from available pool:"
                            + conn);
                    }
                    // FIXME - does not use the shutdown return to possible
                    // shutdown the connection timeout thread
                    _connManager.checkConnCount(this, null);
                    it.remove();
                    continue;
                }

                if (checkMatch(conn, urlCon))
                {
                    // Remove because it's assigned
                    it.remove();
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

    // See if this connection has a matching credential if required or
    // other criteria
    private boolean checkMatch(HttpConnection conn, HttpURLConnection urlCon)
    {
        int matched = 0;

        check:
        {
            int pipelinedCount = conn.getPipelinedUrlConCount();

            if (urlCon.isPipelining())
            {
                if (urlCon._pipeliningMaxDepth > 0
                    && pipelinedCount > urlCon._pipeliningMaxDepth)
                {
                    matched = -10;
                    break check;
                }

                if (_observedMaxUrlCons != 0
                    && (urlCon._pipeliningOptions & HttpURLConnection.PIPE_USE_OBSERVED_CONN_LIMIT) != 0
                    && conn._totalReqUrlConCount > _observedMaxUrlCons)
                {
                    matched = -11;
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

        if (_log.isDebugEnabled())
        {
            _log.debug("checking for match: result: "
                + matched
                + " conn: "
                + conn
                + " url: "
                + urlCon);
            if (false)
            {
                System.out.println(" url: "
                    + urlCon.isPipelining()
                    + " con: "
                    + conn.getPipelinedUrlConCount());
            }
        }

        return matched == MATCHED;
    }

    // Assumed to be synchronized on the HttpConnectionManager
    int getActiveConnectionCount()
    {
        return _assignedConnections.size() + _availableConnections.size();
    }

    // Assumed to be synchronized on the HttpConnectionManager
    void closeAllConnections()
    {
        _log.debug("closeAllConnections");

        HttpConnection conn;

        while (true)
        {
            conn = (HttpConnection)_availableConnections.poll();
            if (conn == null)
                break;
            conn.close();
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
            sb.append("    " + conn.toString());
            sb.append("\n");
            sb.append(conn.dump(6));
        }
        return sb.toString();
    }

}
