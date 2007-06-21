//
// Copyright 2002-2007, oakland software, all rights reserved.
//
// May not be used or redistributed without specific written
// permission from oakland software.
//
package com.oaklandsw.http;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.regexp.RE;
import org.apache.regexp.RESyntaxException;

import com.oaklandsw.util.LogUtils;
import com.oaklandsw.util.StringUtils;
import com.oaklandsw.util.URIUtil;
import com.oaklandsw.util.Util;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;

/**
 * Creates and manages a pool of HttpConnections.
 * <p>
 * Used only to get an HttpConnection to allow repeated use of a specific
 * connection for a HttpURLConnection.
 */
public class HttpConnectionManager
{
    private static final Log      _log                      = LogUtils
                                                                    .makeLogger();

    // From RFC 2616 section 8.1.4
    public static int             DEFAULT_MAX_CONNECTIONS   = 2;

    // See the comment about _urlConReleased in HttpURLConnection
    protected static final int    NOT_RELEASED_TIMEOUT      = 10000;

    // K(connection key) V(ConnectionInfo)
    private Map                   _hostMap                  = new HashMap();

    // K(current Thread) V(List of urlcons)
    private Map                   _threadUrlConMap          = new HashMap();

    int                           _maxConns                 = DEFAULT_MAX_CONNECTIONS;

    private HttpConnectionTimeout _timeout;

    // K(HttpConnection) v(a Thread)
    protected Map                 _threadConnectionMap;

    // K(current Thread) V(AtomicInteger) - count of urlcons to read
    Map                           _threadUrlConCountMap;

    // Used to keep track of the global proxy state at the time
    // this connection was created.
    private int                   _globalProxyIncarnation;

    private String                _proxyHost;
    private int                   _proxyPort                = -1;
    private String                _proxyUser;
    private String                _proxyPassword;

    private String                _nonProxyHostsString;

    private ArrayList             _nonProxyHosts;

    // The maximum number of pipelined urlcons for a connection
    int                           _pipelineMaxDepth;

    static final int              PCONN_MAX_CONNECTIONS     = 0;
    static final int              PCONN_FIXED_NUMBER        = 1;
    static final int              PCONN_MAX_PER_CALLBACK    = 2;

    int                           _pipelineConnectionUseType;

    // If the connection use type is FIXED_NUMBER
    int                           _pipelineFixedLimit;

    // Used to synchronize the pipeline related methods, as distinct
    // from the connection management methods which are synchronized
    // with "this"
    protected String              _pipelineLock;

    // Statistics

    // V(AtomicInteger) - by number of tries when it succeeds
    ArrayList                     _retryCounts;
    ArrayList                     _pipelineReadRetryCounts;

    static final int              COUNT_ATTEMPTED           = 0;
    static final int              COUNT_SUCCESS             = 1;
    static final int              COUNT_FAIL_IO             = 2;
    static final int              COUNT_FAIL_MAX_RETRY      = 3;
    static final int              COUNT_PIPELINE_DEPTH_HIGH = 4;
    static final int              COUNT_LAST                = COUNT_PIPELINE_DEPTH_HIGH;

    int[]                         _requestCounts;
    String[]                      _requestCountNames        = new String[] { //
                                                            "Attempted:                      " //
        , "Success:                        " //
        , "Failed IOException:             " //
        , "Failed Max Retries:             " //
        , "Pipeline max depth (all conns): " //
                                                            };

    // Just a dummy class to enqueue to get a connection thread to close
    static class CloseMarker
    {
    }

    // Used to tell a connection thread to close
    static CloseMarker _closeMarker = new CloseMarker();

    public HttpConnectionManager()
    {
        _timeout = new HttpConnectionTimeout(this);
        _threadConnectionMap = new HashMap();
        _threadUrlConCountMap = new HashMap();
        _pipelineLock = new String();
        resetStatistics();
    }

    public void setProxyHost(String proxyHost)
    {
        _proxyHost = proxyHost;
        // Get rid of all current connections as they are not
        // going to the right place.
        resetConnectionPool();
    }

    public String getProxyHost()
    {
        return _proxyHost;
    }

    public String getProxyPassword()
    {
        return _proxyPassword;
    }

    public void setProxyPassword(String proxyPassword)
    {
        _proxyPassword = proxyPassword;
    }

    public String getProxyUser()
    {
        return _proxyUser;
    }

