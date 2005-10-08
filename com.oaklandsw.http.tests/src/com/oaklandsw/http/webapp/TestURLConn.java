package com.oaklandsw.http.webapp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.servlet.HeaderServlet;
import com.oaklandsw.http.servlet.ParamServlet;
import com.oaklandsw.log.Log;
import com.oaklandsw.log.LogFactory;
import com.oaklandsw.util.Util;

public class TestURLConn extends TestWebappBase
{

    private static final Log _log = LogFactory.getLog(TestURLConn.class);

    public TestURLConn(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestURLConn.class);
        return suite;
    }

    public static void main(String args[])
    {
        mainRun(suite(), args);
    }

    public void testDefaultValues() throws Exception
    {
        URL url = new URL(_urlBase + ParamServlet.NAME);
        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        assertFalse(urlCon.getAllowUserInteraction());
        assertFalse(URLConnection.getDefaultAllowUserInteraction());
        assertTrue(urlCon.getDefaultUseCaches());
        assertTrue(urlCon.getDoInput());
        assertFalse(urlCon.getDoOutput());
        assertEquals(null, URLConnection.getDefaultRequestProperty("XXX"));
        // System.out.println("filemap: " + urlCon.getFileNameMap().getClass());
        // assertEquals(null, urlCon.getFileNameMap());
        assertEquals(url, urlCon.getURL());
        assertTrue(urlCon.getUseCaches());
        assertTrue(urlCon.getIfModifiedSince() == 0);
        assertFalse(com.oaklandsw.http.HttpURLConnection.getPreemptiveAuthentication());
        urlCon.setIfModifiedSince(1234);
        assertEquals(1234, urlCon.getIfModifiedSince());
    }

    public void testDefaultAllowUser() throws Exception
    {
        URL url = new URL(_urlBase + ParamServlet.NAME);
        URLConnection.setDefaultAllowUserInteraction(true);
        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        assertTrue(urlCon.getAllowUserInteraction());
        assertTrue(URLConnection.getDefaultAllowUserInteraction());

        URLConnection.setDefaultAllowUserInteraction(false);
        urlCon = (HttpURLConnection)url.openConnection();
        assertFalse(urlCon.getAllowUserInteraction());
        assertFalse(URLConnection.getDefaultAllowUserInteraction());

    }

    public void testDefaultUseCache() throws Exception
    {
        URL url = new URL(_urlBase + ParamServlet.NAME);
        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setDefaultUseCaches(true);
        assertTrue(urlCon.getUseCaches());
        assertTrue(urlCon.getDefaultUseCaches());

        urlCon.setDefaultUseCaches(false);
        urlCon = (HttpURLConnection)url.openConnection();
        assertFalse(urlCon.getUseCaches());
        assertFalse(urlCon.getDefaultUseCaches());

    }

    /***************************************************************************
     * This does not seem to work, even in the JDK stuff public void
     * testDoInput() throws Exception { URL url = new URL(_urlBase +
     * ParamServlet.NAME); HttpURLConnection urlCon =
     * (HttpURLConnection)url.openConnection(); urlCon.setDoInput(false); try {
     * urlCon.setRequestMethod("GET"); urlCon.connect(); fail("expected
     * exception because input not allowed"); } catch (Exception ex) { // this
     * is expected } }
     **************************************************************************/

    /***************************************************************************
     * This does not seem to work, even in the JDK stuff public void
     * testDefaultRequestProp() throws Exception { URL url = new URL(_urlBase +
     * HeaderServlet.NAME); int response = 0;
     * 
     * URLConnection.setDefaultRequestProperty("default", "request");
     * assertEquals("request",
     * URLConnection.getDefaultRequestProperty("default"));
     * 
     * HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
     * urlCon.setRequestMethod("GET");
     * 
     * assertEquals("request", urlCon.getRequestProperty("default"));
     * urlCon.connect(); response = urlCon.getResponseCode(); assertEquals(200,
     * response);
     * 
     * checkReply(urlCon, "name=\"default\";value=\"request\" <br>
     * "); }
     **************************************************************************/

    public void testResponseStuff() throws Exception
    {
        URL url = new URL(_urlBase + HeaderServlet.NAME);
        int response = 0;

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("GET");

        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        Object content = urlCon.getContent();
        _log.debug("content: " + content);
        assertEquals(null, urlCon.getContentEncoding());
        assertTrue(urlCon.getContentLength() >= 200);
        assertEquals("text/html", urlCon.getContentType());
        assertTrue(urlCon.getDate() > 0);
        assertTrue(urlCon.getExpiration() == 0);
        assertTrue(urlCon.getLastModified() == 0);
        assertEquals(java.net.SocketPermission.class, urlCon.getPermission()
                .getClass());

        assertEquals("text/html", URLConnection
                .guessContentTypeFromStream(urlCon.getInputStream()));

        _log.debug("Field 0: " + urlCon.getHeaderField(0));
        assertTrue(urlCon.getHeaderField(0).indexOf("HTTP") >= 0);
    }

    public void testGetOutput() throws Exception
    {
        URL url = new URL(_urlBase + HeaderServlet.NAME);
        int response = 0;

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("POST");
        urlCon.setDoOutput(true);

        urlCon.connect();

        // Make sure we can get an output stream after we connect
        OutputStream os = urlCon.getOutputStream();
        os.write("some stuff to the stream".getBytes("ASCII"));

        response = urlCon.getResponseCode();
        assertEquals(200, response);
    }

    public void testGetOutputFailed() throws Exception
    {
        URL url = new URL(_urlBase + HeaderServlet.NAME);
        int response = 0;

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("POST");
        urlCon.setDoOutput(true);

        urlCon.connect();

        response = urlCon.getResponseCode();
        assertEquals(200, response);

        try
        {
            // Make sure we can't get an output stream after we get the reply
            urlCon.getOutputStream();
            fail("Should get an exception on getOutputStream() call");
        }
        catch (IOException ex)
        {
            // the test worked
        }

    }

    public void testErrorStream() throws Exception
    {
        URL url = new URL(_urlBase + "/filenot_found/a/c/");
        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.connect();

        InputStream is = urlCon.getErrorStream();
        assertTrue(is != null);
        String errorStr = Util.getStringFromInputStream(is);
        assertTrue(errorStr.length() > 0);
        _log.debug("error stream: " + errorStr);
    }

    public void testErrorStreamGetResp() throws Exception
    {
        URL url = new URL(_urlBase + "/filenot_found/a/c/");
        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.connect();

        assertEquals(404, urlCon.getResponseCode());
        InputStream is = urlCon.getErrorStream();
        assertTrue(is != null);
        String errorStr = Util.getStringFromInputStream(is);
        assertTrue(errorStr.length() > 0);
        _log.debug("error stream: " + errorStr);
    }

    public void testErrorStreamNoError() throws Exception
    {
        URL url = new URL(_urlBase + HeaderServlet.NAME);
        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.connect();

        InputStream is = urlCon.getErrorStream();
        assertEquals(null, is);
    }

    public void testErrorStreamBeforeConnect() throws Exception
    {
        URL url = new URL(_urlBase + HeaderServlet.NAME);
        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();

        InputStream is = urlCon.getErrorStream();
        assertEquals(null, is);
    }

    // bug 281
    public void testBadHost() throws Exception
    {
        URL url = new URL("http://thisisbad");
        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        try
        {
            urlCon.connect();
            fail("Expected exception");
        }
        catch (java.net.UnknownHostException ex)
        {
            // got expected exception
        }

    }

    // bug 281
    public void testBadHost2() throws Exception
    {
        URL url = new URL("http://127.0.0.0");
        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        try
        {
            urlCon.connect();
            fail("Expected exception");
        }
        catch (java.net.ConnectException ex)
        {
            // got expected exception
        }
        catch (java.net.BindException ex)
        {
            // got expected exception
        }

    }

    // bug 281
    public void testBadPort() throws Exception
    {
        URL url = new URL("http://localhost:9999");
        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        try
        {
            urlCon.connect();
            fail("Expected exception");
        }
        catch (IOException ex)
        {
            // got expected exception
        }
    }

}
