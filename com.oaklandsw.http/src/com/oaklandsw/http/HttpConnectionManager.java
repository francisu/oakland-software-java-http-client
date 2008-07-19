//
// Copyright 2002-2007, oakland software, all rights reserved.
//
// May not be used or redistributed without specific written
// permission from oakland software.
//
package com.oaklandsw.http;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.oaklandsw.util.LogUtils;
import com.oaklandsw.util.URIUtil;
import com.oaklandsw.util.Util;

import edu.emory.mathcs.backport.java.util.concurrent.BlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.LinkedBlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;

/**
 * Creates and manages a pool of HttpConnections.
 * <p>
 * Used only to get an HttpConnection to allow repeated use of a specific
 * connection for a HttpURLConnection.
 */
public class HttpConnectionManager
{
    private static final Log      _log                        = LogUtils
                                                                      .makeLogger();

    private static final Log      _connLog                    = LogFactory
                                                                      .getLog(HttpConnection.CONN_LOG);

    // See the comment about _urlConReleased in HttpURLConnection
    protected static final int    NOT_RELEASED_TIMEOUT        = 10000;

    // Keep the connection info around for 10 mins, so we don't lose the
    // statistics
    protected static final int    CONNECTION_INFO_RETAIN_TIME = 600000;

    protected GlobalState         _globalState;

    // K(connection key) V(ConnectionInfo)
    private Map                   _hostMap                    = new HashMap();

    private HttpConnectionTimeout _timeout;

    // K(HttpConnection) v(a Thread)
    protected Map                 _threadConnectionMap;

    // K(current Thread) V(AtomicInteger) - count of urlcons to read
    Map                           _threadUrlConCountMap;

    // Cache of credentials associated with a particular connection
    // K(connectionKey> V(Credential)
    protected Map[]               _credentialCache;

    // The total number of urlCons that need to be written,
    // Synchronized by the connection manager; this is used to help
    // track flushing connections
    int                           _outstandingWrites;

    // The total number of connections that have unflushed data,
    // this is a count of the number of connections that have the
    // _needsFlush flag set
    int                           _unflushedConnections;

    // Used to synchronize the pipeline related methods, as distinct
    // from the connection management methods which are synchronized
    // with "this". This is a lower-level lock than the conn mgr (this)
    // lock, so make sure you don't lock the conn mgr (or anything else)
    // while holding this
    protected String              _pipelineLock;

    BlockingQueue                 _asyncQueue;

    HttpPipelineAsyncThread       _asyncThread;

    // Listens at the level of the socket connections
    ConnectionIOListener          _socketIoListener;

    // Listens at the level of the url connections
    ConnectionIOListener          _urlConnIoListener;

    // Statistics

    // V(AtomicInteger) - by number of tries when it succeeds
    ArrayList                     _retryCounts;
    ArrayList                     _pipelineReadRetryCounts;

    public static final int       COUNT_ATTEMPTED             = 0;
    public static final int       COUNT_SUCCESS               = 1;
    public static final int       COUNT_PIPELINE_WRITE_REQ    = 2;
    public static final int       COUNT_PIPELINE_READ_RESP    = 3;
    public static final int       COUNT_PIPELINE_ERROR        = 4;
    public static final int       COUNT_TOTAL_RETRY           = 5;
    public static final int       COUNT_TOTAL_PIPELINE_RETRY  = 6;
    public static final int       COUNT_FAIL_MAX_RETRY        = 7;
    public static final int       COUNT_FAIL_GE_400           = 8;
    public static final int       COUNT_PIPELINE_DEPTH_HIGH   = 9;
    public static final int       COUNT_FORCED_FLUSHES        = 10;
    public static final int       COUNT_BUFFER_FLUSHES        = 11;
    public static final int       COUNT_AVOIDED_FLUSHES       = 12;
    public static final int       COUNT_FLUSH_IO_ERRORS       = 13;

    public static final int       COUNT_LAST                  = COUNT_FLUSH_IO_ERRORS;

