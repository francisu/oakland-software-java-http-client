/*
 * Copyright 2002, 2008, Oakland Software Incorporated
 * 
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

import java.io.UnsupportedEncodingException;

import org.bouncycastle.util.encoders.Base64;

import com.oaklandsw.http.HttpException;
import com.oaklandsw.http.HttpURLConnection;
import com.oaklandsw.util.Util;

public class Ntlm
{

    public static final int    NONE           = 0;
    public static final int    V1             = 1;
    public static final int    V2             = 2;
    public static final int    AS_NEGOTIATED  = 3;

    public static int          _authMessageNtResponse;
    public static int          _authMessageLmResponse;

    // Use the Negotiate flags that work with NT systems. See bug 2607, 2596
    public static boolean      _useNtNegotiateFlags;

    public static long         _authMessageFlags;
    public static long         _challengeMessageFlags;

    public static boolean      _checkChallengeV2Flags;

    public static final String ENCODING       = Util.ASCII_ENCODING;

    // Do something to call the HttpURLConnection so that class goes through
    // static init and thus license checking
    private static int         _dummyMaxTries = HttpURLConnection.MAX_TRIES;

    public static void init()
    {
        _authMessageNtResponse = AS_NEGOTIATED;
        _authMessageLmResponse = AS_NEGOTIATED;

        _authMessageFlags = Message.NEGOTIATE_NTLM
            | Message.NEGOTIATE_ALWAYS_SIGN;

        _checkChallengeV2Flags = false;
    }

    static
    {
        init();
    }

    public static void forceV2()
    {
        _authMessageLmResponse = V2;
        _authMessageNtResponse = V2;
    }

    public static void forceV1()
    {
        _authMessageLmResponse = V1;
        _authMessageNtResponse = V1;
    }

    protected static int writeIntoTargetInfo(String value,
                                             int type,
                                             int offset,
                                             byte[] targetInfo)
    {
        int domainLength = value.length() * 2;

        Util.toByteLittle(2, 2, targetInfo, offset);
        offset += 2;
        Util.toByteLittle(domainLength, 2, targetInfo, offset);
        offset += 2;
        try
        {
            System.arraycopy(value.getBytes(Util.UNICODE_LE_ENCODING),
                             0,
                             targetInfo,
                             offset,
                             domainLength);
        }
        catch (UnsupportedEncodingException e)
        {
            Util.impossible(e);
        }
        return offset + domainLength;
    }

    public static byte[] createTargetInfo(String domain, String host)
    {
        // Multiply by 2 since the names are unicode
        int domainLength = domain != null ? domain.length() * 2 : 0;
        int hostLength = host != null ? host.length() * 2 : 0;

        byte[] targetInfo = new byte[(domainLength > 0 ? domainLength + 4 : 0)
            + (hostLength > 0 ? hostLength + 4 : 0)
            + 4];
        int offset = 0;

        if (domain != null)
            offset = writeIntoTargetInfo(domain, 2, offset, targetInfo);
        if (host != null)
            writeIntoTargetInfo(host, 1, offset, targetInfo);
        return targetInfo;
    }

    /**
     * Returns the response for the given message.
     * 
     * @param message
     *            the message that was received from the server.
     * @param username
     *            the username to authenticate with.
     * @param password
     *            the password to authenticate with.
     * @param domain
     *            the NT domain to authenticate in.
     */
    public static final String getResponseFor(String message,
                                              String username,
                                              String password,
                                              String host,
                                              String domain)
        throws HttpException
    {
        try
        {
            // No previous message, send the negotiate message
            if (message.equals(""))
            {
                NegotiateMessage negMsg = new NegotiateMessage();
                negMsg.setHost(host);
                negMsg.setDomain(domain);
                negMsg.encode();
                return new String(Base64.encode(negMsg.getBytes()), ENCODING);
            }

            // Decode previous challange and send response
            ChallengeMessage challengeMsg = new ChallengeMessage();
            challengeMsg.setBytes(Base64.decode(message.getBytes(ENCODING)));
            challengeMsg.decode();

            // Do checking of the challenge flags
            if (_checkChallengeV2Flags)
            {
                long expectedFlags = Message.NEGOTIATE_NTLM
                    | Message.NEGOTIATE_TARGET_INFO;
                if ((challengeMsg.getFlags() & expectedFlags) != expectedFlags)
                {
                    throw new HttpException("Expected NTLMv2 flags not set in Challenge message");
                }
            }

            AuthenticateMessage authMsg = new AuthenticateMessage();
            authMsg.setChallenge(challengeMsg);

            // Both are allowed, use the preferred encoding
            if ((challengeMsg.getFlags() & Message.NEGOTIATE_UNICODE) != 0
                && (challengeMsg.getFlags() & Message.NEGOTIATE_OEM) != 0)
            {
                int preferredEncoding = HttpURLConnection
                        .getNtlmPreferredEncoding();
                if (preferredEncoding == HttpURLConnection.NTLM_ENCODING_UNICODE)
                    authMsg.setEncodingOem(false);
                else
                    authMsg.setEncodingOem(true);
            }
            // No choice, select the requested one
            else if ((challengeMsg.getFlags() & Message.NEGOTIATE_UNICODE) != 0)
                authMsg.setEncodingOem(false);
            else
                authMsg.setEncodingOem(true);

            // Prefer NTLM v2 if requested
            // if ((challengeMsg.getFlags() & Message.NEGOTIATE_NTLM2) != 0)
            // authMsg.setUseNtlm2(true);

            authMsg.setNonce(challengeMsg.getNonce());
            authMsg.setHost(host);
            authMsg.setUser(username);
            authMsg.setPassword(password);
            authMsg.setDomain(domain);

            authMsg.encode();
            return new String(Base64.encode(authMsg.getBytes()), ENCODING);
        }

        catch (java.io.UnsupportedEncodingException e)
        {
            throw new IllegalStateException("NTLM requires ASCII support.");
        }

    }
}
