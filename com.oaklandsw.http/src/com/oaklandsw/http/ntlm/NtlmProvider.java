// 
// Copyright Â© 2003 Eric Glass (eglass1 at comcast.net).
//
// Permission to use, copy, modify, and distribute this document
// for any purpose and without any fee is hereby granted, provided
// that the above copyright notice and this list of conditions
// appear in all copies.
//
// Modifications copyright Oakland Software Incorporated 2006, 2008
//
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


package com.oaklandsw.http.ntlm;

public interface NtlmProvider
{

    // Higher level methods used by NtlmPasswordAuthenticator

    /**
     * Computes the 24 byte ANSI password hash given the 8 byte server
     * challenge.
     */
    public byte[] getAnsiHash(int level,
                              String domain,
                              String username,
                              String password,
                              byte[] challenge,
                              byte[] clientChallenge);

    /**
     * Computes the 24 byte Unicode password hash given the 8 byte server
     * challenge.
     */
    public byte[] getUnicodeHash(int level,
                                 String domain,
                                 String username,
                                 String password,
                                 byte[] challenge,
                                 byte[] clientChallenge);

    // Lower level methods

    /**
     * Calculates the LM Response for the given challenge, using the specified
     * password.
     * 
     * @param password
     *            The user's password.
     * @param challenge
     *            The Type 2 challenge from the server.
     * 
     * @return The LM Response.
     */
    public abstract byte[] getLMResponse(String password, byte[] challenge);

    /**
     * Calculates the NTLM Response for the given challenge, using the specified
     * password.
     * 
     * @param password
     *            The user's password.
     * @param challenge
     *            The Type 2 challenge from the server.
     * 
     * @return The NTLM Response.
     */
    public abstract byte[] getNTLMResponse(String password, byte[] challenge);

    /**
     * Calculates the NTLMv2 Response for the given challenge, using the
     * specified authentication target, username, password, target information
     * block, and client challenge.
     * 
     * @param target
     *            The authentication target (i.e., domain).
     * @param user
     *            The username.
     * @param password
     *            The user's password.
     * @param targetInformation
     *            The target information block from the Type 2 message.
     * @param challenge
     *            The Type 2 challenge from the server.
     * @param clientChallenge
     *            The random 8-byte client challenge.
     * 
     * @return The NTLMv2 Response.
     */
    public abstract byte[] getNTLMv2Response(String target,
                                             String user,
                                             String password,
                                             byte[] targetInformation,
                                             byte[] challenge,
                                             byte[] clientChallenge);

    /**
     * Calculates the LMv2 Response for the given challenge, using the specified
     * authentication target, username, password, and client challenge.
     * 
     * @param target
     *            The authentication target (i.e., domain).
     * @param user
     *            The username.
     * @param password
     *            The user's password.
     * @param challenge
     *            The Type 2 challenge from the server.
     * @param clientChallenge
     *            The random 8-byte client challenge.
     * 
     * @return The LMv2 Response.
     */
    public abstract byte[] getLMv2Response(String target,
                                           String user,
                                           String password,
                                           byte[] challenge,
                                           byte[] clientChallenge);

    /**
     * Calculates the NTLM2 Session Response for the given challenge, using the
     * specified password and client challenge.
     * 
     * @param password
     *            The user's password.
     * @param challenge
     *            The Type 2 challenge from the server.
     * @param clientChallenge
     *            The random 8-byte client challenge.
     * 
     * @return The NTLM2 Session Response. This is placed in the NTLM response
     *         field of the Type 3 message; the LM response field contains the
     *         client challenge, null-padded to 24 bytes.
     */
    public abstract byte[] getNTLM2SessionResponse(String password,
                                                   byte[] challenge,
                                                   byte[] clientChallenge);

}