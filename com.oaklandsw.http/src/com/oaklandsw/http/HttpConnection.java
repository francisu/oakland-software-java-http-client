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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.security.Principal;
import java.security.cert.Certificate;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.security.cert.X509Certificate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.oaklandsw.util.LogUtils;
import com.oaklandsw.util.Util;

public class HttpConnection
{
    public static final String     WIRE_LOG           = "com.oaklandsw.log.http.wireLog";

    private static final Log             _log               = LogUtils.makeLogger();

    /** Log for any wire messages. */
    private static final Log             _wireLog           = LogFactory
                                                              .getLog(WIRE_LOG);

    private Exception              _openException;

    // Used to keep track of the global proxy state at the time
    // this connection was created.
    private int                    _proxyIncarnation;

    private static final int       STREAM_BUFFER_SIZE = 65000;

    /** My host. */
    private String                 _host              = null;

    /** My port. */
    private int                    _port              = -1;

    /**
     * The host/port string to use when part of a URL. This has the port number
     * omitted the port is the default port for the protcol.
     */
    private String                 _hostPortURL;

    /**
     * The host/port string to use to find the connection with the connection
     * manager. This always includes the port number.
     */
    private String                 _hostPort;

    // The way this connection is idenfified by the connection
    // manager.
    private String                 _handle;

    /** My proxy host. */
    private String                 _proxyHost         = null;

    /** My proxy port. */
    private int                    _proxyPort         = -1;

    /** My client Socket. */
    private Socket                 _socket            = null;

    /** My InputStream. */
    private BufferedInputStream    _input             = null;

    /** My OutputStream. */
    private BufferedOutputStream   _output            = null;

    /** Whether or not I am connected. */
    private boolean                _open              = false;

    /** Whether or not I am/should connect via SSL. */
    private boolean                _ssl               = false;

    /** SO_TIMEOUT value */
    private int                    _so_timeout        = 0;

    private int                    _connectTimeout;

    /** An alternative factory for SSL sockets to use */
    private SSLSocketFactory       _sslSocketFactory;

    private HostnameVerifier       _hostnameVerifier;

    /** Whether I am tunneling a proxy or not */
    private boolean                _tunnelEstablished = false;

    private HttpURLConnectInternal _tunnelCon;

    private long                   _lastTimeUsed;

    private long                   _idleTimeout;

    private long                   _idlePing;

    // Used only for testing to simulate a connection that fails to
    // connect
    public static long             _testTimeout;

    // For SSL hostname matching testing
    public static boolean          _testNonMatchHost;

    // This is used to store the method for opening a socket
    // using the JRE 1.4 method with the timeout value. If this
    // is properly initialized, it is used instead of the async timeout
    // mechanism.
    private static Method          _connectMethod;

    private static Constructor     _inetSocketAddressCons;

    private static Class           _socketTimeoutException;

    /**
     * Indicates that NTLM authentication is being used on this connection. This
     * is used by HTTP 1.0 connections to avoid closing the connection since it
     * must be kept open for NTLM to function properly, and we can't send the
     * "Connection: Keep-Alive" to a proxy.
     */
    private boolean                _ntlmLeaveOpen;

    /**
     * Indicates the async open process has completed, and that the notification
     * associated with the async open was not spurious.
     */
    private boolean                _asyncOpenCompleted;

    static
    {
        try
        {
            Class inetSocketAddressClass = Class
                    .forName("java.net.InetSocketAddress");
            Class socketAddressClass = Class.forName("java.net.SocketAddress");
            _socketTimeoutException = Class
                    .forName("java.net.SocketTimeoutException");

            _connectMethod = Socket.class.getDeclaredMethod("connect",
                                                            new Class[] {
        socketAddressClass, Integer.TYPE                   });

            _log.debug("1.4 connect method found");

            _inetSocketAddressCons = inetSocketAddressClass
                    .getConstructor(new Class[] { String.class, Integer.TYPE });
            _log.debug("1.4 InetSocketAddress constructor method found");

        }
        catch (NoSuchMethodException nsm)
        {
            _log.debug("1.4 method/constructor NOT found", nsm);
        }
        catch (ClassNotFoundException cnf)
        {
            _log.debug("1.4 InetSocketAddress class NOT found", cnf);
        }
        catch (SecurityException sex)
        {
            _log.debug("1.4 InetSocketAddress class (probably in applet)", sex);
        }

    }

