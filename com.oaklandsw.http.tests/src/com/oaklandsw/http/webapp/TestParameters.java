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
import com.oaklandsw.http.servlet.ParamServlet;

import com.oaklandsw.utillog.Log;
import com.oaklandsw.util.LogUtils;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.OutputStream;

import java.net.URL;


public class TestParameters extends TestWebappBase {
    private static final Log _log = LogUtils.makeLogger();

    public TestParameters(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(TestParameters.class);

        return suite;
    }

    public static void main(String[] args) {
        mainRun(suite(), args);
    }

    public void testGetMethodQueryString() throws Exception {
        URL url = new URL(_urlBase + ParamServlet.NAME +
                "?hadQuestionMark=true");
        int response = 0;

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        String reply = getReply(urlCon);
        assertTrue(checkReplyNoAssert(reply, "<title>Param Servlet: GET</title>"));
        assertTrue(checkReplyNoAssert(reply,
                "<p>QueryString=\"hadQuestionMark=true\"</p>"));

        checkNoActiveConns(url);
    }

    public void testGetMethodEmptyQueryString() throws Exception {
        URL url = new URL(_urlBase + ParamServlet.NAME + "?");
        int response = 0;

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        String reply = getReply(urlCon);
        assertTrue(checkReplyNoAssert(reply, "<title>Param Servlet: GET</title>"));

        // This might fail when being tested with Tomcat 3.2, but since we are
        // not
        // testing with that now, don't worry.
        // if (tomcat32)
        // assertTrue(checkReplyNoAssert(reply, "<p>QueryString=\"\"</p>"));
        // else
        assertTrue(checkReplyNoAssert(reply, "<p>QueryString=null</p>"));

        checkNoActiveConns(url);
    }

    public void testGetMethodNoQueryString() throws Exception {
        URL url = new URL(_urlBase + ParamServlet.NAME);
        int response = 0;

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        String reply = getReply(urlCon);
        assertTrue(checkReplyNoAssert(reply, "<title>Param Servlet: GET</title>"));
        assertTrue(checkReplyNoAssert(reply, "<p>QueryString=null</p>"));

        checkNoActiveConns(url);
    }

    public void testGetMethodQueryString2() throws Exception {
        URL url = new URL(_urlBase + ParamServlet.NAME +
                "?hadQuestionMark=false");
        int response = 0;

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        String reply = getReply(urlCon);
        assertTrue(checkReplyNoAssert(reply, "<title>Param Servlet: GET</title>"));
        assertTrue(checkReplyNoAssert(reply,
                "<p>QueryString=\"hadQuestionMark=false\"</p>"));
        checkNoActiveConns(url);
    }

    public void testGetMethodParameters() throws Exception {
        URL url = new URL(_urlBase + ParamServlet.NAME +
                "?param-one=param-value");
        int response = 0;

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        String reply = getReply(urlCon);
        assertTrue(checkReplyNoAssert(reply, "<title>Param Servlet: GET</title>"));
        assertTrue(checkReplyNoAssert(reply,
                "<p>QueryString=\"param-one=param-value\"</p>"));
        assertTrue(checkReplyNoAssert(reply,
                "<p>Parameters</p>\r\n" +
                "name=\"param-one\";value=\"param-value\"<br>"));
        checkNoActiveConns(url);
    }

    public void testPostMethodParameters() throws Exception {
        URL url = new URL(_urlBase + ParamServlet.NAME);
        int response = 0;

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("POST");

        urlCon.setDoOutput(true);

        OutputStream os = urlCon.getOutputStream();
        os.write("param-one=param-value".getBytes("ASCII"));

        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        String reply = getReply(urlCon);
        assertTrue(checkReplyNoAssert(reply,
                "<title>Param Servlet: POST</title>"));
        assertTrue(checkReplyNoAssert(reply,
                "<p>Parameters</p>\r\n" +
                "name=\"param-one\";value=\"param-value\"<br>"));
        checkNoActiveConns(url);
    }

    public void testGetMethodMultiParameters() throws Exception {
        URL url = new URL(_urlBase + ParamServlet.NAME +
                "?param-one=param-value&param-two=param-value2" +
                "&special-chars=:/?~.");
        int response = 0;

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        String reply = getReply(urlCon);
        assertTrue(checkReplyNoAssert(reply, "<title>Param Servlet: GET</title>"));
        assertTrue(checkReplyNoAssert(reply,
                "name=\"special-chars\";value=\":/?~.\""));
        assertTrue(checkReplyNoAssert(reply,
                "name=\"param-one\";value=\"param-value\""));
        assertTrue(checkReplyNoAssert(reply,
                "name=\"param-two\";value=\"param-value2\""));
        checkNoActiveConns(url);
    }

