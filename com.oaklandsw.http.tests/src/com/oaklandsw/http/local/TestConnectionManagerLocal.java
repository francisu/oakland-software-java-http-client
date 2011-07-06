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

package com.oaklandsw.http.local;

import com.oaklandsw.http.GlobalState;
import com.oaklandsw.http.HttpConnection;
import com.oaklandsw.http.HttpConnectionManager;
import com.oaklandsw.http.HttpTestBase;
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
public class TestConnectionManagerLocal extends HttpTestBase {
    HttpConnectionManager _connManager;
    GlobalState _globalState;

    public TestConnectionManagerLocal(String testName) {
        super(testName);
    }

    public static void main(String[] args) {
        String[] testCaseName = { TestConnectionManagerLocal.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }

    public static Test suite() {
        return new TestSuite(TestConnectionManagerLocal.class);
    }

    public void setUp() {
        _globalState = new GlobalState();
        _connManager = new HttpConnectionManager(_globalState);
        _globalState._connManager = _connManager;
    }

    public void tearDown() {
        // Back to default
        _globalState._maxConns = HttpURLConnection.DEFAULT_MAX_CONNECTIONS;
        _globalState.setProxyHost(null);
        _globalState.setProxyPort(-1);
    }

    // Test the accessor methods
    public void testProxyHostAccessors() {
        _globalState.setProxyHost("proxyhost");
        assertEquals("Proxy Host", "proxyhost", _globalState._proxyHost);
    }

    public void testProxyPortAccessors() {
        _globalState.setProxyPort(8888);
        assertEquals("Proxy Port", 8888, _globalState._proxyPort);
    }

    public void testMaxConnectionsAccessors() {
        // First test the default value (s/b 2 - don't use the constant)
        assertEquals("Default MaxConnections", 2, _globalState._maxConns);

        _globalState._maxConns = 10;
        assertEquals("MaxConnections", 10, _globalState._maxConns);
    }

    public void testGetConnection() throws Exception {
        URL url;
        HttpURLConnection urlCon;

        // Create a new connection
        url = new URL("http://www.nosuchserver.com/path/path?query=string");
        urlCon = HttpURLConnection.openConnection(url);

        HttpConnection conn = _connManager.getConnection(urlCon);
        // Validate the connection properties
        assertEquals("Host", "www.nosuchserver.com", conn._host);
        assertEquals("Port", 80, conn.getPort());
        // Release the connection
        _connManager.releaseConnection(conn);

        // Create a new connection
        url = new URL("https://www.nosuchserver.com/path/path?query=string");
        urlCon = HttpURLConnection.openConnection(url);
        conn = _connManager.getConnection(urlCon);
        // Validate the connection properties
        assertEquals("Host", "www.nosuchserver.com", conn._host);
        assertEquals("Port", 443, conn.getPort());
        // Release the connection
        _connManager.releaseConnection(conn);

        // Create a new connection
        url = new URL("http://www.nowhere.org:8080/path/path?query=string");
        urlCon = HttpURLConnection.openConnection(url);
        conn = _connManager.getConnection(urlCon);
        // Validate the connection properties
        assertEquals("Host", "www.nowhere.org", conn._host);
        assertEquals("Port", 8080, conn.getPort());
        // Release the connection
        _connManager.releaseConnection(conn);
    }
}
