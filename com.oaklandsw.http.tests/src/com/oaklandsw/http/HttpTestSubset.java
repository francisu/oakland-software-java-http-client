/*
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

package com.oaklandsw.http;

import com.oaklandsw.TestCaseBase;

import com.oaklandsw.http.local.TestConnectionManagerLocal;
import com.oaklandsw.http.local.TestDefaults;
import com.oaklandsw.http.local.TestHeaders;
import com.oaklandsw.http.local.TestHttpStatus;
import com.oaklandsw.http.local.TestMethodsNoHost;
import com.oaklandsw.http.local.TestNtlmMessages;
import com.oaklandsw.http.local.TestRequestHeaders;
import com.oaklandsw.http.local.TestResponseHeaders;
import com.oaklandsw.http.local.TestStreams;
import com.oaklandsw.http.local.TestTimeout;
import com.oaklandsw.http.local.TestURIUtil;
import com.oaklandsw.http.webapp.TestOutputStreamChunked;

import junit.framework.Test;
import junit.framework.TestSuite;

public class HttpTestSubset extends TestCaseBase
{

    public HttpTestSubset(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(HttpTestSubset.class.getName());

        // Local tests must run first
        // suite.addTest(AllLocalTests.suite());

        // Must be called before the HttpURLConnection static init
        //com.oaklandsw.http.local.TestProperties.setProperties();

        // This test must run first
        //suite.addTest(TestProperties.suite());

        if (false)
        {
            suite.addTest(TestHttpStatus.suite());
            suite.addTest(TestDefaults.suite());
                suite.addTest(TestHeaders.suite());
                suite.addTest(LocalTestAuthenticator.suite());
                suite.addTest(TestConnectionManagerLocal.suite());
                suite.addTest(TestURIUtil.suite());

                suite.addTest(TestMethodsNoHost.suite());
                suite.addTest(TestResponseHeaders.suite());
                suite.addTest(TestRequestHeaders.suite());
                suite.addTest(TestStreams.suite());
                suite.addTest(TestNtlmMessages.suite());
                suite.addTest(TestTimeout.suite());
        }

        // suite.addTest(TestLicense.suite());

        // suite.addTest(AllCookieTests.suite());
        // suite.addTest(AllErrorsvrTests.suite());

        // suite.addTest(TestAxis.suite());
        // suite.addTest(TestPipelining.suite());
        // suite.addTest(TestFailover.suite());
        //suite.addTest(TestIIS.suite());
        // suite.addTest(TestJCIFS.suite());
        suite.addTest(TestOutputStreamChunked.suite());
        // suite.addTest(TestAuthType.suite());
        // suite.addTest(TestWebDavMethods.suite());
        // suite.addTest(TestBugs.suite());
        // suite.addTest(TestHttps.suite());
        // suite.addTest(TestMethods.suite());
        // suite.addTest(TestTimeout.suite());
        return suite;
    }

    public static void main(String args[])
    {
        mainRun(suite(), args);
    }
}