    public void setProxyUser(String proxyUser)
    {
        _proxyUser = proxyUser;
    }

    /**
     * Set the proxy host to use for all connections.
     * 
     * @param proxyHost -
     *            the proxy host name
     */
    void setNonProxyHosts(String hosts)
    {
        synchronized (this)
        {
            // Get rid of all current connections as they are not
            // going to the right place.
            resetConnectionPool();

            _nonProxyHostsString = hosts;
            if (_nonProxyHostsString == null)
            {
                _nonProxyHosts = null;
                return;
            }

            _nonProxyHosts = new ArrayList();
            StringTokenizer stringtokenizer = new StringTokenizer(_nonProxyHostsString,
                                                                  "|",
                                                                  false);
            while (stringtokenizer.hasMoreTokens())
            {
                String host = stringtokenizer.nextToken().toLowerCase().trim();

                // The syntax for a non-proxy host only allows a "*" as
                // wildcard,
                // so we need to fix it up to be a correct RE.
                host = StringUtils.replace(host, ".", "\\.");
                host = StringUtils.replace(host, "*", ".*");

                RE re;
                try
                {
                    re = new RE(host);
                }
                catch (RESyntaxException rex)
                {
                    throw new RuntimeException("Invalid syntax for nonProxyHosts: '"
                        + hosts
                        + "' on host '"
                        + host
                        + "': "
                        + rex.getMessage());
                }

                re.setMatchFlags(RE.MATCH_CASEINDEPENDENT);
                if (_log.isDebugEnabled())
                    _log.debug("Non proxy host: " + host);
                _nonProxyHosts.add(re);
            }
        }
    }

    String getNonProxyHosts()
    {
        synchronized (this)
        {
            return _nonProxyHostsString;
        }
    }

    public void setProxyPort(int proxyPort)
    {
        // Get rid of all current connections as they are not
        // going to the right place.
        resetConnectionPool();
        _proxyPort = proxyPort;
    }

    public int getProxyPort()
    {
        return _proxyPort;
    }

    public void setMaxConnectionsPerHost(int maxConnections)
    {
        _maxConns = maxConnections;
    }

    public int getMaxConnectionsPerHost()
    {
        return _maxConns;
    }

    // Executes the processPipelinedRead on the specified urlCon, creating
    // the thread if necessary, also releases the connection from the urlCon
    void enqueueWorkForConnection(HttpURLConnection urlCon)
        throws InterruptedException
    {
        HttpConnection conn;
        HttpConnectionThread t;

        conn = urlCon._connection;

        if (_log.isDebugEnabled())
        {
            _log.debug("enqueueWork: "
                + "conn: "
                + conn
                + " enqueued: "
                + urlCon);
        }

        // There is actually a small window here were the capacity could go
        // to zero after this message is written and before the put, but
        // this is only tracing, so it's ok - that's why we say (probably)
        if (conn._queue.remainingCapacity() == 0)
        {
            _log.warn("enqueueWork: (probably) blocking "
                + "for queue capacity on conn: "
                + conn
                + " queue size: "
                + conn._queue.size());
        }

        conn._queue.put(urlCon);

        // Make sure there is a thread to service this. Note the locking
        // in the thread locks the connmgr (this) lock when dequeuing
        // and terminating it prevents the thread from terminating
        // if there is anything on the queue. This means we don't need
        // to lock when we write something to the queue. This is
        // necessary because writing to the queue may block for lack
        // of space (given the implementation of an ArrayBlockingQueue).

        synchronized (this)
        {
            t = (HttpConnectionThread)_threadConnectionMap.get(conn);
            if (t == null)
            {
                t = new HttpConnectionThread(conn, this);
                _threadConnectionMap.put(conn, t);

                t.start();
                if (_log.isDebugEnabled())
                {
                    _log.debug("enqueueWork: "
                        + "conn: "
                        + conn
                        + " created thread");
                }
            }

            // Also releases the connection; we don't use the higher
            // level HttpURLConnection.releaseConnection() because
            // we need to do the enqueue and release atomically
            // to preserve the order of the reads on the connection
            releaseConnection(conn);
        }
    }

    void enqueueCloseForConnection(HttpConnection conn)
        throws InterruptedException
    {
        conn._queue.put(_closeMarker);
    }

