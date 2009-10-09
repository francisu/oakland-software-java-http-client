/*
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation. All rights reserved.
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
 */
package com.oaklandsw.http.local;

import com.oaklandsw.util.URIUtil;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


/**
 *
 * Unit tests for {@link URIUtil}. These tests care currently quite limited and
 * should be expanded to test more functionality.
 *
 * @author Marc A. Saegesser
 * @version $Id: TestURIUtil.java,v 1.1 2002/04/12 21:17:06 marcsaeg Exp $
 */
public class TestURIUtil extends TestCase {
    // ----------------------------------------------------- Instance Variables
    URITestCase[] pathTests = {
            new URITestCase("http://www.server.com/path1/path2", "/path1/path2"),
            new URITestCase("http://www.server.com/path1/path2/",
                "/path1/path2/"),
            new URITestCase("http://www.server.com/path1/path2?query=string",
                "/path1/path2"),
            new URITestCase("http://www.server.com/path1/path2/?query=string",
                "/path1/path2/"),
            new URITestCase("www.noscheme.com/path1/path2", "/path1/path2"),
            new URITestCase("www.noscheme.com/path1/path2#anchor?query=string",
                "/path1/path2"),
            new URITestCase("/noscheme/nohost/path", "/noscheme/nohost/path"),
            new URITestCase("http://www.server.com", "/"),
            new URITestCase("https://www.server.com:443/ssl/path", "/ssl/path"),
            new URITestCase("https://[1:2:3]:443/ssl/path", "/ssl/path"),
            new URITestCase("https://[1:2:3]/ssl/path", "/ssl/path"),
            new URITestCase("http://www.server.com:8080/path/with/port",
                "/path/with/port")
        };
    URITestCase[] queryTests = {
            new URITestCase("http://www.server.com/path1/path2", null),
            new URITestCase("http://www.server.com/path1/path2?query=string",
                "query=string"),
            new URITestCase("http://www.server.com/path1/path2/?query=string",
                "query=string"),
            new URITestCase("www.noscheme.com/path1/path2#anchor?query=string",
                "query=string"),
            new URITestCase("/noscheme/nohost/path?query1=string1&query2=string2",
                "query1=string1&query2=string2"),
            new URITestCase("https://www.server.com:443/ssl/path?query1=string1&query2=string2",
                "query1=string1&query2=string2"),
            new URITestCase("https://[1:2:3]:443/ssl/path?query1=string1&query2=string2",
                "query1=string1&query2=string2"),
            new URITestCase("https://[1:2:3]/ssl/path?query1=string1&query2=string2",
                "query1=string1&query2=string2"),
            new URITestCase("http://www.server.com:8080/path/with/port?query1=string1&query2=string2",
                "query1=string1&query2=string2")
        };
    URITestCase[] hostPortTests = {
            new URITestCase("http://www.server.com/path1/path2",
                "www.server.com"),
            new URITestCase("http://www.server.com:80/path1/path2",
                "www.server.com:80"),
            new URITestCase("http://www.server.com/path1/path2?query=string",
                "www.server.com"),
            new URITestCase("http://www.server.com/path1/path2/?query=string",
                "www.server.com"),
            new URITestCase("www.noscheme.com/path1/path2#anchor?query=string",
                "www.noscheme.com"),
            new URITestCase("www.noscheme.com:443/path1/path2#anchor?query=string",
                "www.noscheme.com:443"),
            new URITestCase("/noscheme/nohost/path?query1=string1", null),
            new URITestCase("https://www.server.com:443/ssl/path",
                "www.server.com:443"),
            new URITestCase("http://www.server.com:8080/path/with/port",
                "www.server.com:8080"),
            new URITestCase("http://www.server.com:8080#fragment",
                "www.server.com:8080"),
            new URITestCase("http://www.server.com#fragment", "www.server.com"),
            new URITestCase("http://repoman#fragment", "repoman"),
            new URITestCase("http://repoman?query=value", "repoman"),
            new URITestCase("http://www.server.com:8080?query=value",
                "www.server.com:8080"),
            new URITestCase("http://www.server.com:8080/path?query=value",
                "www.server.com:8080"),
            new URITestCase("http://www.server.com:8080/path/?query=value",
                "www.server.com:8080"),
            new URITestCase("http://www.server.com?query=value",
                "www.server.com"),
            new URITestCase("http://[1:2:3]:8080", "[1:2:3]:8080"),
            new URITestCase("http://[1]:8080", "[1]:8080"),
            new URITestCase("http://[:1122]:8080", "[:1122]:8080"),
            new URITestCase("http://www.server.com?query=value/",
                "www.server.com")
        };
    URITestCase[] protocolTests = {
            new URITestCase("http://www.server.com/path1/path2", "http"),
            new URITestCase("https://www.server.com:80/path1/path2", "https"),
            new URITestCase("ftp://www.server.com/path1/path2?query=string",
                "ftp"),
            new URITestCase("www.noscheme.com/path1/path2#anchor?query=string",
                null)
        };
    URITestCase[] protocolHostPortTests = {
            new URITestCase("http://www.server.com/path1/path2",
                "http://www.server.com"),
            new URITestCase("https://www.server.com:80/path1/path2",
                "https://www.server.com:80"),
            new URITestCase("ftp://www.server.com/path1/path2?query=string",
                "ftp://www.server.com"),
            new URITestCase("www.noscheme.com/path1/path2#anchor?query=string",
                null),
            new URITestCase("http://www.server.com:8080#fragment",
                "http://www.server.com:8080"),
            new URITestCase("http://www.server.com#fragment",
                "http://www.server.com"),
            new URITestCase("http://repoman#fragment", "http://repoman"),
            new URITestCase("http://repoman?query=value", "http://repoman"),
            new URITestCase("http://www.server.com:8080?query=value",
                "http://www.server.com:8080"),
            new URITestCase("http://www.server.com:8080/path?query=value",
                "http://www.server.com:8080"),
            new URITestCase("http://www.server.com:8080/path/?query=value",
                "http://www.server.com:8080"),
            new URITestCase("http://www.server.com?query=value",
                "http://www.server.com"),
            new URITestCase("http://[1:2:3]:8080", "http://[1:2:3]:8080"),
            new URITestCase("http://[1]:8080", "http://[1]:8080"),
            new URITestCase("http://[:1122]:8080", "http://[:1122]:8080"),
            new URITestCase("http://www.server.com?query=value/",
                "http://www.server.com")
        };

