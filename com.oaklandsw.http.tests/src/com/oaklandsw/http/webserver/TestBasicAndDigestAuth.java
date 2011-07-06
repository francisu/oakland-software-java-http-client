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
package com.oaklandsw.http.webserver;

import com.oaklandsw.http.HttpTestBase;
import com.oaklandsw.http.HttpTestEnv;
import com.oaklandsw.http.HttpURLConnection;
import com.oaklandsw.http.TestUserAgent;

import com.oaklandsw.utillog.Log;
import com.oaklandsw.util.LogUtils;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.net.URL;


public class TestBasicAndDigestAuth extends HttpTestBase {
    private static final Log _log = LogUtils.makeLogger();

    public TestBasicAndDigestAuth(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(TestBasicAndDigestAuth.class);

        return suite;
    }

    public static void main(String[] args) {
        mainRun(suite(), args);
    }

    public void testAuthGet(String urlStr, int authType)
        throws Exception {
        TestUserAgent._type = authType;

        URL url = new URL(urlStr);
        int response = 0;

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        // Try again will authenticate again
        urlCon = HttpURLConnection.openConnection(url);
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);
    }

    public void testBasicAuthGet() throws Exception {
        testAuthGet(HttpTestEnv.TEST_URL_AUTH_BASIC,
            TestUserAgent.WEBSERVER_BASIC);
    }

    public void testDigestAuthGet() throws Exception {
        testAuthGet(HttpTestEnv.TEST_URL_AUTH_DIGEST,
            TestUserAgent.WEBSERVER_DIGEST);
    }

    public void testDigestIisAuthGet() throws Exception {
        testAuthGet(HttpTestEnv.TEST_URL_AUTH_IIS_DIGEST,
            TestUserAgent.WEBSERVER_IIS_DIGEST);
    }

    public void testNoCredAuthRetry(String urlStr, int authType,
        int expectedResponse) throws Exception {
        TestUserAgent._type = TestUserAgent.NULL;

        URL url = new URL(urlStr);
        int response = 0;

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");

        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(401, response);

        String reply = getReply(urlCon);
        assertTrue(checkReplyNoAssert(reply, "Authorization Required"));
        checkNoActiveConns(url);

        TestUserAgent._type = authType;
        url = new URL(urlStr);
        urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");

        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(expectedResponse, response);
        urlCon.getInputStream().close();
        checkNoActiveConns(url);
    }

    public void testBasicNoCredAuthRetry() throws Exception {
        testNoCredAuthRetry(HttpTestEnv.TEST_URL_AUTH_BASIC,
            TestUserAgent.WEBSERVER_BASIC, 200);
    }

    public void testDigestNoCredAuthRetry() throws Exception {
        testNoCredAuthRetry(HttpTestEnv.TEST_URL_AUTH_DIGEST,
            TestUserAgent.WEBSERVER_DIGEST, 200);
    }

    public void testBasicBadCredAuthRetry() throws Exception {
        testNoCredAuthRetry(HttpTestEnv.TEST_URL_AUTH_BASIC, TestUserAgent.BAD,
            401);
    }

    public void testDigestBadCredAuthRetry() throws Exception {
        testNoCredAuthRetry(HttpTestEnv.TEST_URL_AUTH_DIGEST,
            TestUserAgent.BAD, 401);
    }

    public void allTestMethods() throws Exception {
    }
}
