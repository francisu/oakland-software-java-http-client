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
 *
 */
package com.oaklandsw.http.webext;

import com.oaklandsw.http.HttpTestBase;
import com.oaklandsw.http.HttpTestEnv;
import com.oaklandsw.http.HttpURLConnection;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.IOException;

import java.net.URL;


/**
 * Simple tests for the HTTP client hitting an external webserver.
 */
public class TestMethods extends HttpTestBase {
    public TestMethods(String testName) {
        super(testName);

        _doSocksProxyTest = true;
    }

    public static Test suite() {
        return new TestSuite(TestMethods.class);
    }

    public static void main(String[] args) {
        mainRun(suite(), args);
    }

    public void testMethodsOptionsExternal() throws IOException {
        URL url = new URL("http://" +
                HttpTestEnv.TEST_WEBEXT_EXTERNAL_OPTIONS_HOST);
        int response = 0;

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("OPTIONS");
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        String data = urlCon.getHeaderField("allow");
        assertTrue("No data returned.", (data.length() > 0));
    }

    public void testMethodsGetExternal() throws Exception {
        URL url = new URL("http://" + HttpTestEnv.TEST_WEBEXT_EXTERNAL_HOST);

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);

        String data = HttpTestBase.getReply(urlCon);
        assertTrue("No data returned.", (data.length() > 0));
    }

    public void testMethodsHeadExternal() throws Exception {
        HttpTestBase.doGetLikeMethod("http://" +
            HttpTestEnv.TEST_WEBEXT_EXTERNAL_HOST, "HEAD", null, false);
    }

    /**
     * This test proves that bad urls throw an IOException, and not some other
     * throwable like a NullPointerException.
     */
    public void testIOException() {
        try {
            URL url = new URL("http://ggttll.wwwsss.ddeeddss");
            int response = 0;
            HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
            urlCon.setRequestMethod("GET");
            urlCon.connect();
            response = urlCon.getResponseCode();

            if (response >= 400) {
                return;
            }
        } catch (IOException e) {
            return; // IOException and HttpException are ok
        }

        fail("Should have thrown an exception");
    }

    public void allTestMethods() throws Exception {
        testMethodsGetExternal();
    }
}
