package com.oaklandsw.http.webapp;

import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.TestEnv;
import com.oaklandsw.http.servlet.HeaderServlet;
import com.oaklandsw.util.NetUtils;

public class TestHeaders extends TestWebappBase
{

    private static final Log _log = LogFactory.getLog(TestHeaders.class);

    public TestHeaders(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestHeaders.class);
        return suite;
    }

    public static void main(String args[])
    {
        mainRun(suite(), args);
    }

    public void testAddRequestHeader() throws Exception
    {
        URL url = new URL(_urlBase + HeaderServlet.NAME);
        int response = 0;

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("GET");
        // method.setRequestHeader(new
        // Header("addRequestHeader(Header)","True"));
        urlCon.setRequestProperty("addRequestHeader", "Also True");
        ;
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        // Tomcat 4 at least converts the header name to lower case
        // checkReply("name=\"addrequestheader(header)\";value=\"True\"<br>");
        checkReply(urlCon, "name=\"addrequestheader"
            + "\";value=\"Also True\"<br>");

        if (com.oaklandsw.http.HttpURLConnection.getExplicitClose())
            urlCon.getInputStream().close();
        checkNoActiveConns(url);
    }

    // public void testRemoveRequestHeader() throws Exception
    // {
    // HttpClient client = new HttpClient();
    // client.startSession(host, port);
    // GetMethod method = new GetMethod("/" + context + "/headers");
    // method.setRequestHeader(new Header("XXX-A-HEADER", "true"));
    // method.removeRequestHeader("XXX-A-HEADER");
    // method.setUseDisk(false);
    // try
    // {
    // client.executeMethod(method);
    // }
    // catch (Throwable t)
    // {
    // t.printStackTrace();
    // fail("Unable to execute method : " + t.toString());
    // }
    // // Tomcat 4 at least converts the header name to lower case
    // assertTrue(!(method.getResponseBodyAsString().indexOf("xxx-a-header") >=
    // 0));
    // }

    public void testOverwriteRequestHeader() throws Exception
    {
        URL url = new URL(_urlBase + HeaderServlet.NAME);
        int response = 0;

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("GET");
        urlCon.setRequestProperty("xxx-a-header", "one");
        urlCon.setRequestProperty("XXX-A-HEADER", "two");
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        checkReply(urlCon, "name=\"xxx-a-header\";value=\"two\"<br>");
        checkNoActiveConns(url);
    }

    public void testGetResponseHeader() throws Exception
    {
        URL url = new URL(_urlBase + HeaderServlet.NAME);
        int response = 0;

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("GET");
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);
        assertEquals("Yes", urlCon.getHeaderField("headersetbyservlet"));

        // Bug 1440 getHeaderFields() not implemented
        // Need to cast this since getHeaderFields() is not provided prior
        // to JDK14
        Map headerMap = ((com.oaklandsw.http.HttpURLConnection)urlCon)
                .getHeaderFields();
        List headerField = (List)headerMap.get("HeaderSetByServlet");
        assertEquals("Yes", headerField.get(0));

        if (com.oaklandsw.http.HttpURLConnection.getExplicitClose())
            urlCon.getInputStream().close();
        checkNoActiveConns(url);
    }

    public void testHostRequestHeaderIp() throws Exception
    {
        InetAddress addr = InetAddress.getByName(host);
        String ip = addr.getHostAddress();
        hostRequestHeader(ip);
    }

    public void testHostRequestHeaderName() throws Exception
    {
        InetAddress addr = InetAddress.getByName(host);
        String hostname = addr.getHostName();
        hostRequestHeader(hostname);
    }

    // Bug 1031 allow user-agent header to be set by user
    public void testUserAgentHeader() throws Exception
    {
        URL url = new URL(_urlBase + HeaderServlet.NAME);
        int response = 0;

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestProperty("User-Agent", "TestUserAgent");
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        // Tomcat 4 at least converts the header name to lower case
        // checkReply("name=\"addrequestheader(header)\";value=\"True\"<br>");
        checkReply(urlCon, "name=\"user-agent"
            + "\";value=\"TestUserAgent\"<br>");

        if (com.oaklandsw.http.HttpURLConnection.getExplicitClose())
            urlCon.getInputStream().close();
        checkNoActiveConns(url);
    }

    // Make sure normal user-agent header is correct
    public void testNormalUserAgentHeader() throws Exception
    {
        URL url = new URL(_urlBase + HeaderServlet.NAME);
        int response = 0;

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        // Tomcat 4 at least converts the header name to lower case
        // checkReply("name=\"addrequestheader(header)\";value=\"True\"<br>");
        checkReply(urlCon, "name=\"user-agent"
            + "\";value=\""
            + com.oaklandsw.http.HttpURLConnection.DEFAULT_USER_AGENT
            + "\"<br>");

        if (com.oaklandsw.http.HttpURLConnection.getExplicitClose())
            urlCon.getInputStream().close();
        checkNoActiveConns(url);
    }

    public void hostRequestHeader(String connectAddr) throws Exception
    {

        URL url = new URL("http://"
            + connectAddr
            + ":"
            + port
            + TestEnv.TEST_URL_APP
            + HeaderServlet.NAME);
        int response = 0;

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("GET");
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        _log.debug(NetUtils.dumpHeaders(urlCon));

        if (port == 80)
        {
            checkReply(urlCon, "name=\"host\";value=\""
                + connectAddr
                + "\"<br>");
        }
        else
        {
            checkReply(urlCon, "name=\"host\";value=\""
                + connectAddr
                + ":"
                + port
                + "\"<br>");
        }
        if (com.oaklandsw.http.HttpURLConnection.getExplicitClose())
            urlCon.getInputStream().close();
        checkNoActiveConns(url);
    }

    public void allTestMethods() throws Exception
    {
        testAddRequestHeader();
        testOverwriteRequestHeader();
        testGetResponseHeader();
        testUserAgentHeader();
        testHostRequestHeaderIp();
        testHostRequestHeaderName();
    }

}
