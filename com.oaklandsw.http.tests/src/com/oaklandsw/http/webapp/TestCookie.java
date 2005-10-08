package com.oaklandsw.http.webapp;

import java.net.HttpURLConnection;
import java.net.URL;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.servlet.WriteCookieServlet;
import com.oaklandsw.log.Log;
import com.oaklandsw.log.LogFactory;

public class TestCookie extends TestWebappBase
{

    private static final Log _log = LogFactory.getLog(TestCookie.class);

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

    public void testSetCookie(String method) throws Exception
    {
        URL url = new URL(_urlBase + WriteCookieServlet.NAME + "?simple=set");
        int response = 0;

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod(method);
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        String cookie = urlCon.getHeaderField("set-cookie2");
        if (cookie == null)
            cookie = urlCon.getHeaderField("set-cookie");
        assertTrue(cookie.indexOf("simplecookie=value;") >= 0);
        assertTrue(cookie.indexOf("Version=1") >= 0);

        String reply = getReply(urlCon);
        assertTrue(checkReplyNoAssert(reply, "<title>WriteCookieServlet: "
            + method
            + "</title>"));
        assertTrue(checkReplyNoAssert(reply, "Wrote simplecookie.<br>"));
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
        testSetCookie("PUT");
    }

    public void testSetMultiCookie(String method) throws Exception
    {
        URL url = new URL(_urlBase
            + WriteCookieServlet.NAME
            + "?simple=set&domain=set");
        int response = 0;

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod(method);
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        String cookieField;
        // FOR Tomcat 3.2 - this is no longer tested
        //if (urlCon.getHeaderField(0).indexOf("HTTP/1.0") >= 0)
        //    cookieField = "set-cookie2";
        //else
            cookieField = "set-cookie";

        int numFound = 0;
        for (int i = 1; true; i++)
        {
            String fieldName = urlCon.getHeaderFieldKey(i);
            if (fieldName == null)
                break;
            if (fieldName.equalsIgnoreCase(cookieField))
            {
                String cookieVal = urlCon.getHeaderField(i);
                _log.debug("cookie: " + cookieVal);
                if (cookieVal.startsWith("simplecookie=value"))
                    numFound++;
                if (cookieVal.startsWith("domaincookie=value"))
                    numFound++;
            }
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
        testSetMultiCookie("PUT");
    }

    public void allTestMethods() throws Exception
    {
        testSetCookieGet();
        testSetCookiePost();
        testSetCookiePut();

        testSetMultiCookieGet();
        testSetMultiCookiePost();
        testSetMultiCookiePut();
    }

}
