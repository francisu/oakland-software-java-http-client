package com.oaklandsw.http.webapp;

import java.net.URL;

import org.apache.commons.logging.Log;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.HttpConnection;
import com.oaklandsw.http.HttpConnectionManager;
import com.oaklandsw.http.HttpURLConnection;
import com.oaklandsw.http.servlet.ParamServlet;
import com.oaklandsw.http.servlet.RequestBodyServlet;
import com.oaklandsw.util.LogUtils;

public class TestExplicitConnection extends TestWebappBase
{

    private static final Log   _log         = LogUtils.makeLogger();

    protected HttpConnectionManager _connManager;
    
    public TestExplicitConnection(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestExplicitConnection.class);
        return suite;
    }

    public static void main(String args[])
    {
        mainRun(suite(), args);
    }

    public void setUp() throws Exception
    {
        super.setUp();
        _connManager = HttpURLConnection.getConnectionManager();
    }

    public void testConnect() throws Exception
    {
        HttpConnection conn = _connManager.getConnection(_urlBase);

        URL url = new URL(_urlBase + RequestBodyServlet.NAME);

        assertEquals(1, getActiveConns(url));
        assertEquals(1, getTotalConns(url));

        // Once
        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setConnection(conn);
        urlCon.getResponseCode();

        assertEquals(1, getActiveConns(url));
        assertEquals(1, getTotalConns(url));

        // Twice
        urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setConnection(conn);
        urlCon.getResponseCode();

        assertEquals(1, getActiveConns(url));
        assertEquals(1, getTotalConns(url));

        // Disconnect
        urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setConnection(conn);
        urlCon.disconnect();

        checkNoActiveConns(url);
    }

    public void testConnect2Conns() throws Exception
    {
        HttpConnection conn = _connManager.getConnection(_urlBase);
        HttpConnection conn2 = _connManager.getConnection(_urlBase);

        URL url = new URL(_urlBase + ParamServlet.NAME);

        assertEquals(2, getActiveConns(url));
        assertEquals(2, getTotalConns(url));

        // Once
        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        HttpURLConnection urlCon2 = (HttpURLConnection)url.openConnection();
        urlCon = (HttpURLConnection)url.openConnection();
        urlCon2 = (HttpURLConnection)url.openConnection();
        urlCon.setConnection(conn);
        urlCon2.setConnection(conn2);
        checkReply(urlCon, paramReply("GET"));
        checkReply(urlCon2, paramReply("GET"));

        assertEquals(2, getActiveConns(url));
        assertEquals(2, getTotalConns(url));

        // Disconnect
        urlCon = (HttpURLConnection)url.openConnection();
        urlCon2 = (HttpURLConnection)url.openConnection();
        urlCon.setConnection(conn);
        urlCon2.setConnection(conn2);
        urlCon.disconnect();
        urlCon2.disconnect();

        checkNoActiveConns(url);
    }

    public void testConnectBadState() throws Exception
    {
        HttpConnection conn = _connManager.getConnection(_urlBase);

        URL url = new URL(_urlBase + RequestBodyServlet.NAME);

        // Once
        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.connect();
        try
        {
            urlCon.setConnection(conn);
            fail("Missed expected exception");
        }
        catch (IllegalStateException ex)
        {
            // expected
        }

        urlCon.disconnect();

        assertEquals(1, getActiveConns(url));
        assertEquals(1, getTotalConns(url));

        // Disconnect
        urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setConnection(conn);
        urlCon.disconnect();

        checkNoActiveConns(url);
    }

}