    /**
     * Fully-specified constructor.
     * 
     * @param proxyHost
     *            the host I should proxy via
     * @param proxyPort
     *            the port I should proxy via
     * @param host
     *            the host I should connect to. Parameter value must be
     *            non-null.
     * @param port
     *            the port I should connect to
     * @param secure
     *            when <tt>true</tt>, connect via HTTPS (SSL)
     */
    public HttpConnection(String proxyHost,
            int proxyPort,
            String host,
            int port,
            boolean secure,
            int proxyIncarnation,
            String handle)
    {
        if (_log.isDebugEnabled())
        {
            _log.debug("HttpConnectionManager.getConnection:  creating "
                + " connection for "
                + host
                + ":"
                + port
                + " via "
                + proxyHost
                + ":"
                + proxyPort
                + " handle: "
                + handle);
        }

        if (host == null)
        {
            throw new NullPointerException("host parameter is null");
        }
        _proxyHost = proxyHost;
        _proxyPort = proxyPort;
        _host = host;
        _port = port;
        _ssl = secure;
        _proxyIncarnation = proxyIncarnation;
        _handle = handle;
        setHostPort();
    }

    // ------------------------------------------ Attribute Setters and Getters

    /**
     * Specifies an alternative factory for SSL sockets. If <code>factory</code>
     * is <code>null</code> the default implementation is used.
     * 
     * @param factory
     *            An instance of a SSLSocketFactory or <code>null</code>.
     * @throws IllegalStateException
     *             If called after the connection was opened
     */
    void setSSLSocketFactory(SSLSocketFactory factory)
    {
        assertNotOpen();
        _sslSocketFactory = factory;
    }

    void setHostnameVerifier(HostnameVerifier hostnameVerifier)
    {
        assertNotOpen();
        _hostnameVerifier = hostnameVerifier;
    }

    /**
     * Returns the cipher suite associated with this connection.
     */
    String getCipherSuite()
    {
        assertOpen();
        if (!(_socket instanceof SSLSocket))
        {
            throw new IllegalStateException("Called on a connection that is not an SSL connection");
        }

        return ((SSLSocket)_socket).getSession().getCipherSuite();
    }

    /**
     * Returns the certificates(s) that were sent to the server when the
     * connection was established.
     */
    Certificate[] getLocalCertificates()
    {
        assertOpen();
        if (!(_socket instanceof SSLSocket))
        {
            throw new IllegalStateException("Called on a connection that is not an SSL connection");
        }

        try
        {
            return (Certificate[])HttpURLConnection._sslGetLocalCertMethod
                    .invoke(((SSLSocket)_socket).getSession(), null);
        }
        catch (IllegalAccessException iae)
        {
            _log.error(iae);
            throw new RuntimeException("Unexpected exception: " + iae);

        }
        catch (InvocationTargetException ite)
        {
            Object targetException = ite.getTargetException();
            _log.error("Unexpected exception: ", (Throwable)targetException);
            // It had better be a RuntimeException
            throw (RuntimeException)targetException;
        }
    }

    /**
     * Returns the server's certificate chain that was established when the
     * session was setup.
     */
    Certificate[] getServerCertificates() throws SSLPeerUnverifiedException
    {
        assertOpen();
        if (!(_socket instanceof SSLSocket))
        {
            throw new IllegalStateException("Called on a connection that is not an SSL connection");
        }
        try
        {
            return (Certificate[])HttpURLConnection._sslGetServerCertMethod
                    .invoke(((SSLSocket)_socket).getSession(), null);
        }
        catch (IllegalAccessException iae)
        {
            _log.error(iae);
            throw new RuntimeException("Unexpected exception: " + iae);

        }
        catch (InvocationTargetException ite)
        {
            Object targetException = ite.getTargetException();
            _log.error("Unexpected exception: ", (Throwable)targetException);
            // It had better be a RuntimeException
            throw (RuntimeException)targetException;
        }
    }

