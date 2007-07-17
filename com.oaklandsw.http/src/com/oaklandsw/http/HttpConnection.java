//
// Some portions copyright 2002-2007, oakland software, all rights reserved.
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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.Principal;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.security.cert.X509Certificate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.oaklandsw.util.ExposedBufferInputStream;
import com.oaklandsw.util.LogUtils;
import com.oaklandsw.util.Util;

import edu.emory.mathcs.backport.java.util.concurrent.ArrayBlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.BlockingQueue;

public class HttpConnection
{
    public static final String WIRE_LOG           = LogUtils.LOG_PREFIX
                                                      + LogUtils.LOG_WIRE_PREFIX;
    public static final String CONN_LOG           = LogUtils.LOG_PREFIX
                                                      + LogUtils.LOG_CONN_PREFIX;

    private static final Log   _log               = LogUtils.makeLogger();

    private static final Log   _wireLog           = LogFactory.getLog(WIRE_LOG);
    private static final Log   _connLog           = LogFactory.getLog(CONN_LOG);

    private static final int   STREAM_BUFFER_SIZE = 16384;

    public String              _host;
    public int                 _port              = -1;

    /**
     * The host/port string to use when part of a URL. This has the port number
     * omitted the port is the default port for the protcol.
     */
    String                     _hostPortURL;

    /**
     * The host/port string to use to find the connection with the connection
     * manager. This always includes the port number.
     */
    String                     _hostPort;

    // The way this connection is idenfified by the connection
    // manager.
    String                     _handle;

    // Information about the host/port etc that controls this connection
    ConnectionInfo             _connectionInfo;

    HttpConnectionManager      _connManager;

    HttpConnectionThread       _connectionThread;

    String                     _proxyHost;
    int                        _proxyPort         = -1;

    Socket                     _socket;

    ExposedBufferInputStream   _input;
    BufferedOutputStream       _output;

    static final int           CS_VIRGIN          = 0;
    static final int           CS_OPEN            = 1;
    static final int           CS_CLOSED          = 2;

    int                        _state;
    // boolean _open;

    boolean                    _ssl;

    int                        _soTimeout;
    int                        _connectTimeout;

    /** An alternative factory for SSL sockets to use */
    SSLSocketFactory           _sslSocketFactory;

    HostnameVerifier           _hostnameVerifier;

    boolean                    _tunnelEstablished;

    HttpURLConnectInternal     _tunnelCon;

    long                       _lastTimeUsed;
    long                       _idleTimeout;
    long                       _idlePing;

    // Used only for testing to simulate a connection that fails to
    // connect
    public static long         _testTimeout;

    // For SSL hostname matching testing
    public static boolean      _testNonMatchHost;

    private Exception          _openException;

    // Used to keep track of the global proxy state at the time
    // this connection was created.
    int                        _proxyIncarnation;

    // The number of urlcons with outstanding pipelined writes
    // against this connection. This is used to avoid assigning
    // this connection to a non-pipelined urlcon, this is also used
    // to manage the pipeline depth
    int                        _pipelineUrlConCount;

    // High water mark for the above count
    int                        _pipelineUrlConHigh;

    // The total number of urlcons written to this connection
    int                        _totalReqUrlConCount;

    // The total number of urlcons responded on this connection
    int                        _totalResUrlConCount;

    // The threads accessing a connection now. In general,
    // we don't allow a connection to close if someone can access a stream
    // associated with the connection since we don't want the stream
    // to close out from under a user (by another thread). This prevents
    // things like NPEs when accessing the streams. This is synchronized
    // using this object.
    List                       _preventCloseList;

    // Queue for pipelining urlcons to be serviced
    BlockingQueue              _queue;

    // Credential associated with this connection if the authentication
    // is session-based.
    UserCredential[]           _credential        = new UserCredential[HttpURLConnection.AUTH_PROXY + 1];

    // The authentication protocol associated with the above credential
    // Initially set to -1 meaning unknown. Protocol zero means no
    // authentication is done on the connection
    int[]                      _authProtocol      = new int[] { -1, -1 };

