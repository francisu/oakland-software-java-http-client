/*
 * Copyright 2007 Oakland Software Incorporated. All rights Reserved.
 */
package com.oaklandsw.http.webapp;

import junit.framework.Test;
import junit.framework.TestSuite;

public class TestOutputStreamFixed extends TestOutputStream
{

    public TestOutputStreamFixed(String testName)
    {
        super(testName);
        _streamingType = STREAM_FIXED;
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestOutputStreamFixed.class);
        return suite;
    }


}
