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

import com.oaklandsw.util.ExposedBufferInputStream;
import com.oaklandsw.util.ExposedBufferOutputStream;
import com.oaklandsw.utillog.Log;
import com.oaklandsw.util.LogUtils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.util.Vector;


/**
 * For test-nohost testing purposes only.
 *
 * @author <a href="mailto:jsdever@apache.org">Jeff Dever </a>
 */
public class SimpleHttpConnection extends HttpConnection {
    private static final Log _log = LogUtils.makeLogger();
    static GlobalState _globalState;

    static {
        _globalState = new GlobalState();
    }

    int hits = 0;
    Vector bodies = new Vector();
    ByteArrayInputStream bodyInputStream = null;
    ByteArrayOutputStream _bitBucket = new ByteArrayOutputStream();

    public SimpleHttpConnection() {
        super(null, null, -1, "localhost", 80, false, false, "none");
        setup();
        _connectionInfo = new ConnectionInfo(_connManager, "localhost", null);
        _output = new ExposedBufferOutputStream(new ByteArrayOutputStream(),
                8192);
    }

    public SimpleHttpConnection(String host, int port, boolean isSecure) {
        super(null, null, -1, host, port, isSecure, false, "none");
        setup();
        _connectionInfo = new ConnectionInfo(_connManager, host, null);
    }

    public void addResponse(String body) {
        _log.debug("addResponse: " + body);
        bodies.add(body);
    }

    protected void setup() {
        _connManager = new HttpConnectionManager(_globalState);
        _globalState._connManager = _connManager;
    }

    public void open(HttpURLConnection urlCon) throws IOException {
        _state = CS_OPEN;
        _log.trace("open");

        try {
            bodyInputStream = new ByteArrayInputStream(((String) bodies.elementAt(
                        hits)).getBytes());
            hits++;
        } catch (ArrayIndexOutOfBoundsException aiofbe) {
            throw new IOException(
                "SimpleHttpConnection has been opened more times " +
                "than it has responses.  You might need to call addResponse().");
        }
    }

    public void close() {
        _log.trace("close");

        if (bodyInputStream != null) {
            try {
                bodyInputStream.close();
            } catch (IOException e) {
            }

            bodyInputStream = null;
        }
    }

    public void checkConnection() {
        // ignore this
    }

    public ExposedBufferInputStream getInputStream() {
        return new ExposedBufferInputStream(bodyInputStream, 1024);
    }

    public OutputStream getOutputStream() {
        return new BufferedOutputStream(_bitBucket);
    }
}
