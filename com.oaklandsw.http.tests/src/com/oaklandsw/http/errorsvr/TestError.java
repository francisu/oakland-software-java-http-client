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
package com.oaklandsw.http.errorsvr;

import com.oaklandsw.http.HttpException;
import com.oaklandsw.http.HttpTestBase;
import com.oaklandsw.http.HttpTestEnv;
import com.oaklandsw.http.HttpURLConnection;

import com.oaklandsw.utillog.Log;
import com.oaklandsw.util.LogUtils;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.net.URL;


public class TestError extends HttpTestBase {
    private static final Log _log = LogUtils.makeLogger();
    protected static String _errorUrl = HttpTestEnv.TEST_URL_HOST_ERRORSVR;

    public TestError(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(TestError.class);

        return suite;
    }

    public static void main(String[] args) {
        mainRun(suite(), args);
    }

    public void testBadCL() throws Exception {
        URL url = new URL(_errorUrl + "?error=none" + "&badContentLength=true" +
                _errorDebug);

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");
        urlCon.connect();

        try {
            urlCon.getResponseCode();
            fail("Did not get expected exception");
        } catch (HttpException ex) {
            // Expected exception about bad content length
            assertTrue(ex.getMessage().indexOf("Content-Length") > 0);
        }

        checkNoActiveConns(url);
    }

    public void allTestMethods() throws Exception {
        testBadCL();
    }
}
