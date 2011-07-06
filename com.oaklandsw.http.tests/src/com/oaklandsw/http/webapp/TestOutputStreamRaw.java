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
import com.oaklandsw.http.servlet.RequestBodyServlet;

import com.oaklandsw.utillog.Log;
import com.oaklandsw.util.LogUtils;
import com.oaklandsw.util.Util;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.OutputStream;

import java.net.URL;


/**
 * Tests for raw output stream
 */

// Bug 2009
public class TestOutputStreamRaw extends TestOutputStream {
    private static final Log _log = LogUtils.makeLogger();

    public TestOutputStreamRaw(String testName) {
        super(testName);
        _streamingType = STREAM_RAW;
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(TestOutputStreamRaw.class);

        return suite;
    }

    public static void main(String[] args) {
        mainRun(suite(), args);
    }

    public void testGet() throws Exception {
        URL url = new URL(_urlBase + RequestBodyServlet.NAME);

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setDoOutput(true);
        urlCon.setRequestMethod("GET");
        setupStreaming(urlCon, 100);

        OutputStream os = urlCon.getOutputStream();
        String str = "GET " + url.toString() + " HTTP/1.1\r\n\r\n";
        os.write(str.getBytes());
        os.close();
        assertEquals(200, urlCon.getResponseCode());
        assertTrue(urlCon.getHeaderFields().size() > 0);
        urlCon.getInputStream().close();
    }

    // Submitted from a customer
    public void testGetGoogle() throws Exception {
        String request = "GET / HTTP/1.1\r\n" + "Host: www.google.com\r\n" +
            "\r\n";

        URL url = new URL("http", "www.google.com", 80, "/");
        HttpURLConnection connection = HttpURLConnection.openConnection(url);
        connection.setDoOutput(true);
        connection.setRawStreamingMode(true);

        connection.getOutputStream().write(request.getBytes("UTF-8"));
        connection.getOutputStream().close();

        // This line below was missing in the customer's example
        connection.getResponseCode();

        assertTrue(connection.getHeaderFields().size() > 0);
        assertTrue(connection.getHeadersLength() > 0);
    }

    // See bug 2141 for issues about bad response code handling
    public void testGetBad501() throws Exception {
        URL url = new URL(_urlBase + RequestBodyServlet.NAME);

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setDoOutput(true);
        urlCon.setRequestMethod("GET");
        setupStreaming(urlCon, 100);

        OutputStream os = urlCon.getOutputStream();

        // This will get a 501
        String str = "GETx " + url.toString() + " HTTP/1.1\r\n\r\n";
        os.write(str.getBytes());
        os.close();
        assertEquals(501, urlCon.getResponseCode());

        String response = Util.getStringFromInputStream(urlCon.getInputStream());
        assertTrue(response.indexOf("Error report") > 0);
    }

    // See bug 2141 for issues about bad response code handling
    public void testGetBad400() throws Exception {
        URL url = new URL(_urlBase + RequestBodyServlet.NAME);

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setDoOutput(true);
        urlCon.setRequestMethod("GET");
        setupStreaming(urlCon, 100);

        OutputStream os = urlCon.getOutputStream();

        // This will get a 404
        String str = "GET " + url.toString() + "xx" + " HTTP/1.1\r\n\r\n";
        os.write(str.getBytes());
        os.close();
        assertEquals(404, urlCon.getResponseCode());

        String response = Util.getStringFromInputStream(urlCon.getInputStream());
        assertTrue(response.indexOf("Error report") > 0);
    }

    public void allTestMethods() throws Exception {
        // Don't bother with these since we are writing raw output
    }
}