    /**
     * Return my host.
     * 
     * @return my host.
     */
    public String getHost()
    {
        return _host;
    }

    /**
     * Set my host.
     * 
     * @param host
     *            the host I should connect to. Parameter value must be
     *            non-null.
     * @throws IllegalStateException
     *             if I am already connected
     */
    public void setHost(String host) throws IllegalStateException
    {
        if (host == null)
        {
            throw new NullPointerException("host parameter is null");
        }
        assertNotOpen();
        _host = host;
        setHostPort();
    }

    /**
     * Return my port.
     * 
     * If the port is -1 (or less than 0) the default port for the current
     * protocol is returned.
     * 
     * @return my port.
     */
    public int getPort()
    {
        if (_port < 0)
        {
            return isSecure() ? 443 : 80;
        }
        return _port;
    }

    /**
     * Set my port.
     * 
     * @param port
     *            the port I should connect to
     * @throws IllegalStateException
     *             if I am already connected
     */
    public void setPort(int port) throws IllegalStateException
    {
        assertNotOpen();
        _port = port;
        setHostPort();
    }

    private void setHostPort()
    {
        // Always have the port number
        _hostPort = _host + ":" + getPort();

        // Don't show the port on the URL if its the default
        if (_port < 0 || (_port == 80 && !_ssl) || (_port == 443 && _ssl))
            _hostPortURL = _host;
        else
            _hostPortURL = _host + ":" + _port;

    }

    public String getHostPortURL()
    {
        return _hostPortURL;
    }

    public String getHostPort()
    {
        return _hostPort;
    }

    public String getHandle()
    {
        return _handle;
    }

    public void setLastTimeUsed()
    {
        _lastTimeUsed = System.currentTimeMillis();
    }

    public long getLastTimeUsed()
    {
        return _lastTimeUsed;
    }

    public void setIdleTimeout(long ms)
    {
        _idleTimeout = ms;
    }

    public long getIdleTimeout()
    {
        return _idleTimeout;
    }

    public void setIdlePing(long ms)
    {
        _idlePing = ms;
    }

    public long getIdlePing()
    {
        return _idlePing;
    }

    public void setNtlmLeaveOpen(boolean req)
    {
        _ntlmLeaveOpen = req;
    }

    public boolean isNtlmLeaveOpen()
    {
        return _ntlmLeaveOpen;
    }

    // This is the start of the ping message; see the ping() method
    // Use a HEAD request because we are guaranteed never to get
    // a body so we don't need to pay attention to the content-length,
    // etc
    private static final byte[] PING_MSG       = "HEAD / HTTP/1.1\r\nHost: "
                                                       .getBytes();

    // "OPTIONS * HTTP/1.1\r\nHost: ".getBytes();

    private static final byte[] TERMINATOR     = "\r\n\r\n".getBytes();
    private static final int    TERMINATOR_LEN = TERMINATOR.length;

    private static final byte[] CONNECTION     = "connection".getBytes();
    private static final int    CONNECTION_LEN = CONNECTION.length;

    private static final byte[] CLOSED         = "closed".getBytes();
    private static final int    CLOSED_LEN     = CLOSED.length;

    private final int skipSpaces() throws IOException
    {
        int c = 0;
        do
        {
            c = _input.read();
            if (c < 0)
                throw new IOException("EOF during checkConnection");
        }
        while (c == ' ');
        c = Character.toLowerCase((char)c);
        return c;
    }