    int[]                         _requestCounts;
    String[]                      _requestCountNames          = new String[] { //
                                                              "UrlCons Attempted:              " //
        , "UrlCons Success:                " //
        , "Pipeline write request:         " //
        , "Pipeline read response:         " //
        , "Pipeline error:                 " //
        , "Total retries:                  " //
        , "Total pipelined retries:        " //
        , "UrlCons Failed Max Retries:     " //
        , "UrlCons Failed > 400:           " //
        , "Pipeline max depth (all conns): " //
        , "Forced Flushes:                 " //
        , "Buffer Flushes:                 " //
        , "Avoided Flushes:                " //
        , "Flush IOExceptions:             " //
                                                              };

    // Diagnostic interface only
    // public interface CheckResults
    // {
    // public boolean checkCounts();
    // }

    // public CheckResults _checkResults;

    public HttpConnectionManager(GlobalState globalState)
    {
        _globalState = globalState;
        _timeout = new HttpConnectionTimeout(this);
        _threadConnectionMap = new HashMap();
        _threadUrlConCountMap = new HashMap();
        _pipelineLock = new String();

        resetCachedCredentials();
        initAsyncQueue();
        resetStatistics();
    }

    /**
     * Sets a ConnectionIOListener to receive events associated with data on the
     * all connections.
     * 
     * The information for which HttpURLConnection is not available when using
     * this method. Use this if you are very concerned about performance as this
     * does not create an extra pair of objects for each HttpURLConnection.
     * 
     * @param ioListener
     */
    public void setSocketConnectionIOListener(ConnectionIOListener ioListener)
    {
        _socketIoListener = ioListener;
    }

    /**
     * FIXME - this is not worked out yet, remove it or make it public when it
     * is worked out.
     * 
     * Sets a ConnectionIOListener to receive events associated with data on the
     * all connections.
     * 
     * This is used when you must know the HttpURLConnection that is
     * sending/receiving the data. This has a performance penalty of creating
     * two extra object per HttpURLConnection.
     * 
     * @param ioListener
     */
    void setUrlConnectionIOListener(ConnectionIOListener ioListener)
    {
        _urlConnIoListener = ioListener;
    }

