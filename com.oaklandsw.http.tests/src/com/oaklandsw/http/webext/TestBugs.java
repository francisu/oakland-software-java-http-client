// Copyright 2007, Oakland Software Incorporated
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
import com.oaklandsw.http.HttpURLConnection;
import com.oaklandsw.http.TestUserAgent;

import com.oaklandsw.util.Util;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.IOException;
import java.io.InputStream;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.zip.GZIPInputStream;


public class TestBugs extends HttpTestBase {
    HttpURLConnection _urlCon;

    public TestBugs(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestBugs.class);
    }

    public static void main(String[] args) {
        mainRun(suite(), args);
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    // Connect to public test sharepoint server
    public void testBug1816() throws MalformedURLException, IOException {
        TestUserAgent._type = TestUserAgent.OFFICESHARE_ICEWEB;

        // URL url = new
        // URL("http://www.xsolive.com/_layouts/Authenticate.aspx?Source=%2Fdefault%2Easpx");
        URL url = new URL(
                "http://sharepoint.iceweb.com/sites/demo/default.aspx");
        int response = 0;

        _urlCon = HttpURLConnection.openConnection(url);
        _urlCon.connect();
        response = _urlCon.getResponseCode();
        assertEquals(200, response);

        getReply(_urlCon);
    }

    // Bug 1964 gzip encoding does not work
    public void testBug1964() throws MalformedURLException, IOException {
        URL url = new URL("http://www.cnn.com");
        int response = 0;

        _urlCon = HttpURLConnection.openConnection(url);
        _urlCon.addRequestProperty("accept-encoding", "gzip");
        _urlCon.connect();
        response = _urlCon.getResponseCode();
        assertEquals(200, response);

        //byte[] bytes = Util.getBytesFromInputStream(_urlCon.getInputStream());

        //System.out.println(HexFormatter.dump(bytes));
        InputStream is = new GZIPInputStream(_urlCon.getInputStream());

        // Dies here because of negative byte value read
        Util.getStringFromInputStream(is);
    }
}
