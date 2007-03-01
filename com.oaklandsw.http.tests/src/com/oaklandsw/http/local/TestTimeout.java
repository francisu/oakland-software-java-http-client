package com.oaklandsw.http.local;

import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.logging.Log;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.HttpConnection;
import com.oaklandsw.http.HttpTimeoutException;
import com.oaklandsw.http.TestBase;
import com.oaklandsw.http.TestEnv;
import com.oaklandsw.util.LogUtils;

public class TestTimeout extends TestBase
{

    private static final Log _log = LogUtils.makeLogger();

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

    public void tearDown() throws Exception
    {
        com.oaklandsw.http.HttpURLConnection.setDefaultTimeout(0);
        com.oaklandsw.http.HttpURLConnection.setExplicitClose(false);
        HttpConnection._testTimeout = 0;
    }

    // Default can be anything, since it won't connect
    private String _timeoutURL  = "http://localhost:80";

    private int    _testWaitVal = 2000;

    private int    _timeoutVal  = 100;

    private int    _timeoutSlop = 100;

    public void testConnectTimeout(int type) throws Exception
    {
        setupDefaultTimeout(type, _timeoutVal);

        // Waits this long when establishing a connection
        HttpConnection._testTimeout = _testWaitVal;

        // Does not really matter
        URL url = new URL(_timeoutURL);

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();

        // Only valid for oaklandsw implementation
        if (!(urlCon instanceof com.oaklandsw.http.HttpURLConnection))
            return;

        // Simulated timeout stuff does not work on 1.4 or higher
        if (!TestEnv.SIMULATED_TIMEOUT_ENABLED)
            return;

        setupConnTimeout((com.oaklandsw.http.HttpURLConnection)urlCon,
                         type,
                         _timeoutVal);

        long startTime = System.currentTimeMillis();

        try
        {
            urlCon.setRequestMethod("GET");
            urlCon.connect();
            fail("Should have timed out on the connect");
        }
        catch (HttpTimeoutException ex)
        {
            long interval = System.currentTimeMillis() - startTime;

            if (interval > _timeoutVal + _timeoutSlop)
            {
                fail("Timeout took too long: " + interval);
            }

            if (interval < _timeoutVal)
            {
                fail("Timeout was too short: " + interval);
            }
            // Expected
        }
        catch (Exception ex1)
        {
            fail("Should have gotten timeout exception, got: " + ex1);
        }

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

    private int timeoutTestMethod(int type, int num, int numTimeouts)
        throws Exception
    {
        URL url = new URL(_timeoutURL);
        long startTime = System.currentTimeMillis();

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();

        setupConnTimeout((com.oaklandsw.http.HttpURLConnection)urlCon,
                         type,
                         _timeoutVal);

        try
        {
            urlCon.setRequestMethod("GET");
            urlCon.connect();
            fail("Should have timed out on the connect");
        }
        catch (HttpTimeoutException ex)
        {
            long interval = System.currentTimeMillis() - startTime;

            // System.out.println("iter " + num + " interval: " + interval);

            // We allow a few to timeout to allow for GC
            if (numTimeouts > 3 && interval > _timeoutVal + _timeoutSlop)
            {
                fail("Timeout took too long - iteration: "
                    + num
                    + " interval "
                    + interval);
            }
            // Expected
        }
        catch (Exception ex1)
        {
            fail("Should have gotten timeout exception, got: " + ex1);
        }
        return numTimeouts;
    }

    public void testRepeatConnectTimeout(int type) throws Exception
    {
        setupDefaultTimeout(type, _timeoutVal);

        URL url = new URL(_timeoutURL);
        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        // Only valid for nogoop implementation
        if (!(urlCon instanceof com.oaklandsw.http.HttpURLConnection))
            return;

        // Simulated timeout stuff does not work on 1.4 or higher
        if (!TestEnv.SIMULATED_TIMEOUT_ENABLED)
            return;

        // Waits this long when establishing a connection
        HttpConnection._testTimeout = _testWaitVal;

        int numTimeouts = 0;
        for (int i = 0; i < 50; i++)
            numTimeouts = timeoutTestMethod(type, i, numTimeouts);

        // Should have no more connections outstanding
        checkNoActiveConns(url);

    }

    public void testRepeatConnectTimeoutDef() throws Exception
    {
        testRepeatConnectTimeout(DEF);
    }

    public void testRepeatConnectTimeoutDefConnect() throws Exception
    {
        testRepeatConnectTimeout(DEF_CONNECT);
    }

    public void testRepeatConnectTimeoutConn() throws Exception
    {
        testRepeatConnectTimeout(CONN);
    }

    public void testRepeatConnectTimeoutConnConnect() throws Exception
    {
        testRepeatConnectTimeout(CONN_CONNECT);
    }

}
