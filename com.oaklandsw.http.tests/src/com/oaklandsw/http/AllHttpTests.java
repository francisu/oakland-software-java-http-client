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
package com.oaklandsw.http;

import com.oaklandsw.TestCaseBase;

import com.oaklandsw.http.cookie.AllCookieTests;
import com.oaklandsw.http.errorsvr.AllErrorsvrTests;
import com.oaklandsw.http.local.AllLocalTests;
import com.oaklandsw.http.webapp.AllWebappTests;
import com.oaklandsw.http.webext.AllWebextTests;
import com.oaklandsw.http.webserver.AllWebserverTests;

import junit.framework.Test;
import junit.framework.TestSuite;


public class AllHttpTests extends TestCaseBase {
    public AllHttpTests(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(AllHttpTests.class.getName());

        // Make sure they are setup before the HttpURLConnection
        // initializer is called
        com.oaklandsw.http.local.TestProperties.setProperties();

        // Local tests must run first
        suite.addTest(AllLocalTests.suite());

        suite.addTest(AllCookieTests.suite());
        suite.addTest(AllErrorsvrTests.suite());
        suite.addTest(AllWebappTests.suite());
        suite.addTest(AllWebserverTests.suite());
        suite.addTest(AllWebextTests.suite());

        return suite;
    }

    public static void main(String[] args) {
        mainRun(suite(), args);
    }
}
