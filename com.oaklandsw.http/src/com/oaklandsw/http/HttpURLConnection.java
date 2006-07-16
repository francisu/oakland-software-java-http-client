//
// Copyright 2002-2006, oakland software, all rights reserved.
//
// May not be used or redistributed without specific written
// permission from oakland software.
//
package com.oaklandsw.http;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.cert.Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;

import com.oaklandsw.http.cookie.CookieSpec;
import com.oaklandsw.license.License;
import com.oaklandsw.license.LicenseManager;
import com.oaklandsw.license.LicensedCode;
import com.oaklandsw.log.Log;
import com.oaklandsw.log.LogFactory;
import com.oaklandsw.util.Util;

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
 * <code>com.oaklandsw.http.explicitClose</code>- requires the InputStream to
 * be obtained and closed explicitly. This is enabled if the value of this
 * property is set to anything. See setExplicitClose() for important details
 * about how to use this feature.
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
 * to wait before retrying an idempotent request. The default is 50ms. See
 * setRetryInterval().
 * <p>
 * <code>com.oaklandsw.http.preemptiveAuthentication</code>- set to any value
 * to enable preemtive authentication. The default is not set. See
 * setPreemptiveAuthentication().
 * <p>
 * <code>com.oaklandsw.http.userAgent</code>- set to specify an alternate
 * value for the User-Agent HTTP header. This should be used with caution as the
 * DEFAULT_USER_AGENT value contains values known to work correctly with
 * NTLM/IIS. The default is that the User-Agent header is set to
 * DEFAULT_USER_AGENT.
 * <p>
 * <code>com.oaklandsw.http.followRedirectsPost</code>- specifies that
 * redirect response codes are followed for a POST request. see
 * setFollowRedirectsPost() for further details. The default is to not follow
 * redirect response codes for post.
 * <p>
 * <code>com.oaklandsw.http.cookiePolicy</code>- specifies the default cookie
 * policy to be used. See CookiePolicy for the possible values.
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

    private static final Log          _log                              = LogFactory
                                                                                .getLog(HttpURLConnection.class);

    // Used by the tests to make sure we have the correct license type
    static int _licenseType;
    
    public static final String        HTTP_METHOD_GET                   = "GET";
    public static final String        HTTP_METHOD_POST                  = "POST";
    public static final String        HTTP_METHOD_PUT                   = "PUT";
    public static final String        HTTP_METHOD_OPTIONS               = "OPTIONS";
    public static final String        HTTP_METHOD_DELETE                = "DELETE";
    public static final String        HTTP_METHOD_HEAD                  = "HEAD";
    public static final String        HTTP_METHOD_TRACE                 = "TRACE";
    public static final String        HTTP_METHOD_CONNECT               = "CONNECT";

    public static final String        WEBDAV_METHOD_PROPFIND            = "PROPFIND";
    public static final String        WEBDAV_METHOD_PROPPATCH           = "PROPPATCH";
    public static final String        WEBDAV_METHOD_MKCOL               = "MKCOL";
    public static final String        WEBDAV_METHOD_COPY                = "COPY";
    public static final String        WEBDAV_METHOD_MOVE                = "MOVE";
    public static final String        WEBDAV_METHOD_DELETE              = "DELETE";
    public static final String        WEBDAV_METHOD_LOCK                = "LOCK";
    public static final String        WEBDAV_METHOD_UNLOCK              = "UNLOCK";

    public static final String        WEBDAV_METHOD_SEARCH              = "SEARCH";
    public static final String        WEBDAV_METHOD_VERSION_CONTROL     = "VERSION-CONTROL";
    public static final String        WEBDAV_METHOD_BASELINE_CONTROL    = "BASELINE-CONTROL";
    public static final String        WEBDAV_METHOD_REPORT              = "REPORT";
    public static final String        WEBDAV_METHOD_CHECKOUT            = "CHECKOUT";
    public static final String        WEBDAV_METHOD_CHECKIN             = "CHECKIN";
    public static final String        WEBDAV_METHOD_UNCHECKOUT          = "UNCHECKOUT";
    public static final String        WEBDAV_METHOD_MKWORKSPACE         = "MKWORKSPACE";
    public static final String        WEBDAV_METHOD_MERGE               = "MERGE";
    public static final String        WEBDAV_METHOD_UPDATE              = "UPDATE";
    public static final String        WEBDAV_METHOD_ACL                 = "ACL";

    /**
     * This method will be retried automatically.
     */
    public static final int           METHOD_PROP_RETRY                 = 0x0001;

    /**
     * This method will follow redirects.
     */
    public static final int           METHOD_PROP_REDIRECT              = 0x0002;

    /**
     * This is used for an HTTP GET method. If a GET method was specified, and
     * getOutputStream() is subsequently called, this changes the method to a
     * POST method. This is for JDK compatibility.
     */
    public static final int           METHOD_PROP_SWITCH_TO_POST        = 0x0004;

    /**
     * Add the content-length header if not already specified. Used for the POST
     * and PUT methods.
     */
    public static final int           METHOD_PROP_ADD_CL_HEADER         = 0x0008;

    /**
     * The response body is ignored. Used for the HEAD method.
     */
    public static final int           METHOD_PROP_IGNORE_RESPONSE_BODY  = 0x0010;

    /**
     * The request line has a URL (most HTTP methods)
     */
    public static final int           METHOD_PROP_REQ_LINE_URL          = 0x0020;

    /**
     * The request line consists of only a "*" (for OPTIONS)
     */
    public static final int           METHOD_PROP_REQ_LINE_STAR         = 0x0040;

    /**
     * The request line has only the host/port (CONNECT)
     */
    public static final int           METHOD_PROP_REQ_LINE_HOST_PORT    = 0x0080;

    /**
     * The content length value is calculated (for potentially adding a
     * content-length header) (POST/PUT).
     */
    public static final int           METHOD_PROP_CALCULATE_CONTENT_LEN = 0x0100;

    /**
     * A content-type header is automatically added (PUT). This is only used is
     * the METHOD_PROP_CALCULATE_CONTENT_LEN is also set.
     */
    public static final int           METHOD_PROP_SEND_CONTENT_TYPE     = 0x0200;

    /**
     * The connection is left open for this method (CONNECT)
     */
    public static final int           METHOD_PROP_LEAVE_OPEN            = 0x0400;

    /**
     * This is what the method properties are set to initially, this value
     * indicates no method was specified. If this is the case, then the GET
     * method is assumed.
     */
    public static final int           METHOD_PROP_UNSPECIFIED_METHOD    = 0x10000;

    /**
     * A method was specified, but is not known (in the table of methods)
     */
    public static final int           METHOD_PROP_UNKNOWN_METHOD        = 0x20000;

    static final String               HDR_USER_AGENT                    = "User-Agent";
    static final String               HDR_CONTENT_LENGTH                = "Content-Length";
    static final String               HDR_CONTENT_TYPE                  = "Content-Type";
    static final String               HDR_HOST                          = "Host";
    static final String               HDR_TRANSFER_ENCODING             = "Transfer-Encoding";
    static final String               HDR_LOCATION                      = "Location";
    static final String               HDR_EXPECT                        = "Expect";
    static final String               HDR_CONNECTION                    = "Connection";
    static final String               HDR_COOKIE                        = "Cookie";
    static final String               HDR_PROXY_CONNECTION              = "Proxy-Connection";

    static final String               HDR_VALUE_KEEP_ALIVE              = "keep-alive";
    static final String               HDR_VALUE_CLOSE                   = "close";

    public static final int           NTLM_ENCODING_UNICODE             = 1;
    public static final int           NTLM_ENCODING_OEM                 = 2;

    // Stores the properties associated with each method
    // K(Method name) V(Method property)
    protected static Map              _methodPropertyMap;

    protected int                     _methodProperties;

    protected boolean                 _followRedirects;

    /** Whether or not I should automatically processs authentication. */
    protected boolean                 _doAuthentication                 = true;

    protected boolean                 _executing;

    protected Headers                 _reqHeaders;
    protected Headers                 _respHeaders;
    protected Headers                 _respFooters;

    protected String                  _urlString;

    // The output stream contains the data to be submitted on the HTTP request
    protected ByteArrayOutputStream   _outStream;

    protected HttpConnection          _connection;

    // The streams associated with the connection we are bound to
    protected BufferedInputStream     _conInStream;
    protected BufferedOutputStream    _conOutStream;

    // The connection should be released when finished. This is
    // not the case for a CONNECT request
    protected boolean                 _releaseConnection;

    // The actual count of tries
    protected int                     _tryCount;

    // This is the stream from which the resonse is read
    protected InputStream             _responseStream;

    // Indicates there is no data in the response (for whatever reason)
    // this allows the connection to be released immediately (instead
    // of waiting for the getInputStream().close() on _explicitClose)
    protected boolean                 _responseIsEmpty;

    // The entire response, which was read from the
    // _responseStream. This is always used if the _explicitClose
    // option is not specified
    protected byte[]                  _responseBytes;

    protected static final int        MAX_RESPONSE_TEXT                 = 120;

    // This is populated as soon as the response code is known
    protected int                     _responseCode;

    protected char[]                  _responseText                     = new char[MAX_RESPONSE_TEXT];

    // Actual length of the response text
    protected int                     _responseTextLength;

    protected static HttpUserAgent    _defaultUserAgent;
    protected HttpUserAgent           _userAgent;

    // The request has been sent and the reply received
    protected boolean                 _executed;

    // If the connection died due to an I/O exception, it is recorded here.
    // If getResponseCode() is called after the connection is dead and we
    // threw somewhere else, we need to throw this same exception
    protected IOException             _ioException;

    // The request has failed
    protected boolean                 _dead;

    protected static final int        BAD_CONTENT_LENGTH                = -2;
    protected static final int        UNINIT_CONTENT_LENGTH             = -1;

    protected int                     _contentLength;

    protected boolean                 _hasContentLengthHeader;

    // If the "Expect" request header is present
    protected boolean                 _expectContinue;

    protected static int              _defaultConnectionTimeout;
    protected static int              _defaultRequestTimeout;

    protected int                     _connectionTimeout;
    protected int                     _requestTimeout;

    protected String                  _proxyHost;
    protected int                     _proxyPort;

    protected static int              _ntlmPreferredEncoding            = NTLM_ENCODING_UNICODE;

    protected static boolean          _explicitClose;

    // For tests
    public static final int           DEFAULT_IDLE_TIMEOUT              = 14000;
    public static final int           DEFAULT_IDLE_PING                 = 0;

    protected static int              _defaultIdleTimeout               = DEFAULT_IDLE_TIMEOUT;
    protected int                     _idleTimeout;

    protected static int              _defaultIdlePing                  = DEFAULT_IDLE_PING;
    protected int                     _idlePing;

    protected static final boolean    DEFAULT_USE_10_KEEPALIVE          = true;
    protected static boolean          _use10KeepAlive                   = DEFAULT_USE_10_KEEPALIVE;

    /**
     * The maximum number of attempts to attempt recovery from a recoverable
     * IOException.
     */
    public static int                 MAX_TRIES                         = 3;
    protected static int              _tries                            = MAX_TRIES;

    private static int                DEFAULT_RETRY_INTERVAL            = 50;
    protected static int              _retryInterval                    = DEFAULT_RETRY_INTERVAL;

    private static boolean            DEFAULT_PREEMPTIVE_AUTHENTICATION = false;
    protected static boolean          _preemptiveAuthentication         = DEFAULT_PREEMPTIVE_AUTHENTICATION;

    // Indicates some form of the SSL libraries are available
    public static boolean             _isSSLAvailable;

    // We use reflection for this since we need to support the JSSE libraries
    // that don't include this class, we compile on JDK 1.2
    static Class                      _hostnameVerifierClass;
    static Class                      _sslSessionClass;

    static Method                     _hostnameVerifierMethod;
    static Method                     _sslGetLocalCertMethod;
    static Method                     _sslGetServerCertMethod;

    protected static SSLSocketFactory _defaultSSLSocketFactory;
    protected SSLSocketFactory        _sslSocketFactory;

    protected static HostnameVerifier _defaultHostnameVerifier;
    protected HostnameVerifier        _hostnameVerifier;

    protected static CookieContainer  _defaultCookieContainer;
    protected CookieContainer         _cookieContainer;

    protected static CookieSpec       _defaultCookieSpec;
    protected CookieSpec              _cookieSpec;

    /** Whether or not I should use the HTTP/1.1 protocol. */
    protected boolean                 _http11                           = true;

    // Used only for testing purposes
    private static URL                _testURL;

    protected static String           USER_AGENT;

    // This userAgent string is necessary for NTLM/IIS
    public static String              DEFAULT_USER_AGENT                = "oaklandsoftware-HttpClient/"
                                                                            + Version.VERSION
                                                                            + " Mozilla/4.0 (compatible; "
                                                                            + "MSIE 6.0; Windows NT 5.0)";

    private static final String       EVAL_MESSAGE                      = "******\n******\n******\n******\n"
                                                                            + "******  This is an evaluation version.  To purchase go to www.oaklandsoftware.com.\n"
                                                                            + "******\n******\n******\n******\n";

    private static class DefaultHostnameVerifier implements HostnameVerifier
    {
        public boolean verify(String hostName, SSLSession session)
        {
            return false;
        }
    }

    static
    {
        _log.info("Oakland Software HttpURLConnection " + Version.VERSION);

        LicensedCode lc = new HttpClientLicensedCodeImpl();
        LicenseManager lm = new LicenseManager(lc);
        License lic = lm.licenseCheck();
        if (lic == null || lic.validate(lc) != License.VALID)
            throw new RuntimeException("License check failed");

        _licenseType = lic.getLicenseType();
        if (lic.getLicenseType() == License.LIC_EVALUATION)
        {
            System.out.println(EVAL_MESSAGE
                               + "\nExpires: "
                               + lic.getExpirationDate()
                               + "\n\n");
        }

        try
        {

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

            String timeoutStr = System
                    .getProperty("com.oaklandsw.http.timeout");
            if (timeoutStr != null)
            {
                try
                {
                    _defaultConnectionTimeout = Integer.parseInt(timeoutStr);
                    _defaultRequestTimeout = _defaultConnectionTimeout;
                    _log.info("Default timeout: " + _defaultConnectionTimeout);
                }
                catch (Exception ex)
                {
                    throw new RuntimeException("Invalid value specified for timeout: "
                        + timeoutStr);
                }
            }

            timeoutStr = System
                    .getProperty("com.oaklandsw.http.idleConnectionTimeout");
            if (timeoutStr != null)
            {
                try
                {
                    _defaultIdleTimeout = Integer.parseInt(timeoutStr);
                    _log.info("Default idle connection timeout: "
                        + _defaultIdleTimeout);
                }
                catch (Exception ex)
                {
                    throw new RuntimeException("Invalid value specified for idleConnectionTimeout: "
                        + timeoutStr);
                }
            }

            timeoutStr = System
                    .getProperty("com.oaklandsw.http.idleConnectionPing");
            if (timeoutStr != null)
            {
                try
                {
                    _defaultIdlePing = Integer.parseInt(timeoutStr);
                    _log.info("Default idle connection ping: "
                        + _defaultIdlePing);
                }
                catch (Exception ex)
                {
                    throw new RuntimeException("Invalid value specified for idleConnectionPing: "
                        + timeoutStr);
                }
            }

            String explicitStr = System
                    .getProperty("com.oaklandsw.http.explicitClose");
            if (explicitStr != null)
            {
                _log.info("Require explicit close");
                _explicitClose = true;
            }

            String followRedirects = System
                    .getProperty("com.oaklandsw.http.followRedirects");
            if (followRedirects != null
                && followRedirects.equalsIgnoreCase("false"))
            {
                _log.info("Turning OFF follow redirects");
                java.net.HttpURLConnection.setFollowRedirects(false);
            }

            String maxConStr = System
                    .getProperty("com.oaklandsw.http.maxConnectionsPerHost");
            if (maxConStr != null)
            {
                try
                {
                    setMaxConnectionsPerHost(Integer.parseInt(maxConStr));
                    _log.info("Max connections per host: " + maxConStr);
                }
                catch (Exception ex)
                {
                    throw new RuntimeException("Invalid value specified for maxConnectionsPerHost: "
                        + maxConStr);
                }
            }

            String triesStr = System.getProperty("com.oaklandsw.http.tries");
            if (triesStr != null)
            {
                try
                {
                    setTries(Integer.parseInt(triesStr));
                    _log.info("Number of tries: " + triesStr);
                }
                catch (Exception ex)
                {
                    throw new RuntimeException("Invalid value specified for tries: "
                        + triesStr);
                }
            }

            String retryIntervalStr = System
                    .getProperty("com.oaklandsw.http.retryInterval");
            if (retryIntervalStr != null)
            {
                try
                {
                    setRetryInterval(Integer.parseInt(retryIntervalStr));
                    _log.info("Number of retryInterval: " + retryIntervalStr);
                }
                catch (Exception ex)
                {
                    throw new RuntimeException("Invalid value specified for retryInterval: "
                        + retryIntervalStr);
                }
            }

            String preemptiveAuth = System
                    .getProperty("com.oaklandsw.http.preemptiveAuthentication");
            if (preemptiveAuth != null)
            {
                setPreemptiveAuthentication(true);
            }

            /*
             * String useDnsJavaStr =
             * System.getProperty("com.oaklandsw.http.useDnsJava"); if
             * (useDnsJavaStr != null) { try {
             * setUseDnsJava(Boolean.valueOf(useDnsJavaStr).booleanValue());
             * _log.info("useDnsJava: " + useDnsJavaStr); } catch (Exception ex) {
             * throw new RuntimeException( "Invalid value specified for
             * useDnsJava: " + useDnsJavaStr); } }
             */
            String hostProperty = "http.proxyHost";
            String portProperty = "http.proxyPort";
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
                _log.info("Proxy: " + getProxyHost() + ":" + getProxyPort());

            setNonProxyHosts(System.getProperty("http.nonProxyHosts"));
            if (getNonProxyHosts() != null)
                _log.info("Non proxy hosts: " + getNonProxyHosts());

            USER_AGENT = System.getProperties()
                    .getProperty("com.oaklandsw.http.userAgent");

            String cookiePolicy = System
                    .getProperty("com.oaklandsw.http.cookiePolicy");
            if (cookiePolicy != null)
            {
                _log.info("Default cookie policy: " + _defaultCookieSpec);
                // This validates the policy and throws if there is a problem
                _defaultCookieSpec = CookiePolicy.getCookieSpec(cookiePolicy);
            }

        }
        catch (SecurityException sex)
        {
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
            USER_AGENT = DEFAULT_USER_AGENT;

        initSSL();
    }

    private static void initSSL()
    {
        _log.debug("initSSL");
        // See if we have SSL at all
        try
        {
            Class.forName("javax.net.ssl.SSLSocketFactory");
            _log.debug("SSL is available");
            _isSSLAvailable = true;

            // Ok, we have it, see if we have the 1.4 stuff
            try
            {
                // Don't do this for now, unless we really have to, it just
                // adds complexity to testing
                // _hostnameVerifierClass =
                // Class.forName("javax.net.ssl.HostnameVerifier");
                // _log.debug("SSL 1.4 HostnameVerifier FOUND");
                _sslSessionClass = Class.forName("javax.net.ssl.SSLSession");
                _log.debug("SSL 1.4 Session class FOUND");
                try
                {
                    // _hostnameVerifierMethod =
                    // _hostnameVerifierClass.getDeclaredMethod
                    // ("verify", new Class[] { String.class, _sslSessionClass
                    // });
                    _sslGetLocalCertMethod = _sslSessionClass
                            .getDeclaredMethod("getLocalCertificates",
                                               new Class[] {});
                    _sslGetServerCertMethod = _sslSessionClass
                            .getDeclaredMethod("getPeerCertificates",
                                               new Class[] {});
                    _log.debug("SSL 1.4 cert methods FOUND");
                }
                catch (NoSuchMethodException cnf)
                {
                    _log
                            .debug("NOT FOUND - SSL 1.4 session cert/hostname verify methods",
                                   cnf);
                }
            }
            catch (ClassNotFoundException cnf)
            {
                _log.debug("SSL 1.4 implementation NOT FOUND", cnf);
            }

        }
        catch (ClassNotFoundException cnf)
        {
            _log.debug("SSL implementation not found", cnf);
        }

        catch (SecurityException sex)
        {
            _log.debug("Probably in applet: ", sex);
        }

        if (_isSSLAvailable)
        {
            _defaultSSLSocketFactory = (SSLSocketFactory)SSLSocketFactory
                    .getDefault();
            setDefaultHostnameVerifier(new DefaultHostnameVerifier());
        }

    }

    public HttpURLConnection()
    {
        this(_testURL);
    }

    /**
     * @see java.net.HttpURLConnection#HttpURLConnection()
     */
    public HttpURLConnection(URL urlParam)
    {
        super(urlParam);

        if (_log.isDebugEnabled())
            _log.debug("constructor - url: " + urlParam);

        // Get initial value from static
        setInstanceFollowRedirects(java.net.HttpURLConnection
                .getFollowRedirects());

        _methodProperties = METHOD_PROP_UNSPECIFIED_METHOD;

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

        _userAgent = _defaultUserAgent;

        _connectionTimeout = _defaultConnectionTimeout;
        _requestTimeout = _defaultRequestTimeout;
        _idleTimeout = _defaultIdleTimeout;
        _idlePing = _defaultIdlePing;
        _cookieContainer = _defaultCookieContainer;
        _cookieSpec = _defaultCookieSpec;
        _proxyHost = HttpConnectionManager.getProxyHost();
        _proxyPort = HttpConnectionManager.getProxyPort();

        if (_isSSLAvailable)
        {
            _sslSocketFactory = _defaultSSLSocketFactory;
            _hostnameVerifier = _defaultHostnameVerifier;
        }
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
        releaseConnection(CLOSE);
    }

    // Request side

    /**
     * @see java.net.HttpURLConnection#setRequestMethod(String)
     */
    public void setRequestMethod(String meth) throws ProtocolException
    {
        if (connected)
        {
            throw new ProtocolException("Can't reset method: already connected");
        }

        if (_connection != null)
        {
            throw new ProtocolException("getOutputStream() cannot be called "
                + " before setRequestMethod()");
        }

        setRequestMethodInternal(meth);
    }

    void setRequestMethodInternal(String meth) throws ProtocolException
    {
        // This validates the method
        // Do not call the superclass, as it checks the method name and
        // we don't want to restrict the method name to anything.
        // super.setRequestMethod(meth);

        _methodProperties = getMethodProperties(meth);
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
        return _reqHeaders.get(key);
    }

    /**
     * Set whether or not I should automatically follow HTTP redirects (status
     * code 302, etc.)
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
     * 302, etc.) Redirects are followed only for GET, POST, or HEAD requests.
     * 
     * @return <tt>true</tt> if I will automatically follow HTTP redirects
     */
    public final boolean getInstanceFollowRedirects()
    {
        if ((_methodProperties & METHOD_PROP_REDIRECT) != 0)
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
            throw new ProtocolException("cannot write to a URLConnection if doOutput=false "
                + "- call setDoOutput(true)");
        }

        if (_executed)
        {
            throw new ProtocolException("The reply to this URLConnection has already been received");
        }

        // Switch to post method - be compatible with JDK
        if ((_methodProperties & (METHOD_PROP_SWITCH_TO_POST | METHOD_PROP_UNSPECIFIED_METHOD)) != 0)
        {
            setRequestMethodInternal(HTTP_METHOD_POST);
        }

        // Be compatible with JDK
        // FIXME - not https?
        /***********************************************************************
         * if (!HTTP_METHOD_POST.equals(method) &&
         * !HTTP_METHOD_GET.equals(method) &&
         * "http".equalsIgnoreCase(url.getProtocol())) { throw new
         * ProtocolException("HTTP method " + method + " doesn't support
         * output"); }
         **********************************************************************/

        _outStream = new ByteArrayOutputStream();
        return _outStream;
    }

    /**
     * @see java.net.HttpURLConnection#usingProxy()
     */
    public boolean usingProxy()
    {
        if (HttpConnectionManager.getProxyHost(url.getHost()) != null)
            return true;
        return false;
    }

    protected static final boolean CLOSE = true;

    void releaseConnection(boolean close)
    {
        // _log.trace("releaseConnection");
        if (_connection != null)
        {
            if (_releaseConnection)
            {
                if (close || shouldCloseConnection())
                {
                    _log.debug("releaseConnection - closing the connection.");
                    _connection.close();
                }
                HttpConnectionManager.releaseConnection(_connection);
                _connection = null;
                _conInStream = null;
                _conOutStream = null;
                connected = false;
            }
            else
            {
                _log.debug("releaseConnection - no _release (CONNECT case).");
                // This happens on a CONNECT request when
                // the connection has already been closed,
                // just re-open the same connection using
                // the low-level interface
                // This also happens when the connection
                // was explicitly allocated
                if (close)
                {
                    try
                    {
                        _log.debug("releaseConnection - reopening after CLOSE");
                        _connection.close();
                        _connection.openSocket();
                        setStreams();
                        _log.debug("releaseConnection - WORKED");
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

    /**
     * @see java.net.HttpURLConnection#connect()
     */
    public void connect() throws IOException
    {
        // _log.trace("connect");

        // To be compatible with the Sun implementation
        if (connected)
            return;

        if (_connection != null)
            return;

        try
        {
            setConnection(HttpConnectionManager
                    .getConnection(_urlString,
                                   _connectionTimeout,
                                   _idleTimeout,
                                   _idlePing,
                                   _proxyHost,
                                   _proxyPort), RELEASE);
        }
        catch (MalformedURLException ex)
        {
            // This should never happen
            _log.error("Unexpected MalformedURLException (bug): ", ex);
        }
        catch (HttpTimeoutException tex)
        {
            releaseConnection(CLOSE);
            _log.error("HttpURL.getConn (timeout): ", tex);
            throw tex;
        }
        catch (IOException hex)
        {
            releaseConnection(CLOSE);
            _log.error("HttpURL.getConn: ", hex);
            throw hex;
        }
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
        if (_connection != null)
            throw new IllegalStateException("Connection already associated");

        setConnection(conn, !RELEASE);
    }

    static final boolean RELEASE = true;

    void setConnection(HttpConnection conn, boolean release) throws IOException
    {
        _connection = conn;
        _releaseConnection = release;

        _connection.setSoTimeout(_requestTimeout);
        _connection.setConnectTimeout(_connectionTimeout);

        if (!_connection.isOpen())
        {
            _log.debug("Opening the connection.");
            _connection.setSSLSocketFactory(_sslSocketFactory);
            _connection.setHostnameVerifier(_hostnameVerifier);
            _connection.open();
        }

        setStreams();
        connected = true;
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
        return new String(_responseText, 0, _responseTextLength);
    }

    /**
     * @see java.net.HttpURLConnection#getHeaderField(String)
     */
    public String getHeaderField(String name)
    {
        checkConnectNoThrow();

        return _respHeaders.get(name);
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

        return _respHeaders.getKey(keyPosition - 1);
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
                    : (" " + new String(_responseText, 0, _responseTextLength)));
        }

        return _respHeaders.get(position - 1);
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
        if (isConnected())
            throw new IllegalStateException("Connection has been established");
        _cookieContainer = container;
        _cookieSpec = CookiePolicy.getCookieSpec(policy);
    }

    public static void setDefaultCookieSupport(CookieContainer container,
                                               String policy)
    {
        _defaultCookieContainer = container;
        _defaultCookieSpec = CookiePolicy.getCookieSpec(policy);
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
        if (_executed)
            return getResponseStream();

        execute();

        InputStream is = getResponseStream();
        return is;
    }

    /**
     * Returns the data associated with the connection in the event of an error.
     * This returns data only when the respose code is >= 300. This should not
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
            return null;

        setupResponseBody();

        // _responseBytes contains the entire response; if this
        // has not been fully read (the non-explicit close case),
        // then return the stream.
        if (_responseBytes == null)
        {
            if (_responseStream != null)
                is = _responseStream;
            else
                is = new ByteArrayInputStream(new byte[0]);
        }
        else
        {
            is = new ByteArrayInputStream(_responseBytes);
        }

        if (_log.isDebugEnabled())
        {
            _log.debug("getResponseStream - returning: "
                + ((is == null) ? "null" : is.getClass().getName()));
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
        if (_connection != null)
        {
            // Here we make sure and return the connection
            // to get the counts adjusted
            _releaseConnection = true;
            releaseConnection(CLOSE);
        }
    }

    /**
     * Sets the default user agent.
     */
    public static void setDefaultUserAgent(HttpUserAgent userAgent)
    {
        _defaultUserAgent = userAgent;
    }

    /**
     * Gets the default user agent.
     */
    public static HttpUserAgent getDefaultUserAgent()
    {
        return _defaultUserAgent;
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
     * to be recieved. If this is not specified the default timeout value is
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
     *            milliseconds to wait for the sockect connection.
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
     *            milliseconds to wait for the respone.
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
        return _defaultConnectionTimeout;
    }

    /**
     * Set the default timeout value. This is the number of milliseconds to wait
     * for a connection to be established or a response to be recieved. Setting
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
        _log.debug("setDefaultTimeout: " + ms);
        _defaultConnectionTimeout = ms;
        _defaultRequestTimeout = ms;
    }

    /**
     * Return the default connection timeout value.
     * 
     * @return the default timeout value in milliseconds.
     */
    public static int getDefaultConnectionTimeout()
    {
        return _defaultConnectionTimeout;
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
        _log.debug("setDefaultConnectionTimeout: " + ms);
        _defaultConnectionTimeout = ms;
    }

    /**
     * Return the default request timeout value.
     * 
     * @return the default timeout value in milliseconds.
     */
    public static int getDefaultRequestTimeout()
    {
        return _defaultRequestTimeout;
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
        _log.debug("setDefaultRequestTimeout: " + ms);
        _defaultRequestTimeout = ms;
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
        return _defaultIdleTimeout;
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
        _log.debug("setDefaultIdleConnectionTimeout: " + ms);
        _defaultIdleTimeout = ms;
    }

    /**
     * Set the default idle connection timeout value to its default value (14
     * seconds).
     * 
     */
    public static void setDefaultIdleConnectionTimeout()
    {
        _log.debug("setDefaultIdleConnectionTimeout (Default): "
            + DEFAULT_IDLE_TIMEOUT);
        _defaultIdleTimeout = DEFAULT_IDLE_TIMEOUT;
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
        return _defaultIdlePing;
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
        _log.debug("setDefaultIdleConnectionPing: " + ms);
        _defaultIdlePing = ms;
    }

    /**
     * Set the default idle connection ping value to its default value (0
     * seconds).
     * 
     */
    public static void setDefaultIdleConnectionPing()
    {
        _log.debug("setDefaultIdleConnectionPing (Default): "
            + DEFAULT_IDLE_PING);
        _defaultIdlePing = DEFAULT_IDLE_PING;
    }

    /**
     * Returns true if an explicit close is required, false otherwise.
     * 
     * @return the value of the explicit close flag.
     */
    public static boolean getExplicitClose()
    {
        return _explicitClose;
    }

    /**
     * Sets all connections to require the InputStream to be obtained using a
     * call to getInputStream(), and for that stream to be closed. If this is
     * specified, this can result in better performance as the data associated
     * with the connection is not read unless the stream associated with the
     * connection is read. This is equivalent to setting the
     * <code>com.oaklandsw.http.explicitClose</code> property to anything.
     * 
     * <p>
     * Here are the rules for using this option:
     * <ul>
     * <li>In the event of an exception in a call to connect() you are not
     * required to do anything further with the connection.
     * <li>In <i>all other </i> cases, you must do a getResponseCode(),
     * getResponseMessage(), or getInputStream() when you use explicit close.
     * <li>In any case where the response code is > 300, or there is expected
     * to be an empty response (like a 204, Post/Put/Delete, etc), or the
     * response is in fact empty, you are <i>not </i> required to do a
     * getInputStream().close(). If any exception is thrown by one of the above
     * getXXX() methods, you are also not required to close the stream. In all
     * of these cases, the connection is released upon the return of the
     * getXXX() method.
     * <li>The only time you are required to close() the stream is when a
     * successful response actually returned data.
     * <li>Note that in the case of an empty response, say a 204, if you do a
     * getInputStream(), you will just get an empty ByteInputStream.
     * </ul>
     * 
     * @param explicitClose
     *            the value for explicit closing of the connection.
     */
    public static void setExplicitClose(boolean explicitClose)
    {
        _log.debug("setExplicitClose: " + explicitClose);
        _explicitClose = explicitClose;
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
        _log.debug("setMaxConnectionsPerHost: " + maxConnections);
        HttpConnectionManager.setMaxConnectionsPerHost(maxConnections);
    }

    /**
     * Get the maximum number of connections allowed for a given host:port.
     * 
     * @return The maximum number of connections allowed for a given host:port.
     */
    public static int getMaxConnectionsPerHost()
    {
        return HttpConnectionManager.getMaxConnectionsPerHost();
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
        _use10KeepAlive = use;
    }

    /**
     * Get the option to include the HTTP 1.0 Keep-Alive headers in HTTP
     * requests.
     * 
     * @return true if HTTP 1.0 Keep-Alive headers are included
     */
    public static boolean getUse10KeepAlive()
    {
        return _use10KeepAlive;
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
    public static void setTries(int tries) throws IllegalArgumentException
    {
        _log.debug("setTries: " + tries);
        if (tries < 1)
            throw new IllegalArgumentException("You must allow at least one try");
        _tries = tries;
    }

    /**
     * Get the number of times an idempotent request is tried.
     * 
     * @return The number of times to try the request.
     */
    public static int getTries()
    {
        return _tries;
    }

    /**
     * Set the interval to wait before each retry of a failed request. This
     * value defaults to 50 (milliseconds).
     * 
     * @param ms
     *            the interval to wait in milliseconds.
     */
    public static void setRetryInterval(int ms)
    {
        _log.debug("setRetryInterval: " + ms);
        _retryInterval = ms;
    }

    /**
     * Get the interval to wait before each retry of a failed request.
     * 
     * @return The number of milliseconds to wait before retrying.
     */
    public static int getRetryInterval()
    {
        return _retryInterval;
    }

    /**
     * Enable preemptive authentication. This is by default disabled.
     * 
     * @param enabled
     *            true if enabled
     */
    public static void setPreemptiveAuthentication(boolean enabled)
    {
        _log.debug("setPreemptiveAuthentication: " + enabled);
        _preemptiveAuthentication = enabled;
    }

    /**
     * Get the value of preemptive authentication enablement.
     * 
     * @return true if preemptive authentication is enabled.
     */
    public static boolean getPreemptiveAuthentication()
    {
        return _preemptiveAuthentication;
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
        _log.debug("setProxyHost: " + host);
        HttpConnectionManager.setProxyHost(host);
    }

    /**
     * Returns the current value of the proxy server host.
     * 
     * @return the proxy host.
     */
    public static String getProxyHost()
    {
        return HttpConnectionManager.getProxyHost();
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
        _log.debug("setConnectionProxyHost: " + host);
        if (_connection != null)
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
        _log.debug("setProxyPort: " + port);
        HttpConnectionManager.setProxyPort(port);
    }

    /**
     * Returns the current value of the proxy server port number.
     * 
     * @return the proxy port.
     */
    public static int getProxyPort()
    {
        return HttpConnectionManager.getProxyPort();
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
        _log.debug("setConnectionProxyPort: " + port);
        if (_connection != null)
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
     * Sets the hosts to be excluded from the proxy mechanism.
     * 
     * @param hosts
     *            a list of hosts separated by the pipe <code>("|")</code>
     *            character. Each host is a regular expression that is matched
     *            against the host being connected to. If the host matches the
     *            regular expression, it is not proxied.
     */
    public static void setNonProxyHosts(String hosts)
    {
        // It is validated by the connection manager
        _log.debug("setNonProxyHosts: " + hosts);
        HttpConnectionManager.setNonProxyHosts(hosts);
    }

    /**
     * Returns the current value of the hosts to not be accessed through the
     * proxy server.
     * 
     * @return the non-proxied hosts.
     */
    public static String getNonProxyHosts()
    {
        return HttpConnectionManager.getNonProxyHosts();
    }

    /**
     * Close all pooled connections that are not currently in use.
     */
    public static void closeAllPooledConnections()
    {
        HttpConnectionManager.resetConnectionPool();
    }

    /**
     * Prints all pooled connections to System.out.
     */
    public static void dumpConnectionPool()
    {
        HttpConnectionManager.dumpConnectionPool();
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
        if (_connection == null)
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
        if (_connection == null)
            throw new IllegalStateException("Connection has not been established");
        if (_sslGetLocalCertMethod == null)
            throw new IllegalStateException(_sslmethmsg);
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
        if (_connection == null)
            throw new IllegalStateException("Connection has not been established");
        if (_sslGetServerCertMethod == null)
            throw new IllegalStateException(_sslmethmsg);
        return _connection.getServerCertificates();
    }

    /**
     * Returns the default hostname verifier used for SSL connections.
     * 
     * @return the default hostname verifier associated with the SSL connection
     */
    public static HostnameVerifier getDefaultHostnameVerifier()
    {
        return _defaultHostnameVerifier;
    }

    /**
     * Sets the default hostname verifier used for SSL connections
     * 
     */
    public static void setDefaultHostnameVerifier(HostnameVerifier verifier)
    {
        _defaultHostnameVerifier = verifier;
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
        if (verifier != null && _connection != null)
            throw new IllegalStateException("Connection has been established");
        _hostnameVerifier = verifier;
    }

    /**
     * Returns the default SSL socket factory used for SSL connections.
     */
    public static SSLSocketFactory getDefaultSSLSocketFactory()
    {
        return _defaultSSLSocketFactory;
    }

    /**
     * Sets the default SSL socket factory used for SSL connections.
     */
    public static void setDefaultSSLSocketFactory(SSLSocketFactory factory)
    {
        _defaultSSLSocketFactory = factory;
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
        _ntlmPreferredEncoding = encoding;
    }

    /**
     * Gets the preferred encoding for NTLM messages.
     * 
     * @return an int, the preferred encoding.
     */
    public static int getNtlmPreferredEncoding()
    {
        return _ntlmPreferredEncoding;
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

    protected abstract void execute() throws HttpException, IOException;

    protected abstract void setUrl(String urlParam)
        throws MalformedURLException;

    protected abstract String getPathQuery();

    protected abstract boolean shouldCloseConnection();

    protected abstract void setupResponseBody() throws IOException;

}