    /**
     * Indicates that NTLM authentication is being used on this connection. This
     * is used by HTTP 1.0 connections to avoid closing the connection since it
     * must be kept open for NTLM to function properly, and we can't send the
     * "Connection: Keep-Alive" to a proxy.
     */
    boolean                    _ntlmLeaveOpen;

    // True when this connection is released back to the connection manager,
    // used to detect a double release
    boolean                    _released;

    // The current thread reading from this connection
    Thread                     _bugCatchReadThread;
    // Use a count to allow the bug check calls to be nested
    int                        _bugCatchCount;

    static String stateToString(int state)
    {
        switch (state)
        {
            case CS_VIRGIN:
                return "CS_VIRGIN";
            case CS_OPEN:
                return "CS_OPEN";
            case CS_CLOSED:
                return "CS_CLOSED";
        }
        Util.impossible("Unknown connection state: " + state);
        return null;
    }

    void startReadBugCheck()
    {
        if (_bugCatchReadThread != null
            && _bugCatchReadThread != Thread.currentThread())
        {
            Util.impossible("Attempting to read on "
                + "connection "
                + this
                + " already reading from: "
                + _bugCatchReadThread
                + " from thread: "
                + Thread.currentThread());
        }

        // Count is only incremented on a nested call
        if (_bugCatchReadThread == Thread.currentThread())
            _bugCatchCount++;

        _bugCatchReadThread = Thread.currentThread();
    }

    void endReadBugCheck()
    {
        if (_bugCatchReadThread == null
            || _bugCatchReadThread != Thread.currentThread())
        {
            Util.impossible("Error calling endReadBugCheck(): "
                + _bugCatchReadThread
                + " from thread: "
                + Thread.currentThread());
        }

        if (_bugCatchCount > 0)
        {
            _bugCatchCount--;
            return;
        }
        _bugCatchReadThread = null;
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
        if (_connLog.isDebugEnabled())
        {
            _connLog.debug("constructor for "
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
        _queue = new ArrayBlockingQueue(200);
        _preventCloseList = new ArrayList();
    }

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
            _connLog.error(iae);
            throw new RuntimeException("Unexpected exception: " + iae);

        }
        catch (InvocationTargetException ite)
        {
            Object targetException = ite.getTargetException();
            _connLog
                    .error("Unexpected exception: ", (Throwable)targetException);
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
            _connLog.error(iae);
            throw new RuntimeException("Unexpected exception: " + iae);

        }
        catch (InvocationTargetException ite)
        {
            Object targetException = ite.getTargetException();
            _connLog
                    .error("Unexpected exception: ", (Throwable)targetException);
            // It had better be a RuntimeException
            throw (RuntimeException)targetException;
        }
    }

    void setHost(String host) throws IllegalStateException
    {
        if (host == null)
        {
            throw new NullPointerException("host parameter is null");
        }
        assertNotOpen();
        _host = host;
        setHostPort();
    }

    public int getPort()
    {
        if (_port < 0)
        {
            return isSecure() ? 443 : 80;
        }
        return _port;
    }

    void setPort(int port) throws IllegalStateException
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