    // Called when the connection thread is going to terminate
    // because of lack of work
    void connectionThreadTerminated(HttpConnection conn)
    {
        synchronized (this)
        {
            if (_log.isDebugEnabled())
            {
                _log.debug("connThreadTerminated: " + "conn: " + conn);
            }
            _threadConnectionMap.remove(conn);
        }
    }

    //
    // Methods below are synchronized with the _pipelineLock
    // 

    void addUrlConToExecute(HttpURLConnection urlCon)
    {
        Thread thread = Thread.currentThread();

        synchronized (_pipelineLock)
        {
            List conList = (List)_threadUrlConMap.get(thread);
            if (conList == null)
            {
                conList = new ArrayList();
                _threadUrlConMap.put(thread, conList);
            }
            conList.add(urlCon);
            if (_log.isDebugEnabled())
            {
                _log
                        .debug("adding urlcon: "
                            + urlCon
                            + " to Thread: "
                            + thread);
            }
        }
    }

    void removeUrlConToExecute(HttpURLConnection urlCon)
    {
        synchronized (_pipelineLock)
        {
            List conList = (List)_threadUrlConMap.get(Thread.currentThread());
            if (conList == null)
                return;
            conList.remove(urlCon);
            if (conList.isEmpty())
                _threadUrlConMap.remove(Thread.currentThread());
        }
    }

    void addUrlConToRead(Thread thread)
    {
        synchronized (_pipelineLock)
        {
            AtomicInteger ai = (AtomicInteger)_threadUrlConCountMap.get(thread);
            if (ai == null)
            {
                ai = new AtomicInteger();
                _threadUrlConCountMap.put(thread, ai);
            }
            int count = ai.incrementAndGet();
            if (_log.isDebugEnabled())
                _log.debug("addUrlConToRead - count (after) " + count);
        }
    }

    void urlConWasRead(Thread thread)
    {
        synchronized (_pipelineLock)
        {
            AtomicInteger ai = (AtomicInteger)_threadUrlConCountMap.get(thread);
            if (ai == null)
                Util.impossible("No counter for callback: " + thread);
            int count = ai.decrementAndGet();
            if (_log.isDebugEnabled())
                _log.debug("urlConWasRead - count (after) " + count);
            if (count == 0)
            {
                _pipelineLock.notifyAll();
                _threadUrlConCountMap.remove(thread);
            }
        }
    }

    void blockForUrlCons() throws InterruptedException
    {
        synchronized (_pipelineLock)
        {
            Thread thread = Thread.currentThread();
            AtomicInteger ai = (AtomicInteger)_threadUrlConCountMap.get(thread);
            if (ai == null)
            {
                _log.debug("no outstanding connections "
                    + "(they probably all finished - continuing");
                return;
            }
            int count;
            while ((count = ai.get()) != 0)
            {
                if (_log.isDebugEnabled())
                {
                    _log.debug("waiting for "
                        + count
                        + " pipelined urlcons to complete");
                }
                try
                {
                    _pipelineLock.wait();
                }
                catch (InterruptedException ex)
                {
                    if (_log.isDebugEnabled())
                    {
                        _log.debug("interrupted while waiting for "
                            + count
                            + " pipelined urlcons to complete");
                    }
                    throw ex;
                }
            }
            _log.debug("pipelined urlcons are complete");
        }
    }

    // Returns the list of urlCons associated with the current thread
    List getUrlConsToExecute()
    {
        synchronized (_pipelineLock)
        {
            Thread thread = Thread.currentThread();
            List list = (List)_threadUrlConMap.get(thread);
            _threadUrlConMap.remove(thread);
            return list;
        }
    }

    //
    // End of methods synchronized with the _pipelineLocka
    //

    private int getPort(String protocol, int port)
    {
        // default to provided port
        int portForProtocol = port;
        if (portForProtocol <= 0)
        {
            if (protocol.equalsIgnoreCase("https"))
            {
                portForProtocol = 443;
            }
            else
            {
                portForProtocol = 80;
            }
        }
        return portForProtocol;
    }

    /**
     * Returns the proxy host for the specified host.
     */
    public String getProxyHost(String host)
    {
        // This should be OK, as we never lock the connection
        // info while we have the lock on this.
        synchronized (this)
        {
            // Look for hosts to not proxy for
            if (_nonProxyHosts != null)
            {
                int len = _nonProxyHosts.size();
                for (int i = 0; i < len; i++)
                {
                    RE re = (RE)_nonProxyHosts.get(i);
                    if (re.match(host))
                    {
                        if (_log.isDebugEnabled())
                        {
                            _log.debug("Not proxying host: "
                                + host
                                + " because of host rule: "
                                + re.toString());
                        }
                        return null;
                    }
                }
            }
        }
        return _proxyHost;
    }

