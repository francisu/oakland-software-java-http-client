//
// Some portions copyright 2002-3003, oakland software, all rights reserved.
//
// May not be used or redistributed without specific written
// permission from oakland software.
//
/*
 * ====================================================================
 * 
 * The Apache Software License, Version 1.1
 * 
 * Copyright (c) 1999-2002 The Apache Software Foundation. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any, must
 * include the following acknowlegement: "This product includes software
 * developed by the Apache Software Foundation (http://www.apache.org/)."
 * Alternately, this acknowlegement may appear in the software itself, if and
 * wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "The Jakarta Project", "HttpClient", and "Apache Software
 * Foundation" must not be used to endorse or promote products derived from this
 * software without prior written permission. For written permission, please
 * contact apache@apache.org.
 * 
 * 5. Products derived from this software may not be called "Apache" nor may
 * "Apache" appear in their names without prior written permission of the Apache
 * Group.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE APACHE
 * SOFTWARE FOUNDATION OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many individuals on
 * behalf of the Apache Software Foundation. For more information on the Apache
 * Software Foundation, please see <http://www.apache.org/>.
 * 
 * [Additional notices, if required by prior licensing conditions]
 * 
 */

package com.oaklandsw.http;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.regexp.RE;
import org.apache.regexp.RESyntaxException;

import com.oaklandsw.util.StringUtils;
import com.oaklandsw.util.URIUtil;

/**
 * Creates and manages a pool of HttpConnections.
 * <p>
 * Used only to get an HttpConnection to allow repeated use of a specific
 * connection for a HttpURLConnection.
 */
public class HttpConnectionManager
{
    private Log                   _log                    = LogFactory
                                                                  .getLog(HttpConnectionManager.class);

    // RFC 2616 sec 8.1.4
    public static int             DEFAULT_MAX_CONNECTIONS = 2;

    private Map                   _hostMap                = new HashMap();

    private int                   _maxConns               = DEFAULT_MAX_CONNECTIONS;

    private String                _proxyHost;
    private int                   _proxyPort              = -1;
    private String                _proxyUser;
    private String                _proxyPassword;

    private String                _nonProxyHostsString;

    private ArrayList             _nonProxyHosts;

    private HttpConnectionTimeout _timeout;

    // Used to keep track of the global proxy state at the time
    // this connection was created.
    private int                   _globalProxyIncarnation;

    private class ConnectionInfo
    {

        // The concatenation of host/port and if a specific
        // proxy host/port was specified, that is added as well.
        // This is used to tell where the connection is going.
        // If the proxy was not specified on a per-connection basis,
        // it is not included here; the proxy incarnation mechanism
        // is used to deal with that.
        public String     _connectionKey;

        // The number that is incremented whenever the global
        // proxy state changes (ie, all connections are reset)
        // This is used to make sure that older connections opened
        // before the reset are closed.
        public int        _proxyIncarnation;

        // The total number of connections that have been created,
        // this includes any that are presently assigned, and any
        // that are in the pool
        public int        _count;

        // List of connections that are presently not assigned
        public LinkedList _connections;
    }

    // Object only for synchronization
    // You cannot get the HttpConnectionTimeout lock while you have
    // this lock as this lock is obtained with the timeout lock locked.
    private HttpConnectionManager _lock;

    /**
     * No-args constructor
     */
    public HttpConnectionManager()
    {
        _lock = this;
        _timeout = new HttpConnectionTimeout(this);
    }

    /**
     * Set the proxy host to use for all connections.
     * 
     * @param proxyHost -
     *            the proxy host name
     */
    void setProxyHost(String proxyHost)
    {
        _proxyHost = proxyHost;
        // Get rid of all current connections as they are not
        // going to the right place.
        resetConnectionPool();
    }

    /**
     * Get the proxy host.
     * 
     * @return the proxy host name
     */
    String getProxyHost()
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
        synchronized (_lock)
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

