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

import java.io.IOException;

import java.net.Socket;

import javax.net.ssl.SSLSocketFactory;


public class TestSSLSocketFactory extends SSLSocketFactory {
    protected static SSLSocketFactory _default;

    static {
        _default = (SSLSocketFactory) SSLSocketFactory.getDefault();
    }

    // Indicates this factory was used to create the socket
    public boolean _used;

    // Used to test handling of null return
    public boolean _returnNull;

    public Socket createSocket(String host, int port) throws IOException {
        _used = true;

        return _default.createSocket(host, port);
    }

    public Socket createSocket(java.net.InetAddress addr, int port)
        throws IOException {
        _used = true;

        return _default.createSocket(addr, port);
    }

    public Socket createSocket(java.net.InetAddress addr1, int port1,
        java.net.InetAddress addr2, int port2) throws IOException {
        _used = true;

        return _default.createSocket(addr1, port1, addr2, port2);
    }

    public Socket createSocket(String addr1, int port1,
        java.net.InetAddress addr2, int port2) throws IOException {
        _used = true;

        return _default.createSocket(addr1, port1, addr2, port2);
    }

    public Socket createSocket(Socket s, String host, int port,
        boolean autoclose) throws IOException {
        _used = true;

        if (_returnNull) {
            return null;
        }

        return _default.createSocket(s, host, port, autoclose);
    }

    public String[] getDefaultCipherSuites() {
        return _default.getDefaultCipherSuites();
    }

    public String[] getSupportedCipherSuites() {
        return _default.getSupportedCipherSuites();
    }
}
