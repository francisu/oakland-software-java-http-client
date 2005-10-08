package com.oaklandsw.http.errorsvr;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TestAll extends TestCase
{

    public TestAll(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite();
        suite.addTest(TestError.suite());
        suite.addTest(TestDisconnect.suite());
        suite.addTest(TestTimeout.suite());
        suite.addTest(TestData.suite());
        suite.addTest(TestStatusLine.suite());
        return suite;
    }

    public static void main(String args[])
    {
        String[] testCaseName = { TestAll.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }

}
