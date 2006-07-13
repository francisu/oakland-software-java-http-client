
package com.oaklandsw.http;

import com.oaklandsw.license.License;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests the HTTP license code
 */
public class TestLicense extends TestCase
{

    public TestLicense(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite();
        suite.addTest(new TestSuite(TestLicense.class));
        return suite;
    }

    public static void main(String args[])
    {
        String[] testCaseName = { TestLicense.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }

    public void testLicense()
    {
        assertTrue(HttpURLConnection._licenseType == License.LIC_NORMAL);
    }

}
