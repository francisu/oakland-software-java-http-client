package com.oaklandsw.http.webapp;

import java.net.HttpURLConnection;
import java.net.URL;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.TestEnv;
import com.oaklandsw.http.servlet.RequestBodyServlet;
import com.oaklandsw.log.Log;
import com.oaklandsw.log.LogFactory;

public class TestData extends TestWebappBase
{

    private static final Log _log      = LogFactory.getLog(TestData.class);

    protected static String  _errorUrl = TestEnv.TEST_URL_HOST_ERROR;

    public TestData(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestData.class);
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

        checkNoActiveConns(url);
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

        checkNoActiveConns(url);
    }

    public void allTestMethods() throws Exception
    {
        testAccessAfterClose();
        testDisconnect();

    }

}
