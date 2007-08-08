//
// Copyright 2002-2007, oakland software, all rights reserved.
//
// May not be used or redistributed without specific written
// permission from oakland software.
//

package com.oaklandsw.http.ntlm;

import org.bouncycastle.util.encoders.Base64;

import com.oaklandsw.http.HttpException;
import com.oaklandsw.http.HttpURLConnection;

public class Ntlm
{

    public static final int     NONE                   = 0;
    public static final int     V1                     = 1;
    public static final int     V2                     = 2;
    public static final int     AS_NEGOTIATED          = 3;

    public static int           _authMessageNtResponse;
    public static int           _authMessageLmResponse;

    public static long          _authMessageFlags;

    public static boolean       _checkChallengeV2Flags;

    private static final String ENCODING               = "8859_1";

    // Do something to call the HttpURLConnection so that class goes through
    // static init and thus license checking
    private static int          _dummyMaxTries         = HttpURLConnection.MAX_TRIES;

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
