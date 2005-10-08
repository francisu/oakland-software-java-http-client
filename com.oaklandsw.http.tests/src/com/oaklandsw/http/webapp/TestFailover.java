package com.oaklandsw.http.webapp;

import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.HttpConnectionManager;
import com.oaklandsw.http.TestEnv;
import com.oaklandsw.http.servlet.TimeoutServlet;
import com.oaklandsw.log.Log;
import com.oaklandsw.log.LogFactory;

public class TestFailover extends TestWebappBase
{

    private static final Log _log         = LogFactory
                                                  .getLog(TestFailover.class);

    private long             _prevTimeout = com.oaklandsw.http.HttpURLConnection
                                                  .getDefaultIdleConnectionTimeout();

    public TestFailover(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestFailover.class);
        return suite;
    }

    public static void main(String args[])
    {
        mainRun(suite(), args);
    }

    public void tearDown() throws Exception
    {
        com.oaklandsw.http.HttpURLConnection.setDefaultTimeout(0);
        com.oaklandsw.http.HttpURLConnection.setExplicitClose(false);
        com.oaklandsw.http.HttpURLConnection
                .setDefaultIdleConnectionTimeout((int)_prevTimeout);
        com.oaklandsw.http.HttpURLConnection
                .setMaxConnectionsPerHost(HttpConnectionManager.DEFAULT_MAX_CONNECTIONS);
        com.oaklandsw.http.HttpURLConnection.closeAllPooledConnections();
    }

    public void testConnectProxyHostBad() throws Exception
    {
        URL url = new URL(_urlBase + TimeoutServlet.NAME);

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();

        // Only valid for nogoop implementation
        if (!(urlCon instanceof com.oaklandsw.http.HttpURLConnection))
            return;

        com.oaklandsw.http.HttpURLConnection ngUrlCon = (com.oaklandsw.http.HttpURLConnection)urlCon;
        ngUrlCon.setConnectionProxyHost("xxxx");

        try
        {
            urlCon.setRequestMethod("GET");
            urlCon.connect();
            fail("Did not get expected exception");
        }
        catch (UnknownHostException ex)
        {
            // Expected
            // System.out.println(ex);
        }

        // Get one going to a good place
        urlCon = (HttpURLConnection)url.openConnection();
        urlCon.connect();
        urlCon.getResponseCode();
    }

    public void testConnectProxyPortBad() throws Exception
    {
        URL url = new URL(_urlBase + TimeoutServlet.NAME);

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();

        // Only valid for nogoop implementation
        if (!(urlCon instanceof com.oaklandsw.http.HttpURLConnection))
            return;

        com.oaklandsw.http.HttpURLConnection ngUrlCon = (com.oaklandsw.http.HttpURLConnection)urlCon;
        ngUrlCon.setConnectionProxyHost(TestEnv.HOST);
        ngUrlCon.setConnectionProxyPort(1);

        try
        {
            urlCon.setRequestMethod("GET");
            urlCon.connect();
            fail("Did not get expected exception");
        }
        catch (ConnectException ex)
        {
            // Expected
            // System.out.println(ex);
        }

        // Get one going to a good place
        urlCon = (HttpURLConnection)url.openConnection();
        urlCon.connect();
        urlCon.getResponseCode();
    }

    public void testConnectProxyGood() throws Exception
    {
        URL url = new URL(_urlBase + TimeoutServlet.NAME);

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();

        // Only valid for nogoop implementation
        if (!(urlCon instanceof com.oaklandsw.http.HttpURLConnection))
            return;

        com.oaklandsw.http.HttpURLConnection ngUrlCon = (com.oaklandsw.http.HttpURLConnection)urlCon;
        assertEquals(ngUrlCon.getConnectionProxyHost(), null);
        assertEquals(ngUrlCon.getConnectionProxyPort(), -1);

        ngUrlCon.setConnectionProxyHost(TestEnv.TEST_PROXY_HOST);
        ngUrlCon.setConnectionProxyPort(TestEnv.TEST_PROXY_PORT);

        assertEquals(ngUrlCon.getConnectionProxyHost(), TestEnv.TEST_PROXY_HOST);
        assertEquals(ngUrlCon.getConnectionProxyPort(), TestEnv.TEST_PROXY_PORT);

        urlCon.setRequestMethod("GET");
        urlCon.connect();

        try
        {
            ngUrlCon.setConnectionProxyHost("xxx");
            fail("Expected exception");
        }
        catch (IllegalStateException ex)
        {
            // Expected
        }

        try
        {
            ngUrlCon.setConnectionProxyPort(122);
            fail("Expected exception");
        }
        catch (IllegalStateException ex)
        {
            // Expected
        }

        urlCon.getResponseCode();
    }

}
