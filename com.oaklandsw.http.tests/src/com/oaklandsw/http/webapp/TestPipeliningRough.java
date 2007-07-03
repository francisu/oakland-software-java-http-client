/*
 * Copyright 2007 Oakland Software Incorporated. All rights Reserved.
 */
package com.oaklandsw.http.webapp;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.HttpURLConnectInternal;
import com.oaklandsw.http.HttpURLConnection;

/**
 * Tests pipelining with options that have a great deal more retrying than the
 * standard configuration because the observed max connection limit is not used.
 */
public class TestPipeliningRough extends TestPipelining
{

    public TestPipeliningRough(String testName)
    {
        super(testName);

        // Use only one connection at a time
        _pipelineOptions = HttpURLConnection.PIPE_PIPELINE;
        
        //_logging = true;
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestPipeliningRough.class);
        return suite;
    }

    public void setUp() throws Exception
    {
        super.setUp();
        
        // Makes things harsher
        HttpURLConnectInternal._ignoreObservedMaxCount = true;
    }

}
