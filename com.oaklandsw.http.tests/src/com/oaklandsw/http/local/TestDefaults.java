/*
 * Copyright 2004 oakland software, incorporated.  All rights Reserved.
 */
package com.oaklandsw.http.local;

import java.net.URL;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.HttpURLConnection;
import com.oaklandsw.http.TestBase;
import com.oaklandsw.log.Log;
import com.oaklandsw.log.LogFactory;

/**
 * Tests for default values
 */
public class TestDefaults extends TestBase
{
    
    private static final Log _log = LogFactory.getLog(TestDefaults.class);

    public TestDefaults(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestDefaults.class);
        return suite;
    }

    public static void main(String args[])
    {
        mainRun(suite(), args);
    }

    // 962 - per connection idle timeout does not work properly
    public void testDefaultConnectionTimeout()
    throws Exception
    {
        HttpURLConnection.setDefaultIdleConnectionTimeout(99);
        URL url = new URL("http://dummy");
        HttpURLConnection urlCon1 = HttpURLConnection.openConnection(url);
        assertEquals(99, urlCon1.getIdleConnectionTimeout());
        urlCon1.setIdleConnectionTimeout(10);

        HttpURLConnection.setDefaultIdleConnectionTimeout(99);
        HttpURLConnection urlCon2 = HttpURLConnection.openConnection(url);
        assertEquals(99, urlCon2.getIdleConnectionTimeout());

        // Verify the first connection did not change
        assertEquals(10, urlCon1.getIdleConnectionTimeout());
    }
    
    // Same as above except for ping
    public void testDefaultConnectionPing()
    throws Exception
    {
        HttpURLConnection.setDefaultIdleConnectionPing(99);
        URL url = new URL("http://dummy");
        HttpURLConnection urlCon1 = HttpURLConnection.openConnection(url);
        assertEquals(99, urlCon1.getIdleConnectionPing());
        urlCon1.setIdleConnectionPing(10);

        HttpURLConnection.setDefaultIdleConnectionPing(99);
        HttpURLConnection urlCon2 = HttpURLConnection.openConnection(url);
        assertEquals(99, urlCon2.getIdleConnectionPing());

        // Verify the first connection did not change
        assertEquals(10, urlCon1.getIdleConnectionPing());
    }
    

    
}
