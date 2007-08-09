//
// Copyright 2002-2006, oakland software, all rights reserved.
//
// May not be used or redistributed without specific written
// permission from oakland software.
//
package com.oaklandsw.http;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.logging.Log;

import com.oaklandsw.http.cookie.CookieSpec;
import com.oaklandsw.util.ExposedBufferInputStream;
import com.oaklandsw.util.LogUtils;
import com.oaklandsw.util.Util;
import com.oaklandsw.util.Util14Controller;

/**
 * A URLConnection with support for HTTP-specific features.
 * <p>
 * The following properties can be used with this class:
 * <p>
 * <code>http.proxyHost</code>- specifies the host of the proxy server. Note
 * this is identical to the method to specify the proxy host for the
 * java.net.HttpURLConnection. See setProxyHost().
 * <p>
 * <code>http.proxyPort</code>- specifies the port of the proxy server. Note
 * this is identical to the method to specify the proxy port for the
 * java.net.HttpURLConnection. See setProxyPort().
 * <p>
 * <code>http.proxyUser</code>- specifies the user name used for
 * authentication with a proxy server if required. Note this is identical to the
 * method to specify the proxy user for the java.net.HttpURLConnection. See
 * setProxyUser()
 * <p>
 * <code>http.proxyPassword</code>- specifies the password used for
 * authentication with a proxy server if required. Note this is identical to the
 * method to specify the proxy password for the java.net.HttpURLConnection. See
 * setProxyPassword().
 * <p>
 * <code>http.nonProxyHosts</code>- specifies a list of hosts to not direct
 * to the proxy server. Note this is identical to the method to specify such
 * hosts as for the java.net.HttpURLConnection. See setNonProxyHosts().
 * <p>
 * <code>proxySet</code>- setting this to any value causes all requests to go
 * through the proxy server specified by <code>proxyHost</code> and
 * <code>proxyPort</code>. Note that this method is obsolete and provided
 * only for compatibility with JDK 1.0.2 implementations. The preferred method
 * is to use <code>http.proxyHost/Port</code>. See
 * setProxyHost()/setProxyPort().
 * <p>
 * <code>proxyHost</code>- specifies the host of the proxy server. Note this
 * is identical to the method to specify the proxy host for the
 * java.net.HttpURLConnection. This only works if <code>proxySet</code> is set
 * to something. Note that this method is obsolete and provided only for
 * compatibility with JDK 1.0.2 implementations. The preferred method is to use
 * <code>http.proxyHost</code>. See setProxyHost().
 * <p>
 * <code>proxyPort</code>- specifies the port of the proxy server. Note this
 * is identical to the method to specify the proxy port for the
 * java.net.HttpURLConnection. This only works if <code>proxySet</code> is set
 * to something. Note that this method is obsolete and provided only for
 * compatibility with JDK 1.0.2 implementations. The preferred method is to use
 * <code>http.proxyPort</code>. See setProxyPort().
 * <p>
 * <code>com.oaklandsw.http.timeout</code>- specifies the timeout value in
 * milliseconds. See setDefaultTimeout().
 * <p>
 * <code>com.oaklandsw.http.idleConnectionTimeout</code>- the number of
 * milliseconds that a connection can be idle before it is removed from the
 * connection pool. See setIdleConnectionTimeout() and
 * setDefaultIdleConnectionTimeout().
 * <p>
 * <code>com.oaklandsw.http.idleConnectionPing</code>- this is used to ping
 * the connection before sending a POST request. The ping is issued if the
 * connection was idle for at least the number of milliseconds specified. See
 * setIdleConnectionPing() and setDefaultIdleConnectionPing().
 * <p>
 * <code>com.oaklandsw.http.connectionRequestLimit</code>- this is used to
 * limit the number of requests on a connection. Once this limit is reached, the
 * connection is closed. See setConnectionRequestLimit() and
 * setDefaultConnectionRequestLimit().
 * <p>
 * <code>com.oaklandsw.http.maxConnectionsPerHost</code>- sets the maximum
 * number of connections allowed to a given host:port. If not specified, a
 * default of 2 is assumed. See setMaxConnectionsPerHost().
 * <p>
 * <code>com.oaklandsw.http.tries</code>- the number of times to try a
 * sending an idempotent request if there is a problem with getting the
 * response. Also the number of times to try an idle connection ping on a POST
 * request if this is enabled. The default is 3. See setTries().
 * <p>
 * <code>com.oaklandsw.http.retryInterval</code>- the number of milliseconds
 * to wait before retrying an idempotent request. The default is 0ms. See
 * setRetryInterval().
 * <p>
 * <code>com.oaklandsw.http.pipelining</code>- set to any value to enable
 * pipelining requests. The default is not set. See setDefaultPipelining().
 * <p>
 * <code>com.oaklandsw.http.authenticationType</code>- used to indicate a
 * preferred authentication mode for pipelining or streaming. Set to one of
 * "basic", "digest", or "ntlm". The default is not set. See
 * setDefaultAuthenticationType().
 * <p>
 * <code>com.oaklandsw.http.proxyAuthenticationType</code>- used to indicate
 * a preferred authentication mode for pipelining or streaming. Set to one of
 * "basic", "digest", or "ntlm". The default is not set. See
 * setDefaultProxyAuthenticationType().
 * <p>
 * <code>com.oaklandsw.http.userAgent</code>- set to specify an alternate
 * value for the User-Agent HTTP header. The default is that the User-Agent
 * header is set to DEFAULT_USER_AGENT.
 * <p>
 * <code>com.oaklandsw.http.followRedirectsPost</code>- specifies that
 * redirect response codes are followed for a POST request. see
 * setFollowRedirectsPost() for further details. The default is to not follow
 * redirect response codes for post.
 * <p>
 * <code>com.oaklandsw.http.cookiePolicy</code>- specifies the default cookie
 * policy to be used. See CookiePolicy for the possible values.
 * <p>
 * <code>com.oaklandsw.http.skipEnvironmentInit</code>- specified that all
 * property settings are to be ignored. The property settings are normally read
 * in the static initializer of this class. If this property is set none of the
 * properties will be read. This is used in environments where the settings of
 * some system properties might be for other HTTP client implemementations.
 * <p>
 * 
 */

/*
 * <code> com.oaklandsw.http.useDnsJava </code> - set to "true" to use the
 * dnsjava DNS name resolver rather than the default Java implementation. The
 * default is "false". See setUseDnsJava(). <p>
 */
public abstract class HttpURLConnection extends java.net.HttpURLConnection
{
    static
    {
        // Turn off the logging if there is no log4j configuration
        LogUtils.checkInitialLogging();

        // Avoid attempting to load the 1.4 support since this must run
        // on 1.2 systems
        Util14Controller._dontLoad14 = true;
    }

    private static final Log           _log                                 = LogUtils
                                                                                    .makeLogger();

    public static final String         HTTP_METHOD_GET                      = "GET";
    public static final String         HTTP_METHOD_POST                     = "POST";
    public static final String         HTTP_METHOD_PUT                      = "PUT";
    public static final String         HTTP_METHOD_OPTIONS                  = "OPTIONS";
    public static final String         HTTP_METHOD_DELETE                   = "DELETE";
    public static final String         HTTP_METHOD_HEAD                     = "HEAD";
    public static final String         HTTP_METHOD_TRACE                    = "TRACE";
    public static final String         HTTP_METHOD_CONNECT                  = "CONNECT";

    public static final String         WEBDAV_METHOD_PROPFIND               = "PROPFIND";
    public static final String         WEBDAV_METHOD_PROPPATCH              = "PROPPATCH";
    public static final String         WEBDAV_METHOD_MKCOL                  = "MKCOL";
    public static final String         WEBDAV_METHOD_COPY                   = "COPY";
    public static final String         WEBDAV_METHOD_MOVE                   = "MOVE";
    public static final String         WEBDAV_METHOD_DELETE                 = "DELETE";
    public static final String         WEBDAV_METHOD_LOCK                   = "LOCK";
    public static final String         WEBDAV_METHOD_UNLOCK                 = "UNLOCK";

    public static final String         WEBDAV_METHOD_SEARCH                 = "SEARCH";
    public static final String         WEBDAV_METHOD_VERSION_CONTROL        = "VERSION-CONTROL";
    public static final String         WEBDAV_METHOD_BASELINE_CONTROL       = "BASELINE-CONTROL";
    public static final String         WEBDAV_METHOD_REPORT                 = "REPORT";
    public static final String         WEBDAV_METHOD_CHECKOUT               = "CHECKOUT";
    public static final String         WEBDAV_METHOD_CHECKIN                = "CHECKIN";
    public static final String         WEBDAV_METHOD_UNCHECKOUT             = "UNCHECKOUT";
    public static final String         WEBDAV_METHOD_MKWORKSPACE            = "MKWORKSPACE";
    public static final String         WEBDAV_METHOD_MERGE                  = "MERGE";
    public static final String         WEBDAV_METHOD_UPDATE                 = "UPDATE";
    public static final String         WEBDAV_METHOD_ACL                    = "ACL";

    /**
     * This method will be retried automatically.
     */
    public static final int            METHOD_PROP_RETRY                    = 0x0001;

    /**
     * This method will follow redirects.
     */
    public static final int            METHOD_PROP_REDIRECT                 = 0x0002;

    /**
     * This is used for an HTTP GET method. If a GET method was specified, and
     * getOutputStream() is subsequently called, this changes the method to a
     * POST method. This is for JDK compatibility.
     */
    public static final int            METHOD_PROP_SWITCH_TO_POST           = 0x0004;

    /**
     * Add the content-length header if not already specified. Used for the POST
     * and PUT methods.
     */
    public static final int            METHOD_PROP_ADD_CL_HEADER            = 0x0008;

    /**
     * The response body is ignored. Used for the HEAD method.
     */
    public static final int            METHOD_PROP_IGNORE_RESPONSE_BODY     = 0x0010;

    /**
     * The request line has a URL (most HTTP methods)
     */
    public static final int            METHOD_PROP_REQ_LINE_URL             = 0x0020;

    /**
     * The request line consists of only a "*" (for OPTIONS)
     */
    public static final int            METHOD_PROP_REQ_LINE_STAR            = 0x0040;

    /**
     * The request line has only the host/port (CONNECT)
     */
    public static final int            METHOD_PROP_REQ_LINE_HOST_PORT       = 0x0080;

    /**
     * The content length value is calculated (for potentially adding a
     * content-length header) (POST/PUT).
     */
    public static final int            METHOD_PROP_CALCULATE_CONTENT_LEN    = 0x0100;

    /**
     * A content-type header is automatically added (PUT). This is only used is
     * the METHOD_PROP_CALCULATE_CONTENT_LEN is also set.
     */
    public static final int            METHOD_PROP_SEND_CONTENT_TYPE        = 0x0200;

    /**
     * The connection is left open for this method (CONNECT)
     */
    public static final int            METHOD_PROP_LEAVE_OPEN               = 0x0400;

    /**
     * This is what the method properties are set to initially, this value
     * indicates no method was specified. If this is the case, then the GET
     * method is assumed.
     */
    public static final int            METHOD_PROP_UNSPECIFIED_METHOD       = 0x10000;

    /**
     * A method was specified, but is not known (in the table of methods)
     */
    public static final int            METHOD_PROP_UNKNOWN_METHOD           = 0x20000;

    static final String                PROP_PROXY_HOST                      = "http.proxyHost";
    static final String                PROP_PROXY_PORT                      = "http.proxyPort";
    static final String                PROP_PROXY_USER                      = "http.proxyUser";
    static final String                PROP_PROXY_PASSWORD                  = "http.proxyPassword";
    static final String                PROP_SKIP_ENVIRONMENT_INIT           = "com.oaklandsw.http.skipEnvironmentInit";

    static final String                HDR_USER_AGENT                       = "User-Agent";
    static final String                HDR_CONTENT_ENCODING                 = "Content-Encoding";
    static final String                HDR_CONTENT_LENGTH                   = "Content-Length";
    static final String                HDR_CONTENT_TYPE                     = "Content-Type";
    static final String                HDR_HOST                             = "Host";
    static final String                HDR_TRANSFER_ENCODING                = "Transfer-Encoding";
    static final String                HDR_LOCATION                         = "Location";
    static final String                HDR_EXPECT                           = "Expect";
    static final String                HDR_CONNECTION                       = "Connection";
    static final String                HDR_COOKIE                           = "Cookie";
    static final String                HDR_SET_COOKIE                       = "Set-Cookie";
    static final String                HDR_SET_COOKIE2                      = "Set-Cookie2";
    static final String                HDR_PROXY_CONNECTION                 = "Proxy-Connection";
    static final String                HDR_SEQUENCE                         = "Sequence";

    static final byte[]                HDR_USER_AGENT_BYTES                 = HDR_USER_AGENT
                                                                                    .getBytes();
    static final byte[]                HDR_CONTENT_LENGTH_BYTES             = HDR_CONTENT_LENGTH
                                                                                    .getBytes();
    static final byte[]                HDR_CONTENT_TYPE_BYTES               = HDR_CONTENT_TYPE
                                                                                    .getBytes();
    static final byte[]                HDR_HOST_BYTES                       = HDR_HOST
                                                                                    .getBytes();
    static final byte[]                HDR_TRANSFER_ENCODING_BYTES          = HDR_TRANSFER_ENCODING
                                                                                    .getBytes();
    static final byte[]                HDR_LOCATION_BYTES                   = HDR_LOCATION
                                                                                    .getBytes();
    static final byte[]                HDR_EXPECT_BYTES                     = HDR_EXPECT
                                                                                    .getBytes();
    static final byte[]                HDR_CONNECTION_BYTES                 = HDR_CONNECTION
                                                                                    .getBytes();
    static final byte[]                HDR_COOKIE_BYTES                     = HDR_COOKIE
                                                                                    .getBytes();
    static final byte[]                HDR_SET_COOKIE_BYTES                 = HDR_SET_COOKIE
                                                                                    .getBytes();
    static final byte[]                HDR_SET_COOKIE2_BYTES                = HDR_SET_COOKIE2
                                                                                    .getBytes();
    static final byte[]                HDR_PROXY_CONNECTION_BYTES           = HDR_PROXY_CONNECTION
                                                                                    .getBytes();
    static final byte[]                HDR_SEQUENCE_BYTES                   = HDR_SEQUENCE
                                                                                    .getBytes();

