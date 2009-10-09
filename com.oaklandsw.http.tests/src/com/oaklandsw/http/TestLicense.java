package com.oaklandsw.http;

import com.oaklandsw.TestCaseBase;

import com.oaklandsw.license.License;

import junit.framework.Test;
import junit.framework.TestSuite;


/**
 * Tests the HTTP license code
 */
public class TestLicense extends TestCaseBase {
    public TestLicense(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(TestLicense.class);

        return suite;
    }

    public static void main(String[] args) {
        mainRun(suite(), args);
    }

    public void testLicense() {
        // Make sure it's statically initialized
        HttpURLConnection.getDefaultConnectionTimeout();
        assertTrue(HttpLicenseCheck._licenseType == License.LIC_NORMAL);
    }
}
