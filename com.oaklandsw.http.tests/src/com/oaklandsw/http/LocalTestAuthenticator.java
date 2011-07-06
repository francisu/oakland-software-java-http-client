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
package com.oaklandsw.http;

import com.oaklandsw.http.Authenticator;
import com.oaklandsw.http.Credential;
import com.oaklandsw.http.HttpException;
import com.oaklandsw.http.UserCredential;

import com.oaklandsw.utillog.Log;
import com.oaklandsw.util.LogUtils;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.bouncycastle.util.encoders.Base64;

import java.io.InterruptedIOException;

import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;


/**
 * Unit tests for {@link Authenticator}.
 *
 * @author Rodney Waldhoff
 * @author <a href="mailto:jsdever@apache.org">Jeff Dever </a>
 * @version $Id: LocalTestAuthenticator.java,v 1.17 2002/09/30 19:42:13 jsdever
 *          Exp $
 */
public class LocalTestAuthenticator extends HttpTestBase {
    private static final Log _log = LogUtils.makeLogger();

    // ------------------------------------------------------------ Constructor
    public LocalTestAuthenticator(String testName) {
        super(testName);
    }

    // Used only for the regression tests
    public static final boolean authenticateForTests(
        HttpURLConnectInternal method, String authReq, int normalOrProxy)
        throws HttpException, InterruptedIOException {
        Headers h = new Headers();

        if (authReq != null) {
            h.add(Authenticator.REQ_HEADERS_LC[HttpURLConnection.AUTH_NORMAL],
                authReq.getBytes());
        } else {
            h = null;
        }

        // parse the authenticate header
        Map challengeMap = Authenticator.parseAuthenticateHeader(h,
                Authenticator.REQ_HEADERS[HttpURLConnection.AUTH_NORMAL], method);

        return Authenticator.authenticate(method, h,
            Authenticator.REQ_HEADERS[HttpURLConnection.AUTH_NORMAL],
            challengeMap, normalOrProxy);
    }

    // ------------------------------------------------------------------- Main
    public static void main(String[] args) {
        mainRun(suite(), args);
    }

    // ------------------------------------------------------- Utility Methods

    // We assume the web server is running
    public void setUp() throws Exception {
        super.setUp();
        // Get local credentials
        TestUserAgent._type = TestUserAgent.LOCAL;
        TestUserAgent._proxyType = TestUserAgent.LOCAL;
    }

    private void checkAuthorization(String realm, String methodName, String auth)
        throws Exception {
        Hashtable table = new Hashtable();
        StringTokenizer tokenizer = new StringTokenizer(auth, ",=\"");

        while (tokenizer.hasMoreTokens()) {
            String key = null;
            String value = null;

            if (tokenizer.hasMoreTokens()) {
                key = tokenizer.nextToken();
            }

            if (tokenizer.hasMoreTokens()) {
                value = tokenizer.nextToken();
            }

            if ((key != null) && (value != null)) {
                table.put(key.trim(), value.trim());
            }
        }

        String response = (String) table.get("response");
        table.put("methodname", methodName);

        UserCredential cred = (UserCredential) (new TestUserAgent()).getCredential(realm,
                "http://test.url", Credential.AUTH_BASIC);

        String digest = Authenticator.createDigest(cred.getUser(),
                cred.getPassword(), table);
        assertEquals(response, digest);
    }

    // ------------------------------------------------------- TestCase Methods
    public static Test suite() {
        return new TestSuite(LocalTestAuthenticator.class);
    }

    public void testAuthenticationWithNoChallenge() throws Exception {
        SimpleHttpMethod method = new SimpleHttpMethod();

        assertFalse(authenticateForTests(method, null,
                HttpURLConnection.AUTH_NORMAL));
    }

    // ---------------------------------- Test Methods for Basic Authentication
    public void testBasicAuthenticationWithNoCreds() throws Exception {
        SimpleHttpMethod method = new SimpleHttpMethod();

        try {
            TestUserAgent._type = TestUserAgent.NULL;
            authenticateForTests(method, "Basic realm=\"realm1\"",
                HttpURLConnection.AUTH_NORMAL);
            fail("Should have thrown HttpException");
        } catch (HttpException e) {
            // expected
        }
    }

    public void testBasicAuthenticationWithNoRealm() throws Exception {
        SimpleHttpMethod method = new SimpleHttpMethod();

        try {
            authenticateForTests(method, "Basic", HttpURLConnection.AUTH_NORMAL);
            fail("Should have thrown HttpException");
        } catch (HttpException e) {
            // expected
        }
    }

    public void testBasicAuthenticationWithNoRealm2() throws Exception {
        SimpleHttpMethod method = new SimpleHttpMethod();

        try {
            authenticateForTests(method, "Basic", HttpURLConnection.AUTH_NORMAL);
            fail("Should have thrown HttpException");
        } catch (HttpException e) {
            // expected
        }
    }

