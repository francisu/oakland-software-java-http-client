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
package com.oaklandsw.http.local;

import com.oaklandsw.http.HttpTestBase;
import com.oaklandsw.http.HttpURLConnection;
import com.oaklandsw.http.SimpleHttpMethod;

import com.oaklandsw.utillog.Log;
import com.oaklandsw.util.LogUtils;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.net.URL;


/**
 * @author Rodney Waldhoff
 * @author <a href="mailto:jsdever@apache.org">Jeff Dever </a>
 * @version $Revision: 1.7 $ $Date: 2002/08/06 15:15:32 $
 */
public class TestMethodsNoHost extends HttpTestBase {
    private static final Log _log = LogUtils.makeLogger();

    public TestMethodsNoHost(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestMethodsNoHost.class);
    }

    public void testHttpMethodBasePaths() throws Exception {
        SimpleHttpMethod simple = new SimpleHttpMethod();
        String[] paths = {
                "/some/absolute/path", "../some/relative/path", "/",
                "/some/path/with?query=string"
            };

        for (int i = 0; i < paths.length; i++) {
            simple.setPathQuery(paths[i]);
            assertEquals(paths[i], simple.getPathQuery());
        }
    }

    public void testHttpMethodBaseDefaultPath() throws Exception {
        SimpleHttpMethod simple = new SimpleHttpMethod();
        assertEquals("/", simple.getPath());

        simple.setPathQuery("");
        assertEquals("/", simple.getPath());

        simple.setPathQuery(null);
        assertEquals("/", simple.getPath());
    }

    public void testHttpMethodBasePathConstructor() throws Exception {
        SimpleHttpMethod simple = new SimpleHttpMethod();
        assertEquals("/", simple.getPath());

        simple = new SimpleHttpMethod();
        simple.setPathQuery("");
        assertEquals("/", simple.getPath());

        simple = new SimpleHttpMethod();
        simple.setPathQuery("/some/path/");
        assertEquals("/some/path/", simple.getPath());
    }

    // Leaks connections if there is a failure to connect
    public void testBug385() throws Exception {
        // Bad location
        URL url = new URL("http://localhost:55555");

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");

        try {
            // Should fail
            urlCon.connect();
            fail("Did not get expected exception");
        } catch (Exception ex) {
            // Expected
        }

        // Should be no connections
        checkNoTotalConns(url);

        // com.oaklandsw.http.HttpURLConnection.dumpConnectionPool();
    }

    public void testStreamBadChunkSize() throws Exception {
        URL url = new URL("http://doesnotmatter");

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);

        // These should all work
        urlCon.setChunkedStreamingMode(-1);
        urlCon.setChunkedStreamingMode(0);
        urlCon.setChunkedStreamingMode(25);
    }

    public void testStreamRawMultipleTimes() throws Exception {
        URL url = new URL("http://doesnotmatter");

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);

        // These should all work
        urlCon.setRawStreamingMode(true);
        urlCon.setRawStreamingMode(false);
        urlCon.setRawStreamingMode(true);
        urlCon.setRawStreamingMode(true);

        // Should not work since using raw
        try {
            urlCon.setFixedLengthStreamingMode(10);
            fail("Expected exception");
        } catch (IllegalStateException ex) {
            // OK
        }
    }

    public void testStreamBadFixedSize() throws Exception {
        URL url = new URL("http://doesnotmatter");

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);

        try {
            urlCon.setFixedLengthStreamingMode(-1);
            fail("Expected exception");
        } catch (IllegalArgumentException ex) {
            // OK
        }

        urlCon.setFixedLengthStreamingMode(0);
        urlCon.setFixedLengthStreamingMode(100);
    }
}
