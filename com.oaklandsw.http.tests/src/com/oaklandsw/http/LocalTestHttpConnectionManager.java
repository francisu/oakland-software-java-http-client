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

package com.oaklandsw.http;

import java.net.MalformedURLException;

import com.oaklandsw.http.HttpConnection;
import com.oaklandsw.http.HttpConnectionManager;
import com.oaklandsw.http.HttpException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * 
 * Unit tests for {@link HttpConnectionManager}. These tests do not require any
 * network connection or web app.
 * 
 * @author Marc A. Saegesser
 */
public class LocalTestHttpConnectionManager extends TestCase
{

    public LocalTestHttpConnectionManager(String testName)
    {
        super(testName);
    }

    public static void main(String args[])
    {
        String[] testCaseName = { LocalTestHttpConnectionManager.class
                .getName() };
        junit.textui.TestRunner.main(testCaseName);
    }

    public static Test suite()
    {
        return new TestSuite(LocalTestHttpConnectionManager.class);
    }

    public void tearDown()
    {
        // Back to default
        HttpConnectionManager
                .setMaxConnectionsPerHost(HttpConnectionManager.DEFAULT_MAX_CONNECTIONS);
        HttpConnectionManager.setProxyHost(null);
        HttpConnectionManager.setProxyPort(-1);
    }

    // Test the accessor methods
    public void testProxyHostAccessors()
    {
        HttpConnectionManager.setProxyHost("proxyhost");
        assertEquals("Proxy Host", "proxyhost", HttpConnectionManager
                .getProxyHost());
    }

    public void testProxyPortAccessors()
    {
        HttpConnectionManager.setProxyPort(8888);
        assertEquals("Proxy Port", 8888, HttpConnectionManager.getProxyPort());
    }

    public void testMaxConnectionsAccessors()
    {
        // First test the default value (s/b 2 - don't use the constant)
        assertEquals("Default MaxConnections", 2, HttpConnectionManager
                .getMaxConnectionsPerHost());

        HttpConnectionManager.setMaxConnectionsPerHost(10);
        assertEquals("MaxConnections", 10, HttpConnectionManager
                .getMaxConnectionsPerHost());
    }

    public void testGetConnection()
    {
        try
        {
            // Create a new connection
            HttpConnection conn = HttpConnectionManager
                    .getConnection("http://www.nosuchserver.com/path/path?query=string");
            // Validate the connection properties
            assertEquals("Host", "www.nosuchserver.com", conn.getHost());
            assertEquals("Port", 80, conn.getPort());
            // Release the connection
            HttpConnectionManager.releaseConnection(conn);

            // Create a new connection
            conn = HttpConnectionManager
                    .getConnection("https://www.nosuchserver.com/path/path?query=string");
            // Validate the connection properties
            assertEquals("Host", "www.nosuchserver.com", conn.getHost());
            assertEquals("Port", 443, conn.getPort());
            // Release the connection
            HttpConnectionManager.releaseConnection(conn);

            // Create a new connection
            conn = HttpConnectionManager
                    .getConnection("http://www.nowhere.org:8080/path/path?query=string");
            // Validate the connection properties
            assertEquals("Host", "www.nowhere.org", conn.getHost());
            assertEquals("Port", 8080, conn.getPort());
            // Release the connection
            HttpConnectionManager.releaseConnection(conn);

        }
        catch (MalformedURLException e)
        {
            fail("Caught unexpected MalformedURLException ("
                + e.toString()
                + ")");
        }
        catch (HttpException e)
        {
            fail("Caught unexpected HttpException (" + e.toString() + ")");
        }
    }

}
