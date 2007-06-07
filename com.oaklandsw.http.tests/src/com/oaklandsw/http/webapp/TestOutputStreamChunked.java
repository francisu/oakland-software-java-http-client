/*
 * Copyright 2007 Oakland Software Incorporated. All rights Reserved.
 */
package com.oaklandsw.http.webapp;

import junit.framework.Test;
import junit.framework.TestSuite;

public class TestOutputStreamChunked extends TestOutputStream
{

    public TestOutputStreamChunked(String testName)
    {
        super(testName);
        _streamingType = STREAM_CHUNKED;

        // 1.0 (Squid proxy) does not support chunked encoding, it gets a 501
        // error
        _do10ProxyTest = false;
        
        // Netproxy messes this up as well
        _doAuthCloseProxyTest = false;
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestOutputStreamChunked.class);
        return suite;
    }

}
