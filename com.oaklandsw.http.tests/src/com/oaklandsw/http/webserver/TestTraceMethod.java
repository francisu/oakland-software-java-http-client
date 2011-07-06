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
 *
 * Simple tests of {@link TraceMethod}hitting a local HTTP server.
 * <p>
 * This test suite assumes a webserver is running on port 8080 on the 127.0.0.1
 * (localhost) host. It further assumes that this webserver will respond to an
 * HTTP TRACE of <tt>/</tt> with a 200 response.
 * <p>
 * You can change the assumed port by setting the "httpclient.test.localPort"
 * property. You can change the assumed host by setting the
 * "httpclient.test.localHost" property.
 *
 * @author Sean C. Sullivan
 *
 * @version $Id: TestGetMethod.java,v 1.3 2002/02/04 15:26:43 dion Exp $
 *
 */
public class TestTraceMethod extends HttpTestBase {
    private static String _host = HttpTestEnv.TEST_WEBSERVER_HOST;
    private static int _port = HttpTestEnv.WEBSERVER_PORT;

    public TestTraceMethod(String testName) {
        super(testName);
        _doProxyTest = true;
        _do10ProxyTest = true;
        _doExplicitTest = true;
    }

    public static void main(String[] args) {
        String[] testCaseName = { TestTraceMethod.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }

    public static Test suite() {
        return new TestSuite(TestTraceMethod.class);
    }

    public void testExecute() throws IOException {
        URL url = new URL("http://" + _host + ":" + _port + "/");
        int response = 0;

        final String strTestHeaderName = "MyTestHeader";
        final String strTestHeaderValue = "This-is-a-test-value.";

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("TRACE");
        urlCon.setRequestProperty(strTestHeaderName, strTestHeaderValue);
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        assertEquals(strTestHeaderValue,
            urlCon.getRequestProperty(strTestHeaderName));

        //
        // note: the reason that we convert the String's to lowercase is
        // because some HTTP servers send a response body that contains
        // lower request headers
        //
        final String strResponseBody_lowercase = HttpTestBase.getReply(urlCon)
                                                             .toLowerCase();
        assertNotNull(strResponseBody_lowercase);
        assertTrue(strResponseBody_lowercase.length() > 0);

        assertTrue(strResponseBody_lowercase.indexOf(
                strTestHeaderName.toLowerCase()) != -1);
        assertTrue(strResponseBody_lowercase.indexOf(
                strTestHeaderValue.toLowerCase()) != -1);
        checkNoActiveConns(url);
    }

    public void allTestMethods() throws Exception {
        testExecute();
    }
}
