package com.oaklandsw.http.webapp;

import java.io.OutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.apache.commons.logging.Log;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.HttpStatus;
import com.oaklandsw.http.HttpTestEnv;
import com.oaklandsw.http.servlet.ParamServlet;
import com.oaklandsw.http.servlet.RedirectServlet;
import com.oaklandsw.util.LogUtils;

public class TestRedirect extends TestWebappBase
{

    private static final Log   _log         = LogUtils.makeLogger();

    public TestRedirect(String testName)
    {
        super(testName);
    }

    public static void main(String[] args)
    {
        mainRun(suite(), args);
    }

    public static Test suite()
    {
        return new TestSuite(TestRedirect.class);
    }

    public void testRedirect(String method, int redirectCode) throws Exception
    {
        assertTrue(HttpURLConnection.getFollowRedirects());

        String qs = "";
        if (redirectCode > 0)
        {
            qs += "responseCode=" + redirectCode + "&";
        }
        qs += "to="
            + URLEncoder.encode("http://"
                + HttpTestEnv.TOMCAT_HOST
                + ":"
                + HttpTestEnv.TEST_WEBAPP_PORT
                + "/"
                + context
                + ParamServlet.NAME);
        int response = 0;

        URL url;

        String urlStr = _urlBase + RedirectServlet.NAME;
        if (method.equals("GET"))
            urlStr += "?" + qs;
        url = new URL(urlStr);

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod(method);

        if (!method.equals("GET"))
        {
            urlCon.setDoOutput(true);
            OutputStream os = urlCon.getOutputStream();
            os.write(qs.getBytes("ASCII"));
            if (method.equals("PUT"))
            {
                os.write("This is data to be sent in the body of an HTTP PUT."
                        .getBytes("ASCII"));
            }
        }

        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        String reply = getReply(urlCon);
        assertTrue(checkReplyNoAssert(reply, "<title>Param Servlet: "
            + method
            + "</title>"));
        checkNoActiveConns(url);
    }

    public void testRedirectGet301() throws Exception
    {
        testRedirect("GET", 301);
    }

    public void testRedirectGet302() throws Exception
    {
        testRedirect("GET", 302);
    }

    public void testRedirectGet307() throws Exception
    {
        testRedirect("GET", 307);
    }

    public void testRedirectPost301() throws Exception
    {
        testRedirect("POST", 301);
    }

    public void testRedirectPost302() throws Exception
    {
        testRedirect("POST", 302);
    }

    // Not implemented yet
    // Let's let people ask for this
//    public void testRedirectPost303() throws Exception
//    {
//        // This should be converted to a GET
//        testRedirect("POST", 303);
//    }

    public void testRedirectPost307() throws Exception
    {
        testRedirect("POST", 307);
    }

    private static final boolean FAIL = true;

    // Verify that a redirect goes to another host/port
    // bug 905 - redirect gets in loop if to different host
    public void testRedirectHostPort(String hostRedir,
                                     int portRedir,
                                     boolean fail) throws Exception
    {
        assertTrue(HttpURLConnection.getFollowRedirects());

        URL url = new URL(_urlBase
            + RedirectServlet.NAME
            + "?to="
            + URLEncoder.encode("http://"
                + hostRedir
                + ":"
                + portRedir
                + "/"
                + context
                + ParamServlet.NAME));
        int response = 0;

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("GET");

        // Bug 1029 host header not set correctly in redirection
        // Make sure Host: header is what we expect it is
        if (!fail)
            urlCon.setRequestProperty(ParamServlet.HOST_CHECK, hostRedir);

        urlCon.connect();
        try
        {
            response = urlCon.getResponseCode();
            if (fail)
            {
                if (urlCon.usingProxy())
                {
                    // Squid sends a 503, apache 2.x gives 404
                    if (response != 502 && response != 503 && response != 404)
                        fail("Proxy and not correct response");
                }
                else
                {
                    fail("Expected exception");
                }
            }
            else
            {
                assertEquals(200, response);
            }
        }
        catch (IOException ex)
        {
            if (!fail)
                fail("Got unexpected exception: " + ex);
            // Expected
        }

        checkNoActiveConns(url);
    }

    public void testRedirectHost() throws Exception
    {
        testRedirectHostPort("badhostxxx", HttpTestEnv.TEST_WEBAPP_PORT, FAIL);
    }

    public void testRedirectPort() throws Exception
    {
        testRedirectHostPort(HttpTestEnv.TOMCAT_HOST, 9999, FAIL);
    }

    public void testRedirectHostPort() throws Exception
    {
        testRedirectHostPort(getServletIpAddress(), HttpTestEnv.TEST_WEBAPP_PORT, !FAIL);
    }

