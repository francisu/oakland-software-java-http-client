package com.oaklandsw.http.webapp;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.TestUserAgent;
import com.oaklandsw.http.servlet.BasicAuthServlet;
import com.oaklandsw.log.Log;
import com.oaklandsw.log.LogFactory;

public class TestBasicAuth extends TestWebappBase
{

    private static final Log _log = LogFactory.getLog(TestBasicAuth.class);

    public TestBasicAuth(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestBasicAuth.class);
        return suite;
    }

    public static void main(String args[])
    {
        mainRun(suite(), args);
    }

    public void testSimpleAuthGet() throws Exception
    {

        TestUserAgent._type = TestUserAgent.GOOD;
        URL url = new URL(_urlBase + BasicAuthServlet.NAME);
        int response = 0;

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("GET");

        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        String reply = getReply(urlCon);
        assertTrue(checkReplyNoAssert(reply,
                                      "<title>BasicAuth Servlet: GET</title>"));
        assertTrue(checkReplyNoAssert(reply,
                                      "<p>You have authenticated as \"jakarta:commons\"</p>"));
        checkNoActiveConns(url);

        // Try it again, getting the same connection hopefully
        urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("GET");

        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        reply = getReply(urlCon);
        assertTrue(checkReplyNoAssert(reply,
                                      "<title>BasicAuth Servlet: GET</title>"));
        assertTrue(checkReplyNoAssert(reply,
                                      "<p>You have authenticated as \"jakarta:commons\"</p>"));
        checkNoActiveConns(url);

    }

    public void testSimpleAuthPost() throws Exception
    {

        TestUserAgent._type = TestUserAgent.GOOD;
        URL url = new URL(_urlBase + BasicAuthServlet.NAME);
        int response = 0;

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("POST");
        urlCon.setDoOutput(true);
        OutputStream out = urlCon.getOutputStream();
        out.write("testing=one".getBytes("ASCII"));
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        String reply = getReply(urlCon);
        assertTrue(checkReplyNoAssert(reply,
                                      "<title>BasicAuth Servlet: POST</title>"));
        assertTrue(checkReplyNoAssert(reply,
                                      "<p>You have authenticated as \"jakarta:commons\"</p>"));
        checkNoActiveConns(url);

    }

    public void testSimpleAuthPut() throws Exception
    {

        TestUserAgent._type = TestUserAgent.GOOD;
        URL url = new URL(_urlBase + BasicAuthServlet.NAME);
        int response = 0;

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("PUT");
        urlCon.setDoOutput(true);
        OutputStream out = urlCon.getOutputStream();
        out.write("testing one two three".getBytes("ASCII"));
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        String reply = getReply(urlCon);
        assertTrue(checkReplyNoAssert(reply,
                                      "<title>BasicAuth Servlet: PUT</title>"));
        assertTrue(checkReplyNoAssert(reply,
                                      "<p>You have authenticated as \"jakarta:commons\"</p>"));

        checkNoActiveConns(url);
    }

    public void testNoCredAuthRetry() throws Exception
    {
        TestUserAgent._type = TestUserAgent.NULL;
        URL url = new URL(_urlBase + BasicAuthServlet.NAME);
        int response = 0;

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("GET");

        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(401, response);

        String reply = getReply(urlCon);
        assertTrue(checkReplyNoAssert(reply,
                                      "<title>BasicAuth Servlet: GET</title>"));
        assertTrue(checkReplyNoAssert(reply, "<p>Not authorized.</p>"));
        checkNoActiveConns(url);

        TestUserAgent._type = TestUserAgent.GOOD;
        url = new URL(_urlBase + BasicAuthServlet.NAME);
        urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("GET");

        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        reply = getReply(urlCon);
        assertTrue(checkReplyNoAssert(reply,
                                      "<title>BasicAuth Servlet: GET</title>"));
        assertTrue(checkReplyNoAssert(reply,
                                      "<p>You have authenticated as \"jakarta:commons\"</p>"));
        checkNoActiveConns(url);
    }

    public void testBadCredFails() throws Exception
    {

        TestUserAgent._type = TestUserAgent.NULL;
        URL url = new URL(_urlBase + BasicAuthServlet.NAME);
        int response = 0;

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("GET");

        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(401, response);

        String reply = getReply(urlCon);
        assertTrue(checkReplyNoAssert(reply,
                                      "<title>BasicAuth Servlet: GET</title>"));
        assertTrue(checkReplyNoAssert(reply, "<p>Not authorized.</p>"));
        checkNoActiveConns(url);

        TestUserAgent._type = TestUserAgent.BAD;
        url = new URL(_urlBase + BasicAuthServlet.NAME);
        urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("GET");

        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(401, response);

        reply = getReply(urlCon);
        assertTrue(checkReplyNoAssert(reply,
                                      "<title>BasicAuth Servlet: GET</title>"));
        assertTrue(checkReplyNoAssert(reply,
                                      "<p>Not authorized. \"Basic YmFkOmNyZWRz\" not recognized.</p>"));
        checkNoActiveConns(url);
    }

    public void allTestMethods() throws Exception
    {
        testSimpleAuthGet();
        testSimpleAuthPost();
        testSimpleAuthPut();
        testNoCredAuthRetry();
        testBadCredFails();
    }
}
