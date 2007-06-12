package com.oaklandsw.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.logging.Log;

import com.oaklandsw.http.server.ErrorServer;
import com.oaklandsw.http.servlet.ParamServlet;
import com.oaklandsw.http.servlet.TimeoutServlet;
import com.oaklandsw.util.LogUtils;
import com.oaklandsw.util.Util;

public class HttpTestBase extends com.oaklandsw.TestCaseBase
{

    private static final Log   _log             = LogUtils.makeLogger();

    protected static String    _urlBase         = HttpTestEnv.TEST_URL_WEBAPP;

    public boolean             _showStats;

    // For more stress testing
    protected boolean          _extended        = false;

    // For logging
    protected boolean          _logging         = false;

    // Default setup if we are using pipelining. This is used by the
    // PipelineTest class
    protected int              _pipelineOptions = HttpURLConnection.PIPE_STANDARD_OPTIONS;
    protected int              _pipelineMaxDepth;

    protected String           _testAllName;

    // Enables the https protocol test for all of the test methods
    // defined in allTestMethods()
    protected boolean          _doHttps;
    protected boolean          _inHttps;

    // Enables the proxy test for all of the test methods
    // defined in allTestMethods()
    protected boolean          _doProxyTest;
    protected boolean          _inProxyTest;

    // Enables the HTTP 1.0 proxy test for all of the test methods
    // defined in allTestMethods()
    protected boolean          _do10ProxyTest;
    protected boolean          _in10ProxyTest;

    // Enables the authentication proxy test for all of the test methods
    // defined in allTestMethods()
    protected boolean          _doAuthProxyTest;
    protected boolean          _inAuthProxyTest;

    // Enables the authentication close proxy test for all of the test methods
    // defined in allTestMethods()
    protected boolean          _doAuthCloseProxyTest;
    protected boolean          _inAuthCloseProxyTest;

    // Enables the explicit close test for all of the test methods
    // defined in allTestMethods()
    protected boolean          _doExplicitTest;
    protected boolean          _inExplicitTest;

    // Enables the useDnsJava for all of the test methods
    // defined in allTestMethods()
    protected boolean          _doUseDnsJava;
    protected boolean          _inUseDnsJava;

    // Tests everything inside of an applet
    protected boolean          _doAppletTest;
    protected boolean          _inAppletTest;

    // Curently executing one of the test sets above
    protected boolean          _inTestGroup;

    protected final String     UTF8             = "UTF-8";

    // Constants for timeout tests
    protected static final int DEF              = 1;
    protected static final int DEF_REQUEST      = 2;
    protected static final int DEF_CONNECT      = 3;
    protected static final int CONN             = 4;
    protected static final int CONN_REQUEST     = 5;
    protected static final int CONN_CONNECT     = 6;

    public class OpenThread extends Thread
    {

        public String _url;

        public void run()
        {
            try
            {
                Thread.currentThread().setName("Timeout test OpenThread");
                URL url;
                if (_url == null)
                    url = new URL(_urlBase + TimeoutServlet.NAME);
                else
                    url = new URL(_url);

                HttpURLConnection urlCon = (HttpURLConnection)url
                        .openConnection();

                // Turn off the timeout for the connection since the
                // default timeout has been set in the main thread
                urlCon.setTimeout(0);
                urlCon.setRequestMethod("GET");
                urlCon.setRequestProperty("timeout", "4000");
                urlCon.getResponseCode();
            }
            catch (Exception ex)
            {
                System.out.println("Unexpected exception: " + ex);
            }
        }
    }

    public void setUp() throws Exception
    {
        super.setUp();
        HttpTestEnv.setUp();

        HttpURLConnection.getConnectionManager().resetStatistics();
        Util.resetTest();
    }

