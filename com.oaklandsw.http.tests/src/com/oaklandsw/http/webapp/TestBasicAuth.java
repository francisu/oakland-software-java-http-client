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
package com.oaklandsw.http.webapp;

import com.oaklandsw.http.HttpURLConnection;
import com.oaklandsw.http.TestUserAgent;
import com.oaklandsw.http.servlet.BasicAuthServlet;

import com.oaklandsw.utillog.Log;
import com.oaklandsw.util.LogUtils;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.OutputStream;

import java.net.URL;


public class TestBasicAuth extends TestWebappBase {
    private static final Log _log = LogUtils.makeLogger();

    public TestBasicAuth(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(TestBasicAuth.class);

        return suite;
    }

    public static void main(String[] args) {
        mainRun(suite(), args);
    }

    public void testSimpleAuthGet() throws Exception {
        TestUserAgent._type = TestUserAgent.GOOD;

        URL url = new URL(_urlBase + BasicAuthServlet.NAME);
        int response = 0;

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");

        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        String reply = getReply(urlCon);
        assertTrue(checkReplyNoAssert(reply,
                "<title>BasicAuth Servlet: GET</title>"));
        assertTrue(checkReplyNoAssert(reply,
                "<p>You have authenticated as \"jakarta:commons\"</p>"));
        checkNoActiveConns(url);

        // Try it again, getting the same connection hopefully
        urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");

        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        reply = getReply(urlCon);
        assertTrue(checkReplyNoAssert(reply,
                "<title>BasicAuth Servlet: GET</title>"));
        assertTrue(checkReplyNoAssert(reply,
                "<p>You have authenticated as \"jakarta:commons\"</p>"));
        checkNoActiveConns(url);
    }

    // bug 2288 problem with sharing the connection based on credentials
    public void testSimpleAuthGetNoShare() throws Exception {
        TestUserAgent._type = TestUserAgent.GOOD;

        HttpURLConnection.setDefaultUserAgent(null);

        URL url = new URL(_urlBase + BasicAuthServlet.NAME);
        int response = 0;

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");

        TestUserAgent ua = new TestUserAgent();
        ua._localType = TestUserAgent.GOOD;
        urlCon.setUserAgent(ua);
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        String reply = getReply(urlCon);
        assertTrue(checkReplyNoAssert(reply,
                "<title>BasicAuth Servlet: GET</title>"));
        assertTrue(checkReplyNoAssert(reply,
                "<p>You have authenticated as \"jakarta:commons\"</p>"));
        checkNoActiveConns(url);

        // Try it again, should not get the same connection
        urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");
        ua = new TestUserAgent();
        ua._localType = TestUserAgent.GOOD2;
        urlCon.setUserAgent(ua);

        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        reply = getReply(urlCon);
        assertTrue(checkReplyNoAssert(reply,
                "<title>BasicAuth Servlet: GET</title>"));
        assertTrue(checkReplyNoAssert(reply,
                "<p>You have authenticated as \"uname:passwd\"</p>"));
        checkNoActiveConns(url);
    }

    public void testSimpleAuthPost() throws Exception {
        TestUserAgent._type = TestUserAgent.GOOD;

        URL url = new URL(_urlBase + BasicAuthServlet.NAME);
        int response = 0;

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("POST");
        urlCon.setDoOutput(true);

        OutputStream out = urlCon.getOutputStream();
        out.write("testing=one".getBytes("ASCII"));
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        String reply = getReply(urlCon);
        assertTrue(checkReplyNoAssert(reply,
                "<title>BasicAuth Servlet: POST</title>"));
        assertTrue(checkReplyNoAssert(reply,
                "<p>You have authenticated as \"jakarta:commons\"</p>"));
        checkNoActiveConns(url);
    }

    public void testSimpleAuthPut() throws Exception {
        if (_inAuthCloseProxyTest) {
            return;
        }

        TestUserAgent._type = TestUserAgent.GOOD;

        URL url = new URL(_urlBase + BasicAuthServlet.NAME);
        int response = 0;

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("PUT");
        urlCon.setDoOutput(true);

        OutputStream out = urlCon.getOutputStream();
        out.write("testing one two three".getBytes("ASCII"));
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        String reply = getReply(urlCon);
        assertTrue(checkReplyNoAssert(reply,
                "<title>BasicAuth Servlet: PUT</title>"));
        assertTrue(checkReplyNoAssert(reply,
                "<p>You have authenticated as \"jakarta:commons\"</p>"));

        checkNoActiveConns(url);
    }

