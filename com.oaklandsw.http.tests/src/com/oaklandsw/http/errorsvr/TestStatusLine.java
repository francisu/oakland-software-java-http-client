package com.oaklandsw.http.errorsvr;

import java.net.URL;

import org.apache.commons.logging.Log;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.HttpException;
import com.oaklandsw.http.HttpTestBase;
import com.oaklandsw.http.HttpTestEnv;
import com.oaklandsw.http.HttpURLConnection;
import com.oaklandsw.util.LogUtils;
import com.oaklandsw.util.URIUtil;

public class TestStatusLine extends HttpTestBase
{
    private static final Log _log = LogUtils.makeLogger();

    protected static String _errorUrl = HttpTestEnv.TEST_URL_HOST_ERRORSVR;

    protected String        _extraParam;

    public TestStatusLine(String testName)
    {
        super(testName);
    }

    public static void main(String args[])
    {
        mainRun(suite(), args);
    }

    public static Test suite()
    {
        return new TestSuite(TestStatusLine.class);
    }

    public void setUp() throws Exception
    {
        super.setUp();
        _extraParam = "";
    }

    // _ is used in the value for a " "
    public HttpURLConnection getStatus(String status) throws Exception
    {
        URL url = new URL(_errorUrl
            + "&status="
            + status
            + _extraParam
            + _errorDebug);

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("GET");
        urlCon.connect();
        urlCon.getResponseCode();
        return urlCon;
    }

    public void testSuccess(String extraParam, boolean singleEol)
        throws Exception
    {
        _extraParam = extraParam;
        HttpURLConnection urlCon = getStatus("HTTP/1.1_200_OK");
        assertEquals("HTTP/1.1 200 OK", urlCon.getHeaderField(0));
        assertEquals(200, urlCon.getResponseCode());
        assertEquals("OK", urlCon.getResponseMessage());
        checkErrorSvrData(urlCon, singleEol);
    }

    public void testSuccess() throws Exception
    {
        testSuccess("", false);
    }

    public void testSuccessN() throws Exception
    {
        testSuccess("&endOfLine=N", true);
    }

    public void testSuccessR() throws Exception
    {
        testSuccess("&endOfLine=R", true);
    }

    public void testSuccessRN() throws Exception
    {
        testSuccess("&endOfLine=RN", false);
    }

    public void testSuccessNR() throws Exception
    {
        // We don't support LFCR
        try
        {
            testSuccess("&endOfLine=NR", false);
        }
        catch (HttpException ex)
        {
            assertTrue("Wrong exception: " + ex.getMessage(), ex.getMessage()
                    .indexOf("LF after CR") >= 0);
        }
    }

    public void testMultiSpace() throws Exception
    {
        HttpURLConnection urlCon = getStatus("HTTP/1.1__200__OK");
        assertEquals("HTTP/1.1 200 OK", urlCon.getHeaderField(0));
        assertEquals(200, urlCon.getResponseCode());
        assertEquals("OK", urlCon.getResponseMessage());
    }

    public void test404NF1() throws Exception
    {
        HttpURLConnection urlCon = getStatus("HTTP/1.1_404_Not_Found");
        assertEquals("HTTP/1.1 404 Not Found", urlCon.getHeaderField(0));
        assertEquals(404, urlCon.getResponseCode());
        assertEquals("Not Found", urlCon.getResponseMessage());
    }

    public void test404NF2() throws Exception
    {
        String enc = URIUtil.encodeQuery("HTTP/1.1 404 Non Trouve");
        HttpURLConnection urlCon = getStatus(enc);
        assertEquals("HTTP/1.1 404 Non Trouve", urlCon.getHeaderField(0));
        assertEquals(404, urlCon.getResponseCode());
        assertEquals("Non Trouve", urlCon.getResponseMessage());
    }

    public void testNoMessage() throws Exception
    {
        HttpURLConnection urlCon = getStatus("HTTP/1.1_200_");
        assertEquals("HTTP/1.1 200", urlCon.getHeaderField(0));
        assertEquals(200, urlCon.getResponseCode());
        assertEquals("", urlCon.getResponseMessage());
    }

    public void testNoMessage2() throws Exception
    {
        HttpURLConnection urlCon = getStatus("HTTP/1.1_200");
        assertEquals("HTTP/1.1 200", urlCon.getHeaderField(0));
        assertEquals(200, urlCon.getResponseCode());
        assertEquals("", urlCon.getResponseMessage());
    }

    /***************************************************************************
     * 
     * //typical status line statusLine = new StatusLine( assertEquals("HTTP/1.1
     * 200 OK", statusLine.toString()); assertEquals("HTTP/1.1",
     * statusLine.getHttpVersion()); assertEquals(200,
     * statusLine.getStatusCode()); assretEquals("OK",
     * statusLine.getReasonPhrase());
     * 
     * //status line with multi word reason phrase statusLine = new
     * StatusLine("HTTP/1.1 404 Not Found"); assertEquals(404,
     * statusLine.getStatusCode()); assertEquals("Not Found",
     * statusLine.getReasonPhrase());
     * 
     * //reason phrase can be anyting statusLine = new StatusLine("HTTP/1.1 404
     * Non Trouv�"); assertEquals("Non Trouv�", statusLine.getReasonPhrase());
     * 
     * //its ok to end with a \n\r statusLine = new StatusLine("HTTP/1.1 404 Not
     * Found\n\r"); assertEquals("Not Found", statusLine.getReasonPhrase());
     * 
     * //this is valid according to the Status-Line BNF statusLine = new
     * StatusLine("HTTP/1.1 200 "); assertEquals(200,
     * statusLine.getStatusCode()); assertEquals("",
     * statusLine.getReasonPhrase());
     * 
     * //this is not strictly valid, but is lienent statusLine = new
     * StatusLine("HTTP/1.1 200"); assertEquals(200,
     * statusLine.getStatusCode()); assertEquals("",
     * statusLine.getReasonPhrase()); }
     * 
     * public void testFailure() throws Exception { try { statusLine = new
     * StatusLine("xxx 200 OK"); fail(); } catch (HttpException e) { // expected }
     * 
     * try { statusLine = new StatusLine("HTTP/1.1 xxx OK"); fail(); } catch
     * (HttpException e) { // expected } }
     * 
     **************************************************************************/
}
