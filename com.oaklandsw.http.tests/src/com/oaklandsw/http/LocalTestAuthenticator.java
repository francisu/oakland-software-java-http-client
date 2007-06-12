/*
 * $Header:
 * /home/cvspublic/jakarta-commons/httpclient/src/test/org/apache/commons/httpclient/LocalTestAuthenticator.java,v
 * 1.17 2002/09/30 19:42:13 jsdever Exp $ $Revision: 1.17 $ $Date: 2002/09/30
 * 19:42:13 $
 * ====================================================================
 * 
 * The Apache Software License, Version 1.1
 * 
 * Copyright (c) 1999-2002 The Apache Software Foundation. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any, must
 * include the following acknowlegement: "This product includes software
 * developed by the Apache Software Foundation (http://www.apache.org/)."
 * Alternately, this acknowlegement may appear in the software itself, if and
 * wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
 * Foundation" must not be used to endorse or promote products derived from this
 * software without prior written permission. For written permission, please
 * contact apache@apache.org.
 * 
 * 5. Products derived from this software may not be called "Apache" nor may
 * "Apache" appear in their names without prior written permission of the Apache
 * Group.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE APACHE
 * SOFTWARE FOUNDATION OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many individuals on
 * behalf of the Apache Software Foundation. For more information on the Apache
 * Software Foundation, please see <http://www.apache.org/>.
 * 
 * [Additional notices, if required by prior licensing conditions]
 * 
 */

package com.oaklandsw.http;

import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.bouncycastle.util.encoders.Base64;

import com.oaklandsw.http.Authenticator;
import com.oaklandsw.http.Credential;
import com.oaklandsw.http.HttpException;
import com.oaklandsw.http.UserCredential;
import com.oaklandsw.util.LogUtils;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit tests for {@link Authenticator}.
 * 
 * @author Rodney Waldhoff
 * @author <a href="mailto:jsdever@apache.org">Jeff Dever </a>
 * @version $Id: LocalTestAuthenticator.java,v 1.17 2002/09/30 19:42:13 jsdever
 *          Exp $
 */
public class LocalTestAuthenticator extends TestCase
{
    private static final Log _log = LogUtils.makeLogger();

    // Used only for the regression tests
    public static final boolean authenticateForTests(HttpURLConnectInternal method,
                                                     String authReq,
                                                     int normalOrProxy)
        throws HttpException
    {
        Headers h = new Headers();
        if (authReq != null)
        {
            h.add(Authenticator.REQ_HEADERS[HttpURLConnection.AUTH_NORMAL],
                  authReq);
        }
        else
        {
            h = null;
        }

        // parse the authenticate header
        Map challengeMap = Authenticator
                .parseAuthenticateHeader(h,
                                         Authenticator.REQ_HEADERS[HttpURLConnection.AUTH_NORMAL],
                                         method);

        return Authenticator
                .authenticate(method,
                              h,
                              Authenticator.REQ_HEADERS[HttpURLConnection.AUTH_NORMAL],
                              challengeMap,
                              normalOrProxy);
    }

    // ------------------------------------------------------------ Constructor
    public LocalTestAuthenticator(String testName)
    {
        super(testName);
    }

