/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oaklandsw.http.webext;

import com.oaklandsw.http.HttpTestBase;
import com.oaklandsw.http.HttpTestEnv;
import com.oaklandsw.http.HttpURLConnection;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.IOException;

import java.net.URL;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLSocketFactory;


public class TestSSL extends HttpTestBase {
    public TestSSL(String testName) {
        super(testName);

        _doAuthProxyTest = true;
        _doAuthCloseProxyTest = true;
        _doProxyTest = true;
        _do10ProxyTest = true;
        _doExplicitTest = true;
        _doAppletTest = true;
    }

    public static void main(String[] args) {
        mainRun(suite(), args);
    }

    public static Test suite() {
        return new TestSuite(TestSSL.class);
    }

    public void tearDown() throws Exception {
        super.tearDown();
        com.oaklandsw.http.HttpURLConnection.setDefaultSSLSocketFactory((SSLSocketFactory) SSLSocketFactory.getDefault());
        // Need to leave it as it is to check that the default hostname
        // verifier is properly setup
        // com.oaklandsw.http.HttpURLConnection.setDefaultHostnameVerifier(null);
        com.oaklandsw.http.HttpConnection._testNonMatchHost = false;
    }

    public void testHttpsGet(URL url) throws IOException {
        // System.out.println(System.currentTimeMillis() + " do get");
        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");
        urlCon.connect();
        assertEquals(200, urlCon.getResponseCode());

        String data = HttpTestBase.getReply(urlCon);
        assertTrue("No data returned.", (data.length() > 0));
        checkNoActiveConns(url);
    }

    public void testHttpsGetNothing() throws Exception {
        testHttpsGet(new URL(HttpTestEnv.TEST_WEBEXT_SSL_URL));
    }

    public void testHttpsGetSetDefault() throws Exception {
        TestSSLSocketFactory sf = new TestSSLSocketFactory();

        com.oaklandsw.http.HttpURLConnection.setDefaultSSLSocketFactory(sf);
        testHttpsGet(new URL(HttpTestEnv.TEST_WEBEXT_SSL_URL));

        if (com.oaklandsw.http.HttpURLConnection.getDefaultSSLSocketFactory() != sf) {
            fail("Socket factory mismatch");
        }

        if (!sf._used) {
            fail("Default socket factory not used");
        }
    }

    public void testHttpsNullSocketFromFactory() throws Exception {
        TestSSLSocketFactory sf = new TestSSLSocketFactory();
        sf._returnNull = true;
        com.oaklandsw.http.HttpURLConnection.setDefaultSSLSocketFactory(sf);

        try {
            testHttpsGet(new URL(HttpTestEnv.TEST_WEBEXT_SSL_URL));
            fail("Did not get expected exception");
        } catch (IOException ex) {
            assertContains(ex.getMessage(), "returned null for");
        }
    }

    public void testHttpsGetSetDefaultCheck() throws Exception {
        TestSSLSocketFactory sf = new TestSSLSocketFactory();

        com.oaklandsw.http.HttpURLConnection.setDefaultSSLSocketFactory(sf);

        URL url = new URL(HttpTestEnv.TEST_WEBEXT_SSL_URL);
        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);

        if ((urlCon).getSSLSocketFactory() != sf) {
            fail("Socket factory mismatch");
        }

