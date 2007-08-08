// Copyright 2002 oakland software, All rights reserved

package com.oaklandsw.http.webapp;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.logging.Log;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.Credential;
import com.oaklandsw.http.HttpRetryException;
import com.oaklandsw.http.HttpTestBase;
import com.oaklandsw.http.HttpTestEnv;
import com.oaklandsw.http.HttpURLConnection;
import com.oaklandsw.http.PipelineTester;
import com.oaklandsw.http.TestUserAgent;
import com.oaklandsw.http.ntlm.NegotiateMessage;
import com.oaklandsw.util.LogUtils;

public class TestIIS extends HttpTestBase
{
    private static final Log _log = LogUtils.makeLogger();

    String                   _getForm;

    HttpURLConnection        _urlCon;

    public TestIIS(String name)
    {
        super(name);
        // These are actually disabled by the check in allTestMethods()
        // See the comments about NTLM in HttpTestBase for more information
        _doAuthCloseProxyTest = true;
        _doProxyTest = true;
        _doAuthProxyTest = true;

        // Squid works fine as a proxy for NTLM
        _do10ProxyTest = true;

        _doExplicitTest = true;
        _doAppletTest = true;

        _doHttps = true;
    }

    public static void main(String[] args)
    {
        mainRun(suite(), args);
    }

    // We assume the web server is running
    public void setUp() throws Exception
    {
        super.setUp();
        TestUserAgent._type = TestUserAgent.GOOD;
        _getForm = "TestForm2.asp";
        _showStats = true;
    }

    public void tearDown() throws Exception
    {
        super.tearDown();
        _urlCon = null;
    }

    public static Test suite()
    {
        return new TestSuite(TestIIS.class);
    }

    public void iisCheckReply(String reply)
    {
        assertTrue("Incorrect reply",
                   reply.indexOf("This sample is provided") >= 0);
        assertTrue("Incorrect reply", reply.indexOf("This page will take") >= 0);
        assertTrue("Incorrect reply", reply.indexOf("</HTML>") >= 0);
    }

    public void test100PostNormal() throws MalformedURLException, IOException
    {
        URL url = new URL(HttpTestEnv.TEST_URL_IIS
            + HttpTestEnv.TEST_URL_APP_IIS_FORM);
        int response = 0;

        String str = "lname=lastName123&fname=firstName123";

        _urlCon = (HttpURLConnection)url.openConnection();
        _urlCon.setAuthenticationType(Credential.AUTH_NTLM);
        setupStreaming(_urlCon, str.length());
        _urlCon.setRequestMethod("POST");
        _urlCon.setDoOutput(true);
        OutputStream outStr = _urlCon.getOutputStream();
        outStr.write(str.getBytes("ASCII"));
        outStr.close();
        response = _urlCon.getResponseCode();
        assertEquals(200, response);

        String reply = getReply(_urlCon);
        assertTrue("Incorrect reply", reply.indexOf("firstName123") >= 0);
        assertTrue("Incorrect reply", reply.indexOf("lastName123") >= 0);
        iisCheckReply(reply);
        // HttpURLConnection.dumpAll();
        checkNoActiveConns(url);
    }

    public void test110PostClose() throws MalformedURLException, IOException
    {
        test100PostNormal();
        _urlCon.disconnect();
    }

    public void test120MultiPostClose()
        throws MalformedURLException,
            IOException
    {
        test110PostClose();
        test110PostClose();
        test100PostNormal();
        test110PostClose();
        test100PostNormal();
    }

    public void test130PostChunked() throws MalformedURLException, IOException
    {
        _streamingType = STREAM_CHUNKED;
        HttpURLConnection.setDefaultAuthenticationType(Credential.AUTH_NTLM);
        test100PostNormal();
        _streamingType = STREAM_NONE;
    }

    public void test130PostFixed() throws MalformedURLException, IOException
    {
        _streamingType = STREAM_FIXED;
        HttpURLConnection.setDefaultAuthenticationType(Credential.AUTH_NTLM);
        test100PostNormal();
        _streamingType = STREAM_NONE;
    }

