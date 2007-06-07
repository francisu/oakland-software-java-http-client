/*
 * $Header:
 * /home/cvspublic/jakarta-commons/httpclient/src/test/org/apache/commons/httpclient/TestMethodsNoHost.java,v
 * 1.7 2002/08/06 15:15:32 jsdever Exp $ $Revision: 1.7 $ $Date: 2002/08/06
 * 15:15:32 $
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

import java.net.URL;

import org.apache.commons.logging.Log;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.HttpURLConnection;
import com.oaklandsw.http.SimpleHttpMethod;
import com.oaklandsw.http.HttpTestBase;
import com.oaklandsw.util.LogUtils;

/**
 * @author Rodney Waldhoff
 * @author <a href="mailto:jsdever@apache.org">Jeff Dever </a>
 * @version $Revision: 1.7 $ $Date: 2002/08/06 15:15:32 $
 */
public class TestMethodsNoHost extends HttpTestBase
{
    private static final Log _log = LogUtils.makeLogger();

    public TestMethodsNoHost(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        return new TestSuite(TestMethodsNoHost.class);
    }

    public void testHttpMethodBasePaths() throws Exception
    {
        SimpleHttpMethod simple = new SimpleHttpMethod();
        String[] paths = { "/some/absolute/path", "../some/relative/path", "/",
            "/some/path/with?query=string" };

        for (int i = 0; i < paths.length; i++)
        {
            simple.setPathQuery(paths[i]);
            assertEquals(paths[i], simple.getPathQuery());
        }
    }

    public void testHttpMethodBaseDefaultPath() throws Exception
    {
        SimpleHttpMethod simple = new SimpleHttpMethod();
        assertEquals("/", simple.getPath());

        simple.setPathQuery("");
        assertEquals("/", simple.getPath());

        simple.setPathQuery(null);
        assertEquals("/", simple.getPath());
    }

    public void testHttpMethodBasePathConstructor() throws Exception
    {
        SimpleHttpMethod simple = new SimpleHttpMethod();
        assertEquals("/", simple.getPath());

        simple = new SimpleHttpMethod();
        simple.setPathQuery("");
        assertEquals("/", simple.getPath());

        simple = new SimpleHttpMethod();
        simple.setPathQuery("/some/path/");
        assertEquals("/some/path/", simple.getPath());
    }

    // Leaks connections if there is a failure to connect
    public void testBug385() throws Exception
    {
        // Bad location
        URL url = new URL("http://localhost:55555");

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("GET");
        try
        {
            // Should fail
            urlCon.connect();
            fail("Did not get expected exception");
        }
        catch (Exception ex)
        {
            // Expected
        }

        // Should be no connections
        checkNoTotalConns(url);
        // com.oaklandsw.http.HttpURLConnection.dumpConnectionPool();
    }

    public void testStreamBadChunkSize() throws Exception
    {
        URL url = new URL("http://doesnotmatter");

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();

        // These should all work
        urlCon.setChunkedStreamingMode(-1);
        urlCon.setChunkedStreamingMode(0);
        urlCon.setChunkedStreamingMode(25);
    }

    public void testStreamBadFixedSize() throws Exception
    {
        URL url = new URL("http://doesnotmatter");

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();

        try
        {
            urlCon.setFixedLengthStreamingMode(-1);
            fail("Expected exception");
        }
        catch (IllegalArgumentException ex)
        {
            // OK
        }

        urlCon.setFixedLengthStreamingMode(0);
        urlCon.setFixedLengthStreamingMode(100);
    }

}
