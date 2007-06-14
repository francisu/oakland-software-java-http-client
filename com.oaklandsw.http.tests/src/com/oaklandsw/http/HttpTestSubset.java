package com.oaklandsw.http;

import com.oaklandsw.http.local.TestConnectionManagerLocal;
import com.oaklandsw.http.local.TestDefaults;
import com.oaklandsw.http.local.TestHeaders;
import com.oaklandsw.http.local.TestHttpStatus;
import com.oaklandsw.http.local.TestNtlmMessages;
import com.oaklandsw.http.local.TestRequestHeaders;
import com.oaklandsw.http.local.TestResponseHeaders;
import com.oaklandsw.http.local.TestStreams;
import com.oaklandsw.http.local.TestTimeout;
import com.oaklandsw.http.local.TestURIUtil;
import com.oaklandsw.http.webapp.TestBasicAuth;
import com.oaklandsw.http.webapp.TestRedirect;
import com.oaklandsw.http.webext.TestMethods;
import com.oaklandsw.http.webext.TestSSL;
import com.oaklandsw.http.webserver.TestBasicAndDigestAuth;

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

        suite.addTest(TestRedirect.suite());
        suite.addTest(TestBasicAuth.suite());
        suite.addTest(TestBasicAndDigestAuth.suite());
        suite.addTest(TestSSL.suite());
        suite.addTest(TestMethods.suite());

        if (false)
        {
            suite.addTest(TestHeaders.suite());
            suite.addTest(TestHttpStatus.suite());
            suite.addTest(TestDefaults.suite());
            suite.addTest(LocalTestAuthenticator.suite());
            suite.addTest(TestConnectionManagerLocal.suite());
            suite.addTest(TestURIUtil.suite());

            suite.addTest(TestResponseHeaders.suite());
            suite.addTest(TestRequestHeaders.suite());
            suite.addTest(TestStreams.suite());
            suite.addTest(TestNtlmMessages.suite());
            suite.addTest(TestTimeout.suite());
        }

        // suite.addTest(TestLicense.suite());

        // suite.addTest(AllCookieTests.suite());
        // suite.addTest(AllErrorsvrTests.suite());

        // suite.addTest(TestAxis1.suite());
        // suite.addTest(TestAxis2.suite());
        // suite.addTest(TestPipelining.suite());
        // suite.addTest(TestIIS.suite());
        // suite.addTest(TestJCIFS.suite());
        // suite.addTest(TestMultiThread.suite());
        // suite.addTest(TestOutputStreamChunked.suite());
        // suite.addTest(TestAuthType.suite());
        // suite.addTest(TestWebDavMethods.suite());
        // suite.addTest(TestBugs.suite());
        // suite.addTest(TestHttps.suite());
        // suite.addTest(TestMethods.suite());
        // suite.addTest(TestTimeout.suite());
        return suite;
    }

    public static void main(String args[])
    {
        mainRun(suite(), args);
    }
}
