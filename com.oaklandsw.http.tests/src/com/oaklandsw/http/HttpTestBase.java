package com.oaklandsw.http;

import com.oaklandsw.http.server.ErrorServer;
import com.oaklandsw.http.servlet.ParamServlet;
import com.oaklandsw.http.servlet.TimeoutServlet;

import com.oaklandsw.util.Log;
import com.oaklandsw.util.LogUtils;
import com.oaklandsw.util.Util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.Socket;
import java.net.URL;


public class HttpTestBase extends com.oaklandsw.TestCaseBase {
    private static final Log _log = LogUtils.makeLogger();
    protected static String _urlBase = HttpTestEnv.TEST_URL_WEBAPP;
    public static final int STREAM_NONE = HttpURLConnection.STREAM_NONE;
    public static final int STREAM_CHUNKED = HttpURLConnection.STREAM_CHUNKED;
    public static final int STREAM_FIXED = HttpURLConnection.STREAM_FIXED;
    public static final int STREAM_RAW = HttpURLConnection.STREAM_RAW;

    // Constants for timeout tests
    protected static final int DEF = 1;
    protected static final int DEF_REQUEST = 2;
    protected static final int DEF_CONNECT = 3;
    protected static final int CONN = 4;
    protected static final int CONN_REQUEST = 5;
    protected static final int CONN_CONNECT = 6;

    // For error server debugging
    protected static String _errorDebug = System.getProperty("errorDebug", "");

    static {
        if (_errorDebug != "") {
            _errorDebug = "&debug=true";
        }
    }

    protected static final String context = System.getProperty("httpclient.test.webappContext",
            HttpTestEnv.TEST_URL_APP_TOMCAT_1);
    protected static boolean CHECK_CONTENT = true;
    public boolean _showStats;
    protected int _streamingType;

    // For fixed streaming
    protected int _streamingSize;

    // For more stress testing
    protected boolean _extended = false;

    // For logging
    protected boolean _logging = false;

    // Default setup if we are using pipelining. This is used by the
    // PipelineTest class
    protected int _pipelineOptions = HttpURLConnection.PIPE_STANDARD_OPTIONS;
    protected int _pipelineMaxDepth;
    protected String _testAllName;

    // Enables the https protocol test for all of the test methods
    // defined in allTestMethods()
    protected boolean _doHttps;
    protected boolean _inHttps;

    // Enables the proxy test for all of the test methods
    // defined in allTestMethods()
    protected boolean _doProxyTest;
    protected boolean _inProxyTest;

    // Enables the HTTP 1.0 proxy test for all of the test methods
    // defined in allTestMethods()
    protected boolean _do10ProxyTest;
    protected boolean _in10ProxyTest;

    // Enables the SOCKS test for all of the test methods
    // defined in allTestMethods()
    protected boolean _doSocksProxyTest;
    protected boolean _inSocksProxyTest;

    // Enables the ISA proxy test for all of the test methods
    // defined in allTestMethods()
    protected boolean _doIsaProxyTest;
    protected boolean _inIsaProxyTest;

    // Enables the ISA SSL proxy test for all of the test methods
    // defined in allTestMethods()
    protected boolean _doIsaSslProxyTest;
    protected boolean _inIsaSslProxyTest;

    // Enables the authentication proxy test for all of the test methods
    // defined in allTestMethods()
    protected boolean _doAuthProxyTest;
    protected boolean _inAuthProxyTest;

    // Enables the authentication close proxy test for all of the test methods
    // defined in allTestMethods()
    protected boolean _doAuthCloseProxyTest;
    protected boolean _inAuthCloseProxyTest;

    // Enables the explicit close test for all of the test methods
    // defined in allTestMethods()
    protected boolean _doExplicitTest;
    protected boolean _inExplicitTest;

    // Enables the useDnsJava for all of the test methods
    // defined in allTestMethods()
    protected boolean _doUseDnsJava;
    protected boolean _inUseDnsJava;

    // Tests everything inside of an applet
    protected boolean _doAppletTest;
    protected boolean _inAppletTest;

    // Curently executing one of the test sets above
    protected boolean _inTestGroup;
    protected final String UTF8 = "UTF-8";

    public HttpTestBase(String testName) {
        super(testName);
    }

    public void setUp() throws Exception {
        super.setUp();

        System.setProperty("java.protocol.handler.pkgs", "com.oaklandsw");

        HttpURLConnection.resetGlobalState();

        HttpURLConnection.setDefaultUserAgent(new TestUserAgent());

        HttpURLConnection.getConnectionManager().resetStatistics();
        Util.resetTest();
        TestUserAgent.resetTest();
        LogUtils.logNone();
    }