        urlCon.setRequestMethod("GET");
        urlCon.connect();
        assertEquals(200, urlCon.getResponseCode());
        urlCon.getInputStream().close();
        checkNoActiveConns(url);
    }

    public void testHttpsGetSetConnection() throws Exception {
        TestSSLSocketFactory sf = new TestSSLSocketFactory();

        URL url = new URL(HttpTestEnv.TEST_WEBEXT_SSL_URL);
        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        (urlCon).setSSLSocketFactory(sf);

        urlCon.setRequestMethod("GET");
        urlCon.connect();
        assertEquals(200, urlCon.getResponseCode());
        urlCon.getInputStream().close();

        if ((urlCon).getSSLSocketFactory() != sf) {
            fail("Socket factory mismatch");
        }

        if (!sf._used) {
            fail("Default socket factory not used");
        }

        checkNoActiveConns(url);
    }

    // Bug 1143 - a default hostname verifier should be installed which fails
    // if the hostname does not match
    // NOTE - must be first before the DefaultHostnameVerifier is disturbed
    public void testHttpsGetSetHasDefaultVerifier() throws Exception {
        if (com.oaklandsw.http.HttpURLConnection.getDefaultHostnameVerifier() == null) {
            fail("No default verifier present");
        }
    }

    // Bug 1143 - a default hostname verifier should be installed which fails
    // if the hostname does not match
    // NOTE - must be first before the DefaultHostnameVerifier is disturbed
    public void testHttpsGetSetNoVerifierUsedFail() throws Exception {
        com.oaklandsw.http.HttpConnection._testNonMatchHost = true;

        try {
            testHttpsGet(new URL(HttpTestEnv.TEST_WEBEXT_SSL_URL));
            fail("did not get expected exception");
        } catch (IOException ex) {
            // System.out.println("exc: " + ex);
        }
    }

    public void testHttpsGetSetDefaultVerifierUsedPass()
        throws Exception {
        TestHostnameVerifier ver = new TestHostnameVerifier();

        ver._shouldPass = true;
        com.oaklandsw.http.HttpURLConnection.setDefaultHostnameVerifier(ver);
        com.oaklandsw.http.HttpConnection._testNonMatchHost = true;
        testHttpsGet(new URL(HttpTestEnv.TEST_WEBEXT_SSL_URL));

        if (com.oaklandsw.http.HttpURLConnection.getDefaultHostnameVerifier() != ver) {
            fail("Verifier factory mismatch");
        }

        if (!ver._used) {
            fail("Default verifier not used");
        }
    }

    public void testHttpsGetSetDefaultVerifierUsedFail()
        throws Exception {
        TestHostnameVerifier ver = new TestHostnameVerifier();

        ver._shouldPass = false;
        com.oaklandsw.http.HttpURLConnection.setDefaultHostnameVerifier(ver);
        com.oaklandsw.http.HttpConnection._testNonMatchHost = true;

        try {
            testHttpsGet(new URL(HttpTestEnv.TEST_WEBEXT_SSL_URL));
            fail("did not get expected exception");
        } catch (IOException ex) {
            // System.out.println("exc: " + ex);
        }
    }

    public void testHttpsGetSetDefaultVerifierUsedException()
        throws Exception {
        TestHostnameVerifier ver = new TestHostnameVerifier();

        ver._doThrow = true;
        com.oaklandsw.http.HttpURLConnection.setDefaultHostnameVerifier(ver);
        com.oaklandsw.http.HttpConnection._testNonMatchHost = true;

        try {
            testHttpsGet(new URL(HttpTestEnv.TEST_WEBEXT_SSL_URL));
            fail("did not get expected exception");
        } catch (IOException ex) {
            // System.out.println("exc: " + ex);
        }
    }

    public void testHttpsGetSetDefaultVerifierNotUsed()
        throws Exception {
        TestHostnameVerifier ver = new TestHostnameVerifier();

        com.oaklandsw.http.HttpURLConnection.setDefaultHostnameVerifier(ver);
        testHttpsGet(new URL(HttpTestEnv.TEST_WEBEXT_SSL_URL));

        if (com.oaklandsw.http.HttpURLConnection.getDefaultHostnameVerifier() != ver) {
            fail("Verifier factory mismatch");
        }

        if (ver._used) {
            fail("Default verifier unexpectedly used");
        }
    }

    public void testHttpsGetSetDefaultVerifierNotUsedSetCon()
        throws Exception {
        TestHostnameVerifier ver = new TestHostnameVerifier();

        URL url = new URL(HttpTestEnv.TEST_WEBEXT_SSL_URL);
        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        (urlCon).setHostnameVerifier(ver);
        urlCon.setRequestMethod("GET");
        urlCon.connect();

        if ((urlCon).getHostnameVerifier() != ver) {
            fail("Verifier factory mismatch");
        }

        if (ver._used) {
            fail("Default verifier unexpectedly not used");
        }

        assertEquals(200, urlCon.getResponseCode());
        urlCon.getInputStream().close();
        checkNoActiveConns(url);
    }

    public void testHttpsGetSetDefaultVerifierNotUsedSetAfterCon()
        throws Exception {
        TestHostnameVerifier ver = new TestHostnameVerifier();

        URL url = new URL(HttpTestEnv.TEST_WEBEXT_SSL_URL);
        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");
        urlCon.connect();

        try {
            (urlCon).setHostnameVerifier(ver);
            fail("Did not get expected IllegalStateException");
        } catch (IllegalStateException ex) {
        }

        assertEquals(200, urlCon.getResponseCode());
        urlCon.getInputStream().close();
        checkNoActiveConns(url);
    }

    public void testHttpsGetLocalCert() throws Exception {
        URL url = new URL(HttpTestEnv.TEST_WEBEXT_SSL_URL);
        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);

        urlCon.setRequestMethod("GET");
        urlCon.connect();

        Certificate[] certs = (urlCon).getLocalCertificates();

        if (certs != null) {
            fail("Unexpected local certificates");
        }

        assertEquals(200, urlCon.getResponseCode());
        urlCon.getInputStream().close();
        checkNoActiveConns(url);
    }

    public void testHttpsGetServerCert() throws Exception {
        URL url = new URL(HttpTestEnv.TEST_WEBEXT_SSL_URL);
        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);

        urlCon.setRequestMethod("GET");
        urlCon.connect();

        Certificate[] certs = (urlCon).getServerCertificates();

        if (!(certs[0] instanceof X509Certificate)) {
            fail("Invalid certificate");
        }

        assertEquals(200, urlCon.getResponseCode());
        urlCon.getInputStream().close();
        checkNoActiveConns(url);
    }

    public void testHttpsGetCipherSuite() throws Exception {
        URL url = new URL(HttpTestEnv.TEST_WEBEXT_SSL_URL);
        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);

        urlCon.setRequestMethod("GET");
        urlCon.connect();

        String cipherSuite = (urlCon).getCipherSuite();

        if (cipherSuite.indexOf("SSL") != 0) {
            fail("Invalid cipher suite");
        }

        assertEquals(200, urlCon.getResponseCode());
        urlCon.getInputStream().close();
        checkNoActiveConns(url);
    }

    public void allTestMethods() throws Exception {
        testHttpsGetNothing();
        tearDown();
        testHttpsGetSetDefault();
        tearDown();
        testHttpsNullSocketFromFactory();
        tearDown();
        testHttpsGetSetDefaultCheck();
        tearDown();
        testHttpsGetSetConnection();
        tearDown();
        testHttpsGetSetHasDefaultVerifier();
        tearDown();
        testHttpsGetSetNoVerifierUsedFail();
        tearDown();
        testHttpsGetSetDefaultVerifierUsedPass();
        tearDown();
        testHttpsGetSetDefaultVerifierUsedFail();
        tearDown();
        testHttpsGetSetDefaultVerifierUsedException();
        tearDown();
        testHttpsGetSetDefaultVerifierNotUsed();
        tearDown();
        testHttpsGetSetDefaultVerifierNotUsedSetCon();
        tearDown();
        testHttpsGetSetDefaultVerifierNotUsedSetAfterCon();
        tearDown();
        testHttpsGetSetDefaultVerifierNotUsed();
        tearDown();
        testHttpsGetLocalCert();
        tearDown();
        testHttpsGetServerCert();
        tearDown();
        testHttpsGetCipherSuite();
    }
}
