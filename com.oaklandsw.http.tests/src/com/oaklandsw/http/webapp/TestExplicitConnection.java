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

import com.oaklandsw.http.HttpConnection;
import com.oaklandsw.http.HttpConnectionManager;
import com.oaklandsw.http.HttpURLConnection;
import com.oaklandsw.http.servlet.ParamServlet;
import com.oaklandsw.http.servlet.RequestBodyServlet;

import com.oaklandsw.utillog.Log;
import com.oaklandsw.util.LogUtils;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.net.URL;


public class TestExplicitConnection extends TestWebappBase {
    private static final Log _log = LogUtils.makeLogger();
    protected HttpConnectionManager _connManager;

    public TestExplicitConnection(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(TestExplicitConnection.class);

        return suite;
    }

    public static void main(String[] args) {
        mainRun(suite(), args);
    }

    public void setUp() throws Exception {
        super.setUp();
        _connManager = HttpURLConnection.getConnectionManager();
    }

    public void testConnect() throws Exception {
        URL url;
        HttpURLConnection urlCon;

        // 
        url = new URL(_urlBase);
        urlCon = HttpURLConnection.openConnection(url);

        // Create a connection to _urlBase
        HttpConnection conn = _connManager.getConnection(urlCon);

        url = new URL(_urlBase + RequestBodyServlet.NAME);

        assertEquals(1, getActiveConns(url));
        assertEquals(1, getTotalConns(url));

        // Once
        urlCon = HttpURLConnection.openConnection(url);
        // use the connection
        urlCon.setConnection(conn);
        urlCon.getResponseCode();

        assertEquals(1, getActiveConns(url));
        assertEquals(1, getTotalConns(url));

        // Twice
        urlCon = HttpURLConnection.openConnection(url);
        // use it again
        urlCon.setConnection(conn);
        urlCon.getResponseCode();

        assertEquals(1, getActiveConns(url));
        assertEquals(1, getTotalConns(url));

        // Disconnect
        urlCon = HttpURLConnection.openConnection(url);
        urlCon.setConnection(conn);
        urlCon.disconnect();

        checkNoActiveConns(url);
    }

    public void testConnect2Conns() throws Exception {
        URL url;
        HttpURLConnection urlCon;

        url = new URL(_urlBase);
        urlCon = HttpURLConnection.openConnection(url);

        HttpConnection conn = _connManager.getConnection(urlCon);
        HttpConnection conn2 = _connManager.getConnection(urlCon);

        url = new URL(_urlBase + ParamServlet.NAME);

        assertEquals(2, getActiveConns(url));
        assertEquals(2, getTotalConns(url));

        // Once
        urlCon = HttpURLConnection.openConnection(url);

        HttpURLConnection urlCon2 = HttpURLConnection.openConnection(url);
        urlCon = HttpURLConnection.openConnection(url);
        urlCon2 = HttpURLConnection.openConnection(url);
        urlCon.setConnection(conn);
        urlCon2.setConnection(conn2);
        checkReply(urlCon, paramReply("GET"));
        checkReply(urlCon2, paramReply("GET"));

        assertEquals(2, getActiveConns(url));
        assertEquals(2, getTotalConns(url));

        // Disconnect
        urlCon = HttpURLConnection.openConnection(url);
        urlCon2 = HttpURLConnection.openConnection(url);
        urlCon.setConnection(conn);
        urlCon2.setConnection(conn2);
        urlCon.disconnect();
        urlCon2.disconnect();

        checkNoActiveConns(url);
    }

    public void testConnectBadState() throws Exception {
        URL url;
        HttpURLConnection urlCon;

        url = new URL(_urlBase);
        urlCon = HttpURLConnection.openConnection(url);

        HttpConnection conn = _connManager.getConnection(urlCon);

        url = new URL(_urlBase + RequestBodyServlet.NAME);

        // Once
        urlCon = HttpURLConnection.openConnection(url);
        urlCon.connect();

        try {
            urlCon.setConnection(conn);
            fail("Missed expected exception");
        } catch (IllegalStateException ex) {
            // expected
        }

        urlCon.disconnect();

        assertEquals(1, getActiveConns(url));
        assertEquals(1, getTotalConns(url));

        // Disconnect
        urlCon = HttpURLConnection.openConnection(url);
        urlCon.setConnection(conn);
        urlCon.disconnect();

        checkNoActiveConns(url);
    }
}