    /**
     * Pings the connection
     */
    public void checkConnection() throws IOException
    {
        // Don't try to ping a connection as the first
        // thing, only ping if it has been used
        if (_output == null
            || _lastTimeUsed == 0
            || _idlePing == 0
            || (System.currentTimeMillis() - _lastTimeUsed) <= _idlePing)
        {
            _log.trace("checkConnection - skipped");
            return;
        }

        _log.trace("checkConnection - doing ping");

        _output.write(PING_MSG);
        _output.write(_hostPortURL.getBytes());
        _output.write(Util.CRLF_BYTES);
        _output.write(Util.CRLF_BYTES);
        _output.flush();

        // Read until there are 2 crlfs, we assume there
        // is no message body
        int crlfInd = 0;
        int connectionInd = 0;
        while (true)
        {
            int c = _input.read();
            if (c < 0)
                throw new IOException("Idle Connection Ping failed");
            if (c == TERMINATOR[crlfInd])
            {
                crlfInd++;
                if (crlfInd >= TERMINATOR_LEN)
                    break;
            }
            else
            {
                crlfInd = 0;
            }

            // Look for connection: closed
            c = Character.toLowerCase((char)c);
            if (c == CONNECTION[connectionInd++])
            {
                if (connectionInd >= CONNECTION_LEN)
                {
                    c = skipSpaces();
                    if (c != ':')
                    {
                        connectionInd = 0;
                        continue;
                    }

                    c = skipSpaces();
                    int closedInd = 0;
                    while (c == CLOSED[closedInd++])
                    {
                        c = _input.read();
                        if (c < 0)
                        {
                            throw new IOException("EOF during checkConnection");
                        }
                        c = Character.toLowerCase((char)c);
                    }
                    if (closedInd >= CLOSED_LEN)
                    {
                        _log
                                .debug("Found closed connection on checkConnection");
                        throw new IOException("Connection: closed detected on checkConnection");
                    }
                    connectionInd = 0;
                }
            }
            else
            {
                connectionInd = 0;
            }
        }
        setLastTimeUsed();
    }

    /**
     * Return my proxy host.
     * 
     * @return my proxy host.
     */
    public String getProxyHost()
    {
        return _proxyHost;
    }

    /**
     * Set the host I should proxy through.
     * 
     * @param host
     *            the host I should proxy through.
     * @throws IllegalStateException
     *             if I am already connected
     */
    public void setProxyHost(String host) throws IllegalStateException
    {
        assertNotOpen();
        _proxyHost = host;
    }

    /**
     * Return my proxy port.
     * 
     * @return my proxy port.
     */
    public int getProxyPort()
    {
        return _proxyPort;
    }

    /**
     * Set the port I should proxy through.
     * 
     * @param port
     *            the host I should proxy through.
     * @throws IllegalStateException
     *             if I am already connected
     */
    public void setProxyPort(int port) throws IllegalStateException
    {
        assertNotOpen();
        _proxyPort = port;
    }

    /**
     * Return <tt>true</tt> if I will (or I am) connected over a secure
     * (HTTPS/SSL) protocol.
     * 
     * @return <tt>true</tt> if I will (or I am) connected over a secure
     *         (HTTPS/SSL) protocol.
     */
    public boolean isSecure()
    {
        return _ssl;
    }

    /**
     * Return the proxy incarnation number associated with this connection.
     */
    public int getProxyIncarnation()
    {
        return _proxyIncarnation;
    }

    /**
     * Get the protocol.
     * 
     * @return HTTPS if secure, HTTP otherwise
     */
    public String getProtocol()
    {
        return (isSecure() ? "HTTPS" : "HTTP");
    }

    /**
     * Set whether or not I should connect over HTTPS (SSL).
     * 
     * @param secure
     *            whether or not I should connect over HTTPS (SSL).
     * @throws IllegalStateException
     *             if I am already connected
     */
    public void setSecure(boolean secure) throws IllegalStateException
    {
        assertNotOpen();
        _ssl = secure;
    }

    /**
     * Return <tt>true</tt> if I am connected, <tt>false</tt> otherwise.
     * 
     * @return <tt>true</tt> if I am connected
     */
    public boolean isOpen()
    {
        return _open;
    }

