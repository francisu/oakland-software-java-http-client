package com.oaklandsw.http.webext;

import junit.framework.Test;
import junit.framework.TestSuite;


// Bug 1980 non-proxy host does not work on per-connection basis
// It actually should not work on a per-connection proxy setting basis,
// this test verifies that behavior
public class TestNonProxyHostConnection extends TestNonProxyHost {
    public TestNonProxyHostConnection(String testName) {
        super(testName);
        _perConnection = true;
    }

    public static void main(String[] args) {
        mainRun(suite(), args);
    }

    public static Test suite() {
        return new TestSuite(TestNonProxyHostConnection.class);
    }
}
