/*
 * $Header: $ $Revision: $ $Date: $
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

package com.oaklandsw.http.webserver;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.TestBase;
import com.oaklandsw.http.TestEnv;

/**
 * 
 * Simple tests of {@link TraceMethod}hitting a local HTTP server.
 * <p>
 * This test suite assumes a webserver is running on port 8080 on the 127.0.0.1
 * (localhost) host. It further assumes that this webserver will respond to an
 * HTTP TRACE of <tt>/</tt> with a 200 response.
 * <p>
 * You can change the assumed port by setting the "httpclient.test.localPort"
 * property. You can change the assumed host by setting the
 * "httpclient.test.localHost" property.
 * 
 * @author Sean C. Sullivan
 * 
 * @version $Id: TestGetMethod.java,v 1.3 2002/02/04 15:26:43 dion Exp $
 * 
 */
public class TestTraceMethod extends TestBase
{

    private static String _host = TestEnv.TEST_WEBSERVER_HOST;

    private static int    _port = TestEnv.TEST_WEBSERVER_PORT;

    public TestTraceMethod(String testName)
    {
        super(testName);
        _doProxyTest = true;
        _do10ProxyTest = true;
        _doExplicitTest = true;
    }

    public static void main(String args[])
    {
        String[] testCaseName = { TestTraceMethod.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }

    public static Test suite()
    {
        return new TestSuite(TestTraceMethod.class);
    }

    public void testExecute() throws IOException
    {
        URL url = new URL("http://" + _host + ":" + _port + "/");
        int response = 0;

        final String strTestHeaderName = "MyTestHeader";
        final String strTestHeaderValue = "This-is-a-test-value.";

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("TRACE");
        urlCon.setRequestProperty(strTestHeaderName, strTestHeaderValue);
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        assertEquals(strTestHeaderValue, urlCon
                .getRequestProperty(strTestHeaderName));

        //
        // note: the reason that we convert the String's to lowercase is
        // because some HTTP servers send a response body that contains
        // lower request headers
        //
        final String strResponseBody_lowercase = TestBase.getReply(urlCon)
                .toLowerCase();
        assertNotNull(strResponseBody_lowercase);
        assertTrue(strResponseBody_lowercase.length() > 0);

        assertTrue(strResponseBody_lowercase.indexOf(strTestHeaderName
                .toLowerCase()) != -1);
        assertTrue(strResponseBody_lowercase.indexOf(strTestHeaderValue
                .toLowerCase()) != -1);
        checkNoActiveConns(url);
    }

    public void allTestMethods() throws Exception
    {
        testExecute();
    }

}
