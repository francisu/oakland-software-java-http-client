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

import junit.framework.Test;
import junit.framework.TestSuite;


// Bug 1980 non-proxy host does not work on per-connection basis
// It actually should not work on a per-connection proxy setting basis,
// this test verifies that behavior
public class TestNonProxyHostConnection extends TestNonProxyHost {
    public TestNonProxyHostConnection(String testName) {
        super(testName);
        _perConnection = true;
    }

    public static void main(String[] args) {
        mainRun(suite(), args);
    }

    public static Test suite() {
        return new TestSuite(TestNonProxyHostConnection.class);
    }
}
