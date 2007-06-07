package com.oaklandsw.http.webapp;

import java.util.ArrayList;

import org.apache.commons.logging.Log;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.PipelineTester;
import com.oaklandsw.http.servlet.ParamServlet;
import com.oaklandsw.util.LogUtils;

public class TestPipelining extends TestWebappBase
{
    private static final Log _log     = LogUtils.makeLogger();

    protected boolean        _threadFailed;


    public TestPipelining(String testName)
    {
        super(testName);

        // This runs with explicit close anyway, no need to do this special test
        _doExplicitTest = false;

        // Netproxy seems to drop requests
        _doAuthCloseProxyTest = false;
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestPipelining.class);
        return suite;
    }

    public static void main(String args[])
    {
        mainRun(suite(), args);
    }

    public void setUp() throws Exception
    {
        super.setUp();
        _showStats = true;
    }

    public void tearDown() throws Exception
    {
        super.tearDown();
    }

    // Use the pipelining on a single request
    // Tests with two requests, make a single request test
    protected void testSimple(int number) throws Exception
    {
        PipelineTester pt = new PipelineTester(_urlBase + ParamServlet.NAME,
                                               number);
        assertFalse(pt.runTest());
    }

    public void testSimple1() throws Exception
    {
        testSimple(1);
    }

    public void testSimple2() throws Exception
    {
        if (_logging)
        {
            LogUtils
                    .logFile("/home/francis/log4jpipe2" + _testAllName + ".txt");
        }
        testSimple(2);
    }

    public void testSimple10() throws Exception
    {
        if (_logging)
        {
            LogUtils.logFile("/home/francis/log4jpipe10"
                + _testAllName
                + ".txt");
        }
        testSimple(10);
    }

    public void testSimple100() throws Exception
    {
        if (_logging)
        {
            LogUtils.logFile("/home/francis/log4jpipe100"
                + _testAllName
                + ".txt");
        }
        testSimple(100);
    }

    // 100 is the point where the connection is closed from the server
    public void testSimple110() throws Exception
    {
        if (_logging)
        {
            LogUtils.logFile("/home/francis/log4jpipe110"
                + _testAllName
                + ".txt");
        }
        // LogUtils.logAll();
        testSimple(110);
    }

    public void testSimple500() throws Exception
    {
        if (_logging)
        {
            LogUtils.logFile("/home/francis/log4jpipe1500"
                + _testAllName
                + ".txt");
        }
        testSimple(500);
    }

    public void testSimple1000() throws Exception
    {
        if (_logging)
        {
            LogUtils.logFile("/home/francis/log4jpipe1000"
                + _testAllName
                + ".txt");
        }
        testSimple(1000);
    }

    public void testSimple5000() throws Exception
    {
        if (_logging)
        {
            LogUtils.logFile("/home/francis/log4jpipe5000"
                + _testAllName
                + ".txt");
        }
        testSimple(5000);
    }

    public void testSimple10000() throws Exception
    {
        testSimple(10000);
    }

    protected void testThreaded(int threadCount, final int num)
        throws Exception
    {
        Thread t;

        ArrayList threads = new ArrayList();
        for (int i = 0; i < threadCount; i++)
        {
            final int threadInd = i;
            t = new Thread()
            {
                public void run()
                {
                    try
                    {
                        Thread.currentThread().setName("Thread" + threadInd);
                        PipelineTester pt = new PipelineTester(_urlBase
                            + ParamServlet.NAME, num);
                        pt._checkConnections = false;
                        boolean failed = pt.runTestMt();
                        if (failed)
                            _threadFailed = true;
                    }
                    catch (Exception ex)
                    {
                        System.out.println("Unexpected exception: " + ex);
                    }
                }
            };

            t.start();
            threads.add(t);
        }

        for (int i = 0; i < threads.size(); i++)
        {
            ((Thread)threads.get(i)).join(10000);
        }
        PipelineTester.cleanup();
    }

    public void testThreaded1() throws Exception
    {
        if (_logging)
        {
            LogUtils.logFile("/home/francis/log4jpipeMT1"
                + _testAllName
                + ".txt");
        }
        testThreaded(5, 5);
        assertFalse(_threadFailed);
    }

    public void testThreaded2() throws Exception
    {
        if (_logging)
        {
            LogUtils.logFile("/home/francis/log4jpipeMT2"
                + _testAllName
                + ".txt");
        }
        testThreaded(3, 110);
        // HttpURLConnection.dumpAll();
        assertFalse(_threadFailed);
    }

    public void testThreaded3() throws Exception
    {
        int count = _extended ? 10 : 1;
        for (int i = 0; i < count; i++)
        {
            if (_logging)
            {
                LogUtils.logFile("/home/francis/log4jpipeMT3"
                    + i
                    + _testAllName
                    + ".txt");
            }
            // _showStats = false;
            testThreaded(10, 500);
            // HttpURLConnection.dumpAll();
            assertFalse(_threadFailed);
        }
    }

    public void testThreaded4() throws Exception
    {
        int count = _extended ? 10 : 1;
        for (int i = 0; i < count; i++)
        {
            if (_logging)
            {
                LogUtils.logFile("/home/francis/log4jpipeMT4"
                    + i
                    + _testAllName
                    + ".txt");
            }
            // _showStats = false;
            testThreaded(15, 100);
            Thread.sleep(500);
            // HttpURLConnection.dumpAll();
            assertFalse(_threadFailed);
        }
    }

    public void allTestMethods() throws Exception
    {
        // _showStats = false;
        // System.out.println("---------------- All: " + _testAllName);
        // LogUtils.logFile("/home/francis/log4jpipeProxy.txt");

        testSimple1();
        testSimple2();
        testSimple10();
        testSimple100();
        testSimple500();

        testThreaded1();
        testThreaded2();
        testThreaded3();
    }

}