    void setLastTimeUsed()
    {
        _lastTimeUsed = System.currentTimeMillis();
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

    void setCredentialSent(int authType, int normalOrProxy, UserCredential cred)
    {
        if (_connLog.isDebugEnabled())
        {
            _connLog.debug("Credential sent authType: "
                + HttpURLConnectInternal.authTypeToString(authType)
                + " cred: "
                + cred
                + " proxy: "
                + HttpURLConnection.normalOrProxyToString(normalOrProxy));
        }

        _credential[normalOrProxy] = cred;
        _authProtocol[normalOrProxy] = authType;
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

        _connLog.trace("checkConnection - doing ping");

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
                        _connLog
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

    public String getProxyHost()
    {
        return _proxyHost;
    }

    public void setProxyHost(String host) throws IllegalStateException
    {
        assertNotOpen();
        _proxyHost = host;
    }

    public int getProxyPort()
    {
        return _proxyPort;
    }

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
        if (_soTimeout == timeout)
            return;

        _soTimeout = timeout;

        if (_connLog.isDebugEnabled())
            _connLog.debug("setSoTimeout(" + timeout + ")");
        if (_socket != null)
        {
            _socket.setSoTimeout(timeout);
        }
    }

    public void setConnectTimeout(int timeout)
        throws SocketException,
            IllegalStateException
    {
        if (_connLog.isDebugEnabled())
            _connLog.debug("setConnectTimeout(" + timeout + ")");
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
        // Lock to synchronize with close
        synchronized (_connManager)
        {
            if (_state != CS_VIRGIN)
            {
                throw new IOException("Cannot be opened because state is: "
                    + stateToString(_state));
            }
        }

        _connLog.trace("open");
        if (null == _socket)
            normalOpen();
    }

    void openSocket() throws IOException
    {
        String host = (null == _proxyHost) ? _host : _proxyHost;
        int port = (null == _proxyHost) ? _port : _proxyPort;

        host = InetAddress.getByName(host).getHostAddress();

        // Use the connect method with the timeout if its available
        if (_soTimeout > 0)
        {
            _socket = new Socket();
            try
            {
                _socket.connect(new InetSocketAddress(host, port),
                                _connectTimeout);
            }
            catch (SocketTimeoutException tex)
            {
                _connLog.debug("Connection timeout after (ms): "
                    + _connectTimeout);
                throw new HttpTimeoutException();
            }

            catch (Exception ex)
            {
                if (ex instanceof IOException)
                    throw (IOException)ex;
                if (ex instanceof RuntimeException)
                    throw (RuntimeException)ex;
                _connLog.error(ex);
                throw new IOException("Unexpected exception: " + ex);
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
                _connLog.info("Simulating test timeout: " + _testTimeout);
                Thread.sleep(_testTimeout);
                return;
            }
        }
        catch (InterruptedException ex)
        {
            // Will happen when the timeout occurs
            _connLog.info("test timeout interrupted: " + hashCode());
            return;
        }

        openSocket();

        // Make a tunnel
        if (isSecure())
        {
            if (isProxied())
            {
                _connLog.debug("creating tunnel");

                _tunnelCon = new HttpURLConnectInternal();

                // Set this connection to open so that the
                // URL connection does not try to open it
                _tunnelCon
                        .setRequestMethodInternal(HttpURLConnection.HTTP_METHOD_CONNECT);
                _tunnelCon.setConnection(this, !HttpURLConnection.RELEASE);

                // Set open to allow the checking in the urlCon to work
                _state = CS_OPEN;
                // This handles any authentication that's required
                _tunnelCon.execute();
                // Will set open for real later
                _state = CS_VIRGIN;
                _tunnelEstablished = true;
            }

            // Switch socket to SSL
            if (_connLog.isDebugEnabled())
                _connLog.debug("switching to ssl: " + _hostPort);

            if (_sslSocketFactory == null)
            {
                _connLog.error("SSLSocketFactory not specified");
                throw new IOException("SSLSocketFactory not specified");
            }

            _socket = _sslSocketFactory.createSocket(_socket,
                                                     _host,
                                                     _port,
                                                     true);
            if (_connLog.isDebugEnabled())
            {
                _connLog.debug("created socket: " + _socket);
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

            if (_connLog.isDebugEnabled())
            {
                _connLog.debug("socket session: " + sslSocket.getSession());
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

        synchronized (_connManager)
        {
            _state = CS_OPEN;
        }

        if (_connLog.isDebugEnabled())
        {
            _connLog.debug("opened connection to: "
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
                _connLog.error("Common name (CN) not found in: " + dnStr);
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

            if (_connLog.isDebugEnabled())
            {
                _connLog.debug("certHost: "
                    + certHost
                    + " prin name: "
                    + dn.getName());
            }
        }

        catch (Exception ex)
        {
            _connLog.error("Exception when getting cert name: ", ex);
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
            _connLog.info("Cert host: "
                + certHost
                + " does not match: "
                + hostName);
            SSLSession sess = ((SSLSocket)_socket).getSession();
            // if (_hostnameVerifier instanceof HostnameVerifier)
            // {
            try
            {
                _connLog.debug("Calling oaklandsw verifier");
                result = _hostnameVerifier.verify(hostName, sess);
            }
            catch (Exception ex)
            {
                _connLog.error("Unexpected exception from verifier: ", ex);
                throw new IOException("Unexpected exception from verifier: "
                    + ex);
            }
            /*
             * Don't need this for now } else { _connLog.debug("Calling 1.4
             * verifier"); Boolean r; try {
             * 
             * r = (Boolean)HttpURLConnection._hostnameVerifierMethod.
             * invoke(_hostnameVerifier, new Object[] { hostName, sess } ); }
             * catch (IllegalAccessException ex) { _connLog.error("Unexpected
             * exception: " + ex); throw new IOException("Unexpected exception
             * during cert verify: " + ex); } catch (InvocationTargetException
             * itex) { _connLog.error("Unexpected exception: " + itex); throw
             * new IOException("Unexpected exception during cert verify: " +
             * itex); } result = r.booleanValue(); }
             */

            if (!result)
            {
                _connLog.debug("verifier failed");
                throw new IOException("Connection rejected due to failure of hostname verification");
            }
            _connLog.debug("verifier passed");

        }
    }

    private void createStreams() throws IOException, SocketException
    {
        InputStream is = _socket.getInputStream();
        OutputStream os = _socket.getOutputStream();

        if (_wireLog.isDebugEnabled())
        {
            _wireLog.debug("Enabling wire tracing");
            is = new WireLogInputStream(is);
            os = new WireLogOutputStream(os);
        }

        _input = new ExposedBufferInputStream(is, STREAM_BUFFER_SIZE);
        _output = new BufferedOutputStream(os, STREAM_BUFFER_SIZE);

        _socket.setSoLinger(false, 0);
        _socket.setTcpNoDelay(true);
        _socket.setSoTimeout(_soTimeout);
    }

    /**
     * Indicates if the connection is completely transparent from end to end.
     * 
     * @return true if connection is not proxied or tunneled through a
     *         transparent proxy; false otherwise.
     */
    public boolean isTransparent()
    {
        return !isProxied() || _tunnelEstablished;
    }

    public BufferedOutputStream getOutputStream() throws IOException
    {
        _connLog.trace("getOutputStream");
        if (_output == null)
            throw new IOException("Connection is not open");
        return _output;
    }

    public ExposedBufferInputStream getInputStream() throws IOException
    {
        _connLog.trace("getInputStream");
        if (_input == null)
            throw new IOException("Connection is not open");
        return _input;
    }

    void adjustPipelinedUrlConCount(int amount)
    {
        synchronized (this)
        {
            _pipelineUrlConCount += amount;
            if (_log.isDebugEnabled())
            {
                _log.debug("adjustUrlConCount: "
                    + this
                    + " count: "
                    + _pipelineUrlConCount
                    + " amount: "
                    + amount);
            }

            if (_pipelineUrlConCount < 0)
            {
                Util.impossible("_pipelingUrlConCount underflow: "
                    + _pipelineUrlConCount
                    + " delta: "
                    + amount);
            }

            if (_pipelineUrlConCount > _pipelineUrlConHigh)
            {
                _pipelineUrlConHigh = _pipelineUrlConCount;
                if (_pipelineUrlConHigh > _connManager._requestCounts[HttpConnectionManager.COUNT_PIPELINE_DEPTH_HIGH])
                {
                    _connManager._requestCounts[HttpConnectionManager.COUNT_PIPELINE_DEPTH_HIGH] = _pipelineUrlConHigh;
                }
            }
        }

        // If we are freeing a pipeling slot, poke the CM so that
        // anyone waiting on pipeline depth to be available will
        // be woken
        if (amount < 0)
            _connManager.pipelineReadCompleted();

    }

    int getPipelinedUrlConCount()
    {
        synchronized (this)
        {
            return _pipelineUrlConCount;
        }
    }

    //
    // In the locking for the close wait stuff, it can be
    // called from inside of the connection manager, so
    // nothing should be called (in the CM or anything that calls the CM) while
    // this close lock (this object) is locked.

    void startPreventClose() throws IOException
    {
        synchronized (this)
        {
            _preventCloseList.add(Thread.currentThread());

            if (!isOpen())
            {
                throw new IOException("Connection: " + this + " closed");
            }

        }
    }

    void endPreventClose()
    {
        synchronized (this)
        {
            // Could happen because we already removed ourselve when
            // closing
            if (_preventCloseList.size() == 0)
                return;

            // This thread may not be found (because it already closed
            // the connection), that's harmless
            _preventCloseList.remove(Thread.currentThread());
            if (_preventCloseList.size() == 0)
                notifyAll();
        }
    }

    public void close()
    {
        close(!IMMEDIATE);
    }

    static final boolean IMMEDIATE = true;

    /**
     * Low-level close of this connection, releases the socket and stream
     * resources. Specify immediate if you can't wait.
     */
    void close(boolean immediate)
    {
        if (_connLog.isDebugEnabled())
            _connLog.debug("close " + this + " immediate: " + immediate);

        // This thread is allowed to close
        endPreventClose();

        // If there is another thread accessing the connection
        // we have to wait
        synchronized (this)
        {
            while (_preventCloseList.size() > 0)
            {
                try
                {
                    _connLog.debug("Waiting to close");
                    wait();
                }
                catch (InterruptedException e)
                {
                    _connLog.warn("Closing thread interrupted");
                    return;
                }
            }

            if (immediate)
            {
                if (_connectionThread != null)
                    _connectionThread.interrupt();
                // Clear out the queue since we don't guarantee
                // notifications in this case
                _queue.clear();

                if (_connLog.isDebugEnabled())
                    _connLog.debug("close - finished interrupt of conn thread");
            }

            _tunnelEstablished = false;
            _state = CS_CLOSED;
            _connLog.debug("Completing close: \n" + dump(0));

            if (null != _input)
            {
                try
                {
                    _input.close();
                }
                catch (Exception ex)
                {
                    _connLog.warn("Exception caught when closing input", ex);
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
                    _connLog.debug("Exception caught when closing output", ex);
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
                    _connLog.debug("Exception caught when closing socket", ex);
                    // ignored
                }
                _socket = null;
            }
        }

        if (_connLog.isDebugEnabled())
            _connLog.debug("close " + this + " FINISHED");
    }

    boolean isOpen()
    {
        return _state == CS_OPEN;
    }

    protected void assertNotOpen() throws IllegalStateException
    {
        if (_state == CS_OPEN)
        {
            throw new IllegalStateException("Connection is open");
        }
    }

    public void assertOpen() throws IllegalStateException
    {
        if (_state != CS_OPEN)
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

    public int hashCode()
    {
        return System.identityHashCode(this);
    }

    public boolean equals(Object other)
    {
        return this == other;
    }

    public String toString()
    {
        // Save local copy to avoid an NPE if the socket closes
        // in the middle of this
        Socket socket = _socket;

        StringBuffer sb = new StringBuffer();
        sb.append(Util.id(this) + " ");
        sb.append(_hostPort);
        sb.append(" ");
        if (socket != null)
            sb.append("(" + socket.getLocalPort() + ") ");
        sb.append(stateToString(_state) + " ");
        if (_authProtocol[HttpURLConnection.AUTH_NORMAL] > 0)
        {
            sb.append("(auth: "
                + _authProtocol[HttpURLConnection.AUTH_NORMAL]
                + ") ");
        }
        if (_authProtocol[HttpURLConnection.AUTH_PROXY] > 0)
        {
            sb.append("(pxauth: "
                + _authProtocol[HttpURLConnection.AUTH_PROXY]
                + ") ");
        }
        if (_proxyHost != null)
        {
            sb.append("px(" + _proxyHost);
            if (_proxyPort != -1)
                sb.append(":" + _proxyPort);
            sb.append(")");
        }
        return sb.toString();
    }

    public String dump(int indent)
    {
        StringBuffer sb = new StringBuffer();

        sb.append(Util.indent(indent));
        sb.append("Total requested urlcons:      "
            + _totalReqUrlConCount
            + "\n");
        sb.append(Util.indent(indent));
        sb.append("Total responded urlcons:      "
            + _totalResUrlConCount
            + "\n");
        sb.append(Util.indent(indent));
        sb.append("Current pipelined urlcons:    "
            + _pipelineUrlConCount
            + "\n");
        sb.append(Util.indent(indent));
        sb
                .append("High water pipelined urlcons: "
                    + _pipelineUrlConHigh
                    + "\n");
        return sb.toString();
    }

}
