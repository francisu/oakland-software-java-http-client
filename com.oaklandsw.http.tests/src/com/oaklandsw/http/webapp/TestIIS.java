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
import com.oaklandsw.http.HttpConnection;
import com.oaklandsw.http.HttpRetryException;
import com.oaklandsw.http.HttpTestBase;
import com.oaklandsw.http.HttpTestEnv;
import com.oaklandsw.http.HttpURLConnection;
import com.oaklandsw.http.PipelineTester;
import com.oaklandsw.http.TestUserAgent;
import com.oaklandsw.http.ntlm.NegotiateMessage;
import com.oaklandsw.http.servlet.RequestBodyServlet;
import com.oaklandsw.util.LogUtils;

public class TestIIS extends HttpTestBase
{
    private static final Log _log = LogUtils.makeLogger();

    String                   _getForm;

    HttpURLConnection        _urlCon;

    boolean                  _useIisNtlm_5;

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

        _doIsaProxyTest = true;

        _doExplicitTest = true;
        _doAppletTest = true;

        _doHttps = true;

        _useIisNtlm_5 = true;
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
        TestUserAgent._proxyType = TestUserAgent.GOOD;
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

    protected String makeIisUrl(String path)
    {
        if (_useIisNtlm_5)
            return HttpTestEnv.TEST_URL_IIS_5 + path;
        return HttpTestEnv.TEST_URL_IIS_0 + path;
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
        URL url = new URL(makeIisUrl(HttpTestEnv.TEST_URL_APP_IIS_FORM));
        int response = 0;

        String str = "lname=lastName123&fname=firstName123";

        _urlCon = HttpURLConnection.openConnection(url);
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
        URL url = new URL(makeIisUrl(HttpTestEnv.TEST_URL_APP_IIS_QUERY_STRING));
        int response = 0;

        HttpURLConnection urlCon;
        urlCon = HttpURLConnection.openConnection(url);
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

    public static final boolean MULTI = true;

    public void testConnectionReuse(boolean multi)
        throws MalformedURLException,
            IOException
    {
        URL url = new URL(makeIisUrl(HttpTestEnv.TEST_URL_APP_IIS_FORM));
        int response = 0;

        HttpURLConnection.setMultiCredentialsPerAddress(multi);

        HttpURLConnection urlCon;
        urlCon = HttpURLConnection.openConnection(url);
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        String reply = getReply(urlCon);
        iisCheckReply(reply);

        // Try again with a different user
        TestUserAgent._type = TestUserAgent.BAD;
        urlCon = HttpURLConnection.openConnection(url);
        response = urlCon.getResponseCode();
        // Multi will get a new connection, otherwise we will reuse the
        // connection
        if (multi)
            assertEquals(401, response);
        else
            assertEquals(200, response);

        reply = getReply(urlCon);
        checkNoActiveConns(url);
        TestUserAgent._type = TestUserAgent.GOOD;
    }

    public void test210ConnectionReuseMulti()
        throws MalformedURLException,
            IOException
    {
        testConnectionReuse(MULTI);
    }

    public void test211ConnectionReuseNoMulti()
        throws MalformedURLException,
            IOException
    {
        testConnectionReuse(!MULTI);
    }

    public void testConnectionReuse2(boolean multi)
        throws MalformedURLException,
            IOException
    {
        URL url = new URL(makeIisUrl(HttpTestEnv.TEST_URL_APP_IIS_FORM));
        int response = 0;

        HttpURLConnection.setMultiCredentialsPerAddress(multi);

        HttpURLConnection urlCon;
        urlCon = HttpURLConnection.openConnection(url);
        response = urlCon.getResponseCode();
        HttpConnection conn = urlCon.getConnection();
        assertEquals(200, response);

        String reply = getReply(urlCon);
        iisCheckReply(reply);

        // Try again with a different user
        TestUserAgent._type = TestUserAgent.GOOD2;
        urlCon = HttpURLConnection.openConnection(url);
        response = urlCon.getResponseCode();
        assertEquals(200, response);
        // Multi will get a new connection, otherwise we will reuse the
        // connection
        if (multi)
            assertTrue(conn != urlCon.getConnection());
        else
            assertTrue(conn == urlCon.getConnection());
        reply = getReply(urlCon);
        iisCheckReply(reply);

        checkNoActiveConns(url);
    }

    public void test212ConnectionReuseMulti2()
        throws MalformedURLException,
            IOException
    {
        testConnectionReuse2(MULTI);
    }

    public void test213ConnectionReuseNoMulti2()
        throws MalformedURLException,
            IOException
    {
        testConnectionReuse2(!MULTI);
    }

    // Bug 1949 - Test that an idle connection timeout frees up a connection
    public void test220ConnectWaitForIdle() throws Exception
    {
        URL url = new URL(makeIisUrl(HttpTestEnv.TEST_URL_APP_IIS_FORM));

        int waitTime = 2000;

        HttpURLConnection.setMultiCredentialsPerAddress(true);
        HttpURLConnection.setDefaultIdleConnectionTimeout(waitTime);

        long startTime = System.currentTimeMillis();

        HttpURLConnection.setMaxConnectionsPerHost(1);

        // Create connection with the good auth params
        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");
        urlCon.getResponseCode();
        urlCon.getInputStream().close();

        // Now try and create one with the bad params, should block
        // to wait for a connection since they are used up with the good params,
        // but then it should go when the idle connection timer pops
        TestUserAgent._type = TestUserAgent.BAD;
        urlCon = HttpURLConnection.openConnection(url);
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
        return !_inTestGroup || _in10ProxyTest || _inIsaProxyTest;
    }

    public void test230GetStream() throws MalformedURLException, IOException
    {
        URL url = new URL(makeIisUrl(HttpTestEnv.TEST_URL_APP_IIS_FORM));

        HttpURLConnection urlCon;
        urlCon = HttpURLConnection.openConnection(url);
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
        // LogUtils.logFile("/home/francis/log4jout.txt");
        // Pipelining does not work with netproxy
        if (_inAuthCloseProxyTest)
            return;
        HttpURLConnection.setDefaultAuthenticationType(Credential.AUTH_NTLM);
        PipelineTester pt = new PipelineTester(makeIisUrl(HttpTestEnv.TEST_URL_APP_IIS_QUERY_STRING),
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
    // Bug 2181
    public void test300GetBadCred() throws Exception
    {
        TestUserAgent._type = TestUserAgent.BAD;

        // Clean out connections
        HttpURLConnection.closeAllPooledConnections();

        URL url = new URL(makeIisUrl(HttpTestEnv.TEST_URL_APP_IIS_FORM));
        int response = 0;

        HttpURLConnection urlCon;
        urlCon = HttpURLConnection.openConnection(url);
        urlCon.disconnect();
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(401, response);

        checkNoTotalConns(url);

        // Make sure it works correctly with a good URL, after
        // a bad one, that is, it re-requests the credential
        TestUserAgent._type = TestUserAgent.GOOD;

        urlCon = HttpURLConnection.openConnection(url);
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        String reply = getReply(urlCon);
        iisCheckReply(reply);
        checkNoActiveConns(url);
    }

    // Make sure we get a proper failure upon a bad proxy credential
    // Bug 2181
    public void test301GetBadCredProxy() throws Exception
    {
        if (!_inIsaProxyTest)
            return;

        TestUserAgent._type = TestUserAgent.GOOD;
        int saveProxyType = TestUserAgent._proxyType;
        TestUserAgent._proxyType = TestUserAgent.NULL;

        // Clean out connections
        HttpURLConnection.closeAllPooledConnections();

        URL url = new URL(makeIisUrl(HttpTestEnv.TEST_URL_APP_IIS_FORM));
        int response = 0;

        HttpURLConnection urlCon;
        urlCon = HttpURLConnection.openConnection(url);
        urlCon.disconnect();
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(407, response);

        TestUserAgent._proxyType = saveProxyType;

        checkNoTotalConns(url);
    }

    // Bug 2181
    public void test305GetNullCred() throws Exception
    {
        TestUserAgent._type = TestUserAgent.NULL;

        // Clean out connections
        HttpURLConnection.closeAllPooledConnections();

        URL url = new URL(makeIisUrl(HttpTestEnv.TEST_URL_APP_IIS_FORM));
        int response = 0;

        HttpURLConnection urlCon;
        urlCon = HttpURLConnection.openConnection(url);
        urlCon.disconnect();
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(401, response);

        TestUserAgent._type = TestUserAgent.GOOD;

        // Make sure unauthenticated connection is not left around
        checkNoTotalConns(url);
    }

    // Bug 2181
    public void test310GetNullCredProxy() throws Exception
    {
        if (!_inIsaProxyTest)
            return;

        // Clean out connections
        HttpURLConnection.closeAllPooledConnections();

        TestUserAgent._type = TestUserAgent.GOOD;
        int saveProxyType = TestUserAgent._proxyType;
        TestUserAgent._proxyType = TestUserAgent.NULL;

        URL url = new URL(makeIisUrl(HttpTestEnv.TEST_URL_APP_IIS_FORM));
        int response = 0;

        HttpURLConnection urlCon;
        urlCon = HttpURLConnection.openConnection(url);
        urlCon.disconnect();
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(407, response);

        TestUserAgent._proxyType = saveProxyType;

        // Make sure we don't leave unauthenticated connection around
        checkNoTotalConns(url);
    }

    public void test320SimpleAuthGetCache() throws Exception
    {
        // Clean out connections
        HttpURLConnection.closeAllPooledConnections();

        HttpURLConnection.setMultiCredentialsPerAddress(false);
        
        TestUserAgent._type = TestUserAgent.GOOD;
        TestUserAgent._callCount = 0;

        URL url = new URL(makeIisUrl(HttpTestEnv.TEST_URL_APP_IIS_FORM));
        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        getReply(urlCon);

        assertEquals(1, TestUserAgent._callCount);

        // Now again
        urlCon = HttpURLConnection.openConnection(url);
        getReply(urlCon);

        assertEquals(1, TestUserAgent._callCount);

        HttpURLConnection.resetCachedCredentials();
        // For NTLM have to close the pooled connections because
        // it will just assume the credential matches
        HttpURLConnection.closeAllPooledConnections();

        // Make sure it asks again
        urlCon = HttpURLConnection.openConnection(url);
        getReply(urlCon);

        assertEquals(2, TestUserAgent._callCount);
    }

    public void test321SimpleAuthGetCacheProxy() throws Exception
    {
        if (!_inIsaProxyTest)
            return;

        // Clean out connections
        HttpURLConnection.closeAllPooledConnections();

        TestUserAgent._type = TestUserAgent.GOOD;
        TestUserAgent._proxyType = TestUserAgent.GOOD;
        TestUserAgent._callCount = 0;
        TestUserAgent._callCountProxy = 0;

        URL url = new URL(makeIisUrl(HttpTestEnv.TEST_URL_APP_IIS_FORM));
        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        getReply(urlCon);

        assertEquals(1, TestUserAgent._callCount);
        assertEquals(1, TestUserAgent._callCountProxy);

        // Now again
        urlCon = HttpURLConnection.openConnection(url);
        getReply(urlCon);

        assertEquals(1, TestUserAgent._callCount);
        assertEquals(1, TestUserAgent._callCountProxy);

        HttpURLConnection.resetCachedCredentials();
        // For NTLM have to close the pooled connections because
        // it will just assume the credential matches
        HttpURLConnection.closeAllPooledConnections();

        // Make sure it asks again
        urlCon = HttpURLConnection.openConnection(url);
        getReply(urlCon);

        assertEquals(2, TestUserAgent._callCount);
        assertEquals(2, TestUserAgent._callCountProxy);

        // Go to a different server with the same proxy, it should not
        // ask anything
        url = new URL(_urlBase + RequestBodyServlet.NAME);
        urlCon = HttpURLConnection.openConnection(url);
        getReply(urlCon);

        assertEquals(2, TestUserAgent._callCount);
        assertEquals(2, TestUserAgent._callCountProxy);
    }

    public void test325SimpleAuthGetCacheFail() throws Exception
    {
        // Clean out connections
        HttpURLConnection.closeAllPooledConnections();

        HttpURLConnection.setMultiCredentialsPerAddress(false);

        TestUserAgent._type = TestUserAgent.BAD;
        TestUserAgent._callCount = 0;

        URL url = new URL(makeIisUrl(HttpTestEnv.TEST_URL_APP_IIS_FORM));
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

    public void test326SimpleAuthGetCacheFailProxy() throws Exception
    {
        if (!_inIsaProxyTest)
            return;

        // Clean out connections
        HttpURLConnection.closeAllPooledConnections();

        TestUserAgent._type = TestUserAgent.GOOD;
        int saveProxyType = TestUserAgent._proxyType;
        TestUserAgent._proxyType = TestUserAgent.BAD;
        TestUserAgent._callCount = 0;
        TestUserAgent._callCountProxy = 0;

        URL url = new URL(makeIisUrl(HttpTestEnv.TEST_URL_APP_IIS_FORM));
        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        assertEquals(407, urlCon.getResponseCode());
        getReply(urlCon);

        assertEquals(0, TestUserAgent._callCount);
        assertEquals(1, TestUserAgent._callCountProxy);

        // Now again - but good
        TestUserAgent._proxyType = TestUserAgent.GOOD;
        urlCon = HttpURLConnection.openConnection(url);
        assertEquals(200, urlCon.getResponseCode());
        getReply(urlCon);

        assertEquals(1, TestUserAgent._callCount);
        assertEquals(2, TestUserAgent._callCountProxy);

        // Make sure does not ask again
        urlCon = HttpURLConnection.openConnection(url);
        assertEquals(200, urlCon.getResponseCode());
        getReply(urlCon);

        assertEquals(1, TestUserAgent._callCount);
        assertEquals(2, TestUserAgent._callCountProxy);
        TestUserAgent._proxyType = saveProxyType;
    }

    public void test330SimpleAuthGetNoCache() throws Exception
    {
        HttpURLConnection.setMultiCredentialsPerAddress(true);

        TestUserAgent._type = TestUserAgent.GOOD;
        TestUserAgent._callCount = 0;

        URL url = new URL(makeIisUrl(HttpTestEnv.TEST_URL_APP_IIS_FORM));
        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        getReply(urlCon);

        int callCount = TestUserAgent._callCount;

        // Now again - must ask a 2nd time
        urlCon = HttpURLConnection.openConnection(url);
        getReply(urlCon);

        assertTrue(TestUserAgent._callCount > callCount);

        HttpURLConnection.setMultiCredentialsPerAddress(false);
    }

    public void test331SimpleAuthGetNoCacheProxy() throws Exception
    {
        if (!_inIsaProxyTest)
            return;

        HttpURLConnection.setMultiCredentialsPerAddress(true);

        TestUserAgent._type = TestUserAgent.GOOD;
        TestUserAgent._proxyType = TestUserAgent.GOOD;
        TestUserAgent._callCount = 0;
        TestUserAgent._callCountProxy = 0;

        URL url = new URL(makeIisUrl(HttpTestEnv.TEST_URL_APP_IIS_FORM));
        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        getReply(urlCon);

        int callCount = TestUserAgent._callCount;
        int callCountProxy = TestUserAgent._callCount;

        // Now again - must ask a 2nd time
        urlCon = HttpURLConnection.openConnection(url);
        getReply(urlCon);

        assertTrue(TestUserAgent._callCount > callCount);
        // FIXME
        if (false)
            assertTrue(TestUserAgent._callCountProxy > callCountProxy);

        HttpURLConnection.setMultiCredentialsPerAddress(false);
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
        test210ConnectionReuseMulti();
        test211ConnectionReuseNoMulti();
        test205MultiGet();
        test205MultiGet();
        test230GetStream();

        // Pipelining does not appear to work through the ISA PROXY
        if (!_inIsaProxyTest)
        {
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
        }
        test300GetBadCred();
        test301GetBadCredProxy();
        test305GetNullCred();
        test310GetNullCredProxy();

        test320SimpleAuthGetCache();
        test321SimpleAuthGetCacheProxy();
        test325SimpleAuthGetCacheFail();
        test326SimpleAuthGetCacheFailProxy();
        test330SimpleAuthGetNoCache();
        test331SimpleAuthGetNoCacheProxy();

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

    // Test test force NTLM V1
    public void testForceNtlmV1() throws Exception
    {
        _useIisNtlm_5 = false;
        _inTestGroup = true;
        try
        {
            NegotiateMessage._testForceV1 = true;
            allTestMethods();
        }
        finally
        {
            _useIisNtlm_5 = true;
            NegotiateMessage._testForceV1 = false;
        }

    }

}