    /**
     * Return <tt>true</tt> if I am (or I will be) connected via a proxy,
     * <tt>false</tt> otherwise.
     * 
     * @return <tt>true</tt> if I am (or I will be) connected via a proxy,
     *         <tt>false</tt> otherwise.
     */
    public boolean isProxied()
    {
        return (_proxyHost != null);
    }

    // --------------------------------------------------- Other Public Methods

    /**
     * Set my {@link Socket}'s timeout, via {@link Socket#setSoTimeout}. If
     * the connection is already open, the SO_TIMEOUT is changed. If no
     * connection is open, then subsequent connections will use the timeout
     * value. This timeout controls the time to wait for read operations.
     * 
     * @param timeout
     *            the timeout value
     * @throws SocketException -
     *             if there is an error in the underlying protocol, such as a
     *             TCP error.
     * @throws IllegalStateException
     *             if I am not connected
     */
    public void setSoTimeout(int timeout)
        throws SocketException,
            IllegalStateException
    {
        if (_log.isDebugEnabled())
            _log.debug("setSoTimeout(" + timeout + ")");
        _so_timeout = timeout;
        if (_socket != null)
        {
            _socket.setSoTimeout(timeout);
        }
    }

    public void setConnectTimeout(int timeout)
        throws SocketException,
            IllegalStateException
    {
        if (_log.isDebugEnabled())
            _log.debug("setConnectTimeout(" + timeout + ")");
        _connectTimeout = timeout;
    }

    /**
     * Open this connection to the current host and port (via a proxy if so
     * configured).
     * 
     * @throws IOException
     *             when there are errors opening the connection
     */
    public void open() throws IOException
    {
        _log.trace("open");

        assertNotOpen();
        try
        {
            if (null == _socket)
            {
                // Do the async open if we need a timeout and don't have
                // the timeout connect method available
                if (_connectTimeout > 0 && _connectMethod == null)
                    asyncOpen();
                else
                    normalOpen();
            }
        }
        catch (IOException e)
        {
            // Connection wasn't opened properly
            // so close everything out
            close();
            throw e;
        }
    }

    void openSocket() throws IOException
    {
        String host = (null == _proxyHost) ? _host : _proxyHost;
        int port = (null == _proxyHost) ? _port : _proxyPort;

        host = InetAddress.getByName(host).getHostAddress();

        // Use the connect method with the timeout if its available
        if (_so_timeout > 0 && _connectMethod != null)
        {
            if (_log.isDebugEnabled())
                _log.debug("using 1.4+ connect method with timeout");
            _socket = new Socket();
            try
            {
                Object sa = _inetSocketAddressCons.newInstance(new Object[] {
                    host, new Integer(port) });
                _connectMethod.invoke(_socket, new Object[] { sa,
                    new Integer(_connectTimeout) });
            }
            catch (InstantiationException ie)
            {
                _log.error(ie);
                throw new IOException("Unexpected exception: " + ie);

            }
            catch (IllegalAccessException iae)
            {
                _log.error(iae);
                throw new IOException("Unexpected exception: " + iae);

            }
            catch (InvocationTargetException ite)
            {
                Object targetException = ite.getTargetException();

                if (_socketTimeoutException.isAssignableFrom(targetException
                        .getClass()))
                {
                    _log.debug("Connection timeout after (ms): "
                        + _connectTimeout);
                    throw new HttpTimeoutException();
                }

                if (targetException instanceof IOException)
                    throw (IOException)targetException;
                if (targetException instanceof RuntimeException)
                    throw (RuntimeException)targetException;
                _log.error(targetException);
                throw new IOException("Unexpected exception: "
                    + targetException);
            }

        }
        else
        {
            _socket = new Socket(host, port);
        }

        createStreams();
    }

