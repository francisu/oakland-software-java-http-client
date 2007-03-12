package com.oaklandsw.http.webapp;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.TestCaseBase;
import com.oaklandsw.util.SystemUtils;

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
        // Axis requires 1.3
        if (SystemUtils.isJavaVersionAtLeast(1.3f))
            suite.addTest(TestAxis.suite());
        suite.addTest(TestIIS.suite());
        suite.addTest(TestExplicitConnection.suite());
        suite.addTest(TestMethods.suite());
        suite.addTest(TestMultiThread.suite());
        suite.addTest(TestParameters.suite());
        suite.addTest(TestHeaders.suite());
        suite.addTest(TestRedirect.suite());
        suite.addTest(TestBasicAuth.suite());
        suite.addTest(TestCookie.suite());
        suite.addTest(TestNoData.suite());
        suite.addTest(TestDisconnect.suite());
        return suite;
    }

    public static void main(String args[])
    {
        mainRun(suite(), args);
    }

}
