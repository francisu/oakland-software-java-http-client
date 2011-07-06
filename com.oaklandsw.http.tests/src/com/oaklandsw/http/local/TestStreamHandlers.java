/*
 * Copyright 2004 oakland software, incorporated. All rights Reserved.
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
package com.oaklandsw.http.local;

import com.oaklandsw.http.HttpTestBase;
import com.oaklandsw.http.URLStreamHandlerFactoryImpl;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;


public class TestStreamHandlers extends HttpTestBase {
    public TestStreamHandlers(String testName) {
        super(testName);
    }

    public static void main(String[] args) {
        mainRun(suite(), args);
    }

    public static Test suite() {
        return new TestSuite(TestStreamHandlers.class);
    }

    public void testExplicitHttp() throws Exception {
        URL url = new URL("http", "host", 9999, "file",
                new com.oaklandsw.http.Handler());

        // Should work
        if (!(url.openConnection() instanceof com.oaklandsw.http.HttpURLConnection)) {
            fail("Wrong URL connection class");
        }
    }

    public void testExplicitHttps() throws Exception {
        URL url = new URL("https", "host", 9999, "file",
                new com.oaklandsw.https.Handler());

        // Should work
        if (!(url.openConnection() instanceof com.oaklandsw.http.HttpURLConnection)) {
            fail("Wrong URL connection class");
        }
    }

    public void testExplicitFactoryHttp() throws Exception {
        URLStreamHandlerFactory factory = new URLStreamHandlerFactoryImpl();
        URL.setURLStreamHandlerFactory(factory);

        URL url = new URL("http://host/file");

        // Should work
        if (!(url.openConnection() instanceof com.oaklandsw.http.HttpURLConnection)) {
            fail("Wrong URL connection class");
        }
    }

    public void testExplicitFactoryHttps() throws Exception {
        URLStreamHandlerFactory factory = new URLStreamHandlerFactoryImpl();

        try {
            URL.setURLStreamHandlerFactory(factory);
        } catch (Error ex) {
            // Expected, if the factory was already set
        }

        URL url = new URL("https://host/file");

        // Should work
        if (!(url.openConnection() instanceof com.oaklandsw.http.HttpURLConnection)) {
            fail("Wrong URL connection class");
        }
    }

    public void testExplicitFactoryBad() throws Exception {
        URLStreamHandlerFactory factory = new URLStreamHandlerFactoryImpl();

        try {
            URL.setURLStreamHandlerFactory(factory);
        } catch (Error ex) {
            // Expected, if the factory was already set
        }

        try {
            new URL("bad://host/file");
            fail("Did not get expected exception");
        } catch (MalformedURLException badProtocol) {
            // Expected
        }
    }

    // Try creating the factory inline
    public void testExplicitFactoryInline() throws Exception {
        // Note, when run with other tests, this setting of the
        // factory has no effect because it can be set only once,
        // this is here mainly as a sample
        try {
            URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory() {
                    public URLStreamHandler createURLStreamHandler(
                        String protocol) {
                        if (protocol.equalsIgnoreCase("http")) {
                            return new com.oaklandsw.http.Handler();
                        } else if (protocol.equalsIgnoreCase("https")) {
                            return new com.oaklandsw.https.Handler();
                        }

                        return null;
                    }
                });
        } catch (Error ex) {
            // Expected, if the factory was already set
        }

        URL url = new URL("https://host/file");

        // Should work
        if (!(url.openConnection() instanceof com.oaklandsw.http.HttpURLConnection)) {
            fail("Wrong URL connection class");
        }
    }
}