    static final byte[]                HDR_USER_AGENT_LC                    = HDR_USER_AGENT
                                                                                    .toLowerCase()
                                                                                    .getBytes();
    static final byte[]                HDR_CONTENT_ENCODING_LC              = HDR_CONTENT_ENCODING
                                                                                    .toLowerCase()
                                                                                    .getBytes();
    static final byte[]                HDR_CONTENT_LENGTH_LC                = HDR_CONTENT_LENGTH
                                                                                    .toLowerCase()
                                                                                    .getBytes();
    static final byte[]                HDR_CONTENT_TYPE_LC                  = HDR_CONTENT_TYPE
                                                                                    .toLowerCase()
                                                                                    .getBytes();
    static final byte[]                HDR_HOST_LC                          = HDR_HOST
                                                                                    .toLowerCase()
                                                                                    .getBytes();
    static final byte[]                HDR_TRANSFER_ENCODING_LC             = HDR_TRANSFER_ENCODING
                                                                                    .toLowerCase()
                                                                                    .getBytes();
    static final byte[]                HDR_LOCATION_LC                      = HDR_LOCATION
                                                                                    .toLowerCase()
                                                                                    .getBytes();
    static final byte[]                HDR_EXPECT_LC                        = HDR_EXPECT
                                                                                    .toLowerCase()
                                                                                    .getBytes();
    static final byte[]                HDR_CONNECTION_LC                    = HDR_CONNECTION
                                                                                    .toLowerCase()
                                                                                    .getBytes();
    static final byte[]                HDR_COOKIE_LC                        = HDR_COOKIE
                                                                                    .toLowerCase()
                                                                                    .getBytes();
    static final byte[]                HDR_SET_COOKIE_LC                    = HDR_SET_COOKIE
                                                                                    .toLowerCase()
                                                                                    .getBytes();
    static final byte[]                HDR_SET_COOKIE2_LC                   = HDR_SET_COOKIE2
                                                                                    .toLowerCase()
                                                                                    .getBytes();
    static final byte[]                HDR_PROXY_CONNECTION_LC              = HDR_PROXY_CONNECTION
                                                                                    .toLowerCase()
                                                                                    .getBytes();
    static final byte[]                HDR_SEQUENCE_LC                      = HDR_SEQUENCE
                                                                                    .toLowerCase()
                                                                                    .getBytes();

    static final String                HDR_VALUE_KEEP_ALIVE                 = "keep-alive";
    static final String                HDR_VALUE_CLOSE                      = "close";
    static final String                HDR_VALUE_CHUNKED                    = "chunked";
    static final String                HDR_VALUE_DEFAULT_CONTENT_TYPE       = "application/x-www-form-urlencoded";
    static final String                HDR_VALUE_GZIP                       = "gzip";
    static final String                HDR_VALUE_X_GZIP                     = "x-gzip";
    static final String                HDR_VALUE_CHARSET                    = "charset";

    static final byte[]                HDR_VALUE_KEEP_ALIVE_BYTES           = HDR_VALUE_KEEP_ALIVE
                                                                                    .getBytes();
    static final byte[]                HDR_VALUE_CLOSE_BYTES                = HDR_VALUE_CLOSE
                                                                                    .getBytes();
    static final byte[]                HDR_VALUE_CHUNKED_BYTES              = HDR_VALUE_CHUNKED
                                                                                    .getBytes();
    static final byte[]                HDR_VALUE_DEFAULT_CONTENT_TYPE_BYTES = HDR_VALUE_DEFAULT_CONTENT_TYPE
                                                                                    .getBytes();
    static final byte[]                HDR_VALUE_GZIP_BYTES                 = HDR_VALUE_GZIP
                                                                                    .getBytes();
    static final byte[]                HDR_VALUE_X_GZIP_BYTES               = HDR_VALUE_X_GZIP
                                                                                    .getBytes();
    static final byte[]                HDR_VALUE_CHARSET_BYTES              = HDR_VALUE_CHARSET
                                                                                    .getBytes();

    public static final int            NTLM_ENCODING_UNICODE                = 1;
    public static final int            NTLM_ENCODING_OEM                    = 2;

    // Created only once; stores all of the user's persistent parameters
    protected static GlobalState       _globalState;

    /**
     * This is a heuristic to prevent a programming problem. If you create a
     * urlcon and it has a good response (200), but you don't get the
     * InputStream and close it, the underlying connection will be tied up. The
     * Sun implementation has this same issue, but they don't have a max
     * connection limit, so they just go on creating connections. However, our
     * implementation has a limit on the number of connections (by default 2),
     * so you will quickly hang if you do this. This bit is set if some
     * connection has had an InputStream read. If this bit is *not* set, and the
     * connection wait times out, there is a good chance it's a programming
     * error, so we give a message with that information.
     */
    static boolean                     _urlConReleased;

    protected HttpConnectionManager    _connManager;

    // Stores the properties associated with each method
    // K(Method name) V(Method property)
    protected static Map               _methodPropertyMap;

    protected int                      _methodProperties;

    /**
     * The properties for the actual method sent; the method may be different
     * than the requested method during NTLM authentication.
     */
    protected int                      _actualMethodPropsSent;

    protected boolean                  _followRedirects;

    /** Whether or not I should automatically processs authentication. */
    protected boolean                  _doAuthentication                    = true;

    protected boolean                  _executing;

    protected Headers                  _reqHeaders;
    protected Headers                  _respHeaders;
    protected Headers                  _respFooters;

    protected String                   _urlString;

    // The output stream contains the data to be submitted on the HTTP request,
    // this could be a ByteArrayOutputStream, or for the streaming case
    // one of the Streaming* streams
    protected OutputStream             _outStream;

    protected HttpConnection           _connection;

    // The streams associated with the connection we are bound to
    protected ExposedBufferInputStream _conInStream;
    protected BufferedOutputStream     _conOutStream;

    // The connection should be released when finished. This is
    // not the case for a CONNECT request
    protected boolean                  _releaseConnection;

    // The actual count of tries, per destination, this is reset
    // on a redirect or when authentication is required
    protected int                      _tryCount;

    // The number of times this request has been retried due
    // to redirection or authentication
    protected int                      _forwardAuthCount;

    // This is the stream from which the resonse is read
    protected InputStream              _responseStream;

    // Indicates there is no data in the response (for whatever reason)
    // this allows the connection to be released immediately
    // of waiting for the getInputStream().close()
    protected boolean                  _responseIsEmpty;

    protected static final int         MAX_RESPONSE_TEXT                    = 120;

    // This is populated as soon as the response code is known
    protected int                      _responseCode;

    // Where the response text is initially stored
    protected byte[]                   _responseTextBytes                   = new byte[MAX_RESPONSE_TEXT];

    // Actual length of the response text in bytes
    protected int                      _responseTextLength;

    protected HttpUserAgent            _userAgent;

    // The request has been sent, but we have not done the read for the reply.
    // This is the state we are in when streaming and the user just
    // called getOutputStream(). This is used only for streaming mode and
    // pipelined mode
    protected boolean                  _requestSent;

    // The response headers have been read and can be processed
    protected boolean                  _responseHeadersRead;

    // The user has completed their streaming writing and closed the
    // output stream
    protected boolean                  _streamingWritingFinished;

    // It is expected that pipelineBlock() will be called to block for
    // the completion of this urlcon.
    protected boolean                  _pipelineExpectBlock;

    // The request has been sent and the reply received
    protected boolean                  _executed;

    // This has been queued by the pipeline mechanism to write, so it
    // occupies an outstandingWrite count in the connection manager
    boolean                            _pipelineQueuedForWrite;

    // If the connection died due to an I/O exception, it is recorded here.
    // If getResponseCode() is called after the connection is dead and we
    // threw somewhere else, we need to throw this same exception
    protected IOException              _ioException;

    // The request has failed
    protected boolean                  _dead;

    protected static final int         BAD_CONTENT_LENGTH                   = -2;
    protected static final int         UNINIT_CONTENT_LENGTH                = -1;

    protected int                      _contentLength;

    protected boolean                  _hasContentLengthHeader;

    protected static final int         DEFAULT_CHUNK_LEN                    = 4096;

    static final int                   STREAM_NONE                          = 0;
    static final int                   STREAM_CHUNKED                       = 1;
    static final int                   STREAM_FIXED                         = 2;
    static final int                   STREAM_RAW                           = 3;

    protected int                      _streamingMode;

    // For fixed stream mode
    protected int                      _streamingFixedLen                   = -1;

    // If the "Expect" request header is present
    protected boolean                  _expectContinue;

    // From RFC 2616 section 8.1.4
    public static int                  DEFAULT_MAX_CONNECTIONS              = GlobalState.DEFAULT_MAX_CONNECTIONS;

    protected Callback                 _callback;

    protected int                      _connectionTimeout;
    protected int                      _requestTimeout;

    /**
     * Used during NTLM authentication during streaming to provide content for
     * the requests that we know are going to be rejected during the
     * authentication process. This allows authentication to proceed with
     * streaming. If this is not specified, an empty message is sent.
     */
    protected String                   _authenticationDummyContent;

    /**
     * The method used to send this content (see above).
     */
    protected String                   _authenticationDummyMethod;

    public static final int            PIPE_NONE                            = 0x00;

    /**
     * Use pipelining
     */
    public static final int            PIPE_PIPELINE                        = 0x01;

    /**
     * Use as many connections as possible (up to the maximum number of
     * connections to a host/port), spreading the requests across all available
     * connections evenly. If this is not set, a single connection will be used
     * for all requests.
     */
    public static final int            PIPE_MAX_CONNECTIONS                 = 0x02;

    public static final int            PIPE_STANDARD_OPTIONS                = PIPE_PIPELINE
                                                                                | PIPE_MAX_CONNECTIONS;
    int                                _pipeliningOptions;

    /**
     * The maximum number of simitaneous requests that can be outstanding on a
     * pipelined connection. If this is zero, then the number is unlimited.
     */
    int                                _pipeliningMaxDepth;

    protected String                   _proxyHost;
    protected int                      _proxyPort;
    protected String                   _proxyUser;
    protected String                   _proxyPassword;

    protected int                      _idleTimeout;

    protected int                      _idlePing;

    protected int                      _connectionRequestLimit;

    public static int                  MAX_TRIES                            = GlobalState.MAX_TRIES;
    protected int                      _maxTries;

    protected int[]                    _authenticationType                  = new int[AUTH_PROXY + 1];

    protected SSLSocketFactory         _sslSocketFactory;

    protected HostnameVerifier         _hostnameVerifier;

    protected CookieContainer          _cookieContainer;

    protected CookieSpec               _cookieSpec;

    // Use HTTP 1.1?
    protected boolean                  _http11                              = true;

    // Based on the response headers should the connection be closed?
    protected boolean                  _shouldClose;

    /**
     * Header values we are interested in. The actual data used in these arrays
     * is not the size of the arrays, use the corresponding ...Len field.
     * 
     * NOTE: be sure to change HttpURLConnectInternal.resetBeforeRead if
     * anything is added to this list
     */
    protected byte[]                   _hdrContentEncoding;
    protected int                      _hdrContentEncodingLen;

    protected byte[]                   _hdrContentLength;
    protected int                      _hdrContentLengthLen;

    protected byte[]                   _hdrContentType;
    protected int                      _hdrContentTypeLen;

    protected byte[]                   _hdrConnection;
    protected int                      _hdrConnectionLen;

    protected byte[]                   _hdrProxyConnection;
    protected int                      _hdrProxyConnectionLen;

    protected byte[]                   _hdrTransferEncoding;
    protected int                      _hdrTransferEncodingLen;

    protected byte[]                   _hdrWWWAuth;
    protected int                      _hdrWWWAuthLen;

    protected byte[]                   _hdrLocation;
    protected int                      _hdrLocationLen;

    protected byte[]                   _hdrProxyAuth;
    protected int                      _hdrProxyAuthLen;

    protected int                      _hdrContentLengthInt;

    private static boolean             _inLicenseCheck;

    private static boolean             _inInit;

    // Used only for testing purposes
    private static URL                 _testURL;

    // Indexes into the arrays that distinguish normal or proxy
    static final int                   AUTH_NORMAL                          = 0;
    static final int                   AUTH_PROXY                           = 1;

    protected static byte[]            USER_AGENT;

    // This userAgent string is necessary for NTLM/IIS
    public static String               DEFAULT_USER_AGENT                   = "oaklandsoftware-HttpClient/"
                                                                                + Version.VERSION
                                                                                + " Mozilla/4.0 (compatible; "
                                                                                + "MSIE 6.0; Windows NT 5.0)";

    static byte[]                      DEFAULT_USER_AGENT_BYTES             = DEFAULT_USER_AGENT
                                                                                    .getBytes();