    public void testInvalidAuthenticationScheme() throws Exception {
        SimpleHttpMethod method = new SimpleHttpMethod();

        try {
            assertTrue(authenticateForTests(method, "invalid realm=\"realm1\"",
                    HttpURLConnection.AUTH_NORMAL));
            fail("Should have thrown UnsupportedOperationException");
        } catch (HttpException uoe) {
            // expected
        }
    }

    public void testBasicAuthenticationCaseInsensitivity()
        throws Exception {
        SimpleHttpMethod method = new SimpleHttpMethod();
        assertTrue(authenticateForTests(method, "bAsIc ReAlM=\"realm1\"",
                HttpURLConnection.AUTH_NORMAL));
        assertTrue(null != method.getRequestProperty("Authorization"));

        String expected = "Basic " +
            new String(Base64.encode("username:password".getBytes()));
        assertEquals(expected, method.getRequestProperty("Authorization"));
    }

    public void testBasicAuthenticationWithDefaultCreds()
        throws Exception {
        SimpleHttpMethod method = new SimpleHttpMethod();
        assertTrue(authenticateForTests(method, "Basic realm=\"realm1\"",
                HttpURLConnection.AUTH_NORMAL));
        assertTrue(null != method.getRequestProperty("Authorization"));

        String expected = "Basic " +
            new String(Base64.encode("username:password".getBytes()));
        assertEquals(expected, method.getRequestProperty("Authorization"));
    }

    public void testBasicAuthentication() throws Exception {
        SimpleHttpMethod method = new SimpleHttpMethod();
        assertTrue(authenticateForTests(method, "Basic realm=\"realm1\"",
                HttpURLConnection.AUTH_NORMAL));
        assertTrue(null != method.getRequestProperty("Authorization"));

        String expected = "Basic " +
            new String(Base64.encode("username:password".getBytes()));
        assertEquals(expected, method.getRequestProperty("Authorization"));
    }

    public void testBasicAuthenticationWithMutlipleRealms()
        throws Exception {
        {
            SimpleHttpMethod method = new SimpleHttpMethod();
            assertTrue(authenticateForTests(method, "Basic realm=\"realm1\"",
                    HttpURLConnection.AUTH_NORMAL));
            assertTrue(null != method.getRequestProperty("Authorization"));

            String expected = "Basic " +
                new String(Base64.encode("username:password".getBytes()));
            assertEquals(expected, method.getRequestProperty("Authorization"));
        }

        {
            SimpleHttpMethod method = new SimpleHttpMethod();
            assertTrue(authenticateForTests(method, "Basic realm=\"realm2\"",
                    HttpURLConnection.AUTH_NORMAL));
            assertTrue(null != method.getRequestProperty("Authorization"));

            String expected = "Basic " +
                new String(Base64.encode("uname2:password2".getBytes()));
            assertEquals(expected, method.getRequestProperty("Authorization"));
        }
    }

    public void testPreemptiveAuthorizationTrueNoCreds()
        throws Exception {
        SimpleHttpMethod method = new SimpleHttpMethod();
        method.setAuthenticationType(Credential.AUTH_BASIC);
        TestUserAgent._type = TestUserAgent.NULL;
        assertFalse(authenticateForTests(method, null,
                HttpURLConnection.AUTH_NORMAL));
    }

    public void testPreemptiveAuthorizationTrueWithCreds()
        throws Exception {
        SimpleHttpMethod method = new SimpleHttpMethod();
        method.setAuthenticationType(Credential.AUTH_BASIC);

        assertTrue(authenticateForTests(method, null,
                HttpURLConnection.AUTH_NORMAL));
        assertTrue(null != method.getRequestProperty("Authorization"));

        String expected = "Basic " +
            new String(Base64.encode("username:password".getBytes()));
        assertEquals(expected, method.getRequestProperty("Authorization"));
    }

    // --------------------------------- Test Methods for Digest Authentication
    public void testDigestAuthenticationWithNoCreds() throws Exception {
        SimpleHttpMethod method = new SimpleHttpMethod();

        try {
            TestUserAgent._type = TestUserAgent.NULL;
            authenticateForTests(method, "Digest realm=\"realm1\"",
                HttpURLConnection.AUTH_NORMAL);
            fail("Should have thrown HttpException");
        } catch (HttpException e) {
            // expected
        }
    }

    public void testDigestAuthenticationWithNoRealm() throws Exception {
        SimpleHttpMethod method = new SimpleHttpMethod();

        try {
            authenticateForTests(method, "Digest", HttpURLConnection.AUTH_NORMAL);
            fail("Should have thrown HttpException");
        } catch (HttpException e) {
            // expected
        }
    }

