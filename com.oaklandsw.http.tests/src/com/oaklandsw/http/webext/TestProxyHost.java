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

import junit.framework.Test;
import junit.framework.TestSuite;

import java.net.URL;


public class TestProxyHost extends HttpTestBase {
    public TestProxyHost(String testName) {
        super(testName);
    }

    public static void main(String[] args) {
        mainRun(suite(), args);
    }

    public static Test suite() {
        return new TestSuite(TestProxyHost.class);
    }

    public void setUp() throws Exception {
        super.setUp();
        com.oaklandsw.http.HttpURLConnection.setProxyHost(HttpTestEnv.TEST_PROXY_HOST);
        com.oaklandsw.http.HttpURLConnection.setProxyPort(HttpTestEnv.NORMAL_PROXY_PORT);
    }

    public void tearDown() throws Exception {
        super.tearDown();
        com.oaklandsw.http.HttpURLConnection.setProxyHost(null);
        com.oaklandsw.http.HttpURLConnection.setProxyPort(-1);
    }

    // 958 unlimited max connections
    public void testUnlimitedMaxConnections() throws Exception {
        // Only 1
        com.oaklandsw.http.HttpURLConnection.setMaxConnectionsPerHost(1);

        // Take one connection to timeout server
        (new OpenThread()).start();

        // Let the threads get started
        Thread.sleep(500);

        URL url = new URL("http://www.sun.com");

        com.oaklandsw.http.HttpURLConnection urlCon = (com.oaklandsw.http.HttpURLConnection) url.openConnection();

        // Should not timeout if more than one proxy connection allowed
        urlCon.setConnectionTimeout(1000);
        urlCon.setRequestMethod("GET");
        urlCon.connect();
        urlCon.getResponseCode();
    }
}
