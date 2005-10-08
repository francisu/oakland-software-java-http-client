package com.oaklandsw.http.webext;

import java.net.HttpURLConnection;
import java.net.URL;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.HttpTimeoutException;
import com.oaklandsw.http.TestBase;
import com.oaklandsw.http.TestEnv;
import com.oaklandsw.log.Log;
import com.oaklandsw.log.LogFactory;

public class TestTimeout extends TestBase
{

    private static final Log _log = LogFactory.getLog(TestTimeout.class);

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

    public void tearDown() throws Exception
    {
        com.oaklandsw.http.HttpURLConnection.setDefaultTimeout(0);
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

    public void testConnectTimeoutDefConnect() throws Exception
    {
        testConnectTimeout(DEF_CONNECT);
    }

    public void testConnectTimeoutConn() throws Exception
    {
        testConnectTimeout(CONN);
    }

    public void testConnectTimeoutConnConnect() throws Exception
    {
        testConnectTimeout(CONN_CONNECT);
    }

    public void allTestMethods() throws Exception
    {
        testConnectTimeoutDef();
        testConnectTimeoutDefConnect();
        testConnectTimeoutConn();
        testConnectTimeoutConnConnect();
    }

}
