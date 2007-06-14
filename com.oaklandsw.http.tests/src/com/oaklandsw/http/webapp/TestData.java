package com.oaklandsw.http.webapp;

import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.logging.Log;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.HttpTestEnv;
import com.oaklandsw.http.servlet.RequestBodyServlet;
import com.oaklandsw.util.LogUtils;

public class TestData extends TestWebappBase
{

    private static final Log _log      = LogUtils.makeLogger();

    protected static String  _errorUrl = HttpTestEnv.TEST_URL_HOST_ERRORSVR;

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
        URL url = new URL(_urlBase + RequestBodyServlet.NAME);

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("GET");
        urlCon.connect();
        urlCon.disconnect();

        urlCon.getInputStream().close();

        doGetLikeMethod("GET", CHECK_CONTENT);

        checkNoActiveConns(url);
    }

    public void testAccessAfterClose() throws Exception
    {
        URL url = new URL(_urlBase + RequestBodyServlet.NAME);

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("GET");
        urlCon.connect();

        urlCon.getInputStream().close();

        // Should not throw
        urlCon.getInputStream().close();

        checkNoActiveConns(url);
    }

    public void allTestMethods() throws Exception
    {
        testAccessAfterClose();
        testDisconnect();

    }

}