    private final void normalOpen() throws IOException
    {
        try
        {
            // To simulate a connection timeout
            if (_testTimeout > 0)
            {
                _log.info("Simulating test timeout: " + _testTimeout);
                Thread.sleep(_testTimeout);
                return;
            }
        }
        catch (InterruptedException ex)
        {
            // Will happen when the timeout occurs
            _log.info("test timeout interrupted: " + hashCode());
            return;
        }

        openSocket();

        // Make a tunnel
        if (isSecure())
        {
            if (isProxied())
            {
                _log.debug("creating tunnel");

                _tunnelCon = new HttpURLConnectInternal();

                // Set this connection to open so that the
                // URL connection does not try to open it
                _tunnelCon
                        .setRequestMethodInternal(HttpURLConnection.HTTP_METHOD_CONNECT);
                _tunnelCon.setConnection(this, !HttpURLConnection.RELEASE);

                // This handles any authentication that's required
                _tunnelCon.execute();
                _tunnelEstablished = true;
            }

            // Switch socket to SSL
            if (_log.isDebugEnabled())
                _log.debug("switching to ssl: " + getHostPort());

            if (_sslSocketFactory == null)
            {
                _log.error("SSLSocketFactory not specified");
                throw new IOException("SSLSocketFactory not specified");
            }

            _socket = _sslSocketFactory.createSocket(_socket,
                                                     _host,
                                                     _port,
                                                     true);
            if (_log.isDebugEnabled())
            {
                _log.debug("created socket: " + _socket);
            }

            if (_socket == null)
            {
                throw new IOException("SSLSocketFactory: "
                    + _sslSocketFactory
                    + " ("
                    + _sslSocketFactory.getClass()
                    + ") returned null for host: "
                    + _host
                    + " port: "
                    + _port);

            }

            SSLSocket sslSocket = (SSLSocket)_socket;

            if (_log.isDebugEnabled())
            {
                _log.debug("socket session: " + sslSocket.getSession());
            }

            if (sslSocket.getSession() == null)
            {
                throw new IOException("SSLSocketFactory: "
                    + _sslSocketFactory
                    + " ("
                    + _sslSocketFactory.getClass()
                    + ") getSession() returned null host: "
                    + _host
                    + " port: "
                    + _port);

            }

            if (sslSocket.getSession().getPeerCertificateChain() == null)
            {
                throw new IOException("SSLSocketFactory: "
                    + _sslSocketFactory
                    + " ("
                    + _sslSocketFactory.getClass()
                    + ") getPeerCertificationChain() returned null"
                    + " for host: "
                    + _host
                    + " port: "
                    + _port);

            }

            checkCertificate(sslSocket.getSession().getPeerCertificateChain()[0],
                             _host);
        }

        createStreams();

        synchronized (this)
        {
            _open = true;
        }

        if (_log.isDebugEnabled())
        {
            _log
                    .debug("opened connection to: "
                        + this
                        + " (socket) "
                        + _socket);
        }
    }

