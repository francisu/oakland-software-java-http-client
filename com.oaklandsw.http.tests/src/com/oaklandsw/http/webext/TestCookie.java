package com.oaklandsw.http.webext;

import com.oaklandsw.http.CookieContainer;
import com.oaklandsw.http.HttpTestBase;
import com.oaklandsw.http.HttpURLConnection;

import com.oaklandsw.util.Log;
import com.oaklandsw.util.LogUtils;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.net.URL;


public class TestCookie extends HttpTestBase {
    private static final Log _log = LogUtils.makeLogger();

    public TestCookie(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(TestCookie.class);

        return suite;
    }

    public static void main(String[] args) {
        mainRun(suite(), args);
    }

    public void testSetCookie() throws Exception {
        URL url = new URL("http://google.com");
        int response = 0;

        CookieContainer cc = new CookieContainer();

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");
        urlCon.setCookieSupport(cc, null);
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        // We get a bunch of cookies from this site
        assertTrue(cc.getCookies().length > 0);
        checkNoActiveConns(url);
    }

    public void allTestMethods() throws Exception {
        testSetCookie();
    }
}
