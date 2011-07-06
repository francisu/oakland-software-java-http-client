/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.oaklandsw.http.local;

import com.oaklandsw.TestCaseBase;

import com.oaklandsw.http.LocalTestAuthenticator;

import junit.framework.Test;
import junit.framework.TestSuite;


/**
 * Tests that don't require any external host. I.e., that run entirely within
 * this JVM.
 *
 * (True unit tests, by some definitions.)
 *
 * @author Rodney Waldhoff
 * @author <a href="mailto:jsdever@apache.org">Jeff Dever </a>
 * @version $Revision: 1.12 $ $Date: 2002/09/05 00:21:57 $
 */
public class AllLocalTests extends TestCaseBase {
    public AllLocalTests(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(AllLocalTests.class.getName());

        // Must be called before the HttpURLConnection static init
        com.oaklandsw.http.local.TestProperties.setProperties();

        // This test must run first
        suite.addTest(TestProperties.suite());

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

        return suite;
    }

    public static void main(String[] args) {
        mainRun(suite(), args);
    }
}