    public void test200Get() throws MalformedURLException, IOException
    {
        URL url = new URL(HttpTestEnv.TEST_URL_IIS
            + HttpTestEnv.TEST_URL_APP_IIS_QUERY_STRING);
        int response = 0;

        HttpURLConnection urlCon;
        urlCon = (HttpURLConnection)url.openConnection();
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        String reply = getReply(urlCon);
        iisCheckReply(reply);
        checkNoActiveConns(url);
    }

    public void test205MultiGet() throws MalformedURLException, IOException
    {
        test200Get();
        test200Get();
        test200Get();
    }

    public void test210ConnectionReuse()
        throws MalformedURLException,
            IOException
    {
        URL url = new URL(HttpTestEnv.TEST_URL_IIS
            + HttpTestEnv.TEST_URL_APP_IIS_FORM);
        int response = 0;

        HttpURLConnection urlCon;
        urlCon = (HttpURLConnection)url.openConnection();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        String reply = getReply(urlCon);
        iisCheckReply(reply);

        // Try again with a different user, should not reuse the
        // same connection
        TestUserAgent._type = TestUserAgent.BAD;
        urlCon = (HttpURLConnection)url.openConnection();
        response = urlCon.getResponseCode();
        assertEquals(401, response);
        checkNoActiveConns(url);
    }

    // Bug 1949 - Test that an idle connection timeout frees up a connection
    public void test210ConnectWaitForIdle() throws Exception
    {
        URL url = new URL(HttpTestEnv.TEST_URL_IIS
            + HttpTestEnv.TEST_URL_APP_IIS_FORM);

        int waitTime = 2000;

        HttpURLConnection.setDefaultIdleConnectionTimeout(waitTime);

        long startTime = System.currentTimeMillis();

        HttpURLConnection.setMaxConnectionsPerHost(1);

        // Create connection with the good auth params
        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("GET");
        urlCon.getResponseCode();
        urlCon.getInputStream().close();

        // Now try and create one with the bad params, should block
        // to wait for a connection since they are used up with the good params,
        // but then it should go when the idle connection timer pops
        TestUserAgent._type = TestUserAgent.BAD;
        urlCon = (HttpURLConnection)url.openConnection();
        assertEquals(401, urlCon.getResponseCode());

        // The failed connection will be closed, and the idle connection should
        // be gone
        assertEquals(0, getTotalConns(url));

        // Make sure we actually waited for the idle timeout
        assertTrue(System.currentTimeMillis() - startTime > waitTime);
    }

    protected boolean isExpectRetryException()
    {
        // If in a group of tests, this test is in the middle so
        // the connection will have been authenticated, except when
        // we are running HTTP 1.0 which closes the connection
        // each time
        return !_inTestGroup || _in10ProxyTest;
    }

    public void test230GetStream() throws MalformedURLException, IOException
    {
        URL url = new URL(HttpTestEnv.TEST_URL_IIS
            + HttpTestEnv.TEST_URL_APP_IIS_FORM);

        HttpURLConnection urlCon;
        urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setFixedLengthStreamingMode(5);

        urlCon.setDoOutput(true);
        urlCon.getOutputStream().write("12345".getBytes("ASCII"));
        urlCon.getOutputStream().close();

        try
        {
            // Since authentication happens, it should fail at this point
            urlCon.getResponseCode();
            // We are in a group of tests, the initial tests will authenticate
            // the connection, so we won't fail
            if (isExpectRetryException())
                fail("Should have gotten retry exception");

            urlCon.getInputStream().close();
        }
        catch (HttpRetryException ex)
        {
            if (!isExpectRetryException())
                fail("Should not get exception here because already authenticated");
            assertEquals("Unexpected response code", 401, ex.responseCode());
        }
        checkNoActiveConns(url);
    }

    public void test240GetPipeline(int num) throws Exception
    {
         //LogUtils.logFile("/home/francis/log4jout.txt");
        // Pipelining does not work with netproxy
        if (_inAuthCloseProxyTest)
            return;
        HttpURLConnection.setDefaultAuthenticationType(Credential.AUTH_NTLM);
        PipelineTester pt = new PipelineTester(HttpTestEnv.TEST_URL_IIS
                                                   + HttpTestEnv.TEST_URL_APP_IIS_QUERY_STRING,
                                               num,
                                               _pipelineOptions,
                                               _pipelineMaxDepth);
        assertFalse(pt.runTest());
    }

