package com.oaklandsw.http.webext;

import com.oaklandsw.http.HttpTestBase;
import com.oaklandsw.http.HttpTestEnv;
import com.oaklandsw.http.HttpURLConnection;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.IOException;

import java.net.URL;


public class TestNonProxyHost extends HttpTestBase {
    protected boolean _perConnection;

    public TestNonProxyHost(String testName) {
        super(testName);
    }

    public static void main(String[] args) {
        mainRun(suite(), args);
    }

    public static Test suite() {
        return new TestSuite(TestNonProxyHost.class);
    }

    public void setUp() throws Exception {
        super.setUp();

        if (!_perConnection) {
            com.oaklandsw.http.HttpURLConnection.setProxyHost(HttpTestEnv.TEST_PROXY_HOST);
            com.oaklandsw.http.HttpURLConnection.setProxyPort(HttpTestEnv.NORMAL_PROXY_PORT);
        }
    }

    public void tearDown() throws Exception {
        super.tearDown();
        com.oaklandsw.http.HttpURLConnection.setProxyHost(null);
        com.oaklandsw.http.HttpURLConnection.setProxyPort(-1);
    }

    public void testNPHost(String hosts, String connectHost, boolean proxied)
        throws IOException {
        int response = 0;
        HttpURLConnection.setNonProxyHosts(hosts);

        URL url = new URL("http://" + connectHost + "/");

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);

        if (_perConnection) {
            urlCon.setConnectionProxyHost(HttpTestEnv.TEST_PROXY_HOST);
            urlCon.setConnectionProxyPort(HttpTestEnv.NORMAL_PROXY_PORT);
        }

        urlCon.setRequestMethod("GET");
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        boolean urlConUsingProxy = !HttpURLConnection.isNonProxyHost(connectHost);
        boolean connUsingProxy = urlCon.getConnection().isProxied();

        // We ignore the non proxy host mechanism when the proxy is set per-connection
        if (_perConnection) {
            urlConUsingProxy = true;
            proxied = true;
        }

        assertEquals("Disagreement on using proxy", urlConUsingProxy,
            connUsingProxy);

        if (proxied) {
            assertTrue("Unexpectedly using proxy", connUsingProxy);
        } else {
            assertFalse("Unexpectedly NOT using proxy", connUsingProxy);
        }

        String data = HttpTestBase.getReply(urlCon);
        assertTrue("No data returned.", (data.length() > 0));
        checkNoActiveConns(url);
    }

    public void testNPHostSimple() throws IOException {
        testNPHost("java.sun.com", "java.sun.com", false);
    }

    // bug 1766 align non-proxy host with Java specification
    public void testNPHostWild() throws IOException {
        testNPHost("*.com", "java.sun.com", false);
    }

    // bug 1766 align non-proxy host with Java specification
    public void testNPHostWild2() throws IOException {
        testNPHost("abc|qed|def|a.b.c|*.com", "java.sun.com", false);
    }

    public void testNPHostWild3() throws IOException {
        testNPHost("abc|qed|def|a.b.c", "java.sun.com", true);
    }

    public void testNPHostBadSyntax() throws IOException {
        try {
            com.oaklandsw.http.HttpURLConnection.setNonProxyHosts("(**^^.*.com");
            fail("Did not get expected exception");
        } catch (RuntimeException ex) {
            assertTrue("Incorrect exception",
                ex.getMessage().indexOf("Invalid syntax") >= 0);
        }
    }

    public void allTestMethods() throws Exception {
        testNPHostSimple();
        testNPHostWild();
        testNPHostWild2();
        testNPHostBadSyntax();
    }
}