    public void tearDown() throws Exception {
        super.tearDown();
        LogUtils.logNone();

        _inTestGroup = false;

        if (!HttpURLConnection.getConnectionManager().checkEverythingEmpty()) {
            HttpURLConnection.dumpAll();
            Util.impossible("Connection manager not clean");
        }

        if (Util._impossibleException != null) {
            HttpURLConnection.dumpAll();
            Util.impossible("Impossible called: ", Util._impossibleException);
        }

        HttpURLConnectInternal._ignoreObservedMaxCount = false;

        HttpURLConnection.closeAllPooledConnections();
    }

    protected void setupDefaultTimeout(int type, int value) {
        switch (type) {
        case DEF:
            HttpURLConnection.setDefaultTimeout(value);

            break;

        case DEF_REQUEST:
            HttpURLConnection.setDefaultRequestTimeout(value);

            break;

        case DEF_CONNECT:
            HttpURLConnection.setDefaultConnectionTimeout(value);

            break;
        }
    }

    protected void setupConnTimeout(HttpURLConnection urlCon, int type,
        int value) {
        switch (type) {
        case CONN:
            urlCon.setTimeout(value);

            break;

        case CONN_REQUEST:
            urlCon.setRequestTimeout(value);

            break;

        case CONN_CONNECT:
            urlCon.setConnectionTimeout(value);

            break;
        }
    }

    protected void setupStreaming(HttpURLConnection urlCon, int size) {
        setupStreaming(_streamingType, urlCon, size);
    }

    public static void setupStreaming(int streamingType,
        HttpURLConnection urlCon, int size) {
        switch (streamingType) {
        case HttpURLConnection.STREAM_NONE:
            break;

        case HttpURLConnection.STREAM_CHUNKED:
            urlCon.setChunkedStreamingMode(size);

            break;

        case HttpURLConnection.STREAM_FIXED:
            urlCon.setFixedLengthStreamingMode(size);

            break;

        case HttpURLConnection.STREAM_RAW:
            urlCon.setRawStreamingMode(true);

            break;

        default:
            Util.impossible("Invalid streamingtype: " + streamingType);
        }
    }

    protected void writePostStart(OutputStream os, URL url, String data)
        throws Exception {
        if (_streamingType == STREAM_RAW) {
            String str = "POST " + url.toString() + " HTTP/1.1\r\n" +
                "Content-Length: " + data.length() + "\r\n" + "\r\n";

            os.write(str.getBytes());
        }
    }

    protected void writePostEnd(OutputStream os, URL url)
        throws Exception {
        if (_streamingType == STREAM_RAW) {
            String str = "\r\n";
            os.write(str.getBytes());
        }
    }

    protected boolean isInStreamTest() {
        return _streamingType != HttpURLConnection.STREAM_NONE;
    }

    protected void checkErrorSvrData(HttpURLConnection urlCon,
        boolean singleEolChar) throws Exception {
        int compareCl;

        if (singleEolChar) {
            compareCl = ErrorServer.CONTENT_LENGTH - ErrorServer.CONTENT_EOLS;
        } else {
            compareCl = ErrorServer.CONTENT_LENGTH;
        }

        if (urlCon.getHeaderField("Content-Length") != null) {
            // Make sure the content length header is OK
            int headerCl = Integer.parseInt(urlCon.getHeaderField(
                        "Content-Length"));
            assertEquals(compareCl, headerCl);
        }

        InputStream is = urlCon.getInputStream();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Util.copyStreams(is, os);

        assertEquals(compareCl, os.toByteArray().length);
        is.close();
    }

    // protected static String host = TestEnv.HOST;
    // protected static int port = TestEnv.PORT;
    public static String getReply(HttpURLConnection urlCon) {
        try {
            InputStream inStr = urlCon.getInputStream();
            String str = Util.getStringFromInputStream(inStr, null);
            inStr.close();

            return str;
        } catch (IOException e) {
            return null;
        }
    }

    // Returns false if failed
    public static boolean checkReplyNoAssert(String actualReply,
        String replyToCheck) {
        if (actualReply.toLowerCase().indexOf(replyToCheck.toLowerCase()) >= 0) {
            return true;
        }

        _log.debug("FAILED check reply: \n" + replyToCheck + "\n-----------\n" +
            actualReply);

        return false;
    }

    public static void checkReply(HttpURLConnection urlCon, String replyToCheck) {
        String reply = getReply(urlCon);

        if (!checkReplyNoAssert(reply, replyToCheck)) {
            fail("Reply text did not match - expected: " + replyToCheck +
                " got: " + reply);
        }
    }

