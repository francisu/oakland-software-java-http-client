package com.oaklandsw.http.webapp;

import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.HostnameVerifier;
import com.oaklandsw.http.HttpConnection;
import com.oaklandsw.http.HttpTestBase;
import com.oaklandsw.http.HttpTestEnv;
import com.oaklandsw.http.HttpURLConnection;
import com.oaklandsw.http.TestUserAgent;
import com.oaklandsw.http.servlet.RequestBodyServlet;
import com.oaklandsw.util.LogUtils;

public class TestSSL extends TestWebappBase
{

    int _verifierDelay;

    public TestSSL(String testName)
    {
        super(testName);

        _doAuthProxyTest = true;
        _doAuthCloseProxyTest = true;
        _doProxyTest = true;
        _do10ProxyTest = true;
        _doIsaProxyTest = true;
        _doIsaSslProxyTest = true;
        _doExplicitTest = true;
        _doAppletTest = true;
    }

    public static void main(String args[])
    {
        mainRun(suite(), args);
    }

    public static Test suite()
    {
        return new TestSuite(TestSSL.class);

    }

    TrustManager[] allCertsTrustManager = new TrustManager[] { new X509TrustManager()
                                        {

                                            public X509Certificate[] getAcceptedIssuers()
                                            {
                                                System.out
                                                        .println("\ngetAcceptedIssuers\n");
                                                return null;
                                            }

                                            public void checkClientTrusted(X509Certificate[] certs,
                                                                           String authType)
                                            {
                                                System.out
                                                        .println("\ncheckClientTrusted\n");

                                            }

                                            public void checkServerTrusted(X509Certificate[] certs,
                                                                           String authType)
                                            {
                                                // System.out.println("\ncheckServerTrusted\n");

                                            }
                                        } };

    public void setUp() throws Exception
    {
        super.setUp();
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.getClientSessionContext();
        sc.init(null, allCertsTrustManager, new SecureRandom());

        // Register the SocketFactory / Hostname Verifier for Oakland HttpClient
        HttpURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        HttpURLConnection.setDefaultHostnameVerifier(new HostnameVerifier()
        {
            public boolean verify(String string, SSLSession sSLSession)
            {
                try
                {
                    Thread.sleep(_verifierDelay);
                }
                catch (InterruptedException e)
                {
                }
                return true;
            }
        });

    }

    public void tearDown() throws Exception
    {
        super.tearDown();
        com.oaklandsw.http.HttpURLConnection
                .setDefaultSSLSocketFactory((SSLSocketFactory)SSLSocketFactory
                        .getDefault());
        // Need to leave it as it is to check that the default hostname
        // verifier is properly setup
        // com.oaklandsw.http.HttpURLConnection.setDefaultHostnameVerifier(null);
        com.oaklandsw.http.HttpConnection._testNonMatchHost = false;

        _verifierDelay = 0;
    }

    public void testHttpsNormal() throws Exception
    {
        URL url = new URL(_urlBase + RequestBodyServlet.NAME);

        HttpURLConnection urlCon;

        url = new URL(HttpTestEnv.TEST_LOCAL_SSL_URL + "httptest/index.html");
        urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("POST");
        urlCon.connect();
        assertEquals(200, urlCon.getResponseCode());

        String data = HttpTestBase.getReply(urlCon);
        assertTrue("No data returned.", (data.length() > 0));
        checkNoActiveConns(url);
        HttpURLConnection.setDefaultIdleConnectionPing(0);
        HttpURLConnection.closeAllPooledConnections();
    }

    // Bug 2202 idle connection ping on a proxied SSL connection can cause problems
    // bug 2546 don't do idle connection ping if authentication is in progress
    public void testHttpsPostIdleConnectionPing() throws Exception
    {
        LogUtils.logAll();
        
        HttpURLConnection.setDefaultIdleConnectionPing(50);

        URL url;
        HttpURLConnection urlCon;

        _verifierDelay = 100;
        HttpConnection._pingDone = false;
        TestUserAgent._delayTime = 100;
        
        url = new URL(HttpTestEnv.TEST_LOCAL_SSL_URL + "httptest/index.html");
        urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("POST");
        urlCon.connect();
        assertEquals(200, urlCon.getResponseCode());
        String data = HttpTestBase.getReply(urlCon);
        assertTrue("No data returned.", (data.length() > 0));
        
        // Make sure no ping happened in this case
        assertFalse(HttpConnection._pingDone);
        
        Thread.sleep(400);
        urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("POST");
        urlCon.connect();
        assertEquals(200, urlCon.getResponseCode());
        data = HttpTestBase.getReply(urlCon);
        assertTrue("No data returned.", (data.length() > 0));

        // Make sure ping is back working
        assertTrue(HttpConnection._pingDone);

        checkNoActiveConns(url);
        HttpURLConnection.setDefaultIdleConnectionPing(0);
        HttpURLConnection.closeAllPooledConnections();
        LogUtils.logNone();
    }

    public void allTestMethods() throws Exception
    {
        testHttpsNormal();
        testHttpsPostIdleConnectionPing();

    }
}
