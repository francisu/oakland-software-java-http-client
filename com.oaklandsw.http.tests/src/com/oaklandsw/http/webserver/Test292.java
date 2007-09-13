package com.oaklandsw.http.webserver;

import java.net.URL;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.HttpTestBase;
import com.oaklandsw.http.HttpTestEnv;
import com.oaklandsw.http.HttpURLConnection;

public class Test292 extends HttpTestBase
{

    public Test292(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        return new TestSuite(Test292.class);
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
            HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
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
        com.oaklandsw.http.HttpURLConnection.setMaxConnectionsPerHost(1);

        final URL url = new URL(HttpTestEnv.TEST_URL_WEBSERVER);

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
