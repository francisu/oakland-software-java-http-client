package com.oaklandsw.http.webext;

import java.net.URL;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.HttpTestBase;
import com.oaklandsw.http.HttpTestEnv;

public class TestProxyHost extends HttpTestBase
{

    public TestProxyHost(String testName)
    {
        super(testName);
    }

    public static void main(String args[])
    {
        mainRun(suite(), args);
    }

    public static Test suite()
    {
        return new TestSuite(TestProxyHost.class);
    }

    public void setUp() throws Exception
    {
        super.setUp();
        com.oaklandsw.http.HttpURLConnection.setProxyHost(HttpTestEnv.TEST_PROXY_HOST);
        com.oaklandsw.http.HttpURLConnection.setProxyPort(HttpTestEnv.NORMAL_PROXY_PORT);
    }

    public void tearDown() throws Exception
    {
        super.tearDown();
        com.oaklandsw.http.HttpURLConnection.setProxyHost(null);
        com.oaklandsw.http.HttpURLConnection.setProxyPort(-1);
    }

    // 958 unlimited max connections
    public void testUnlimitedMaxConnections() throws Exception
    {
        // Only 1
        com.oaklandsw.http.HttpURLConnection.setMaxConnectionsPerHost(1);

        // Take one connection to timeout server
        (new OpenThread()).start();

        // Let the threads get started
        Thread.sleep(500);

        URL url = new URL("http://www.sun.com");

        com.oaklandsw.http.HttpURLConnection urlCon = (com.oaklandsw.http.HttpURLConnection)url
                .openConnection();

        // Should not timeout if more than one proxy connection allowed
        urlCon.setConnectionTimeout(1000);
        urlCon.setRequestMethod("GET");
        urlCon.connect();
        urlCon.getResponseCode();
    }

}
