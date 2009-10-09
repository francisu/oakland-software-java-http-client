package com.oaklandsw.http.local;

import com.oaklandsw.http.HttpTestBase;
import com.oaklandsw.http.HttpURLConnection;

import junit.framework.Test;
import junit.framework.TestSuite;


public class TestProperties extends HttpTestBase {
    static final String proxyHost = "testproxyhost";
    static final int proxyPort = 123;
    static final String nonProxyHosts = "test1|.*.test2.com|123.345.222.333|.*.com";

    public TestProperties(String testName) {
        super(testName);
    }

    public static void main(String[] args) {
        String[] testCaseName = { TestProperties.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }

    public static Test suite() {
        return new TestSuite(TestProperties.class);
    }

    public static void setProperties() {
        // Set property values before TestEnv.setUp is called as
        // the properties are detected in the HttpURLConnection
        // static initializer
        System.setProperty("http.proxyHost", proxyHost);
        System.setProperty("http.proxyPort", Integer.toString(proxyPort));
        System.setProperty("http.nonProxyHosts", nonProxyHosts);

        System.setProperty("com.oaklandsw.http.pipelining", "true");
    }

    public static void resetProperties() {
        System.getProperties().remove("http.proxyHost");
        System.getProperties().remove("http.proxyPort");
        System.getProperties().remove("https.proxyHost");
        System.getProperties().remove("https.proxyPort");
        System.getProperties().remove("http.nonProxyHosts");
        System.getProperties().remove("com.oaklandsw.http.pipelining");
        System.getProperties().remove("com.oaklandsw.http.skipEnvironmentInit");
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
        resetProperties();
    }

    public void testPropsSkip() throws Exception {
        System.setProperty("com.oaklandsw.http.skipEnvironmentInit", "true");
        setProperties();
        HttpURLConnection.resetGlobalState();
        assertEquals(null, com.oaklandsw.http.HttpURLConnection.getProxyHost());
        assertEquals(-1, com.oaklandsw.http.HttpURLConnection.getProxyPort());
        assertEquals(null,
            com.oaklandsw.http.HttpURLConnection.getNonProxyHosts());
    }

    public void testProps() throws Exception {
        setProperties();
        HttpURLConnection.resetGlobalState();
        assertEquals(proxyHost,
            com.oaklandsw.http.HttpURLConnection.getProxyHost());
        assertEquals(proxyPort,
            com.oaklandsw.http.HttpURLConnection.getProxyPort());
        assertEquals(nonProxyHosts,
            com.oaklandsw.http.HttpURLConnection.getNonProxyHosts());
        assertEquals(true,
            com.oaklandsw.http.HttpURLConnection.isDefaultPipelining());
    }

    public void testPropsSslProxy() throws Exception {
        System.setProperty("https.proxyHost", proxyHost);
        System.setProperty("https.proxyPort", Integer.toString(proxyPort));
        HttpURLConnection.resetGlobalState();
        assertEquals(proxyHost,
            com.oaklandsw.http.HttpURLConnection.getProxyHost());
        assertEquals(proxyPort,
            com.oaklandsw.http.HttpURLConnection.getProxyPort());
        assertEquals(true, com.oaklandsw.http.HttpURLConnection.isProxySsl());
    }

    // 961 and 966
    public void testDefaultTimeoutValues() throws Exception {
        assertEquals(14000,
            com.oaklandsw.http.HttpURLConnection.getDefaultIdleConnectionTimeout());
        assertEquals(0,
            com.oaklandsw.http.HttpURLConnection.getDefaultIdleConnectionPing());
    }
}