    private static void init()
    {
        // Turn off the logging if there is no log4j configuration
        LogUtils.checkInitialLogging();

        try
        {
            _inInit = true;
            if (_globalState._connManager == null)
                _globalState._connManager = new HttpConnectionManager(_globalState);

            //
            // Standard HTTP methods
            //
            _methodPropertyMap = new HashMap();

            setMethodProperties(HTTP_METHOD_GET, METHOD_PROP_RETRY
                | METHOD_PROP_REDIRECT
                | METHOD_PROP_SWITCH_TO_POST
                | METHOD_PROP_REQ_LINE_URL);
            setMethodProperties(HTTP_METHOD_POST, METHOD_PROP_REDIRECT
                | METHOD_PROP_ADD_CL_HEADER
                | METHOD_PROP_REQ_LINE_URL
                | METHOD_PROP_CALCULATE_CONTENT_LEN
                | METHOD_PROP_SEND_CONTENT_TYPE);
            setMethodProperties(HTTP_METHOD_HEAD, METHOD_PROP_RETRY
                | METHOD_PROP_REDIRECT
                | METHOD_PROP_IGNORE_RESPONSE_BODY
                | METHOD_PROP_REQ_LINE_URL);
            setMethodProperties(HTTP_METHOD_PUT, METHOD_PROP_RETRY
                | METHOD_PROP_ADD_CL_HEADER
                | METHOD_PROP_REQ_LINE_URL
                | METHOD_PROP_CALCULATE_CONTENT_LEN);
            setMethodProperties(HTTP_METHOD_OPTIONS, METHOD_PROP_RETRY
                | METHOD_PROP_REQ_LINE_STAR);
            setMethodProperties(HTTP_METHOD_DELETE, METHOD_PROP_RETRY
                | METHOD_PROP_REQ_LINE_URL);
            setMethodProperties(HTTP_METHOD_TRACE, METHOD_PROP_RETRY
                | METHOD_PROP_REQ_LINE_URL);
            setMethodProperties(HTTP_METHOD_CONNECT,
                                METHOD_PROP_IGNORE_RESPONSE_BODY
                                    | METHOD_PROP_REQ_LINE_HOST_PORT
                                    | METHOD_PROP_LEAVE_OPEN);

            // WebDAV methods
            int webDavProps = METHOD_PROP_RETRY
                | METHOD_PROP_REDIRECT
                | METHOD_PROP_ADD_CL_HEADER
                | METHOD_PROP_CALCULATE_CONTENT_LEN
                | METHOD_PROP_REQ_LINE_URL;

            setMethodProperties(WEBDAV_METHOD_PROPFIND, webDavProps);
            setMethodProperties(WEBDAV_METHOD_PROPPATCH, webDavProps);
            setMethodProperties(WEBDAV_METHOD_MKCOL, webDavProps);
            setMethodProperties(WEBDAV_METHOD_COPY, webDavProps);
            setMethodProperties(WEBDAV_METHOD_MOVE, webDavProps);
            setMethodProperties(WEBDAV_METHOD_DELETE, webDavProps);
            setMethodProperties(WEBDAV_METHOD_LOCK, webDavProps);
            setMethodProperties(WEBDAV_METHOD_UNLOCK, webDavProps);

            setMethodProperties(WEBDAV_METHOD_SEARCH, webDavProps);
            setMethodProperties(WEBDAV_METHOD_VERSION_CONTROL, webDavProps);
            setMethodProperties(WEBDAV_METHOD_BASELINE_CONTROL, webDavProps);
            setMethodProperties(WEBDAV_METHOD_REPORT, webDavProps);
            setMethodProperties(WEBDAV_METHOD_CHECKOUT, webDavProps);
            setMethodProperties(WEBDAV_METHOD_CHECKIN, webDavProps);
            setMethodProperties(WEBDAV_METHOD_UNCHECKOUT, webDavProps);
            setMethodProperties(WEBDAV_METHOD_MKWORKSPACE, webDavProps);
            setMethodProperties(WEBDAV_METHOD_MERGE, webDavProps);
            setMethodProperties(WEBDAV_METHOD_UPDATE, webDavProps);
            setMethodProperties(WEBDAV_METHOD_ACL, webDavProps);

            if (System.getProperty(PROP_SKIP_ENVIRONMENT_INIT) == null)
            {
                String str = System.getProperty("com.oaklandsw.http.timeout");
                if (str != null)
                {
                    try
                    {
                        setDefaultConnectionTimeout(Integer.parseInt(str));
                        setDefaultRequestTimeout(getDefaultConnectionTimeout());
                        _log.info("Default timeout: "
                            + getDefaultConnectionTimeout());
                    }
                    catch (Exception ex)
                    {
                        throw new RuntimeException("Invalid value specified for timeout: "
                            + str);
                    }
                }

                str = System
                        .getProperty("com.oaklandsw.http.idleConnectionTimeout");
                if (str != null)
                {
                    try
                    {
                        setDefaultIdleConnectionTimeout(Integer.parseInt(str));
                        _log.info("Default idle connection timeout: "
                            + getDefaultIdleConnectionTimeout());
                    }
                    catch (Exception ex)
                    {
                        throw new RuntimeException("Invalid value specified for idleConnectionTimeout: "
                            + str);
                    }
                }

                str = System
                        .getProperty("com.oaklandsw.http.idleConnectionPing");
                if (str != null)
                {
                    try
                    {
                        setDefaultIdleConnectionPing(Integer.parseInt(str));
                        _log.info("Default idle connection ping: "
                            + getDefaultIdleConnectionPing());
                    }
                    catch (Exception ex)
                    {
                        throw new RuntimeException("Invalid value specified for idleConnectionPing: "
                            + str);
                    }
                }

                str = System
                        .getProperty("com.oaklandsw.http.connectionRequestLimit");
                if (str != null)
                {
                    try
                    {
                        setDefaultConnectionRequestLimit(Integer.parseInt(str));
                        _log.info("Default connection request limit: "
                            + getDefaultConnectionRequestLimit());
                    }
                    catch (Exception ex)
                    {
                        throw new RuntimeException("Invalid value specified for connectionRequestLimit: "
                            + str);
                    }
                }

                str = System.getProperty("com.oaklandsw.http.followRedirects");
                if (str != null && str.equalsIgnoreCase("false"))
                {
                    _log.info("Turning OFF follow redirects");
                    java.net.HttpURLConnection.setFollowRedirects(false);
                }

                str = System
                        .getProperty("com.oaklandsw.http.maxConnectionsPerHost");
                if (str != null)
                {
                    try
                    {
                        setMaxConnectionsPerHost(Integer.parseInt(str));
                        _log.info("Max connections per host: " + str);
                    }
                    catch (Exception ex)
                    {
                        throw new RuntimeException("Invalid value specified for maxConnectionsPerHost: "
                            + str);
                    }
                }

                str = System.getProperty("com.oaklandsw.http.tries");
                if (str != null)
                {
                    try
                    {
                        setDefaultMaxTries(Integer.parseInt(str));
                        _log.info("Number of tries: " + str);
                    }
                    catch (Exception ex)
                    {
                        throw new RuntimeException("Invalid value specified for tries: "
                            + str);
                    }
                }

                str = System.getProperty("com.oaklandsw.http.retryInterval");
                if (str != null)
                {
                    try
                    {
                        setRetryInterval(Integer.parseInt(str));
                        _log.info("Number of retryInterval: " + str);
                    }
                    catch (Exception ex)
                    {
                        throw new RuntimeException("Invalid value specified for retryInterval: "
                            + str);
                    }
                }

                str = System
                        .getProperty("com.oaklandsw.http.preemptiveAuthentication");
                if (str != null)
                {
                    setPreemptiveAuthentication(true);
                }

                str = System
                        .getProperty("com.oaklandsw.http.authenticationType");
                if (str != null)
                {
                    setDefaultAuthenticationType(Authenticator.schemeToInt(str));
                }

                str = System
                        .getProperty("com.oaklandsw.http.proxyAuthenticationType");
                if (str != null)
                {
                    setDefaultProxyAuthenticationType(Authenticator
                            .schemeToInt(str));
                }

                str = System.getProperty("com.oaklandsw.http.pipelining");
                if (str != null)
                {
                    setDefaultPipelining(true);
                }

                if (false)
                {
                    String useDnsJavaStr = System
                            .getProperty("com.oaklandsw.http.useDnsJava");
                    if (useDnsJavaStr != null)
                    {
                        try
                        {
                            // setUseDnsJava(Boolean.valueOf(useDnsJavaStr).booleanValue());
                            _log.info("useDnsJava: " + useDnsJavaStr);
                        }
                        catch (Exception ex)
                        {
                            throw new RuntimeException("Invalid value specified for useDnsJava: "
                                + useDnsJavaStr);
                        }
                    }
                }

                String hostProperty = PROP_PROXY_HOST;
                String portProperty = PROP_PROXY_PORT;
                if (System.getProperty("proxySet") != null)
                {
                    hostProperty = "proxyHost";
                    portProperty = "proxyPort";
                }

                String proxyHost = System.getProperty(hostProperty);
                int proxyPort = -1;
                if (proxyHost != null)
                {
                    String portStr = System.getProperty(portProperty);
                    if (portStr != null)
                    {
                        try
                        {
                            proxyPort = Integer.parseInt(portStr);
                        }
                        catch (Exception ex)
                        {
                            throw new RuntimeException("Invalid value specified for proxyPort: "
                                + portStr);
                        }
                    }
                    if (proxyPort == 0)
                        proxyPort = 80;
                    setProxyHost(proxyHost);
                    setProxyPort(proxyPort);
                }

                if (getProxyHost() != null)
                {
                    _log
                            .info("Proxy: "
                                + getProxyHost()
                                + ":"
                                + getProxyPort());
                }

                str = System.getProperty("http.nonProxyHosts");
                if (str != null)
                {
                    setNonProxyHosts(str);
                    if (getNonProxyHosts() != null)
                        _log.info("Non proxy hosts: " + getNonProxyHosts());
                }

                str = System.getProperties()
                        .getProperty("com.oaklandsw.http.userAgent");
                if (str != null)
                    USER_AGENT = str.getBytes();

                str = System.getProperty("com.oaklandsw.http.cookiePolicy");
                if (str != null)
                {
                    _log
                            .info("Default cookie policy: "
                                + checkConnectionManager()._globalState._defaultCookieSpec);
                    // This validates the policy and throws if there is a
                    // problem
                    checkConnectionManager()._globalState._defaultCookieSpec = CookiePolicy
                            .getCookieSpec(str);
                }
            }

        }
        catch (SecurityException sex)
        {
            if (_log.isDebugEnabled())
                _log.debug("Probably in Applet - properties not used", sex);
        }

        try
        {
            _testURL = new URL("http://dummy.url");
        }
        catch (MalformedURLException ex)
        {
            Util.impossible("Can't set dummy URL", ex);
        }

        if (USER_AGENT == null)
            USER_AGENT = DEFAULT_USER_AGENT_BYTES;

        // Do the license check last to make sure we are fully up, because
        // the license check can cause a use of the HTTP client (in the
        // case where it needs to get a resource) which we must
        // allow.
        if (_inLicenseCheck)
            return;

        // Do the license check dynamically so we don't need to ship
        // the license stuff with the source (and those who build from
        // the source do not have to deal with license issues)
        ClassLoader cl = HttpURLConnection.class.getClassLoader();
        Class licClass = null;
        try
        {
            _inLicenseCheck = true;
            licClass = cl.loadClass("com.oaklandsw.http.HttpLicenseCheck");
            Object licObject = licClass.newInstance();
            Method licMethod = licClass.getMethod("checkLicense",
                                                  new Class[] {});
            licMethod.invoke(licObject, new Object[] {});
        }
        catch (ClassNotFoundException e)
        {
            // Ignored, means there is no license check required
        }
        catch (RuntimeException e)
        {
            // This is a failure in the license check
            System.err.println(e);
            e.printStackTrace(System.err);
            throw new RuntimeException();
        }
        catch (Exception e)
        {
            // Something else is wrong
            Util.impossible(e);
        }
        finally
        {
            _inLicenseCheck = false;
            _inInit = false;
        }
    }

    static
    {
        resetGlobalState();

        // Causes init() to run
        checkConnectionManager();
    }

    public HttpURLConnection()
    {
        this(_testURL);
    }

    // Always returns a conn mgr
    static HttpConnectionManager checkConnectionManager()
    {
        synchronized (HttpURLConnection.class)
        {
            if (_globalState._connManager == null)
            {
                // init() creates a new cm
                init();
            }
            return _globalState._connManager;
        }
    }

    static void resetConnectionManager()
    {
        synchronized (HttpURLConnection.class)
        {
            // Use the same CM through the init() method
            if (!_inInit)
                _globalState._connManager = null;
        }
    }

    /**
     * Constructor, works the same as the constructor for
     * java.net.HttpURLConnection.
     */
    public HttpURLConnection(URL urlParam)
    {
        super(urlParam);

        // The connection manager might have been reset, see if we need to
        // create a new one
        // Make a local copy so that this connection uses the same conn mgr
        // for its entire life
        _connManager = checkConnectionManager();

        if (_log.isDebugEnabled())
            _log.debug("constructor - url: " + urlParam);

        // Get initial value from static
        setInstanceFollowRedirects(java.net.HttpURLConnection
                .getFollowRedirects());

        _actualMethodPropsSent = _methodProperties = METHOD_PROP_UNSPECIFIED_METHOD;

        _reqHeaders = new Headers();
        _respHeaders = new Headers();
        // The footers are created on demand, since they are not
        // frequently used

        _contentLength = UNINIT_CONTENT_LENGTH;

        _urlString = urlParam.toExternalForm();

        try
        {
            setUrl(_urlString);
        }
        catch (MalformedURLException ex)
        {
            // this should not happen, since we are getting the
            // URL from the URL class.
            throw new RuntimeException("(bug - unexpected MalformedURLException) "
                + ex);
        }

        GlobalState gs = _connManager._globalState;

        _userAgent = gs._defaultUserAgent;

        _connectionTimeout = gs._defaultConnectionTimeout;
        _requestTimeout = gs._defaultRequestTimeout;
        _idleTimeout = gs._defaultIdleTimeout;
        _idlePing = gs._defaultIdlePing;
        _connectionRequestLimit = gs._defaultConnectionRequestLimit;
        _maxTries = gs._defaultMaxTries;
        _cookieContainer = gs._defaultCookieContainer;
        _cookieSpec = gs._defaultCookieSpec;
        // Don't use direct assignment because there is other
        // processing in this call
        setPipeliningOptions(gs._defaultPipeliningOptions);
        setPipeliningMaxDepth(gs._defaultPipeliningMaxDepth);
        _callback = gs._defaultCallback;
        _authenticationType[AUTH_NORMAL] = gs._defaultAuthenticationType;
        _authenticationType[AUTH_PROXY] = gs._defaultProxyAuthenticationType;
        _authenticationDummyContent = null;
        _authenticationDummyMethod = HTTP_METHOD_HEAD;
        _proxyHost = gs._proxyHost;
        _proxyPort = gs._proxyPort;
        _proxyUser = gs._proxyUser;
        _proxyPassword = gs._proxyPassword;

        _sslSocketFactory = gs._defaultSSLSocketFactory;
        _hostnameVerifier = gs._defaultHostnameVerifier;

        _connManager.recordCount(HttpConnectionManager.COUNT_ATTEMPTED);
    }

    /**
     * Creates an HTTP connection directly, without using the URLStreamHandler.
     * 
     * This is used if the currently selected HTTP implementation is something
     * other than the Oakland Software implementation, and you need a Oakland
     * Software HTTP connection. It is exactly the same as calling
     * <tt>java.net.URL.openConnection()</tt>
     * 
     * @see java.net.URL#openConnection()
     */
    public static HttpURLConnection openConnection(URL url)
    {
        HttpURLConnection urlCon = new HttpURLConnectInternal(url);
        return urlCon;
    }

    /**
     * Releases the associated transport collection when this object is to be
     * collected.
     */
    public void finalize()
    {
        // Close the underlying connection if it's associated
        // since we can't be sure if its state.
        try
        {
            releaseConnection(CLOSE);
        }
        catch (InterruptedIOException e)
        {
            _log.warn("finalize() - interrupted", e);
        }
    }

    // Request side

    /**
     * @see java.net.HttpURLConnection#setRequestMethod(String)
     */
    public void setRequestMethod(String meth)
    {
        if (connected)
        {
            throw new IllegalStateException("Can't reset method: already connected");
        }

        setRequestMethodInternal(meth);
    }

    void setRequestMethodInternal(String meth)
    {
        // This validates the method
        // Do not call the superclass, as it checks the method name and
        // we don't want to restrict the method name to anything.
        // super.setRequestMethod(meth);

        _actualMethodPropsSent = _methodProperties = getMethodProperties(meth);
        this.method = meth;
    }