    /**
     * Get an HttpConnection for a given URL. The URL must be fully specified
     * (i.e. contain a protocol and a host (and optional port number). If the
     * maximum number of connections for the host has been reached, this method
     * will block for <code>connectionTimeout</code> milliseconds or until a
     * connection becomes available. If no connection becomes available before
     * the timeout expires an HttpException will be thrown.
     * 
     * @param urlCon -
     *            an HttpURLConnection
     * @return an HttpConnection for the given host:port
     * @exception java.net.MalformedURLException
     * @exception com.oaklandsw.http.HttpException -
     *                If no connection becomes available before the timeout
     *                expires
     */
    public HttpConnection getConnection(HttpURLConnection urlCon)
        throws HttpException,
            MalformedURLException
    {
        long connectionTimeout = urlCon.getConnectionTimeout();
        String url = urlCon.getUrlString();
        long idleTimeout = urlCon.getIdleConnectionTimeout();
        long idlePing = urlCon.getIdleConnectionPing();
        String proxyHost = urlCon.getConnectionProxyHost();
        int proxyPort = urlCon.getConnectionProxyPort();

        if (!HttpURLConnection._urlConReleased)
            connectionTimeout = NOT_RELEASED_TIMEOUT;

        // Get the protocol and port (use default port if not specified)
        final String protocol = URIUtil.getProtocol(url);
        if (!protocol.toLowerCase().startsWith("http"))
        {
            throw new IllegalArgumentException("The protocol must be http or https; found: "
                + protocol);
        }

        String hostAndPort = URIUtil.getProtocolHostPort(url);
        String connectionKey = getConnectionKey(hostAndPort,
                                                proxyHost,
                                                proxyPort);
        if (_log.isDebugEnabled())
        {
            _log.debug("Requested connection: " + connectionKey);
        }

        HttpConnection conn = null;

        synchronized (this)
        {
            // Look for a list of connections for the given host:port
            ConnectionInfo ci = getConnectionInfo(connectionKey);

            // Don't have any available connections, wait for one
            // -1 in _maxConns is unlimited
            while (ci.getActiveConnectionCount() >= _maxConns
                && _maxConns >= 0
                && (conn = ci.getMatchingConnection(urlCon)) == null)
            {
                try
                {
                    if (_log.isDebugEnabled())
                    {
                        _log.debug("Waiting for: "
                            + connectionKey
                            + " waiting for: "
                            + connectionTimeout);
                    }

                    long startTime = 0;
                    if (connectionTimeout > 0)
                        startTime = System.currentTimeMillis();

                    this.wait(connectionTimeout);

                    // Need to refetch this as it might have changed since
                    // the wait unlocked
                    ci = getConnectionInfo(connectionKey);

                    // Add the 32ms because wait seems to wake a little early sometimes
                    long waitTime = System.currentTimeMillis() - startTime + 32;

                    if (connectionTimeout > 0
                        && (waitTime >= connectionTimeout))
                    {
                        if (!HttpURLConnection._urlConReleased
                            && connectionTimeout == NOT_RELEASED_TIMEOUT)
                        {
                            throw new IllegalStateException("Possible programming error: "
                                + "You have timed out waiting for a "
                                + "connection and our records indicate you have not "
                                + "done a getInputStream() and read the results yet.  If"
                                + "the reponse code is successful (20x), "
                                + "and there is data returned, you must go a "
                                + "getInputStream() and read and close the stream. ");
                        }

                        _log.info("Timed out waiting for connection");
                        throw new HttpTimeoutException();
                    }

                    if (_log.isDebugEnabled())
                    {
                        _log.debug("Waiting END: "
                            + connectionKey
                            + " waited: "
                            + waitTime);
                    }
                }
                catch (InterruptedException ex)
                {
                    throw new HttpException("Thread killed while waiting");
                }
            }

            // We have the capacity to get a connection, and we did not get
            // one in the above loop, see if there is one available
            if (conn == null)
                conn = ci.getMatchingConnection(urlCon);

            if (conn != null)
            {
                _log.debug("Using existing connection");
                // The connection has to be open because the
                // ConnectionInfo.checkMatching removes any closed connections
                try
                {
                    conn.assertOpen();
                }
                catch (Exception ex)
                {
                    Util.impossible("Returned connection "
                        + "not open: "
                        + conn, ex);
                }
            }
            else
            {
                _log.debug("Creating new connection");

                // Create a new connection
                boolean isSecure = protocol.equalsIgnoreCase("HTTPS");

                String host;
                int port;
                // Start after the protocol
                int start = hostAndPort.indexOf(':') + 3;
                int ind = hostAndPort.indexOf(":", start);
                if (ind > 0)
                {
                    host = hostAndPort.substring(start, ind);
                    try
                    {
                        port = Integer.parseInt(hostAndPort.substring(ind + 1));
                    }
                    catch (Exception ex)
                    {
                        throw new HttpException("Invalid port: " + hostAndPort);
                    }
                }
                else
                {
                    host = hostAndPort.substring(start);
                    port = getPort(protocol, -1);
                }

                // If the proxy info was not explicitly specified
                if (proxyHost == null)
                    proxyHost = getProxyHost(host);
                if (proxyPort <= 0)
                    proxyPort = _proxyPort;

                // Make a new connection, note that this does not open
                // the connection, it only creates it, so this takes
                // little time
                conn = new HttpConnection(proxyHost,
                                          proxyPort,
                                          host,
                                          port,
                                          isSecure,
                                          ci._proxyIncarnation,
                                          connectionKey);
                ci.addNewConnection(conn);
            }

            conn._idleTimeout = idleTimeout;
            conn._idlePing = idlePing;
            conn._released = false;

            if (_log.isDebugEnabled())
                _log.debug("Obtained connection: " + conn);
        }

        return conn;
    }

