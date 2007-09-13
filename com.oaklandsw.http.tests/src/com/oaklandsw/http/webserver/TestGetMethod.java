/*
 * $Header:
 * /home/cvspublic/jakarta-commons/httpclient/src/test/org/apache/commons/httpclient/TestGetMethodLocal.java,v
 * 1.3 2002/02/04 15:26:43 dion Exp $ $Revision: 1.3 $ $Date: 2002/02/04
 * 15:26:43 $
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
import java.net.URL;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.HttpTestBase;
import com.oaklandsw.http.HttpTestEnv;
import com.oaklandsw.http.HttpURLConnection;

/**
 * Simple tests of {@link GetMethod}hitting a local webserver.
 * <p>
 * This test suite assumes a webserver is running on port 8080 on the 127.0.0.1
 * (localhost) host. It further assumes that this webserver will respond to an
 * HTTP GET of <tt>/</tt> with a 200 response.
 * <p>
 * You can change the assumed port by setting the "httpclient.test.localPort"
 * property. You can change the assumed host by setting the
 * "httpclient.test.localHost" property.
 * 
 * @author Rodney Waldhoff
 * @version $Id: TestGetMethod.java,v 1.3 2002/02/04 15:26:43 dion Exp $
 */
public class TestGetMethod extends HttpTestBase
{

    String _host = HttpTestEnv.TEST_WEBSERVER_HOST;

    int    _port = HttpTestEnv.TEST_WEBSERVER_PORT;

    public TestGetMethod(String testName)
    {
        super(testName);
        _doProxyTest = true;
        _do10ProxyTest = true;
        _doExplicitTest = true;
    }

    public static Test suite()
    {
        return new TestSuite(TestGetMethod.class);
    }

    public static void main(String args[])
    {
        String[] testCaseName = { TestGetMethod.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }

    public void testGetSlashWithoutDisk() throws IOException
    {
        //LogUtils.logAll();
        URL url = new URL("http://" + _host + ":" + _port + "/");
        int response = 0;

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        String data = HttpTestBase.getReply(urlCon);
        assertTrue("No data returned.", (data.length() > 0));
    }

    /**
     * NO recycle now
     * 
     * public void testRecycle() { HttpClient client = new HttpClient();
     * client.startSession(TestEnv.HOST, TestEnv.PORT);
     * 
     * GetMethod method = new GetMethod("/"); method.setUseDisk(false); try {
     * client.executeMethod(method); } catch (Throwable t) {
     * t.printStackTrace(); fail("Unable to execute method : " + t.toString()); }
     * 
     * try { String data = method.getResponseBodyAsString(); assertTrue("No data
     * returned.",(data.length() > 0)); } catch (Throwable t) {
     * t.printStackTrace(); fail("Unable to execute method : " + t.toString()); }
     * assertEquals(200,method.getStatusCode());
     * 
     * method.recycle(); method.setPath("/");
     * 
     * try { client.executeMethod(method); } catch (Throwable t) {
     * t.printStackTrace(); fail("Unable to execute method : " + t.toString()); }
     * 
     * try { String data = method.getResponseBodyAsString(); assertTrue("No data
     * returned.",(data.length() > 0)); } catch (Throwable t) {
     * t.printStackTrace(); fail("Unable to execute method : " + t.toString()); }
     * assertEquals(200,method.getStatusCode()); }
     */

    public void test404() throws IOException
    {
        int response = 0;

        URL url = new URL("http://"
            + _host
            + ":"
            + _port
            + "/notpresent/really/not");

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(404, response);

        checkNoActiveConns(url);
    }

    public void allTestMethods() throws Exception
    {
        test404();
        testGetSlashWithoutDisk();
    }

}