    private final void checkRequestProp(String key, String value)
    {
        // Does checking - but skip the checking if we are in the
        // middle of executing, since this may be called internally
        if (!_executing)
            super.setRequestProperty(key, value);

        if (key.equalsIgnoreCase(HDR_CONTENT_LENGTH))
        {
            try
            {
                _contentLength = Integer.parseInt(value);
                _hasContentLengthHeader = true;
            }
            catch (Exception ex)
            {
                _contentLength = BAD_CONTENT_LENGTH;
                // Just swallow, something will get thrown
                // later when the bad content length is detected
            }
        }
        else if (key.equalsIgnoreCase(HDR_EXPECT))
        {
            _expectContinue = true;
        }
    }

    /**
     * @see java.net.HttpURLConnection#setRequestProperty(String,String)
     */
    public void setRequestProperty(String key, String value)
    {
        if (_log.isTraceEnabled())
            _log.trace("setReqProp: " + key + ": " + value);

        checkRequestProp(key, value);
        _reqHeaders.set(key, value);
    }

    /**
     * @see java.net.HttpURLConnection#addRequestProperty(String,String)
     */
    public void addRequestProperty(String key, String value)
    {
        if (_log.isTraceEnabled())
            _log.trace("addReqProp: " + key + ": " + value);
        checkRequestProp(key, value);
        _reqHeaders.add(key, value);
    }

    /**
     * @see java.net.HttpURLConnection#getRequestProperty(String)
     */
    public String getRequestProperty(String key)
    {
        if (key == null)
            return null;
        return _reqHeaders.getAsString(key);
    }

    /**
     * Set whether or not I should automatically follow HTTP redirects (status
     * code 302, etc).
     * 
     * @param followRedirects
     *            true to follow redirects, false otherwise
     */
    public final void setInstanceFollowRedirects(boolean followRedirects)
    {
        _followRedirects = followRedirects;
    }

    /**
     * Whether or not I should automatically follow HTTP redirects (status code
     * 302, etc) Redirects are followed only for GET, POST, or HEAD requests.
     * 
     * @return <tt>true</tt> if I will automatically follow HTTP redirects
     */
    public final boolean getInstanceFollowRedirects()
    {
        if ((_actualMethodPropsSent & METHOD_PROP_REDIRECT) != 0)
            return _followRedirects;

        // Don't allow redirects any other time.
        return false;
    }

    /**
     * @see java.net.HttpURLConnection#getOutputStream()
     */
    public OutputStream getOutputStream() throws IOException
    {
        if (!doOutput)
        {
            throw new IllegalStateException("cannot write to a URLConnection if doOutput=false "
                + "- call setDoOutput(true)");
        }

        if (_executed)
        {
            throw new IllegalStateException("The reply to this URLConnection has already been received");
        }

        try
        {
            // Return the same output stream so the user can just do stuff
            // calling getOutputStream().whatever
            if (_outStream != null)
                return _outStream;

            // Switch to post method - be compatible with JDK
            if ((_actualMethodPropsSent & (METHOD_PROP_SWITCH_TO_POST | METHOD_PROP_UNSPECIFIED_METHOD)) != 0)
            {
                setRequestMethodInternal(HTTP_METHOD_POST);
            }

            if (_streamingMode > STREAM_NONE)
            {
                _log.debug("getOutputStream - streaming start");

                boolean throwAutoRetry = (_pipeliningOptions & PIPE_PIPELINE) != 0;

                // Return the right kind of stream so the user can complete the
                // request. The requested is finished in the normal manner
                switch (_streamingMode)
                {
                    case STREAM_CHUNKED:
                        executeStart();

                        _log.debug("getOutputStream - "
                            + "returning StreamingChunkedOutputStream");
                        _outStream = new StreamingChunkedOutputStream(_connection
                                                                              .getOutputStream(),
                                                                      this,
                                                                      throwAutoRetry);
                        return _outStream;
                    case STREAM_FIXED:
                        _contentLength = _streamingFixedLen;
                        executeStart();

                        _log.debug("getOutputStream - "
                            + "returning StreamingFixedOutputStream");
                        _outStream = new StreamingFixedOutputStream(_connection
                                                                            .getOutputStream(),
                                                                    this,
                                                                    throwAutoRetry,
                                                                    _streamingFixedLen);
                        return _outStream;
                    case STREAM_RAW:
                        executeStart();

                        _log.debug("getOutputStream - "
                            + "returning StreamingRawOutputStream");
                        _outStream = new StreamingRawOutputStream(_connection
                                .getOutputStream(), this, throwAutoRetry);
                        return _outStream;
                }
            }

            if ((_pipeliningOptions & PIPE_PIPELINE) != 0)
            {
                throw new IllegalStateException("You cannot use the pipelining feature for "
                    + "sending output unless the connection is streaming.  "
                    + "Call set[Chunked|FixedLength|Raw]StreamingMode() "
                    + "to turn on streaming.");
            }

            _log.debug("getOutputStream - returning "
                + "ByteArrayOutputStream (non streaming)");
            _outStream = new ByteArrayOutputStream();
            return _outStream;
        }
        finally
        {
            if (_connManager._urlConnIoListener != null && _outStream != null)
            {
                _outStream = new IOListenerOutputStream(_outStream,
                                                        _connManager._urlConnIoListener,
                                                        _connection,
                                                        this);
            }
        }
    }

    /**
     * @see java.net.HttpURLConnection#usingProxy()
     */
    public boolean usingProxy()
    {
        if (_connManager._globalState.getProxyHost(url.getHost()) != null)
            return true;
        return false;
    }

    // The connection needs to be closed
    protected static final int      CLOSE     = 0;

    // The connection was released from reading (as opposed to writing)
    protected static final int      READING   = 1;

    protected static final String[] _relTypes = { "CLOSE", "READING" };

    void releaseConnection(int type) throws InterruptedIOException
    {
        // Need to synchronize for updating the connection state because
        // multiple thread access is possible
        synchronized (this)
        {
            if (_log.isDebugEnabled())
                _log.debug("releaseConnection: " + _relTypes[type]);

            try
            {
                if (connected)
                {
                    if (_releaseConnection)
                    {
                        if (_log.isDebugEnabled())
                        {
                            _log.debug("releaseConnection: "
                                + "now connected = FALSE");
                        }
                        connected = false;
                        _outStream = null;

                        boolean closed = false;
                        if (type == CLOSE || _shouldClose)
                        {
                            _log.debug("releaseConnection: "
                                + "closing the connection.");
                            _connection.close();
                            closed = true;
                        }

                        // When reading and pipelining the connection is not
                        // owned from the connection manager point of view
                        if (closed || (_pipeliningOptions & PIPE_PIPELINE) == 0)
                        {
                            // See the comment on this field
                            _urlConReleased = true;
                            _connManager.releaseConnection(_connection);
                        }

                        // Never reset these because the connection might be in
                        // use in another thread (with this same urlcon)
                        // _connection = null;
                        // _conInStream = null;
                        // _conOutStream = null;

                    }
                    else
                    {
                        _log.debug("releaseConnection: "
                            + "no _release (CONNECT case).");

                        // This happens on a CONNECT request when
                        // the connection has already been closed,
                        // just re-open the same connection using
                        // the low-level interface
                        // This also happens when the connection
                        // was explicitly allocated
                        if (type == CLOSE)
                        {
                            try
                            {
                                _log.debug("releaseConnection: "
                                    + "reopening after CLOSE");
                                _connection.close();
                                _connection.openSocket();
                                setStreams();
                                _log.debug("releaseConnection:  WORKED");
                            }
                            catch (IOException ex)
                            {
                                _log.warn("Exception re-opening "
                                    + "closed socket for CONNECT: ", ex);
                            }
                        }
                    }
                }
            }
            catch (InterruptedException ex)
            {
                InterruptedIOException ioex = new InterruptedIOException();
                ioex.initCause(ex);
                throw ioex;
            }
        }
    }

    /**
     * @see java.net.HttpURLConnection#connect()
     */
    public void connect() throws IOException
    {
        // _log.trace("connect");

        if (connected)
            return;

        try
        {
            setConnection(_connManager.getConnection(this), RELEASE);
        }
        catch (MalformedURLException ex)
        {
            Util.impossible("Unexpected MalformedURLException (bug): ", ex);
        }
        catch (HttpTimeoutException tex)
        {
            releaseConnection(CLOSE);
            _log.debug("HttpURL.getConn (timeout): ", tex);
            throw tex;
        }
        catch (IOException hex)
        {
            releaseConnection(CLOSE);
            _log.debug("HttpURL.getConn: ", hex);
            throw hex;
        }
        catch (InterruptedException ex)
        {
            // No need to release the connection because if we got
            // this, the connection was not assigned
            InterruptedIOException iex = new InterruptedIOException();
            iex.initCause(ex);
            throw iex;
        }
    }

    /**
     * Returns the HttpConnectionManager
     */
    public static HttpConnectionManager getConnectionManager()
    {
        return checkConnectionManager();
    }

    /**
     * Associates a connection with this object. This is used in conjunction
     * with HttpConnectionManager.getConnection() to provide a means of
     * controlling the allocation of socket connections and associating them
     * with HttpURLConnections. To close the underlying socket connection, call
     * disconnect() on this object.
     * <p>
     * The caller must ensure that the same HttpConnection object is not used by
     * multiple HttpURLConnections.
     * 
     * @exception IOException -
     *                if the connection cannot be opened
     * @exception IllegalStateException -
     *                if a connection has already been associated with this
     *                object.
     */
    public void setConnection(HttpConnection conn) throws IOException
    {
        if (connected)
            throw new IllegalStateException("Connection already associated");

        setConnection(conn, !RELEASE);
    }

    /**
     * Returns the connection associated with this object, if any.
     * 
     * @return the connection associated with this object.
     */
    public HttpConnection getConnection()
    {
        return _connection;
    }

    static final boolean RELEASE = true;

    void setConnection(HttpConnection conn, boolean release) throws IOException
    {
        // Need to serialize the connection state because mutiple
        // threads can access this (consider a read immediately as the
        // write is finishing)
        synchronized (this)
        {
            // Set connected early, this is how we tell if there is an actual
            // HttpConnection associated with this, even if the open fails
            if (_log.isDebugEnabled())
                _log.debug("setConnection: to: " + conn);
            connected = true;
            _connection = conn;

            if ((_pipeliningOptions & PIPE_PIPELINE) != 0)
                _connection.adjustPipelinedUrlConCount(1);
            _connection._totalReqUrlConCount++;

            if (_log.isDebugEnabled())
            {
                _log.debug("setConnection: to: "
                    + conn
                    + " total req count:"
                    + _connection._totalReqUrlConCount);
            }

            _releaseConnection = release;

            _connection.setSoTimeout(_requestTimeout);

            if (!_connection.isOpen())
            {
                _log.debug("Opening the connection.");
                _connection.setConnectTimeout(_connectionTimeout);
                _connection.setSSLSocketFactory(_sslSocketFactory);
                _connection.setHostnameVerifier(_hostnameVerifier);
                _connection.open();
            }

            setStreams();
        }
    }

    private void setStreams() throws IOException
    {
        _conInStream = _connection.getInputStream();
        _conOutStream = _connection.getOutputStream();
    }

    private void checkConnectNoThrow()
    {
        if (_executing || _executed)
            return;
        try
        {
            execute();
        }
        catch (IOException ex)
        {
            _log.warn("HttpURLCon: exception connecting: ", ex);
        }
    }

    /**
     * Returns true if this connection is currently connected.
     * 
     * @return a boolean value, true if connected.
     */
    public boolean isConnected()
    {
        return connected;
    }

    // Response side

    /**
     * @see java.net.HttpURLConnection#getResponseCode()
     */
    public int getResponseCode() throws IOException
    {
        if (_ioException != null)
            throw _ioException;
        execute();
        return _responseCode;
    }

    /**
     * @see java.net.HttpURLConnection#getResponseMessage()
     */
    public String getResponseMessage() throws IOException
    {
        if (_ioException != null)
            throw _ioException;
        execute();
        return Util.bytesToString(_responseTextBytes, _responseTextLength);
    }

    /**
     * Used to allow data to be sent on with the HTTP request using chunked
     * encoding. The size of each chunk is determined by the size of the write()
     * calls to the OutputStream.
     * <p>
     * This uses the HTTP chunked transfer encoding mode, which is an optional
     * feature not supported by all HTTP servers.
     * <p>
     * Use this if:
     * <ul>
     * <li>you don't know the total size of the data you are sending,
     * <li>you are sure the server supports chunked transfer encoding,
     * <li>you don't want to tie up the full amount of memory of the data prior
     * to sending the request; and
     * <li>the request will not require authentication or redirection
     * </ul>
     * 
     * <p>
     * Use of this method avoids double-copying of the data.
     * 
     * <p>
     * When using this method, you may not call write(int) method on the
     * returned stream. If you must use single byte writes, then wrap the
     * returned stream in a BufferedOutputStream(). The chunk size will then be
     * the size of that buffer.
     * 
     * <p>
     * This is used to improve performance sending by not first buffering the
     * data associated with the request. If the request requires authentication
     * or redirection, this will not work as the data will have been sent. In
     * this case, you will get an HttpRetryException
     * 
     * @param chunkLen -
     *            This is ignored and exists for compatibility with the Java
     *            HttpURLConnection API. The size of each chunk is based on the
     *            size of each write to the stream.
     * @throws IllegalStateException -
     *             If the connection is already connected or a different
     *             streaming mode has been set.
     */
    public void setChunkedStreamingMode(int chunkLen)
    {
        // Note, don't use the superclass for this because that only exists
        // on 1.5 and higher and we need to support this for all versions
        if (connected
            || (_streamingMode != STREAM_NONE && _streamingMode != STREAM_CHUNKED))
            throw new IllegalStateException();

        _streamingMode = STREAM_CHUNKED;
    }

    /**
     * Used to allow data to be sent on with the HTTP request without buffering
     * and when the length of the data <i>is known</i> in advance.
     * <p>
     * Use of this method avoids double-copying of the data.
     * <p>
     * This is used to improve performance sending by not first buffering the
     * data associated with the request. If the request requires authentication
     * or redirection, this will not work as the data will have been sent. In
     * this case, you will get an HttpRetryException
     * 
     * @param fixedLen -
     *            The size of the data being sent (the Content-Length)
     * @throws IllegalStateException -
     *             If the connection is already connected or a different
     *             streaming mode has been set.
     */
    public void setFixedLengthStreamingMode(int fixedLen)
    {
        // Note, don't use the superclass for this because that only exists
        // on 1.5 and higher and we need to support this for all versions
        if (connected
            || (_streamingMode != STREAM_NONE && _streamingMode != STREAM_FIXED))
            throw new IllegalStateException();
        if (fixedLen < 0)
            throw new IllegalArgumentException("fixedLen must be greater than or equal to zero");
        _streamingMode = STREAM_FIXED;
        _streamingFixedLen = fixedLen;
    }

