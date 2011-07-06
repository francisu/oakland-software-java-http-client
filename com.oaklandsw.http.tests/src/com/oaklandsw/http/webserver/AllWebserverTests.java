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
package com.oaklandsw.http.webserver;

import com.oaklandsw.TestCaseBase;

import junit.framework.Test;
import junit.framework.TestSuite;


/**
 * A suite composed of only those tests which require the local webserver and
 * test web application.
 *
 * @author Rodney Waldhoff
 * @version $Id: TestAll.java,v 1.4 2002/09/03 01:24:52 sullis Exp $
 */
public class AllWebserverTests extends TestCaseBase {
    public AllWebserverTests(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(AllWebserverTests.class.getName());
        suite.addTest(TestConnectionManagerWebserver.suite());
        suite.addTest(TestBasicAndDigestAuth.suite());
        suite.addTest(TestMethods.suite());
        suite.addTest(TestCharacter.suite());
        suite.addTest(TestWebDavMethods.suite());
        suite.addTest(TestGetMethod.suite());
        suite.addTest(TestTraceMethod.suite());
        suite.addTest(Test292.suite());

        return suite;
    }

    public static void main(String[] args) {
        mainRun(suite(), args);
    }
}
