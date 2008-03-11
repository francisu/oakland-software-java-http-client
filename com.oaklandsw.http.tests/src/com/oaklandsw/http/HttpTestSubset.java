package com.oaklandsw.http;

import com.oaklandsw.http.cookie.AllCookieTests;
import com.oaklandsw.http.errorsvr.AllErrorsvrTests;
import com.oaklandsw.http.errorsvr.TestStatusLine;
import com.oaklandsw.http.errorsvr.TestTimeout;
import com.oaklandsw.http.local.TestConnectionManagerLocal;
import com.oaklandsw.http.local.TestDefaults;
import com.oaklandsw.http.webapp.TestHeaders;
import com.oaklandsw.http.local.AllLocalTests;
import com.oaklandsw.http.local.TestHttpStatus;
import com.oaklandsw.http.local.TestNtlmMessages;
import com.oaklandsw.http.local.TestRequestHeaders;
import com.oaklandsw.http.local.TestResponseHeaders;
import com.oaklandsw.http.local.TestStreams;
import com.oaklandsw.http.local.TestURIUtil;
import com.oaklandsw.http.webapp.TestAuthType;
import com.oaklandsw.http.webapp.TestAxis1;
import com.oaklandsw.http.webapp.TestAxis2;
import com.oaklandsw.http.webapp.TestBasicAuth;
import com.oaklandsw.http.webapp.TestCookie;
import com.oaklandsw.http.webapp.TestDisconnect;
import com.oaklandsw.http.webapp.TestExplicitConnection;
import com.oaklandsw.http.webapp.TestFailover;
import com.oaklandsw.http.webapp.TestFtpProxy;
import com.oaklandsw.http.webapp.TestIIS;
import com.oaklandsw.http.webapp.TestJCIFS;
import com.oaklandsw.http.webapp.TestMultiThread;
import com.oaklandsw.http.webapp.TestNoData;
import com.oaklandsw.http.webapp.TestOutputStream;
import com.oaklandsw.http.webapp.TestOutputStreamChunked;
import com.oaklandsw.http.webapp.TestOutputStreamFixed;
import com.oaklandsw.http.webapp.TestOutputStreamRaw;
import com.oaklandsw.http.webapp.TestParameters;
import com.oaklandsw.http.webapp.TestPipelining;
import com.oaklandsw.http.webapp.TestPipeliningRough;
import com.oaklandsw.http.webapp.TestRedirect;
import com.oaklandsw.http.webapp.TestTunneling;
import com.oaklandsw.http.webapp.TestWebStart;
import com.oaklandsw.http.webext.TestBugs;
import com.oaklandsw.http.webext.TestHttps;
import com.oaklandsw.http.webext.TestMethods;
import com.oaklandsw.http.webext.TestSSL;
import com.oaklandsw.http.webserver.TestBasicAndDigestAuth;
import com.oaklandsw.http.webserver.TestWebDavMethods;

import junit.framework.Test;
import junit.framework.TestSuite;

public class HttpTestSubset extends HttpTestBase
{

    public HttpTestSubset(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(HttpTestSubset.class.getName());

        // Local tests must run first
        // suite.addTest(AllLocalTests.suite());

        // Must be called before the HttpURLConnection static init
        // com.oaklandsw.http.local.TestProperties.setProperties();

        // This test must run first
        // suite.addTest(TestProperties.suite());

        if (false)
        {
            suite.addTest(TestFailover.suite());
            suite.addTest(TestAxis1.suite());
            suite.addTest(TestAxis2.suite());
            suite.addTest(TestIIS.suite());
            suite.addTest(TestJCIFS.suite());
            suite.addTest(TestExplicitConnection.suite());
            suite.addTest(TestMethods.suite());
            suite.addTest(TestAuthType.suite());
            suite.addTest(TestPipelining.suite());
            suite.addTest(TestPipeliningRough.suite());
            suite.addTest(TestOutputStream.suite());
            suite.addTest(TestOutputStreamChunked.suite());
            suite.addTest(TestOutputStreamFixed.suite());
            suite.addTest(TestOutputStreamRaw.suite());
            suite.addTest(TestMultiThread.suite());
            suite.addTest(TestParameters.suite());
            suite.addTest(TestHeaders.suite());
            suite.addTest(TestRedirect.suite());
            suite.addTest(TestBasicAuth.suite());
            suite.addTest(TestCookie.suite());
            suite.addTest(TestNoData.suite());
            suite.addTest(TestDisconnect.suite());
            suite.addTest(TestWebStart.suite());
            suite.addTest(TestTunneling.suite());
        }

        suite.addTest(AllLocalTests.suite());

        suite.addTest(TestLicense.suite());

        suite.addTest(AllCookieTests.suite());
        suite.addTest(AllErrorsvrTests.suite());

        suite.addTest(TestFtpProxy.suite());

        if (false)
        {
            suite.addTest(TestOutputStreamRaw.suite());
            suite.addTest(TestAxis1.suite());
            suite.addTest(TestAxis2.suite());
            suite.addTest(TestRedirect.suite());
            suite.addTest(TestIIS.suite());
            suite.addTest(TestCookie.suite());
            suite.addTest(TestHttps.suite());

            suite.addTest(TestBasicAndDigestAuth.suite());
            suite.addTest(TestSSL.suite());
            suite.addTest(TestMethods.suite());

            suite.addTest(TestHttpStatus.suite());
            suite.addTest(TestDefaults.suite());
            suite.addTest(LocalTestAuthenticator.suite());
            suite.addTest(TestConnectionManagerLocal.suite());
            suite.addTest(TestURIUtil.suite());

            suite.addTest(TestResponseHeaders.suite());
            suite.addTest(TestRequestHeaders.suite());
            suite.addTest(TestStreams.suite());
            suite.addTest(TestNtlmMessages.suite());
            suite.addTest(TestHeaders.suite());
            suite.addTest(TestStatusLine.suite());
            suite.addTest(TestHttps.suite());
            suite.addTest(TestLicense.suite());
            suite.addTest(AllCookieTests.suite());
            suite.addTest(TestTimeout.suite());
            suite.addTest(TestAxis1.suite());
            suite.addTest(TestAxis2.suite());
            suite.addTest(TestPipelining.suite());
            suite.addTest(TestRedirect.suite());
            suite.addTest(TestBasicAuth.suite());
            suite.addTest(TestIIS.suite());
            suite.addTest(TestJCIFS.suite());
            suite.addTest(TestMultiThread.suite());
            suite.addTest(TestOutputStreamChunked.suite());
            suite.addTest(TestAuthType.suite());
            suite.addTest(TestWebDavMethods.suite());
            suite.addTest(TestBugs.suite());
            suite.addTest(TestHttps.suite());
            suite.addTest(TestMethods.suite());
            suite.addTest(TestTimeout.suite());
            suite.addTest(AllErrorsvrTests.suite());
            suite.addTest(TestNoData.suite());
        }

        return suite;
    }

    public static void main(String args[])
    {
        mainRun(suite(), args);
    }
}
