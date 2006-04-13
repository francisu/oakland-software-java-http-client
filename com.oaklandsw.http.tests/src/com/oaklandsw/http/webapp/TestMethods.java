package com.oaklandsw.http.webapp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.servlet.RequestBodyServlet;
import com.oaklandsw.log.Log;
import com.oaklandsw.log.LogFactory;
import com.oaklandsw.util.Util;

public class TestMethods extends TestWebappBase
{

    private static final Log _log = LogFactory.getLog(TestMethods.class);

    public TestMethods(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestMethods.class);
        return suite;
    }

    public static void main(String args[])
    {
        mainRun(suite(), args);
    }

    public void tearDown() throws Exception
    {
        com.oaklandsw.http.HttpURLConnection.setExplicitClose(false);
    }

    public void testGetMethod() throws Exception
    {
        doGetLikeMethod("GET", CHECK_CONTENT);
    }

    public void testPostMethod() throws Exception
    {
        doGetLikeMethod("POST", CHECK_CONTENT);
    }

    public void testHeadMethod() throws Exception
    {
        doGetLikeMethod("HEAD", !CHECK_CONTENT);
    }

    public void testDeleteMethod() throws Exception
    {
        doGetLikeMethod("DELETE", !CHECK_CONTENT);
    }

    public void testPutMethod() throws Exception
    {
        doGetLikeMethod("PUT", CHECK_CONTENT);
    }

    /*
     * // options does not seem to be allowed with apache 2.0.50 public void
     * testOptionsMethod() throws Exception { doGetLikeMethod("OPTIONS",
     * !CHECK_CONTENT); //
     * assertTrue(method.getAllowedMethods().hasMoreElements()); }
     */

    public void testBigHTTP() throws Exception
    {
        String bigHttp = _urlBase.substring(0, 4).toUpperCase()
            + _urlBase.substring(4);
        URL url = new URL(bigHttp + RequestBodyServlet.NAME);
        int response = 0;

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("GET");
        response = urlCon.getResponseCode();
        assertEquals(200, response);
        checkReply(urlCon, "GET");
    }

    /***************************************************************************
     * * this test will fail with JDK 1.2 - and there is no way to fix * it, it
     * works fine on JDK 1.4 - see bug 303. public void testBigHTTP2() throws
     * Exception { URL url = new URL("HTTP", TestEnv.TEST_WEBAPP_HOST,
     * TestEnv.TEST_WEBAPP_PORT, TestEnv.TEST_URL_APP +
     * RequestBodyServlet.NAME); int response = 0;
     * 
     * HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
     * urlCon.setRequestMethod("GET"); response = urlCon.getResponseCode();
     * assertEquals(200, response); checkReply(urlCon, "GET"); }
     */

    protected static final String QUOTE      = "quote=It+was+the+best+of+times%2C+it+was+the+worst+of+times.";

    protected static final int    HUGE_TIMES = 100;

    public void testDirectHttpURLConnection() throws Exception
    {
        URL url = new URL(_urlBase + RequestBodyServlet.NAME);
        int response = 0;

        HttpURLConnection urlCon = com.oaklandsw.http.HttpURLConnection
                .openConnection(url);
        response = urlCon.getResponseCode();
        assertEquals(200, response);
    }

    public void testPostBodySmall() throws Exception
    {
        URL url = new URL(_urlBase + RequestBodyServlet.NAME);
        int response = 0;

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("POST");
        urlCon.setDoOutput(true);
        OutputStream outStr = urlCon.getOutputStream();
        outStr.write("a".getBytes("ASCII"));
        outStr.close();
        response = urlCon.getResponseCode();
        assertEquals(200, response);
        checkReply(urlCon, "<tt>a</tt>");
    }

    public void testPostBody() throws Exception
    {
        URL url = new URL(_urlBase + RequestBodyServlet.NAME);
        int response = 0;

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("POST");
        urlCon.setDoOutput(true);
        OutputStream outStr = urlCon.getOutputStream();
        outStr.write(QUOTE.getBytes("ASCII"));
        outStr.close();
        response = urlCon.getResponseCode();
        assertEquals(200, response);
        checkReply(urlCon, "<tt>" + QUOTE + "</tt>");
    }

    public void testPostHuge() throws Exception
    {
        URL url = new URL(_urlBase + RequestBodyServlet.NAME);
        int response = 0;

        String postData = "";
        for (int i = 0; i < HUGE_TIMES; i++)
            postData += QUOTE;

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("POST");
        urlCon.setDoOutput(true);
        OutputStream outStr = urlCon.getOutputStream();
        outStr.write(postData.getBytes("ASCII"));
        outStr.close();
        response = urlCon.getResponseCode();
        assertEquals(200, response);
        checkReply(urlCon, "<tt>" + postData + "</tt>");
    }

    public void testPostBodyCustomLength() throws Exception
    {
        URL url = new URL(_urlBase + RequestBodyServlet.NAME);
        int response = 0;

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("POST");
        urlCon.setDoOutput(true);
        urlCon.setRequestProperty("Content-Length", String.valueOf(QUOTE
                .length()));
        OutputStream outStr = urlCon.getOutputStream();
        outStr.write(QUOTE.getBytes("ASCII"));
        outStr.close();
        response = urlCon.getResponseCode();
        assertEquals(200, response);
        checkReply(urlCon, "<tt>" + QUOTE + "</tt>");
    }

    public void testPostBodyLengthTooLong() throws Exception
    {
        URL url = new URL(_urlBase + RequestBodyServlet.NAME);

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("POST");
        urlCon.setDoOutput(true);
        urlCon.setRequestProperty("Content-Length", String.valueOf(QUOTE
                .length() + 10));
        OutputStream outStr = urlCon.getOutputStream();
        outStr.write(QUOTE.getBytes("ASCII"));
        outStr.close();
        try
        {
            urlCon.getResponseCode();
            fail("Expected exception not received");
        }
        catch (IOException ex)
        {
        }
    }

    public void testPostBodyLengthShort() throws Exception
    {
        URL url = new URL(_urlBase + RequestBodyServlet.NAME);
        int response = 0;

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("POST");
        urlCon.setDoOutput(true);
        urlCon.setRequestProperty("Content-Length", String.valueOf(QUOTE
                .length() - 10));
        OutputStream outStr = urlCon.getOutputStream();
        outStr.write(QUOTE.getBytes("ASCII"));
        outStr.close();
        response = urlCon.getResponseCode();
        assertEquals(200, response);
        checkReply(urlCon, "<tt>"
            + QUOTE.substring(0, QUOTE.length() - 10)
            + "</tt>");
    }

    // From Caterpillar
    public void testPostWithSleep() throws Exception
    {
        URL url = new URL(_urlBase + RequestBodyServlet.NAME);

        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setRequestMethod("POST");

        conn.setUseCaches(false);
        conn.setDoOutput(true);
        conn.setDoInput(true);
        // conn.setTimeout(300000000);

        OutputStream out = conn.getOutputStream();
        PrintWriter pw = new PrintWriter(out);
        pw.print("Input");
        pw.flush();
        pw.close();
        // out.close();

        conn.connect();
        Thread.sleep(3 * 1000);
        InputStream is = conn.getInputStream();
        assertEquals(200, conn.getResponseCode());

        int temp = is.read();
        while (temp != -1)
            temp = is.read();

        is.close();
    }

    public void testPostAsXMLRPC() throws Exception
    {
        URL url = new URL(_urlBase + RequestBodyServlet.NAME);
        int response = 0;

        byte[] request = "This is a test request".getBytes("ASCII");

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setDoInput(true);
        urlCon.setDoOutput(true);
        urlCon.setUseCaches(false);
        urlCon.setAllowUserInteraction(false);
        urlCon.setRequestProperty("Content-Length", Integer
                .toString(request.length));
        urlCon.setRequestProperty("Content-Type", "text/xml");
        OutputStream out = urlCon.getOutputStream();
        out.write(request);
        out.flush();
        out.close();
        urlCon.getInputStream();
        response = urlCon.getResponseCode();
        assertEquals(200, response);
    }

    // 965 - connect() should not be called in getOutputStream()
    public void testNoConnectOnGetOutputStream() throws Exception
    {
        URL url = new URL(_urlBase + RequestBodyServlet.NAME);

        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setRequestMethod("POST");
        
        assertFalse(((com.oaklandsw.http.HttpURLConnection)conn).isConnected());
        conn.setDoOutput(true);
        conn.getOutputStream();
        assertFalse("Should not be connected", 
                    ((com.oaklandsw.http.HttpURLConnection)conn).isConnected());
    }

    public void testHeadMethodExplicitClose() throws Exception
    {
        com.oaklandsw.http.HttpURLConnection.setExplicitClose(true);

        URL url = new URL(_urlBase + RequestBodyServlet.NAME);

        // Make sure we don't require explicit close for the HEAD method
        for (int i = 0; i < com.oaklandsw.http.HttpURLConnection
                .getMaxConnectionsPerHost() + 5; i++)
        {
            HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
            urlCon.setRequestMethod("HEAD");
            assertEquals(200, urlCon.getResponseCode());
        }

        com.oaklandsw.http.HttpURLConnection.setExplicitClose(false);
    }

    // Test a sequence of calls used by Credit-Suisse (bug 1433)
    public void testGetCs() throws Exception
    {
        URL url = new URL(_urlBase + RequestBodyServlet.NAME);

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();

        urlCon.getHeaderField(1);
        
        InputStream in = urlCon.getInputStream();
        urlCon.getContentLength();
        Util.getStringFromInputStream(in);
        assertEquals(200, urlCon.getResponseCode());
    }


    public void allTestMethods() throws Exception
    {
        testGetMethod();
        testPostMethod();
        testHeadMethod();
        testDeleteMethod();
        testPutMethod();
        // testOptionsMethod();
        testPostBodySmall();
        testPostBody();
        testPostBodyCustomLength();
        testPostBodyLengthTooLong();
        testPostBodyLengthShort();

    }

    /*
     * //enable this when chunked requests are properly implemented //note: only
     * few servers support this public void testPostBodyChunked() throws
     * Exception { HttpClient client = new HttpClient();
     * client.startSession(host, port); PostMethod method = new PostMethod("/" +
     * context + "/body"); method.setUseDisk(false); String body =
     * "quote=It+was+the+best+of+times%2C+it+was+the+worst+of+times.";
     * method.setRequestBody(new ByteArrayInputStream(body.getBytes()));
     * method.setRequestContentLength(PostMethod.CONTENT_LENGTH_CHUNKED); try {
     * client.executeMethod(method); } catch (Throwable t) {
     * t.printStackTrace(); fail("Unable to execute method : " + t.toString()); }
     * assertTrue(method.getResponseBodyAsString().indexOf(" <tt>
     * quote=It+was+the+best+of+times%2C+it+was+the+worst+of+times. </tt> ") >=
     * 0); assertEquals(200,method.getStatusCode()); }
     * 
     * public void testPutBody() throws Exception { HttpClient client = new
     * HttpClient(); client.startSession(host, port); PutMethod method = new
     * PutMethod("/" + context + "/body"); method.setRequestBody("This is data
     * to be sent in the body of an HTTP PUT."); try {
     * client.executeMethod(method); } catch (Throwable t) {
     * t.printStackTrace(); fail("Unable to execute method : " + t.toString()); }
     * assertTrue(method.getResponseBodyAsString(),method.getResponseBodyAsString().indexOf("
     * <tt> This is data to be sent in the body of an HTTP PUT. </tt> ") >= 0);
     * assertEquals(200,method.getStatusCode()); }
     */

}
