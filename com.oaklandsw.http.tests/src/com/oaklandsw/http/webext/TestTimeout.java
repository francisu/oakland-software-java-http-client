package com.oaklandsw.http.webext;

import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.logging.Log;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.HttpTimeoutException;
import com.oaklandsw.http.TestBase;
import com.oaklandsw.http.TestEnv;
import com.oaklandsw.util.LogUtils;

public class TestTimeout extends TestBase
{

    private static final Log _log = LogUtils.makeLogger();

    static
    {
        TestEnv.setUp();
    }

    public TestTimeout(String testName)
    {
        super(testName);
        _doUseDnsJava = true;
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestTimeout.class);
        return suite;
    }

    public static void main(String args[])
    {
        mainRun(suite(), args);
    }

    public void testConnectTimeout(int type) throws Exception
    {
        // Make it really small - so we timeout while waiting
        // for the name resolution
        setupDefaultTimeout(type, 1);

        URL url;
        url = new URL("http://www.bbc.co.uk");
        // url = new URL("http://xxx.nepad.org");

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();

        // Only valid for nogoop implementation
        if (!(urlCon instanceof com.oaklandsw.http.HttpURLConnection))
            return;

        setupConnTimeout((com.oaklandsw.http.HttpURLConnection)urlCon, type, 1);

        urlCon.setRequestMethod("GET");

        try
        {
            urlCon.connect();
            fail("Should have timed out on the connect");
        }
        catch (HttpTimeoutException ex)
        {
            // Got expected exception
        }
        com.oaklandsw.http.HttpURLConnection.closeAllPooledConnections();

    }

    public void testConnectTimeoutDef() throws Exception
    {
        testConnectTimeout(DEF);
    }

    // This does not actually work since the connection does not timeout
    // in time
    // public void testConnectTimeoutDefConnect() throws Exception
    // {
    // testConnectTimeout(DEF_CONNECT);
    // }

    public void testConnectTimeoutConn() throws Exception
    {
        testConnectTimeout(CONN);
    }

    // This test does not work since the connection does not timeout
    // public void testConnectTimeoutConnConnect() throws Exception
    // {
    // testConnectTimeout(CONN_CONNECT);
    // }

    public void allTestMethods() throws Exception
    {
        testConnectTimeoutDef();
        // testConnectTimeoutDefConnect();
        testConnectTimeoutConn();
        // testConnectTimeoutConnConnect();
    }

}
