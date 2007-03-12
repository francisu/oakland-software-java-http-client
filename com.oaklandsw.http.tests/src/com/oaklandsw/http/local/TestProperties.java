package com.oaklandsw.http.local;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.oaklandsw.http.HttpTestEnv;

public class TestProperties extends TestCase
{

    static final String proxyHost     = "testproxyhost";

    static final int    proxyPort     = 123;

    static final String nonProxyHosts = "test1|.*.test2.com|123.345.222.333|.*.com";

    public TestProperties(String testName)
    {
        super(testName);
    }

    public static void main(String args[])
    {
        String[] testCaseName = { TestProperties.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }

    //
    // This test must run before any other since the properties
    // are inspected only in the static initializer of HttpURLConnection
    // 
    public static Test suite()
    {
        return new TestSuite(TestProperties.class);
    }

    public static void setProperties()
    {
        // Set property values before TestEnv.setUp is called as
        // the properties are detected in the HttpURLConnection
        // static initializer

        System.setProperty("http.proxyHost", proxyHost);
        System.setProperty("http.proxyPort", Integer.toString(proxyPort));
        System.setProperty("http.nonProxyHosts", nonProxyHosts);

        // Note the only way we can test the functionality of 1767 is to
        // enable this statement below and run the tests, since the static
        // initializer is called only once. 
        
        // 1767 skipEnvironmentInit
        //System.setProperty("com.oaklandsw.http.skipEnvironmentInit", "true");

    }

    public void setUp()
    {
        setProperties();
        HttpTestEnv.setUp();
    }

    public void tearDown()
    {
        // Reset everything
        com.oaklandsw.http.HttpURLConnection.setProxyPort(0);
        com.oaklandsw.http.HttpURLConnection.setProxyHost(null);
        com.oaklandsw.http.HttpURLConnection.setNonProxyHosts(null);
    }

    public void testProps() throws Exception
    {
        if (System.getProperty("com.oaklandsw.http.skipEnvironmentInit") == null)
        {

            assertEquals(proxyHost, com.oaklandsw.http.HttpURLConnection
                    .getProxyHost());
            assertEquals(proxyPort, com.oaklandsw.http.HttpURLConnection
                    .getProxyPort());
            assertEquals(nonProxyHosts, com.oaklandsw.http.HttpURLConnection
                    .getNonProxyHosts());
        }
        else
        {
            assertEquals(null, com.oaklandsw.http.HttpURLConnection
                    .getProxyHost());
            assertEquals(-1, com.oaklandsw.http.HttpURLConnection.getProxyPort());
            assertEquals(null, com.oaklandsw.http.HttpURLConnection
                    .getNonProxyHosts());

        }

    }

    // 961 and 966
    public void testDefaultTimeoutValues() throws Exception
    {
        assertEquals(14000, com.oaklandsw.http.HttpURLConnection
                .getDefaultIdleConnectionTimeout());
        assertEquals(0, com.oaklandsw.http.HttpURLConnection
                .getDefaultIdleConnectionPing());
    }

}
