package com.oaklandsw.http.webapp;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.TestBase;

public class TestAll extends TestBase
{

    public TestAll(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite();
        suite.addTest(TestTimeout.suite());
        suite.addTest(TestFailover.suite());
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