    public void testDigestAuthenticationWithNoRealm2()
        throws Exception {
        SimpleHttpMethod method = new SimpleHttpMethod();

        try {
            authenticateForTests(method, "Digest", HttpURLConnection.AUTH_NORMAL);
            fail("Should have thrown HttpException");
        } catch (HttpException e) {
            // expected
        }
    }

    public void testDigestAuthenticationCaseInsensitivity()
        throws Exception {
        SimpleHttpMethod method = new SimpleHttpMethod();
        assertTrue(authenticateForTests(method, "dIgEsT ReAlM=\"realm1\"",
                HttpURLConnection.AUTH_NORMAL));
        assertTrue(null != method.getRequestProperty("Authorization"));
    }

    public void testDigestAuthenticationWithDefaultCreds()
        throws Exception {
        SimpleHttpMethod method = new SimpleHttpMethod();
        assertTrue(authenticateForTests(method, "Digest realm=\"realm1\"",
                HttpURLConnection.AUTH_NORMAL));
        assertTrue(null != method.getRequestProperty("Authorization"));
        checkAuthorization("realm1", method.getRequestMethod(),
            method.getRequestProperty("Authorization"));
    }

    public void testDigestAuthentication() throws Exception {
        SimpleHttpMethod method = new SimpleHttpMethod();
        assertTrue(authenticateForTests(method, "Digest realm=\"realm1\"",
                HttpURLConnection.AUTH_NORMAL));
        assertTrue(null != method.getRequestProperty("Authorization"));
        checkAuthorization("realm1", method.getRequestMethod(),
            method.getRequestProperty("Authorization"));
    }

    public void testDigestAuthenticationWithMultipleRealms()
        throws Exception {
        {
            SimpleHttpMethod method = new SimpleHttpMethod();
            assertTrue(authenticateForTests(method, "Digest realm=\"realm1\"",
                    HttpURLConnection.AUTH_NORMAL));
            assertTrue(null != method.getRequestProperty("Authorization"));
            checkAuthorization("realm1", method.getRequestMethod(),
                method.getRequestProperty("Authorization"));
        }

        {
            SimpleHttpMethod method = new SimpleHttpMethod();
            assertTrue(authenticateForTests(method, "Digest realm=\"realm2\"",
                    HttpURLConnection.AUTH_NORMAL));
            assertTrue(null != method.getRequestProperty("Authorization"));
            checkAuthorization("realm2", method.getRequestMethod(),
                method.getRequestProperty("Authorization"));
        }
    }

    // --------------------------------- Test Methods for NTLM Authentication
    public void testNTLMAuthenticationWithNoCreds() throws Exception {
        SimpleHttpMethod method = new SimpleHttpMethod();

        try {
            TestUserAgent._type = TestUserAgent.NULL;
            authenticateForTests(method, "NTLM", HttpURLConnection.AUTH_NORMAL);
            fail("Should have thrown HttpException");
        } catch (HttpException e) {
            // expected
        }
    }

    public void testNTLMAuthenticationCaseInsensitivity()
        throws Exception {
        SimpleHttpMethod method = new SimpleHttpMethod();
        assertTrue(authenticateForTests(method, "nTLM",
                HttpURLConnection.AUTH_NORMAL));
        assertTrue(null != method.getRequestProperty("Authorization"));
    }

    /**
     * Test that the Unauthorized response is returned when doAuthentication is
     * false.
     */
    public void testDoAuthenticateFalse() throws Exception {
        SimpleHttpMethod method = new SimpleHttpMethod();
        method.setDoAuthentication(false);

        SimpleHttpConnection conn = new SimpleHttpConnection();
        conn.addResponse("HTTP/1.1 401 Unauthorized\r\n" +
            "WWW-Authenticate: Basic realm=\"Protected\"\r\n" +
            "Connection: close\r\n" + "Server: HttpClient Test/2.0\r\n");
        conn.addResponse("HTTP/1.1 200 OK\r\n" + "Connection: close\r\n" +
            "Server: HttpClient Test/2.0\r\n");
        method.setState(conn);
        assertNotNull(method.getHeaderField("WWW-Authenticate"));
        assertNull(method.getRequestProperty("Authorization"));
        assertEquals(401, method.getResponseCode());
    }

    /**
     */
    public void testInvalidCredentials() throws Exception {
        SimpleHttpMethod method = new SimpleHttpMethod();
        method.setDoAuthentication(false);

        SimpleHttpConnection conn = new SimpleHttpConnection();
        conn.addResponse("HTTP/1.1 401 Unauthorized\r\n" +
            "WWW-Authenticate: Basic realm=\"Protected\"\r\n" +
            "Connection: close\r\n" + "Server: HttpClient Test/2.0\r\n");
        method.setState(conn);
        assertEquals(401, method.getResponseCode());
    }