    public void test240GetPipeline1() throws Exception
    {
        test240GetPipeline(1);
    }

    public void test240GetPipeline2() throws Exception
    {
        test240GetPipeline(2);
    }

    public void test240GetPipeline10() throws Exception
    {
        test240GetPipeline(10);
    }

    public void test240GetPipeline100() throws Exception
    {
        test240GetPipeline(100);
    }

    // Make sure we get a proper failure upon a bad credential
    public void test300GetBadCred() throws MalformedURLException, IOException
    {
        TestUserAgent._type = TestUserAgent.BAD;

        URL url = new URL(HttpTestEnv.TEST_URL_IIS
            + HttpTestEnv.TEST_URL_APP_IIS_FORM);
        int response = 0;

        HttpURLConnection urlCon;
        urlCon = (HttpURLConnection)url.openConnection();
        urlCon.disconnect();
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(401, response);

        // Make sure it works correctly with a good URL, after
        // a bad one, that is, it re-requests the credential
        TestUserAgent._type = TestUserAgent.GOOD;

        urlCon = (HttpURLConnection)url.openConnection();
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        String reply = getReply(urlCon);
        iisCheckReply(reply);
        checkNoActiveConns(url);
    }

    public void test305GetNullCred() throws MalformedURLException, IOException
    {
        TestUserAgent._type = TestUserAgent.NULL;

        URL url = new URL(HttpTestEnv.TEST_URL_IIS
            + HttpTestEnv.TEST_URL_APP_IIS_FORM);
        int response = 0;

        HttpURLConnection urlCon;
        urlCon = (HttpURLConnection)url.openConnection();
        urlCon.disconnect();
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(401, response);

        TestUserAgent._type = TestUserAgent.GOOD;
        checkNoActiveConns(url);
    }

    public void test400MultiGetPost() throws MalformedURLException, IOException
    {
        test200Get();
        test100PostNormal();
        test200Get();
        test100PostNormal();
        test200Get();
        test100PostNormal();
    }

    public void allTestMethods() throws Exception
    {
        if (!isAllowNtlmProxy())
            return;

        _showStats = false;
        if (false && _in10ProxyTest)
            LogUtils.logFile("/home/francis/log4j10proxy.txt");

        // LogUtils.logWireOnly();

        test100PostNormal();
        test110PostClose();
        test120MultiPostClose();
        test200Get();
        test205MultiGet();
        test205MultiGet();
        test230GetStream();
        test240GetPipeline1();
        test240GetPipeline2();

        if (false && _in10ProxyTest)
            LogUtils.logConnOnly();
        test240GetPipeline10();
        int count = 1;
        if (_in10ProxyTest)
            count = 5;
        for (int i = 0; i < count; i++)
        {
            test240GetPipeline100();
            if (false && _in10ProxyTest)
                HttpURLConnection.dumpAll();
        }

        if (false && _in10ProxyTest)
        {
            System.out.println("****sleeping for profile");
            Thread.sleep(10000000);
        }

        test300GetBadCred();
        test305GetNullCred();
        test400MultiGetPost();

    }

    public void testBadEncoding()
    {
        try
        {
            // Bad value
            com.oaklandsw.http.HttpURLConnection.setNtlmPreferredEncoding(18);
            fail("Expected exception");
        }
        catch (IllegalArgumentException ex)
        {

        }
    }

    // Test everything with NTLM OEM response forced
    public void testForceOem() throws Exception
    {
        com.oaklandsw.http.HttpURLConnection
                .setNtlmPreferredEncoding(com.oaklandsw.http.HttpURLConnection.NTLM_ENCODING_OEM);
        _inTestGroup = true;
        allTestMethods();
        com.oaklandsw.http.HttpURLConnection
                .setNtlmPreferredEncoding(com.oaklandsw.http.HttpURLConnection.NTLM_ENCODING_UNICODE);
    }

    // Test test force NTLM V1 - this test will not pass if the NTLMv2 is set
    public void testForceNtlmV1() throws Exception
    {
        // Don't do this if only NTLM v2 is available
        if (HttpTestEnv.REQUIRE_NTLMV2 == null)
        {
            NegotiateMessage._testForceV1 = true;
            allTestMethods();
            NegotiateMessage._testForceV1 = false;
        }
    }

}
