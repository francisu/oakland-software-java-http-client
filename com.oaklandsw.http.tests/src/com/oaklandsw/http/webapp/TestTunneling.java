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

import com.oaklandsw.utillog.Log;
import com.oaklandsw.util.LogUtils;
import com.oaklandsw.util.Util;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.InputStream;
import java.io.OutputStream;

import java.net.URL;


public class TestTunneling extends TestWebappBase {
    private static final Log _log = LogUtils.makeLogger();

    public TestTunneling(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(TestTunneling.class);

        return suite;
    }

    public static void main(String[] args) {
        mainRun(suite(), args);
    }

    // FIXME - more tests are necessary for this

    // Test using the connect method and tunneled streaming mode
    public void testTunnelConnect() throws Exception {
        if (!_in10ProxyTest) {
            return;
        }

        URL url = new URL("http://" + HttpTestEnv.FTP_HOST + ":21");
        int response = 0;

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("CONNECT");
        urlCon.setDoOutput(true);
        urlCon.setDoInput(true);
        urlCon.setTunneledStreamingMode(true);

        OutputStream outStr = urlCon.getOutputStream();

        response = urlCon.getResponseCode();
        assertEquals(200, response);

        InputStream is = urlCon.getInputStream();

        byte[] output;
        output = "USER anonymous\n".getBytes("ASCII");
        outStr.write(output);
        output = "PASS anonuser\n".getBytes("ASCII");
        outStr.write(output);
        output = "STAT\n".getBytes("ASCII");
        outStr.write(output);
        output = "QUIT\n".getBytes("ASCII");
        outStr.write(output);
        outStr.flush();
        outStr.close();

        byte[] bytes = Util.getBytesFromInputStream(is);
        String res = new String(bytes);
        // System.out.println(res);
        assertContains(res, "221 Goodbye.");
    }

    // Test using the connect method and tunneled streaming mode
    public void testTunnelMethodCheck() throws Exception {
        if (!_in10ProxyTest) {
            return;
        }

        URL url = new URL("http://" + HttpTestEnv.FTP_HOST + ":21");

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");
        urlCon.setDoOutput(true);
        urlCon.setDoInput(true);
        urlCon.setTunneledStreamingMode(true);

        try {
            urlCon.getOutputStream();
            fail("Did not get expected exception");
        } catch (IllegalStateException ex) {
            // Expected
        }
    }

    public void allTestMethods() throws Exception {
        testTunnelConnect();
        testTunnelMethodCheck();
    }
}
