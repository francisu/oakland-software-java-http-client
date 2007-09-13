package com.oaklandsw.http.webapp;

import java.io.OutputStream;
import java.net.URL;

import org.apache.commons.logging.Log;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.HttpTestEnv;
import com.oaklandsw.http.HttpURLConnection;
import com.oaklandsw.http.TestUserAgent;
import com.oaklandsw.http.ntlm.Ntlm;
import com.oaklandsw.http.servlet.ParamServlet;
import com.oaklandsw.util.LogUtils;

/**
 * Test with the JCIFS filter.
 * 
 * This tests both with the unmodified JCIFS filter and with the Oakland
 * Software version that supports NTLMv2
 */
public class TestJCIFS extends TestWebappBase
{
    private static final Log _log = LogUtils.makeLogger();

    protected boolean        _threadFailed;

    public TestJCIFS(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestJCIFS.class);
        return suite;
    }

    public static void main(String args[])
    {
        mainRun(suite(), args);
    }

    public void setUp() throws Exception
    {
        super.setUp();
        TestUserAgent._type = TestUserAgent.GOOD;
        _showStats = true;
    }

    public void tearDown() throws Exception
    {
        super.tearDown();
        Ntlm.init();
    }

    public void testPostMethod(int agentType, String servlet) throws Exception
    {
        URL url = new URL(_urlBase + servlet);

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("POST");
        urlCon.setDoOutput(true);
        byte[] output = TestOutputStream.QUOTE.getBytes("ASCII");
        OutputStream outStr = urlCon.getOutputStream();
        outStr.write(output);
        outStr.close();

        int response = urlCon.getResponseCode();

        check:
        {
            if (response == 401 && agentType == TestUserAgent.BAD)
                break check;

            // 500 is when the max connections have exceeded
            assertTrue(response == 200 || response == 500);

            String reply = getReply(urlCon);
            if (response == 200)
            {
                assertTrue(checkReplyNoAssert(reply,
                                              "<title>Param Servlet: POST</title>"));
                assertTrue(checkReplyNoAssert(reply, "It was the best of times"));
            }
            else if (response == 500)
            {
                assertTrue(checkReplyNoAssert(reply, "no more connections"));
            }
        }

    }

    // Bug 1946 - make sure JCIFS "server" (really the servet filter) is
    // supported
    public void testGetMethodParameters(int agentType, String servlet)
        throws Exception
    {
        //logAll();
        URL url = new URL(_urlBase + servlet + "?param-one=param-value");
        int response = 0;

        TestUserAgent._type = agentType;

        // logAll();

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        response = urlCon.getResponseCode();

        check:
        {
            if (response == 401 && agentType == TestUserAgent.BAD)
                break check;

            // 500 is when the max connections have exceeded
            assertTrue(response == 200 /*|| response == 500*/);

            String reply = getReply(urlCon);
            if (response == 200)
            {
                assertTrue(checkReplyNoAssert(reply,
                                              "<title>Param Servlet: GET</title>"));
                assertTrue(checkReplyNoAssert(reply,
                                              "<p>QueryString=\"param-one=param-value\"</p>"));
                assertTrue(checkReplyNoAssert(reply, "<p>Parameters</p>\r\n"
                    + "name=\"param-one\";value=\"param-value\"<br>"));
            }
            else if (false && response == 500)
            {
                assertTrue(checkReplyNoAssert(reply, "no more connections"));
            }
        }

        urlCon.disconnect();
        //checkNoTotalConns(url);
    }

    public void testNormal() throws Exception
    {
        testGetMethodParameters(TestUserAgent.GOOD, ParamServlet.NAME_NTLM);
    }

    public void testNormalPost() throws Exception
    {
        testPostMethod(TestUserAgent.GOOD, ParamServlet.NAME_NTLM);
    }

    public void testBad() throws Exception
    {
        testGetMethodParameters(TestUserAgent.BAD, ParamServlet.NAME_NTLM);
    }

    public void testBadPost() throws Exception
    {
        testPostMethod(TestUserAgent.BAD, ParamServlet.NAME_NTLM);
    }

    public static final boolean FORCE = true;

    protected void noDomain2Normal() throws Exception
    {
        URL url = new URL(_urlBase + ParamServlet.NAME_NTLM2);

        TestUserAgent._type = TestUserAgent.NO_DOMAIN;

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        assertEquals(401, urlCon.getResponseCode());
        assertContains(urlCon.getResponseMessage(), "Domain name");
    }

    public void test2NormalForce2() throws Exception
    {
        // logAll();
        Ntlm.forceV2();
        testGetMethodParameters(TestUserAgent.GOOD, ParamServlet.NAME_NTLM2);
    }

    public void test2NormalForce2Oaklandswtest() throws Exception
    {
        // This test does not pass with a win2k3 server set with SMB signing
        // signing is turned off on the win2k3 server
        //logAll();
        Ntlm.forceV2();
        for (int i = 1; i < 10; i++)
        {
            testGetMethodParameters(TestUserAgent.OAKLANDSWTEST_DOMAIN,
                                    ParamServlet.NAME_NTLM2);
        }
    }

    public void test2NormalPostForce2() throws Exception
    {
        Ntlm.forceV2();
        testPostMethod(TestUserAgent.GOOD, ParamServlet.NAME_NTLM2);
    }

    public void test2NormalPostForce2Loop() throws Exception
    {
        Ntlm.forceV2();
        //logAll();
        for (int i = 1; i < 10; i++)
            testPostMethod(TestUserAgent.GOOD, ParamServlet.NAME_NTLM2);
    }

    public void test2NormalForce2NoDomain() throws Exception
    {
        Ntlm.forceV2();
        noDomain2Normal();
    }

    public void test2BadForce2() throws Exception
    {
        Ntlm.forceV2();
        testGetMethodParameters(TestUserAgent.BAD, ParamServlet.NAME_NTLM2);
    }

    public void test2NormalForce1() throws Exception
    {
        // Don't do this if only NTLM v2 is available
        if (HttpTestEnv.REQUIRE_NTLMV2 == null)
        {
            Ntlm.forceV1();
            // This fails if the target IIS server is configured to require
            // only NTLMv2 authentication
            testGetMethodParameters(TestUserAgent.GOOD, ParamServlet.NAME_NTLM2);
        }
    }

    public void test2NormalForce1NoDomain() throws Exception
    {
        Ntlm.forceV1();
        noDomain2Normal();
    }

    public void test2BadForce1() throws Exception
    {
        Ntlm.forceV1();
        testGetMethodParameters(TestUserAgent.BAD, ParamServlet.NAME_NTLM2);
    }

    public void test2Normal() throws Exception
    {
        testGetMethodParameters(TestUserAgent.GOOD, ParamServlet.NAME_NTLM2);
    }

    public void test2NormalNoDomain() throws Exception
    {
        noDomain2Normal();
    }

    public void test2Bad() throws Exception
    {
        testGetMethodParameters(TestUserAgent.BAD, ParamServlet.NAME_NTLM2);
    }

    public void allTestMethods() throws Exception
    {
        testNormal();
        testNormalPost();
        testBad();
        testBadPost();
    }

}
