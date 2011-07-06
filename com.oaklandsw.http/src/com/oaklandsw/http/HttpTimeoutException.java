//
// Copyright 2002-3003, oakland software, all rights reserved.
//

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

/**
 * Indicates a timeout has occurred.
 * 
 * A timeout may occur:
 * <ul>
 * <li>Connection Establishment - When an initial TCP connection is being
 * established to the specified host.
 * <li>Connection Pool - When waiting for a connection to the host to become
 * available. By default, only 2 connections are allowed to a given host and if
 * more requests for connections to that host are made, they are queued until
 * one of the connections becomes available.
 * <li>Response - When waiting for the response to a request.
 * </ul>
 */
public class HttpTimeoutException extends RuntimeException
{
    /**
     * Creates a new HttpTimeoutException.
     */
    public HttpTimeoutException()
    {
        super();
    }

    /**
     * Creates a new HttpTimeoutException with the specified message.
     * 
     * @param message
     *            exception message
     */
    public HttpTimeoutException(String message)
    {
        super(message);
    }
}
