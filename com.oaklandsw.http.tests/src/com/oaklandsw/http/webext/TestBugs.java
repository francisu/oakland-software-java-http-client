// Copyright 2007, Oakland Software Incorporated

package com.oaklandsw.http.webext;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.HttpTestBase;
import com.oaklandsw.http.TestUserAgent;

public class TestBugs extends HttpTestBase
{
    HttpURLConnection _urlCon;

    public TestBugs(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        return new TestSuite(TestBugs.class);
    }

    public static void main(String args[])
    {
        mainRun(suite(), args);
    }

    public void tearDown() throws Exception
    {
        super.tearDown();
    }

    // Connect to public test sharepoint server
    public void testBug1816()
        throws MalformedURLException,
            IOException
    {
        TestUserAgent._type = TestUserAgent.OFFICESHARE_ICEWEB;
        // URL url = new
        // URL("http://www.xsolive.com/_layouts/Authenticate.aspx?Source=%2Fdefault%2Easpx");
        URL url = new URL("http://sharepoint.iceweb.com/sites/demo/default.aspx");
        int response = 0;

        _urlCon = (HttpURLConnection)url.openConnection();
        _urlCon.connect();
        response = _urlCon.getResponseCode();
        assertEquals(200, response);

        getReply(_urlCon);
    }

}
