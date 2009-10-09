/*
 * $Header:
 * /home/cvspublic/jakarta-commons/httpclient/src/test/org/apache/commons/httpclient/TestRequestHeaders.java,v
 * 1.2 2002/08/06 19:47:29 jsdever Exp $ $Revision: 1.2 $ $Date: 2002/08/06
 * 19:47:29 $
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999-2002 The Apache Software Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. The end-user documentation included with the redistribution, if any, must
 * include the following acknowlegement: "This product includes software
 * developed by the Apache Software Foundation (http://www.apache.org/)."
 * Alternately, this acknowlegement may appear in the software itself, if and
 * wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
 * Foundation" must not be used to endorse or promote products derived from this
 * software without prior written permission. For written permission, please
 * contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache" nor may
 * "Apache" appear in their names without prior written permission of the Apache
 * Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE APACHE
 * SOFTWARE FOUNDATION OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many individuals on
 * behalf of the Apache Software Foundation. For more information on the Apache
 * Software Foundation, please see <http://www.apache.org/>.
 *
 * [Additional notices, if required by prior licensing conditions]
 *
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
