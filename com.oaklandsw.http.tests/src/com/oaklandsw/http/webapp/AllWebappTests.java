package com.oaklandsw.http.webapp;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.TestCaseBase;

public class AllWebappTests extends TestCaseBase
{

    public AllWebappTests(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(AllWebappTests.class.getName());
        suite.addTest(TestTimeout.suite());
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
        suite.addTest(TestFtpProxy.suite());
        suite.addTest(TestSSL.suite());
        return suite;
    }

    public static void main(String args[])
    {
        mainRun(suite(), args);
    }

}