    private String getConnectionKey(String hostAndPort,
                                    String proxyHost,
                                    int proxyPort)
    {
        // No proxy information
        if (proxyHost == null && proxyPort > 0)
            return hostAndPort;

        // Have to build the key
        StringBuffer buf = new StringBuffer(200);
        buf.append(hostAndPort);

        // If the proxy is per-connection, add that to the connection
        // key so we get the connection going to the right place
        if (proxyHost != null)
            buf.append(proxyHost);
        if (proxyPort > 0)
            buf.append(Integer.toString(proxyPort));
        return buf.toString();
    }

    /**
     * Get the pool (list) of connections available for the given host and port
     * and proxy host/port
     * 
     * @param connectionKey
     *            the key for the connection pool
     * @return the information for the connections of this host and port.
     */
    private ConnectionInfo getConnectionInfo(String connectionKey)
    {
        ConnectionInfo ci;

        synchronized (this)
        {
            // Look for a list of connections for the given host:port
            ci = (ConnectionInfo)_hostMap.get(connectionKey);
            if (ci == null)
            {
                if (_log.isDebugEnabled())
                {
                    _log.debug("Creating new connection "
                        + "info for: "
                        + connectionKey);
                }
                ci = new ConnectionInfo(this,
                                        connectionKey,
                                        _globalProxyIncarnation);
                _hostMap.put(connectionKey, ci);
            }
            return ci;
        }
    }

    /**
     * Make the given HttpConnection available for use by other requests. If
     * another thread is blocked in getConnection() waiting for a connection for
     * this host:port, they will be woken up.
     * 
     * @param conn -
     *            The HttpConnection to make available.
     */
    public void releaseConnection(HttpConnection conn)
    {
        if (_log.isDebugEnabled())
        {
            _log.debug("Releasing connection: " + conn);
        }

        if (conn._released && conn.isOpen())
            Util.impossible("Connection released twice: " + conn);

        synchronized (this)
        {
            ConnectionInfo ci = getConnectionInfo(conn._handle);

            // This connection does not belong to this ConnectionInfo
            // because the connection was created before a reset.
            if (ci._proxyIncarnation != conn.getProxyIncarnation())
            {
                if (_log.isDebugEnabled())
                {
                    _log.debug("Closed connection due to "
                        + "non-equal proxyIncarnation: "
                        + conn
                        + " ci._proxyInc: "
                        + ci._proxyIncarnation
                        + " conn inc: "
                        + conn.getProxyIncarnation());
                }
                conn.close();
                return;
            }

            // Don't put a closed connection back
            if (!conn.isOpen())
            {
                if (_log.isDebugEnabled())
                {
                    _log.debug("releaseConnection - "
                        + "was closed (not putting back): "
                        + conn);
                }

                // This connection is already in the avail queue, remove it
                if (conn._released)
                {
                    if (_log.isDebugEnabled())
                    {
                        _log.debug("releaseConnection - "
                            + "removing closed conn from avail queue");
                    }
                    ci.removeAvailConnection(conn);
                }
                else
                {
                    ci.removeAssignedConnection(conn);
                }

                if (checkConnCount(ci, null))
                    _timeout.shutdown();
            }
            else
            {
                conn.setLastTimeUsed();

                // Put the connect back in the available list
                ci.connectionAvail(conn);
                if (conn._idleTimeout > 0)
                    _timeout.startIdleTimeout(conn);
            }

            conn._released = true;
            _log.debug("releaseConnection: released = true");
            this.notifyAll();
        }
    }

