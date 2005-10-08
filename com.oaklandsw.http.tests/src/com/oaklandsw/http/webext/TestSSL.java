package com.oaklandsw.http.webext;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLSocketFactory;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.TestBase;
import com.oaklandsw.http.TestEnv;

public class TestSSL extends TestBase
{

    public TestSSL(String testName)
    {
        super(testName);
    }

    public static void main(String args[])
    {
        mainRun(suite(), args);
    }

    public static Test suite()
    {
        return new TestSuite(TestSSL.class);
    }

    public void setUp() throws Exception
    {
        TestEnv.setUp();
    }

    public void tearDown() throws Exception
    {
        com.oaklandsw.http.HttpURLConnection
                .setDefaultSSLSocketFactory((SSLSocketFactory)SSLSocketFactory
                        .getDefault());
        com.oaklandsw.http.HttpURLConnection.setDefaultHostnameVerifier(null);
        com.oaklandsw.http.HttpConnection._testNonMatchHost = false;
    }

    public void testHttpsGet(URL url) throws IOException
    {
        // System.out.println(System.currentTimeMillis() + " do get");
        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("GET");
        urlCon.connect();
        assertEquals(200, urlCon.getResponseCode());

        String data = TestBase.getReply(urlCon);
        assertTrue("No data returned.", (data.length() > 0));
        checkNoActiveConns(url);
    }

    public void testHttpsGetSetDefault() throws Exception
    {
        TestSSLSocketFactory sf = new TestSSLSocketFactory();

        com.oaklandsw.http.HttpURLConnection.setDefaultSSLSocketFactory(sf);
        testHttpsGet(new URL(TestEnv.TEST_WEBEXT_SSL_URL));

        if (com.oaklandsw.http.HttpURLConnection.getDefaultSSLSocketFactory() != sf)
            fail("Socket factory mismatch");

        if (!sf._used)
            fail("Default socket factory not used");
    }

    public void testHttpsNullSocketFromFactory() throws Exception
    {
        TestSSLSocketFactory sf = new TestSSLSocketFactory();
        sf._returnNull = true;
        com.oaklandsw.http.HttpURLConnection.setDefaultSSLSocketFactory(sf);
        try
        {
            testHttpsGet(new URL(TestEnv.TEST_WEBEXT_SSL_URL));
            fail("Did not get expected exception");
        }
        catch (IOException ex)
        {
            assertContains(ex.getMessage(), "returned null for");
        }
    }

    public void testHttpsGetSetDefaultCheck() throws Exception
    {
        TestSSLSocketFactory sf = new TestSSLSocketFactory();

        com.oaklandsw.http.HttpURLConnection.setDefaultSSLSocketFactory(sf);
        URL url = new URL(TestEnv.TEST_WEBEXT_SSL_URL);
        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();

        if (((com.oaklandsw.http.HttpURLConnection)urlCon).getSSLSocketFactory() != sf)
            fail("Socket factory mismatch");

        urlCon.setRequestMethod("GET");
        urlCon.connect();
        assertEquals(200, urlCon.getResponseCode());
        checkNoActiveConns(url);
    }

    public void testHttpsGetSetConnection() throws Exception
    {
        TestSSLSocketFactory sf = new TestSSLSocketFactory();

        URL url = new URL(TestEnv.TEST_WEBEXT_SSL_URL);
        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        ((com.oaklandsw.http.HttpURLConnection)urlCon).setSSLSocketFactory(sf);

        urlCon.setRequestMethod("GET");
        urlCon.connect();
        assertEquals(200, urlCon.getResponseCode());
        if (((com.oaklandsw.http.HttpURLConnection)urlCon).getSSLSocketFactory() != sf)
            fail("Socket factory mismatch");
        if (!sf._used)
            fail("Default socket factory not used");
        checkNoActiveConns(url);
    }

    public void testHttpsGetSetDefaultVerifierUsedPass() throws Exception
    {
        TestHostnameVerifier ver = new TestHostnameVerifier();

        ver._shouldPass = true;
        com.oaklandsw.http.HttpURLConnection.setDefaultHostnameVerifier(ver);
        com.oaklandsw.http.HttpConnection._testNonMatchHost = true;
        testHttpsGet(new URL(TestEnv.TEST_WEBEXT_SSL_URL));

        if (com.oaklandsw.http.HttpURLConnection.getDefaultHostnameVerifier() != ver)
            fail("Verifier factory mismatch");

        if (!ver._used)
            fail("Default verifier not used");

    }

