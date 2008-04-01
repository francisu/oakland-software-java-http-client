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
    private static final Log _log                   = LogUtils.makeLogger();

    protected boolean        _threadFailed;

    static final String      NTLM_URL_JCIFS         = _urlBase
                                                        + ParamServlet.NAME_NTLM;
    static final String      NTLM_URL_JCIFS_OAK_0   = HttpTestEnv.TEST_URL_HOST_WEBAPP_2
                                                        + HttpTestEnv.TEST_URL_APP_TOMCAT_2
                                                        + ParamServlet.NAME_NTLM2_0;
    static final String      NTLM_URL_JCIFS_OAK_5   = HttpTestEnv.TEST_URL_HOST_WEBAPP_3
                                                        + HttpTestEnv.TEST_URL_APP_TOMCAT_3
                                                        + ParamServlet.NAME_NTLM2_5;

    // Alternate implementation (repoman) - this is not a win server 2003 machine
    // so it has connection limitation issues
    static final String      NTLM_URL_JCIFS_OAK_5_2 = HttpTestEnv.TEST_URL_HOST_WEBAPP_4
                                                        + HttpTestEnv.TEST_URL_APP_TOMCAT_4
                                                        + ParamServlet.NAME_NTLM2_5;

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
        // Resets NTLM state to normal
        Ntlm.init();
    }

    public void tearDown() throws Exception
    {
        super.tearDown();
        Ntlm.init();
    }

    public void testPostMethod(int agentType, String urlStr) throws Exception
    {
        URL url = new URL(urlStr);

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
    public void testGetMethodParameters(int agentType, String urlStr)
        throws Exception
    {
        testGetMethodParameters(agentType, urlStr, 200);
    }
        
        // Bug 1946 - make sure JCIFS "server" (really the servet filter) is
    // supported
    public void testGetMethodParameters(int agentType, String urlStr, int checkResponse)
        throws Exception
    {
        URL url = new URL(urlStr + "?param-one=param-value");
        int response = 0;

        TestUserAgent._type = agentType;

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        response = urlCon.getResponseCode();

        check:
        {
            if (response == 401 && agentType == TestUserAgent.BAD)
                break check;

            assertTrue(response == checkResponse);

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
        // checkNoTotalConns(url);
    }

    public void testNormal() throws Exception
    {
        testGetMethodParameters(TestUserAgent.GOOD, NTLM_URL_JCIFS);
    }

    public void testNormalPost() throws Exception
    {
        testPostMethod(TestUserAgent.GOOD, NTLM_URL_JCIFS);
    }

    public void testBad() throws Exception
    {
        testGetMethodParameters(TestUserAgent.BAD, NTLM_URL_JCIFS);
    }

    public void testBadPost() throws Exception
    {
        testPostMethod(TestUserAgent.BAD, NTLM_URL_JCIFS);
    }

    public void test2Normal_0() throws Exception
    {
        testGetMethodParameters(TestUserAgent.GOOD, NTLM_URL_JCIFS_OAK_0);
    }

    public void test2Normal_5() throws Exception
    {
        testGetMethodParameters(TestUserAgent.GOOD, NTLM_URL_JCIFS_OAK_5);
    }

    public void test2Normal_5_2() throws Exception
    {
        testGetMethodParameters(TestUserAgent.GOOD, NTLM_URL_JCIFS_OAK_5_2);
    }

    public void test2NormalForce2_0() throws Exception
    {
        Ntlm.forceV2();
        testGetMethodParameters(TestUserAgent.GOOD, NTLM_URL_JCIFS_OAK_0);
    }

    public void test2NormalForce2_5() throws Exception
    {
        Ntlm.forceV2();
        testGetMethodParameters(TestUserAgent.GOOD, NTLM_URL_JCIFS_OAK_5);
    }

    public void test2NormalForce2_5_2() throws Exception
    {
        Ntlm.forceV2();
        testGetMethodParameters(TestUserAgent.GOOD, NTLM_URL_JCIFS_OAK_5_2);
    }

    public void test2NormalForce1_0() throws Exception
    {
        Ntlm.forceV1();
        testGetMethodParameters(TestUserAgent.GOOD, NTLM_URL_JCIFS_OAK_0);
    }

    public void test2NormalForce1_5() throws Exception
    {
        Ntlm.forceV1();
        // Should fail
        // This fails if the target IIS server is configured to require
        // only NTLMv2 authentication
        testGetMethodParameters(TestUserAgent.GOOD, NTLM_URL_JCIFS_OAK_5, 401);
    }

    public void test2NormalForce2Oaklandswtest() throws Exception
    {
        // This test does not pass with a win2k3 server set with SMB signing
        // signing is turned off on the win2k3 server
        Ntlm.forceV2();
        for (int i = 1; i < 10; i++)
        {
            testGetMethodParameters(TestUserAgent.OAKLANDSWTEST_DOMAIN,
                                    NTLM_URL_JCIFS_OAK_5);
        }
    }

    public void test2NormalPostForce2() throws Exception
    {
        Ntlm.forceV2();
        testPostMethod(TestUserAgent.GOOD, NTLM_URL_JCIFS_OAK_5);
    }

    public void test2NormalPostForce2Loop() throws Exception
    {
        Ntlm.forceV2();
        for (int i = 1; i < 10; i++)
            testPostMethod(TestUserAgent.GOOD, NTLM_URL_JCIFS_OAK_5);
    }

    public void test2BadForce2() throws Exception
    {
        Ntlm.forceV2();
        testGetMethodParameters(TestUserAgent.BAD, NTLM_URL_JCIFS_OAK_5);
    }

    public void test2BadForce1() throws Exception
    {
        Ntlm.forceV1();
        testGetMethodParameters(TestUserAgent.BAD, NTLM_URL_JCIFS_OAK_5);
    }

    public void test2Bad_0() throws Exception
    {
        testGetMethodParameters(TestUserAgent.BAD, NTLM_URL_JCIFS_OAK_0);
    }

    public void test2Bad_5() throws Exception
    {
        testGetMethodParameters(TestUserAgent.BAD, NTLM_URL_JCIFS_OAK_5);
    }

    public void allTestMethods() throws Exception
    {
        testNormal();
        testNormalPost();
        
        test2Normal_0();
        test2Normal_5();
        
        test2NormalForce2_0();
        Ntlm.init();
        test2NormalForce2_5();
        Ntlm.init();
        
        testBad();
        testBadPost();
    }

}
