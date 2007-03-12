package com.oaklandsw.http.webext;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.HttpTestBase;
import com.oaklandsw.http.HttpTestEnv;

public class TestNonProxyHost extends HttpTestBase
{

    public TestNonProxyHost(String testName)
    {
        super(testName);
    }

    public static void main(String args[])
    {
        mainRun(suite(), args);
    }

    public static Test suite()
    {
        return new TestSuite(TestNonProxyHost.class);
    }

    public void setUp() throws Exception
    {
        super.setUp();
        com.oaklandsw.http.HttpURLConnection.setProxyHost(HttpTestEnv.TEST_PROXY_HOST);
        com.oaklandsw.http.HttpURLConnection.setProxyPort(HttpTestEnv.TEST_PROXY_PORT);
    }

    public void tearDown() throws Exception
    {
        super.tearDown();
        com.oaklandsw.http.HttpURLConnection.setProxyHost(null);
        com.oaklandsw.http.HttpURLConnection.setProxyPort(-1);
    }

    public void testNPHost(String hosts, String connectHost, boolean proxied)
        throws IOException
    {
        int response = 0;
        com.oaklandsw.http.HttpURLConnection.setNonProxyHosts(hosts);
        URL url = new URL("http://" + connectHost + "/");

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("GET");
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);
        if (proxied)
            assertTrue("Unexpectedly using proxy", urlCon.usingProxy());
        else
            assertFalse("Unexpectedly using proxy", urlCon.usingProxy());

        String data = HttpTestBase.getReply(urlCon);
        assertTrue("No data returned.", (data.length() > 0));
        checkNoActiveConns(url);
    }

    public void testNPHostSimple() throws IOException
    {
        testNPHost("java.sun.com", "java.sun.com", false);
    }

    // bug 1766 align non-proxy host with Java specification
    public void testNPHostWild() throws IOException
    {
        testNPHost("*.com", "java.sun.com", false);
    }

    // bug 1766 align non-proxy host with Java specification
    public void testNPHostWild2() throws IOException
    {
        testNPHost("abc|qed|def|a.b.c|*.com", "java.sun.com", false);
    }

    public void testNPHostWild3() throws IOException
    {
        testNPHost("abc|qed|def|a.b.c", "java.sun.com", true);
    }

    public void testNPHostBadSyntax() throws IOException
    {
        try
        {
            com.oaklandsw.http.HttpURLConnection.setNonProxyHosts("(**^^.*.com");
            fail("Did not get expected exception");
        }
        catch (RuntimeException ex)
        {
            assertTrue("Incorrect exception", ex.getMessage()
                    .indexOf("Invalid syntax") >= 0);
        }
    }

    public void allTestMethods() throws Exception
    {
        testNPHostSimple();
        testNPHostWild();
        testNPHostWild2();
        testNPHostBadSyntax();
    }

}
