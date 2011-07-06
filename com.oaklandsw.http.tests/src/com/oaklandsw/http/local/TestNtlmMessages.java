// Copyright 2002 oakland software, All rights reserved
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
package com.oaklandsw.http.local;

import com.oaklandsw.http.HttpTestBase;
import com.oaklandsw.http.ntlm.AuthenticateMessage;
import com.oaklandsw.http.ntlm.ChallengeMessage;
import com.oaklandsw.http.ntlm.NegotiateMessage;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.IOException;

import java.net.MalformedURLException;


public class TestNtlmMessages extends HttpTestBase {
    public TestNtlmMessages(String name) {
        super(name);
    }

    public static void main(String[] args) {
        mainRun(suite(), args);
    }

    // We assume the web server is running
    public void setUp() throws Exception {
        super.setUp();
    }

    public static Test suite() {
        return new TestSuite(TestNtlmMessages.class);
    }

    public void test100Messages() throws MalformedURLException, IOException {
        byte[] msgBytes;

        NegotiateMessage msg = new NegotiateMessage();
        msg.setHost("testhost");
        msg.setDomain("testdomain");
        msg.encode();

        msgBytes = msg.getBytes();

        msg = new NegotiateMessage();
        msg.setBytes(msgBytes);
        msg.decode();

        byte[] nonce = new byte[] { 89, 82, 72, 23, 33, 43, 7, 8 };

        ChallengeMessage cmsg = new ChallengeMessage();
        cmsg.setNonce(nonce);
        cmsg.encode();

        msgBytes = cmsg.getBytes();

        cmsg = new ChallengeMessage();
        cmsg.setBytes(msgBytes);
        cmsg.decode();

        AuthenticateMessage amsg = new AuthenticateMessage();
        amsg.setChallenge(cmsg);
        amsg.setHost("testhost");
        amsg.setUser("testuser");
        amsg.setPassword("testpass");
        amsg.setDomain("testdomain");
        amsg.setNonce(nonce);
        amsg.encode();

        amsg = new AuthenticateMessage();
        amsg.setChallenge(cmsg);
        amsg.setHost("testhost");
        amsg.setUser("testuser");
        amsg.setPassword("testpast");
        amsg.setDomain("testdomain");
        amsg.setNonce(nonce);
        amsg.encode();
    }
}