    // Checks if there are no more connections, then gets rid of the host table
    // entry.
    // Return true to shutdown
    boolean checkConnCount(ConnectionInfo ci, Iterator hostMapIterator)
    {
        synchronized (this)
        {
            _log.debug("checkConnCount");
            if (ci.getActiveConnectionCount() == 0)
            {
                if (_log.isDebugEnabled())
                {
                    _log.debug("Removing ConnectionInfo for: "
                        + ci._connectionKey);
                }
                if (hostMapIterator != null)
                    hostMapIterator.remove();
                else
                    _hostMap.remove(ci._connectionKey);
            }

            // Shutdown the timeout thread if this is the last
            // connection
            if (_hostMap.size() == 0)
                return true;
        }

        // no shutdown
        return false;
    }

    /**
     * Returns the ConnectionInfo for the URL using the current proxy settings.
     * This is used only by the tests
     */
    private ConnectionInfo getConnectionInfoFromUrl(String url)
    {
        synchronized (this)
        {
            String hostAndPort = URIUtil.getProtocolHostPort(url);
            String proxyHost = HttpURLConnection.getProxyHost();
            int proxyPort = HttpURLConnection.getProxyPort();
            String connectionKey = getConnectionKey(hostAndPort,
                                                    proxyHost,
                                                    proxyPort);
            return getConnectionInfo(connectionKey);
        }
    }

    /**
     * Returns the number of connections currently in use for the specified
     * host/port.
     */
    int getActiveConnectionCount(String url)
    {
        synchronized (this)
        {
            ConnectionInfo ci = getConnectionInfoFromUrl(url);
            int count = ci._assignedConnections.size();
            if (_log.isDebugEnabled())
            {
                _log.debug("active connection count: "
                    + ci._connectionKey
                    + " - "
                    + count);
            }
            return count;
        }
    }

    /**
     * Returns the number of connections currently in use for the specified
     * host/port.
     */
    int getTotalConnectionCount(String url)
    {
        synchronized (this)
        {
            ConnectionInfo ci = getConnectionInfoFromUrl(url);
            if (_log.isDebugEnabled())
            {
                _log.debug("total connection count: "
                    + ci._connectionKey
                    + " - "
                    + ci.getActiveConnectionCount());
            }
            return ci.getActiveConnectionCount();
        }
    }

    public boolean checkEverythingEmpty()
    {
        if (_threadUrlConCountMap.size() > 0)
        {
            _log.error("_threadUrlConCountMap not empty: "
                + _threadConnectionMap.size());
            System.out.println(_threadUrlConCountMap);
            return false;
        }
        return true;
    }

