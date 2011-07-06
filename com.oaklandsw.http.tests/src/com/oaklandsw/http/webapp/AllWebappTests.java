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
package com.oaklandsw.http.webapp;

import com.oaklandsw.TestCaseBase;

import junit.framework.Test;
import junit.framework.TestSuite;


public class AllWebappTests extends TestCaseBase {
    public AllWebappTests(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(AllWebappTests.class.getName());
        suite.addTest(TestTimeout.suite());
        suite.addTest(TestFailover.suite());
        suite.addTest(TestAxis1.suite());
        suite.addTest(TestAxis2.suite());
        suite.addTest(TestIIS.suite());
        // SSO no longer supported
        //suite.addTest(TestJCIFS.suite());
        suite.addTest(TestExplicitConnection.suite());
        suite.addTest(TestMethods.suite());
        suite.addTest(TestAuthType.suite());
        suite.addTest(TestPipelining.suite());
        suite.addTest(TestPipeliningRough.suite());
        suite.addTest(TestOutputStream.suite());
        suite.addTest(TestOutputStreamChunked.suite());
        suite.addTest(TestOutputStreamFixed.suite());
        suite.addTest(TestOutputStreamRaw.suite());
        suite.addTest(TestMultiThread.suite());
        suite.addTest(TestParameters.suite());
        suite.addTest(TestHeaders.suite());
        suite.addTest(TestRedirect.suite());
        suite.addTest(TestBasicAuth.suite());
        suite.addTest(TestCookie.suite());
        suite.addTest(TestNoData.suite());
        suite.addTest(TestDisconnect.suite());
        suite.addTest(TestWebStart.suite());
        suite.addTest(TestTunneling.suite());
        suite.addTest(TestFtpProxy.suite());
        suite.addTest(TestURLConn.suite());
        suite.addTest(TestURLMultiConn.suite());
        suite.addTest(TestSSL.suite());

        return suite;
    }

    public static void main(String[] args) {
        mainRun(suite(), args);
    }
}
