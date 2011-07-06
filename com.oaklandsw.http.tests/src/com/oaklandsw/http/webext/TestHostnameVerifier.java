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

import javax.net.ssl.SSLSession;


public class TestHostnameVerifier implements com.oaklandsw.http.HostnameVerifier {
    // Indicates this was called
    public boolean _used;

    // Should throw an exception
    public boolean _doThrow;

    // Tells it how to vote
    public boolean _shouldPass;

    public boolean verify(String hostName, SSLSession sess) {
        if (hostName == null) {
            throw new IllegalArgumentException("Null hostname");
        }

        if (sess == null) {
            throw new IllegalArgumentException("Null session");
        }

        if (_doThrow) {
            throw new RuntimeException("expected exception (_doThrow)");
        }

        _used = true;

        return _shouldPass;
    }
}