    public void tearDown() throws Exception
    {
        super.tearDown();
        LogUtils.logNone();

        _inTestGroup = false;

        if (!HttpURLConnection.getConnectionManager().checkEverythingEmpty())
        {
            HttpURLConnection.dumpAll();
            Util.impossible("Connection manager not clean");
        }

        if (Util._impossibleException != null)
        {
            HttpURLConnection.dumpAll();
            Util.impossible("Impossible called: ", Util._impossibleException);
        }

        HttpURLConnection.setDefaultTimeout(0);
        HttpURLConnection.setDefaultConnectionTimeout(0);
        HttpURLConnection.setDefaultRequestTimeout(0);
        HttpURLConnection.setDefaultAuthenticationType(0);
        HttpURLConnection.setPreemptiveAuthentication(false);
        HttpURLConnection.setDefaultExplicitClose(false);
        HttpURLConnection.setDefaultMaxTries(HttpURLConnection.MAX_TRIES);
        HttpURLConnection.setDefaultPipelining(false);

        HttpURLConnection
                .setDefaultIdleConnectionTimeout(HttpURLConnection.DEFAULT_IDLE_TIMEOUT);
        HttpURLConnection
                .setDefaultIdleConnectionPing(HttpURLConnection.DEFAULT_IDLE_PING);
        HttpURLConnection
                .setMaxConnectionsPerHost(HttpConnectionManager.DEFAULT_MAX_CONNECTIONS);
        HttpURLConnection.setDefaultPipelining(false);
        HttpURLConnection.closeAllPooledConnections();
    }