    /**
     * Used to allow direct access to sending the HTTP request.
     * <p>
     * When using this option, you get the OutputStream using
     * {@link #getOutputStream()} and then write the HTTP request that stream.
     * After the request is written close the stream. The HTTP response is
     * handled normally.
     * <p>
     * This is used if you wish to have complete control over the exact content
     * of the HTTP request.
     * 
     * @param useRaw
     *            True if enabling raw streaming, false if not
     * @throws IllegalStateException -
     *             If the connection is already connected or a different
     *             streaming mode has been set.
     */
    public void setRawStreamingMode(boolean useRaw)
    {
        if (connected)
            throw new IllegalStateException();
        if (true)
        {
            if (_streamingMode != STREAM_NONE && _streamingMode != STREAM_RAW)
                throw new IllegalStateException();
            _streamingMode = STREAM_RAW;
        }
        else
        {
            _streamingMode = STREAM_NONE;
        }
    }

    /**
     * @see java.net.HttpURLConnection#getHeaderField(String)
     */
    public String getHeaderField(String name)
    {
        checkConnectNoThrow();

        if (name == null)
            return null;
        return _respHeaders.getAsString(name);
    }

    /**
     * @see java.net.URLConnection#getHeaderFields()
     */
    public Map getHeaderFields()
    {
        checkConnectNoThrow();

        return _respHeaders.getMap();
    }

    /**
     * @see java.net.HttpURLConnection#getHeaderFieldKey(int)
     */
    public String getHeaderFieldKey(int keyPosition)
    {
        checkConnectNoThrow();

        // Note: HttpClient does not consider the returned Status Line as
        // a response header. However, getHeaderFieldKey(0) is supposed to
        // return null. Hence the special case below ...

        if (keyPosition == 0)
        {
            return null;
        }

        return _respHeaders.getKeyAsString(keyPosition - 1);
    }

    /**
     * @see java.net.HttpURLConnection#getHeaderField(int)
     */
    public String getHeaderField(int position)
    {
        checkConnectNoThrow();

        // Note: HttpClient does not consider the returned Status Line as
        // a response header. However, getHeaderField(0) is supposed to
        // return the status line. Hence the special case below ...

        if (position == 0)
        {
            // Make this up because we did not actually keep the line
            return "HTTP/1."
                + (_http11 ? "1" : "0")
                + " "
                + _responseCode
                + ((_responseTextLength == 0)
                    ? ""
                    : (" " + Util.bytesToString(_responseTextBytes,
                                                _responseTextLength)));
        }

        return _respHeaders.getAsString(position - 1);
    }

    /**
     * Returns the number of response headers.
     */
    public int getHeadersLength()
    {
        checkConnectNoThrow();

        return _respHeaders.length();
    }

    /**
     * Associates the given CookieContainer with this connection. This must be
     * done before the request is connected. This will cause any cookies in the
     * container to be sent with the request, and any cookies received from the
     * server are stored in the container.
     * 
     * @param container
     *            the container store/retrieve the cookies associated with this
     *            request, null if the request should not used cookies.
     * @param policy
     *            the name of the CookiePolicy to use in processing cookies. -1
     *            will use the default policy, if the container is not null.
     */
    public void setCookieSupport(CookieContainer container, String policy)
    {
        if (connected)
            throw new IllegalStateException("Connection has been established");
        _cookieContainer = container;
        _cookieSpec = CookiePolicy.getCookieSpec(policy);
    }

    public static void setDefaultCookieSupport(CookieContainer container,
                                               String policy)
    {
        checkConnectionManager()._globalState._defaultCookieContainer = container;
        checkConnectionManager()._globalState._defaultCookieSpec = CookiePolicy
                .getCookieSpec(policy);
    }

    /**
     * Returns the CookieContainer associated with this request.
     * 
     * @return CookieContainer
     */
    public CookieContainer getCookieContainer()
    {
        return _cookieContainer;

    }

    /**
     * Returns the CookiePolicy string associated with this request.
     * 
     * @return an string identifying the cookie policy
     */
    public String getCookiePolicy()
    {
        return _cookieSpec.getPolicyName();
    }

    /**
     * Gets an InputStream for the data returned in the response. You should not
     * use <tt>getErrorStream()</tt> if you have called this, as this function
     * returns the response even in the case of an error.
     * 
     * @see java.net.HttpURLConnection#getInputStream()
     */
    public InputStream getInputStream() throws IOException
    {
        if (!_executed)
            execute();
        return getResponseStream();
    }

    private static final int CT_START   = 0;
    private static final int CT_PARAMS  = 1;
    private static final int CT_CHARSET = 2;

    /**
     * Gets a Reader for the data returned in the response.
     * 
     * This will return whatever data is provided in the response, regardless of
     * whether the request succeeded or not.
     * 
     * This is an alternative to using {@link #getInputStream()}, used if you
     * want to read the data as character data instead of as bytes. The
     * characters returned will be decoded according to the
     * <code>Content-Encoding</code> and <code>Content-Type</code> headers
     * in the response. If the charset of the response is not specified using a
     * <code>Content-Type</code> header, then ISO-8859-1 is used, in
     * accordance with the rules of RFC 2616 (3.7.1).
     */
    public Reader getReader() throws IOException
    {
        // Get the input stream first which causes the request to execute
        InputStream is = getInputStream();

        if (_hdrContentEncoding != null)
        {
            // FIXME - right now we handle only gzip encoding
            if (Util.bytesEqual(HDR_VALUE_X_GZIP_BYTES,
                                _hdrContentEncoding,
                                _hdrContentEncodingLen)
                || Util.bytesEqual(HDR_VALUE_GZIP_BYTES,
                                   _hdrContentEncoding,
                                   _hdrContentEncodingLen))
            {
                if (_log.isDebugEnabled())
                    _log.debug("Using gzip content encoding");
                is = new GZIPInputStream(is);
            }
        }

        String charset = "ISO-8859-1";
        if (_hdrContentType != null)
        {
            byte charsetValue[] = new byte[200];
            int charsetIndex = 0;

            // FIXME - this does not handle errors at all
            lookForCharset:
            {
                int state = CT_START;
                for (int i = 0; i < _hdrContentTypeLen; i++)
                {
                    switch (state)
                    {
                        case CT_START:
                            if (_hdrContentType[i] == ';')
                            {
                                state = CT_PARAMS;
                            }
                            // No params
                            if (_hdrContentType[i] == '\r')
                            {
                                break lookForCharset;
                            }
                            break;
                        case CT_PARAMS:
                            if (Util.bytesEqual(HDR_VALUE_CHARSET_BYTES,
                                                _hdrContentType,
                                                i,
                                                _hdrContentTypeLen))
                            {
                                // Move past the '=' after charset
                                i += HDR_VALUE_CHARSET_BYTES.length;
                                state = CT_CHARSET;
                            }
                            break;
                        case CT_CHARSET:
                            if (_hdrContentType[i] == '\r')
                            {
                                break lookForCharset;
                            }
                            charsetValue[charsetIndex++] = _hdrContentType[i];
                            break;
                    }
                }
                
                if (charsetIndex > 0)
                {
                    charset = new String(charsetValue, 0, charsetIndex);
                }
                if (_log.isDebugEnabled())
                    _log.debug("Using charset: " + charset);
            }
        }

        Reader r;
        r = new InputStreamReader(is, charset);
        return r;
    }

    /**
     * Returns the data associated with the connection in the event of an error.
     * This returns data only when the response code is >= 300. This should not
     * be used if getInputStream() has already been called since that will have
     * read the response.
     * 
     * @see java.net.HttpURLConnection#getErrorStream()
     */
    public InputStream getErrorStream()
    {
        _log.trace("getErrorStream");

        // This does not force a connection
        if (!connected)
            return null;

        // Only do this if there is an error
        try
        {
            if (getResponseCode() < 300)
                return null;
            // If no data available, return null (only if we have
            // read it all)
            if (_responseIsEmpty)
                return null;

            return getResponseStream();
        }
        catch (IOException ex)
        {
            // If something threw then we are not going to have
            // any error data
            return null;
        }

    }

    /**
     * Gets an input stream to read the response.
     */
    protected InputStream getResponseStream() throws IOException
    {
        InputStream is;

        if (_dead)
        {
            throw new IllegalStateException("This HttpURLConnection has previously "
                + "failed (and the error has been reported)");
        }

        // Could be the type of request does not return data, just give
        // back an empty stream
        if (_responseStream == null)
            return new ByteArrayInputStream(new byte[] {});

        is = _responseStream;

        if (_log.isDebugEnabled())
        {
            _log.debug("getResponseStream - returning: "
                + ((is == null) ? "null" : is.getClass().getName()));
        }

        if (_connManager._urlConnIoListener != null)
        {
            is = new IOListenerInputStream(is,
                                           _connManager._urlConnIoListener,
                                           _connection,
                                           this);
        }

        return is;
    }

    /**
     * @see java.net.HttpURLConnection#disconnect()
     */
    public void disconnect()
    {
        _log.trace("disconnect");
        try
        {
            connect();
        }
        catch (IOException ex)
        {
            // We can't get the connection, so do nothing
            return;
        }

        // We could have been connected and the connection
        // released, like after we do a getInputStream()
        if (connected)
        {
            // Here we make sure and return the connection
            // to get the counts adjusted
            _releaseConnection = true;
            try
            {
                releaseConnection(CLOSE);
            }
            catch (InterruptedIOException e)
            {
                // Can't throw it, just have to swallow it
                _log.warn("InterruptedException during disconnect()", e);
            }
        }
    }

    /**
     * Sets the default user agent.
     */
    public static void setDefaultUserAgent(HttpUserAgent userAgent)
    {
        checkConnectionManager()._globalState._defaultUserAgent = userAgent;
    }

    /**
     * Gets the default user agent.
     */
    public static HttpUserAgent getDefaultUserAgent()
    {
        return checkConnectionManager()._globalState._defaultUserAgent;
    }

    /**
     * Sets the user agent for this connection.
     * 
     * @param userAgent
     *            a object that implements the HttpUserAgent interface.
     * @see com.oaklandsw.http.HttpUserAgent
     */
    public void setUserAgent(HttpUserAgent userAgent)
    {
        _userAgent = userAgent;
    }

    /**
     * Sets the user agent for this connection.
     * 
     * @return the user agent object.
     * @see com.oaklandsw.http.HttpUserAgent
     */
    public HttpUserAgent getUserAgent()
    {
        return _userAgent;
    }

    /**
     * Automatic processing of responses where authentication is required
     * (status codes 401 and 407).
     * 
     * @return <tt>true</tt> if authentications will be processed
     *         automatically
     */
    public boolean getDoAuthentication()
    {
        return _doAuthentication;
    }

    /**
     * Automatic processing of responses where authentication is required
     * (status codes 401 and 407).
     * <p>
     * The default is true.
     * 
     * @param doAuthentication
     *            <tt>true</tt> to process authentications
     */
    public void setDoAuthentication(boolean doAuthentication)
    {
        if (_log.isDebugEnabled())
            _log.debug("setDoAuthentication: " + doAuthentication);
        _doAuthentication = doAuthentication;
    }

    /**
     * Return the timeout value associated with this connection.
     * 
     * @return the timeout value in milliseconds.
     * @deprecated please use either getConnectionTimeout() or
     *             getRequestTimeout()
     */
    public int getTimeout()
    {
        return getConnectionTimeout();
    }

    /**
     * Set the timeout value associated with this connection. This is the number
     * of milliseconds to wait for a connection to be established or a response
     * to be received. If this is not specified the default timeout value is
     * used. This is the same as calling setConnectTimeout() and
     * setRequestTimeout() with the same value.
     * 
     * @param ms
     *            milliseconds to wait for a connection or response.
     */
    public void setTimeout(int ms)
    {
        if (_log.isDebugEnabled())
            _log.debug("setTimeout: " + ms);
        _connectionTimeout = ms;
        _requestTimeout = ms;
    }

    /**
     * Return the connection timeout value associated with this connection.
     * 
     * @return the timeout value in milliseconds.
     */
    public int getConnectionTimeout()
    {
        return _connectionTimeout;
    }

    /**
     * Set the connection timeout value associated with this connection. This is
     * the number of milliseconds to wait for the socket connection to be
     * established. If this is not specified the default connection timeout
     * value is used.
     * 
     * @param ms
     *            milliseconds to wait for the socket connection.
     */
    public void setConnectionTimeout(int ms)
    {
        if (_log.isDebugEnabled())
            _log.debug("setConnectionTimeout: " + ms);
        _connectionTimeout = ms;
    }

    /**
     * Return the request timeout value associated with this connection.
     * 
     * @return the timeout value in milliseconds.
     */
    public int getRequestTimeout()
    {
        return _requestTimeout;
    }

    /**
     * Set the request timeout value associated with this connection. This is
     * the number of milliseconds to wait for a response after the request was
     * sent. If this is not specified the default request timeout value is used.
     * 
     * @param ms
     *            milliseconds to wait for the response.
     */
    public void setRequestTimeout(int ms)
    {
        if (_log.isDebugEnabled())
            _log.debug("setRequestTimeout: " + ms);
        _requestTimeout = ms;
    }

    /**
     * Return the default connection timeout value.
     * 
     * @return the default timeout value in milliseconds.
     * @deprecated please use getDefaultConnectionTimeout() or
     *             getDefaultRequestTimeout()
     */
    public static int getDefaultTimeout()
    {
        return checkConnectionManager()._globalState._defaultConnectionTimeout;
    }

    /**
     * Set the default timeout value. This is the number of milliseconds to wait
     * for a connection to be established or a response to be received. Setting
     * the value to 0 means no timeout is used, which is the default value for
     * this property. This is equivalent to setting the
     * <code>com.oaklandsw.http.timeout</code> property. This is the same as
     * calling setDefaultConnectTimeout() and setDefaultRequestTimeout() with
     * the same value.
     * 
     * @param ms
     *            milliseconds to wait for a connection or response.
     */
    public static void setDefaultTimeout(int ms)
    {
        if (_log.isDebugEnabled())
            _log.debug("setDefaultTimeout: " + ms);
        checkConnectionManager()._globalState._defaultConnectionTimeout = ms;
        checkConnectionManager()._globalState._defaultRequestTimeout = ms;
    }

    /**
     * Return the default connection timeout value.
     * 
     * @return the default timeout value in milliseconds.
     */
    public static int getDefaultConnectionTimeout()
    {
        return checkConnectionManager()._globalState._defaultConnectionTimeout;
    }

