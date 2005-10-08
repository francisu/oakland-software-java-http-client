package com.oaklandsw.http.webserver;

import java.net.HttpURLConnection;
import java.net.URL;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.HttpConnectionManager;
import com.oaklandsw.http.TestBase;
import com.oaklandsw.http.TestEnv;

public class Test292 extends TestBase
{

    public Test292(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        return new TestSuite(Test292.class);
    }

    public void setUp() throws Exception
    {
        TestEnv.setUp();
    }

    public void tearDown() throws Exception
    {
        com.oaklandsw.http.HttpURLConnection.setExplicitClose(false);
        com.oaklandsw.http.HttpURLConnection
                .setMaxConnectionsPerHost(HttpConnectionManager.DEFAULT_MAX_CONNECTIONS);
    }

    public static void main(String args[])
    {
        String[] testCaseName = { Test292.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }

    private void simpleGet(URL url)
    {
        try
        {
            HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
            urlCon.setRequestMethod("GET");
            urlCon.connect();
            assertEquals(200, urlCon.getResponseCode());
            urlCon.getInputStream().close();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            fail("caught exception: " + ex);
        }
    }

    public void test() throws Exception
    {
        com.oaklandsw.http.HttpURLConnection.setExplicitClose(true);
        com.oaklandsw.http.HttpURLConnection.setMaxConnectionsPerHost(1);

        final URL url = new URL(TestEnv.TEST_URL_WEBSERVER);

        simpleGet(url);

        Thread t1 = new Thread()
        {
            public void run()
            {
                simpleGet(url);
            }
        };

        t1.start();

        Thread t2 = new Thread()
        {
            public void run()
            {

                simpleGet(url);
            }
        };

        t2.start();

        Thread.sleep(3000);

        simpleGet(url);
        simpleGet(url);

        checkNoActiveConns(url);

    }

}
