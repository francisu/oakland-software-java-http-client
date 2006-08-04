package com.oaklandsw.http.webapp;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.servlet.CallMyselfServlet;

public class TestCallMyself extends TestWebappBase
{

    private static final Log _log = LogFactory.getLog(TestCallMyself.class);

    public TestCallMyself(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestCallMyself.class);
        return suite;
    }

    public static void main(String args[])
    {
        mainRun(suite(), args);
    }

    public void doCallMyself(String firstMethod, String innerMethod)
        throws Exception
    {
        URL url = new URL(_urlBase
            + CallMyselfServlet.NAME
            + "?call="
            + CallMyselfServlet.NAME
            + "&method="
            + innerMethod);
        int response = 0;

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod(firstMethod);

        if (firstMethod.equals("POST"))
        {
            urlCon.setDoOutput(true);
            OutputStream os = urlCon.getOutputStream();
            os.write("param-one=param-value".getBytes("ASCII"));
        }

        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        String reply = getReply(urlCon);
        assertTrue(checkReplyNoAssert(reply, "<title>Param Servlet: "
            + innerMethod
            + "</title>"));
        assertTrue(checkReplyNoAssert(reply, "<title>CallMyself Servlet: "
            + firstMethod
            + "</title>"));

        if (firstMethod.equals("POST"))
        {
            assertTrue(checkReplyNoAssert(reply,
                                          "<p>Request Body</p>\r\nparam-one=param-value"));
        }
        if (innerMethod.equals("POST"))
        {
            assertTrue(checkReplyNoAssert(reply, "<p>Request Body</p>\r\n"
                + "servlet-param-one=servlet-param-value"));
        }

    }

    public void testCallMyselfGG() throws Exception
    {
        doCallMyself("GET", "GET");
    }

    public void testCallMyselfPG() throws Exception
    {
        doCallMyself("POST", "GET");
    }

    public void testCallMyselfGP() throws Exception
    {
        doCallMyself("GET", "POST");
    }

    public void testCallMyselfPP() throws Exception
    {
        doCallMyself("POST", "POST");
    }

    public void allTestMethods() throws Exception
    {
        testCallMyselfGG();
        testCallMyselfPG();
        testCallMyselfGP();
        testCallMyselfPP();
    }

}