    /**
     * Set the default connection timeout value. This is the number of
     * milliseconds to wait for a connection to be established. Setting the
     * value to 0 means no timeout is used, which is the default value for this
     * property.
     * 
     * @param ms
     *            milliseconds to wait for a socket connection.
     */
    public static void setDefaultConnectionTimeout(int ms)
    {
        if (_log.isDebugEnabled())
            _log.debug("setDefaultConnectionTimeout: " + ms);
        checkConnectionManager()._globalState._defaultConnectionTimeout = ms;
    }

    /**
     * Return the default request timeout value.
     * 
     * @return the default timeout value in milliseconds.
     */
    public static int getDefaultRequestTimeout()
    {
        return checkConnectionManager()._globalState._defaultRequestTimeout;
    }

    /**
     * Set the default request timeout value. This is the number of milliseconds
     * to wait for a response to a request. Setting the value to 0 means no
     * timeout is used, which is the default value for this property.
     * 
     * @param ms
     *            milliseconds to wait for a response.
     */
    public static void setDefaultRequestTimeout(int ms)
    {
        if (_log.isDebugEnabled())
            _log.debug("setDefaultRequestTimeout: " + ms);
        checkConnectionManager()._globalState._defaultRequestTimeout = ms;
    }

    /**
     * Return the idle connection timeout value associated with this connection.
     * 
     * @return the idle connection timeout value in milliseconds.
     */
    public int getIdleConnectionTimeout()
    {
        return _idleTimeout;
    }

    /**
     * Set the idle connection timeout value associated with this connection.
     * This is the number of milliseconds to wait before closing an idle
     * connection. Setting the value to 0 means idle connections are never
     * automatically closed. If this is not specified the default idle
     * connection timeout value is used.
     * 
     * @param ms
     *            milliseconds to wait before closing an idle connection.
     */
    public void setIdleConnectionTimeout(int ms)
    {
        if (_log.isDebugEnabled())
            _log.debug("setIdleConnectionTimeout: " + ms);
        _idleTimeout = ms;
    }

    /**
     * Return the default idle connection timeout value.
     * 
     * @return the default idle connection timeout value in milliseconds.
     */
    public static int getDefaultIdleConnectionTimeout()
    {
        return checkConnectionManager()._globalState._defaultIdleTimeout;
    }

    /**
     * Set the default idle connection timeout value. This is the number of
     * milliseconds to wait before closing an idle connection. Setting the value
     * to 0 means idle connections are never automatically closed. The default
     * value for this is 14000 (14 seconds). This value was chosen to be
     * slightly less than the default KeepAliveTimeout value for the Apache web
     * server. This is equivalent to setting the
     * <code>com.oaklandsw.http.idleConnectionTimeout</code> property.
     * 
     * @param ms
     *            milliseconds to wait before closing an idle connection.
     */
    public static void setDefaultIdleConnectionTimeout(int ms)
    {
        if (_log.isDebugEnabled())
            _log.debug("setDefaultIdleConnectionTimeout: " + ms);
        checkConnectionManager()._globalState._defaultIdleTimeout = ms;
    }

    /**
     * Set the default idle connection timeout value to its default value (14
     * seconds).
     * 
     */
    public static void setDefaultIdleConnectionTimeout()
    {
        if (_log.isDebugEnabled())
        {
            _log.debug("setDefaultIdleConnectionTimeout (Default): "
                + GlobalState.DEFAULT_IDLE_TIMEOUT);
        }
        checkConnectionManager()._globalState._defaultIdleTimeout = GlobalState.DEFAULT_IDLE_TIMEOUT;
    }

    /**
     * Return the idle connection ping value associated with this connection.
     * 
     * @return the idle connection ping value in milliseconds.
     */
    public int getIdleConnectionPing()
    {
        return _idlePing;
    }

    /**
     * Set the idle connection ping value associated with this connection. This
     * is used to ping the connection before sending a POST request. The ping is
     * issued if the connection was idle for at least the number of milliseconds
     * specified. Setting the value to 0 means idle connections are never pinged
     * before a POST request. If this is not specified the default idle
     * connection ping value is used.
     * 
     * In the event the ping fails, it is retried on a different connection. The
     * number of tries is controlled by setTries().
     * 
     * @param ms
     *            milliseconds to wait before pinging an idle connection.
     */
    public void setIdleConnectionPing(int ms)
    {
        if (_log.isDebugEnabled())
            _log.debug("setIdleConnectionPing: " + ms);
        _idlePing = ms;
    }

    /**
     * Return the default idle connection ping value.
     * 
     * @return the default idle connection ping value in milliseconds.
     */
    public static int getDefaultIdleConnectionPing()
    {
        return checkConnectionManager()._globalState._defaultIdlePing;
    }

    /**
     * Set the default idle connection ping value. See setIdleConnectionPing().
     * This is equivalent to setting the
     * <code>com.oaklandsw.http.idleConnectionPing</code> property.
     * 
     * @param ms
     *            milliseconds to wait before pinging an idle connection.
     */
    public static void setDefaultIdleConnectionPing(int ms)
    {
        if (_log.isDebugEnabled())
            _log.debug("setDefaultIdleConnectionPing: " + ms);
        checkConnectionManager()._globalState._defaultIdlePing = ms;
    }

    /**
     * Set the default idle connection ping value to its default value (0
     * seconds).
     * 
     */
    public static void setDefaultIdleConnectionPing()
    {
        if (_log.isDebugEnabled())
        {
            _log.debug("setDefaultIdleConnectionPing (Default): "
                + GlobalState.DEFAULT_IDLE_PING);
        }
        checkConnectionManager()._globalState._defaultIdlePing = GlobalState.DEFAULT_IDLE_PING;
    }

    /**
     * Return the connection request limit value associated with this
     * connection.
     * 
     * @see #setConnectionRequestLimit(int)
     * @return the connection request limit value
     */
    public int getConnectionRequestLimit()
    {
        return _connectionRequestLimit;
    }

    /**
     * Sets the number of HttpURLConnection requests that can be used on the
     * underlying socket connection. After this number is reached, the socket
     * connection is closed.
     * <p>
     * This is useful in certain pipelining situations where the connection
     * might be closed by the server after a certain number of requests.
     * However, if the server closes a connection after a certain number of
     * requests using a Connection-Close header, this fact is automatically
     * noted and pipelining will not send more than that observed number of
     * requests on a socket connection.
     * 
     * @param requests
     *            number of requests allowed for each underlying socket
     *            connection
     */
    public void setConnectionRequestLimit(int requests)
    {
        if (_log.isDebugEnabled())
            _log.debug("setConnectionRequestLimit: " + requests);
        _connectionRequestLimit = requests;
    }

    /**
     * Return the default idle connection ping value.
     * 
     * @return the default idle connection ping value.
     */
    public static int getDefaultConnectionRequestLimit()
    {
        return checkConnectionManager()._globalState._defaultConnectionRequestLimit;
    }

    /**
     * Set the default idle connection request limit value.
     * 
     * This is equivalent to setting the
     * <code>com.oaklandsw.http.connectionRequestLimit</code> property.
     * 
     * @See #setConnectionRequestLimit(int).
     * @param requests
     *            number of requests allowed for each underlying socket
     *            connection
     */
    public static void setDefaultConnectionRequestLimit(int requests)
    {
        if (_log.isDebugEnabled())
            _log.debug("setDefaultConnectionRequestLimit: " + requests);
        checkConnectionManager()._globalState._defaultConnectionRequestLimit = requests;
    }

    /**
     * Set the default idle connection ping value to its default value (0
     * seconds).
     * 
     */
    public static void setDefaultConnectionRequestLimit()
    {
        if (_log.isDebugEnabled())
        {
            _log.debug("setDefaultConnectionRequestLimit (Default): "
                + GlobalState.DEFAULT_CONNECTION_REQUEST_LIMIT);
        }
        checkConnectionManager()._globalState._defaultConnectionRequestLimit = GlobalState.DEFAULT_CONNECTION_REQUEST_LIMIT;
    }

    /**
     * This returns true. All connections are assumed that they will get an
     * input stream and close it if the connection succeeds.
     * 
     * @deprecated
     * @return the value of the explicit close flag.
     */
    public static boolean isExplicitClose()
    {
        return true;
    }

    /**
     * This does nothing.
     * 
     * @deprecated
     * @param explicitClose
     *            the value for explicit closing of the connection.
     */
    public static void setExplicitClose(boolean explicitClose)
    {
    }

    /**
     * Set the maximum number of connections allowed for a given host:port. Per
     * RFC 2616 section 8.1.4, this value defaults to 2.
     * <p>
     * Note that this refers to the connections to the target host of the
     * request, it does not refer to the maximum number of connections to a
     * proxy server. The maximum number of connections to a proxy server is
     * always unlimited.
     * 
     * @param maxConnections
     *            a number of connections allowed for each host:port, specify -1
     *            for an unlimited number of connections.
     */
    public static void setMaxConnectionsPerHost(int maxConnections)
    {
        if (_log.isDebugEnabled())
            _log.debug("setMaxConnectionsPerHost: " + maxConnections);
        checkConnectionManager()._globalState._maxConns = maxConnections;
    }

    /**
     * Get the maximum number of connections allowed for a given host:port.
     * 
     * @return The maximum number of connections allowed for a given host:port.
     */
    public static int getMaxConnectionsPerHost()
    {
        return checkConnectionManager()._globalState._maxConns;
    }

    /**
     * Set the option to include the HTTP 1.0 Keep-Alive headers in HTTP
     * requests. The default is that they are included.
     * 
     * @param use
     *            true if HTTP 1.0 Keep-Alive headers are included
     */
    public static void setUse10KeepAlive(boolean use)
    {
        checkConnectionManager()._globalState._use10KeepAlive = use;
    }

    /**
     * Get the option to include the HTTP 1.0 Keep-Alive headers in HTTP
     * requests.
     * 
     * @return true if HTTP 1.0 Keep-Alive headers are included
     */
    public static boolean getUse10KeepAlive()
    {
        return checkConnectionManager()._globalState._use10KeepAlive;
    }

    /**
     * Set the number of times an idempotent request is to be tried before
     * considering it a failure. This value defaults to 3. This also controls
     * the number of times a ping message is set after failure when the
     * idleConnectionPing is enabled. See setIdleConnectionPing().
     * 
     * @deprecated please use setDefaultMaxTries()
     * @param tries
     *            the number of times to try the request.
     */
    public static void setTries(int tries)
    {
        setDefaultMaxTries(tries);
    }

    /**
     * Set the number of times an idempotent request is to be tried before
     * considering it a failure. This value defaults to 3. This also controls
     * the number of times a ping message is set after failure when the
     * idleConnectionPing is enabled. See setIdleConnectionPing().
     * 
     * @param tries
     *            the number of times to try the request.
     */
    public static void setDefaultMaxTries(int tries)
        throws IllegalArgumentException
    {
        if (_log.isDebugEnabled())
            _log.debug("setDefaultMaxTries: " + tries);
        if (tries < 1)
            throw new IllegalArgumentException("You must allow at least one try");
        checkConnectionManager()._globalState._defaultMaxTries = tries;
    }

    /**
     * Get the number of times an idempotent request is tried.
     * 
     * @deprecated please use getDefaultMaxTries()
     * @return The number of times to try the request.
     */
    public static int getTries()
    {
        return getDefaultMaxTries();
    }

    /**
     * Get the number of times an idempotent request is tried.
     * 
     * @return The number of times to try the request.
     */
    public static int getDefaultMaxTries()
    {
        return checkConnectionManager()._globalState._defaultMaxTries;
    }

    /**
     * Set the number of times a request can be redirected or it can have its
     * authentication retried in the event of an authentication failure
     * (particularly in a pipelined environment).
     * 
     * This value defaults to 100.
     * 
     * @param forwards
     *            the number of times to redirect or retry authentication
     */
    public static void setDefaultMaxForwards(int forwards)
        throws IllegalArgumentException
    {
        if (_log.isDebugEnabled())
            _log.debug("setDefaultMaxForwards: " + forwards);
        if (forwards < 1)
            throw new IllegalArgumentException("You must allow at least one try");
        checkConnectionManager()._globalState._defaultMaxForwards = forwards;
    }

    /**
     * Get the number of of forwards and authentication retries allowed
     * 
     * @return The number of times to redirect or retry authentication.
     */
    public static int getDefaultMaxForwards()
    {
        return checkConnectionManager()._globalState._defaultMaxForwards;
    }

    /**
     * Set the interval to wait before each retry of a failed request. This
     * value defaults to 0 (milliseconds).
     * 
     * @param ms
     *            the interval to wait in milliseconds.
     */
    public static void setRetryInterval(int ms)
    {
        if (_log.isDebugEnabled())
            _log.debug("setRetryInterval: " + ms);
        checkConnectionManager()._globalState._retryInterval = ms;
    }

    /**
     * Get the interval to wait before each retry of a failed request.
     * 
     * @return The number of milliseconds to wait before retrying.
     */
    public static int getRetryInterval()
    {
        return checkConnectionManager()._globalState._retryInterval;
    }

    /**
     * Enable preemptive authentication for all connections.
     * <p>
     * This is by default disabled.
     * 
     * @deprecated
     * @param enabled
     *            true if enabled
     */
    public static void setPreemptiveAuthentication(boolean enabled)
    {
        if (_log.isDebugEnabled())
            _log.debug("setPreemptiveAuthentication: " + enabled);
        throw new IllegalArgumentException("This method is no longer supported.  "
            + "For preemptive authentication, use set[Proxy]AuthType().  Basic and Digest "
            + "authentication are always done preemptively.  NTLM cannot be done preemptively.");
    }

    /**
     * Get the value of preemptive authentication enablement for all
     * connections.
     * 
     * @deprecated
     * @return true if preemptive authentication is enabled.
     */
    public static boolean getPreemptiveAuthentication()
    {
        throw new IllegalArgumentException("This method is no longer supported.  "
            + "For preemptive authentication, use set[Proxy]AuthType().  Basic and Digest "
            + "authentication are always done preemptively.  NTLM cannot be done preemptively.");
    }

    protected static void checkAuthenticationType(int type)
    {
        if (type < 0 || type > Credential.AUTH_NTLM)
        {
            throw new IllegalArgumentException("Invalid authentication type specified: "
                + type
                + " see the Credential class for value values");
        }
    }

    /**
     * Sets the default authentication type.
     * <p>
     * The value of the authentication type is one of the values in the
     * Credential class (NTLM, BASIC, DIGEST).
     * 
     * @param type
     *            an integer, the default authentication type.
     * @see #setAuthenticationType
     */
    public static void setDefaultAuthenticationType(int type)
    {
        if (_log.isDebugEnabled())
            _log.debug("setDefaultAuthenticationType: " + type);
        checkAuthenticationType(type);
        checkConnectionManager()._globalState._defaultAuthenticationType = type;
    }