    public String getServletIpAddress() throws Exception {
        URL url = new URL(_urlBase + ParamServlet.NAME);
        int response = 0;

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");
        urlCon.setRequestProperty(ParamServlet.HOST_IP_ADDRESS, "get");
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);
        urlCon.getInputStream().close();

        return urlCon.getHeaderField(ParamServlet.HOST_IP_ADDRESS);
    }

    public static String paramReply(String methodType) {
        return "<title>Param Servlet: " + methodType + "</title>";
    }

    public static HttpURLConnection doGetLikeMethod(String methodType,
        boolean checkContent) throws IOException {
        return doGetLikeMethod(_urlBase + ParamServlet.NAME, methodType,
            paramReply(methodType), checkContent);
    }

    public static HttpURLConnection doGetLikeMethod(String urlStr,
        String methodType, String replyText, boolean checkContent)
        throws IOException {
        URL url = new URL(urlStr);
        int response = 0;

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod(methodType);
        urlCon.connect();
        response = urlCon.getResponseCode();

        int lookFor = 200;

        // HTTP 1.0 does not seem to support OPTIONS
        if (urlCon.getHeaderField(0).startsWith("HTTP/1.0") &&
                methodType.equals("OPTIONS")) {
            lookFor = 404;
        }

        assertEquals(lookFor, response);

        if (checkContent) {
            checkReply(urlCon, replyText);
        } else {
            urlCon.getInputStream().close();
        }

        return urlCon;
    }

    public static void checkNoActiveConns(URL url) {
        _log.debug("checkNoActiveConns");

        int count = getActiveConns(url);

        if (count > 0) {
            HttpURLConnection.dumpAll();
        }

        assertEquals(0, count);
    }

    public static void checkNoTotalConns(URL url) {
        _log.debug("checkNoTotalConns");

        int count = getTotalConns(url);

        if (count > 0) {
            HttpURLConnection.dumpAll();
        }

        assertEquals(0, count);
    }

    public static int getActiveConns(URL url) {
        return HttpURLConnection.getConnectionManager()
                                .getActiveConnectionCount(url.toString());
    }

    public static int getTotalConns(URL url) {
        return HttpURLConnection.getConnectionManager()
                                .getTotalConnectionCount(url.toString());
    }

    protected void resetProxyParams() {
        HttpURLConnection.setProxyHost(null);
        HttpURLConnection.setProxyPort(-1);
        HttpURLConnection.setProxyUser(null);
        HttpURLConnection.setProxyPassword(null);
        HttpURLConnection.setDefaultProxyAuthenticationType(0);
        HttpURLConnection.setDefaultSocketFactory(new AbstractSocketFactory());
    }

    // Test everything as SSL
    public void testHttps() throws Exception {
        if (!_doHttps) {
            return;
        }

        _inHttps = true;
        _testAllName = "testHttps";
        _inTestGroup = true;
        HttpTestEnv.HTTP_PROTOCOL = "https:";

        try {
            allTestMethods();
        } finally {
            HttpTestEnv.HTTP_PROTOCOL = "http:";
            _inHttps = false;
        }
    }

    // Test everything through a proxy server
    public void testProxy() throws Exception {
        if (!_doProxyTest) {
            return;
        }

        _testAllName = "testProxy";
        _inProxyTest = true;
        _inTestGroup = true;
        HttpURLConnection.setProxyHost(HttpTestEnv.TEST_PROXY_HOST);
        HttpURLConnection.setProxyPort(HttpTestEnv.NORMAL_PROXY_PORT);

        // Bug 954 setProxyHost/setProxyPort had no effect
        URL url = new URL(_urlBase);
        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        assertEquals(HttpTestEnv.TEST_PROXY_HOST,
            urlCon.getConnectionProxyHost());
        assertEquals(HttpTestEnv.NORMAL_PROXY_PORT,
            urlCon.getConnectionProxyPort());

        try {
            allTestMethods();
        } finally {
            resetProxyParams();
            _inProxyTest = false;
        }
    }

    // Test everything through a HTTP 1.0 proxy server
    public void test10Proxy() throws Exception {
        if (!_do10ProxyTest) {
            return;
        }

        LogUtils.logAll();

        _testAllName = "test10Proxy";
        _in10ProxyTest = true;
        _inTestGroup = true;
        HttpURLConnection.setProxyHost(HttpTestEnv.TEST_10_PROXY_HOST);
        HttpURLConnection.setProxyPort(HttpTestEnv.TEST_10_PROXY_PORT);

        HttpURLConnection.setUse10KeepAlive(true);

        // Need more attempt to retry authentication in this environment
        HttpURLConnection.setDefaultMaxForwards(200);

        try {
            // LogUtils.logFile("/home/francis/log4j10proxy.txt");
            allTestMethods();
        } finally {
            resetProxyParams();
            _in10ProxyTest = false;
        }
    }

    // Test everything through a SOCKS proxy server
    // Bug 2180/977 - add support for SOCKS proxy
    public void testSocksProxy() throws Exception {
        if (!_doSocksProxyTest) {
            return;
        }

        _testAllName = "testSocksProxy";
        _inSocksProxyTest = true;
        _inTestGroup = true;

        // Note, this test depends on an external SOCKS proxy server more or
        // less randomly selected from the list pointed to by the Wikipedia
        // page. If this stops working for some reason, then we need to pick
        // another proxy server. The HttpTestEnv is where the proxy server
        // host/port are setup
        HttpURLConnection.setDefaultSocketFactory(new AbstractSocketFactory() {
                public Socket createSocket(HttpURLConnection urlCon,
                    String host, int port) {
                    // For 1.4.2 have to use these properties. Once support for
                    // 1.4.2 is dropped
                    // we can use the new Socket(java.net.Proxy.Type.SOCKS) method
                    System.setProperty("socksProxyHost",
                        HttpTestEnv.TEST_SOCKS_PROXY_HOST);
                    System.setProperty("socksProxyPort",
                        Integer.toString(HttpTestEnv.SOCKS_PROXY_PORT));

                    Socket s = new Socket();
                    System.out.println("created socks: " + s);

                    System.getProperties().remove("socksProxyHost");
                    System.getProperties().remove("socksProxyPort");

                    return s;
                }
            });

        try {
            // LogUtils.logFile("/home/francis/log4jSocksproxy.txt");
            allTestMethods();
        } finally {
            resetProxyParams();
            _inSocksProxyTest = false;
        }
    }

    // Test everything through a the ISA proxy
    public void testIsaProxy() throws Exception {
        if (!_doIsaProxyTest) {
            return;
        }

        _testAllName = "testIsaProxy";
        _inIsaProxyTest = true;
        _inTestGroup = true;

        TestUserAgent._proxyType = TestUserAgent.GOOD;

        HttpURLConnection.setProxyHost(HttpTestEnv.ISA_HOST);
        HttpURLConnection.setProxyPort(HttpTestEnv.ISA_PORT);

        try {
            // LogUtils.logFile("/home/francis/log4jIsaproxy.txt");
            allTestMethods();
        } finally {
            resetProxyParams();
            _inIsaProxyTest = false;
        }
    }

    // Bug 2199 support SSL proxy
    // Test everything through a the ISA proxy using SSL
    public void testIsaSslProxy() throws Exception {
        if (!_doIsaSslProxyTest) {
            return;
        }

        _testAllName = "testIsaSslProxy";
        _inIsaSslProxyTest = true;
        _inTestGroup = true;

        TestUserAgent._proxyType = TestUserAgent.GOOD;

        HttpURLConnection.setProxySsl(true);
        HttpURLConnection.setProxyHost(HttpTestEnv.ISA_HOST);
        HttpURLConnection.setProxyPort(HttpTestEnv.ISA_SSL_PORT);

        try {
            // LogUtils.logFile("/home/francis/log4jIsaSslproxy.txt");
            allTestMethods();
        } finally {
            resetProxyParams();
            _inIsaSslProxyTest = false;
        }
    }

    protected boolean isInAuthProxyTest() {
        return _inAuthProxyTest || _inAuthCloseProxyTest;
    }

    // True if this proxy supports NTLM. Apache as a proxy does not support
    // NTLM because it closes the connection in the middle of the
    // authentication. Netproxy also does not support it because it hangs.
    // Netproxy has a few problems: 1) It does not pass through
    // either a Connection: close or a Proxy-Connection: close
    // after the initial 401 message, and 2) it hangs after
    // the NTLM Authenticate request is sent in reading the
    // response.
    protected boolean isAllowNtlmProxy() {
        return (!isInAuthProxyTest() && !_inProxyTest) || _inIsaProxyTest ||
        _inIsaSslProxyTest;
    }

    protected boolean isInAnyProxy() {
        return _inProxyTest || _inAuthProxyTest || _in10ProxyTest ||
        _inIsaProxyTest || _inIsaSslProxyTest;
    }

    // Test everything through an authenticating proxy server (uses only basic)
    public void testAuthProxy() throws Exception {
        if (!_doAuthProxyTest) {
            return;
        }

        _testAllName = "testAuthProxy";
        _inAuthProxyTest = true;
        _inTestGroup = true;
        HttpURLConnection.setProxyHost(HttpTestEnv.TEST_AUTH_PROXY_HOST);
        HttpURLConnection.setProxyPort(HttpTestEnv.AUTH_PROXY_PORT);
        HttpURLConnection.setProxyUser(HttpTestEnv.TEST_AUTH_PROXY_USER);
        HttpURLConnection.setProxyPassword(HttpTestEnv.TEST_AUTH_PROXY_PASSWORD);
        HttpURLConnection.setDefaultProxyAuthenticationType(Credential.AUTH_BASIC);
        // Proxy type is used only for auth
        TestUserAgent._proxyType = TestUserAgent.PROXY;

        try {
            allTestMethods();
        } finally {
            resetProxyParams();
            _inAuthProxyTest = false;
        }
    }

    // Test everything through an authenticating proxy server (uses only basic)
    // that closes the connection after sending the 407 response.
    public void testAuthCloseProxy() throws Exception {
        if (!_doAuthCloseProxyTest) {
            return;
        }

        _testAllName = "testAuthCloseProxy";
        _inAuthCloseProxyTest = true;
        _inTestGroup = true;
        HttpURLConnection.setProxyHost(HttpTestEnv.TEST_AUTH_PROXY_CLOSE_HOST);
        HttpURLConnection.setProxyPort(HttpTestEnv.AUTH_PROXY_CLOSE_PORT);
        HttpURLConnection.setProxyUser(HttpTestEnv.TEST_AUTH_PROXY_CLOSE_USER);
        HttpURLConnection.setProxyPassword(HttpTestEnv.TEST_AUTH_PROXY_CLOSE_PASSWORD);
        HttpURLConnection.setDefaultProxyAuthenticationType(Credential.AUTH_BASIC);
        // Proxy type is used only for auth
        TestUserAgent._proxyType = TestUserAgent.NETPROXY;

        try {
            allTestMethods();
        } finally {
            resetProxyParams();
            _inAuthCloseProxyTest = false;
        }
    }

    public void testExplicitClose() throws Exception {
        if (!_doExplicitTest) {
            return;
        }

        _testAllName = "testExplicitClose";
        _inExplicitTest = true;
        _inTestGroup = true;

        int maxCon = HttpURLConnection.getMaxConnectionsPerHost();

        try {
            // Make sure we don't hang
            for (int i = 0; i < (maxCon + 2); i++) {
                allTestMethods();
            }
        } finally {
            _inExplicitTest = false;
        }
    }

    public void XXtestUseDnsJava() throws Exception {
        if (!_doUseDnsJava) {
            return;
        }

        _testAllName = "testDnsJava";
        _inUseDnsJava = true;
        _inTestGroup = true;

        try {
            // This feature is not supported for now
            if (false) {
                // HttpURLConnection.setUseDnsJava(true);
                allTestMethods();

                // HttpURLConnection.setUseDnsJava(false);
            }
        } finally {
            _inUseDnsJava = false;
        }
    }

    // TODO - right now this does not work, try it when testing on
    // a local machine
    public void XXtestApplet() throws Exception {
        if (!_doAppletTest) {
            return;
        }

        _testAllName = "testApplet";
        _inTestGroup = true;

        try {
            if (false) {
                sun.applet.AppletSecurity s = new sun.applet.AppletSecurity();
                System.setSecurityManager(s);
                allTestMethods();
                System.setSecurityManager(null);
            }
        } finally {
            _inAppletTest = false;
        }
    }

    protected void allTestMethods() throws Exception {
        // subclassed
    }

    public class OpenThread extends Thread {
        public String _url;

        public void run() {
            try {
                Thread.currentThread().setName("Timeout test OpenThread");

                URL url;

                if (_url == null) {
                    url = new URL(_urlBase + TimeoutServlet.NAME);
                } else {
                    url = new URL(_url);
                }

                HttpURLConnection urlCon = HttpURLConnection.openConnection(url);

                // Turn off the timeout for the connection since the
                // default timeout has been set in the main thread
                urlCon.setTimeout(0);
                urlCon.setRequestMethod("GET");
                urlCon.setRequestProperty("timeout", "4000");
                urlCon.getResponseCode();
                urlCon.getInputStream().close();
            } catch (Exception ex) {
                System.out.println("Unexpected exception: " + ex);
            }
        }
    }
}