    public TestURIUtil(String testName) {
        super(testName);
    }

    public static void main(String[] args) {
        String[] testCaseName = { TestURIUtil.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }

    public static Test suite() {
        return new TestSuite(TestURIUtil.class);
    }

    public void testGetPath() {
        String testValue = "";
        String expectedResult = "";

        for (int i = 0; i < pathTests.length; i++) {
            testValue = pathTests[i].getTestValue();
            expectedResult = pathTests[i].getExpectedResult();
            assertEquals("Path test", expectedResult, URIUtil.getPath(testValue));
        }
    }

    public void testGetQueryString() {
        String testValue = "";
        String expectedResult = "";

        for (int i = 0; i < queryTests.length; i++) {
            testValue = queryTests[i].getTestValue();
            expectedResult = queryTests[i].getExpectedResult();
            assertEquals("Query test", expectedResult,
                URIUtil.getQuery(testValue));
        }
    }

    public void testGetHostPort() {
        String testValue = "";
        String expectedResult = "";

        for (int i = 0; i < hostPortTests.length; i++) {
            testValue = hostPortTests[i].getTestValue();
            expectedResult = hostPortTests[i].getExpectedResult();
            assertEquals("Host/port test", expectedResult,
                URIUtil.getHostPort(testValue));
        }
    }

    public void testGetProcotol() {
        String testValue = "";
        String expectedResult = "";

        for (int i = 0; i < protocolTests.length; i++) {
            testValue = protocolTests[i].getTestValue();
            expectedResult = protocolTests[i].getExpectedResult();
            assertEquals("Protocol test", expectedResult,
                URIUtil.getProtocol(testValue));
        }
    }

    public void testGetProcotolHostPort() {
        String testValue = "";
        String expectedResult = "";

        for (int i = 0; i < protocolHostPortTests.length; i++) {
            testValue = protocolHostPortTests[i].getTestValue();
            expectedResult = protocolHostPortTests[i].getExpectedResult();
            assertEquals("Protocol test", expectedResult,
                URIUtil.getProtocolHostPort(testValue));
        }
    }

    private class URITestCase {
        private String testValue;
        private String expectedResult;

        public URITestCase(String tv, String er) {
            this.testValue = tv;
            this.expectedResult = er;
        }

        public String getTestValue() {
            return testValue;
        }

        public String getExpectedResult() {
            return expectedResult;
        }
    }
}
