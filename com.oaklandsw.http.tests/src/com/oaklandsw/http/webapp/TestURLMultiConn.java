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
import com.oaklandsw.http.servlet.HeaderServlet;

import com.oaklandsw.utillog.Log;
import com.oaklandsw.util.LogUtils;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.net.URL;


public class TestURLMultiConn extends TestWebappBase {
    private static final Log _log = LogUtils.makeLogger();

    public TestURLMultiConn(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(TestURLMultiConn.class);

        return suite;
    }

    public static void main(String[] args) {
        mainRun(suite(), args);
    }

    public void testMultiConnections() throws Exception {
        URL url = new URL(_urlBase + HeaderServlet.NAME);

        // Make sure I can do multiple connections to the same
        // place
        for (int i = 0; i < 50; i++) {
            //System.out.println("Connect: " + i);
            HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
            urlCon.connect();
            assertEquals(200, urlCon.getResponseCode());
            urlCon.getInputStream().close();
        }
    }
}