    public void testHttpsGetSetDefaultVerifierUsedFail() throws Exception
    {
        TestHostnameVerifier ver = new TestHostnameVerifier();

        ver._shouldPass = false;
        com.oaklandsw.http.HttpURLConnection.setDefaultHostnameVerifier(ver);
        com.oaklandsw.http.HttpConnection._testNonMatchHost = true;

        try
        {
            testHttpsGet(new URL(TestEnv.TEST_WEBEXT_SSL_URL));
            fail("did not get expected exception");
        }
        catch (IOException ex)
        {
            // System.out.println("exc: " + ex);
        }
    }

    public void testHttpsGetSetDefaultVerifierUsedException() throws Exception
    {
        TestHostnameVerifier ver = new TestHostnameVerifier();

        ver._doThrow = true;
        com.oaklandsw.http.HttpURLConnection.setDefaultHostnameVerifier(ver);
        com.oaklandsw.http.HttpConnection._testNonMatchHost = true;
        try
        {
            testHttpsGet(new URL(TestEnv.TEST_WEBEXT_SSL_URL));
            fail("did not get expected exception");
        }
        catch (IOException ex)
        {
            // System.out.println("exc: " + ex);
        }
    }

    public void testHttpsGetSetDefaultVerifierNotUsed() throws Exception
    {
        TestHostnameVerifier ver = new TestHostnameVerifier();

        com.oaklandsw.http.HttpURLConnection.setDefaultHostnameVerifier(ver);
        testHttpsGet(new URL(TestEnv.TEST_WEBEXT_SSL_URL));

        if (com.oaklandsw.http.HttpURLConnection.getDefaultHostnameVerifier() != ver)
            fail("Verifier factory mismatch");

        if (ver._used)
            fail("Default verifier unexpectedly used");

    }

    public void testHttpsGetSetDefaultVerifierNotUsedSetCon() throws Exception
    {
        TestHostnameVerifier ver = new TestHostnameVerifier();

        URL url = new URL(TestEnv.TEST_WEBEXT_SSL_URL);
        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        ((com.oaklandsw.http.HttpURLConnection)urlCon).setHostnameVerifier(ver);
        urlCon.setRequestMethod("GET");
        urlCon.connect();

        if (((com.oaklandsw.http.HttpURLConnection)urlCon).getHostnameVerifier() != ver)
            fail("Verifier factory mismatch");

        if (ver._used)
            fail("Default verifier unexpectedly not used");
        assertEquals(200, urlCon.getResponseCode());
        checkNoActiveConns(url);
    }

    public void testHttpsGetSetDefaultVerifierNotUsedSetAfterCon()
        throws Exception
    {
        TestHostnameVerifier ver = new TestHostnameVerifier();

        URL url = new URL(TestEnv.TEST_WEBEXT_SSL_URL);
        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("GET");
        urlCon.connect();

        try
        {
            ((com.oaklandsw.http.HttpURLConnection)urlCon)
                    .setHostnameVerifier(ver);
            fail("Did not get expected IllegalStateException");
        }
        catch (IllegalStateException ex)
        {
        }
        assertEquals(200, urlCon.getResponseCode());
        checkNoActiveConns(url);
    }

    public void testHttpsGetLocalCert() throws Exception
    {
        if (!TestEnv.GETSERVERCERT_ENABLED)
            return;

        URL url = new URL(TestEnv.TEST_WEBEXT_SSL_URL);
        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();

        urlCon.setRequestMethod("GET");
        urlCon.connect();
        Certificate[] certs = ((com.oaklandsw.http.HttpURLConnection)urlCon)
                .getLocalCertificates();
        if (certs != null)
            fail("Unexpected local certificates");
        assertEquals(200, urlCon.getResponseCode());
        checkNoActiveConns(url);
    }

    public void testHttpsGetServerCert() throws Exception
    {
        if (!TestEnv.GETSERVERCERT_ENABLED)
            return;

        URL url = new URL(TestEnv.TEST_WEBEXT_SSL_URL);
        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();

        urlCon.setRequestMethod("GET");
        urlCon.connect();
        Certificate[] certs = ((com.oaklandsw.http.HttpURLConnection)urlCon)
                .getServerCertificates();
        if (!(certs[0] instanceof X509Certificate))
            fail("Invalid certificate");
        assertEquals(200, urlCon.getResponseCode());
        checkNoActiveConns(url);
    }

    public void testHttpsGetCipherSuite() throws Exception
    {
        URL url = new URL(TestEnv.TEST_WEBEXT_SSL_URL);
        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();

        urlCon.setRequestMethod("GET");
        urlCon.connect();
        String cipherSuite = ((com.oaklandsw.http.HttpURLConnection)urlCon)
                .getCipherSuite();
        if (cipherSuite.indexOf("SSL") != 0)
            fail("Invalid cipher suite");
        assertEquals(200, urlCon.getResponseCode());
        checkNoActiveConns(url);
    }

    public void allTestMethods() throws Exception
    {
    }
}
