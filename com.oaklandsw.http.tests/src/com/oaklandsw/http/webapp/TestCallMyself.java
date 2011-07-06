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

import com.oaklandsw.http.HttpURLConnection;
import com.oaklandsw.http.servlet.CallMyselfServlet;

import com.oaklandsw.utillog.Log;
import com.oaklandsw.util.LogUtils;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.OutputStream;

import java.net.URL;


public class TestCallMyself extends TestWebappBase {
    private static final Log _log = LogUtils.makeLogger();

    public TestCallMyself(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(TestCallMyself.class);

        return suite;
    }

    public static void main(String[] args) {
        mainRun(suite(), args);
    }

    public void doCallMyself(String firstMethod, String innerMethod)
        throws Exception {
        URL url = new URL(_urlBase + CallMyselfServlet.NAME + "?call=" +
                CallMyselfServlet.NAME + "&method=" + innerMethod);
        int response = 0;

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod(firstMethod);

        if (firstMethod.equals("POST")) {
            urlCon.setDoOutput(true);

            OutputStream os = urlCon.getOutputStream();
            os.write("param-one=param-value".getBytes("ASCII"));
        }

        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        String reply = getReply(urlCon);
        assertTrue(checkReplyNoAssert(reply,
                "<title>Param Servlet: " + innerMethod + "</title>"));
        assertTrue(checkReplyNoAssert(reply,
                "<title>CallMyself Servlet: " + firstMethod + "</title>"));

        if (firstMethod.equals("POST")) {
            assertTrue(checkReplyNoAssert(reply,
                    "<p>Request Body</p>\r\nparam-one=param-value"));
        }

        if (innerMethod.equals("POST")) {
            assertTrue(checkReplyNoAssert(reply,
                    "<p>Request Body</p>\r\n" +
                    "servlet-param-one=servlet-param-value"));
        }
    }

    public void testCallMyselfGG() throws Exception {
        doCallMyself("GET", "GET");
    }

    public void testCallMyselfPG() throws Exception {
        doCallMyself("POST", "GET");
    }

    public void testCallMyselfGP() throws Exception {
        doCallMyself("GET", "POST");
    }

    public void testCallMyselfPP() throws Exception {
        doCallMyself("POST", "POST");
    }

    public void allTestMethods() throws Exception {
        testCallMyselfGG();
        testCallMyselfPG();
        testCallMyselfGP();
        testCallMyselfPP();
    }
}
