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

package com.oaklandsw.http.webserver;

import java.net.URL;

import com.oaklandsw.http.HttpConnection;
import com.oaklandsw.http.HttpConnectionManager;
import com.oaklandsw.http.HttpTestBase;
import com.oaklandsw.http.HttpTestEnv;
import com.oaklandsw.http.HttpURLConnection;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * 
 * Unit tests for {@link HttpConnectionManager}. These tests do not require any
 * network connection or web app.
 * 
 * @author Marc A. Saegesser
 */
public class TestConnectionManagerWebserver extends HttpTestBase
{

    protected HttpConnectionManager _connManager;

    public TestConnectionManagerWebserver(String testName)
    {
        super(testName);
    }

    public static void main(String args[])
    {
        String[] testCaseName = { TestConnectionManagerWebserver.class
                .getName() };
        junit.textui.TestRunner.main(testCaseName);
    }

    public static Test suite()
    {
        return new TestSuite(TestConnectionManagerWebserver.class);
    }

    public void setUp()
    {
        _connManager = HttpURLConnection.getConnectionManager();
    }

    public void testGetMultipleConnections() throws Exception
    {
        String urlStr = "http://localhost/path/path?query=string";
        URL url = new URL(urlStr);
        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);

        // Create a new connection
        HttpConnection conn1 = _connManager.getConnection(urlCon);
        conn1.open();
        // Release the connection
        _connManager.releaseConnection(conn1);

        // Get the same connection again
        HttpConnection conn2 = _connManager.getConnection(urlCon);
        assertEquals("Same connection", conn1, conn2);
        // don't release yet

        // Get another new connection
        HttpConnection conn3 = _connManager.getConnection(urlCon);
        assertTrue(conn2 != conn3);

        _connManager.releaseConnection(conn2);
        _connManager.releaseConnection(conn3);

        // Clean up
        conn1 = _connManager.getConnection(urlCon);
        conn1.close();
        _connManager.releaseConnection(conn1);

        checkNoActiveConns(url);
    }

    public void testMultiConnectionProxy() throws Exception
    {
        URL url;
        HttpURLConnection urlCon;

        String urlStr = HttpTestEnv.TEST_URL_WEBSERVER
            + "/path/path?query=string";

        // Create a new connection
        url = new URL(urlStr);
        urlCon = HttpURLConnection.openConnection(url);
        urlCon.connect();
        HttpConnection conn1 = urlCon.getConnection();
        _connManager.releaseConnection(conn1);

        // New connection for a proxy
        url = new URL(urlStr);
        urlCon = HttpURLConnection.openConnection(url);
        urlCon.setConnectionProxyHost(HttpTestEnv.APACHE_HOST);
        urlCon.setConnectionProxyPort(HttpTestEnv.TEST_PROXY_PORT);
        urlCon.connect();
        HttpConnection conn2 = urlCon.getConnection();
        assertFalse("proxy same as non-proxy", conn1 == conn2);
        _connManager.releaseConnection(conn2);

        // New connection for another proxy
        url = new URL(urlStr);
        urlCon = HttpURLConnection.openConnection(url);
        urlCon.setConnectionProxyHost(HttpTestEnv.AUTH_PROXY_HOST);
        urlCon.setConnectionProxyPort(HttpTestEnv.TEST_AUTH_PROXY_PORT);
        urlCon.connect();
        HttpConnection conn3 = urlCon.getConnection();
        assertFalse("proxy same as non-proxy", conn3 == conn1);
        assertFalse("proxy same as non-proxy", conn3 == conn2);
        _connManager.releaseConnection(conn3);

        // New connection for another proxy
        url = new URL(urlStr);
        urlCon = HttpURLConnection.openConnection(url);
        urlCon.setConnectionProxyHost(HttpTestEnv.AUTH_PROXY_HOST);
        urlCon.setConnectionProxyPort(HttpTestEnv.TEST_AUTH_PROXY_PORT);
        urlCon.setConnectionProxyUser("xxx");
        urlCon.connect();
        HttpConnection conn4 = urlCon.getConnection();
        _connManager.releaseConnection(conn4);

        // This will will not consider the proxy connections associated
        // with the URL, only the direct connection
        assertEquals(0, getActiveConns(url));
        assertEquals(1, getTotalConns(url));
    }

}
