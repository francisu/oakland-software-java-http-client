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

import com.oaklandsw.http.Credential;
import com.oaklandsw.http.HttpTestBase;
import com.oaklandsw.http.HttpTestEnv;
import com.oaklandsw.http.HttpURLConnection;

import com.oaklandsw.utillog.Log;
import com.oaklandsw.util.LogUtils;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.IOException;

import java.net.URL;


public class TestHttps extends HttpTestBase {
    private static final Log _log = LogUtils.makeLogger();

    public TestHttps(String testName) {
        super(testName);
        _doProxyTest = true;
        _do10ProxyTest = true;
        _doAuthProxyTest = true;
        _doAuthCloseProxyTest = true;
        _doExplicitTest = true;
        _doUseDnsJava = true;
    }

    public static void main(String[] args) {
        mainRun(suite(), args);
    }

    public static Test suite() {
        return new TestSuite(TestHttps.class);
    }

    public void testHttpsGet(URL url) throws IOException {
        int response = 0;

        // System.out.println(System.currentTimeMillis() + " do get");
        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        String data = HttpTestBase.getReply(urlCon);
        assertTrue("No data returned.", (data.length() > 0));
        checkNoActiveConns(url);
    }

    public void testHttpsGet() throws Exception {
        testHttpsGet(new URL(HttpTestEnv.TEST_WEBEXT_SSL_URL_PORT));
    }

    public void testHttpsGetMulti() throws Exception {
        for (int i = 0; i < 10; i++)
            testHttpsGet(new URL(HttpTestEnv.TEST_WEBEXT_SSL_URL_PORT));
    }

    public void testHttpsGetNoPort() throws Exception {
        testHttpsGet(new URL(HttpTestEnv.TEST_WEBEXT_SSL_URL));
    }

    // Bug 2109 authentiation through proxy does not work with SSL
    // when proxy credentials set for the connection
    public void testHttpsGetAuthProxy() throws IOException {
        int response = 0;

        URL url = new URL(HttpTestEnv.TEST_WEBEXT_SSL_URL_PORT);

        HttpURLConnection.setDefaultUserAgent(null);

        // System.out.println(System.currentTimeMillis() + " do get");
        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");

        urlCon.setConnectionProxyHost(HttpTestEnv.TEST_AUTH_PROXY_HOST);
        urlCon.setConnectionProxyPort(HttpTestEnv.AUTH_PROXY_PORT);
        urlCon.setConnectionProxyUser(HttpTestEnv.TEST_AUTH_PROXY_USER);
        urlCon.setConnectionProxyPassword(HttpTestEnv.TEST_AUTH_PROXY_PASSWORD);
        urlCon.setProxyAuthenticationType(Credential.AUTH_BASIC);

        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        String data = HttpTestBase.getReply(urlCon);
        assertTrue("No data returned.", (data.length() > 0));
        checkNoActiveConns(url);
    }

    public void allTestMethods() throws Exception {
        testHttpsGet();
        testHttpsGetMulti();
        testHttpsGetNoPort();
    }
}
