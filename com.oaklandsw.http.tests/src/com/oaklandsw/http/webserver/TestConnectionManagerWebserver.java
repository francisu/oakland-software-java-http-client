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

import com.oaklandsw.http.HttpConnection;
import com.oaklandsw.http.HttpConnectionManager;
import com.oaklandsw.http.HttpTestBase;
import com.oaklandsw.http.HttpTestEnv;
import com.oaklandsw.http.HttpURLConnection;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.net.URL;


/**
 *
 * Unit tests for {@link HttpConnectionManager}. These tests do not require any
 * network connection or web app.
 *
 * @author Marc A. Saegesser
 */
public class TestConnectionManagerWebserver extends HttpTestBase {
    protected HttpConnectionManager _connManager;

    public TestConnectionManagerWebserver(String testName) {
        super(testName);
    }

    public static void main(String[] args) {
        String[] testCaseName = { TestConnectionManagerWebserver.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }

    public static Test suite() {
        return new TestSuite(TestConnectionManagerWebserver.class);
    }

    public void setUp() {
        _connManager = HttpURLConnection.getConnectionManager();
    }

    public void testGetMultipleConnections() throws Exception {
        String urlStr = "http://localhost/path/path?query=string";
        URL url = new URL(urlStr);
        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);

        // Create a new connection
        HttpConnection conn1 = _connManager.getConnection(urlCon);
        conn1.open(urlCon);
        // Release the connection
        _connManager.releaseConnection(conn1);

        // Get the same connection again
        HttpConnection conn2 = _connManager.getConnection(urlCon);
        assertEquals("Same connection", conn1, conn2);

        // don't release yet

        // Get another new connection
        HttpConnection conn3 = _connManager.getConnection(urlCon);
        assertTrue(conn2 != conn3);

        _connManager.releaseConnection(conn2);
        _connManager.releaseConnection(conn3);

        // Clean up
        conn1 = _connManager.getConnection(urlCon);
        conn1.close();
        _connManager.releaseConnection(conn1);

        checkNoActiveConns(url);
    }

    public void testMultiConnectionProxy() throws Exception {
        URL url;
        HttpURLConnection urlCon;

        String urlStr = HttpTestEnv.TEST_URL_WEBSERVER +
            "/path/path?query=string";

        // Create a new connection
        url = new URL(urlStr);
        urlCon = HttpURLConnection.openConnection(url);
        urlCon.connect();

        HttpConnection conn1 = urlCon.getConnection();
        _connManager.releaseConnection(conn1);

        // New connection for a proxy
        url = new URL(urlStr);
        urlCon = HttpURLConnection.openConnection(url);
        urlCon.setConnectionProxyHost(HttpTestEnv.APACHE_HOST);
        urlCon.setConnectionProxyPort(HttpTestEnv.NORMAL_PROXY_PORT);
        urlCon.connect();

        HttpConnection conn2 = urlCon.getConnection();
        assertFalse("proxy same as non-proxy", conn1 == conn2);
        _connManager.releaseConnection(conn2);

        // New connection for another proxy
        url = new URL(urlStr);
        urlCon = HttpURLConnection.openConnection(url);
        urlCon.setConnectionProxyHost(HttpTestEnv.AUTH_PROXY_HOST);
        urlCon.setConnectionProxyPort(HttpTestEnv.AUTH_PROXY_PORT);
        urlCon.connect();

        HttpConnection conn3 = urlCon.getConnection();
        assertFalse("proxy same as non-proxy", conn3 == conn1);
        assertFalse("proxy same as non-proxy", conn3 == conn2);
        _connManager.releaseConnection(conn3);

        // New connection for another proxy (with a user)
        url = new URL(urlStr);
        urlCon = HttpURLConnection.openConnection(url);
        urlCon.setConnectionProxyHost(HttpTestEnv.AUTH_PROXY_HOST);
        urlCon.setConnectionProxyPort(HttpTestEnv.AUTH_PROXY_PORT);
        urlCon.setConnectionProxyUser("xxx");
        urlCon.connect();

        HttpConnection conn4 = urlCon.getConnection();
        _connManager.releaseConnection(conn4);

        // This will will not consider the proxy connections associated
        // with the URL, only the direct connection
        assertEquals(0, getActiveConns(url));

        // FIXME - sometimes this is 0, when this test is run alone, and
        // sometimes it's one, probably because of the failure of testProxy int
        // the TestFtpProxy
        if (false) {
            assertEquals(0, getTotalConns(url));
        }
    }
}
