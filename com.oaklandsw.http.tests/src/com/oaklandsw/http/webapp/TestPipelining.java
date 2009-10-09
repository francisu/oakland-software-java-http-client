package com.oaklandsw.http.webapp;

import com.oaklandsw.http.AbstractCallback;
import com.oaklandsw.http.Callback;
import com.oaklandsw.http.HttpConnectionManager;
import com.oaklandsw.http.HttpTestEnv;
import com.oaklandsw.http.HttpURLConnection;
import com.oaklandsw.http.PipelineTester;
import com.oaklandsw.http.servlet.ParamServlet;

import com.oaklandsw.util.Log;
import com.oaklandsw.util.LogUtils;
import com.oaklandsw.util.Util;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;

import java.net.URL;

import java.util.ArrayList;


public class TestPipelining extends TestWebappBase {
    private static final Log _log = LogUtils.makeLogger();
    static final int OPT_EMPTY = 0;
    static final int OPT_GET_INPUT = 1;
    static final int OPT_READ = 2;
    static final int OPT_CLOSE = 3;
    protected static final boolean CHECK_RESULT = true;
    protected boolean _threadFailed;
    protected String _threadTestName;
    protected PipelineTester _pt;
    protected String _urlString;
    protected boolean _failed;
    protected String _outputData;
    protected boolean _doOutput;
    protected String _requestType;

    public TestPipelining(String testName) {
        super(testName);

        // .._extended = true;

        // This runs with explicit close anyway, no need to do this special test
        _doExplicitTest = false;

        // Netproxy seems to drop requests
        _doAuthCloseProxyTest = false;

        _urlString = _urlBase + ParamServlet.NAME;

        // _logging = true;
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(TestPipelining.class);

        return suite;
    }

    public static void main(String[] args) {
        mainRun(suite(), args);
    }

