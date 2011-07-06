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
package com.oaklandsw.http.webext;

import com.oaklandsw.TestCaseBase;

import junit.framework.Test;
import junit.framework.TestSuite;


/**
 * A suite composed of only those tests which require an external Internet
 * connection.
 *
 * Optionally this test can be run using a proxy. If the proxy is not
 * authenticating do not set user / password.
 *
 * System properties: httpclient.test.proxyHost - proxy host name
 * httpclient.test.proxyPort - proxy port httpclient.test.proxyUser - proxy auth
 * username httpclient.test.proxyPass - proxy auth password
 *
 * @author Rodney Waldhoff
 * @author Ortwin Gl�ck
 * @version $Id: TestAll.java,v 1.3 2002/07/23 14:39:14 dion Exp $
 */
public class AllWebextTests extends TestCaseBase {
    public AllWebextTests(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(AllWebextTests.class.getName());
        suite.addTest(TestBugs.suite());
        suite.addTest(TestCookie.suite());
        suite.addTest(TestSSL.suite());
        suite.addTest(TestHttps.suite());
        suite.addTest(TestMethods.suite());
        suite.addTest(TestTimeout.suite());
        suite.addTest(TestProxyHost.suite());
        suite.addTest(TestNonProxyHost.suite());
        suite.addTest(TestNonProxyHostConnection.suite());

        return suite;
    }

    public static void main(String[] args) {
        mainRun(suite(), args);
    }
}
