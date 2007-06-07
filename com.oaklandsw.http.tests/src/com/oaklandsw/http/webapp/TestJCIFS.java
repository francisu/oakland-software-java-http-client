package com.oaklandsw.http.webapp;

import java.net.URL;

import org.apache.commons.logging.Log;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.HttpURLConnection;
import com.oaklandsw.http.TestUserAgent;
import com.oaklandsw.http.servlet.ParamServlet;
import com.oaklandsw.util.LogUtils;

/**
 * Test with the JCIFS filter
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

    // Bug 1946 - make sure JCIFS "server" (really the servet filter) is
    // supported
    public void testGetMethodParameters(int agentType) throws Exception
    {
        URL url = new URL(_urlBase
            + ParamServlet.NAME_NTLM
            + "?param-one=param-value");
        int response = 0;

        TestUserAgent._type = agentType;

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("GET");
        urlCon.connect();
        response = urlCon.getResponseCode();

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
                                              "<title>Param Servlet: GET</title>"));
                assertTrue(checkReplyNoAssert(reply,
                                              "<p>QueryString=\"param-one=param-value\"</p>"));
                assertTrue(checkReplyNoAssert(reply, "<p>Parameters</p>\r\n"
                    + "name=\"param-one\";value=\"param-value\"<br>"));
            }
            else if (response == 500)
            {
                assertTrue(checkReplyNoAssert(reply, "no more connections"));
            }
        }

        urlCon.disconnect();
        checkNoTotalConns(url);
    }

    public void testNormal() throws Exception
    {
        testGetMethodParameters(TestUserAgent.GOOD);
    }

    public void testBad() throws Exception
    {
        testGetMethodParameters(TestUserAgent.BAD);
    }

    public void allTestMethods() throws Exception
    {
        testNormal();
        testBad();
    }

}