    /**
     * See if any connections should be killed because they have passed their
     * idle timeout time.
     * 
     * @return the number of milliseconds when the next connection will timeout
     */
    long checkIdleConnections()
    {
        _log.trace("checkIdleConnections");

        boolean shutdown = false;
        long currentTime = System.currentTimeMillis();
        long wakeTime = 0;

        synchronized (this)
        {
            Collection hosts = _hostMap.values();
            Iterator hostIt = hosts.iterator();
            while (hostIt.hasNext())
            {
                ConnectionInfo ci = (ConnectionInfo)hostIt.next();
                Iterator connIt = ci._availableConnections.iterator();
                while (connIt.hasNext())
                {
                    HttpConnection conn = (HttpConnection)connIt.next();
                    long idleTimeout = conn._idleTimeout;
                    if (_log.isDebugEnabled())
                    {
                        _log.debug("Looking at: "
                            + conn
                            + " idle timeout: "
                            + idleTimeout
                            + " last used: "
                            + conn._lastTimeUsed);
                    }
                    if (idleTimeout == 0)
                        continue;
                    long timeToDie = conn._lastTimeUsed + idleTimeout;
                    if (timeToDie <= currentTime)
                    {
                        if (_log.isDebugEnabled())
                        {
                            _log.debug("Closed due to to idle timeout of: "
                                + idleTimeout
                                + " - "
                                + ci._connectionKey);
                        }
                        conn.close();
                        connIt.remove();
                        shutdown |= checkConnCount(ci, hostIt);
                        // Wake anyone who might be waiting for a connection
                        // slot
                        this.notifyAll();
                    }
                    else if (wakeTime == 0 || timeToDie < wakeTime)
                    {
                        wakeTime = timeToDie;
                    }
                }
            }
        }

        // Can't get the timeout lock while the manager is locked
        if (shutdown)
            _timeout.shutdown();

        // Time to wake up again
        if (_log.isDebugEnabled())
        {
            _log.debug("Time to wake timeout thread: " + wakeTime);
        }
        return wakeTime;
    }

    /**
     * Resets the connection pool, terminating all of the currently open
     * connections. This is called when the state of the proxy host changes.
     */
    void resetConnectionPool()
    {
        synchronized (this)
        {
            _log.trace("resetConnectionPool");

            // Use this to make sure we destroy any connections
            // returned with a prior proxy incarnation number
            _globalProxyIncarnation++;

            Collection hosts = _hostMap.values();
            Iterator it = hosts.iterator();
            while (it.hasNext())
            {
                // Close all of the existing connections. If there
                // are connections that are in use, they are closed
                // as they are released, since the ConnectionInfo
                // entry will not be found.
                ConnectionInfo ci = (ConnectionInfo)it.next();
                if (_log.isDebugEnabled())
                    _log.debug("Closing connections for: " + ci._connectionKey);
                ci.closeAllConnections();
            }
            _hostMap = new HashMap();
        }

        // Can't get the timeout lock while the manager is locked
        _timeout.shutdown();
    }

    void dumpConnectionPool()
    {
        synchronized (this)
        {
            _log.trace("dumpConnectionPool");

            Collection hosts = _hostMap.values();
            Iterator it = hosts.iterator();
            while (it.hasNext())
            {
                ConnectionInfo ci = (ConnectionInfo)it.next();
                System.out.println(ci.dump(0));
            }
        }
    }

    // Statistics

    public void resetStatistics()
    {
        _retryCounts = new ArrayList();
        _pipelineReadRetryCounts = new ArrayList();
        _requestCounts = new int[COUNT_LAST + 1];
    }

    void recordRetry(ArrayList counts, int numTries)
    {
        synchronized (this)
        {
            // System.out.println("record retry: " + numTries);

            AtomicInteger ai = null;
            if (numTries < counts.size())
                ai = (AtomicInteger)counts.get(numTries);
            else
            {
                for (int i = counts.size(), len = numTries; i <= len; i++)
                {
                    ai = new AtomicInteger();
                    counts.add(i, ai);
                }
                // When this exists, the last ai is the one we want
            }
            ai.incrementAndGet();
        }
    }

    void recordCount(int reason)
    {
        synchronized (this)
        {
            _requestCounts[reason]++;
        }
    }

    String getRetryStatistics(List counts)
    {
        StringBuffer sb = new StringBuffer();

        for (int i = 0, len = counts.size(); i < len; i++)
        {
            if (i == 0)
                continue;
            AtomicInteger ai = (AtomicInteger)counts.get(i);
            if (ai != null && ai.intValue() > 0)
            {
                // The number it is recorded under is the num tries
                // before the retry
                sb.append("  Retry times: "
                    + (i + 1)
                    + " count: "
                    + ai.intValue()
                    + "\n");
            }
        }
        return sb.toString();
    }

    String getFailureStatistics()
    {
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < _requestCounts.length; i++)

            sb.append(_requestCountNames[i] + " " + _requestCounts[i] + "\n");

        return sb.toString();
    }

    String getStatistics()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("Retry counts: \n");
        sb.append(getRetryStatistics(_retryCounts));
        sb.append("Pipeline Retry counts: \n");
        sb.append(getRetryStatistics(_pipelineReadRetryCounts));
        sb.append("Counts: \n");
        sb.append(getFailureStatistics());
        return sb.toString();
    }

}
