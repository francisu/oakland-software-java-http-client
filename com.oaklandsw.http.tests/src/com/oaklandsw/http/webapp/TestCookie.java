package com.oaklandsw.http.webapp;

import java.net.URL;

import com.oaklandsw.util.Log;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.Cookie;
import com.oaklandsw.http.CookieContainer;
import com.oaklandsw.http.HttpURLConnection;
import com.oaklandsw.http.servlet.ReadCookieServlet;
import com.oaklandsw.http.servlet.WriteCookieServlet;
import com.oaklandsw.util.LogUtils;

public class TestCookie extends TestWebappBase
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

    public void testSetCookieTooLate() throws Exception
    {
        URL url = new URL(_urlBase + WriteCookieServlet.NAME + "?simple=set");

        CookieContainer cc = new CookieContainer();

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.connect();
        try
        {
            urlCon.setCookieSupport(cc, null);
            fail("Expected exception");
        }
        catch (Exception ex)
        {
            assertTrue(ex.getMessage().indexOf("has been established") > -1);
        }
        getReply(urlCon);
    }

    public void testSetCookie(String method) throws Exception
    {
        URL url = new URL(_urlBase + WriteCookieServlet.NAME + "?simple=set");
        int response = 0;

        CookieContainer cc = new CookieContainer();

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod(method);
        urlCon.setCookieSupport(cc, null);
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        assertEquals(1, cc.getCookies().length);
        Cookie c = cc.getCookies()[0];
        assertEquals("simplecookie", c.getName());
        assertEquals("value", c.getValue());
        // Update value for when cookie is given back to servlet
        c.setValue(c.getValue() + "updated");
        assertEquals(1, c.getVersion());
        getReply(urlCon);

        // Now have the cookie read
        url = new URL(_urlBase + ReadCookieServlet.NAME);

        urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod(method);
        urlCon.setCookieSupport(cc, null);
        response = urlCon.getResponseCode();
        assertEquals(200, response);
        String reply = getReply(urlCon);
        assertTrue(checkReplyNoAssert(reply, "<title>ReadCookieServlet: "
            + method
            + "</title>"));
        assertTrue(checkReplyNoAssert(reply, "simplecookie=\"valueupdated\""));
        
        // Now have the cookie removed
        url = new URL(_urlBase + WriteCookieServlet.NAME + "?simple=unset");

        urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod(method);
        urlCon.setCookieSupport(cc, null);
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        assertEquals(0, cc.getCookies().length);
        getReply(urlCon);
        checkNoActiveConns(url);
    }

    public void testSetCookieGet() throws Exception
    {
        testSetCookie("GET");
    }

    public void testSetCookiePost() throws Exception
    {
        testSetCookie("POST");
    }

    public void testSetCookiePut() throws Exception
    {
        if (_inAuthCloseProxyTest)
            return;

        testSetCookie("PUT");
    }

    public void testSetMultiCookie(String method) throws Exception
    {
        URL url = new URL(_urlBase
            + WriteCookieServlet.NAME
            + "?simple=set&domain=set");
        int response = 0;

        CookieContainer cc = new CookieContainer();

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod(method);
        urlCon.setCookieSupport(cc, null);
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        int numFound = 0;
        for (int i = 0; i < cc.getCookies().length; i++)
        {
            Cookie cookie = cc.getCookies()[i];
            // System.out.println(cookie);
            if (cookie.getName().equals("simplecookie"))
                numFound++;
            if (cookie.getName().equals("domaincookie"))
                numFound++;
        }

        assertEquals(2, numFound);

        String reply = getReply(urlCon);
        assertTrue(checkReplyNoAssert(reply, "<title>WriteCookieServlet: "
            + method
            + "</title>"));
        assertTrue(checkReplyNoAssert(reply, "Wrote simplecookie.<br>"));
        assertTrue(checkReplyNoAssert(reply, "Wrote domaincookie.<br>"));
        checkNoActiveConns(url);
    }

    public void testSetMultiCookieGet() throws Exception
    {
        testSetMultiCookie("GET");
    }

    public void testSetMultiCookiePost() throws Exception
    {
        testSetMultiCookie("POST");
    }

    public void testSetMultiCookiePut() throws Exception
    {
        if (_inAuthCloseProxyTest)
            return;
        testSetMultiCookie("PUT");
    }

    public void testSetExpiredCookie(String method) throws Exception
    {
        URL url = new URL(_urlBase
            + WriteCookieServlet.NAME
            + "?simple=set&expire=1");
        int response = 0;

        CookieContainer cc = new CookieContainer();

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod(method);
        urlCon.setCookieSupport(cc, null);
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);
        getReply(urlCon);

        assertEquals(1, cc.getCookies().length);

        Thread.sleep(1100);
        cc.purgeExpiredCookies();

        assertEquals(0, cc.getCookies().length);
    }

    public void testSetExpiredCookieGet() throws Exception
    {
        testSetExpiredCookie("GET");
    }

    public void testSetExpiredCookiePost() throws Exception
    {
        testSetExpiredCookie("POST");
    }

    public void testSetExpiredCookiePut() throws Exception
    {
        if (_inAuthCloseProxyTest)
            return;
        testSetExpiredCookie("PUT");
    }

    public void allTestMethods() throws Exception
    {
        testSetCookieTooLate();
        testSetCookieGet();
        testSetCookiePost();
        testSetCookiePut();

        testSetMultiCookieGet();
        testSetMultiCookiePost();
        testSetMultiCookiePut();

        testSetExpiredCookieGet();
        testSetExpiredCookiePost();
        testSetExpiredCookiePut();
    }

}
