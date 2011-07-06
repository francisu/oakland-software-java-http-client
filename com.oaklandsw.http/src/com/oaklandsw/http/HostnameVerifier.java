//
// Copyright 2003, oakland software, all rights reserved.
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

import javax.net.ssl.SSLSession;

/**
 * An interface to allow the acceptance of SSL connections where the hostname
 * does not match the host on the certificate.
 * 
 */
public interface HostnameVerifier
{

    /**
     * Called when an SSL connection is established and the hostname on the
     * certificate does not match the hostname to which the connection is made.
     * 
     * @param hostName
     *            the host name to which the connection was made
     * @param session
     *            the SSLSession associated with this connection.
     * @return true, if the connection is allowed to proceed, false if it should
     *         fail.
     */
    public boolean verify(String hostName, SSLSession session);

}
