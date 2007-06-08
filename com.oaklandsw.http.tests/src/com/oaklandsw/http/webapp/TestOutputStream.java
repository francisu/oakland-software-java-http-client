package com.oaklandsw.http.webapp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;

import org.apache.commons.logging.Log;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.HttpRetryException;
import com.oaklandsw.http.HttpURLConnection;
import com.oaklandsw.http.servlet.RequestBodyServlet;
import com.oaklandsw.util.LogUtils;
import com.oaklandsw.util.Util;

/**
 * Tests for various types of OutputStream handling, including chunked and
 * streaming.
 */
public class TestOutputStream extends TestWebappBase
{

    private static final Log   _log           = LogUtils.makeLogger();

    protected static final int STREAM_NONE    = 0;
    protected static final int STREAM_CHUNKED = 1;
    protected static final int STREAM_FIXED   = 2;

    protected int              _streamingType;

    public TestOutputStream(String testName)
    {
        super(testName);
        _streamingType = STREAM_NONE;
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestOutputStream.class);
        return suite;
    }

    public static void main(String args[])
    {
        mainRun(suite(), args);
    }

    protected void setupStreaming(HttpURLConnection urlCon, int size)
    {
        switch (_streamingType)
        {
            case STREAM_NONE:
                break;
            case STREAM_CHUNKED:
                urlCon.setChunkedStreamingMode(size);
                break;
            case STREAM_FIXED:
                urlCon.setFixedLengthStreamingMode(size);
                break;
            default:
                Util.impossible("Invalid streamingtype: " + _streamingType);
        }
    }

    protected boolean isInStreamTest()
    {
        return _streamingType != STREAM_NONE;
    }

    // Return true if the connection worked
    protected boolean checkResponse(HttpURLConnection urlCon) throws Exception
    {
        int response = 0;
        try
        {
            response = urlCon.getResponseCode();
            assertEquals(200, response);
            return true;
        }
        catch (HttpRetryException ex)
        {
            ex.printStackTrace();
            fail("got unexpected HttpRetryException");
            return false;
        }
    }

    static final boolean GET_STREAM = true;

    public void testGetNoData(boolean getStream) throws Exception
    {
        // Only applies to the streaming tests
        if (_streamingType == STREAM_NONE)
            return;

        URL url = new URL(_urlBase + RequestBodyServlet.NAME);

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        setupStreaming(urlCon, 100);
        urlCon.setDoOutput(true);
        if (getStream)
            urlCon.getOutputStream();
        try
        {
            // This should not be allowed if streaming and we have not gotten
            // and closed the connection
            urlCon.getResponseCode();
            fail("Should have gotten illegal state exception");
        }
        catch (IllegalStateException ex)
        {
            // Expected
        }
    }

    public void testGetNoDataGetStream() throws Exception
    {
        testGetNoData(GET_STREAM);
    }

    public void testGetNoDataNoGetStream() throws Exception
    {
        testGetNoData(!GET_STREAM);
    }

    public void testPostBodyLengthWrongStream(int size) throws Exception
    {
        if (_streamingType != STREAM_FIXED)
            return;

        URL url = new URL(_urlBase + RequestBodyServlet.NAME);

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("POST");
        urlCon.setDoOutput(true);
        byte[] output = QUOTE.getBytes("ASCII");
        setupStreaming(urlCon, size);
        OutputStream outStr = urlCon.getOutputStream();
        outStr.write(output);
        try
        {
            outStr.close();
            fail("Did not get expected exception");
        }
        catch (IOException ex)
        {
            // Expected
        }
    }

    public void testPostBodyLengthLongStream() throws Exception
    {
        testPostBodyLengthWrongStream(4);
    }

    public void testPostBodyLengthShortStream() throws Exception
    {
        testPostBodyLengthWrongStream(400000);
    }

    protected static final String QUOTE      = "quote=It+was+the+best+of+times%2C+it+was+the+worst+of+times.";

    protected static final int    HUGE_TIMES = 100;

    public void testPostBodySmall() throws Exception
    {
        URL url = new URL(_urlBase + RequestBodyServlet.NAME);

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("POST");
        urlCon.setDoOutput(true);
        setupStreaming(urlCon, 1);
        OutputStream outStr = urlCon.getOutputStream();
        outStr.write("a".getBytes("ASCII"));
        outStr.close();
        if (checkResponse(urlCon))
            checkReply(urlCon, "<tt>a</tt>");
    }

    public void testPostBody() throws Exception
    {
        URL url = new URL(_urlBase + RequestBodyServlet.NAME);

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("POST");
        urlCon.setDoOutput(true);
        byte[] output = QUOTE.getBytes("ASCII");
        setupStreaming(urlCon, output.length);
        OutputStream outStr = urlCon.getOutputStream();
        outStr.write(output);
        outStr.close();
        if (checkResponse(urlCon))
            checkReply(urlCon, "<tt>" + QUOTE + "</tt>");
    }

    public void testPostHuge() throws Exception
    {
        URL url = new URL(_urlBase + RequestBodyServlet.NAME);

        String postData = "";
        for (int i = 0; i < HUGE_TIMES; i++)
            postData += QUOTE;

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("POST");
        urlCon.setDoOutput(true);
        byte[] output = postData.getBytes("ASCII");
        // The output.length is ignored for chuncked
        setupStreaming(urlCon, output.length);
        OutputStream outStr = urlCon.getOutputStream();
        if (_streamingType == STREAM_CHUNKED)
        {
            // Do this so we actually get chunks
            for (int i = 0; i < HUGE_TIMES; i++)
                outStr.write(QUOTE.getBytes("ASCII"));
        }
        else
        {
            outStr.write(output);

        }
        outStr.close();
        if (checkResponse(urlCon))
            checkReply(urlCon, "<tt>" + postData + "</tt>");
    }

    public void testPostBodyCustomLength() throws Exception
    {
        URL url = new URL(_urlBase + RequestBodyServlet.NAME);

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("POST");
        urlCon.setDoOutput(true);
        urlCon.setRequestProperty("Content-Length", String.valueOf(QUOTE
                .length()));
        byte[] output = QUOTE.getBytes("ASCII");
        setupStreaming(urlCon, output.length);
        OutputStream outStr = urlCon.getOutputStream();
        outStr.write(output);
        outStr.close();
        if (checkResponse(urlCon))
            checkReply(urlCon, "<tt>" + QUOTE + "</tt>");
    }

    public void testPostBodyLengthTooLong() throws Exception
    {
        if (_streamingType != STREAM_NONE)
            return;

        URL url = new URL(_urlBase + RequestBodyServlet.NAME);

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("POST");
        urlCon.setDoOutput(true);
        urlCon.setRequestProperty("Content-Length", String.valueOf(QUOTE
                .length() + 10));
        byte[] output = QUOTE.getBytes("ASCII");
        setupStreaming(urlCon, output.length);
        OutputStream outStr = urlCon.getOutputStream();
        outStr.write(output);
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
        if (_streamingType != STREAM_NONE)
            return;

        URL url = new URL(_urlBase + RequestBodyServlet.NAME);

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("POST");
        urlCon.setDoOutput(true);
        urlCon.setRequestProperty("Content-Length", String.valueOf(QUOTE
                .length() - 10));
        byte[] output = QUOTE.getBytes("ASCII");
        setupStreaming(urlCon, output.length);
        OutputStream outStr = urlCon.getOutputStream();
        outStr.write(output);
        outStr.close();
        if (checkResponse(urlCon))
        {
            checkReply(urlCon, "<tt>"
                + QUOTE.substring(0, QUOTE.length() - 10)
                + "</tt>");
        }
    }

    // From Caterpillar
    public void testPostWithSleep() throws Exception
    {
        URL url = new URL(_urlBase + RequestBodyServlet.NAME);

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("POST");

        urlCon.setUseCaches(false);
        urlCon.setDoOutput(true);
        urlCon.setDoInput(true);
        // conn.setTimeout(300000000);

        byte[] output = "Input".getBytes("ASCII");
        setupStreaming(urlCon, output.length);

        OutputStream out = urlCon.getOutputStream();
        PrintWriter pw = new PrintWriter(out);
        pw.print("Input");
        pw.flush();
        pw.close();
        // out.close();

        urlCon.connect();
        Thread.sleep(3 * 1000);
        InputStream is = urlCon.getInputStream();
        assertEquals(200, urlCon.getResponseCode());

        int temp = is.read();
        while (temp != -1)
            temp = is.read();

        is.close();
    }

    public void testBug1796() throws Exception
    {
        int tm = 5000;
        String addr = _urlBase + RequestBodyServlet.NAME;

        String in = "<xmlText></xmlText>";

        HttpURLConnection con = getConnection(addr, "POST", tm);
        con.setRequestProperty("Content-Type", "text/xml; charset=UTF-8");
        con.setDoOutput(true);

        String str = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + in;
        setupStreaming(con, str.length());

        con.connect();
        final OutputStreamWriter output = new OutputStreamWriter(con
                .getOutputStream(), "UTF-8");
        output.write(str);
        output.flush();
        output.close();

        // Customer did not have the output.close(), and had the two .flush(),
        // .close() statements after that which were causing the problem.
        // They worked in the JRE version however
        if (false)
        {
            output.close();
            con.getOutputStream().flush();
            con.getOutputStream().close();
        }

        if (checkResponse(con))
        {
            InputStream inStr = con.getInputStream();
            con.getContentLength();
            assertTrue(Util.getStringFromInputStream(inStr).indexOf("xmlText") > 0);
        }
    }

    HttpURLConnection getConnection(String addr, String verb, int tout)
        throws Exception
    {
        final URL url = new URL(addr);
        final HttpURLConnection con = openConnection(url, tout);
        con.setDoInput(true);
        con.setRequestMethod(verb);
        return con;
    }

    HttpURLConnection openConnection(URL url, int tout) throws Exception
    {
        com.oaklandsw.http.HttpURLConnection c = com.oaklandsw.http.HttpURLConnection
                .openConnection(url);
        c.setConnectionTimeout(tout);
        c.setTimeout(tout);
        c.setRequestTimeout(tout);
        return c;
    }

    public void testPostAsXMLRPC() throws Exception
    {
        URL url = new URL(_urlBase + RequestBodyServlet.NAME);

        byte[] request = "This is a test request".getBytes("ASCII");

        _log.debug("testPostAsXMLRPC");

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setDoInput(true);
        urlCon.setDoOutput(true);
        urlCon.setUseCaches(false);
        urlCon.setAllowUserInteraction(false);
        urlCon.setRequestProperty("Content-Length", Integer
                .toString(request.length));
        urlCon.setRequestProperty("Content-Type", "text/xml");
        setupStreaming(urlCon, request.length);
        OutputStream out = urlCon.getOutputStream();
        out.write(request);
        out.flush();
        out.close();
        if (checkResponse(urlCon))
        {
            urlCon.getInputStream().close();
        }
    }

    // 965 - connect() should not be called in getOutputStream()
    public void testNoConnectOnGetOutputStream() throws Exception
    {
        URL url = new URL(_urlBase + RequestBodyServlet.NAME);

        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setRequestMethod("POST");

        assertFalse(conn.isConnected());
        conn.setDoOutput(true);
        conn.getOutputStream();
        assertFalse("Should not be connected", conn.isConnected());
    }

    public void allTestMethods() throws Exception
    {
        testPostBodySmall();
        testPostBody();
        testPostBodyCustomLength();
        testPostBodyLengthTooLong();
        testPostBodyLengthShort();
        testBug1796();
        testPostAsXMLRPC();
        testNoConnectOnGetOutputStream();
    }

}