    private final void checkCertificate(X509Certificate cert, String hostName)
        throws IOException
    {
        String certHost;

        try
        {
            Principal dn = cert.getSubjectDN();
            String dnStr = dn.getName();

            int cnStart = dnStr.indexOf("CN=");
            if (cnStart == -1)
            {
                _log.error("Common name (CN) not found in: " + dnStr);
                // allow the connection
                return;
            }

            // Start after the CN=
            cnStart += 3;

            int cnEnd = dnStr.indexOf(",", cnStart);
            if (cnEnd > -1)
                certHost = dnStr.substring(cnStart, cnEnd);
            else
                certHost = dnStr.substring(cnStart);
            certHost = certHost.toLowerCase();

            if (_log.isDebugEnabled())
            {
                _log.debug("certHost: "
                    + certHost
                    + " prin name: "
                    + dn.getName());
            }
        }

        catch (Exception ex)
        {
            _log.error("Exception when getting cert name: ", ex);
            // allow the connection
            return;
        }

        // For testing purposes
        if (_testNonMatchHost)
            hostName = hostName + "_nonmatch";

        boolean result = false;
        if (!certHost.equals(hostName.toLowerCase())
            && _hostnameVerifier != null)
        {
            _log
                    .info("Cert host: "
                        + certHost
                        + " does not match: "
                        + hostName);
            SSLSession sess = ((SSLSocket)_socket).getSession();
            // if (_hostnameVerifier instanceof HostnameVerifier)
            // {
            try
            {
                _log.debug("Calling oaklandsw verifier");
                result = _hostnameVerifier.verify(hostName, sess);
            }
            catch (Exception ex)
            {
                _log.error("Unexpected exception from verifier: ", ex);
                throw new IOException("Unexpected exception from verifier: "
                    + ex);
            }
            /*
             * Don't need this for now } else { _log.debug("Calling 1.4
             * verifier"); Boolean r; try {
             * 
             * r = (Boolean)HttpURLConnection._hostnameVerifierMethod.
             * invoke(_hostnameVerifier, new Object[] { hostName, sess } ); }
             * catch (IllegalAccessException ex) { _log.error("Unexpected
             * exception: " + ex); throw new IOException("Unexpected exception
             * during cert verify: " + ex); } catch (InvocationTargetException
             * itex) { _log.error("Unexpected exception: " + itex); throw new
             * IOException("Unexpected exception during cert verify: " + itex); }
             * result = r.booleanValue(); }
             */

            if (!result)
            {
                _log.debug("verifier failed");
                throw new IOException("Connection rejected due to failure of hostname verification");
            }
            _log.debug("verifier passed");

        }
    }

    private final void normalAsyncWrapper()
    {
        try
        {
            _log.debug("Attempting async open");
            normalOpen();
        }
        catch (Exception ex)
        {
            _log.info("Async open exception: ", ex);
            // Save the problem so we can throw on the
            // requesting thread
            synchronized (this)
            {
                _openException = ex;
                // Indicate completion here in case the thread
                // was interrupted
                _asyncOpenCompleted = true;
            }
        }

        synchronized (this)
        {
            _log.debug("Async open - doing notify to: " + hashCode());
            _asyncOpenCompleted = true;
            notify();
        }
    }

    // Used when we have to do the timeout by hand, spins
    // a thread to do the open and waits for the thread to
    // either notify us (and open or fail), or it times out
    private final void asyncOpen() throws IOException
    {
        final HttpConnection thisObject = this;

        Thread openThread = new Thread()
        {
            HttpConnection _connection = thisObject;

            public void run()
            {
                _connection.normalAsyncWrapper();
            }
        };

        openThread.start();
        // Thread.yield();

        try
        {
            synchronized (this)
            {
                // Note that _open might be set before the socket
                // is entirely open in the case of SSL, but we
                // have established the connection and that's
                // the main point, so we don't want to be waiting
                if (!_open && _openException == null)
                {

                    if (_connectTimeout > 0)
                    {
                        long timeoutEnd = System.currentTimeMillis()
                            + _connectTimeout;

                        _log.debug("Waiting for open time: "
                            + _connectTimeout
                            + " ("
                            + timeoutEnd
                            + ") on "
                            + hashCode());

                        // Protected against spurious notify
                        while (!_asyncOpenCompleted
                            && System.currentTimeMillis() < timeoutEnd)
                        {
                            if (!_asyncOpenCompleted)
                            {
                                _log.debug("Spurious notify ("
                                    + System.currentTimeMillis()
                                    + ") on "
                                    + hashCode());
                            }
                            wait(_connectTimeout);
                        }
                    }
                    else
                    {
                        _log.debug("Waiting for open on " + hashCode());

                        // Protected against spurious notify
                        while (!_asyncOpenCompleted)
                        {
                            if (!_asyncOpenCompleted)
                                _log.debug("Spurious notify on " + hashCode());
                            wait();
                        }
                    }

                    // Needed only for testing, to make sure these do not
                    // accumulate
                    if (_testTimeout > 0)
                        openThread.interrupt();
                }

                if (_log.isDebugEnabled())
                    _log.debug("Done with wait on " + hashCode());
                if (!_open)
                {
                    // The open failed, but did not timeout
                    if (_openException != null)
                        throw (IOException)_openException;

                    _log.debug("Connection TIMEOUT after (ms): "
                        + _connectTimeout);
                    throw new HttpTimeoutException();
                }
                if (_log.isDebugEnabled())
                    _log.debug("OPENED " + hashCode());
            }

        }
        catch (InterruptedException ex)
        {
            _log.error("Unexpected thread interruption: ", ex);
        }

    }

