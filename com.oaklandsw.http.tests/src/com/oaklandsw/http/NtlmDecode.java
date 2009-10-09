/*
 * Copyright 2004 oakland software, incorporated. All rights Reserved.
 */
package com.oaklandsw.http;

import com.oaklandsw.http.ntlm.AuthenticateMessage;
import com.oaklandsw.http.ntlm.ChallengeMessage;
import com.oaklandsw.http.ntlm.Message;
import com.oaklandsw.http.ntlm.NegotiateMessage;

import org.bouncycastle.util.encoders.Base64;


public class NtlmDecode {
    public static void main(String[] args) throws Exception {
        // Decode using generic message to find type
        Message msg = new Message();
        msg.setBytes(Base64.decode(args[0].getBytes("8859_1")));
        msg.decode();

        switch (msg.getType()) {
        case Message.MSG_CHALLENGE:
            msg = new ChallengeMessage();

            break;

        case Message.MSG_NEGOTIATE:
            msg = new NegotiateMessage();

            break;

        case Message.MSG_AUTHENTICATE:
            msg = new AuthenticateMessage();

            break;
        }

        // Decode real message
        msg.setBytes(Base64.decode(args[0].getBytes("8859_1")));
        msg.decode();

        System.out.println(msg.toStringDump(0));
    }
}
