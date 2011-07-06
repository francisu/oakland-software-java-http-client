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
 * Represents the user agent for the HTTP client. This interface is optionally
 * implemented by the user if the user requires notification of certain HTTP
 * events.
 */
public interface HttpUserAgent
{

    /**
     * Get the authentication credentials. This is used when a request for
     * authentication has occurred.
     * 
     * @param url
     *            the URL of the request
     * @param scheme
     *            the value representing the authentication scheme as found in
     *            Credential
     */
    public Credential getCredential(String realm, String url, int scheme);

    /**
     * Get the proxy authentication credentials. This is used when a request for
     * authentication has occurred.
     * 
     * @param url
     *            the URL of the request
     * @param scheme
     *            the value representing the authentication scheme as found in
     *            Credential
     */
    public Credential getProxyCredential(String realm, String url, int scheme);

}