    // ------------------------------------------------------------------- Main
    public static void main(String args[])
    {
        String[] testCaseName = { LocalTestAuthenticator.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }

    // ------------------------------------------------------- Utility Methods

    // We assume the web server is running
    protected void setUp()
    {
        HttpTestEnv.setUp();
        // Get local credentials
        TestUserAgent._type = TestUserAgent.LOCAL;
        TestUserAgent._proxyType = TestUserAgent.LOCAL;
    }

    private void checkAuthorization(String realm, String methodName, String auth)
        throws Exception
    {
        Hashtable table = new Hashtable();
        StringTokenizer tokenizer = new StringTokenizer(auth, ",=\"");
        while (tokenizer.hasMoreTokens())
        {
            String key = null;
            String value = null;
            if (tokenizer.hasMoreTokens())
                key = tokenizer.nextToken();
            if (tokenizer.hasMoreTokens())
                value = tokenizer.nextToken();
            if (key != null && value != null)
            {
                table.put(key.trim(), value.trim());
            }
        }
        String response = (String)table.get("response");
        table.put("methodname", methodName);

        UserCredential cred = (UserCredential)(new TestUserAgent())
                .getCredential(realm, "http://test.url", Credential.AUTH_BASIC);

        String digest = Authenticator.createDigest(cred.getUser(), cred
                .getPassword(), table);
        assertEquals(response, digest);
    }

    // ------------------------------------------------------- TestCase Methods

    public static Test suite()
    {
        return new TestSuite(LocalTestAuthenticator.class);
    }

    public void testAuthenticationWithNoChallenge() throws Exception
    {
        SimpleHttpMethod method = new SimpleHttpMethod();

        assertFalse(authenticateForTests(method,
                                         null,
                                         HttpURLConnection.AUTH_NORMAL));
    }

    // ---------------------------------- Test Methods for Basic Authentication

    public void testBasicAuthenticationWithNoCreds()
    {
        SimpleHttpMethod method = new SimpleHttpMethod();
        try
        {
            TestUserAgent._type = TestUserAgent.NULL;
            authenticateForTests(method,
                                 "Basic realm=\"realm1\"",
                                 HttpURLConnection.AUTH_NORMAL);
            fail("Should have thrown HttpException");
        }
        catch (HttpException e)
        {
            // expected
        }
    }

    public void testBasicAuthenticationWithNoRealm()
    {
        SimpleHttpMethod method = new SimpleHttpMethod();
        try
        {
            authenticateForTests(method, "Basic", HttpURLConnection.AUTH_NORMAL);
            fail("Should have thrown HttpException");
        }
        catch (HttpException e)
        {
            // expected
        }
    }

    public void testBasicAuthenticationWithNoRealm2()
    {
        SimpleHttpMethod method = new SimpleHttpMethod();
        try
        {
            authenticateForTests(method, "Basic", HttpURLConnection.AUTH_NORMAL);
            fail("Should have thrown HttpException");
        }
        catch (HttpException e)
        {
            // expected
        }
    }

    public void testInvalidAuthenticationScheme() throws Exception
    {
        SimpleHttpMethod method = new SimpleHttpMethod();
        try
        {
            assertTrue(authenticateForTests(method,
                                            "invalid realm=\"realm1\"",
                                            HttpURLConnection.AUTH_NORMAL));
            fail("Should have thrown UnsupportedOperationException");
        }
        catch (HttpException uoe)
        {
            // expected
        }
    }

    public void testBasicAuthenticationCaseInsensitivity() throws Exception
    {
        SimpleHttpMethod method = new SimpleHttpMethod();
        assertTrue(authenticateForTests(method,
                                        "bAsIc ReAlM=\"realm1\"",
                                        HttpURLConnection.AUTH_NORMAL));
        assertTrue(null != method.getRequestProperty("Authorization"));
        String expected = "Basic "
            + new String(Base64.encode("username:password".getBytes()));
        assertEquals(expected, method.getRequestProperty("Authorization"));
    }

    public void testBasicAuthenticationWithDefaultCreds() throws Exception
    {
        SimpleHttpMethod method = new SimpleHttpMethod();
        assertTrue(authenticateForTests(method,
                                        "Basic realm=\"realm1\"",
                                        HttpURLConnection.AUTH_NORMAL));
        assertTrue(null != method.getRequestProperty("Authorization"));
        String expected = "Basic "
            + new String(Base64.encode("username:password".getBytes()));
        assertEquals(expected, method.getRequestProperty("Authorization"));
    }

    public void testBasicAuthentication() throws Exception
    {
        SimpleHttpMethod method = new SimpleHttpMethod();
        assertTrue(authenticateForTests(method,
                                        "Basic realm=\"realm1\"",
                                        HttpURLConnection.AUTH_NORMAL));
        assertTrue(null != method.getRequestProperty("Authorization"));
        String expected = "Basic "
            + new String(Base64.encode("username:password".getBytes()));
        assertEquals(expected, method.getRequestProperty("Authorization"));
    }

    public void testBasicAuthenticationWithMutlipleRealms() throws Exception
    {
        {
            SimpleHttpMethod method = new SimpleHttpMethod();
            assertTrue(authenticateForTests(method,
                                            "Basic realm=\"realm1\"",
                                            HttpURLConnection.AUTH_NORMAL));
            assertTrue(null != method.getRequestProperty("Authorization"));
            String expected = "Basic "
                + new String(Base64.encode("username:password".getBytes()));
            assertEquals(expected, method.getRequestProperty("Authorization"));
        }
        {
            SimpleHttpMethod method = new SimpleHttpMethod();
            assertTrue(authenticateForTests(method,
                                            "Basic realm=\"realm2\"",
                                            HttpURLConnection.AUTH_NORMAL));
            assertTrue(null != method.getRequestProperty("Authorization"));
            String expected = "Basic "
                + new String(Base64.encode("uname2:password2".getBytes()));
            assertEquals(expected, method.getRequestProperty("Authorization"));
        }
    }

    public void testPreemptiveAuthorizationTrueNoCreds() throws Exception
    {
        HttpURLConnection.setPreemptiveAuthentication(true);
        SimpleHttpMethod method = new SimpleHttpMethod();

        TestUserAgent._type = TestUserAgent.NULL;
        assertFalse(authenticateForTests(method,
                                         null,
                                         HttpURLConnection.AUTH_NORMAL));
    }

    public void testPreemptiveAuthorizationTrueWithCreds() throws Exception
    {
        HttpURLConnection.setPreemptiveAuthentication(true);
        SimpleHttpMethod method = new SimpleHttpMethod();

        assertTrue(authenticateForTests(method,
                                        null,
                                        HttpURLConnection.AUTH_NORMAL));
        assertTrue(null != method.getRequestProperty("Authorization"));
        String expected = "Basic "
            + new String(Base64.encode("username:password".getBytes()));
        assertEquals(expected, method.getRequestProperty("Authorization"));
    }

    public void testPreemptiveAuthorizationFalse() throws Exception
    {
        HttpURLConnection.setPreemptiveAuthentication(false);
        SimpleHttpMethod method = new SimpleHttpMethod();

        assertTrue(!authenticateForTests(method,
                                         null,
                                         HttpURLConnection.AUTH_NORMAL));
        assertTrue(null == method.getRequestProperty("Authorization"));
    }

    // --------------------------------- Test Methods for Digest Authentication

    public void testDigestAuthenticationWithNoCreds()
    {
        SimpleHttpMethod method = new SimpleHttpMethod();
        try
        {
            TestUserAgent._type = TestUserAgent.NULL;
            authenticateForTests(method,
                                 "Digest realm=\"realm1\"",
                                 HttpURLConnection.AUTH_NORMAL);
            fail("Should have thrown HttpException");
        }
        catch (HttpException e)
        {
            // expected
        }
    }

    public void testDigestAuthenticationWithNoRealm()
    {
        SimpleHttpMethod method = new SimpleHttpMethod();
        try
        {
            authenticateForTests(method,
                                 "Digest",
                                 HttpURLConnection.AUTH_NORMAL);
            fail("Should have thrown HttpException");
        }
        catch (HttpException e)
        {
            // expected
        }
    }

    public void testDigestAuthenticationWithNoRealm2()
    {
        SimpleHttpMethod method = new SimpleHttpMethod();
        try
        {
            authenticateForTests(method,
                                 "Digest",
                                 HttpURLConnection.AUTH_NORMAL);
            fail("Should have thrown HttpException");
        }
        catch (HttpException e)
        {
            // expected
        }
    }

    public void testDigestAuthenticationCaseInsensitivity() throws Exception
    {
        SimpleHttpMethod method = new SimpleHttpMethod();
        assertTrue(authenticateForTests(method,
                                        "dIgEsT ReAlM=\"realm1\"",
                                        HttpURLConnection.AUTH_NORMAL));
        assertTrue(null != method.getRequestProperty("Authorization"));
    }

    public void testDigestAuthenticationWithDefaultCreds() throws Exception
    {
        SimpleHttpMethod method = new SimpleHttpMethod();
        assertTrue(authenticateForTests(method,
                                        "Digest realm=\"realm1\"",
                                        HttpURLConnection.AUTH_NORMAL));
        assertTrue(null != method.getRequestProperty("Authorization"));
        checkAuthorization("realm1", method.getName(), method
                .getRequestProperty("Authorization"));
    }

    public void testDigestAuthentication() throws Exception
    {
        SimpleHttpMethod method = new SimpleHttpMethod();
        assertTrue(authenticateForTests(method,
                                        "Digest realm=\"realm1\"",
                                        HttpURLConnection.AUTH_NORMAL));
        assertTrue(null != method.getRequestProperty("Authorization"));
        checkAuthorization("realm1", method.getName(), method
                .getRequestProperty("Authorization"));
    }

    public void testDigestAuthenticationWithMultipleRealms() throws Exception
    {
        {
            SimpleHttpMethod method = new SimpleHttpMethod();
            assertTrue(authenticateForTests(method,
                                            "Digest realm=\"realm1\"",
                                            HttpURLConnection.AUTH_NORMAL));
            assertTrue(null != method.getRequestProperty("Authorization"));
            checkAuthorization("realm1", method.getName(), method
                    .getRequestProperty("Authorization"));
        }
        {
            SimpleHttpMethod method = new SimpleHttpMethod();
            assertTrue(authenticateForTests(method,
                                            "Digest realm=\"realm2\"",
                                            HttpURLConnection.AUTH_NORMAL));
            assertTrue(null != method.getRequestProperty("Authorization"));
            checkAuthorization("realm2", method.getName(), method
                    .getRequestProperty("Authorization"));
        }
    }

    // --------------------------------- Test Methods for NTLM Authentication

    public void testNTLMAuthenticationWithNoCreds()
    {
        SimpleHttpMethod method = new SimpleHttpMethod();
        try
        {
            TestUserAgent._type = TestUserAgent.NULL;
            authenticateForTests(method, "NTLM", HttpURLConnection.AUTH_NORMAL);
            fail("Should have thrown HttpException");
        }
        catch (HttpException e)
        {
            // expected
        }
    }

    public void testNTLMAuthenticationCaseInsensitivity() throws Exception
    {
        SimpleHttpMethod method = new SimpleHttpMethod();
        assertTrue(authenticateForTests(method,
                                        "nTLM",
                                        HttpURLConnection.AUTH_NORMAL));
        assertTrue(null != method.getRequestProperty("Authorization"));
    }

    /**
     * Test that the Unauthorized response is returned when doAuthentication is
     * false.
     */
    public void testDoAuthenticateFalse() throws Exception
    {
        SimpleHttpMethod method = new SimpleHttpMethod();
        method.setDoAuthentication(false);
        SimpleHttpConnection conn = new SimpleHttpConnection();
        conn.addResponse("HTTP/1.1 401 Unauthorized\r\n"
            + "WWW-Authenticate: Basic realm=\"Protected\"\r\n"
            + "Connection: close\r\n"
            + "Server: HttpClient Test/2.0\r\n");
        conn.addResponse("HTTP/1.1 200 OK\r\n"
            + "Connection: close\r\n"
            + "Server: HttpClient Test/2.0\r\n");
        method.setState(conn);
        assertNotNull(method.getHeaderField("WWW-Authenticate"));
        assertNull(method.getRequestProperty("Authorization"));
        assertEquals(401, method.getResponseCode());

    }

    /** 
     */
    public void testInvalidCredentials() throws Exception
    {
        SimpleHttpMethod method = new SimpleHttpMethod();
        method.setDoAuthentication(false);
        SimpleHttpConnection conn = new SimpleHttpConnection();
        conn.addResponse("HTTP/1.1 401 Unauthorized\r\n"
            + "WWW-Authenticate: Basic realm=\"Protected\"\r\n"
            + "Connection: close\r\n"
            + "Server: HttpClient Test/2.0\r\n");
        method.setState(conn);
        assertEquals(401, method.getResponseCode());
    }

    // --------------------------------- Test Methods for Multiple
    // Authentication

    public void testMultipleChallengeBasic() throws Exception
    {
        SimpleHttpMethod method = new SimpleHttpMethod();
        SimpleHttpConnection conn = new SimpleHttpConnection();
        conn.addResponse("HTTP/1.1 401 Unauthorized\r\n"
            + "WWW-Authenticate: Unsupported\r\n"
            + "WWW-Authenticate: Basic realm=\"Protected\"\r\n"
            + "Connection: close\r\n"
            + "Server: HttpClient Test/2.0\r\n");
        conn.addResponse("HTTP/1.1 200 OK\r\n"
            + "Connection: close\r\n"
            + "Server: HttpClient Test/2.0\r\n");
        method.setState(conn);
        method.getResponseCode();
        String authHeader = method.getRequestProperty("Authorization");
        assertNotNull(authHeader);

        String authValue = authHeader;
        assertTrue(authValue.startsWith("Basic"));
    }

    public void testMultipleChallengeBasicLongRealm() throws Exception
    {
        SimpleHttpMethod method = new SimpleHttpMethod();
        SimpleHttpConnection conn = new SimpleHttpConnection();
        conn
                .addResponse("HTTP/1.1 401 Unauthorized\r\n"
                    + "WWW-Authenticate: Unsupported\r\n"
                    + "WWW-Authenticate: Basic realm=\"This site is protected.  We put this message into the realm string, against all reasonable rationale, so that users would see it in the authentication dialog generated by your browser.\"\r\n"
                    + "Connection: close\r\n"
                    + "Server: HttpClient Test/2.0\r\n");
        conn.addResponse("HTTP/1.1 200 OK\r\n"
            + "Connection: close\r\n"
            + "Server: HttpClient Test/2.0\r\n");
        method.setState(conn);
        method.getResponseCode();
        String authHeader = method.getRequestProperty("Authorization");
        assertNotNull(authHeader);

        String authValue = authHeader;
        assertTrue(authValue.startsWith("Basic"));
    }

    public void testMultipleChallengeDigest() throws Exception
    {
        SimpleHttpMethod method = new SimpleHttpMethod();
        SimpleHttpConnection conn = new SimpleHttpConnection();
        conn.addResponse("HTTP/1.1 401 Unauthorized\r\n"
            + "WWW-Authenticate: Unsupported\r\n"
            + "WWW-Authenticate: Digest realm=\"Protected\"\r\n"
            + "WWW-Authenticate: Basic realm=\"Protected\"\r\n"
            + "Connection: close\r\n"
            + "Server: HttpClient Test/2.0\r\n");
        conn.addResponse("HTTP/1.1 200 OK\r\n"
            + "Connection: close\r\n"
            + "Server: HttpClient Test/2.0\r\n");
        method.setState(conn);
        method.getResponseCode();
        String authHeader = method.getRequestProperty("Authorization");
        assertNotNull(authHeader);

        String authValue = authHeader;
        assertTrue(authValue.startsWith("Digest"));
    }

    public void testMultipleProxyChallengeBasic() throws Exception
    {
        SimpleHttpMethod method = new SimpleHttpMethod();
        SimpleHttpConnection conn = new SimpleHttpConnection();
        conn.addResponse("HTTP/1.1 407 Proxy Authentication Required\r\n"
            + "Proxy-Authenticate: Basic realm=\"Protected\"\r\n"
            + "Proxy-Authenticate: Unsupported\r\n"
            + "Connection: close\r\n"
            + "Server: HttpClient Test/2.0\r\n");
        conn.addResponse("HTTP/1.1 200 OK\r\n"
            + "Connection: close\r\n"
            + "Server: HttpClient Test/2.0\r\n");
        method.setState(conn);
        method.getResponseCode();
        String authHeader = method.getRequestProperty("Proxy-Authorization");
        assertNotNull(authHeader);

        String authValue = authHeader;
        assertTrue(authValue.startsWith("Basic"));
    }

    public void testMultipleProxyChallengeDigest() throws Exception
    {
        SimpleHttpMethod method = new SimpleHttpMethod();
        SimpleHttpConnection conn = new SimpleHttpConnection();
        conn.addResponse("HTTP/1.1 407 Proxy Authentication Required\r\n"
            + "Proxy-Authenticate: Basic realm=\"Protected\"\r\n"
            + "Proxy-Authenticate: Digest realm=\"Protected\"\r\n"
            + "Proxy-Authenticate: Unsupported\r\n"
            + "Connection: close\r\n"
            + "Server: HttpClient Test/2.0\r\n");
        conn.addResponse("HTTP/1.1 200 OK\r\n"
            + "Connection: close\r\n"
            + "Server: HttpClient Test/2.0\r\n");
        method.setState(conn);
        method.getResponseCode();
        String authHeader = method.getRequestProperty("Proxy-Authorization");
        assertNotNull(authHeader);

        String authValue = authHeader;
        assertTrue(authValue.startsWith("Digest"));
    }

}
