package com.oaklandsw.http.webapp;

import java.net.URL;

import com.oaklandsw.util.Log;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.HttpURLConnection;
import com.oaklandsw.http.servlet.HeaderServlet;
import com.oaklandsw.util.LogUtils;

public class TestURLMultiConn extends TestWebappBase
{

    private static final Log   _log         = LogUtils.makeLogger();

    public TestURLMultiConn(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestURLMultiConn.class);
        return suite;
    }

    public static void main(String args[])
    {
        mainRun(suite(), args);
    }

    public void testMultiConnections() throws Exception
    {
        URL url = new URL(_urlBase + HeaderServlet.NAME);

        // Make sure I can do multiple connections to the same
        // place
        for (int i = 0; i < 50; i++)
        {
            //System.out.println("Connect: " + i);
            HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
            urlCon.connect();
            assertEquals(200, urlCon.getResponseCode());
            urlCon.getInputStream().close();
        }

    }

}
