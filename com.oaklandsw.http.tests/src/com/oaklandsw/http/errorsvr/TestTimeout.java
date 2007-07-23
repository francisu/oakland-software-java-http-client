package com.oaklandsw.http.errorsvr;

import java.io.InputStream;
import java.net.URL;

import org.apache.commons.logging.Log;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.HttpTimeoutException;
import com.oaklandsw.http.HttpTestBase;
import com.oaklandsw.http.HttpTestEnv;
import com.oaklandsw.http.HttpURLConnection;
import com.oaklandsw.util.LogUtils;

public class TestTimeout extends HttpTestBase
{

    private static final Log   _log         = LogUtils.makeLogger();

    public TestTimeout(String testName)
    {
        super(testName);
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

    // Test read timeout
    public void testReadTimeout(int type) throws Exception
    {
        // 1 second timeout
        setupDefaultTimeout(type, 1000);

        // Delay the content for 5 seconds
        URL url = new URL(HttpTestEnv.TEST_URL_HOST_ERRORSVR
            + "?error=timeout"
            + "&when=before-content"
            + "&sec=5"
            + _errorDebug);

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        setupConnTimeout(urlCon, type, 1000);

        urlCon.setRequestMethod("GET");
        urlCon.connect();

        // Should work
        urlCon.getResponseCode();

        InputStream is = urlCon.getInputStream();

        try
        {
            is.read();
            if (type != CONN_CONNECT && type != DEF_CONNECT)
                fail("Should have timed out on the read");
            else
                is.close();
        }
        catch (HttpTimeoutException ex)
        {
            if (type == CONN_CONNECT || type == DEF_CONNECT)
                fail("These timeouts should have no effect");
            // Expected
        }

        checkNoActiveConns(url);
    }

    public void testReadTimeoutDef() throws Exception
    {
        testReadTimeout(DEF);
    }

    public void testReadTimeoutDefConnect() throws Exception
    {
        testReadTimeout(DEF_CONNECT);
    }

    public void testReadTimeoutDefRequest() throws Exception
    {
        testReadTimeout(DEF_REQUEST);
    }

    public void testReadTimeoutConn() throws Exception
    {
        testReadTimeout(CONN);
    }

    public void testReadTimeoutConnConnect() throws Exception
    {
        testReadTimeout(CONN_CONNECT);
    }

    public void testReadTimeoutConnRequest() throws Exception
    {
        testReadTimeout(CONN_REQUEST);
    }

    public void allTestMethods() throws Exception
    {
        testReadTimeoutDef();
        testReadTimeoutDefConnect();
        testReadTimeoutDefRequest();
        testReadTimeoutConn();
        testReadTimeoutConnConnect();
        testReadTimeoutConnRequest();
    }

}
