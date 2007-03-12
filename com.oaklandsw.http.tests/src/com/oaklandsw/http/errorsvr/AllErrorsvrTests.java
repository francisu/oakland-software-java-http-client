package com.oaklandsw.http.errorsvr;

import com.oaklandsw.TestCaseBase;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllErrorsvrTests extends TestCaseBase
{

    public AllErrorsvrTests(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(AllErrorsvrTests.class.getName());
        suite.addTest(TestIdleTimeouts.suite());
        suite.addTest(TestError.suite());
        suite.addTest(TestDisconnect.suite());
        suite.addTest(TestTimeout.suite());
        suite.addTest(TestData.suite());
        suite.addTest(TestStatusLine.suite());
        return suite;
    }

    public static void main(String args[])
    {
        mainRun(suite(), args);
    }

}