    public void testPostMethodMultiParameters() throws Exception {
        URL url = new URL(_urlBase + ParamServlet.NAME);
        int response = 0;

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("POST");

        urlCon.setDoOutput(true);

        OutputStream os = urlCon.getOutputStream();
        os.write(("param-one=param-value&param-two=param-value2" +
            "&special-chars=:/?~.").getBytes("ASCII"));

        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        String reply = getReply(urlCon);
        assertTrue(checkReplyNoAssert(reply,
                "<title>Param Servlet: POST</title>"));
        /***********************************************************************
         * assertTrue(checkReplyNoAssert(reply, "
         * <p>
         * Request Body
         * </p>
         * \r\n" + "param-one=param-value&param-two=param-value2" +
         * "&special-chars=:/?~."));
         **********************************************************************/
        assertTrue(checkReplyNoAssert(reply,
                "name=\"special-chars\";value=\":/?~.\""));
        assertTrue(checkReplyNoAssert(reply,
                "name=\"param-one\";value=\"param-value\""));
        assertTrue(checkReplyNoAssert(reply,
                "name=\"param-two\";value=\"param-value2\""));
        checkNoActiveConns(url);
    }

    public void testGetMethodParameterWithoutValue() throws Exception {
        URL url = new URL(_urlBase + ParamServlet.NAME +
                "?param-without-value=");
        int response = 0;

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        String reply = getReply(urlCon);
        assertTrue(checkReplyNoAssert(reply, "<title>Param Servlet: GET</title>"));
        assertTrue(checkReplyNoAssert(reply,
                "<p>QueryString=\"param-without-value=\"</p>"));
        checkNoActiveConns(url);
    }

    public void testGetMethodParameterAppearsTwice() throws Exception {
        URL url = new URL(_urlBase + ParamServlet.NAME + "?foo=one&foo=two");
        int response = 0;

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        String reply = getReply(urlCon);
        assertTrue(checkReplyNoAssert(reply, "<title>Param Servlet: GET</title>"));
        assertTrue(checkReplyNoAssert(reply, "name=\"foo\";value=\"one\""));
        assertTrue(checkReplyNoAssert(reply, "name=\"foo\";value=\"two\""));
        checkNoActiveConns(url);
    }

    // Bug 973
    public void testGetMethodNoSlashParameters() throws Exception {
        URL url = new URL("http://" + HttpTestEnv.TOMCAT_HOST + "?foo=one");
        int response = 0;

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);
    }

    public void testGetMethodSlashParameters() throws Exception {
        URL url = new URL("http://" + HttpTestEnv.TOMCAT_HOST + "/?foo=one");
        int response = 0;

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);
    }

    public void allTestMethods() throws Exception {
        testGetMethodQueryString();
        testGetMethodEmptyQueryString();
        testGetMethodNoQueryString();
        testGetMethodQueryString2();
        testGetMethodParameters();
        testPostMethodParameters();
        testGetMethodMultiParameters();
        testPostMethodMultiParameters();
        testGetMethodParameterWithoutValue();
        testGetMethodParameterAppearsTwice();
    }

    /***************************************************************************
     * This cannot be done with the URLConnection interface public void
     * testGetMethodOverwriteQueryString() throws Exception { HttpClient client =
     * new HttpClient(); client.startSession(host, port); GetMethod method = new
     * GetMethod("/" + context + "/params");
     * method.setQueryString("query=string"); method.setQueryString(new
     * NameValuePair[] { new NameValuePair("param","eter"), new
     * NameValuePair("para","meter") }); method.setUseDisk(false); try {
     * client.executeMethod(method); } catch (Throwable t) {
     * t.printStackTrace(); fail("Unable to execute method : " + t.toString()); }
     * assertTrue(method.getResponseBodyAsString().indexOf(" <title>Param
     * Servlet: GET </title>") >= 0); assertEquals(200,method.getStatusCode());
     * assertTrue(method.getResponseBodyAsString().indexOf("name=\"query\";value=\"string\"") ==
     * -1);
     * assertTrue(method.getResponseBodyAsString().indexOf("name=\"param\";value=\"eter\"") >=
     * 0);
     * assertTrue(method.getResponseBodyAsString().indexOf("name=\"para\";value=\"meter\"") >=
     * 0); }
     *
     */

    /***************************************************************************
     * public void testPostMethodParameterAndQueryString() throws Exception {
     * HttpClient client = new HttpClient(); client.startSession(host, port);
     * PostMethod method = new PostMethod("/" + context + "/params");
     * method.setQueryString("query=string");
     * method.addParameter("param","eter"); method.addParameter("para","meter");
     * method.setUseDisk(false); try { client.executeMethod(method); } catch
     * (Throwable t) { t.printStackTrace(); fail("Unable to execute method : " +
     * t.toString()); } assertTrue(method.getResponseBodyAsString().indexOf("
     * <title>Param Servlet: POST </title>") >= 0);
     * assertEquals(200,method.getStatusCode());
     * assertTrue(method.getResponseBodyAsString().indexOf("
     * <p>
     * QueryString=\"query=string\"
     * </p>") >= 0);
     * assertTrue(method.getResponseBodyAsString(),method.getResponseBodyAsString().indexOf("name=\"param\";value=\"eter\"") >=
     * 0);
     * assertTrue(method.getResponseBodyAsString().indexOf("name=\"para\";value=\"meter\"") >=
     * 0); }
     *
     **************************************************************************/
}
