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

import java.io.IOException;

import java.net.URL;


/**
 * Simple tests for the HTTP client hitting a local webserver.
 *
 * The default configuration of Tomcat 4 will work fine.
 *
 * Tomcat 3.x will fail the OPTIONS test, because it treats OPTIONS as a GET
 * request.
 *
 * @author Remy Maucherat
 * @author Rodney Waldhoff
 * @version $Id: TestMethods.java,v 1.3 2002/02/04 15:26:43 dion Exp $
 */
public class TestMethods extends HttpTestBase {
    private static String _host = HttpTestEnv.TEST_WEBSERVER_HOST;
    private static int _port = HttpTestEnv.WEBSERVER_PORT;

    public TestMethods(String testName) {
        super(testName);
        _doProxyTest = true;
        _do10ProxyTest = true;
        _doExplicitTest = true;
    }

    public static void main(String[] args) {
        mainRun(suite(), args);
    }

    public static Test suite() {
        return new TestSuite(TestMethods.class);
    }

    /**
     * This test assumes that the webserver listening on host/port will respond
     * properly to an OPTIONS request. Tomcat 4 is one such web server, but
     * Tomcat 3.x is not.
     */

    // Options does not seem to be allowed now with apache 2.0.50
    public void NORUNtestMethodsOptions() throws IOException {
        URL url = new URL("http://" + _host + ":" + _port + "/");
        int response = 0;

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("OPTIONS");
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        String data = urlCon.getHeaderField("allow");
        assertTrue("No data returned.", (data.length() > 0));

        checkNoActiveConns(url);
    }

    public void testMethodsGet() throws IOException {
        URL url = new URL("http://" + _host + ":" + _port + "/");
        int response = 0;

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        String data = HttpTestBase.getReply(urlCon);
        assertTrue("No data returned.", (data.length() > 0));

        url = new URL("http://" + _host + ":" + _port +
                HttpTestEnv.TEST_WEBSERVER_PAGE);

        urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        data = HttpTestBase.getReply(urlCon);
        assertTrue("No data returned.", (data.length() > 0));

        checkNoActiveConns(url);
    }

    public void testMethodsHead() throws IOException {
        URL url = new URL("http://" + _host + ":" + _port + "/");
        int response = 0;

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("HEAD");
        urlCon.connect();
        response = urlCon.getResponseCode();
        // Can be a 304 after a previous recent HEAD request
        assertTrue((response == 200) || (response == 304));

        url = new URL("http://" + _host + ":" + _port +
                HttpTestEnv.TEST_WEBSERVER_PAGE);

        urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("HEAD");
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertTrue((response == 200) || (response == 304));
    }

    public void allTestMethods() throws Exception {
        // testMethodsOptions();
        testMethodsGet();
        testMethodsHead();
    }
}
