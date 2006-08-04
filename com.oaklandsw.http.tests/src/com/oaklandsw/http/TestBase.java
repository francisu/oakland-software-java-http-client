package com.oaklandsw.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;

import com.oaklandsw.http.server.ErrorServer;
import com.oaklandsw.http.servlet.ParamServlet;
import com.oaklandsw.http.servlet.TimeoutServlet;
import com.oaklandsw.util.Util;

public class TestBase extends com.oaklandsw.TestCaseBase
{

    private static final Log   _log         = LogFactory.getLog(TestBase.class);

    protected static String    _urlBase     = TestEnv.TEST_URL_TOMCAT;

    // Enables the https protocol test for all of the test methods
    // defined in allTestMethods()
    protected boolean          _doHttps;

    // Enables the proxy test for all of the test methods
    // defined in allTestMethods()
    protected boolean          _doProxyTest;

    // Enables the HTTP 1.0 proxy test for all of the test methods
    // defined in allTestMethods()
    protected boolean          _do10ProxyTest;

    // Enables the authentication proxy test for all of the test methods
    // defined in allTestMethods()
    protected boolean          _doAuthProxyTest;

    // Enables the authentication close proxy test for all of the test methods
    // defined in allTestMethods()
    protected boolean          _doAuthCloseProxyTest;

    // Enables the explicit close test for all of the test methods
    // defined in allTestMethods()
    protected boolean          _doExplicitTest;

    // Enables the useDnsJava for all of the test methods
    // defined in allTestMethods()
    protected boolean          _doUseDnsJava;

    // Tests everything inside of an applet
    protected boolean          _doAppletTest;

    protected final String     UTF8         = "UTF-8";

    // Constants for timeout tests
    protected static final int DEF          = 1;
    protected static final int DEF_REQUEST  = 2;
    protected static final int DEF_CONNECT  = 3;
    protected static final int CONN         = 4;
    protected static final int CONN_REQUEST = 5;
    protected static final int CONN_CONNECT = 6;

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

                // Only valid for oaklandsw implementation
                if (!(urlCon instanceof com.oaklandsw.http.HttpURLConnection))
                    return;

