/*
 * Copyright 2008 Oakland Software Incorporated. All rights Reserved.
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
package com.oaklandsw.http;

import java.net.Socket;

import com.oaklandsw.utillog.Log;

import com.oaklandsw.util.LogUtils;

/**
 * Creates the Socket to be used by the HTTP connection.
 */
public class AbstractSocketFactory
{
    private static final Log   _log             = LogUtils.makeLogger();

    /**
     * Creates a socket to be used by an HTTP connection.
     * 
     * The default implementation returns a standard socket. This can be
     * overridden as required for things like a SOCKS connection.
     * 
     * Don't include the host/port on the constructor for the socket, as a
     * connect() call is always issued after the socket is created. These are
     * provided only for information.
     * 
     * @param urlCon
     *            the HttpURLConnection that is using this socket
     * @param host
     *            the hostname of the socket
     * @param port
     *            the port number of the socket
     * @return
     */
    public Socket createSocket(HttpURLConnection urlCon, String host, int port)
    {
        _log.debug("createSocket (default socket factory)");
        return new Socket();
    }

}
