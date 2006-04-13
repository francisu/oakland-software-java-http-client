package com.oaklandsw.http.errorsvr;

import java.io.InputStream;
import java.net.URL;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.TestBase;
import com.oaklandsw.http.TestEnv;
import com.oaklandsw.http.server.ErrorServer;
import com.oaklandsw.log.Log;
import com.oaklandsw.log.LogFactory;
import com.oaklandsw.util.Util;

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

    public void tearDown() throws Exception
    {
        com.oaklandsw.http.HttpURLConnection.setDefaultTimeout(0);
        com.oaklandsw.http.HttpURLConnection.setExplicitClose(false);
        com.oaklandsw.http.HttpURLConnection.setTries(3);
    }

    public void testIdleConnTimeout(int type) throws Exception
    {
        URL url = new URL(TestEnv.TEST_URL_HOST_ERROR
            + "?"
            + ErrorServer.ERROR_IDLE_TIMEOUT
            + "=1000"
            + "&"
            + ErrorServer.ERROR_KEEP_ALIVE);

        com.oaklandsw.http.HttpURLConnection urlCon = (com.oaklandsw.http.HttpURLConnection)url
                .openConnection();
        urlCon.getResponseCode();
        
        // Wait until after the connection timed out at the server
        Thread.sleep(2000);

        urlCon = (com.oaklandsw.http.HttpURLConnection)url.openConnection();
        com.oaklandsw.http.HttpURLConnection.setTries(1);

        urlCon.getHeaderField(1);
        
        InputStream in = urlCon.getInputStream();
        urlCon.getContentLength();
        Util.getStringFromInputStream(in);
        
        // Should fail
        System.out.println("response: " + urlCon.getResponseCode());
        System.out.println("time: " + System.currentTimeMillis());
        checkNoActiveConns(url);
    }

    public void testIdleConnTimeout() throws Exception
    {
        testIdleConnTimeout(1);
    }

    public void allTestMethods() throws Exception
    {
        testIdleConnTimeout();
    }

}
