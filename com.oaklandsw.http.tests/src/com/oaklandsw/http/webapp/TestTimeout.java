package com.oaklandsw.http.webapp;

import java.net.URL;
import java.util.ArrayList;

import org.apache.commons.logging.Log;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.HttpTimeoutException;
import com.oaklandsw.http.HttpURLConnection;
import com.oaklandsw.http.servlet.TimeoutServlet;
import com.oaklandsw.util.LogUtils;

public class TestTimeout extends TestWebappBase
{

    private static final Log _log = LogUtils.makeLogger();

    public TestTimeout(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestTimeout.class);
        return suite;
    }

    public static void main(String args[])
    {
        mainRun(suite(), args);
    }

    // Bug 1585 - sometimes timeouts don't work
    public void testConnectionIdleTimeoutShutdown() throws Exception
    {
        com.oaklandsw.http.HttpURLConnection
                .setDefaultIdleConnectionTimeout(2000);

        URL url = new URL(_urlBase + TimeoutServlet.NAME);
        int response = 0;

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();

        urlCon.setRequestMethod("GET");
        response = urlCon.getResponseCode();
        assertEquals(200, response);
        urlCon.getInputStream().close();

        // Wait for timeout thread to get started
        Thread.sleep(500);

        // Causes timeout thread to terminate
        com.oaklandsw.http.HttpURLConnection.closeAllPooledConnections();

        // Do another one
        urlCon = (HttpURLConnection)url.openConnection();

        urlCon.setRequestMethod("GET");
        response = urlCon.getResponseCode();
        assertEquals(200, response);
        urlCon.getInputStream().close();

        // This would fail to timeout since the timeout thread would
        // shutdown
        int connCount = getTotalConns(url);
        Thread.sleep(4000);

        // Make sure we have one less connection
        assertEquals(connCount - 1, getTotalConns(url));
    }

    public void testConnectionIdleTimeout() throws Exception
    {
        com.oaklandsw.http.HttpURLConnection
                .setDefaultIdleConnectionTimeout(2000);

        URL url = new URL(_urlBase + TimeoutServlet.NAME);
        int response = 0;

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();

        urlCon.setRequestMethod("GET");
        response = urlCon.getResponseCode();
        assertEquals(200, response);
        urlCon.getInputStream().close();

        // Wait for the connection to timeout
        int connCount = getTotalConns(url);
        Thread.sleep(4000);

        // Make sure we have one less connection
        assertEquals(connCount - 1, getTotalConns(url));
    }

    protected static final int CONN_COUNT = 10;

    // Tests timeout waiting for a connection to be available
    public void testConnectWaitForThread() throws Exception
    {
        // Use up up lots of connections available by default
        for (int i = 0; i < CONN_COUNT; i++)
            (new OpenThread()).start();

        // Let the threads get started
        Thread.sleep(1000);

        URL url = new URL(_urlBase + TimeoutServlet.NAME);

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();

        // No timeout, but this should not be able to get a connection
        urlCon.setRequestMethod("GET");
        urlCon.connect();
        urlCon.getResponseCode();
    }

    // Bug 958 unlimited max connections
    public void testUnlimitedMaxConnections() throws Exception
    {
        // Use up up lots of connections available by default
        for (int i = 0; i < CONN_COUNT; i++)
            (new OpenThread()).start();

        // Let the threads get started
        Thread.sleep(1000);

        URL url = new URL(_urlBase + TimeoutServlet.NAME);

        // Unlimited
        com.oaklandsw.http.HttpURLConnection.setMaxConnectionsPerHost(-1);

        com.oaklandsw.http.HttpURLConnection urlCon = (com.oaklandsw.http.HttpURLConnection)url
                .openConnection();

        // Should not timeout if unlimited number of connections
        urlCon.setConnectionTimeout(1000);
        urlCon.setRequestMethod("GET");
        urlCon.connect();
        urlCon.getResponseCode();
    }

    // Tests timeout waiting for a connection to be available
    public void testConnectTimeout(int type) throws Exception
    {
        // Use up the two connections available by default
        (new OpenThread()).start();
        (new OpenThread()).start();

        // Give them a chance to get started on their wait
        Thread.sleep(1000);

        setupDefaultTimeout(type, 1000);

        URL url = new URL(_urlBase + TimeoutServlet.NAME);

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();

        setupConnTimeout(urlCon, type, 1000);

        // No timeout, but this should not be able to get a connection
        urlCon.setRequestMethod("GET");

        try
        {
            urlCon.connect();
            if (type != CONN_REQUEST && type != DEF_REQUEST)
                fail("Should have timed out");
            else
                urlCon.getResponseCode();
        }
        catch (HttpTimeoutException ex)
        {
            if (type == CONN_REQUEST || type == DEF_REQUEST)
                fail("These timeouts should have no effect");
            // Got expected exception
        }

    }

    public void testConnectTimeoutDef() throws Exception
    {
        testConnectTimeout(DEF);
    }

    public void testConnectTimeoutDefConnect() throws Exception
    {
        testConnectTimeout(DEF_CONNECT);
    }

    public void testConnectTimeoutDefRequest() throws Exception
    {
        testConnectTimeout(DEF_REQUEST);
    }

    public void testConnectTimeoutConn() throws Exception
    {
        testConnectTimeout(CONN);
    }

    public void testConnectTimeoutConnConnect() throws Exception
    {
        testConnectTimeout(CONN_CONNECT);
    }

    public void testConnectTimeoutConnRequest() throws Exception
    {
        testConnectTimeout(CONN_REQUEST);
    }

    public void testRequestTimeout(int type) throws Exception
    {
        setupDefaultTimeout(type, 2000);

        URL url = new URL(_urlBase + TimeoutServlet.NAME);
        int response = 0;

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();

        setupConnTimeout(urlCon, type, 2000);

        urlCon.setRequestMethod("GET");
        urlCon.setRequestProperty("timeout", "5000");

        try
        {
            response = urlCon.getResponseCode();
            if (type != CONN_CONNECT && type != DEF_CONNECT)
                fail("Should have timed out");
        }
        catch (HttpTimeoutException ex)
        {
            if (type == CONN_CONNECT || type == DEF_CONNECT)
                fail("These timeouts should have no effect");
            // Got expected exception
        }

        // Remove the timeout
        com.oaklandsw.http.HttpURLConnection.setDefaultTimeout(0);

        urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("GET");
        urlCon.setRequestProperty("timeout", "5000");

        // should work
        response = urlCon.getResponseCode();
        assertEquals(200, response);
    }

    public void testRequestTimeoutDef() throws Exception
    {
        testRequestTimeout(DEF);
    }

    public void testRequestTimeoutDefConnect() throws Exception
    {
        testRequestTimeout(DEF_CONNECT);
    }

    public void testRequestTimeoutDefRequest() throws Exception
    {
        testRequestTimeout(DEF_REQUEST);
    }

    public void testRequestTimeoutConn() throws Exception
    {
        testRequestTimeout(CONN);
    }

    public void testRequestTimeoutConnConnect() throws Exception
    {
        testRequestTimeout(CONN_CONNECT);
    }

    public void testRequestTimeoutConnRequest() throws Exception
    {
        testRequestTimeout(CONN_REQUEST);
    }

    public void testRequestNoTimeout(int type) throws Exception
    {
        setupDefaultTimeout(type, 5000);

        URL url = new URL(_urlBase + TimeoutServlet.NAME);
        int response = 0;

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();

        setupConnTimeout(urlCon, type, 5000);

        urlCon.setRequestMethod("GET");
        urlCon.setRequestProperty("timeout", "1000");

        response = urlCon.getResponseCode();
        assertEquals(200, response);
    }

    public void testRequestNoTimeoutDef() throws Exception
    {
        testRequestNoTimeout(DEF);
    }

    public void testRequestNoTimeoutDefConnect() throws Exception
    {
        testRequestNoTimeout(DEF_CONNECT);
    }

    public void testRequestNoTimeoutDefRequest() throws Exception
    {
        testRequestNoTimeout(DEF_REQUEST);
    }

    public void testRequestNoTimeoutConn() throws Exception
    {
        testRequestNoTimeout(CONN);
    }

    public void testRequestNoTimeoutConnConnect() throws Exception
    {
        testRequestNoTimeout(CONN_CONNECT);
    }

    public void testRequestNoTimeoutConnRequest() throws Exception
    {
        testRequestNoTimeout(CONN_REQUEST);
    }

    public void testRequestNoTimeoutLoop(int type) throws Exception
    {
        setupDefaultTimeout(type, 5000);
        com.oaklandsw.http.HttpURLConnection.setDefaultExplicitClose(true);

        URL url = new URL(_urlBase + TimeoutServlet.NAME);
        int response = 0;

        com.oaklandsw.http.HttpURLConnection.closeAllPooledConnections();

        for (int i = 0; i < com.oaklandsw.http.HttpURLConnection
                .getMaxConnectionsPerHost() + 5; i++)
        {
            HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();

            setupConnTimeout(urlCon, type, 5000);

            urlCon.setRequestMethod("GET");

            response = urlCon.getResponseCode();
            assertEquals(200, response);
            urlCon.getInputStream().close();
        }
    }

    public void testRequestNoTimeoutLoopDef() throws Exception
    {
        testRequestNoTimeoutLoop(DEF);
    }

    public void testRequestNoTimeoutLoopDefConnect() throws Exception
    {
        testRequestNoTimeoutLoop(DEF_CONNECT);
    }

    public void testRequestNoTimeoutLoopDefRequest() throws Exception
    {
        testRequestNoTimeoutLoop(DEF_REQUEST);
    }

    public void testRequestNoTimeoutLoopConn() throws Exception
    {
        testRequestNoTimeoutLoop(CONN);
    }

    public void testRequestNoTimeoutLoopConnConnect() throws Exception
    {
        testRequestNoTimeoutLoop(CONN_CONNECT);
    }

    public void testRequestNoTimeoutLoopConnRequest() throws Exception
    {
        testRequestNoTimeoutLoop(CONN_REQUEST);
    }

    // Don't close the connections, but do a gc() after each one, this
    // should cause the connection to get closed, allowing the new one
    // to be opened. We should be able to cycle through all of these
    // with no timeout
    // If there is something wrong, and connections get returned to the
    // pool in an invalid state, exceptions will occur in this test,
    // including timeout exceptions
    // Bug 403
    public void testRequestNoTimeoutLoopNoCloseWithGC(int type)
        throws Exception
    {
        setupDefaultTimeout(type, 2000);
        com.oaklandsw.http.HttpURLConnection.setDefaultExplicitClose(true);

        URL url = new URL(_urlBase + TimeoutServlet.NAME);
        int response = 0;

        com.oaklandsw.http.HttpURLConnection.closeAllPooledConnections();

        int i = 0;
        for (i = 0; i < com.oaklandsw.http.HttpURLConnection
                .getMaxConnectionsPerHost() + 5; i++)
        {
            HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();

            setupConnTimeout(urlCon, type, 2000);

            urlCon.setRequestMethod("POST");

            response = urlCon.getResponseCode();
            assertEquals(200, response);

            // If we collect, the garbage collector will finalize the connection
            // since there is no reference to it, this will cause it to be
            // closed, causing the transport to be closed.
            _log.debug("Running gc to close connection");
            System.gc();
        }
    }

    public void testRequestNoTimeoutLoopNoCloseWithGCDef() throws Exception
    {
        testRequestNoTimeoutLoopNoCloseWithGC(DEF);
    }

    public void testRequestNoTimeoutLoopNoCloseWithGCDefConnect()
        throws Exception
    {
        testRequestNoTimeoutLoopNoCloseWithGC(DEF_CONNECT);
    }

    public void testRequestNoTimeoutLoopNoCloseWithGCDefRequest()
        throws Exception
    {
        testRequestNoTimeoutLoopNoCloseWithGC(DEF_REQUEST);
    }

    public void testRequestNoTimeoutLoopNoCloseWithGCConn() throws Exception
    {
        testRequestNoTimeoutLoopNoCloseWithGC(CONN);
    }

    public void testRequestNoTimeoutLoopNoCloseWithGCConnConnect()
        throws Exception
    {
        testRequestNoTimeoutLoopNoCloseWithGC(CONN_CONNECT);
    }

    public void testRequestNoTimeoutLoopNoCloseWithGCConnRequest()
        throws Exception
    {
        testRequestNoTimeoutLoopNoCloseWithGC(CONN_REQUEST);
    }

    // Don't close the explicit connections, so the one after the max
    // connections per host should timeout waiting for the connection
    // to be available
    public void testRequestNoTimeoutLoopNoClose(int type) throws Exception
    {
        setupDefaultTimeout(type, 2000);
        com.oaklandsw.http.HttpURLConnection.setDefaultExplicitClose(true);

        URL url = new URL(_urlBase + TimeoutServlet.NAME);
        int response = 0;

        com.oaklandsw.http.HttpURLConnection.closeAllPooledConnections();

        int i = 0;
        ArrayList cons = new ArrayList();
        try
        {
            for (i = 0; i < com.oaklandsw.http.HttpURLConnection
                    .getMaxConnectionsPerHost() + 5; i++)
            {
                HttpURLConnection urlCon = (HttpURLConnection)url
                        .openConnection();
                // Add it to the array to prevent it from being garbage
                // collected
                // while it is still open
                cons.add(urlCon);

                setupConnTimeout(urlCon, type, 2000);

                urlCon.setRequestMethod("POST");

                response = urlCon.getResponseCode();
                assertEquals(200, response);

                // If we collect, the garbage collector will finalize the
                // connection
                // since there is no reference to it, but it won't because we
                // have
                // it added to the array above
                System.gc();
            }
            fail("Should have timed out");
        }
        catch (HttpTimeoutException ex)
        {
            if (i != com.oaklandsw.http.HttpURLConnection
                    .getMaxConnectionsPerHost())
                fail("Did not get timeout at the right loop increment: " + i);
            // Got expected exception
        }
    }

    public void testRequestNoTimeoutLoopNoCloseDef() throws Exception
    {
        testRequestNoTimeoutLoopNoClose(DEF);
    }

    public void testRequestNoTimeoutLoopNoCloseDefConnect() throws Exception
    {
        testRequestNoTimeoutLoopNoClose(DEF_CONNECT);
    }

    public void testRequestNoTimeoutLoopNoCloseConn() throws Exception
    {
        testRequestNoTimeoutLoopNoClose(CONN);
    }

    public void testRequestNoTimeoutLoopNoCloseConnConnect() throws Exception
    {
        testRequestNoTimeoutLoopNoClose(CONN_CONNECT);
    }

    // Bug 968 mix of zero/non zero request timeout in MT does not work
    public void testMtRequestTimeout() throws Exception
    {
        URL url = new URL(_urlBase + TimeoutServlet.NAME);
        int response = 0;

        com.oaklandsw.http.HttpURLConnection urlCon = (com.oaklandsw.http.HttpURLConnection)url
                .openConnection();

        // First one, set request timeout
        urlCon.setTimeout(2000);
        urlCon.setRequestMethod("GET");
        urlCon.setRequestProperty("timeout", "0");
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        // Second one, don't set timeout, should not have a request timeout
        urlCon = (com.oaklandsw.http.HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("GET");
        urlCon.setRequestProperty("timeout", "4000");

        // Should not get a timeout here
        response = urlCon.getResponseCode();

        // Wait longer than the previous req timeout (2000) and the
        // time it takes to respond
        Thread.sleep(5000);

        assertEquals(200, response);
    }

}