    /**
     * Gets the default authentication type.
     * 
     * @return an integer, the default authentication type.
     * @see #getAuthenticationType()
     */
    public static int getDefaultAuthenticationType()
    {
        return checkConnectionManager()._globalState._defaultAuthenticationType;
    }

    /**
     * Returns the authentication type associated with this connection.
     * 
     * @return an integer, the authentication type
     * @see #setAuthenticationType(int)
     */
    public int getAuthenticationType()
    {
        return _authenticationType[AUTH_NORMAL];
    }

    int getAuthenticationType(int normalOrProxy)
    {
        return _authenticationType[normalOrProxy];
    }

    /**
     * Sets the authentication type for this connection used for pipelining or
     * streaming.
     * <p>
     * When using pipelining with authentication, you must specify an
     * authentication type. When using streaming with authentication, you may
     * specify the authentication type to avoid a HttpRetryException. In either
     * case, specifying an authentication type forces the authentication process
     * to complete for the request (Basic or Digest) or connection (NTLM) before
     * the pipelining or streaming starts.
     * 
     * <p>
     * Authentication behaves as follows:
     * 
     * <ul>
     * <li>Basic - If you specify Credential.AUTH_BASIC, each request will be
     * preemptively authenticated with Basic Authentication (this is the same as
     * calling setPreemptiveAuthentication(true). This allows pipelining and
     * streaming to perform effectively.
     * <li>Digest - If you specify Credential.AUTH_DIGEST, each request must be
     * authenticated before pipelining may continue. This effectively removes
     * the advantage of pipelining, since each request has the unavoidable
     * digest round-trip, but can be used effectively with streaming.
     * <li>NTLM - If you specify Credential.AUTH_NTLM, authentication takes
     * place on only the first request for underlying socket connection (since
     * NTLM authentication is good for the life of the socket connection). Once
     * authentication is complete, streaming or pipelining can continue
     * effectively.
     * </ul>
     * 
     * @param authenticationType
     * @see #setPipelining(boolean)
     * @see #setChunkedStreamingMode(int)
     * @see #setFixedLengthStreamingMode(int)
     */
    public void setAuthenticationType(int authenticationType)
    {
        checkAuthenticationType(authenticationType);
        _authenticationType[AUTH_NORMAL] = authenticationType;
    }

    /**
     * Sets the default proxy authentication type.
     * <p>
     * The value of the authentication type is one of the values in the
     * Credential class (NTLM, BASIC, DIGEST).
     * 
     * @param type
     *            an integer, the default proxy authentication type.
     * @see #setProxyAuthenticationType
     */
    public static void setDefaultProxyAuthenticationType(int type)
    {
        if (_log.isDebugEnabled())
            _log.debug("setDefaultProxyAuthenticationType: " + type);
        checkAuthenticationType(type);
        checkConnectionManager()._globalState._defaultProxyAuthenticationType = type;
    }

    /**
     * Gets the default proxy authentication type.
     * 
     * @return an integer, the default proxy authentication type.
     * @see #getProxyAuthenticationType()
     */
    public static int getDefaultProxyAuthenticationType()
    {
        return checkConnectionManager()._globalState._defaultProxyAuthenticationType;
    }

    /**
     * Returns the proxy authentication type associated with this connection.
     * 
     * @return an integer, the proxy authentication type
     * @see #setProxyAuthenticationType(int)
     */
    public int getProxyAuthenticationType()
    {
        return _authenticationType[AUTH_PROXY];
    }

    /**
     * Sets the proxy authentication type for this connection.
     * <p>
     * Same as setAuthenticationType, except for a proxy.
     * 
     * @param authenticationType
     * @see #setAuthenticationType(int)
     */
    public void setProxyAuthenticationType(int authenticationType)
    {
        checkAuthenticationType(authenticationType);
        _authenticationType[AUTH_PROXY] = authenticationType;
    }

    public String getAuthenticationDummyContent()
    {
        return _authenticationDummyContent;
    }

    public void setAuthenticationDummyContent(String authenticationDummyContent)
    {
        _authenticationDummyContent = authenticationDummyContent;
    }

    public String getAuthenticationDummyMethod()
    {
        return _authenticationDummyMethod;
    }

    public void setAuthenticationDummyMethod(String authenticationDummyMethod)
    {
        _authenticationDummyMethod = authenticationDummyMethod;
    }

    /**
     * Enable pipelining for all connections. Pipelining allows multiple HTTP
     * requests to be sent in a single underlying socket connection before a
     * response is received. This can have a substantial performance benefit if
     * you are fetching multiple objects from the same server.
     * 
     * <p>
     * This is by default disabled.
     * 
     * @param enabled
     *            true if enabled
     */
    public static void setDefaultPipelining(boolean enabled)
    {
        if (_log.isDebugEnabled())
            _log.debug("setDefaultPipelining: " + enabled);
        if (enabled)
            checkConnectionManager()._globalState._defaultPipeliningOptions = PIPE_STANDARD_OPTIONS;
        else
            checkConnectionManager()._globalState._defaultPipeliningOptions = 0;
    }

    /**
     * Get the value of pipelining enablement for all connections.
     * 
     * @return true if pipelining is enabled.
     */
    public static boolean isDefaultPipelining()
    {
        return (checkConnectionManager()._globalState._defaultPipeliningOptions & PIPE_PIPELINE) != 0;
    }

    /**
     * Set specific pipeline options for all connections. This allows finer
     * control of the pipelining behavior.
     * 
     * @param pipeliningOptions -
     *            see the PIPE_ constants.
     * 
     */
    public static void setDefaultPipeliningOptions(int pipeliningOptions)
    {
        if (_log.isDebugEnabled())
        {
            _log.debug("setDefaultPipeliningOptions: "
                + plOptionsToString(pipeliningOptions));
        }

        checkConnectionManager()._globalState._defaultPipeliningOptions = pipeliningOptions;
    }

    /**
     * Get the value of pipelining options for all connections.
     * 
     * @return an int, the options
     */
    public static int getDefaultPipeliningOptions()
    {
        return checkConnectionManager()._globalState._defaultPipeliningOptions;
    }

    /**
     * Enable pipelining for this connection. Pipelining allows multiple HTTP
     * requests to be sent in a single underlying socket connection before a
     * response is received. This can have a substantial performance benefit if
     * you are fetching multiple objects from the same server.
     * <p>
     * By default, the following pipeline options are set: PIPE_PIPELINE and
     * PIPE_MAX_CONNECTIONS.
     * 
     * @param enabled
     *            true if enabled for this connection
     */
    public void setPipelining(boolean enabled)
    {
        if (enabled)
            setPipeliningOptions(PIPE_STANDARD_OPTIONS);
        else
            setPipeliningOptions(PIPE_NONE);
    }

    /**
     * Get the value of pipelining enablement for this connection.
     * 
     * @return true if pipelining is enabled.
     */
    public boolean isPipelining()
    {
        return (_pipeliningOptions & PIPE_PIPELINE) != 0;
    }

    /**
     * Returns a string representation of the specified pipelining options.
     * 
     * @param options
     * @return a String which is a text representation of the pipelining
     *         options.
     */
    public static String plOptionsToString(int options)
    {
        if (options == 0)
            return "PIPE_NONE";
        StringBuffer sb = new StringBuffer();
        if ((options & PIPE_PIPELINE) != 0)
            sb.append("PIPE_PIPELINE ");
        if ((options & PIPE_MAX_CONNECTIONS) != 0)
            sb.append("PIPE_MAX_CONNECTIONS ");
        return sb.toString();
    }

    /**
     * Set specific pipeline options for this connection. This allows finer
     * control of the pipelining behavior.
     * 
     * @param pipeliningOptions -
     *            see the PIPE_ constants.
     * 
     */
    public void setPipeliningOptions(int pipeliningOptions)
    {
        if (_log.isDebugEnabled())
        {
            _log.debug("setPipeliningOptions: "
                + plOptionsToString(pipeliningOptions));
        }

        _pipeliningOptions = pipeliningOptions;
    }

    /**
     * Get the value of pipelining options for this connection.
     * 
     * @return an int, the options
     */
    public int getPipeliningOptions()
    {
        return _pipeliningOptions;
    }

    /**
     * Sets the maximum depth of pipelining for all connections.
     * 
     * @see #setPipeliningMaxDepth(int)
     * 
     * @param pipeliningMaxDepth
     *            the maximum pipelining depth.
     * 
     */
    public static void setDefaultPipeliningMaxDepth(int pipeliningMaxDepth)
    {
        checkConnectionManager()._globalState._defaultPipeliningMaxDepth = pipeliningMaxDepth;
    }

    /**
     * Get the value of the maximum depth of pipelining for all connections.
     * 
     * @return an int, the maximum pipelining depth.
     */
    public static int getDefaultPipeliningMaxDepth()
    {
        return checkConnectionManager()._globalState._defaultPipeliningMaxDepth;
    }

    /**
     * Sets the maximum depth of pipelining for this connection.
     * <p>
     * The maximum depth is the number of outstanding HTTP requests that can be
     * active at any one time on a socket connection. If this is 0 then there is
     * no limit. Note that most of the time this is not necessary, as the
     * pipelining mechanism automatically senses the maximum number of HTTP
     * requests allowed on a socket connection before that connection is closed
     * by the server and uses this as the maximum depth. See
     * PIPE_USE_OBSERVED_CONN_LIMIT.
     * 
     * @param pipeliningMaxDepth
     *            the maximum pipelining depth.
     * 
     */
    public void setPipeliningMaxDepth(int pipeliningMaxDepth)
    {
        if (_log.isDebugEnabled())
        {
            _log.debug("setPipeliningMaxDepth: " + pipeliningMaxDepth);
        }
        _pipeliningMaxDepth = pipeliningMaxDepth;
    }

    /**
     * Get the value of the maximum depth of pipelining for this connection.
     * 
     * @return an int, the maximum pipelining depth.
     */
    public int getPipeliningMaxDepth()
    {
        return _pipeliningMaxDepth;
    }

    /**
     * Sets the Callback object for all connections. The specified Callback
     * object will be used for any connection opened after this is set.
     * <p>
     * Callback objects are used to handle notifications related to pipelining
     * or non-blocking I/O.
     * 
     * @param cb
     *            a Callback object.
     */
    public static void setDefaultCallback(Callback cb)
    {
        checkConnectionManager()._globalState._defaultCallback = cb;
    }

    /**
     * Returns the default Callback object.
     * 
     * @return a callback object
     */
    public static Callback getDefaultCallback()
    {
        return checkConnectionManager()._globalState._defaultCallback;
    }

    /**
     * Sets the Callback object associated with this connection.
     * <p>
     * Callback objects are used to handle notifications related to pipelining
     * or non-blocking I/O.
     * 
     * @param cb
     */
    public void setCallback(Callback cb)
    {
        _callback = cb;
    }

    /**
     * Gets the Callback object associated with this connection.
     * 
     * @return a Callback object
     */
    public Callback getCallback()
    {
        return _callback;
    }

    protected void checkPipeline()
    {
        if ((_pipeliningOptions & PIPE_PIPELINE) == 0)
        {
            throw new IllegalStateException("This HttpURLConnection is not enabled for pipelining, call setPipelining(true)");
        }

        if (_callback == null)
        {
            throw new IllegalStateException("This pipelined HttpURLConnection does not have a Callback");
        }
    }

    /**
     * Begins the pipelined execution of this connection.
     * 
     * You may subsequently call {@link #pipelineBlock()} to wait for all of the
     * pipelined connections you have executed on this thread.
     */
    public void pipelineExecute()
        throws InterruptedException,
            InterruptedIOException
    {
        if (_log.isDebugEnabled())
        {
            _log.debug("pipelineExecute: " + this);
        }

        checkPipeline();
        _connManager.addUrlConToRead(Thread.currentThread());
        _pipelineExpectBlock = true;
        // Execute this on the async thread, this way we can have
        // everything we need to do queued so which will help us
        // to manage how often to actually flush
        _connManager.enqueueAsyncUrlCon(this);
    }

    /**
     * Blocks until all pipelined HttpURLConnections are completed on this
     * thread.
     */
    public static void pipelineBlock() throws InterruptedException
    {
        checkConnectionManager().blockForUrlCons();
    }

    /**
     * Begins the pipelined execution of this connection, when you don't need to
     * block for completion of the connection or if this thread cannot tolerate
     * blocking.
     * <p>
     * This will never block for any reason; it moves the work of writing the
     * HTTP request to another thread.
     */
    public void pipelineExecuteAsync() throws InterruptedException
    {
        if (_log.isDebugEnabled())
        {
            _log.debug("pipelineExecuteAsync: " + this);
        }

        checkPipeline();
        _connManager.enqueueAsyncUrlCon(this);
    }

    // /**
    // * Specifies whether or not to use the dnsjava DNS name resolver. By
    // default
    // * the Java implementation is used.
    // *
    // * @param useDnsJava
    // * true to use the dnsjava DNS name resolver.
    // */
    // public static void setUseDnsJava(boolean useDnsJava)
    // {
    // _log.debug("setUseDnsJava: " + useDnsJava);
    // if (useDnsJava)
    // {
    // throw new RuntimeException("dnsjava is not currently supported. "
    // + "If you require this, please "
    // + "contact support@oaklandsoftware.com");
    // // Resolver.setMethod(Resolver.METHOD_DNSJAVA);
    // }
    // Resolver.setMethod(Resolver.METHOD_NATIVE_JAVA);
    // }
    //
    // /**
    // * Returns true if the dnsjava DNS name resolver is in use.
    // *
    // * @return true if the dnsjava DNS name resolver is in use.
    // */
    // public static boolean getUseDnsJava()
    // {
    // if (Resolver.getMethod() == Resolver.METHOD_DNSJAVA)
    // return true;
    // return false;
    // }

    /**
     * Sets the host to be used as a proxy server.
     * <p>
     * Note that when this is called, all existing connections are closed, since
     * the existing connections are no longer going to the desired destination.
     * 
     * @param host
     *            the name of the host to be used as a proxy server.
     */
    public static void setProxyHost(String host)
    {
        if (_log.isDebugEnabled())
            _log.debug("setProxyHost: " + host);
        checkConnectionManager()._globalState.setProxyHost(host);
    }

    /**
     * Returns the current value of the proxy server host.
     * 
     * @return the proxy host.
     */
    public static String getProxyHost()
    {
        return checkConnectionManager()._globalState._proxyHost;
    }

