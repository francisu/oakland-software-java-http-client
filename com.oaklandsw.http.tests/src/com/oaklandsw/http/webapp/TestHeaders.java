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

import com.oaklandsw.http.HttpTestEnv;
import com.oaklandsw.http.HttpURLConnection;
import com.oaklandsw.http.servlet.HeaderServlet;

import com.oaklandsw.utillog.Log;
import com.oaklandsw.util.LogUtils;
import com.oaklandsw.util.NetUtils;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.net.InetAddress;
import java.net.URL;

import java.util.List;
import java.util.Map;


public class TestHeaders extends TestWebappBase {
    private static final Log _log = LogUtils.makeLogger();

    public TestHeaders(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(TestHeaders.class);

        return suite;
    }

    public static void main(String[] args) {
        mainRun(suite(), args);
    }

    public void testAddRequestHeader() throws Exception {
        URL url = new URL(_urlBase + HeaderServlet.NAME);
        int response = 0;

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");
        urlCon.setRequestProperty("addRequestHeader", "Also True");
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        // Netproxy seems to strip the header
        if (!_inAuthCloseProxyTest) {
            // Tomcat 4 at least converts the header name to lower case
            checkReply(urlCon,
                "name=\"addrequestheader" + "\";value=\"Also True\"<br>");
        } else {
            // checkReply - above gets the InputStream
            urlCon.getInputStream().close();
        }

        checkNoActiveConns(url);
    }

    // public void testRemoveRequestHeader() throws Exception
    // {
    // HttpClient client = new HttpClient();
    // client.startSession(host, port);
    // GetMethod method = new GetMethod("/" + context + "/headers");
    // method.setRequestHeader(new Header("XXX-A-HEADER", "true"));
    // method.removeRequestHeader("XXX-A-HEADER");
    // method.setUseDisk(false);
    // try
    // {
    // client.executeMethod(method);
    // }
    // catch (Throwable t)
    // {
    // t.printStackTrace();
    // fail("Unable to execute method : " + t.toString());
    // }
    // // Tomcat 4 at least converts the header name to lower case
    // assertTrue(!(method.getResponseBodyAsString().indexOf("xxx-a-header") >=
    // 0));
    // }
    public void testOverwriteRequestHeader() throws Exception {
        URL url = new URL(_urlBase + HeaderServlet.NAME);
        int response = 0;

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");
        urlCon.setRequestProperty("xxx-a-header", "one");
        urlCon.setRequestProperty("XXX-A-HEADER", "two");
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        // Netproxy seems to strip the header
        if (!_inAuthCloseProxyTest) {
            checkReply(urlCon, "name=\"xxx-a-header\";value=\"two\"<br>");
        } else {
            urlCon.getInputStream().close();
        }

        checkNoActiveConns(url);
    }

    public void testGetResponseHeader() throws Exception {
        URL url = new URL(_urlBase + HeaderServlet.NAME);
        int response = 0;

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);
        assertEquals("Yes", urlCon.getHeaderField("headersetbyservlet"));

        // Bug 1440 getHeaderFields() not implemented
        // Need to cast this since getHeaderFields() is not provided prior
        // to JDK14
        Map headerMap = urlCon.getHeaderFields();
        List headerField = (List) headerMap.get("HeaderSetByServlet");
        assertEquals("Yes", headerField.get(0));

        urlCon.getInputStream().close();
        checkNoActiveConns(url);
    }

    public void testGetResponseHeader0() throws Exception {
        URL url = new URL(_urlBase + HeaderServlet.NAME);
        int response = 0;

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);
        assertEquals("HTTP/1.1 200 OK", urlCon.getHeaderField(0));

        urlCon.getInputStream().close();
        checkNoActiveConns(url);
    }

    public void testHostRequestHeaderIp() throws Exception {
        // This only works if we are proxying through something on this
        // machine
        if ((com.oaklandsw.http.HttpURLConnection.getProxyHost() != null) &&
                !com.oaklandsw.http.HttpURLConnection.getProxyHost()
                                                         .equals(InetAddress.getLocalHost()
                                                                                .getHostName())) {
            return;
        }

        InetAddress addr = InetAddress.getByName(HttpTestEnv.TOMCAT_HOST);
        String ip = addr.getHostAddress();
        hostRequestHeader(ip);
    }

    public void testHostRequestHeaderName() throws Exception {
        InetAddress addr = InetAddress.getByName(HttpTestEnv.TOMCAT_HOST);
        String hostname = addr.getHostName();
        hostRequestHeader(hostname);
    }

    // Bug 1031 allow user-agent header to be set by user
    public void testUserAgentHeader() throws Exception {
        URL url = new URL(_urlBase + HeaderServlet.NAME);
        int response = 0;

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestProperty("User-Agent", "TestUserAgent");
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        // Tomcat 4 at least converts the header name to lower case
        // checkReply("name=\"addrequestheader(header)\";value=\"True\"<br>");
        checkReply(urlCon,
            "name=\"user-agent" + "\";value=\"TestUserAgent\"<br>");

        // checkReply closes the connection
        checkNoActiveConns(url);
    }

    // Make sure normal user-agent header is correct
    public void testNormalUserAgentHeader() throws Exception {
        URL url = new URL(_urlBase + HeaderServlet.NAME);
        int response = 0;

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        // Tomcat 4 at least converts the header name to lower case
        // checkReply("name=\"addrequestheader(header)\";value=\"True\"<br>");
        checkReply(urlCon,
            "name=\"user-agent" + "\";value=\"" +
            new String(com.oaklandsw.http.HttpURLConnection.DEFAULT_USER_AGENT) +
            "\"<br>");

        // checkReply closes the connection
        checkNoActiveConns(url);
    }

    public void hostRequestHeader(String connectAddr) throws Exception {
        URL url = new URL("http://" + connectAddr + ":" +
                HttpTestEnv.TOMCAT_PORT_1 + "/" +
                HttpTestEnv.TEST_URL_APP_TOMCAT_1 + HeaderServlet.NAME);
        int response = 0;

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        _log.debug(NetUtils.dumpHeaders(urlCon));

        if (HttpTestEnv.TOMCAT_PORT_1 == 80) {
            checkReply(urlCon, "name=\"host\";value=\"" + connectAddr +
                "\"<br>");
        } else {
            checkReply(urlCon,
                "name=\"host\";value=\"" + connectAddr + ":" +
                HttpTestEnv.TOMCAT_PORT_1 + "\"<br>");
        }

        // checkReply closes the connection
        checkNoActiveConns(url);
    }

    public void allTestMethods() throws Exception {
        testAddRequestHeader();
        testOverwriteRequestHeader();
        testGetResponseHeader();
        testUserAgentHeader();
        testHostRequestHeaderIp();

        // Netproxy does not seem to like this now 24 sep 09 FRU
        if (!_inAuthCloseProxyTest) {
            testHostRequestHeaderName();
        }
    }
}