    public void testNoRedirect() throws Exception
    {
        HttpURLConnection.setFollowRedirects(false);

        URL url = new URL(_urlBase
            + RedirectServlet.NAME
            + "?to="
            + URLEncoder.encode("http://"
                + HttpTestEnv.TOMCAT_HOST
                + ":"
                + HttpTestEnv.TEST_WEBAPP_PORT
                + "/"
                + context
                + ParamServlet.NAME));
        int response = 0;

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("GET");
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(302, response);

        HttpURLConnection.setFollowRedirects(true);
        if (com.oaklandsw.http.HttpURLConnection.getExplicitClose())
            urlCon.getInputStream().close();
        checkNoActiveConns(url);
    }

    public void testRelativeRedirect() throws Exception
    {
        URL url = new URL(_urlBase
            + RedirectServlet.NAME
            + "?to="
            + URLEncoder.encode("/" + context + "/params"));
        int response = 0;

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("GET");
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        String reply = getReply(urlCon);
        assertTrue(checkReplyNoAssert(reply,
                                      "<title>Param Servlet: GET</title>"));

        checkNoActiveConns(url);
    }

    public void testRedirectWithQueryString() throws Exception
    {
        String qs = URLEncoder.encode("http://"
            + HttpTestEnv.TOMCAT_HOST
            + ":"
            + HttpTestEnv.TEST_WEBAPP_PORT
            + "/"
            + context
            + "/params?foo=bar&bar=foo");

        URL url = new URL(_urlBase + RedirectServlet.NAME + "?to=" + qs);
        int response = 0;

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("GET");
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        String reply = getReply(urlCon);
        assertTrue(checkReplyNoAssert(reply,
                                      "<title>Param Servlet: GET</title>"));
        assertTrue(checkReplyNoAssert(reply,
                                      "<p>QueryString=\"foo=bar&bar=foo\"</p>"));
        checkNoActiveConns(url);
    }

    public void testRecursiveRedirect() throws Exception
    {
        String qs = "to="
            + URLEncoder.encode("http://"
                + HttpTestEnv.TOMCAT_HOST
                + ":"
                + HttpTestEnv.TEST_WEBAPP_PORT
                + "/"
                + context
                + "/params?foo=bar");
        for (int i = 0; i < 10; i++)
        {
            qs = "to="
                + URLEncoder.encode("http://"
                    + HttpTestEnv.TOMCAT_HOST
                    + ":"
                    + HttpTestEnv.TEST_WEBAPP_PORT
                    + "/"
                    + context
                    + "/redirect?"
                    + qs);
        }

        URL url = new URL(_urlBase + RedirectServlet.NAME + "?" + qs);
        int response = 0;

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("GET");
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        String reply = getReply(urlCon);
        assertTrue(checkReplyNoAssert(reply,
                                      "<title>Param Servlet: GET</title>"));
        assertTrue(checkReplyNoAssert(reply, "<p>QueryString=\"foo=bar\"</p>"));
        checkNoActiveConns(url);
    }

    public void testPutRedirect() throws Exception
    {
        String qs = "to="
            + URLEncoder.encode("http://"
                + HttpTestEnv.TOMCAT_HOST
                + ":"
                + HttpTestEnv.TEST_WEBAPP_PORT
                + "/"
                + context
                + "/params?foo=bar&bar=foo");
        URL url = new URL(_urlBase + RedirectServlet.NAME);
        int response = 0;

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("PUT");
        urlCon.setDoOutput(true);

        OutputStream os = urlCon.getOutputStream();
        os.write(qs.getBytes("ASCII"));
        os.write("This is data to be sent in the body of an HTTP PUT."
                .getBytes("ASCII"));

        urlCon.connect();

        // Should not redirect
        response = urlCon.getResponseCode();
        assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, response);

        if (com.oaklandsw.http.HttpURLConnection.getExplicitClose())
            urlCon.getInputStream().close();
        checkNoActiveConns(url);
    }

    public void testDetectRedirectLoop() throws Exception
    {

        URL url = new URL(_urlBase + RedirectServlet.NAME + "?loop=true");

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("GET");

        try
        {
            urlCon.getResponseCode();
            fail("Did not receive expected exception about a loop");
        }
        catch (java.net.ProtocolException ex)
        {
            // Expected this exception
        }
        checkNoActiveConns(url);

    }

    // For explicit close testing
    public void allTestMethods() throws Exception
    {
        testRedirectGet301();
        testRedirectGet302();
        testRedirectGet307();
        testRedirectPost301();
        testRedirectPost302();
        testRedirectPost307();
        testRedirectHost();
        testRedirectPort();
        testNoRedirect();
        testRelativeRedirect();
        testRedirectWithQueryString();
        testRecursiveRedirect();
        testPutRedirect();
        testDetectRedirectLoop();
        doGetLikeMethod("GET", CHECK_CONTENT);
    }

}
