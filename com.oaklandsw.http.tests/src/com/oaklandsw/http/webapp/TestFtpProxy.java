package com.oaklandsw.http.webapp;

import java.net.URL;

import org.apache.commons.logging.Log;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.HttpTestEnv;
import com.oaklandsw.http.HttpURLConnection;
import com.oaklandsw.http.TestUserAgent;
import com.oaklandsw.util.LogUtils;
import com.oaklandsw.util.Util;

public class TestFtpProxy extends TestWebappBase
{

    private static final Log _log = LogUtils.makeLogger();

    public TestFtpProxy(String testName)
    {
        super(testName);

        _doAuthCloseProxyTest = false;
        _doExplicitTest = false;
        _doAppletTest = false;

        _doIsaProxyTest = true;
        _do10ProxyTest = true;
    }

    protected boolean doTest()
    {
        return _inAuthProxyTest || _inProxyTest || _in10ProxyTest || _inIsaProxyTest;
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestFtpProxy.class);
        return suite;
    }

    public static void main(String args[])
    {
        mainRun(suite(), args);
    }

    public String testFtp(String suffix) throws Exception
    {
        URL url = new URL("ftp://" + HttpTestEnv.FTP_HOST + suffix);
        int response = 0;

        TestUserAgent._type = TestUserAgent.OAKLANDSWTEST_DOMAIN;

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");
        response = urlCon.getResponseCode();
        assertEquals(200, response);
        return Util.getStringFromInputStream(urlCon.getInputStream());
    }

    public void testFtpFile() throws Exception
    {
        if (!doTest())
            return;

        String res = testFtp("/HttpRequestSample.java");

        // System.out.println(res);
        assertTrue(res.length() > 1000);
        assertTrue(res.contains("public class HttpRequestSample"));
    }

    public void testFtpDir() throws Exception
    {
        if (!doTest())
            return;

        String res = testFtp("");

        // System.out.println(res);
        assertTrue(res.length() > 100);
        assertTrue(res.contains("HttpRequestSample.java"));
    }

    public void allTestMethods() throws Exception
    {
        testFtpFile();
        testFtpDir();
    }

}
