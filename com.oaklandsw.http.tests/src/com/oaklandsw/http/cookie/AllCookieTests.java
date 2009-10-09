/*
 * ====================================================================
 *
 * Copyright 1999-2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many individuals on
 * behalf of the Apache Software Foundation. For more information on the Apache
 * Software Foundation, please see <http://www.apache.org/>.
 */
package com.oaklandsw.http.cookie;

import com.oaklandsw.TestCaseBase;

import junit.framework.*;


/**
 * @author oleg Kalnichevski
 *
 * @version $Id: TestCookieAll.java 155418 2005-02-26 13:01:52Z dirkv $
 */
public class AllCookieTests extends TestCaseBase {
    public AllCookieTests(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(AllCookieTests.class.getName());
        suite.addTest(TestCookie.suite());
        suite.addTest(TestCookieCompatibilitySpec.suite());
        suite.addTest(TestCookieRFC2109Spec.suite());
        suite.addTest(TestCookieNetscapeDraft.suite());
        suite.addTest(TestCookiePolicy.suite());

        return suite;
    }

    public static void main(String[] args) {
        mainRun(suite(), args);
    }
}
