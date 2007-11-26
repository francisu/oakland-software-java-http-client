package com.oaklandsw.http.webapp;

import java.io.OutputStream;
import java.net.URL;

import org.apache.commons.logging.Log;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.HttpRetryException;
import com.oaklandsw.http.HttpURLConnection;
import com.oaklandsw.http.servlet.RequestBodyServlet;
import com.oaklandsw.util.LogUtils;

/**
 * Tests for raw output stream
 */
// Bug 2009
public class TestOutputStreamRaw extends TestOutputStream
{

    private static final Log _log = LogUtils.makeLogger();

    public TestOutputStreamRaw(String testName)
    {
        super(testName);
        _streamingType = STREAM_RAW;
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestOutputStreamRaw.class);
        return suite;
    }

    public static void main(String args[])
    {
        mainRun(suite(), args);
    }

    public void testGet() throws Exception
    {
        URL url = new URL(_urlBase + RequestBodyServlet.NAME);

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setDoOutput(true);
        urlCon.setRequestMethod("GET");
        setupStreaming(urlCon, 100);
        OutputStream os = urlCon.getOutputStream();
        String str = "GET " + url.toString() + " HTTP/1.1\r\n\r\n";
        os.write(str.getBytes());
        os.close();
        assertEquals(200, urlCon.getResponseCode());
        assertTrue(urlCon.getHeaderFields().size() > 0);
        urlCon.getInputStream().close();
    }

    // Submitted from a customer
    public void testGetGoogle() throws Exception
    {
        String request = "GET / HTTP/1.1\r\n"
            + "Host: www.google.com\r\n"
            + "\r\n";

        URL url = new URL("http", "www.google.com", 80, "/");
        HttpURLConnection connection = HttpURLConnection.openConnection(url);
        connection.setDoOutput(true);
        connection.setRawStreamingMode(true);

        connection.getOutputStream().write(request.getBytes("UTF-8"));
        connection.getOutputStream().close();

        // This line below was missing in the customer's example
        connection.getResponseCode();
        assertTrue(connection.getHeaderFields().size() > 0);
        assertTrue(connection.getHeadersLength() > 0);
    }

    public void testGetBad() throws Exception
    {
        URL url = new URL(_urlBase + RequestBodyServlet.NAME);

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setDoOutput(true);
        urlCon.setRequestMethod("GET");
        setupStreaming(urlCon, 100);
        OutputStream os = urlCon.getOutputStream();
        String str = "GETx " + url.toString() + " HTTP/1.1\r\n\r\n";
        os.write(str.getBytes());
        os.close();
        try
        {
            urlCon.getResponseCode();
            fail("Did not get expected exception");
        }
        catch (HttpRetryException ex)
        {
            // Expected
        }
    }

    public void allTestMethods() throws Exception
    {
        // Don't bother with these since we are writing raw output
    }

}
