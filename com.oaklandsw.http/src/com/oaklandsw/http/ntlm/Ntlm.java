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
    public static final int     FORCE_NONE     = 0;
    public static final int     FORCE_V1       = 1;
    public static final int     FORCE_V2       = 2;

    // For testing
    public static int           _forceNtlmType;

    private static final String ENCODING       = "8859_1";

    // Do something to call the HttpURLConnection so that class goes through
    // static init and thus license checking
    private static int          _dummyMaxTries = HttpURLConnection.MAX_TRIES;

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
            if (_forceNtlmType == FORCE_V2)
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
