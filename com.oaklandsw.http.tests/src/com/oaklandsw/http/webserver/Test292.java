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

import junit.framework.Test;
import junit.framework.TestSuite;

import java.net.URL;


public class Test292 extends HttpTestBase {
    public Test292(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(Test292.class);
    }

    public static void main(String[] args) {
        String[] testCaseName = { Test292.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }

    private void simpleGet(URL url) {
        try {
            HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
            urlCon.setRequestMethod("GET");
            urlCon.connect();
            assertEquals(200, urlCon.getResponseCode());
            urlCon.getInputStream().close();
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("caught exception: " + ex);
        }
    }

    public void test() throws Exception {
        com.oaklandsw.http.HttpURLConnection.setMaxConnectionsPerHost(1);

        final URL url = new URL(HttpTestEnv.TEST_URL_WEBSERVER);

        simpleGet(url);

        Thread t1 = new Thread() {
                public void run() {
                    simpleGet(url);
                }
            };

        t1.start();

        Thread t2 = new Thread() {
                public void run() {
                    simpleGet(url);
                }
            };

        t2.start();

        Thread.sleep(3000);

        simpleGet(url);
        simpleGet(url);

        checkNoActiveConns(url);
    }
}