    public void setUp() throws Exception {
        super.setUp();
        // LogUtils.logFile("/home/francis/log4jpipeALL.txt");
        _showStats = true;

        _streamingType = STREAM_NONE;
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    // Use the pipelining on a single request
    // Tests with two requests, make a single request test
    protected void testSimple(int number) throws Exception {
        _pt = new PipelineTester(_urlString, number, _pipelineOptions,
                _pipelineMaxDepth);
        _pt._streamingType = _streamingType;
        _pt._streamingSize = _streamingSize;
        _pt._doOutput = _doOutput;
        _pt._outputData = _outputData;
        _pt._requestType = _requestType;

        assertFalse(_pt.runTest());
        HttpURLConnection.dumpAll();
    }

    public void testPipelineError() throws Exception {
        HttpURLConnection.setDefaultPipelining(true);

        HttpURLConnection urlCon = HttpURLConnection.openConnection(new URL(
                    _urlString));

        try {
            urlCon.getResponseCode();
            fail("Did not get expected exception");
        } catch (IllegalStateException ex) {
            // Expected
        }
    }

    public void testPipelineMix() throws Exception {
        SimpleCallback cb = new SimpleCallback();

        HttpURLConnection.setDefaultPipelining(true);
        HttpURLConnection.setDefaultCallback(cb);

        HttpURLConnection urlCon = HttpURLConnection.openConnection(new URL(
                    _urlString));

        urlCon.pipelineExecute();

        urlCon = HttpURLConnection.openConnection(new URL(_urlString));
        urlCon.setPipelining(false);
        assertEquals(200, urlCon.getResponseCode());

        HttpURLConnection.pipelineBlock();
        assertTrue(cb._read);
    }

    public void testSimple10Async() throws Exception {
        if (_logging) {
            LogUtils.logFile("/home/francis/log4jpipe10Async" + _testAllName +
                ".txt");
        }

        PipelineTester pt = new PipelineTester(_urlString, 10,
                _pipelineOptions, _pipelineMaxDepth);
        pt._async = true;
        assertFalse(pt.runTest());
        HttpURLConnection.dumpAll();
    }

    // Bug 1972 connection released twice
    // Currently fails on 64-bit JVM on Linux 24 Sep 09 FRU
    public void testSimple10Async404() throws Exception {
        if (_logging) {
            LogUtils.logFile("/home/francis/log4jpipe10Async404" +
                _testAllName + ".txt");
        }

        // May do a log of retries due to pipelining and the error
        HttpURLConnection.setDefaultMaxTries(100);

        PipelineTester pt = new PipelineTester(HttpTestEnv.TEST_URL_HOST_ERRORSVR +
                "?error=404&", 10, _pipelineOptions, _pipelineMaxDepth);
        pt._async = true;
        pt._expectedResponse = 404;
        assertFalse(pt.runTest());
        HttpURLConnection.dumpAll();
    }

    public void testCallbackActions(final int options)
        throws Exception {
        if (_logging) {
            LogUtils.logFile("/home/francis/log4jpipe10NoRead" + _testAllName +
                ".txt");
        }

        Callback cb = new Callback() {
                public void error(HttpURLConnection urlCon1, InputStream is,
                    Exception ex) {
                    fail(ex.getMessage());
                }

                public void readResponse(HttpURLConnection urlCon1,
                    InputStream is) {
                    try {
                        InputStream newIs = null;

                        if (options >= OPT_GET_INPUT) {
                            newIs = urlCon1.getInputStream();
                            assertEquals(is, newIs);
                        }

                        if (options >= OPT_READ) {
                            Util.getStringFromInputStream(is);
                        }

                        if (options == OPT_CLOSE) {
                            is.close();
                        }
                    } catch (Exception e) {
                        fail(e.getMessage());
                    }
                }

                public void writeRequest(HttpURLConnection urlCon1,
                    OutputStream os) {
                }
            };

        HttpURLConnection.setDefaultPipelining(true);

        HttpURLConnection urlCon;

        for (int i = 0; i < 200; i++) {
            urlCon = HttpURLConnection.openConnection(new URL(_urlString));
            urlCon.setCallback(cb);
            urlCon.pipelineExecute();
        }

        HttpURLConnection.pipelineBlock();
        HttpURLConnection.dumpAll();
    }

    public void testCallbackEmpty() throws Exception {
        testCallbackActions(OPT_EMPTY);
    }

    public void testCallbackRead() throws Exception {
        testCallbackActions(OPT_READ);
    }

    public void testCallbackClose() throws Exception {
        testCallbackActions(OPT_CLOSE);
    }

    public void testCallbackGetInput() throws Exception {
        testCallbackActions(OPT_GET_INPUT);
    }

    public void testShutdown1() throws Exception {
        testSimple(10);
        HttpURLConnection.immediateShutdown();
        HttpURLConnection.dumpAll();
        testSimple(10);
        HttpURLConnection.dumpAll();
    }

    public void testShutdown2Base() throws Exception {
        // LogUtils.logFile("/home/francis/log4jshutdown.txt");
        _failed = false;

        // Run this a few times and make sure everything is OK
        for (int i = 1; i <= 5; i++) {
            Runnable run = new Runnable() {
                    public void run() {
                        try {
                            testSimple(1000);
                        } catch (InterruptedException e) {
                            // Expected
                        } catch (InterruptedIOException e) {
                            // Expected
                        } catch (Exception e) {
                            System.err.println(e);
                            _failed = true;

                            // Expected
                        }
                    }
                };

            Thread t = new Thread(run);
            t.start();
            Thread.sleep(100 * i);

            // Stop making new connections
            t.interrupt();
            _pt._stop = true;
            // Wait for it to die
            Thread.sleep(100);

            HttpURLConnection.immediateShutdown();
            HttpURLConnection.getConnectionManager().checkEverythingEmpty();
            assertEquals(0,
                HttpURLConnection.getConnectionManager()
                                 .getTotalConnectionCount(_urlString));
            HttpURLConnection.dumpAll();

            assertFalse(_failed);
        }

        testSimple(100);
    }

    // Currently fails on 64-bit JVM on Linux 24 Sep 09 FRU
    public void XXtestShutdown2Loop() throws Exception {
        // logAll();
        // LogUtils.logFile("/home/francis/log4jshutdown.txt");
        for (int i = 0; i < 5; i++) {
            testShutdown2Base();
            Thread.sleep(50);
        }
    }

    public void testSimple1() throws Exception {
        testSimple(1);
    }

    public void testSimple2() throws Exception {
        if (_logging) {
            LogUtils.logFile("/home/francis/log4jpipe2" + _testAllName +
                ".txt");
        }

        testSimple(2);
    }

    public void testSimple10() throws Exception {
        if (_logging) {
            LogUtils.logFile("/home/francis/log4jpipe10" + _testAllName +
                ".txt");
        }

        testSimple(10);
    }

    // Currently fails on 64-bit JVM on Linux 24 Sep 09 FRU
    public void XXtestSimple100() throws Exception {
        if (_logging) {
            LogUtils.logFile("/home/francis/log4jpipe100" + _testAllName +
                ".txt");
        }

        testSimple(100);
    }

    // 100 is the point where the connection is closed from the server
    // Currently fails on 64-bit JVM on Linux 24 Sep 09 FRU
    public void XXtestSimple110() throws Exception {
        if (_logging) {
            LogUtils.logFile("/home/francis/log4jpipe110" + _testAllName +
                ".txt");
        }

        // LogUtils.logAll();
        testSimple(110);
    }

    // 100 is the point where the connection is closed from the server
    public void testSimple110ReqLimit() throws Exception {
        if (_logging) {
            LogUtils.logFile("/home/francis/log4jpipe110l" + _testAllName +
                ".txt");
        }

        // logAll();
        // LogUtils.logConnOnly();
        HttpURLConnection.setMaxConnectionsPerHost(1);
        HttpURLConnection.setDefaultConnectionRequestLimit(100);
        testSimple(110);
    }

    // Currently fails on 64-bit JVM on Linux 24 Sep 09 FRU
    public void XXtestSimple500() throws Exception {
        if (_logging) {
            LogUtils.logFile("/home/francis/log4jpipe1500" + _testAllName +
                ".txt");
        }

        testSimple(500);
    }

    // Currently fails on 64-bit JVM on Linux 24 Sep 09 FRU
    public void XXtestSimple500ReqLimit() throws Exception {
        if (_logging) {
            LogUtils.logFile("/home/francis/log4jpipe1500l" + _testAllName +
                ".txt");
        }

        HttpURLConnection.setMaxConnectionsPerHost(1);
        HttpURLConnection.setDefaultConnectionRequestLimit(100);
        testSimple(500);
    }

    // Currently fails on 64-bit JVM on Linux 24 Sep 09 FRU
    public void XXtestSimple1000() throws Exception {
        if (_logging) {
            LogUtils.logFile("/home/francis/log4jpipe1000" + _testAllName +
                ".txt");
        }

        testSimple(1000);
    }

    // Currently fails on 64-bit JVM on Linux 24 Sep 09 FRU
    public void XXtestSimple5000() throws Exception {
        if (_logging) {
            LogUtils.logFile("/home/francis/log4jpipe5000" + _testAllName +
                ".txt");
        }

        testSimple(5000);
    }

    // Currently fails on 64-bit JVM on Linux 24 Sep 09 FRU
    public void XXtestSimple10000() throws Exception {
        if (!true || _logging) {
            LogUtils.logFile("/home/francis/log4jpipe10000" + _testAllName +
                ".txt");
        }

        testSimple(10000);
    }

    public void testStreamingChunk() throws Exception {
        _streamingType = STREAM_CHUNKED;
        _doOutput = true;
        _requestType = "PUT";
        _outputData = "abc";

        if (_logging) {
            LogUtils.logFile("/home/francis/log4jpipechunked" + _testAllName +
                ".txt");
        }

        testSimple1();

        //testSimple110();
        //testSimple1000();
    }

    public void testStreamingFixed() throws Exception {
        _streamingType = STREAM_FIXED;
        _doOutput = true;
        _requestType = "PUT";
        _outputData = "abc";
        _streamingSize = _outputData.length();

        if (_logging) {
            LogUtils.logFile("/home/francis/log4jpipefixed" + _testAllName +
                ".txt");
        }

        testSimple1();

        //testSimple110();
        //testSimple1000();
    }

    // Currently fails on 64-bit JVM on Linux 24 Sep 09 FRU
    public void XXtestStreamingRaw() throws Exception {
        _streamingType = STREAM_RAW;
        _doOutput = true;
        _requestType = "PUT";
        _outputData = "abc";

        if (_logging) {
            LogUtils.logFile("/home/francis/log4jpiperaw" + _testAllName +
                ".txt");
        }

        testSimple1();

        //testSimple110();
        //testSimple1000();
    }

    // Make sure the pipeling depth limit is respected
    public void testSimpleDepth() throws Exception {
        if (_logging) {
            LogUtils.logFile("/home/francis/log4jpipeDepth" + _testAllName +
                ".txt");
        }

        _pipelineMaxDepth = 10;

        testSimple(100);

        assertTrue(HttpURLConnection.getConnectionManager()
                                    .getCount(HttpConnectionManager.COUNT_PIPELINE_DEPTH_HIGH) <= 10);

        // For the testAll case, make sure we don't interfere
        // with other tests
        _pipelineMaxDepth = 0;
    }

    protected void testThreaded(int threadCount, int num)
        throws Exception {
        testThreaded(threadCount, _urlString, num, CHECK_RESULT);
    }

    protected void testThreaded(int threadCount, final String url,
        final int num, final boolean checkResult) throws Exception {
        Thread t;

        _threadTestName = "t" + threadCount + "_" + num;

        if (false) {
            System.out.println("*** Starting executing " + threadCount +
                " threads " + _testAllName);
        }

        _log.debug("*** Starting executing " + threadCount + " threads " +
            _testAllName + " " + _threadTestName);

        ArrayList threads = new ArrayList();

        for (int i = 0; i < threadCount; i++) {
            final int threadInd = i;
            t = new Thread() {
                        public void run() {
                            try {
                                Thread.currentThread()
                                      .setName("Thread" + threadInd +
                                    _threadTestName + _testAllName);

                                PipelineTester pt = new PipelineTester(url,
                                        num, _pipelineOptions, _pipelineMaxDepth);
                                pt._checkConnections = false;
                                pt._checkResult = checkResult;

                                boolean failed = pt.runTestMt();

                                if (failed) {
                                    _threadFailed = true;
                                }
                            } catch (Exception ex) {
                                System.out.println("Unexpected exception: " +
                                    ex);
                                _threadFailed = true;
                            }
                        }
                    };

            t.start();
            threads.add(t);
        }

        for (int i = 0; i < threadCount; i++) {
            ((Thread) threads.get(i)).join(100000);
        }

        _log.debug("*** Finished executing " + threadCount + " threads " +
            _testAllName);

        if (false) {
            System.out.println("*** Finished executing " + threadCount +
                " threads " + _testAllName + " " + _threadTestName);
        }

        PipelineTester.cleanup();
    }

    public void testThreaded1() throws Exception {
        if (_logging) {
            LogUtils.logFile("/home/francis/log4jpipeMT1" + _testAllName +
                ".txt");
        }

        testThreaded(5, 5);
        assertFalse(_threadFailed);
    }

    // Currently fails on 64-bit JVM on Linux 24 Sep 09 FRU
    public void XXtestThreaded2() throws Exception {
        System.out.println("----------------    Threaded2");

        if (_logging) {
            LogUtils.logFile("/home/francis/log4jpipeMT2" + _testAllName +
                ".txt");
        }

        testThreaded(3, 110);
        // HttpURLConnection.dumpAll();
        assertFalse(_threadFailed);
    }

    // Currently fails on 64-bit JVM on Linux 24 Sep 09 FRU
    public void XXtestThreaded3() throws Exception {
        System.out.println("----------------    Threaded3");

        int count = _extended ? 10 : 1;

        for (int i = 0; i < count; i++) {
            if (_logging) {
                LogUtils.logFile("/home/francis/log4jpipeMT3" + i +
                    _testAllName + ".txt");
            }

            // _showStats = false;
            testThreaded(10, 500);
            // HttpURLConnection.dumpAll();
            assertFalse(_threadFailed);
        }
    }

    // Currently fails on 64-bit JVM on Linux 24 Sep 09 FRU
    public void XXtestThreaded4() throws Exception {
        System.out.println("----------------    Threaded4");

        HttpURLConnection.setMaxConnectionsPerHost(10);
        HttpURLConnection.setDefaultRequestTimeout(5000);

        int count = _extended ? 10 : 1;

        for (int i = 0; i < count; i++) {
            if (!true || _logging) {
                LogUtils.logFile("/home/francis/log4jpipeMT4" + i +
                    _testAllName + ".txt");
            }

            // _showStats = false;
            testThreaded(100, 250);

            // Thread.sleep(500);
            // HttpURLConnection.dumpAll();
        }

        assertFalse(_threadFailed);
        HttpURLConnection.dumpAll();
    }

    public void allTestMethods() throws Exception {
        System.out.println("---------------- All: " + _testAllName);

        if (!true) {
            _showStats = false;
            LogUtils.logConnFile("/home/francis/log4jpipeAll" + _testAllName +
                ".txt");
        }

        testSimple1();
        testSimple2();
        testSimple10();
        //testSimple100();
        //testSimple110();
        System.out.println("----------------    110ReqLimit");
        testSimple110ReqLimit();
        System.out.println("----------------    500");
        //testSimple500();
        System.out.println("----------------    500ReqLimit");
        //testSimple500ReqLimit();
        System.out.println("----------------    Depth");

        // SimpleDepth depends on reset statistics
        HttpURLConnection.resetStatistics();
        HttpURLConnection.closeAllPooledConnections();
        testSimpleDepth();

        System.out.println("----------------    Threaded1");
        testThreaded1();
        System.out.println("----------------    Threaded2");
        //testThreaded2();
        System.out.println("----------------    Threaded3");

        //testThreaded3();
        // System.out.println("---------------- Threaded4");
        // testThreaded4();

        // FIXME - these don't seem to work correctly for the 1.0 proxy
        if (!_in10ProxyTest) {
            System.out.println("----------------    StreamingChunk");
            testStreamingChunk();
            System.out.println("----------------    StreamingFixed");
            testStreamingFixed();
        }

        // Can't test this through the proxy because it will not
        // write the request correctly
        // System.out.println("---------------- StreamingRaw");
        // testStreamingRaw();
    }

    class SimpleCallback extends AbstractCallback {
        public boolean _read;

        public void readResponse(HttpURLConnection urlCon, InputStream is) {
            _read = true;
        }
    }
}
