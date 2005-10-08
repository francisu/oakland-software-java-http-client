//
// Copyright 2002-3003, oakland software, all rights reserved.
//
// May not be used or redistributed without specific written
// permission from oakland software.
//

package com.oaklandsw.http.ntlm;

import java.io.UnsupportedEncodingException;

import com.oaklandsw.http.Base64;
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
            NegotiateMessage msg = new NegotiateMessage();
            msg.setHost(host);
            msg.setDomain(domain);
            msg.encode();
            return new String(Base64.encode(msg.getBytes()), "8859_1");
        }

        // Decode previous challange and send response
        ChallengeMessage msg = new ChallengeMessage();
        msg.setBytes(Base64.decode(message.getBytes("8859_1")));
        msg.decode();

        AuthenticateMessage amsg = new AuthenticateMessage();
        // Both are allowed, use the preferred encoding
        if ((msg.getFlags() & Message.NEGOTIATE_UNICODE) != 0
            && (msg.getFlags() & Message.NEGOTIATE_OEM) != 0)
        {
            int preferredEncoding = HttpURLConnection
                    .getNtlmPreferredEncoding();
            if (preferredEncoding == HttpURLConnection.NTLM_ENCODING_UNICODE)
                amsg.setEncodingOem(false);
            else
                amsg.setEncodingOem(true);
        }
        // No choice, select the requested one
        else if ((msg.getFlags() & Message.NEGOTIATE_UNICODE) != 0)
            amsg.setEncodingOem(false);
        else
            amsg.setEncodingOem(true);

        amsg.setNonce(msg.getNonce());
        amsg.setHost(host);
        amsg.setUser(username);
        amsg.setPassword(password);
        amsg.setDomain(domain);
        amsg.encode();
        return new String(Base64.encode(amsg.getBytes()), "8859_1");
    }

}