    private void createStreams() throws IOException, SocketException
    {
        InputStream is = _socket.getInputStream();
        OutputStream os = _socket.getOutputStream();

        if (_wireLog.isDebugEnabled())
        {
            _wireLog.debug("Enabling wire tracing");
            _input = new WireLogInputStream(is);
            _output = new WireLogOutputStream(os);
        }
        else
        {
            _input = new BufferedInputStream(is, STREAM_BUFFER_SIZE);
            _output = new BufferedOutputStream(os, STREAM_BUFFER_SIZE);
        }

        _socket.setTcpNoDelay(true);
        _socket.setSoTimeout(_so_timeout);
    }

    /**
     * Indicates if the connection is completely transparent from end to end.
     * 
     * @return true if conncetion is not proxied or tunneled through a
     *         transparent proxy; false otherwise.
     */
    public boolean isTransparent()
    {
        return !isProxied() || _tunnelEstablished;
    }

    public BufferedOutputStream getOutputStream()
        throws IOException,
            IllegalStateException
    {
        _log.trace("getOutputStream");
        if (_output == null)
            throw new IllegalStateException("Connection is not open");
        return _output;
    }

    public BufferedInputStream getInputStream()
        throws IOException,
            IllegalStateException
    {
        _log.trace("getInputStream");
        if (_input == null)
            throw new IllegalStateException("Connection is not open");
        return _input;
    }

    /**
     * Close my socket and streams.
     */
    public void close()
    {
        if (_log.isDebugEnabled())
            _log.debug("CLOSE " + this);

        _open = false;
        _tunnelEstablished = false;

        if (null != _input)
        {
            try
            {
                _input.close();
            }
            catch (Exception ex)
            {
                _log.warn("Exception caught when closing input", ex);
                // ignored
            }
            _input = null;
        }

        if (null != _output)
        {
            try
            {
                _output.close();
            }
            catch (Exception ex)
            {
                _log.debug("Exception caught when closing output", ex);
                // ignored
            }
            _output = null;
        }

        if (null != _socket)
        {
            try
            {
                _socket.close();
            }
            catch (Exception ex)
            {
                _log.debug("Exception caught when closing socket", ex);
                // ignored
            }
            _socket = null;
        }
    }

    /**
     * Throw an {@link IllegalStateException}if I am connected.
     * 
     * @throws IllegalStateException
     *             if connected
     */
    protected void assertNotOpen() throws IllegalStateException
    {
        if (_open)
        {
            throw new IllegalStateException("Connection is open");
        }
    }

    /**
     * Throw an {@link IllegalStateException}if I am not connected.
     * 
     * @throws IllegalStateException
     *             if not connected
     */
    public void assertOpen() throws IllegalStateException
    {
        if (!_open)
        {
            throw new IllegalStateException("Connection is not open");
        }

        // Make sure we can access the socket, if we can't its a bug
        try
        {
            int dummy = _socket.getSoTimeout();
            // To avoid compiler warning
            dummy = dummy + 1;
        }
        catch (Exception ex)
        {
            throw new IllegalStateException("Connection's socket is not open");
        }
    }

    public String toString()
    {
        // return "Conn: " + _hostPort;
        return "Conn: "
            + _hostPort
            + ((_socket != null) ? " local: "
                + Integer.toString(_socket.getLocalPort()) : "")
            + " ("
            + System.identityHashCode(this)
            + ")";
    }

}
