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
 * Contains the credentials specific to the NTLM authentication protocol.
 */
public class NtlmCredential extends UserCredential implements Credential
{

    protected String _host;

    protected String _domain;

    public NtlmCredential()
    {
    }

    public NtlmCredential(String user,
            String password,
            String host,
            String domain)
    {
        super(user, password);
        _host = host;
        _domain = domain;
    }

    /**
     * Gets the value of host
     * 
     * @return the value of host
     */
    public String getHost()
    {
        return _host;
    }

    /**
     * Sets the value of host
     * 
     * @param argHost
     *            Value to assign to host
     */
    public void setHost(String argHost)
    {
        _host = argHost;
    }

    /**
     * Gets the value of domain
     * 
     * @return the value of domain
     */
    public String getDomain()
    {
        return _domain;
    }

    /**
     * Sets the value of domain
     * 
     * @param argDomain
     *            Value to assign to domain
     */
    public void setDomain(String argDomain)
    {
        _domain = argDomain;
    }

    String getKey()
    {
        if (_domain == null)
            return super.getKey();
        return _domain + super.getKey();
    }

    public String toString()
    {
        return "Host: "
            + _host
            + " Domain: "
            + _domain
            + " "
            + super.toString();
    }

}