    /**
     * Sets the host to be used as a proxy server for this connection.
     * <p>
     * This must be called before the connection is connected.
     * 
     * @param host
     *            the name of the host to be used as a proxy server.
     */
    public void setConnectionProxyHost(String host)
    {
        if (_log.isDebugEnabled())
            _log.debug("setConnectionProxyHost: " + host);
        if (connected)
            throw new IllegalStateException("Connection has been established");
        _proxyHost = host;
    }

    /**
     * Returns the current value of the proxy server host on this connection.
     * 
     * @return the proxy host.
     */
    public String getConnectionProxyHost()
    {
        return _proxyHost;
    }

    /**
     * Sets the port on the proxy server host.
     * <p>
     * Note that when this is called, all existing connections are closed, since
     * the existing connections are no longer going to the desired destination.
     * 
     * @param port
     *            the port number of the proxy server.
     */
    public static void setProxyPort(int port)
    {
        if (_log.isDebugEnabled())
            _log.debug("setProxyPort: " + port);
        checkConnectionManager()._globalState.setProxyPort(port);
    }

    /**
     * Returns the current value of the proxy server port number.
     * 
     * @return the proxy port.
     */
    public static int getProxyPort()
    {
        return checkConnectionManager()._globalState._proxyPort;
    }

    /**
     * Sets the port on the proxy server host.
     * <p>
     * This must be called before the connection is connected.
     * 
     * @param port
     *            the port number of the proxy server.
     */
    public void setConnectionProxyPort(int port)
    {
        if (_log.isDebugEnabled())
            _log.debug("setConnectionProxyPort: " + port);
        if (connected)
            throw new IllegalStateException("Connection has been established");
        _proxyPort = port;
    }

    /**
     * Returns the current value of the proxy server port number.
     * 
     * @return the proxy port.
     */
    public int getConnectionProxyPort()
    {
        return _proxyPort;
    }

    /**
     * Sets the user to be used to authenticate with the proxy server. If the
     * proxy authenticates with NTLM, this value specifies both the domain name
     * and user name as follows: "domain\\user".
     * <p>
     * This is used only if no HTTPUserAgent is provided.
     * 
     * @param user
     *            the name of the user to be used with the proxy server.
     */
    public static void setProxyUser(String user)
    {
        if (_log.isDebugEnabled())
            _log.debug("setProxyUser: " + user);
        checkConnectionManager()._globalState._proxyUser = user;
    }

    /**
     * Returns the current value of the proxy server user.
     * 
     * @return the proxy user.
     */
    public static String getProxyUser()
    {
        return checkConnectionManager()._globalState._proxyUser;
    }

    /**
     * Sets the user to be used to authenticate with the proxy server.
     * <p>
     * This is used only if no HTTPUserAgent is provided.
     * <p>
     * This must be called before the connection is connected.
     * 
     * @param user
     *            the name of the host to be used as a proxy server.
     */
    public void setConnectionProxyUser(String user)
    {
        if (_log.isDebugEnabled())
            _log.debug("setConnectionProxyUser: " + user);
        if (connected)
            throw new IllegalStateException("Connection has been established");
        _proxyUser = user;
    }

    /**
     * Returns the current value of the proxy user on this connection.
     * 
     * @return the proxy user.
     */
    public String getConnectionProxyUser()
    {
        return _proxyUser;
    }

    /**
     * Sets the password to be used to authenticate with the proxy server for
     * this connection.
     * <p>
     * This is used only if no HTTPUserAgent is provided.
     * <p>
     * This must be called before the connection is connected.
     * 
     * @param password
     *            the name of the password to be used with the proxy server.
     */
    public static void setProxyPassword(String password)
    {
        if (_log.isDebugEnabled())
            _log.debug("setProxyPassword: " + password);
        checkConnectionManager()._globalState._proxyPassword = password;
    }

    /**
     * Returns the current value of the proxy password.
     * 
     * @return the proxy password.
     */
    public static String getProxyPassword()
    {
        return checkConnectionManager()._globalState._proxyPassword;
    }

    /**
     * Sets the password to be used to authenticate with the proxy server for
     * this connection.
     * <p>
     * This is used only if no HTTPUserAgent is provided.
     * <p>
     * This must be called before the connection is connected.
     * 
     * @param password
     *            the name of the host to be used as a proxy server.
     */
    public void setConnectionProxyPassword(String password)
    {
        if (_log.isDebugEnabled())
            _log.debug("setConnectionProxyPassword: " + password);
        if (connected)
            throw new IllegalStateException("Connection has been established");
        _proxyPassword = password;
    }

    /**
     * Returns the current value of the proxy server password on this
     * connection.
     * 
     * @return the proxy password.
     */
    public String getConnectionProxyPassword()
    {
        return _proxyPassword;
    }

    /**
     * Sets the hosts to be excluded from the proxy mechanism.
     * 
     * This applies to all connections whose proxy host is set using global
     * means (that is with a System.property or the static
     * {@link #setProxyHost(String)} method.
     * 
     * This does <i>not</i> apply if you set the proxy host on the connection
     * directly using {@link #setConnectionProxyHost(String)}. This allows you
     * to override the proxy setting for a connection without regard to the
     * setting of the global non-proxy hosts.
     * 
     * @param hosts
     *            a list of hosts separated by the pipe <code>("|")</code>
     *            character. Each host is a string containing either a hostname
     *            or IP address. The "*" character may be used within this
     *            string to provide wildcarding.
     * @see #isNonProxyHost(String)
     * 
     */
    public static void setNonProxyHosts(String hosts)
    {
        // It is validated by the connection manager
        if (_log.isDebugEnabled())
            _log.debug("setNonProxyHosts: " + hosts);
        checkConnectionManager()._globalState.setNonProxyHosts(hosts);
    }

    /**
     * Returns the current value of the hosts to not be accessed through the
     * proxy server.
     * 
     * @return the non-proxied hosts.
     */
    public static String getNonProxyHosts()
    {
        return checkConnectionManager()._globalState.getNonProxyHosts();
    }

    /**
     * Returns true if the specified host is treated as a non-proxy host.
     * 
     * @return true if the specified host is treated as a non-proxy host.
     */
    public static boolean isNonProxyHost(String hostName)
    {
        return checkConnectionManager()._globalState.isNonProxyHost(hostName);
    }

    /**
     * Close all pooled connections that are not currently in use.
     * <p>
     * HTTP requests that are currently in progress will be allowed complete
     * normally. The connections associated with those requests will be closed
     * when the connections complete.
     * 
     * @throws InterruptedException
     */
    public static void closeAllPooledConnections() throws InterruptedException
    {
        checkConnectionManager()
                .resetConnectionPool(!HttpConnectionManager.IMMEDIATE);
    }

    /**
     * Immediately close all connections and stop all work in progress.
     * 
     * @throws InterruptedException
     */
    public static void immediateShutdown() throws InterruptedException
    {
        checkConnectionManager()
                .resetConnectionPool(HttpConnectionManager.IMMEDIATE);

    }

    /**
     * Prints all pooled connections to System.out.
     */
    public static void dumpConnectionPool()
    {
        checkConnectionManager().dumpConnectionPool();
    }

    /**
     * Returns various statistics in printed form
     */
    public static String getStatistics()
    {
        return checkConnectionManager().getStatistics();
    }

    /**
     * Resets the statistics counters.
     */
    public static void resetStatistics()
    {
        checkConnectionManager().resetStatistics();
    }

    public static void dumpAll()
    {
        System.out.println(getStaticConfiguration());
        System.out.println(getStatistics());
        dumpConnectionPool();
    }

    public static String getStaticConfiguration()
    {
        StringBuffer sb = new StringBuffer();

        sb.append("Max connections/host:    ");
        sb.append(getMaxConnectionsPerHost());

        sb.append("\nPipelining options:    ");
        sb.append(plOptionsToString(getDefaultPipeliningOptions()));

        sb.append("\nPipelining max depth:  ");
        sb.append(getDefaultPipeliningMaxDepth());

        sb.append("\nAuthentication Type:   ");
        sb.append(HttpURLConnectInternal
                .authTypeToString(getDefaultAuthenticationType()));

        sb.append("\n");

        _log.debug(sb.toString());
        return sb.toString();
    }

    /**
     * Return the number of the try for this request.
     */
    int getActualTries()
    {
        return _tryCount;
    }

    /**
     * Returns the cipher suite associated with this connection.
     * 
     * @return the Cipher Suite associated with the SSL connection.
     */
    public String getCipherSuite()
    {
        if (!connected)
            throw new IllegalStateException("Connection has not been established");
        return _connection.getCipherSuite();
    }

    private static String _sslmethmsg = "This method is not available until JRE 1.4";

    /**
     * Returns the certificates(s) that were sent to the server when the
     * connection was established.
     * 
     * Note: this method does not work on implementations prior to 1.4.
     * 
     * @return the local Certificates associated with the SSL connection
     * @since JDK 1.4
     */
    public Certificate[] getLocalCertificates()
    {
        if (!connected)
            throw new IllegalStateException("Connection has not been established");
        return _connection.getLocalCertificates();
    }

    /**
     * Returns the server's certificate chain that was established when the
     * session was setup.
     * 
     * Note: this method does not work on implementations prior to 1.4.
     * 
     * @return the server Certificates associated with the SSL connection
     * @since JDK 1.4
     */
    public Certificate[] getServerCertificates()
        throws SSLPeerUnverifiedException
    {
        if (!connected)
            throw new IllegalStateException("Connection has not been established");
        return _connection.getServerCertificates();
    }

    /**
     * Returns the default hostname verifier used for SSL connections.
     * 
     * @return the default hostname verifier associated with the SSL connection
     */
    public static HostnameVerifier getDefaultHostnameVerifier()
    {
        return checkConnectionManager()._globalState._defaultHostnameVerifier;
    }

    /**
     * Sets the default hostname verifier used for SSL connections
     * 
     */
    public static void setDefaultHostnameVerifier(HostnameVerifier verifier)
    {
        checkConnectionManager()._globalState._defaultHostnameVerifier = verifier;
    }

    /**
     * Returns the hostname verifier used for this SSL connection.
     * 
     * @return the default hostname verifier associated with the SSL connection
     */
    public HostnameVerifier getHostnameVerifier()
    {
        return _hostnameVerifier;
    }

    /**
     * Sets the hostname verifier used for this SSL connection
     * 
     */
    public void setHostnameVerifier(HostnameVerifier verifier)
    {
        if (verifier != null && connected)
            throw new IllegalStateException("Connection has been established");
        _hostnameVerifier = verifier;
    }

    /**
     * Returns the default SSL socket factory used for SSL connections.
     */
    public static SSLSocketFactory getDefaultSSLSocketFactory()
    {
        return checkConnectionManager()._globalState._defaultSSLSocketFactory;
    }

    /**
     * Sets the default SSL socket factory used for SSL connections.
     */
    public static void setDefaultSSLSocketFactory(SSLSocketFactory factory)
    {
        checkConnectionManager()._globalState._defaultSSLSocketFactory = factory;
    }

    /**
     * Returns the SSL socket factory used this SSL connection.
     */
    public SSLSocketFactory getSSLSocketFactory()
    {
        return _sslSocketFactory;
    }

    /**
     * Sets the SSL socket factory used for this SSL connection.
     */
    public void setSSLSocketFactory(SSLSocketFactory factory)
    {
        _sslSocketFactory = factory;
    }

    /**
     * Sets the preferred encoding for NTLM authenticate messages. By default
     * the preferred encoding is Unicode, this method can be used to override
     * that. The preferred encoding is sent only if it is an allowed encoding as
     * indicated by the challenge message.
     */
    public static void setNtlmPreferredEncoding(int encoding)
    {
        switch (encoding)
        {
            case NTLM_ENCODING_OEM:
            case NTLM_ENCODING_UNICODE:
                break;
            default:
                throw new IllegalArgumentException("Encoding must be either NTLM_ENCODING_OEM or NTLM_ENCODING_DEFAULT");
        }
        checkConnectionManager()._globalState._ntlmPreferredEncoding = encoding;
    }

    /**
     * Gets the preferred encoding for NTLM messages.
     * 
     * @return an int, the preferred encoding.
     */
    public static int getNtlmPreferredEncoding()
    {
        return checkConnectionManager()._globalState._ntlmPreferredEncoding;
    }

    /**
     * Sets the properties for the specified method to the specified property
     * value.
     * 
     * @param methodName
     *            the name of the method, like GET or POST
     * @param properties
     *            the property value which is one or more (union) of the
     *            constants beginning METHOD_PROP_
     */
    public static void setMethodProperties(String methodName, int properties)
    {
        synchronized (_methodPropertyMap)
        {
            Integer propVal = (Integer)_methodPropertyMap.get(methodName);
            if (propVal != null)
                _methodPropertyMap.remove(methodName);
            _methodPropertyMap.put(methodName, new Integer(properties));
        }
    }

    protected int getMethodProperties(String methodName)
    {
        Integer propVal = (Integer)_methodPropertyMap.get(methodName);
        if (propVal == null)
            return METHOD_PROP_UNKNOWN_METHOD;
        return propVal.intValue();
    }

    String getUrlString()
    {
        return _urlString;
    }

    public String toString()
    {
        // Add the Util.id for better diagnostics if desired
        return /* Util.id(this) + " " + */_urlString;
    }

    static String normalOrProxyToString(int normalOrProxy)
    {
        if (normalOrProxy == AUTH_NORMAL)
            return "AUTH_NORMAL";
        return "AUTH_PROXY";
    }

    // Used only for the tests
    public static void resetUrlConReleased()
    {
        _urlConReleased = false;
    }

    /**
     * Resets the global persistent state. This is the state set by all of the
     * setDefaultxxx() methods as well as that set by the System.setProperty()
     * methods.
     */
    public static void resetGlobalState()
    {
        // The only time this gets created
        _globalState = new GlobalState();
        // Make sure we have a conn manager to start with
        _globalState._connManager = new HttpConnectionManager(_globalState);

        init();
    }

    protected abstract void execute() throws HttpException, IOException;

    protected abstract void executeStart() throws HttpException, IOException;

    // Returns true if it worked, false if processPipelinedWrite() should be
    // called
    protected abstract boolean processPipelinedWrite()
        throws InterruptedException,
            InterruptedIOException;

    // Returns true if it worked, false if processPipelinedWrite() should be
    // called
    protected abstract boolean processPipelinedRead()
        throws InterruptedException,
            InterruptedIOException;

    abstract void streamWriteFinished(boolean ok)
        throws IOException,
            InterruptedIOException;

    protected abstract void setUrl(String urlParam)
        throws MalformedURLException;

    protected abstract String getPathQuery();

    protected abstract boolean shouldCloseConnection();

    protected abstract void setupResponseBody() throws IOException;

}
