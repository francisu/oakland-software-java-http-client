//
// Copyright 2002-3003, oakland software, all rights reserved.
//
// May not be used or redistributed without specific written
// permission from oakland software.
//

package com.oaklandsw.http.ntlm;

import java.io.UnsupportedEncodingException;

import org.bouncycastle.util.encoders.Base64;

import com.oaklandsw.http.HttpException;
import com.oaklandsw.http.HttpURLConnection;

public class Ntlm
{

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
     * 
     * @throws UnsupportedEncodingException
     *             if ASCII encoding is not supported by the JVM.
     */
    public static final String getResponseFor(String message,
                                              String username,
                                              String password,
                                              String host,
                                              String domain)
        throws UnsupportedEncodingException,
            HttpException
    {
        // No previous message, send the negotiate message
        if (message == null || message.trim().equals(""))
        {
            NegotiateMessage negMsg = new NegotiateMessage();
            negMsg.setHost(host);
            negMsg.setDomain(domain);
            negMsg.encode();
            return new String(Base64.encode(negMsg.getBytes()), "8859_1");
        }

        // Decode previous challange and send response
        ChallengeMessage challengeMsg = new ChallengeMessage();
        challengeMsg.setBytes(Base64.decode(message.getBytes("8859_1")));
        challengeMsg.decode();

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
        //if ((challengeMsg.getFlags() & Message.NEGOTIATE_NTLM2) != 0)
        //    authMsg.setUseNtlm2(true);
        
        authMsg.setNonce(challengeMsg.getNonce());
        authMsg.setHost(host);
        authMsg.setUser(username);
        authMsg.setPassword(password);
        authMsg.setDomain(domain);

        // Testing
        //authMsg.setUseNtlm2(true);
        //authMsg.encode();

        //authMsg.setUseNtlm2(false);
        authMsg.encode();
        return new String(Base64.encode(authMsg.getBytes()), "8859_1");
    }

}
