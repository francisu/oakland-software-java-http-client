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

import com.oaklandsw.http.SimpleHttpConnection;
import com.oaklandsw.http.SimpleHttpMethod;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


/**
 * Tests for reading response headers.
 *
 * @author <a href="mailto:dims@apache.org">Davanum Srinivas </a>
 * @author <a href="mailto:jsdever@apache.org">Jeff Dever </a>
 * @version $Id: TestResponseHeaders.java,v 1.3 2002/08/06 19:47:29 jsdever Exp $
 */
public class TestResponseHeaders extends TestCase {
    // ------------------------------------------------------------ Constructor
    public TestResponseHeaders(String testName) {
        super(testName);
    }

    // ------------------------------------------------------------------- Main
    public static void main(String[] args) {
        String[] testCaseName = { TestResponseHeaders.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }

    // ------------------------------------------------------- TestCase Methods
    public static Test suite() {
        return new TestSuite(TestResponseHeaders.class);
    }

    // ----------------------------------------------------------- Test Methods
    public void testHeaders() throws Exception {
        String body = "XXX\r\nYYY\r\nZZZ";
        String headers = "HTTP/1.1 200 OK\r\n" + "Connection: close\r\n" +
            "Content-Length: " + body.length() + "\r\n" +
            "Content-Type: text/xml; charset=utf-8\r\n" +
            "Date: Wed, 28 Mar 2001 05:05:04 GMT\r\n" +
            "Server: UserLand Frontier/7.0-WinNT\r\n";
        SimpleHttpMethod method = new SimpleHttpMethod();
        SimpleHttpConnection conn = new SimpleHttpConnection();
        method.setState(conn);
        conn.addResponse(headers + "\r\n" + body);
        method.getResponseCode();
        assertEquals("close", method.getHeaderField("Connection"));
        assertEquals(body.length(),
            Integer.parseInt(method.getHeaderField("Content-Length")));
        assertEquals("text/xml; charset=utf-8",
            method.getHeaderField("Content-Type"));
        assertEquals("Wed, 28 Mar 2001 05:05:04 GMT",
            method.getHeaderField("Date"));
        assertEquals("UserLand Frontier/7.0-WinNT",
            method.getHeaderField("Server"));
    }

    public void testNullHeaders() throws Exception {
        String body = "XXX\r\nYYY\r\nZZZ";
        String headers = "HTTP/1.1 200 OK\r\n" + "Content-Length: " +
            body.length() + "\r\n";
        SimpleHttpMethod method = new SimpleHttpMethod();
        SimpleHttpConnection conn = new SimpleHttpConnection();
        method.setState(conn);
        conn.addResponse(headers + "\r\n" + body);
        method.getResponseCode();
        assertEquals(null, method.getHeaderField(null));
        assertEquals(null, method.getHeaderField("bogus"));
    }

    public void testFoldedHeaders() throws Exception {
        String body = "XXX\r\nYYY\r\nZZZ";
        String headers = "HTTP/1.1 200 OK\r\n" + "Connection: close\r\n" +
            "Content-Length: " + body.length() + "\r\n" +
            "Content-Type: text/xml; charset=utf-8\r\n" +
            "\tboundary=XXXX\r\n" + "Date: Wed, 28 Mar 2001\r\n" +
            " 05:05:04 GMT\r\n" + "Server: UserLand Frontier/7.0-WinNT\r\n";
        SimpleHttpMethod method = new SimpleHttpMethod();
        SimpleHttpConnection conn = new SimpleHttpConnection();
        method.setState(conn);
        conn.addResponse(headers + "\r\n" + body);
        method.getResponseCode();
        assertEquals("close", method.getHeaderField("Connection"));
        assertEquals(body.length(),
            Integer.parseInt(method.getHeaderField("Content-Length")));
        assertEquals("text/xml; charset=utf-8 boundary=XXXX",
            method.getHeaderField("Content-Type"));
        assertEquals("Wed, 28 Mar 2001 05:05:04 GMT",
            method.getHeaderField("Date"));
        assertEquals("UserLand Frontier/7.0-WinNT",
            method.getHeaderField("Server"));
        assertTrue(method.getHeaderField("Content-Type").indexOf("boundary") != -1);
    }
}