    protected void setupDefaultTimeout(int type, int value)
    {
        switch (type)
        {
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

    protected void setupConnTimeout(HttpURLConnection urlCon,
                                    int type,
                                    int value)
    {
        switch (type)
        {
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

    protected void checkErrorSvrData(HttpURLConnection urlCon,
                                     boolean singleEolChar) throws Exception
    {
        int compareCl;
        if (singleEolChar)
            compareCl = ErrorServer.CONTENT_LENGTH - ErrorServer.CONTENT_EOLS;
        else
            compareCl = ErrorServer.CONTENT_LENGTH;

        if (urlCon.getHeaderField("Content-Length") != null)
        {
            // Make sure the content length header is OK
            int headerCl = Integer.parseInt(urlCon
                    .getHeaderField("Content-Length"));
            assertEquals(compareCl, headerCl);
        }

        InputStream is = urlCon.getInputStream();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Util.copyStreams(is, os);

        assertEquals(compareCl, os.toByteArray().length);
        is.close();
    }

    // For error server debugging
    protected static String _errorDebug = System.getProperty("errorDebug", "");

    static
    {
        if (_errorDebug != "")
            _errorDebug = "&debug=true";
    }

    public HttpTestBase(String testName)
    {
        super(testName);
    }

    protected static final String context = System
                                                  .getProperty("httpclient.test.webappContext",
                                                               HttpTestEnv.TEST_URL_APP_TOMCAT);

    // protected static String host = TestEnv.HOST;
    // protected static int port = TestEnv.PORT;

    public static void setLogging(boolean on)
    {
        if (on)
            LogUtils.logAll();
        else
            LogUtils.logNone();
    }

    public static String getReply(HttpURLConnection urlCon)
    {
        try
        {
            InputStream inStr = urlCon.getInputStream();
            String str = Util.getStringFromInputStream(inStr, null);
            inStr.close();
            return str;
        }
        catch (IOException e)
        {
            return null;
        }

    }

    // Returns false if failed
    public static boolean checkReplyNoAssert(String actualReply,
                                             String replyToCheck)
    {
        _log.debug("check reply: \n"
            + replyToCheck
            + "\n-----------\n"
            + actualReply);

        if (actualReply.toLowerCase().indexOf(replyToCheck.toLowerCase()) >= 0)
            return true;
        return false;
    }

    public static void checkReply(HttpURLConnection urlCon, String replyToCheck)
    {
        String reply = getReply(urlCon);
        if (!checkReplyNoAssert(reply, replyToCheck))
        {
            fail("Reply text did not match - expected: "
                + replyToCheck
                + " got: "
                + reply);
        }
    }

    public String getServletIpAddress() throws Exception
    {
        URL url = new URL(_urlBase + ParamServlet.NAME);
        int response = 0;

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("GET");
        urlCon.setRequestProperty(ParamServlet.HOST_IP_ADDRESS, "get");
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);
        return urlCon.getHeaderField(ParamServlet.HOST_IP_ADDRESS);
    }

    public static String paramReply(String methodType)
    {
        return "<title>Param Servlet: " + methodType + "</title>";
    }

    protected static boolean CHECK_CONTENT = true;

    public static HttpURLConnection doGetLikeMethod(String methodType,
                                                    boolean checkContent)
        throws IOException
    {
        return doGetLikeMethod(_urlBase + ParamServlet.NAME,
                               methodType,
                               paramReply(methodType),
                               checkContent);
    }

    public static HttpURLConnection doGetLikeMethod(String urlStr,
                                                    String methodType,
                                                    String replyText,
                                                    boolean checkContent)
        throws IOException
    {
        URL url = new URL(urlStr);
        int response = 0;

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod(methodType);
        urlCon.connect();
        response = urlCon.getResponseCode();

        int lookFor = 200;

        // HTTP 1.0 does not seem to support OPTIONS
        if (urlCon.getHeaderField(0).startsWith("HTTP/1.0")
            && methodType.equals("OPTIONS"))
            lookFor = 404;

        assertEquals(lookFor, response);

        if (checkContent)
        {
            checkReply(urlCon, replyText);
        }
        else
        {
            if (urlCon.isExplicitClose())
                urlCon.getInputStream().close();
        }

        return urlCon;
    }

    public static void checkNoActiveConns(URL url)
    {
        assertEquals(0, getActiveConns(url));
    }

    public static void checkNoTotalConns(URL url)
    {
        assertEquals(0, getTotalConns(url));
    }

    public static int getActiveConns(URL url)
    {
        return HttpURLConnection.getConnectionManager()
                .getActiveConnectionCount(url.toString());
    }

    public static int getTotalConns(URL url)
    {
        return HttpURLConnection.getConnectionManager()
                .getTotalConnectionCount(url.toString());
    }

    protected void resetProxyParams()
    {
        HttpURLConnection.setProxyHost(null);
        HttpURLConnection.setProxyPort(-1);
        HttpURLConnection.setProxyUser(null);
        HttpURLConnection.setProxyPassword(null);
        HttpURLConnection.setDefaultProxyAuthenticationType(0);
    }

    // Test everything as SSL
    public void testHttps() throws Exception
    {
        if (!_doHttps)
            return;

        _inHttps = true;
        _testAllName = "testHttps";
        _inTestGroup = true;
        HttpTestEnv.HTTP_PROTOCOL = "https:";
        try
        {
            allTestMethods();
        }
        finally
        {
            HttpTestEnv.HTTP_PROTOCOL = "http:";
            _inHttps = false;
        }
    }

    // Test everything through a proxy server
    public void testProxy() throws Exception
    {
        if (!_doProxyTest)
            return;

        _testAllName = "testProxy";
        _inProxyTest = true;
        _inTestGroup = true;
        HttpURLConnection.setProxyHost(HttpTestEnv.TEST_PROXY_HOST);
        HttpURLConnection.setProxyPort(HttpTestEnv.TEST_PROXY_PORT);

        // Bug 954 setProxyHost/setProxyPort had no effect
        URL url = new URL(_urlBase);
        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        assertEquals(HttpTestEnv.TEST_PROXY_HOST, urlCon
                .getConnectionProxyHost());
        assertEquals(HttpTestEnv.TEST_PROXY_PORT, urlCon
                .getConnectionProxyPort());

        try
        {
            allTestMethods();
        }
        finally
        {
            resetProxyParams();
            _inProxyTest = false;
        }
    }

    // Test everything through a HTTP 1.0 proxy server
    public void test10Proxy() throws Exception
    {
        if (!_do10ProxyTest)
            return;

        _testAllName = "test10Proxy";
        _in10ProxyTest = true;
        _inTestGroup = true;
        HttpURLConnection.setProxyHost(HttpTestEnv.TEST_10_PROXY_HOST);
        HttpURLConnection.setProxyPort(HttpTestEnv.TEST_10_PROXY_PORT);
        try
        {
            allTestMethods();
        }
        finally
        {
            resetProxyParams();
            _in10ProxyTest = false;
        }
    }

    protected boolean isInAuthProxyTest()
    {
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
    protected boolean isAllowNtlmProxy()
    {
        return !isInAuthProxyTest() && !_inProxyTest;
    }

    // Test everything through an authenticating proxy server (uses only basic)
    public void testAuthProxy() throws Exception
    {
        if (!_doAuthProxyTest)
            return;

        _testAllName = "testAuthProxy";
        _inAuthProxyTest = true;
        _inTestGroup = true;
        HttpURLConnection.setProxyHost(HttpTestEnv.TEST_AUTH_PROXY_HOST);
        HttpURLConnection.setProxyPort(HttpTestEnv.TEST_AUTH_PROXY_PORT);
        HttpURLConnection.setProxyUser(HttpTestEnv.TEST_AUTH_PROXY_USER);
        HttpURLConnection
                .setProxyPassword(HttpTestEnv.TEST_AUTH_PROXY_PASSWORD);
        HttpURLConnection
                .setDefaultProxyAuthenticationType(Credential.AUTH_BASIC);
        // Proxy type is used only for auth
        TestUserAgent._proxyType = TestUserAgent.PROXY;

        try
        {
            allTestMethods();
        }
        finally
        {
            resetProxyParams();
            _inAuthProxyTest = false;
        }
    }

    // Test everything through an authenticating proxy server (uses only basic)
    // that closes the connection after sending the 407 response.
    public void testAuthCloseProxy() throws Exception
    {
        if (!_doAuthCloseProxyTest)
            return;

        _testAllName = "testAuthCloseProxy";
        _inAuthCloseProxyTest = true;
        _inTestGroup = true;
        HttpURLConnection.setProxyHost(HttpTestEnv.TEST_AUTH_PROXY_CLOSE_HOST);
        HttpURLConnection.setProxyPort(HttpTestEnv.TEST_AUTH_PROXY_CLOSE_PORT);
        HttpURLConnection.setProxyUser(HttpTestEnv.TEST_AUTH_PROXY_CLOSE_USER);
        HttpURLConnection
                .setProxyPassword(HttpTestEnv.TEST_AUTH_PROXY_CLOSE_PASSWORD);
        HttpURLConnection
                .setDefaultProxyAuthenticationType(Credential.AUTH_BASIC);
        // Proxy type is used only for auth
        TestUserAgent._proxyType = TestUserAgent.NETPROXY;
        try
        {
            allTestMethods();
        }
        finally
        {
            resetProxyParams();
            _inAuthCloseProxyTest = false;
        }
    }

    public void testExplicitClose() throws Exception
    {
        if (!_doExplicitTest)
            return;

        _testAllName = "testExplicitClose";
        _inExplicitTest = true;
        _inTestGroup = true;
        HttpURLConnection.setDefaultExplicitClose(true);

        int maxCon = HttpURLConnection.getMaxConnectionsPerHost();

        try
        {
            // Make sure we don't hang
            for (int i = 0; i < maxCon + 5; i++)
            {
                allTestMethods();
            }
        }
        finally
        {
            HttpURLConnection.setDefaultExplicitClose(false);
            _inExplicitTest = false;
        }

    }

    public void testUseDnsJava() throws Exception
    {
        if (!_doUseDnsJava)
            return;

        _testAllName = "testDnsJava";
        _inUseDnsJava = true;
        _inTestGroup = true;
        try
        {
            // This feature is not supported for now
            if (false)
            {
                // HttpURLConnection.setUseDnsJava(true);
                allTestMethods();
                // HttpURLConnection.setUseDnsJava(false);
            }
        }
        finally
        {
            _inUseDnsJava = false;
        }

    }

    // TODO - right now this does not work, try it when testing on
    // a local machine
    public void testApplet() throws Exception
    {
        if (!_doAppletTest)
            return;

        _testAllName = "testApplet";
        _inTestGroup = true;
        try
        {
            if (false)
            {
                System.setSecurityManager(new sun.applet.AppletSecurity());
                allTestMethods();
                System.setSecurityManager(null);
            }
        }
        finally
        {
            _inAppletTest = false;
        }
    }

    protected void allTestMethods() throws Exception
    {
        // subclassed
    }

}
