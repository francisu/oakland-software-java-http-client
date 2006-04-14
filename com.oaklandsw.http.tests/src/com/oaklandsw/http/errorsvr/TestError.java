package com.oaklandsw.http.errorsvr;

import java.net.HttpURLConnection;
import java.net.URL;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.HttpException;
import com.oaklandsw.http.TestBase;
import com.oaklandsw.http.TestEnv;
import com.oaklandsw.log.Log;
import com.oaklandsw.log.LogFactory;

public class TestError extends TestBase
{

    private static final Log _log      = LogFactory.getLog(TestError.class);

    protected static String  _errorUrl = TestEnv.TEST_URL_HOST_ERROR;

    public TestError(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestError.class);
        return suite;
    }

    public static void main(String args[])
    {
        mainRun(suite(), args);
    }

    public void testBadCL() throws Exception
    {
        URL url = new URL(_errorUrl
            + "?error=none"
            + "&badContentLength=true"
            + _errorDebug);

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("GET");
        urlCon.connect();

        try
        {
            urlCon.getResponseCode();
            fail("Did not get expected exception");
        }
        catch (HttpException ex)
        {
            // Expected exception about bad content length
            assertTrue(ex.getMessage().indexOf("Content-Length") > 0);
        }
        checkNoActiveConns(url);
    }

    public void allTestMethods() throws Exception
    {
        testBadCL();
    }

}
