package com.oaklandsw.http.errorsvr;

import java.io.IOException;
import java.net.URL;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.HttpURLConnection;
import com.oaklandsw.http.TestBase;
import com.oaklandsw.http.TestEnv;
import com.oaklandsw.http.server.ErrorServer;
import com.oaklandsw.log.Log;
import com.oaklandsw.log.LogFactory;

/**
 * Test the idle connection timeouts and idle connection ping.
 */
public class TestIdleTimeouts extends TestBase
{

    private static final Log _log = LogFactory.getLog(TestIdleTimeouts.class);

    public TestIdleTimeouts(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestIdleTimeouts.class);
        return suite;
    }

    public static void main(String args[])
    {
        mainRun(suite(), args);
    }

    protected URL getTimeoutUrl(int timeout) throws Exception
    {
        String timeoutStr = "&"
            + ErrorServer.ERROR_IDLE_TIMEOUT
            + "="
            + timeout;
        if (timeout == 0)
            timeoutStr = "";

        return new URL(TestEnv.TEST_URL_HOST_ERROR
            + "?"
            + ErrorServer.ERROR_KEEP_ALIVE
            + timeoutStr);
    }

    protected URL getNormalUrl() throws Exception
    {
        return new URL(TestEnv.TEST_URL_HOST_ERROR);
    }

    // server timesout idle connection and we hit it
    public void testServerIdleConnTimeout() throws Exception
    {
        URL url = getTimeoutUrl(1000);

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.getResponseCode();

        // Wait until after the connection timed out at the server
        Thread.sleep(2000);

        url = getNormalUrl();
        urlCon = (HttpURLConnection)url.openConnection();
        HttpURLConnection.setTries(1);

        try
        {
            urlCon.getResponseCode();
            fail("Did not get expected exception");
        }
        catch (IOException ex)
        {
            // Expected
        }
    }

    // client timesout idle connection and we hit it
    public void testClientIdleConnTimeout() throws Exception
    {
        URL url = getTimeoutUrl(2000);

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setIdleConnectionTimeout(1000);
        urlCon.getResponseCode();

        // Wait for connection to timeout at the client
        Thread.sleep(1500);

        // Should work since it's on a new connection
        url = getNormalUrl();
        urlCon = (HttpURLConnection)url.openConnection();
        HttpURLConnection.setTries(1);

        assertEquals(200, urlCon.getResponseCode());
    }

    // client timesout idle connection and we hit it
    public void testClientIdleConnPing(int numTries, int serverTimeout) throws Exception
    {
        URL url = getTimeoutUrl(serverTimeout);

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.getResponseCode();

        // Wait for connection to timeout at the server
        Thread.sleep(1000);

        // Should work since it's on a new connection, because the ping
        // killed the previous connection
        url = new URL(TestEnv.TEST_URL_HOST_ERROR
            + "?"
            + ErrorServer.POST_NO_DATA);
        urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setIdleConnectionPing(500);
        urlCon.setRequestMethod("POST");
        HttpURLConnection.setTries(numTries);

        if (numTries == 1 && serverTimeout > 0)
        {
            try
            {
                urlCon.getResponseCode();
                fail("expected exception");
            }
            catch (IOException ex)
            {
                // Expected
            }
        }
        else
        {
            assertEquals(200, urlCon.getResponseCode());
        }
    }

    public void testClientIdleConnPing1() throws Exception
    {
        testClientIdleConnPing(1, 1000);
    }

    // Bug 1439 idle connection ping kills connection 
    public void testClientIdleConnPing1NoTimeout() throws Exception
    {
        testClientIdleConnPing(1, 0);
    }

    public void testClientIdleConnPing2() throws Exception
    {
        testClientIdleConnPing(2, 1000);
    }

    public void allTestMethods() throws Exception
    {
        testServerIdleConnTimeout();
        testClientIdleConnTimeout();
        testClientIdleConnPing1();
        testClientIdleConnPing2();
    }

}