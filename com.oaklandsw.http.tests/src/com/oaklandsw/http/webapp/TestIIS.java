// Copyright 2002 oakland software, All rights reserved

package com.oaklandsw.http.webapp;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.TestBase;
import com.oaklandsw.http.TestEnv;
import com.oaklandsw.http.TestUserAgent;
import com.oaklandsw.http.ntlm.NegotiateMessage;

public class TestIIS extends TestBase
{

    HttpURLConnection _urlCon;

    String            _getForm;

    public TestIIS(String name)
    {
        super(name);
        // _doHttps = true;
    }

    public static void main(String[] args)
    {
        mainRun(suite(), args);
    }

    // We assume the web server is running
    protected void setUp() throws Exception
    {
        super.setUp();
        TestUserAgent._type = TestUserAgent.GOOD;
        _getForm = "TestForm2.asp";
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }
    
    public static Test suite()
    {
        return new TestSuite(TestIIS.class);
    }

    public void iisCheckReply(String reply)
    {
        assertTrue("Incorrect reply", reply.indexOf("ABCDEndOfFormWXYZ") >= 0);
        assertTrue("Incorrect reply",
                   reply.indexOf("ABCDMiddleOfFormWXYZ") >= 0);
        assertTrue("Incorrect reply", reply.indexOf("ABCDStartOfFormWXYZ") >= 0);
    }

    public void test100Post() throws MalformedURLException, IOException
    {
        URL url = new URL(TestEnv._protocol
            + TestEnv.TEST_URL_IIS
            + "TestForm1.asp");
        int response = 0;

        //setLogging(true);
        _urlCon = (HttpURLConnection)url.openConnection();

        _urlCon.setRequestMethod("POST");
        _urlCon.setDoOutput(true);
        OutputStream outStr = _urlCon.getOutputStream();
        outStr.write("lname=lastName&fname=firstName".getBytes("ASCII"));
        outStr.close();
        response = _urlCon.getResponseCode();
        assertEquals(200, response);

        String reply = getReply(_urlCon);
        iisCheckReply(reply);
        //setLogging(false);
    }

    public void test110PostClose() throws MalformedURLException, IOException
    {
        test100Post();
        _urlCon.disconnect();
    }

    public void test120MultiPostClose()
        throws MalformedURLException,
            IOException
    {
        test110PostClose();
        test110PostClose();
        test100Post();
        test110PostClose();
        test100Post();
    }

    public void test200Get() throws MalformedURLException, IOException
    {
        URL url = new URL(TestEnv._protocol + TestEnv.TEST_URL_IIS + _getForm);
        int response = 0;

        _urlCon = (HttpURLConnection)url.openConnection();
        _urlCon.connect();
        response = _urlCon.getResponseCode();
        assertEquals(200, response);

        String reply = getReply(_urlCon);
        iisCheckReply(reply);
    }

    public void test205MultiGet() throws MalformedURLException, IOException
    {
        test200Get();
        test200Get();
        test200Get();
    }

    public void test300GetBadCred() throws MalformedURLException, IOException
    {
        TestUserAgent._type = TestUserAgent.BAD;

        URL url = new URL(TestEnv._protocol
            + TestEnv.TEST_URL_IIS
            + "TestForm3.asp");
        int response = 0;

        _urlCon = (HttpURLConnection)url.openConnection();
        _urlCon.disconnect();
        _urlCon.connect();
        response = _urlCon.getResponseCode();
        assertEquals(401, response);

        // Make sure it works correctly with a good URL, after
        // a bad one, that is, it re-requests the credential
        TestUserAgent._type = TestUserAgent.GOOD;

        _urlCon = (HttpURLConnection)url.openConnection();
        _urlCon.connect();
        response = _urlCon.getResponseCode();
        assertEquals(200, response);

        String reply = getReply(_urlCon);
        iisCheckReply(reply);
    }

    public void test305GetNullCred() throws MalformedURLException, IOException
    {
        TestUserAgent._type = TestUserAgent.NULL;

        URL url = new URL(TestEnv._protocol
            + TestEnv.TEST_URL_IIS
            + "TestForm3.asp");
        int response = 0;

        _urlCon = (HttpURLConnection)url.openConnection();
        _urlCon.disconnect();
        _urlCon.connect();
        response = _urlCon.getResponseCode();
        assertEquals(401, response);

        TestUserAgent._type = TestUserAgent.GOOD;
    }

    public void test400MultiGetPost() throws MalformedURLException, IOException
    {
        test200Get();
        test100Post();
        test200Get();
        test100Post();
        test200Get();
        test100Post();
    }

    public void allTestMethods() throws Exception
    {
        test100Post();
        test110PostClose();
        test120MultiPostClose();
        test200Get();
        test205MultiGet();
        test300GetBadCred();
        test305GetNullCred();
        test400MultiGetPost();
    }

    public void testBadEncoding()
    {
        try
        {
            // Bad value
            com.oaklandsw.http.HttpURLConnection.setNtlmPreferredEncoding(18);
            fail("Expected exception");
        }
        catch (IllegalArgumentException ex)
        {

        }

    }

    // Test everything with NTLM OEM response forced
    public void testForceOem() throws Exception
    {
        com.oaklandsw.http.HttpURLConnection
                .setNtlmPreferredEncoding(com.oaklandsw.http.HttpURLConnection.NTLM_ENCODING_OEM);
        allTestMethods();
        com.oaklandsw.http.HttpURLConnection
                .setNtlmPreferredEncoding(com.oaklandsw.http.HttpURLConnection.NTLM_ENCODING_UNICODE);
    }


    // Test test force NTLM V1
    public void testForceNtlmV1() throws Exception
    {
        NegotiateMessage._testForceV1 = true;
        allTestMethods();
        NegotiateMessage._testForceV1 = false;
    }

}
