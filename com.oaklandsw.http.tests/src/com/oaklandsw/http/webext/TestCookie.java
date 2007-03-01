package com.oaklandsw.http.webext;

import java.net.URL;

import org.apache.commons.logging.Log;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.CookieContainer;
import com.oaklandsw.http.HttpURLConnection;
import com.oaklandsw.http.TestBase;
import com.oaklandsw.util.LogUtils;

public class TestCookie extends TestBase
{

    private static final Log   _log         = LogUtils.makeLogger();

    public TestCookie(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestCookie.class);
        return suite;
    }

    public static void main(String args[])
    {
        mainRun(suite(), args);
    }

    public void testSetCookie() throws Exception
    {
        URL url = new URL("http://my.yahoo.com");
        int response = 0;

        CookieContainer cc = new CookieContainer();

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("GET");
        urlCon.setCookieSupport(cc, null);
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        // We get a bunch of cookies from this site
        assertTrue(cc.getCookies().length > 0);
        checkNoActiveConns(url);
    }

    public void allTestMethods() throws Exception
    {
        testSetCookie();
    }

}