                // The syntax for a non-proxy host only allows a "*" as wildcard,
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
                _log.debug("Non proxy host: " + host);
                _nonProxyHosts.add(re);
            }
        }
    }

    String getNonProxyHosts()
    {
        synchronized (_lock)
        {
            return _nonProxyHostsString;
        }
    }

    /**
     * Set the proxy port to use for all connections.
     * 
     * @param proxyPort -
     *            the proxy port number
     */
    void setProxyPort(int proxyPort)
    {
        // Get rid of all current connections as they are not
        // going to the right place.
        resetConnectionPool();
        _proxyPort = proxyPort;
    }

    /**
     * Get the proxy port number.
     * 
     * @return the proxy port number
     */
    int getProxyPort()
    {
        return _proxyPort;
    }

    /**
     * Set the maximum number of connections allowed for a given host:port. Per
     * RFC 2616 section 8.1.4, this value defaults to 2.
     * 
     * @param maxConnections -
     *            number of connections allowed for each host:port
     */
    void setMaxConnectionsPerHost(int maxConnections)
    {
        _maxConns = maxConnections;
    }

    /**
     * Get the maximum number of connections allowed for a given host:port.
     * 
     * @return The maximum number of connections allowed for a given host:port.
     */
    int getMaxConnectionsPerHost()
    {
        return _maxConns;
    }

    /**
     * Return the port provided if not -1 (default), return 443 if the protocol
     * is HTTPS, otherwise 80.
     * 
     * This functionality is a URLUtil and may be better off in URIUtils
     * 
     * @param protocol
     *            the protocol to use to get the port for, e.g. http or https
     * @param port
     *            the port provided with the url (could be -1)
     * @return the port for the specified port and protocol.
     */
    private int getPort(String protocol, int port)
    {
        // default to provided port
        int portForProtocol = port;
        if (portForProtocol <= 0)
        {
            if (protocol.equalsIgnoreCase("HTTPS"))
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
    String getProxyHost(String host)
    {
        // This should be OK, as we never lock the connection
        // info while we have the lock on this.
        synchronized (_lock)
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
     * will block forever until a connection becomes available.
     * 
     * @param url -
     *            a fully specified URL.
     * @return an HttpConnection for the given host:port
     * @exception java.net.MalformedURLException
     * @exception com.oaklandsw.http.HttpException
     */
    public HttpConnection getConnection(String url)
        throws HttpException,
            MalformedURLException
    {
        return getConnection(url, 0, 0, 0, null, 0);
    }

    /**
     * Get an HttpConnection for a given URL. The URL must be fully specified
     * (i.e. contain a protocol and a host (and optional port number). If the
     * maximum number of connections for the host has been reached, this method
     * will block for <code>connectionTimeout</code> milliseconds or until a
     * connection becomes available. If no connection becomes available before
     * the timeout expires an HttpException will be thrown.
     * 
     * @param url -
     *            a fully specified string.
     * @param connectionTimeout -
     *            the time (in milliseconds) to wait for a connection to become
     *            available
     * @param idleTimeout -
     *            the time (in milliseconds) to destroy the newly created
     *            connection after it becomes idle.
     * @param idlePing -
     *            the idle time interval (in milliseconds) to send a ping
     *            message before a POST on the newly created connection.
     * @param proxyHost -
     *            the host to use as a proxy.
     * @param proxyPort -
     *            the port number to use as a proxy.
     * @return an HttpConnection for the given host:port
     * @exception java.net.MalformedURLException
     * @exception com.oaklandsw.http.HttpException -
     *                If no connection becomes available before the timeout
     *                expires
     */
    public HttpConnection getConnection(String url,
                                        long connectionTimeout,
                                        long idleTimeout,
                                        long idlePing,
                                        String proxyHost,
                                        int proxyPort)
        throws HttpException,
            MalformedURLException
    {

        // Get the protocol and port (use default port if not specified)
        final String protocol = URIUtil.getProtocol(url);
        String hostAndPort = URIUtil.getProtocolHostPort(url);
        String connectionKey = getConnectionKey(hostAndPort,
                                                proxyHost,
                                                proxyPort);
        if (_log.isDebugEnabled())
        {
            _log.debug("Requested connection: " + connectionKey);
        }

        HttpConnection conn = null;

        synchronized (_lock)
        {
            // Look for a list of connections for the given host:port
            ConnectionInfo ci = getConnectionInfo(connectionKey);

            // Don't have any available connections, wait for one
            // -1 in _maxConns is unlimited
            while (ci._count >= _maxConns
                && _maxConns >= 0
                && ci._connections.size() == 0)
            {
                try
                {
                    if (_log.isDebugEnabled())
                        _log.debug("Waiting for: " + connectionKey);

                    long startTime = 0;
                    if (connectionTimeout > 0)
                        startTime = System.currentTimeMillis();

                    _lock.wait(connectionTimeout);

                    // Need to refetch this as it might have changed since
                    // the wait unlocked
                    ci = getConnectionInfo(connectionKey);

                    if (connectionTimeout > 0
                        && (System.currentTimeMillis() - startTime >= connectionTimeout))
                    {
                        _log.info("Timed out waiting for connection");
                        throw new HttpTimeoutException();
                    }

                    if (_log.isDebugEnabled())
                        _log.debug("Waiting END: " + connectionKey);
                }
                catch (InterruptedException ex)
                {
                    throw new HttpException("Thread killed while waiting");
                }
            }

            if (ci._connections.size() > 0)
            {
                _log.debug("Using existing connection");
                conn = (HttpConnection)ci._connections.removeFirst();
                conn.assertOpen();
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
                ci._count++;
            }

            conn.setIdleTimeout(idleTimeout);
            conn.setIdlePing(idlePing);

            if (_log.isDebugEnabled())
                _log.debug("Obtained connection: " + conn);
        }

        return conn;
    }

    private String getConnectionKey(String hostAndPort,
                                    String proxyHost,
                                    int proxyPort)
    {
        String connectionKey = hostAndPort;

        // If the proxy is per-connection, add that to the connection
        // key so we get the connection going to the right place
        if (proxyHost != null || proxyPort > 0)
        {
            // Start with enough space for the port
            int bufLen = 20;
            if (proxyHost != null)
                bufLen = proxyHost.length();
            StringBuffer buf = new StringBuffer(bufLen);
            buf.append(hostAndPort);
            if (proxyHost != null)
                buf.append(proxyHost);
            if (proxyPort > 0)
                buf.append(Integer.toString(proxyPort));
            connectionKey = buf.toString();
        }

        return connectionKey;
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

        synchronized (_lock)
        {
            // Look for a list of connections for the given host:port
            ci = (ConnectionInfo)_hostMap.get(connectionKey);
            if (ci == null)
            {
                _log
                        .debug("Creating new connection info for: "
                            + connectionKey);
                ci = new ConnectionInfo();
                ci._connectionKey = connectionKey;
                ci._proxyIncarnation = _globalProxyIncarnation;
                ci._connections = new LinkedList();
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
    void releaseConnection(HttpConnection conn)
    {
        if (_log.isDebugEnabled())
        {
            _log.debug("Releasing connection: " + conn);
        }

        boolean startIdle = false;
        boolean shutdown = false;
        synchronized (_lock)
        {
            ConnectionInfo ci = getConnectionInfo(conn.getHandle());

            // This connection does not belong to this ConnectionInfo
            // because the connection was created before a reset.
            if (ci._proxyIncarnation != conn.getProxyIncarnation())
            {
                conn.close();
                return;
            }

            // Don't put a closed connection back
            if (!conn.isOpen())
            {
                _log.debug("Closed connection: " + conn);
                shutdown = reduceConnCount(ci, null);
            }
            else
            {
                conn.setLastTimeUsed();

                // Put the connect back in the available list
                ci._connections.addFirst(conn);
                if (conn.getIdleTimeout() > 0)
                    startIdle = true;
            }
            _lock.notifyAll();
        }

        // Can't get the timeout lock while the manager is locked
        if (shutdown)
            _timeout.shutdown();
        if (startIdle)
            _timeout.startIdleTimeout(conn);

    }

    // Reduces the number of connections present, and if there
    // are no more connections, gets ride of the host table
    // entry. This assumes the ConnectionInfo is locked.
    // Return true to shutdown
    private final boolean reduceConnCount(ConnectionInfo ci,
                                          Iterator hostMapIterator)
    {
        synchronized (_lock)
        {
            _log.trace("reduceConnCount");
            ci._count--;
            if (ci._count == 0)
            {
                _log.debug("Removing ConnectionInfo for: " + ci._connectionKey);
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
     */
    private ConnectionInfo getConnectionInfoFromUrl(String url)
    {
        synchronized (_lock)
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
    public int getActiveConnectionCount(String url)
    {
        synchronized (_lock)
        {
            ConnectionInfo ci = getConnectionInfoFromUrl(url);
            int count = ci._count - ci._connections.size();
            _log.debug("active connection count: " + url + " - " + count);
            return count;
        }
    }

    /**
     * Returns the number of connections currently in use for the specified
     * host/port.
     */
    public int getTotalConnectionCount(String url)
    {
        synchronized (_lock)
        {
            ConnectionInfo ci = getConnectionInfoFromUrl(url);
            _log.debug("total connection count: " + url + " - " + ci._count);
            return ci._count;
        }
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

        synchronized (_lock)
        {
            Collection hosts = _hostMap.values();
            Iterator hostIt = hosts.iterator();
            while (hostIt.hasNext())
            {
                ConnectionInfo ci = (ConnectionInfo)hostIt.next();
                Iterator connIt = ci._connections.iterator();
                while (connIt.hasNext())
                {
                    HttpConnection conn = (HttpConnection)connIt.next();
                    long idleTimeout = conn.getIdleTimeout();
                    if (_log.isDebugEnabled())
                    {
                        _log.debug("Looking at: "
                            + conn
                            + " idle timeout: "
                            + idleTimeout
                            + " last used: "
                            + conn.getLastTimeUsed());
                    }
                    if (idleTimeout == 0)
                        continue;
                    long timeToDie = conn.getLastTimeUsed() + idleTimeout;
                    if (timeToDie <= currentTime)
                    {
                        _log.debug("Closed due to to idle timeout of: "
                            + idleTimeout
                            + " - "
                            + ci._connectionKey);
                        conn.close();
                        connIt.remove();
                        shutdown |= reduceConnCount(ci, hostIt);
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
        _log.debug("Time to wake timeout thread: " + wakeTime);
        return wakeTime;
    }

    /**
     * Resets the connection pool, terminating all of the currently open
     * connections. This is called when the state of the proxy host changes.
     */
    void resetConnectionPool()
    {
        synchronized (_lock)
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
                _log.debug("Closing connections for: " + ci._connectionKey);
                for (int i = 0; i < ci._connections.size(); i++)
                    ((HttpConnection)ci._connections.get(i)).close();
            }
            _hostMap = new HashMap();
        }

        // Can't get the timeout lock while the manager is locked
        _timeout.shutdown();
    }

    void dumpConnectionPool()
    {
        synchronized (_lock)
        {
            _log.trace("dumpConnectionPool");

            Collection hosts = _hostMap.values();
            Iterator it = hosts.iterator();
            while (it.hasNext())
            {
                // Close all of the existing connections. If there
                // are connections that are in use, they are closed
                // as they are released, since the ConnectionInfo
                // entry will not be found.
                ConnectionInfo ci = (ConnectionInfo)it.next();
                System.out.println("Connection: "
                    + ci._connectionKey
                    + " total connections: "
                    + ci._count
                    + " avail connections: "
                    + ci._connections.size());
            }
        }

    }

}
