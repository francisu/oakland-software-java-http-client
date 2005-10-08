package com.oaklandsw.http.webapp;

import java.net.URL;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.HttpURLConnection;
import com.oaklandsw.http.TestEnv;
import com.oaklandsw.http.servlet.HeaderServlet;
import com.oaklandsw.log.Log;
import com.oaklandsw.log.LogFactory;

public class TestHttp10 extends TestWebappBase
{

    private static final Log _log = LogFactory.getLog(TestHttp10.class);

    public TestHttp10(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestHttp10.class);
        return suite;
    }

    public static void main(String args[])
    {
        mainRun(suite(), args);
    }

    // Bug 974
    // Bug 975
    // Bug 980
    
    // Test HTTP 1.0 keep-alive - header
    public void test10KeepAliveHeader() throws Exception
    {
        URL url = new URL(_urlBase + HeaderServlet.NAME);

        com.oaklandsw.http.HttpURLConnection urlCon = (com.oaklandsw.http.HttpURLConnection)url
                .openConnection();
        urlCon.getResponseCode();

        String conName;
        if (urlCon.getConnectionProxyHost() != null)
        {
            // The servlet will not see the proxy-connection header the actual
            // proxy case since it is removed by the proxy
            conName = "proxy-connection";
            assertFalse(checkReplyNoAssert(getReply(urlCon), "name=\""
                                           + conName
                                           + "\";value=\"keep-alive\"<br>"));
        }
        else
        {
            conName = "connection";
            checkReply(urlCon, "name=\"" + conName + "\";value=\"keep-alive\"<br>");
        }

        if (com.oaklandsw.http.HttpURLConnection.getExplicitClose())
            urlCon.getInputStream().close();
        checkNoActiveConns(url);
    }

    // Test HTTP 1.0 keep-alive disabled - header
    public void testNo10KeepAliveHeader() throws Exception
    {
        URL url = new URL(_urlBase + HeaderServlet.NAME);

        com.oaklandsw.http.HttpURLConnection.setUse10KeepAlive(false);

        com.oaklandsw.http.HttpURLConnection urlCon = (com.oaklandsw.http.HttpURLConnection)url
                .openConnection();
        urlCon.getResponseCode();

        String conName = "connection";
        if (urlCon.getConnectionProxyHost() != null)
            conName = "proxy-connection";

        assertFalse(checkReplyNoAssert(getReply(urlCon), "name=\""
            + conName
            + "\";value=\"keep-alive\"<br>"));

        if (com.oaklandsw.http.HttpURLConnection.getExplicitClose())
            urlCon.getInputStream().close();
        checkNoActiveConns(url);

        // Re-enable it
        com.oaklandsw.http.HttpURLConnection.setUse10KeepAlive(true);
    }

    // Test Squid proxy keeps connections alive
    // Don't include this in allTestMethods() it's only used for the 1.0 proxy
    public void test10ProxyKeepAlive() throws Exception
    {
        HttpURLConnection.setProxyHost(TestEnv.TEST_10_PROXY_HOST);
        HttpURLConnection.setProxyPort(TestEnv.TEST_10_PROXY_PORT);

        URL url = new URL(_urlBase + HeaderServlet.NAME);

        checkNoTotalConns(url);

        HttpURLConnection urlCon = (com.oaklandsw.http.HttpURLConnection)url
                .openConnection();
        urlCon.getResponseCode();

        // Should keep the connection open
        assertEquals(1, getTotalConns(url));

        // Now disconnect it
        urlCon = (com.oaklandsw.http.HttpURLConnection)url.openConnection();
        urlCon.connect();
        urlCon.disconnect();
        
        checkNoActiveConns(url);
        
        HttpURLConnection.setProxyHost(null);
        HttpURLConnection.setProxyPort(-1);

    }

    // Test Squid proxy does not keeps connections alive if our keep-alive is disabled
    // Don't include this in allTestMethods() it's only used for the 1.0 proxy
    public void test10ProxyNoKeepAlive() throws Exception
    {
        HttpURLConnection.setProxyHost(TestEnv.TEST_10_PROXY_HOST);
        HttpURLConnection.setProxyPort(TestEnv.TEST_10_PROXY_PORT);

        com.oaklandsw.http.HttpURLConnection.setUse10KeepAlive(false);

        URL url = new URL(_urlBase + HeaderServlet.NAME);
        HttpURLConnection urlCon = (com.oaklandsw.http.HttpURLConnection)url
                .openConnection();
        urlCon.getResponseCode();

        // The connection should be closed by tghe proxy
        checkNoActiveConns(url);
        
        com.oaklandsw.http.HttpURLConnection.setUse10KeepAlive(true);

        HttpURLConnection.setProxyHost(null);
        HttpURLConnection.setProxyPort(-1);

    }

    
    public void allTestMethods() throws Exception
    {
        test10KeepAliveHeader();
    }

}
