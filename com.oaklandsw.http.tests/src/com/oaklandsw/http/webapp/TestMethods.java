package com.oaklandsw.http.webapp;

import java.io.InputStream;
import java.net.Socket;
import java.net.URL;

import org.apache.commons.logging.Log;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.ConnectionIOListener;
import com.oaklandsw.http.HttpConnectionManager;
import com.oaklandsw.http.HttpTestEnv;
import com.oaklandsw.http.HttpURLConnection;
import com.oaklandsw.http.servlet.ParamServlet;
import com.oaklandsw.http.servlet.RequestBodyServlet;
import com.oaklandsw.util.LogUtils;
import com.oaklandsw.util.SystemUtils;
import com.oaklandsw.util.Util;

public class TestMethods extends TestWebappBase
{

    private static final Log _log = LogUtils.makeLogger();

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
        // Netproxy does not like the delete method
        if (!_inAuthCloseProxyTest)
            doGetLikeMethod("DELETE", !CHECK_CONTENT);
    }

    public void testPutMethod() throws Exception
    {
        // Netproxy does not like the PUT method
        if (!_inAuthCloseProxyTest)
            doGetLikeMethod("PUT", CHECK_CONTENT);
    }

    // options does not seem to be allowed with apache 2.0.50
    public void NORUNtestOptionsMethod() throws Exception
    {
        doGetLikeMethod("OPTIONS", !CHECK_CONTENT);
        // assertTrue(method.getAllowedMethods().hasMoreElements());
    }

    public void testBigHTTP() throws Exception
    {
        String bigHttp = _urlBase.substring(0, 4).toUpperCase()
            + _urlBase.substring(4);
        URL url = new URL(bigHttp + RequestBodyServlet.NAME);
        int response = 0;

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");
        response = urlCon.getResponseCode();
        assertEquals(200, response);
        checkReply(urlCon, "GET");
    }

    // this test will fail with JDK 1.2 - and there is no way to fix it
    // it works fine on JDK 1.4 - see bug 303.
    public void testBigHTTP2() throws Exception
    {
        if (SystemUtils.isJavaVersionAtLeast(1.4f))
        {
            URL url = new URL("HTTP",
                              HttpTestEnv.TEST_WEBAPP_HOST,
                              HttpTestEnv.TEST_WEBAPP_PORT,
                              HttpTestEnv.TEST_URL_APP
                                  + RequestBodyServlet.NAME);
            int response = 0;

            HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
            urlCon.setRequestMethod("GET");
            response = urlCon.getResponseCode();
            assertEquals(200, response);
            checkReply(urlCon, "GET");
        }
    }

    public void testDirectHttpURLConnection() throws Exception
    {
        URL url = new URL(_urlBase + RequestBodyServlet.NAME);
        int response = 0;

        HttpURLConnection urlCon = com.oaklandsw.http.HttpURLConnection
                .openConnection(url);
        response = urlCon.getResponseCode();
        assertEquals(200, response);
    }

    public void testHeadMethodExplicitClose() throws Exception
    {
        URL url = new URL(_urlBase + RequestBodyServlet.NAME);

        // Make sure we don't require explicit close for the HEAD method
        for (int i = 0; i < com.oaklandsw.http.HttpURLConnection
                .getMaxConnectionsPerHost() + 5; i++)
        {
            HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
            urlCon.setRequestMethod("HEAD");
            assertEquals(200, urlCon.getResponseCode());
        }
    }

    // Test a sequence of calls used by Credit-Suisse (bug 1433)
    public void testGetCs() throws Exception
    {
        URL url = new URL(_urlBase + RequestBodyServlet.NAME);

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);

        urlCon.getHeaderField(1);

        InputStream in = urlCon.getInputStream();
        urlCon.getContentLength();
        Util.getStringFromInputStream(in);
        assertEquals(200, urlCon.getResponseCode());
    }

    class Lis implements ConnectionIOListener
    {

        HttpURLConnection _urlCon;

        public void read(byte[] bytes,
                         int offset,
                         int length,
                         Socket socket,
                         HttpURLConnection urlCon)
        {
            String result = new String(bytes, offset, length);
            assertTrue(result.indexOf("HTTP/1.1 200 OK") >= 0);
            assertNull(urlCon);
            // assertEquals(_urlCon, urlCon);
        }

        public void write(byte[] bytes,
                          int offset,
                          int length,
                          Socket socket,
                          HttpURLConnection urlCon)
        {
            String result = new String(bytes, offset, length);
            assertTrue(result.indexOf("GET /") >= 0);
            assertTrue(result.indexOf(" HTTP/1.1") >= 0);
            assertNull(urlCon);
            // assertEquals(_urlCon, urlCon);
        }
    }

    // Bug 2010 add method of accessing socket connection data
    public void testGetMethodSocketListener() throws Exception
    {
        HttpConnectionManager cm = HttpURLConnection.getConnectionManager();
        Lis ioListener = new Lis();
        cm.setSocketConnectionIOListener(ioListener);
        URL url = new URL(_urlBase + ParamServlet.NAME);

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        ioListener._urlCon = urlCon;
        urlCon.connect();
        getReply(urlCon);
    }

    public void allTestMethods() throws Exception
    {
        testGetMethod();
        testPostMethod();
        testHeadMethod();
        testDeleteMethod();
        testPutMethod();
        // testOptionsMethod();
    }

}
