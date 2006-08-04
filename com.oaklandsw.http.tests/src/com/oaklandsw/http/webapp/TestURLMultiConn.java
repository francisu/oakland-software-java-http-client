package com.oaklandsw.http.webapp;

import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.servlet.HeaderServlet;

public class TestURLMultiConn extends TestWebappBase
{

    private static final Log _log = LogFactory.getLog(TestURLMultiConn.class);

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
            System.out.println("Connect: " + i);
            HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
            urlCon.connect();
            assertEquals(200, urlCon.getResponseCode());
            // urlCon.getInputStream().close();
        }

        System.out.println("Sleeping");
        Thread.sleep(20000);

    }

}
