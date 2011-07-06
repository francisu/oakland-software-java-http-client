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
 * Simple tests of {@link GetMethod}hitting a local webserver.
 * <p>
 * This test suite assumes a webserver is running on port 8080 on the 127.0.0.1
 * (localhost) host. It further assumes that this webserver will respond to an
 * HTTP GET of <tt>/</tt> with a 200 response.
 * <p>
 * You can change the assumed port by setting the "httpclient.test.localPort"
 * property. You can change the assumed host by setting the
 * "httpclient.test.localHost" property.
 *
 * @author Rodney Waldhoff
 * @version $Id: TestGetMethod.java,v 1.3 2002/02/04 15:26:43 dion Exp $
 */
public class TestGetMethod extends HttpTestBase {
    String _host = HttpTestEnv.TEST_WEBSERVER_HOST;
    int _port = HttpTestEnv.WEBSERVER_PORT;

    public TestGetMethod(String testName) {
        super(testName);
        _doProxyTest = true;
        _do10ProxyTest = true;
        _doExplicitTest = true;
    }

    public static Test suite() {
        return new TestSuite(TestGetMethod.class);
    }

    public static void main(String[] args) {
        String[] testCaseName = { TestGetMethod.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }

    public void testGetSlashWithoutDisk() throws IOException {
        //LogUtils.logAll();
        URL url = new URL("http://" + _host + ":" + _port + "/");
        int response = 0;

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        String data = HttpTestBase.getReply(urlCon);
        assertTrue("No data returned.", (data.length() > 0));
    }

    /**
     * NO recycle now
     *
     * public void testRecycle() { HttpClient client = new HttpClient();
     * client.startSession(TestEnv.HOST, TestEnv.PORT);
     *
     * GetMethod method = new GetMethod("/"); method.setUseDisk(false); try {
     * client.executeMethod(method); } catch (Throwable t) {
     * t.printStackTrace(); fail("Unable to execute method : " + t.toString()); }
     *
     * try { String data = method.getResponseBodyAsString(); assertTrue("No data
     * returned.",(data.length() > 0)); } catch (Throwable t) {
     * t.printStackTrace(); fail("Unable to execute method : " + t.toString()); }
     * assertEquals(200,method.getStatusCode());
     *
     * method.recycle(); method.setPath("/");
     *
     * try { client.executeMethod(method); } catch (Throwable t) {
     * t.printStackTrace(); fail("Unable to execute method : " + t.toString()); }
     *
     * try { String data = method.getResponseBodyAsString(); assertTrue("No data
     * returned.",(data.length() > 0)); } catch (Throwable t) {
     * t.printStackTrace(); fail("Unable to execute method : " + t.toString()); }
     * assertEquals(200,method.getStatusCode()); }
     */
    public void test404() throws IOException {
        int response = 0;

        URL url = new URL("http://" + _host + ":" + _port +
                "/notpresent/really/not");

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(404, response);

        checkNoActiveConns(url);
    }

    public void allTestMethods() throws Exception {
        test404();
        testGetSlashWithoutDisk();
    }
}
