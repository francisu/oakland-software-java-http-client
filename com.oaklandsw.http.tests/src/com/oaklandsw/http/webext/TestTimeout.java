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
import com.oaklandsw.http.HttpTimeoutException;
import com.oaklandsw.http.HttpURLConnection;

import com.oaklandsw.utillog.Log;
import com.oaklandsw.util.LogUtils;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.net.URL;


public class TestTimeout extends HttpTestBase {
    private static final Log _log = LogUtils.makeLogger();

    public TestTimeout(String testName) {
        super(testName);
        _doUseDnsJava = true;
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(TestTimeout.class);

        return suite;
    }

    public static void main(String[] args) {
        mainRun(suite(), args);
    }

    public void testConnectTimeout(int type) throws Exception {
        // Make it really small - so we timeout while waiting
        // for the name resolution
        setupDefaultTimeout(type, 1);

        URL url;
        url = new URL("http://www.bbc.co.uk");

        // url = new URL("http://xxx.nepad.org");
        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);

        setupConnTimeout(urlCon, type, 1);

        urlCon.setRequestMethod("GET");

        try {
            urlCon.connect();
            fail("Should have timed out on the connect");
        } catch (HttpTimeoutException ex) {
            // Got expected exception
        }

        com.oaklandsw.http.HttpURLConnection.closeAllPooledConnections();
    }

    public void testConnectTimeoutDef() throws Exception {
        testConnectTimeout(DEF);
    }

    // This does not actually work since the connection does not timeout
    // in time
    // public void testConnectTimeoutDefConnect() throws Exception
    // {
    // testConnectTimeout(DEF_CONNECT);
    // }
    public void testConnectTimeoutConn() throws Exception {
        testConnectTimeout(CONN);
    }

    // This test does not work since the connection does not timeout
    // public void testConnectTimeoutConnConnect() throws Exception
    // {
    // testConnectTimeout(CONN_CONNECT);
    // }
    public void allTestMethods() throws Exception {
        testConnectTimeoutDef();
        // testConnectTimeoutDefConnect();
        testConnectTimeoutConn();

        // testConnectTimeoutConnConnect();
    }
}
