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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.TestBase;

/**
 * Simple tests for the HTTP client hitting an external webserver.
 * 
 * This test suite assumes you have an internet connection that can communicate
 * with http://java.sun.com/.
 * 
 * @author Remy Maucherat
 * @author Rodney Waldhoff
 * @author Ortwin Gl�ck
 * @version $Id: TestMethods.java,v 1.5 2002/09/23 13:48:49 jericho Exp $
 */
public class TestMethods extends TestBase
{

    // -------------------------------------------------------------- Constants

    private static final String externalHost = "java.sun.com";

    // private final String PROXY_HOST =
    // System.getProperty("httpclient.test.proxyHost");
    // private final String PROXY_PORT =
    // System.getProperty("httpclient.test.proxyPort");
    // private final String PROXY_USER =
    // System.getProperty("httpclient.test.proxyUser");
    // private final String PROXY_PASS =
    // System.getProperty("httpclient.test.proxyPass");

    public TestMethods(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        return new TestSuite(TestMethods.class);
    }

    public static void main(String args[])
    {
        mainRun(suite(), args);
    }

    public void testMethodsOptionsExternal() throws IOException
    {
        URL url = new URL("http://" + externalHost);
        int response = 0;

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("OPTIONS");
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        String data = urlCon.getHeaderField("allow");
        assertTrue("No data returned.", (data.length() > 0));
    }

    public void testMethodsGetExternal() throws Exception
    {
        HttpURLConnection urlCon = TestBase.doGetLikeMethod("http://"
            + externalHost, "GET", null, false);

        String data = TestBase.getReply(urlCon);
        assertTrue("No data returned.", (data.length() > 0));
    }

    public void testMethodsHeadExternal() throws Exception
    {
        TestBase.doGetLikeMethod("http://" + externalHost, "HEAD", null, false);

    }

    /**
     * This test proves that bad urls throw an IOException, and not some other
     * throwable like a NullPointerException.
     */

    public void testIOException()
    {

        try
        {
            URL url = new URL("http://ggttll.wwwsss.ddeeddss");
            int response = 0;
            HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
            urlCon.setRequestMethod("GET");
            urlCon.connect();
            response = urlCon.getResponseCode();
            if (response >= 400)
                return;
        }
        catch (IOException e)
        {
            return; // IOException and HttpException are ok
        }

        fail("Should have thrown an exception");

    }

}
