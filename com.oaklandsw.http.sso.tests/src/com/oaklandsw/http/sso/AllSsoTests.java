package com.oaklandsw.http.sso;


import com.oaklandsw.TestCaseBase;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllSsoTests extends TestCaseBase
{

    public AllSsoTests(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(AllSsoTests.class.getName());

        suite.addTest(TestLocal.suite());
        return suite;
    }

    public static void main(String args[])
    {
        mainRun(suite(), args);
    }
}
