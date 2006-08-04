package com.oaklandsw.http.webapp;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.servlet.HeaderServlet;

public class TestNoData extends TestWebappBase
{

    private static final Log _log = LogFactory.getLog(TestNoData.class);

    public TestNoData(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestNoData.class);
        return suite;
    }

    public static void main(String args[])
    {
        mainRun(suite(), args);
    }

    public void testNullInputStream(int code, boolean noBody) throws Exception
    {
        String urlStr = _urlBase + HeaderServlet.NAME + "?responseCode=" + code;
        if (noBody)
            urlStr += "&noBody=true";

        URL url = new URL(urlStr);

        int response = 0;

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("GET");
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(code, response);

        // Should be empty byte array streams
        InputStream is = urlCon.getInputStream();
        assertTrue(is.getClass() == ByteArrayInputStream.class);
        is = urlCon.getErrorStream();
        assertEquals(null, is);

        // Make sure the connection has been returned to the pool and
        // is not stuck
        checkNoActiveConns(url);
    }

    public void testPostNoResponseData() throws Exception
    {
        String urlStr = _urlBase
            + HeaderServlet.NAME
            + "?responseCode=200"
            + "&noBody=true";
        URL url = new URL(urlStr);

        int response = 0;

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("POST");
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        // Should not need to close connection
    }

    public void testNullInputStream() throws Exception
    {
        testPostNoResponseData();

        testNullInputStream(204, true);
        doGetLikeMethod("GET", CHECK_CONTENT);
        testNullInputStream(304, true);
        doGetLikeMethod("GET", CHECK_CONTENT);

        testNullInputStream(404, true);
        doGetLikeMethod("GET", CHECK_CONTENT);

        testNullInputStream(204, false);
        doGetLikeMethod("GET", CHECK_CONTENT);
        testNullInputStream(304, false);
        doGetLikeMethod("GET", CHECK_CONTENT);

        testNullInputStream(404, true);
        doGetLikeMethod("GET", CHECK_CONTENT);
    }

    public void allTestMethods() throws Exception
    {
        testNullInputStream();
    }

}
