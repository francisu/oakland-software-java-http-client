/*
 * Copyright 2007 Oakland Software Incorporated. All rights Reserved.
 */
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

import com.oaklandsw.http.HttpURLConnectInternal;
import com.oaklandsw.http.HttpURLConnection;

import junit.framework.Test;
import junit.framework.TestSuite;


/**
 * Tests pipelining with options that have a great deal more retrying than the
 * standard configuration because the observed max connection limit is not used.
 */
public class TestPipeliningRough extends TestPipelining {
    public TestPipeliningRough(String testName) {
        super(testName);

        // Use only one connection at a time
        _pipelineOptions = HttpURLConnection.PIPE_PIPELINE;

        //_logging = true;
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(TestPipeliningRough.class);

        return suite;
    }

    public void setUp() throws Exception {
        super.setUp();

        // Makes things harsher
        HttpURLConnectInternal._ignoreObservedMaxCount = true;
    }
}
