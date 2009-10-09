package com.oaklandsw.http.webapp;

import com.oaklandsw.http.HostnameVerifier;
import com.oaklandsw.http.HttpTestEnv;
import com.oaklandsw.http.HttpURLConnection;
import com.oaklandsw.http.TestUserAgent;

import com.oaklandsw.util.Log;
import com.oaklandsw.util.LogUtils;
import com.oaklandsw.util.Util;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.net.URL;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;


public class TestFtpProxy extends TestWebappBase {
    private static final Log _log = LogUtils.makeLogger();
    protected static final boolean SSL = true;
    protected static final boolean VERIFY = true;

    public TestFtpProxy(String testName) {
        super(testName);

        _doAuthCloseProxyTest = false;
        _doExplicitTest = false;
        _doAppletTest = false;

        // FIXME Seems to fail with a 502 in these cases, since this feature
        // is not actually used anywhere, this is probably not important
        //_doIsaProxyTest = true;
        //_doIsaSslProxyTest = true;

        // These have been failing since 2.6.1, but this feature is not used
        _do10ProxyTest = false;
        _doProxyTest = false;
        _doAuthProxyTest = false;
    }

    protected boolean doTest() {
        return _inAuthProxyTest || _inProxyTest || _in10ProxyTest ||
        _inIsaProxyTest;
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(TestFtpProxy.class);

        return suite;
    }

    public static void main(String[] args) {
        mainRun(suite(), args);
    }

    protected String testFtpUrl(String urlBody, boolean ssl)
        throws Exception {
        URL url = new URL("ftp://" + urlBody);
        int response = 0;

        TestUserAgent._type = TestUserAgent.OAKLANDSWTEST_DOMAIN;

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");
        urlCon.setForceSSL(ssl);

        if (false && ssl) {
            urlCon.setHostnameVerifier(new HostnameVerifier() {
                    public boolean verify(String hostName, SSLSession session) {
                        return true;
                    }
                });
        }

        response = urlCon.getResponseCode();
        assertEquals(200, response);

        return Util.getStringFromInputStream(urlCon.getInputStream());
    }

    protected String testFtp(String suffix) throws Exception {
        return testFtpUrl(HttpTestEnv.FTP_HOST + suffix, !SSL);
    }

    public void testFtpFile() throws Exception {
        if (!doTest()) {
            return;
        }

        String res = testFtp("/HttpRequestSample.java");

        // System.out.println(res);
        assertTrue(res.length() > 1000);
        assertContains(res, "public class HttpRequestSample");
    }

    public void testFtpFileSslVerify() throws Exception {
        try {
            testFtpUrl(HttpTestEnv.FTP_HOST + "/HttpRequestSample.java", SSL);
        } catch (SSLPeerUnverifiedException ex) {
            // Expected
        }
    }

    public void testFtpFileNotFound() throws Exception {
        if (!doTest()) {
            return;
        }

        URL url = new URL("ftp://" + HttpTestEnv.FTP_HOST + "/notfound");
        int response = 0;

        TestUserAgent._type = TestUserAgent.OAKLANDSWTEST_DOMAIN;

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");
        response = urlCon.getResponseCode();

        // Some proxies return a 404, and some indicate the error in text
        if (response == 200) {
            String res = Util.getStringFromInputStream(urlCon.getInputStream());
            assertContains(res, "550");
        } else if (response != 404) {
            fail("Invalid response: " + response);
        }
    }

    public void testFtpDir() throws Exception {
        if (!doTest()) {
            return;
        }

        String res = testFtp("");

        // System.out.println(res);
        assertTrue(res.length() > 100);
        assertContains(res, "HttpRequestSample.java");
    }

    public void testFtpHp() throws Exception {
        if (!doTest()) {
            return;
        }

        String res = testFtpUrl("ftp.itrc.hp.com/mpe-ix_patches/c.75.00/.INTHD64A.txt",
                !SSL);
        assertTrue(res.length() > 1000);

        // assertTrue(res.contains("HttpRequestSample.java"));
    }

    public void testFtpsHp() throws Exception {
        // This does not work with the proxy, the idea was to get an SSL
        // connection to the proxy and then have it use FTP the normal way.
        // Can't get this to work with the ISA proxy
        if (false) {
            if (!_inIsaSslProxyTest) {
                return;
            }

            String res = testFtpUrl("ftp.itrc.hp.com:21/mpe-ix_patches/c.75.00/.INTHD64A.txt",
                    SSL);
            assertTrue(res.length() > 1000);
        }
    }

    public void allTestMethods() throws Exception {
        testFtpFile();
        testFtpFileSslVerify();
        testFtpFileNotFound();
        testFtpDir();
        testFtpHp();
        testFtpsHp();
    }
}