    // --------------------------------- Test Methods for Multiple
    // Authentication
    public void testMultipleChallengeBasic() throws Exception {
        SimpleHttpMethod method = new SimpleHttpMethod();
        SimpleHttpConnection conn = new SimpleHttpConnection();
        conn.addResponse("HTTP/1.1 401 Unauthorized\r\n" +
            "WWW-Authenticate: Unsupported\r\n" +
            "WWW-Authenticate: Basic realm=\"Protected\"\r\n" +
            "Connection: close\r\n" + "Server: HttpClient Test/2.0\r\n");
        conn.addResponse("HTTP/1.1 200 OK\r\n" + "Connection: close\r\n" +
            "Server: HttpClient Test/2.0\r\n");
        method.setState(conn);
        method.getResponseCode();

        String authHeader = method.getRequestProperty("Authorization");
        assertNotNull(authHeader);

        String authValue = authHeader;
        assertTrue(authValue.startsWith("Basic"));
    }

    public void testMultipleChallengeBasicLongRealm() throws Exception {
        SimpleHttpMethod method = new SimpleHttpMethod();
        SimpleHttpConnection conn = new SimpleHttpConnection();
        conn.addResponse("HTTP/1.1 401 Unauthorized\r\n" +
            "WWW-Authenticate: Unsupported\r\n" +
            "WWW-Authenticate: Basic realm=\"This site is protected.  We put this message into the realm string, against all reasonable rationale, so that users would see it in the authentication dialog generated by your browser.\"\r\n" +
            "Connection: close\r\n" + "Server: HttpClient Test/2.0\r\n");
        conn.addResponse("HTTP/1.1 200 OK\r\n" + "Connection: close\r\n" +
            "Server: HttpClient Test/2.0\r\n");
        method.setState(conn);
        method.getResponseCode();

        String authHeader = method.getRequestProperty("Authorization");
        assertNotNull(authHeader);

        String authValue = authHeader;
        assertTrue(authValue.startsWith("Basic"));
    }

    public void testMultipleChallengeDigest() throws Exception {
        SimpleHttpMethod method = new SimpleHttpMethod();
        SimpleHttpConnection conn = new SimpleHttpConnection();
        conn.addResponse("HTTP/1.1 401 Unauthorized\r\n" +
            "WWW-Authenticate: Unsupported\r\n" +
            "WWW-Authenticate: Digest realm=\"Protected\"\r\n" +
            "WWW-Authenticate: Basic realm=\"Protected\"\r\n" +
            "Connection: close\r\n" + "Server: HttpClient Test/2.0\r\n");
        conn.addResponse("HTTP/1.1 200 OK\r\n" + "Connection: close\r\n" +
            "Server: HttpClient Test/2.0\r\n");
        method.setState(conn);
        method.getResponseCode();

        String authHeader = method.getRequestProperty("Authorization");
        assertNotNull(authHeader);

        String authValue = authHeader;
        assertTrue(authValue.startsWith("Digest"));
    }

    public void testMultipleProxyChallengeBasic() throws Exception {
        SimpleHttpMethod method = new SimpleHttpMethod();
        SimpleHttpConnection conn = new SimpleHttpConnection();
        conn.addResponse("HTTP/1.1 407 Proxy Authentication Required\r\n" +
            "Proxy-Authenticate: Basic realm=\"Protected\"\r\n" +
            "Proxy-Authenticate: Unsupported\r\n" + "Connection: close\r\n" +
            "Server: HttpClient Test/2.0\r\n");
        conn.addResponse("HTTP/1.1 200 OK\r\n" + "Connection: close\r\n" +
            "Server: HttpClient Test/2.0\r\n");
        method.setState(conn);
        method.getResponseCode();

        String authHeader = method.getRequestProperty("Proxy-Authorization");
        assertNotNull(authHeader);

        String authValue = authHeader;
        assertTrue(authValue.startsWith("Basic"));
    }

    public void testMultipleProxyChallengeDigest() throws Exception {
        SimpleHttpMethod method = new SimpleHttpMethod();
        SimpleHttpConnection conn = new SimpleHttpConnection();
        conn.addResponse("HTTP/1.1 407 Proxy Authentication Required\r\n" +
            "Proxy-Authenticate: Basic realm=\"Protected\"\r\n" +
            "Proxy-Authenticate: Digest realm=\"Protected\"\r\n" +
            "Proxy-Authenticate: Unsupported\r\n" + "Connection: close\r\n" +
            "Server: HttpClient Test/2.0\r\n");
        conn.addResponse("HTTP/1.1 200 OK\r\n" + "Connection: close\r\n" +
            "Server: HttpClient Test/2.0\r\n");
        method.setState(conn);
        method.getResponseCode();

        String authHeader = method.getRequestProperty("Proxy-Authorization");
        assertNotNull(authHeader);

        String authValue = authHeader;
        assertTrue(authValue.startsWith("Digest"));
    }
}
