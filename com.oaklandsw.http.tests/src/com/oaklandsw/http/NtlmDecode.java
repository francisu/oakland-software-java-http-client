/*
 * Copyright 2004 oakland software, incorporated. All rights Reserved.
 */
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
