package com.oaklandsw.http.webapp;

import com.oaklandsw.util.Log;

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

    protected PipelineTester _pt;

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
        _pt = null;
        // logAll();
    }

    protected static final boolean PREEMPTIVE = true;

    public void testPipeliningSimple(int number, Class expectedEx)
        throws Exception
    {
        testPipeliningSimpleSetup(number, expectedEx);
        testPipeliningSimpleRun(number, expectedEx);
    }

    public void testPipeliningSimpleSetup(int number, Class expectedEx)
        throws Exception
    {
        if (number == 0)
        {
        }
        else
        {
            // Net proxy cannot deal with pipelining
            if (_inAuthCloseProxyTest)
                return;

            _pt = new PipelineTester(_urlBase + ParamServlet.NAME,
                                     number,
                                     _pipelineOptions,
                                     _pipelineMaxDepth);
        }
    }

    public void testPipeliningSimpleRun(int number, Class expectedEx)
        throws Exception
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

            if (expectedEx != null)
                _pt._checkResult = false;
            boolean result = _pt.runTest();
            if (expectedEx != null)
                assertEquals(expectedEx, _pt._failException.getClass());
            else
                assertFalse(result);
        }
    }

    // Check that having the auth type set will work even
    // though there is no authentication
    protected void testAuthenticationTypeSet(int type, int count)
        throws Exception
    {
        testAuthenticationTypeSet(type, count, null);
    }

    // Check that having the auth type set will work even
    // though there is no authentication
    protected void testAuthenticationTypeSet(int type,
                                             int count,
                                             Class expectedException)
        throws Exception
    {
        TestUserAgent._type = TestUserAgent.GOOD;
        HttpURLConnection.setDefaultAuthenticationType(type);
        testPipeliningSimple(count, expectedException);
    }

    public void testAuthenticationTypeSetBasic() throws Exception
    {
        testAuthenticationTypeSet(Credential.AUTH_BASIC, 0);
    }

    public void testAuthenticationTypeSetDigest() throws Exception
    {
        testAuthenticationTypeSet(Credential.AUTH_DIGEST, 0);
    }

    public void testAuthenticationTypeSetNtlm() throws Exception
    {
        testAuthenticationTypeSet(Credential.AUTH_NTLM, 0);
    }

    public void testAuthenticationTypeSetNtlmPost() throws Exception
    {
        TestUserAgent._type = TestUserAgent.GOOD;
        HttpURLConnection.setDefaultAuthenticationType(Credential.AUTH_NTLM);
        doGetLikeMethod("POST", CHECK_CONTENT);
    }

    public void testAuthenticationTypeSetBasic1p() throws Exception
    {
        testAuthenticationTypeSet(Credential.AUTH_BASIC, 1);
    }

    public void testAuthenticationTypeSetDigest1p() throws Exception
    {
        // We don't allow digest with pipelining
        TestUserAgent._type = TestUserAgent.GOOD;
        HttpURLConnection.setDefaultAuthenticationType(Credential.AUTH_DIGEST);
        testPipeliningSimpleSetup(1, IllegalStateException.class);
        if (_pt != null)
        {
            _pt._ignoreFailType = 8;
            testPipeliningSimpleRun(1, IllegalStateException.class);
        }
    }

    public void testAuthenticationTypeSetNtlm1p() throws Exception
    {
        testAuthenticationTypeSet(Credential.AUTH_NTLM, 1);
    }

    public void testAuthenticationTypeSetNtlm5() throws Exception
    {
        testAuthenticationTypeSet(Credential.AUTH_NTLM, 5);
    }

    public void testAuthenticationTypeSetBasicPre() throws Exception
    {
        testAuthenticationTypeSet(Credential.AUTH_BASIC, 0);
    }

    public void testAuthenticationTypeSetBasic1pPre() throws Exception
    {
        testAuthenticationTypeSet(Credential.AUTH_BASIC, 1);
    }

    public void testAuthenticationTypeSetBasic5Pre() throws Exception
    {
        testAuthenticationTypeSet(Credential.AUTH_BASIC, 5);
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
