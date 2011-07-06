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

import com.oaklandsw.http.HttpConnection;
import com.oaklandsw.http.SimpleHttpConnection;
import com.oaklandsw.http.SimpleHttpMethod;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


/**
 * Tests for reading response headers.
 *
 * @author <a href="mailto:jsdever@apache.org">Jeff Dever </a>
 * @version $Id: TestRequestHeaders.java,v 1.2 2002/08/06 19:47:29 jsdever Exp $
 */
public class TestRequestHeaders extends TestCase {
    SimpleHttpMethod method = null;
    HttpConnection conn = null;

    // ------------------------------------------------------------ Constructor
    public TestRequestHeaders(String testName) {
        super(testName);
    }

    // ------------------------------------------------------------------- Main
    public static void main(String[] args) {
        String[] testCaseName = { TestRequestHeaders.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }

    // ------------------------------------------------------- TestCase Methods
    public static Test suite() {
        return new TestSuite(TestRequestHeaders.class);
    }

    public void setUp() {
        method = new SimpleHttpMethod();

        // assign conn in test case
    }

    public void tearDown() {
        method = null;
        conn = null;
    }

    public void testNullHeader() throws Exception {
        conn = new SimpleHttpConnection("some.host.name", 80, false);
        assertEquals(null, method.getRequestProperty(null));
        assertEquals(null, method.getRequestProperty("bogus"));
    }

    public void testHostHeaderPortHTTP80() throws Exception {
        conn = new SimpleHttpConnection("some.host.name", 80, false);
        method.testAddRequestHeaders(conn);
        assertEquals("some.host.name",
            method.getRequestProperty("Host").toString().trim());
    }

    public void testHostHeaderPortHTTP81() throws Exception {
        conn = new SimpleHttpConnection("some.host.name", 81, false);
        method.testAddRequestHeaders(conn);
        assertEquals("some.host.name:81",
            method.getRequestProperty("Host").toString().trim());
    }

    public void testHostHeaderPortHTTPS443() throws Exception {
        conn = new SimpleHttpConnection("some.host.name", 443, true);
        method.testAddRequestHeaders(conn);
        assertEquals("some.host.name",
            method.getRequestProperty("Host").toString().trim());
    }

    public void testHostHeaderPortHTTPS444() throws Exception {
        conn = new SimpleHttpConnection("some.host.name", 444, true);
        method.testAddRequestHeaders(conn);
        assertEquals("some.host.name:444",
            method.getRequestProperty("Host").toString().trim());
    }

    public void testHeadersPreserveCaseKeyIgnoresCase()
        throws Exception {
        method.setRequestProperty("NAME", "VALUE");

        String upHeader = method.getRequestProperty("NAME");
        String loHeader = method.getRequestProperty("name");
        String mixHeader = method.getRequestProperty("nAmE");
        assertEquals("VALUE", upHeader);
        assertEquals("VALUE", loHeader);
        assertEquals("VALUE", mixHeader);
    }
}