    protected void initAsyncQueue()
    {
        // Create this with unlimited capacity so it never blocks
        _asyncQueue = new LinkedBlockingQueue();
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
                if (_connLog.isDebugEnabled())
                {
                    _connLog.debug("enqueueWork: "
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

    // Called when the connection thread is going to terminate
    // because of lack of work
    void connectionThreadTerminated(HttpConnection conn)
    {
        synchronized (this)
        {
            if (_connLog.isDebugEnabled())
            {
                _connLog.debug("connThreadTerminated: " + "conn: " + conn);
            }
            _threadConnectionMap.remove(conn);
        }
    }

    // Executes the processPipelinedRead on the specified urlCon, creating
    // the thread if necessary, also releases the connection from the urlCon
    void enqueueAsyncUrlCon(HttpURLConnection urlCon)
        throws InterruptedException
    {
        if (_log.isDebugEnabled())
        {
            _log.debug("enqueueAsyncUrlCon: " + " enqueued: " + urlCon);
        }

        addUrlConToWrite(urlCon);

        // In contrast to the connection thread, this put() can not block
        // because it's implemented with a LinkedBlockingQueue
        synchronized (this)
        {
            _asyncQueue.put(urlCon);

            if (_asyncThread == null)
                startAsyncThread();
        }
    }

    protected void startAsyncThread()
    {
        _asyncThread = new HttpPipelineAsyncThread(this);
        _asyncThread.start();
        if (_connLog.isDebugEnabled())
        {
            _connLog.debug("enqueueAsyncUrlCon: "
                + " created thread: "
                + _asyncThread);
        }

    }

    // Called when the pipelined async thread terminates due to lack of
    // work
    void asyncThreadTerminated()
    {
        synchronized (this)
        {
            if (_connLog.isDebugEnabled())
            {
                _connLog.debug("asyncThreadTerminated: " + _asyncThread);
            }
            _asyncThread = null;

            // Still work to do, start up another thread to handle it
            if (!_asyncQueue.isEmpty())
                startAsyncThread();
        }
    }

    // Called when a pipelined read is done, making a pipeline
    // slot available on the connection; wake up anyone waiting for
    // a new connection
    void pipelineReadCompleted()
    {
        synchronized (this)
        {
            _log.trace("pipelineReadCompleted");
            notifyAll();
        }
    }

    //
    // Methods below are synchronized with the _pipelineLock;
    // they cannot use the conn manager lock because of issues
    // with the order of locking
    // 

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

    void addUrlConToWrite(HttpURLConnection urlCon)
    {
        synchronized (_pipelineLock)
        {
            _outstandingWrites++;
            if (_log.isDebugEnabled())
            {
                _log.debug("addUrlConToWrite - "
                    + _outstandingWrites
                    + " outstandingWrites after");
            }
            urlCon._pipelineQueuedForWrite = true;
        }
    }

    void urlConWasWritten(HttpURLConnection urlCon) throws IOException
    {
        // We can be called sometimes (in error conditions) when the
        // urlcon is not actually pipelined
        if (!urlCon._pipelineQueuedForWrite)
            return;

        urlCon._pipelineQueuedForWrite = false;

        boolean flushAll = false;

        synchronized (_pipelineLock)
        {
            _outstandingWrites--;
            if (_log.isDebugEnabled())
            {
                _log.debug("urlConWasWritten - "
                    + _outstandingWrites
                    + " outstandingWrites after");
            }
            if (_outstandingWrites == 0)
            {
                // Make sure all connections are flushed
                if (_unflushedConnections > 0)
                    flushAll = true;
            }
            else if (_outstandingWrites < 0)
            {
                _outstandingWrites = 0;
                // Don't worry about a check for underflow
                // TODO - why is this again?
            }

        }

        // Do this outside of _pipelineLock since it locks
        // the conn mgr
        if (flushAll)
            flushAllConnections();
    }

    int getOutstandingWrites()
    {
        synchronized (_pipelineLock)
        {
            return _outstandingWrites;
        }
    }

    void updateNeedsFlushCount(HttpConnection conn)
    {
        synchronized (_pipelineLock)
        {
            if (conn._needsFlush)
                _unflushedConnections++;
            else
                _unflushedConnections--;
            if (_connLog.isDebugEnabled())
            {
                _connLog.debug("toggleNeedsFlush - unflushed: "
                    + _unflushedConnections);
            }
        }
    }

    void urlConWasRead(Thread thread)
    {
        synchronized (_pipelineLock)
        {
            AtomicInteger ai = (AtomicInteger)_threadUrlConCountMap.get(thread);
            // This can happen during a resetAllConnections()
            if (ai == null)
                return;
            int count = ai.decrementAndGet();
            if (_log.isDebugEnabled())
                _log.debug("urlConWasRead - count (after) " + count);

            // count < 0 can happen during an immediateShutdown

            if (count <= 0)
            {
                _pipelineLock.notifyAll();
                _threadUrlConCountMap.remove(thread);
            }
        }
    }

    // Used during shutdown to wake everyone waiting because everything
    // has been canceled
    void wakeAllBlockers()
    {
        synchronized (_pipelineLock)
        {
            Iterator it = _threadUrlConCountMap.values().iterator();
            while (it.hasNext())
            {
                AtomicInteger ai = (AtomicInteger)it.next();
                ai.set(0);
                _pipelineLock.notifyAll();
                it.remove();
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
                _connLog.debug("no outstanding connections "
                    + "(they probably all finished - continuing");
                return;
            }
            int count;
            while ((count = ai.get()) != 0)
            {
                if (_connLog.isDebugEnabled())
                {
                    _connLog.debug("waiting for "
                        + count
                        + " pipelined urlcons to complete");
                }
                try
                {
                    _pipelineLock.wait();
                }
                catch (InterruptedException ex)
                {
                    if (_connLog.isDebugEnabled())
                    {
                        _connLog.debug("interrupted while waiting for "
                            + count
                            + " pipelined urlcons to complete");
                    }
                    throw ex;
                }
            }
            _connLog.debug("pipelined urlcons are complete");
        }
    }

    //
    // End of methods synchronized with the _pipelineLock
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
            InterruptedException,
            MalformedURLException
    {
        long connectionTimeout = urlCon.getConnectionTimeout();
        String url = urlCon.getUrlString();
        long idleTimeout = urlCon.getIdleConnectionTimeout();
        long idlePing = urlCon.getIdleConnectionPing();
        String proxyHost = urlCon.getConnectionProxyHost();
        int proxyPort = urlCon.getConnectionProxyPort();

        // Make sure this is not on the non-proxy list
        if (_globalState._nonProxyHostsString != null && proxyHost != null)
        {
            // Only applies if the proxy was set globally, if the proxy
            // is set directly, then it goes on the proxy even if it's in
            // the non-proxy hosts list
            if (proxyHost.equals(_globalState._proxyHost)
                && _globalState.isNonProxyHost(URIUtil.getHost(url)))
            {
                proxyHost = null;
                proxyPort = -1;
            }
        }

        if (!HttpURLConnection._urlConReleased)
        {
            if (_connLog.isDebugEnabled())
            {
                _connLog.debug("Setting not released timeout: "
                    + NOT_RELEASED_TIMEOUT);
            }
            connectionTimeout = NOT_RELEASED_TIMEOUT;
        }

        // Get the protocol and port (use default port if not specified)
        // FIXME - this is called also in HttpURLConnectInternal
        final String protocol = URIUtil.getProtocol(url);
        String hostAndPort = URIUtil.getProtocolHostPort(url);
        HttpConnection conn = null;

        synchronized (this)
        {
            // Look for a list of connections for the given host:port
            ConnectionInfo ci = getConnectionInfo(hostAndPort,
                                                  proxyHost,
                                                  proxyPort);
            String connectionKey = ci._connectionKey;
            String proxyKey = ci._proxyKey;

            if (_log.isDebugEnabled())
            {
                _log.debug("Requested connection: " + connectionKey);
            }

            while (true)
            {
                // Found one
                conn = ci.getMatchingConnection(urlCon);
                if (conn != null)
                    break;

                // Can create one
                if (_globalState._maxConns <= 0
                    || ci.getActiveConnectionCount() < _globalState._maxConns)
                    break;

                // Have to wait for an existing one to be freed
                if (_connLog.isDebugEnabled())
                {
                    _connLog.debug("Waiting for: "
                        + connectionKey
                        + " waiting for: "
                        + connectionTimeout
                        + "\n"
                        + ci.dump(0));
                }

                long startTime = System.currentTimeMillis();

                wait(connectionTimeout);

                // Need to refetch this as it might have changed since
                // the wait unlocked
                ci = getConnectionInfo(connectionKey, proxyKey);

                // Add the 32ms because wait seems to wake a little early
                // sometimes
                long waitTime = (System.currentTimeMillis() - startTime) + 32;

                if (connectionTimeout > 0 && (waitTime >= connectionTimeout))
                {
                    if (!HttpURLConnection._urlConReleased
                        && connectionTimeout == NOT_RELEASED_TIMEOUT)
                    {
                        IllegalStateException ex = new IllegalStateException("Possible programming error: "
                            + "You have timed out waiting for a "
                            + "connection and our records indicate you have not "
                            + "called getInputStream() and read the results yet.  If "
                            + "the response code is successful (20x), "
                            + "and there is data returned, you must call "
                            + "getInputStream() and close the stream. ");
                        if (_connLog.isDebugEnabled())
                        {
                            _connLog.debug("Possible programming error", ex);
                        }
                        throw ex;
                    }

                    _connLog.debug("Timed out waiting for connection");
                    throw new HttpTimeoutException();
                }

                if (_connLog.isDebugEnabled())
                {
                    _connLog.debug("Waiting END: "
                        + connectionKey
                        + " waited: "
                        + waitTime);
                }
            }

            // We have the capacity to get a connection, and we did not get
            // one in the above loop, see if there is one available
            if (conn == null)
                conn = ci.getMatchingConnection(urlCon);

            if (conn == null)
            {
                _connLog.debug("Creating new connection");

                String host;
                int port;
                // Start after the protocol
                int start = hostAndPort.indexOf(':') + 3;
                int portLookStart = start;
                boolean ipV6Literal = false;
                if (hostAndPort.charAt(start) == '[')
                {
                    portLookStart = hostAndPort.indexOf(']', start);
                    start++;
                    ipV6Literal = true;
                }
                int ind = hostAndPort.indexOf(':', portLookStart);
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

                if (ipV6Literal)
                    host = host.substring(0, host.length() - 1);
                
                // Create a new connection
                boolean isSecure = protocol.equalsIgnoreCase("HTTPS")
                    || urlCon.isForceSSL()
                    || urlCon.isConnectionProxySsl();

                // Make a new connection, note that this does not open
                // the connection, it only creates it, so this takes
                // little time
                conn = new HttpConnection(urlCon,
                                          proxyHost,
                                          proxyPort,
                                          host,
                                          port,
                                          isSecure,
                                          ipV6Literal,
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

    private StringBuffer createProxyKey(String proxyHost, int proxyPort)
    {
        // No proxy information
        if (proxyHost == null && proxyPort > 0)
            return null;

        // Have to build the key
        StringBuffer buf = new StringBuffer(200);
        if (proxyHost != null)
            buf.append(proxyHost);
        if (proxyPort > 0)
            buf.append(Integer.toString(proxyPort));
        return buf;
    }

    /**
     * Get the pool (list) of connections available for the given host and port
     * and proxy host/port
     * 
     * @param connectionKey
     *            the key for the connection pool
     * @return the information for the connections of this host and port.
     */
    private ConnectionInfo getConnectionInfo(String connectionKey,
                                             String proxyKey)
    {
        ConnectionInfo ci;

        synchronized (this)
        {
            // Look for a list of connections for the given host:port
            ci = (ConnectionInfo)_hostMap.get(connectionKey);
            if (ci == null)
            {
                if (_connLog.isDebugEnabled())
                {
                    _connLog.debug("Creating new connection "
                        + "info for: "
                        + connectionKey);
                }
                ci = new ConnectionInfo(this, connectionKey, proxyKey);
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
     * @throws InterruptedException
     */
    public void releaseConnection(HttpConnection conn)
        throws InterruptedException
    {
        if (_log.isDebugEnabled())
        {
            _log.debug("Releasing connection: " + conn);
        }

        // This connection does not belong to this connManager
        if (conn._connManager != this)
        {
            if (_connLog.isDebugEnabled())
            {
                _connLog.debug("Closed and ignored connection due to "
                    + "old connMgr");
            }
            conn.close();
            return;
        }

        synchronized (this)
        {
            if (conn._released && conn.isOpen())
                Util.impossible("Connection released twice: " + conn);

            ConnectionInfo ci = getConnectionInfo(conn._connectionKey,
                                                  conn._proxyKey);

            // Don't put a closed connection back
            if (!conn.isOpen())
            {
                if (_connLog.isDebugEnabled())
                {
                    _connLog.debug("releaseConnection - "
                        + "was closed (not putting back): "
                        + conn);
                }

                // This connection is already in the avail queue, remove it
                if (conn._released)
                {
                    if (ci.isConnectionAvail(conn))
                    {
                        if (_connLog.isDebugEnabled())
                        {
                            _connLog.debug("releaseConnection - "
                                + "removing closed conn from avail queue");
                        }
                        ci.removeAvailConnection(conn);
                    }
                }
                else
                {
                    ci.removeAssignedConnection(conn);
                }
            }
            else
            {
                // Put the connect back in the available list
                ci.connectionAvail(conn);
                if (conn._idleTimeout > 0)
                    _timeout.startIdleTimeout(conn);
            }

            conn._released = true;
            _log.debug("releaseConnection: released = true");
            // Wake all waiters
            notifyAll();
        }
    }

    protected void flushAllConnections() throws IOException
    {
        _connLog.debug("flushAllConnections");

        synchronized (this)
        {
            Collection hosts = _hostMap.values();
            Iterator it = hosts.iterator();
            while (it.hasNext())
            {
                // Close all of the existing connections.
                ConnectionInfo ci = (ConnectionInfo)it.next();
                if (_connLog.isDebugEnabled())
                {
                    _connLog.debug("Flushing connections for: "
                        + ci._connectionKey);
                }
                ci.flushAllAvailConnections();
            }
        }
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
            return getConnectionInfo(hostAndPort, HttpURLConnection
                    .getProxyHost(), HttpURLConnection.getProxyPort());
        }
    }

    private ConnectionInfo getConnectionInfo(String hostAndPort,
                                             String proxyHost,
                                             int proxyPort)
    {
        synchronized (this)
        {
            StringBuffer sb = createProxyKey(proxyHost, proxyPort);
            String proxyKey = null;
            String connectionKey;
            if (sb == null)
                connectionKey = hostAndPort;
            else
            {
                proxyKey = sb.toString();
                sb.append(hostAndPort);
                connectionKey = sb.toString();
            }
            return getConnectionInfo(connectionKey, proxyKey);
        }
    }

    /**
     * Returns the number of connections currently in use for the specified
     * host/port.
     */
    public int getActiveConnectionCount(String url)
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
    public int getTotalConnectionCount(String url)
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
            _log.error(_threadUrlConCountMap);
            return false;
        }
        return true;
    }

    /**
     * See if any connections should be killed because they have passed their
     * idle timeout time.
     * 
     * @return the number of milliseconds when the next connection will timeout
     * @throws InterruptedException
     */
    long checkIdleConnections() throws InterruptedException
    {
        _log.trace("checkIdleConnections");

        long currentTime = System.currentTimeMillis();
        long wakeTime = 0;

        List connectionsToClose = null;

        synchronized (this)
        {
            Collection hosts = _hostMap.values();
            Iterator hostIt = hosts.iterator();
            while (hostIt.hasNext())
            {
                ConnectionInfo ci = (ConnectionInfo)hostIt.next();

                // Get rid of any stale ConnectionInfos
                if (ci.getActiveConnectionCount() == 0
                    && System.currentTimeMillis() - ci._usedTime > CONNECTION_INFO_RETAIN_TIME)
                {
                    if (_log.isDebugEnabled())
                        _log.debug("Removing:  " + ci);
                    hostIt.remove();
                    continue;
                }

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
                    // Don't kill connections that have not done anything yet
                    if (timeToDie <= currentTime && conn._lastTimeUsed > 0)
                    {
                        if (_connLog.isDebugEnabled())
                        {
                            _connLog.debug("Closed due to to idle timeout of: "
                                + idleTimeout
                                + " - "
                                + conn);
                        }

                        // Do the actual close outside of the lock
                        if (connectionsToClose == null)
                            connectionsToClose = new ArrayList();
                        connectionsToClose.add(conn);

                        connIt.remove();
                        // Wake anyone who might be waiting for a connection
                        // slot
                        notifyAll();
                    }
                    else if (wakeTime == 0 || timeToDie < wakeTime)
                    {
                        wakeTime = timeToDie;
                    }
                }
            }
        }

        // Close them outside of the lock because they might want on some
        // other work to complete
        if (connectionsToClose != null)
        {
            for (int i = 0, len = connectionsToClose.size(); i < len; i++)
            {
                HttpConnection conn = (HttpConnection)connectionsToClose.get(i);
                if (_connLog.isDebugEnabled())
                {
                    _connLog
                            .debug("Actually closing connection (idle) " + conn);
                }
                conn.close();
            }
        }

        // Time to wake up again
        if (_log.isDebugEnabled())
        {
            _log.debug("Time to wake timeout thread: " + wakeTime);
        }
        return wakeTime;
    }

    private String getCacheKey(ConnectionInfo ci, int authType)
    {
        if (authType == HttpURLConnection.AUTH_NORMAL)
            return ci._connectionKey;
        return ci._proxyKey;
    }

    // public for tests
    public Credential getCachedCredential(ConnectionInfo ci, int authType)
    {
        synchronized (this)
        {
            String key = getCacheKey(ci, authType);
            Credential cred = (Credential)_credentialCache[authType].get(key);
            if (_log.isDebugEnabled())
            {
                _log.debug("getCachedCredential: " + key + " cred: " + cred);
                _log.debug("credential cache: " + _credentialCache[authType]);
            }
            return cred;
        }
    }

    void setCachedCredential(ConnectionInfo ci, int authType, Credential cred)
    {
        synchronized (this)
        {
            String key = getCacheKey(ci, authType);
            if (_log.isDebugEnabled())
                _log.debug("setCachedCredential: " + key + " cred: " + cred);
            _credentialCache[authType].put(key, cred);
        }
    }

    void resetCachedCredential(ConnectionInfo ci, int authType)
    {
        synchronized (this)
        {
            String key = getCacheKey(ci, authType);
            if (_log.isDebugEnabled())
                _log.debug("resetCachedCredential: " + key);
            _credentialCache[authType].remove(key);
        }
    }

    void resetCachedCredentials()
    {
        _log.debug("resetCachedCredentials");
        synchronized (this)
        {
            _credentialCache = new Map[HttpURLConnection.AUTH_PROXY + 1];
            for (int i = 0; i <= HttpURLConnection.AUTH_PROXY; i++)
                _credentialCache[i] = new HashMap();
        }
    }

    static final boolean IMMEDIATE = true;

    /**
     * Resets the connection pool, terminating all of the currently open
     * connections. This is called when the state of the proxy host changes.
     * 
     * @throws InterruptedException
     */
    void resetConnectionPool(boolean immediate) throws InterruptedException
    {
        // No longer use this connection manager
        HttpURLConnection.resetConnectionManager();

        synchronized (this)
        {
            _connLog.debug("resetConnectionPool - immediate: " + immediate);

            if (immediate)
            {
                // Reset all of the pipelining stuff
                initAsyncQueue();

                // This will cause the thread to end and the pointer to reset
                if (_asyncThread != null)
                    _asyncThread.interrupt();

                wakeAllBlockers();
                _connLog
                        .debug("resetConnectionPool - finished immediate async/pipeline");
            }

            Collection hosts = _hostMap.values();
            Iterator it = hosts.iterator();
            while (it.hasNext())
            {
                // Close all of the existing connections.
                ConnectionInfo ci = (ConnectionInfo)it.next();
                if (_connLog.isDebugEnabled())
                {
                    _connLog.debug("Closing connections for: "
                        + ci._connectionKey);
                }
                ci.closeAllConnections(immediate);
            }

            _hostMap = new HashMap();

            _timeout.shutdown();
            _connLog.debug("resetConnectionPool - FINISHED");
        }
    }

    String getConnectionPool()
    {
        StringBuffer sb = new StringBuffer();

        synchronized (this)
        {
            _connLog.trace("dumpConnectionPool");

            Collection hosts = _hostMap.values();
            Iterator it = hosts.iterator();
            while (it.hasNext())
            {
                ConnectionInfo ci = (ConnectionInfo)it.next();
                String s = ci.dump(0);
                sb.append(s);
                _connLog.debug(s);
            }
        }
        return sb.toString();
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
        synchronized (counts)
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

            if (counts == _retryCounts)
                recordCount(COUNT_TOTAL_RETRY);
            else
                recordCount(COUNT_TOTAL_PIPELINE_RETRY);
        }
    }

    void recordCount(int reason)
    {
        synchronized (_requestCounts)
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

    String getCountStatistics()
    {
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < _requestCounts.length; i++)

            sb.append(_requestCountNames[i] + " " + _requestCounts[i] + "\n");

        return sb.toString();
    }

    public int getCount(int count)
    {
        return _requestCounts[count];
    }

    public GlobalState getGlobalState()
    {
        return _globalState;
    }

    String getStatistics()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("Retry counts: \n");
        sb.append(getRetryStatistics(_retryCounts));
        sb.append("Pipeline Retry counts: \n");
        sb.append(getRetryStatistics(_pipelineReadRetryCounts));
        sb.append("Counts: \n");
        sb.append(getCountStatistics());
        _connLog.debug(sb.toString());
        return sb.toString();
    }

}
