package com.oaklandsw.http.webapp;

import java.io.OutputStream;
import java.net.URL;

import org.apache.commons.logging.Log;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.HttpURLConnection;
import com.oaklandsw.http.TestUserAgent;
import com.oaklandsw.http.servlet.BasicAuthServlet;
import com.oaklandsw.util.LogUtils;

public class TestBasicAuth extends TestWebappBase
{

    private static final Log   _log         = LogUtils.makeLogger();

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

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
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
        urlCon = HttpURLConnection.openConnection(url);
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

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
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
        if (_inAuthCloseProxyTest)
            return;

        TestUserAgent._type = TestUserAgent.GOOD;
        URL url = new URL(_urlBase + BasicAuthServlet.NAME);
        int response = 0;

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
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

        HttpURLConnection.setMultiCredentialsPerAddress(true);
        
        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
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
        urlCon = HttpURLConnection.openConnection(url);
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

        HttpURLConnection.setMultiCredentialsPerAddress(false);
        
    }

    public void testBadCredFails() throws Exception
    {
        TestUserAgent._type = TestUserAgent.NULL;
        URL url = new URL(_urlBase + BasicAuthServlet.NAME);
        int response = 0;

        HttpURLConnection.setMultiCredentialsPerAddress(true);

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
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
        urlCon = HttpURLConnection.openConnection(url);
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
        
        HttpURLConnection.setMultiCredentialsPerAddress(false);
    }

    public void testSimpleAuthGetCache() throws Exception
    {
        TestUserAgent._type = TestUserAgent.GOOD;
        TestUserAgent._callCount = 0;
        
        URL url = new URL(_urlBase + BasicAuthServlet.NAME);
        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        getReply(urlCon);
        
        assertEquals(1, TestUserAgent._callCount);
        
        // Now again
        urlCon = HttpURLConnection.openConnection(url);
        getReply(urlCon);

        assertEquals(1, TestUserAgent._callCount);
        
        HttpURLConnection.resetCachedCredentials();

        // Make sure it asks again
        urlCon = HttpURLConnection.openConnection(url);
        getReply(urlCon);

        assertEquals(2, TestUserAgent._callCount);
    }
    
    public void testSimpleAuthGetCacheFail() throws Exception
    {
        TestUserAgent._type = TestUserAgent.BAD;
        TestUserAgent._callCount = 0;
        
        URL url = new URL(_urlBase + BasicAuthServlet.NAME);
        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        assertEquals(401, urlCon.getResponseCode());
        getReply(urlCon);
        
        assertEquals(1, TestUserAgent._callCount);
        
        // Now again - but good
        TestUserAgent._type = TestUserAgent.GOOD;
        urlCon = HttpURLConnection.openConnection(url);
        assertEquals(200, urlCon.getResponseCode());
        getReply(urlCon);

        assertEquals(2, TestUserAgent._callCount);
        
        // Make sure does not ask again
        urlCon = HttpURLConnection.openConnection(url);
        assertEquals(200, urlCon.getResponseCode());
        getReply(urlCon);

        assertEquals(2, TestUserAgent._callCount);
    }
    
    public void testSimpleAuthGetNoCache() throws Exception
    {
        HttpURLConnection.setMultiCredentialsPerAddress(true);

        TestUserAgent._type = TestUserAgent.GOOD;
        TestUserAgent._callCount = 0;
        
        URL url = new URL(_urlBase + BasicAuthServlet.NAME);
        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        getReply(urlCon);
        
        assertEquals(1, TestUserAgent._callCount);
        
        // Now again - must ask a 2nd time
        urlCon = HttpURLConnection.openConnection(url);
        getReply(urlCon);

        assertEquals(2, TestUserAgent._callCount);

        HttpURLConnection.setMultiCredentialsPerAddress(false);
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