                // Turn off the timeout for the connection since the
                // default timeout has been set in the main thread
                ((com.oaklandsw.http.HttpURLConnection)urlCon).setTimeout(0);
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
        TestEnv.setUp();
    }

    public void tearDown() throws Exception
    {
        super.tearDown();

        com.oaklandsw.http.HttpURLConnection.setDefaultTimeout(0);
        com.oaklandsw.http.HttpURLConnection.setDefaultConnectionTimeout(0);
        com.oaklandsw.http.HttpURLConnection.setDefaultRequestTimeout(0);
        com.oaklandsw.http.HttpURLConnection.setExplicitClose(false);
        com.oaklandsw.http.HttpURLConnection
                .setTries(com.oaklandsw.http.HttpURLConnection.MAX_TRIES);

        com.oaklandsw.http.HttpURLConnection
                .setDefaultIdleConnectionTimeout(com.oaklandsw.http.HttpURLConnection.DEFAULT_IDLE_TIMEOUT);
        com.oaklandsw.http.HttpURLConnection
                .setDefaultIdleConnectionPing(com.oaklandsw.http.HttpURLConnection.DEFAULT_IDLE_PING);
        com.oaklandsw.http.HttpURLConnection
                .setMaxConnectionsPerHost(HttpConnectionManager.DEFAULT_MAX_CONNECTIONS);
        com.oaklandsw.http.HttpURLConnection.closeAllPooledConnections();

    }

    protected void setupDefaultTimeout(int type, int value)
    {
        switch (type)
        {
            case DEF:
                com.oaklandsw.http.HttpURLConnection.setDefaultTimeout(value);
                break;
            case DEF_REQUEST:
                com.oaklandsw.http.HttpURLConnection
                        .setDefaultRequestTimeout(value);
                break;
            case DEF_CONNECT:
                com.oaklandsw.http.HttpURLConnection
                        .setDefaultConnectionTimeout(value);
                break;
        }
    }

    protected void setupConnTimeout(com.oaklandsw.http.HttpURLConnection urlCon,
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

    public TestBase(String testName)
    {
        super(testName);
    }

    protected static final String context = System
                                                  .getProperty("httpclient.test.webappContext",
                                                               TestEnv.TEST_URL_APP_TOMCAT);

    protected static String       host    = TestEnv.HOST;
    protected static int          port    = TestEnv.PORT;

    public static void setLogging(boolean on)
    {
        Properties logProps = new Properties();
        if (on)
            logProps.setProperty("log4j.logger.com.oaklandsw", "DEBUG");
        else
            logProps.setProperty("log4j.logger.com.oaklandsw", "FATAL");
        PropertyConfigurator.configure(logProps);
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
            if (com.oaklandsw.http.HttpURLConnection.getExplicitClose())
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
        return com.oaklandsw.http.HttpURLConnection.getConnectionManager()
                .getActiveConnectionCount(url.toString());
    }

    public static int getTotalConns(URL url)
    {
        return com.oaklandsw.http.HttpURLConnection.getConnectionManager()
                .getTotalConnectionCount(url.toString());
    }

    // Test everything through a proxy server
    public void testHttps() throws Exception
    {
        if (!_doHttps)
            return;

        System.out.println("https test");
        TestEnv._protocol = "https:";
        allTestMethods();
        TestEnv._protocol = "http:";
    }

    // Test everything through a proxy server
    public void testProxy() throws Exception
    {
        if (!_doProxyTest)
            return;

        com.oaklandsw.http.HttpURLConnection
                .setProxyHost(TestEnv.TEST_PROXY_HOST);
        com.oaklandsw.http.HttpURLConnection
                .setProxyPort(TestEnv.TEST_PROXY_PORT);

        // Bug 954 setProxyHost/setProxyPort had no effect
        URL url = new URL(_urlBase);
        com.oaklandsw.http.HttpURLConnection urlCon = (com.oaklandsw.http.HttpURLConnection)url
                .openConnection();
        assertEquals(TestEnv.TEST_PROXY_HOST, urlCon.getConnectionProxyHost());
        assertEquals(TestEnv.TEST_PROXY_PORT, urlCon.getConnectionProxyPort());

        allTestMethods();
        com.oaklandsw.http.HttpURLConnection.setProxyHost(null);
        com.oaklandsw.http.HttpURLConnection.setProxyPort(-1);

    }

    // Test everything through a HTTP 1.0 proxy server
    public void test10Proxy() throws Exception
    {
        if (!_do10ProxyTest)
            return;

        com.oaklandsw.http.HttpURLConnection
                .setProxyHost(TestEnv.TEST_10_PROXY_HOST);
        com.oaklandsw.http.HttpURLConnection
                .setProxyPort(TestEnv.TEST_10_PROXY_PORT);
        allTestMethods();
        com.oaklandsw.http.HttpURLConnection.setProxyHost(null);
        com.oaklandsw.http.HttpURLConnection.setProxyPort(-1);

    }

    // Test everything through a proxy server
    public void testAuthProxy() throws Exception
    {
        if (!_doAuthProxyTest)
            return;

        com.oaklandsw.http.HttpURLConnection
                .setProxyHost(TestEnv.TEST_AUTH_PROXY_HOST);
        com.oaklandsw.http.HttpURLConnection
                .setProxyPort(TestEnv.TEST_AUTH_PROXY_PORT);
        // Proxy type is used only for auth
        TestUserAgent._proxyType = TestUserAgent.PROXY;
        allTestMethods();
        com.oaklandsw.http.HttpURLConnection.setProxyHost(null);
        com.oaklandsw.http.HttpURLConnection.setProxyPort(-1);

    }

    // Test everything through a proxy server
    public void testAuthCloseProxy() throws Exception
    {
        if (!_doAuthCloseProxyTest)
            return;

        com.oaklandsw.http.HttpURLConnection
                .setProxyHost(TestEnv.TEST_AUTH_PROXY_CLOSE_HOST);
        com.oaklandsw.http.HttpURLConnection
                .setProxyPort(TestEnv.TEST_AUTH_PROXY_CLOSE_PORT);
        // Proxy type is used only for auth
        TestUserAgent._proxyType = TestUserAgent.NETPROXY;
        allTestMethods();
        com.oaklandsw.http.HttpURLConnection.setProxyHost(null);
        com.oaklandsw.http.HttpURLConnection.setProxyPort(-1);

    }

    public void testExplicitClose() throws Exception
    {
        if (!_doExplicitTest)
            return;

        com.oaklandsw.http.HttpURLConnection.setExplicitClose(true);

        int maxCon = com.oaklandsw.http.HttpURLConnection
                .getMaxConnectionsPerHost();

        // Make sure we don't hang
        for (int i = 0; i < maxCon + 5; i++)
        {
            allTestMethods();
        }

        com.oaklandsw.http.HttpURLConnection.setExplicitClose(false);

    }

    public void testUseDnsJava() throws Exception
    {
        // FIXME - we don't support this for now
        /**
         * if (!_doUseDnsJava) return;
         * 
         * com.oaklandsw.http.HttpURLConnection.setUseDnsJava(true);
         * allTestMethods();
         * com.oaklandsw.http.HttpURLConnection.setUseDnsJava(false);
         */

    }

    // TODO - right now this does not work, try it when testing on
    // a local machine
    public void testApplet() throws Exception
    {
        if (!_doAppletTest)
            return;
        //        
        // System.setSecurityManager(new sun.applet.AppletSecurity());
        // allTestMethods();
        // System.setSecurityManager(null);
    }

    protected void allTestMethods() throws Exception
    {
        // subclassed
    }

}
