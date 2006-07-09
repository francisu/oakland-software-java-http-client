// Copyright 2002 oakland software, All rights reserved

package com.oaklandsw.http.local;

import java.io.IOException;
import java.net.MalformedURLException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.oaklandsw.http.TestEnv;
import com.oaklandsw.http.ntlm.AuthenticateMessage;
import com.oaklandsw.http.ntlm.ChallengeMessage;
import com.oaklandsw.http.ntlm.NegotiateMessage;

public class TestNtlmMessages extends TestCase
{

    public TestNtlmMessages(String name)
    {
        super(name);
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(suite());
    }

    // We assume the web server is running
    protected void setUp()
    {
        TestEnv.setUp();
    }

    public static Test suite()
    {
        return new TestSuite(TestNtlmMessages.class);
    }

    public void test100Messages() throws MalformedURLException, IOException
    {
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