    public void testNoCredAuthRetry() throws Exception {
        TestUserAgent._type = TestUserAgent.NULL;

        URL url = new URL(_urlBase + BasicAuthServlet.NAME);
        int response = 0;

        HttpURLConnection.setMultiCredentialsPerAddress(true);

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");

        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(401, response);

        String reply = getReply(urlCon);
        assertTrue(checkReplyNoAssert(reply,
                "<title>BasicAuth Servlet: GET</title>"));
        assertTrue(checkReplyNoAssert(reply, "<p>Not authorized.</p>"));
        checkNoActiveConns(url);

        TestUserAgent._type = TestUserAgent.GOOD;
        url = new URL(_urlBase + BasicAuthServlet.NAME);
        urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");

        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        reply = getReply(urlCon);
        assertTrue(checkReplyNoAssert(reply,
                "<title>BasicAuth Servlet: GET</title>"));
        assertTrue(checkReplyNoAssert(reply,
                "<p>You have authenticated as \"jakarta:commons\"</p>"));
        checkNoActiveConns(url);

        HttpURLConnection.setMultiCredentialsPerAddress(false);
    }

    public void testBadCredFails() throws Exception {
        TestUserAgent._type = TestUserAgent.NULL;

        URL url = new URL(_urlBase + BasicAuthServlet.NAME);
        int response = 0;

        HttpURLConnection.setMultiCredentialsPerAddress(true);

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");

        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(401, response);

        String reply = getReply(urlCon);
        assertTrue(checkReplyNoAssert(reply,
                "<title>BasicAuth Servlet: GET</title>"));
        assertTrue(checkReplyNoAssert(reply, "<p>Not authorized.</p>"));
        checkNoActiveConns(url);

        TestUserAgent._type = TestUserAgent.BAD;
        url = new URL(_urlBase + BasicAuthServlet.NAME);
        urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");

        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(401, response);

        reply = getReply(urlCon);
        assertTrue(checkReplyNoAssert(reply,
                "<title>BasicAuth Servlet: GET</title>"));
        assertTrue(checkReplyNoAssert(reply,
                "<p>Not authorized. \"Basic YmFkOmNyZWRz\" not recognized.</p>"));
        checkNoActiveConns(url);

        HttpURLConnection.setMultiCredentialsPerAddress(false);
    }

    public void testSimpleAuthGetCache() throws Exception {
        TestUserAgent._type = TestUserAgent.GOOD;
        TestUserAgent._callCount = 0;

        HttpURLConnection.setMultiCredentialsPerAddress(false);

        URL url = new URL(_urlBase + BasicAuthServlet.NAME);
        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        getReply(urlCon);

        assertEquals(1, TestUserAgent._callCount);

        // Now again
        urlCon = HttpURLConnection.openConnection(url);
        getReply(urlCon);

        assertEquals(1, TestUserAgent._callCount);

        HttpURLConnection.resetCachedCredentials();

        // Make sure it asks again
        urlCon = HttpURLConnection.openConnection(url);
        getReply(urlCon);

        assertEquals(2, TestUserAgent._callCount);
    }

    public void testSimpleAuthGetCacheFail() throws Exception {
        TestUserAgent._type = TestUserAgent.BAD;
        TestUserAgent._callCount = 0;

        HttpURLConnection.setMultiCredentialsPerAddress(false);

        URL url = new URL(_urlBase + BasicAuthServlet.NAME);
        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        assertEquals(401, urlCon.getResponseCode());
        getReply(urlCon);

        assertEquals(1, TestUserAgent._callCount);

        // Now again - but good
        TestUserAgent._type = TestUserAgent.GOOD;
        urlCon = HttpURLConnection.openConnection(url);
        assertEquals(200, urlCon.getResponseCode());
        getReply(urlCon);

        assertEquals(2, TestUserAgent._callCount);

        // Make sure does not ask again
        urlCon = HttpURLConnection.openConnection(url);
        assertEquals(200, urlCon.getResponseCode());
        getReply(urlCon);

        assertEquals(2, TestUserAgent._callCount);
    }

    public void testSimpleAuthGetNoCache() throws Exception {
        HttpURLConnection.setMultiCredentialsPerAddress(true);

        TestUserAgent._type = TestUserAgent.GOOD;
        TestUserAgent._callCount = 0;

        URL url = new URL(_urlBase + BasicAuthServlet.NAME);
        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        getReply(urlCon);

        assertEquals(1, TestUserAgent._callCount);

        // Now again - must ask a 2nd time
        urlCon = HttpURLConnection.openConnection(url);
        getReply(urlCon);

        assertEquals(2, TestUserAgent._callCount);

        HttpURLConnection.setMultiCredentialsPerAddress(false);
    }

    public void allTestMethods() throws Exception {
        testSimpleAuthGet();
        testSimpleAuthPost();
        testSimpleAuthPut();
        testNoCredAuthRetry();
        testBadCredFails();
    }
}
