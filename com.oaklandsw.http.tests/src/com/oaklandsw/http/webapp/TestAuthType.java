package com.oaklandsw.http.webapp;

import org.apache.commons.logging.Log;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.Credential;
import com.oaklandsw.http.HttpURLConnection;
import com.oaklandsw.http.PipelineTester;
import com.oaklandsw.http.TestUserAgent;
import com.oaklandsw.http.servlet.ParamServlet;
import com.oaklandsw.util.LogUtils;

public class TestAuthType extends TestWebappBase
{

    private static final Log _log = LogUtils.makeLogger();

    public TestAuthType(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestAuthType.class);
        return suite;
    }

    public static void main(String args[])
    {
        mainRun(suite(), args);
    }
    
    public void setUp() throws Exception
    {
        super.setUp();
        //logAll();
    }

    protected static final boolean PREEMPTIVE = true;

    public void testPipeliningSimple(int number) throws Exception
    {
        if (number == 0)
        {
            // No pipelining
            doGetLikeMethod("GET", CHECK_CONTENT);
        }
        else
        {
            // Net proxy cannot deal with pipelining
            if (_inAuthCloseProxyTest)
                return;

            PipelineTester pt = new PipelineTester(_urlBase + ParamServlet.NAME,
                                                   number,
                                                   _pipelineOptions,
                                                   _pipelineMaxDepth);
            assertFalse(pt.runTest());
        }
    }

    // Check that having the auth type set will work even
    // though there is no authentication
    protected void testAuthenticationTypeSet(int type,
                                             boolean preemptive,
                                             int count) throws Exception
    {
        TestUserAgent._type = TestUserAgent.GOOD;
        HttpURLConnection.setDefaultAuthenticationType(type);
        testPipeliningSimple(count);
    }

    public void testAuthenticationTypeSetBasic() throws Exception
    {
        testAuthenticationTypeSet(Credential.AUTH_BASIC, !PREEMPTIVE, 0);
    }

    public void testAuthenticationTypeSetDigest() throws Exception
    {
        testAuthenticationTypeSet(Credential.AUTH_DIGEST, !PREEMPTIVE, 0);
    }

    public void testAuthenticationTypeSetNtlm() throws Exception
    {
        testAuthenticationTypeSet(Credential.AUTH_NTLM, !PREEMPTIVE, 0);
    }

    public void testAuthenticationTypeSetNtlmPost() throws Exception
    {
        TestUserAgent._type = TestUserAgent.GOOD;
        HttpURLConnection.setDefaultAuthenticationType(Credential.AUTH_NTLM);
        doGetLikeMethod("POST", CHECK_CONTENT);
    }

    public void testAuthenticationTypeSetBasic1p() throws Exception
    {
        testAuthenticationTypeSet(Credential.AUTH_BASIC, !PREEMPTIVE, 1);
    }

    public void testAuthenticationTypeSetDigest1p() throws Exception
    {
        // We don't allow digest with pipelining
        try
        {
            testAuthenticationTypeSet(Credential.AUTH_DIGEST, !PREEMPTIVE, 1);
            if (!_inAuthCloseProxyTest)
                fail("Did not get expected exception");
        }
        catch (IllegalStateException ex)
        {
            // expected
        }
    }

    public void testAuthenticationTypeSetNtlm1p() throws Exception
    {
        testAuthenticationTypeSet(Credential.AUTH_NTLM, !PREEMPTIVE, 1);
    }

    public void testAuthenticationTypeSetNtlm5() throws Exception
    {
        testAuthenticationTypeSet(Credential.AUTH_NTLM, !PREEMPTIVE, 5);
    }

    public void testAuthenticationTypeSetBasicPre() throws Exception
    {
        testAuthenticationTypeSet(Credential.AUTH_BASIC, PREEMPTIVE, 0);
    }

    public void testAuthenticationTypeSetBasic1pPre() throws Exception
    {
        testAuthenticationTypeSet(Credential.AUTH_BASIC, PREEMPTIVE, 1);
    }

    public void testAuthenticationTypeSetBasic5Pre() throws Exception
    {
        testAuthenticationTypeSet(Credential.AUTH_BASIC, PREEMPTIVE, 5);
    }

    public void allTestMethods() throws Exception
    {
        testAuthenticationTypeSetBasic();
        testAuthenticationTypeSetDigest();
        testAuthenticationTypeSetNtlm();
        testAuthenticationTypeSetBasic1p();
        testAuthenticationTypeSetDigest1p();
        testAuthenticationTypeSetNtlm1p();
        testAuthenticationTypeSetNtlm5();
        testAuthenticationTypeSetBasicPre();
        testAuthenticationTypeSetBasic1pPre();
        testAuthenticationTypeSetBasic5Pre();
    }

}
