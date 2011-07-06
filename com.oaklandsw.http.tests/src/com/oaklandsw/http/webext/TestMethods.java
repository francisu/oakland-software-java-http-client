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
import com.oaklandsw.http.HttpURLConnection;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.IOException;

import java.net.URL;


/**
 * Simple tests for the HTTP client hitting an external webserver.
 */
public class TestMethods extends HttpTestBase {
    public TestMethods(String testName) {
        super(testName);

        _doSocksProxyTest = true;
    }

    public static Test suite() {
        return new TestSuite(TestMethods.class);
    }

    public static void main(String[] args) {
        mainRun(suite(), args);
    }

    public void testMethodsOptionsExternal() throws IOException {
        URL url = new URL("http://" +
                HttpTestEnv.TEST_WEBEXT_EXTERNAL_OPTIONS_HOST);
        int response = 0;

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("OPTIONS");
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        String data = urlCon.getHeaderField("allow");
        assertTrue("No data returned.", (data.length() > 0));
    }

    public void testMethodsGetExternal() throws Exception {
        URL url = new URL("http://" + HttpTestEnv.TEST_WEBEXT_EXTERNAL_HOST);

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);

        String data = HttpTestBase.getReply(urlCon);
        assertTrue("No data returned.", (data.length() > 0));
    }

    public void testMethodsHeadExternal() throws Exception {
        HttpTestBase.doGetLikeMethod("http://" +
            HttpTestEnv.TEST_WEBEXT_EXTERNAL_HOST, "HEAD", null, false);
    }

    /**
     * This test proves that bad urls throw an IOException, and not some other
     * throwable like a NullPointerException.
     */
    public void testIOException() {
        try {
            URL url = new URL("http://ggttll.wwwsss.ddeeddss");
            int response = 0;
            HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
            urlCon.setRequestMethod("GET");
            urlCon.connect();
            response = urlCon.getResponseCode();

            if (response >= 400) {
                return;
            }
        } catch (IOException e) {
            return; // IOException and HttpException are ok
        }

        fail("Should have thrown an exception");
    }

    public void allTestMethods() throws Exception {
        testMethodsGetExternal();
    }
}
