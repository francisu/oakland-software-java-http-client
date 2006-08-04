package com.oaklandsw.http.webapp;

import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.servlet.RequestBodyServlet;

public class TestDisconnect extends TestWebappBase
{

    private static final Log _log = LogFactory.getLog(TestDisconnect.class);

    public TestDisconnect(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestDisconnect.class);
        return suite;
    }

    public static void main(String args[])
    {
        mainRun(suite(), args);
    }

    public void testDisconnect() throws Exception
    {

        com.oaklandsw.http.HttpURLConnection.setExplicitClose(true);

        URL url = new URL(_urlBase + RequestBodyServlet.NAME);

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("GET");
        urlCon.connect();
        urlCon.disconnect();

        urlCon.getInputStream().close();

        doGetLikeMethod("GET", CHECK_CONTENT);

        com.oaklandsw.http.HttpURLConnection.setExplicitClose(false);
    }

    public void testAccessAfterClose() throws Exception
    {
        com.oaklandsw.http.HttpURLConnection.setExplicitClose(true);

        URL url = new URL(_urlBase + RequestBodyServlet.NAME);

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("GET");
        urlCon.connect();

        urlCon.getInputStream().close();

        // Should not throw
        urlCon.getInputStream().close();

        com.oaklandsw.http.HttpURLConnection.setExplicitClose(false);
    }

    public void allTestMethods() throws Exception
    {
        testDisconnect();
        testAccessAfterClose();
    }

}
