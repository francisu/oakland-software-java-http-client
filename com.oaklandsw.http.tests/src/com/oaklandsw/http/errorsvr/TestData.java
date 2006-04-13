package com.oaklandsw.http.errorsvr;

import java.net.HttpURLConnection;
import java.net.URL;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.TestBase;
import com.oaklandsw.http.TestEnv;
import com.oaklandsw.log.Log;
import com.oaklandsw.log.LogFactory;

public class TestData extends TestBase
{

    private static final Log _log      = LogFactory.getLog(TestData.class);

    protected static String  _errorUrl = TestEnv.TEST_URL_HOST_ERROR;

    public TestData(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestData.class);
        return suite;
    }

    public static void main(String args[])
    {
        mainRun(suite(), args);
    }

    public void setUp()
    {
        TestEnv.setUp();
    }

    public void tearDown()
    {
        com.oaklandsw.http.HttpURLConnection.setExplicitClose(false);
    }

    public HttpURLConnection testDataCloseBase(String serverArgs) throws Exception
    {
        URL url = new URL(_errorUrl + serverArgs + _errorDebug);

        com.oaklandsw.http.HttpURLConnection urlCon = (com.oaklandsw.http.HttpURLConnection)url
                .openConnection();
        urlCon.setRequestMethod("GET");
        urlCon.connect();

        // Should work
        urlCon.getResponseCode();
        assertEquals(200, urlCon.getResponseCode());

        checkErrorSvrData(urlCon, false);
        checkNoActiveConns(url);
        return urlCon;
    }

    public void testDataCloseNoCL() throws Exception
    {
        testDataCloseBase("?error=none"
            + "&close=true"
            + "&noContentLength=true");
    }

    public void testDataNoCL() throws Exception
    {
        testDataCloseBase("?error=none" + "&noContentLength=true");
    }

    public void testData10CloseNoCL() throws Exception
    {
        testDataCloseBase("?error=none"
            + "&version=1.0"
            + "&close=true"
            + "&noContentLength=true");
    }

    public void testData10NoCL() throws Exception
    {
        testDataCloseBase("?error=none"
            + "&version=1.0"
            + "&noContentLength=true");
    }

    // Bug 1438 - explicit close can hang
    public void testData10KeepAliveNoCL() throws Exception
    {
        // Use explicit close in this case because the connection will
        // be kept alive and can be continuously be read from
        String serverArgs = "?error=none"
            + "&version=1.0"
            + "&keepAlive=true"
            + "&noContentLength=true";

        URL url = new URL(_errorUrl + serverArgs + _errorDebug);

        com.oaklandsw.http.HttpURLConnection urlCon = (com.oaklandsw.http.HttpURLConnection)url
                .openConnection();
        com.oaklandsw.http.HttpURLConnection.setExplicitClose(true);
        urlCon.setRequestMethod("GET");
        urlCon.connect();

        // Should work
        urlCon.getResponseCode();
        assertEquals(200, urlCon.getResponseCode());

        urlCon.getInputStream().close();

        assertEquals(1, getTotalConns(url));
        urlCon.disconnect();
        checkNoActiveConns(url);
        checkNoTotalConns(url);
    }

    public void testData10KeepAliveCL() throws Exception
    {
        HttpURLConnection urlCon = testDataCloseBase("?error=none" + "&version=1.0" + "&keepAlive=true");
        urlCon.disconnect();
        checkNoTotalConns(urlCon.getURL());
    }

    public void testSpaceCL() throws Exception
    {
        URL url = new URL(_errorUrl
            + "?error=none"
            + "&spaceContentLength=true"
            + _errorDebug);

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("GET");
        urlCon.connect();
        assertEquals(200, urlCon.getResponseCode());
        checkNoActiveConns(url);
    }

     public void allTestMethods() throws Exception
    {
        testDataCloseNoCL();
        testDataNoCL();
        testData10CloseNoCL();
        testData10NoCL();
        testData10KeepAliveNoCL();
        testSpaceCL();
    }

}
