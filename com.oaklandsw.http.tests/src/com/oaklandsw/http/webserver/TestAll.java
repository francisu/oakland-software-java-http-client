/*
 * $Header:
 * /home/cvspublic/jakarta-commons/httpclient/src/test/org/apache/commons/httpclient/TestLocalHost.java,v
 * 1.4 2002/09/03 01:24:52 sullis Exp $ $Revision: 1.4 $ $Date: 2002/09/03
 * 01:24:52 $
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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.oaklandsw.http.WebserverTestHttpConnectionManager;

/**
 * A suite composed of only those tests which require the local webserver and
 * test web application.
 * 
 * @author Rodney Waldhoff
 * @version $Id: TestAll.java,v 1.4 2002/09/03 01:24:52 sullis Exp $
 */
public class TestAll extends TestCase
{

    public TestAll(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite();
        suite.addTest(WebserverTestHttpConnectionManager.suite());
        suite.addTest(TestMethods.suite());
        suite.addTest(TestWebDavMethods.suite());
        suite.addTest(TestGetMethod.suite());
        suite.addTest(TestTraceMethod.suite());
        suite.addTest(Test292.suite());
        return suite;
    }

    public static void main(String args[])
    {
        String[] testCaseName = { TestAll.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }

}
