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
package com.oaklandsw.http.errorsvr;

import com.oaklandsw.TestCaseBase;

import junit.framework.Test;
import junit.framework.TestSuite;


public class AllErrorsvrTests extends TestCaseBase {
    public AllErrorsvrTests(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(AllErrorsvrTests.class.getName());
        suite.addTest(TestIdleTimeouts.suite());
        suite.addTest(TestError.suite());
        suite.addTest(TestDisconnect.suite());
        suite.addTest(TestTimeout.suite());
        suite.addTest(TestTimeoutBeforeHeaders.suite());
        suite.addTest(TestTimeoutDuringStatus.suite());
        suite.addTest(TestData.suite());
        suite.addTest(TestStatusLine.suite());

        return suite;
    }

    public static void main(String[] args) {
        mainRun(suite(), args);
    }
}
