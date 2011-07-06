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

import com.oaklandsw.http.HttpConnection;
import com.oaklandsw.http.HttpException;
import com.oaklandsw.http.HttpURLConnectInternal;

import com.oaklandsw.utillog.Log;
import com.oaklandsw.util.LogUtils;

import java.io.IOException;


/**
 * For test-nohost testing purposes only.
 */
public class SimpleHttpMethod extends HttpURLConnectInternal {
    private static final Log _log = LogUtils.makeLogger();
    HttpConnection _saveConnection;
    String headerName;
    String headerValue;

    public SimpleHttpMethod() {
        super();
    }

    public SimpleHttpMethod(String hName, String hValue) {
        super();
        headerName = hName;
        headerValue = hValue;
    }

    public String getHeaderField(String name) {
        try {
            if (name.equalsIgnoreCase(headerName)) {
                return headerValue;
            }

            return super.getHeaderField(name);
        } catch (NullPointerException e) {
            return super.getHeaderField(name);
        }
    }

    public void connect() throws IOException {
        _log.trace("connect");

        if (connected) {
            return;
        }

        _connection = _saveConnection;
        _connection.open(null);
        _conInStream = _connection.getInputStream();
        _conOutStream = _connection.getOutputStream();
        connected = true;
    }

    protected void releaseConnection(int close) {
        _log.trace("releaseConnection");

        if (_connection != null) {
            connected = false;
        }
    }

    public void execute(HttpConnection connection)
        throws HttpException, IOException {
        _saveConnection = connection;
        execute();
    }

    public void setConnection(HttpConnection connection) {
        // Don't call super
        _connection = connection;
    }

    public void setState(HttpConnection connection)
        throws HttpException, IOException {
        _saveConnection = connection;
        connection._connManager = _connManager;
    }

    public void testAddRequestHeaders(HttpConnection connection)
        throws HttpException, IOException {
        _connection = connection;
        super.addRequestHeaders();
    }
}
