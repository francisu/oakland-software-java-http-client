package com.oaklandsw.http.webapp;

import java.io.InputStream;
import java.net.URL;

import org.apache.commons.logging.Log;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.HttpURLConnection;
import com.oaklandsw.http.servlet.HeaderServlet;
import com.oaklandsw.util.LogUtils;
import com.oaklandsw.util.Util;

public class TestNoData extends TestWebappBase
{

    private static final Log _log = LogUtils.makeLogger();

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

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(code, response);

        // Should be empty streams
        InputStream is = urlCon.getInputStream();
        int count = Util.flushStream(is);
        assertEquals(0, count);

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

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
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

        testNullInputStream(204, false);
        doGetLikeMethod("GET", CHECK_CONTENT);
        testNullInputStream(304, false);
        doGetLikeMethod("GET", CHECK_CONTENT);
    }

    // Bug 1956
    public void testHangIfStreamNotRead() throws Exception
    {
        HttpURLConnection.resetUrlConReleased();

        String urlStr = _urlBase + HeaderServlet.NAME;
        URL url = new URL(urlStr);

        try
        {
            HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
            for (int i = 0; i < HttpURLConnection.getMaxConnectionsPerHost() + 1; i++)
            {
                urlCon = HttpURLConnection.openConnection(url);
                // The last of of these will hang here because all of the
                // connections are allocated
                urlCon.getResponseCode();
            }
        }
        catch (IllegalStateException is)
        {
            // Expected
        }

        // The pool will still have 2 connections, this is expected
        assertEquals(2, getActiveConns(url));
    }

    public void allTestMethods() throws Exception
    {
        testNullInputStream();
    }

}
