/*
 * Copyright 2004 oakland software, incorporated. All rights Reserved.
 */
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

import com.oaklandsw.utillog.Log;
import com.oaklandsw.util.LogUtils;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.net.URL;


/**
 * Tests for default values
 */
public class TestDefaults extends HttpTestBase {
    private static final Log _log = LogUtils.makeLogger();

    public TestDefaults(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(TestDefaults.class);

        return suite;
    }

    public static void main(String[] args) {
        mainRun(suite(), args);
    }

    // 962 - per connection idle timeout does not work properly
    public void testDefaultConnectionTimeout() throws Exception {
        HttpURLConnection.setDefaultIdleConnectionTimeout(99);

        URL url = new URL("http://dummy");
        HttpURLConnection urlCon1 = HttpURLConnection.openConnection(url);
        assertEquals(99, urlCon1.getIdleConnectionTimeout());
        urlCon1.setIdleConnectionTimeout(10);

        HttpURLConnection.setDefaultIdleConnectionTimeout(99);

        HttpURLConnection urlCon2 = HttpURLConnection.openConnection(url);
        assertEquals(99, urlCon2.getIdleConnectionTimeout());

        // Verify the first connection did not change
        assertEquals(10, urlCon1.getIdleConnectionTimeout());
    }

    // Same as above except for ping
    public void testDefaultConnectionPing() throws Exception {
        HttpURLConnection.setDefaultIdleConnectionPing(99);

        URL url = new URL("http://dummy");
        HttpURLConnection urlCon1 = HttpURLConnection.openConnection(url);
        assertEquals(99, urlCon1.getIdleConnectionPing());
        urlCon1.setIdleConnectionPing(10);

        HttpURLConnection.setDefaultIdleConnectionPing(99);

        HttpURLConnection urlCon2 = HttpURLConnection.openConnection(url);
        assertEquals(99, urlCon2.getIdleConnectionPing());

        // Verify the first connection did not change
        assertEquals(10, urlCon1.getIdleConnectionPing());
    }

    public void testDefaultPipelining() throws Exception {
        HttpURLConnection.setDefaultPipelining(true);

        URL url = new URL("http://dummy");
        HttpURLConnection urlCon1 = HttpURLConnection.openConnection(url);
        assertEquals(true, urlCon1.isPipelining());

        // Execute this pipelining connection (since it's associated with the
        // thread
        try {
            // Will throw since no Callback is set
            urlCon1.pipelineExecute();
            fail("Expected exception");
        } catch (IllegalStateException ex) {
            // Expected
        }

        HttpURLConnection.setDefaultPipelining(false);

        urlCon1 = HttpURLConnection.openConnection(url);
        assertEquals(false, urlCon1.isPipelining());
    }
}
