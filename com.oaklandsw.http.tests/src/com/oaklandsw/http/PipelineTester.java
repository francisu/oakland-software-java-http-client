/*
 * Copyright 2007 Oakland Software Incorporated. All rights Reserved.
 */
package com.oaklandsw.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import org.apache.commons.logging.Log;

//import com.oaklandsw.http.HttpConnectionManager.CheckResults;
import com.oaklandsw.util.LogUtils;
import com.oaklandsw.util.Util;

public class PipelineTester //implements CheckResults
{
    private static final Log   _log              = LogUtils.makeLogger();

    public static boolean      _showStats;

    public int                 _readCountOk;
    public int                 _readAutoRetry;
    public int                 _writeAutoRetry;
    public int                 _writeCount;

    public int[]               _responseCounts;

    public int                 _failType;
    public Exception           _failException;
    public int                 _failResponse;

    public int                 _iterations;
    public boolean             _stop;

    public String              _url;
    public boolean             _checkConnections = true;

    public int                 _pipeliningOptions;
    public int                 _pipeliningMaxDepth;

    public boolean             _async;
    public boolean             _readData         = true;

    public int                 _expectedResponse = 200;

    public boolean             _checkResult      = true;

    public int                 _streamingType;
    public int                 _streamingSize;
    public boolean             _doOutput;
    public String              _outputData;
    public String              _requestType;

    // Don't report on this type of failure
    public int                 _ignoreFailType;

    public static final String COUNT_PROP        = "count";

    public PipelineTester(String url,
            int iterations,
            int pipeliningOptions,
            int pipeliningMaxDepth)
    {
        _url = url;
        _iterations = iterations;
        _pipeliningMaxDepth = pipeliningMaxDepth;
        _pipeliningOptions = pipeliningOptions;

        //if (false)
        //    HttpURLConnection.getConnectionManager()._checkResults = this;
    }

    public class TestCallback implements Callback
    {

        public void writeRequest(HttpURLConnection urlCon, OutputStream os)
        {
            _writeCount++;

            // Unexpected
            if (!_doOutput)
            {
                setFailType(22, null, 0);
            }

            try
            {
                if (_streamingType == HttpTestBase.STREAM_RAW)
                {
                    String urlString = urlCon.getUrlString();
                    String outString = "PUT "
                        + urlString
                        + " HTTP/1.1\r\n"
                        + "Content-Length: "
                        + _outputData.length()
                        + "\r\n\r\n";
                    os.write(outString.getBytes());
                }
                os.write(_outputData.getBytes());
                os.close();
            }
            catch (AutomaticHttpRetryException arex)
            {
                synchronized (PipelineTester.this)
                {
                    _writeAutoRetry++;
                }
                // The read will be redriven
                return;
            }
            catch (IOException e)
            {
                setFailType(23, e, 0);
            }

        }

        public void readResponse(HttpURLConnection urlCon, InputStream is)
        {
            // System.out.println(urlCon.getHeaderFields());

            int response;
            try
            {
                if (is == null)
                {
                    setFailType(5, null, 0);
                    return;
                }

                response = urlCon.getResponseCode();
                if (response != _expectedResponse)
                {
                    // System.out.println("Got unexpected response: " +
                    // response);
                    setFailType(6, null, response);
                    return;
                }

                if (_readData)
                {
                    String str = null;
                    try
                    {
                        str = Util.getStringFromInputStream(is);
                    }
                    catch (AutomaticHttpRetryException arex)
                    {
                        synchronized (PipelineTester.this)
                        {
                            _readAutoRetry++;
                        }
                        // The read will be redriven
                        return;
                    }

                    // Check that the response was actually received
                    String checkStr = "name=\"count\";value=\"";
                    int reqNumberStart = str.indexOf(checkStr)
                        + checkStr.length();
                    int reqNumberEnd = str.indexOf('"', reqNumberStart);
                    String reqNumberStr = str.substring(reqNumberStart,
                                                        reqNumberEnd);
                    int reqNumber = Integer.parseInt(reqNumberStr);

                    if (reqNumber < 1 || reqNumber > _iterations)
                    {
                        System.out.println("Invalid reqNumber: " + reqNumber);
                        setFailType(40, null, 0);
                    }

                    if (_outputData != null && str.indexOf(_outputData) < 0)
                    {
                        System.out.println("Output not in output: " + str);
                        setFailType(41, null, 0);

                    }
                    synchronized (PipelineTester.this)
                    {
                        _readCountOk++;
                        _log.debug("response:" + reqNumber);
                        // System.out.println("response: " + reqNumber);
                        _responseCounts[reqNumber]++;
                    }
                }
            }
            catch (IOException e)
            {
                setFailType(7, e, 0);
            }
        }

        public void error(HttpURLConnection urlCon, InputStream is, Exception ex)
        {
            try
            {
                if (urlCon.getResponseCode() == _expectedResponse && ex == null)
                {
                    synchronized (PipelineTester.this)
                    {
                        _readCountOk++;
                    }
                }
                else
                {
                    setFailType(8, ex, urlCon.getResponseCode());
                }
            }
            catch (IOException e)
            {
                setFailType(9, e, 0);
            }
        }

        public String toString()
        {
            return "Callback" + Thread.currentThread().getName();
        }
    }

    public synchronized void setFailType(int failType,
                                         Exception ex,
                                         int responseCode)
    {
        // We don't care if we have been stopped
        if (_stop)
            return;

        if (_ignoreFailType != failType)
        {
            System.out.println("failure "
                + failType
                + " respCode: "
                + responseCode
                + " exception: "
                + ex);
        }

        if (ex != null)
        {
            if (ex instanceof AutomaticHttpRetryException)
            {
                _readAutoRetry++;
                return;
            }

            if (_ignoreFailType != failType)
                ex.printStackTrace(System.out);
            _failException = ex;
        }
        if (responseCode != 0)
            _failResponse = responseCode;
        _failType = failType;
    }

    public boolean runTest() throws Exception
    {
        boolean result = runTestMt();
        cleanup();
        return result;
    }

    // For the multi-threaded tests, does nothing to cleanup at the
    // end of each tst
    public boolean runTestMt() throws Exception
    {
        URL url = new URL(_url);

        Callback cb = new TestCallback();

        _writeCount = _readCountOk = 0;
        _responseCounts = new int[_iterations + 1];

        // LogUtils.logAll();

        HttpURLConnectInternal._addDiagSequence = true;

        // For testing
        // HttpURLConnection.setDefaultIdleConnectionTimeout(0);

        HttpURLConnection.setDefaultMaxTries(100);

        HttpURLConnection.setDefaultCallback(null);
        HttpURLConnection.setDefaultPipelining(true);

        // Create the connections
        for (int i = 0; i < _iterations; i++)
        {
            if (_stop)
            {
                _log.debug("stopped as requested");
                break;
            }

            // Not in the header, add it as a query request param
            String urlString = _url
                + (_url.endsWith("&") ? "" : "?")
                + COUNT_PROP
                + "="
                + Integer.toString(i + 1)
                + "&threadName="
                + Thread.currentThread().getName();
            url = new URL(urlString);
            // System.out.println(Thread.currentThread().getName()
            // + " creating Connection");
            HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
            if (_requestType != null)
                urlCon.setRequestMethod(_requestType);
            urlCon.setCallback(cb);
            HttpTestBase.setupStreaming(_streamingType, urlCon, _streamingSize);
            urlCon.setDoOutput(_doOutput);
            urlCon.setPipeliningOptions(_pipeliningOptions);
            urlCon.setPipeliningMaxDepth(_pipeliningMaxDepth);
            urlCon.setRequestProperty(COUNT_PROP, Integer.toString(i + 1));
            urlCon.setRequestProperty("threadName", Thread.currentThread()
                    .getName());
            if (_async)
                urlCon.pipelineExecuteAsync();
            else
                urlCon.pipelineExecute();
        }

        if (_showStats)
        {
            System.out.println("-------------------------start -------------");
        }

        if (_showStats)
        {
            System.out
                    .println("Number of connections for test: " + _iterations);
        }

        // Process the connection
        // System.out.println(Thread.currentThread().getName()
        // + " before execute and block");
        // For async assume it's going to be done in a second
        if (_async)
            Thread.sleep(1000);
        else
            HttpURLConnection.pipelineBlock();
        // System.out.println(Thread.currentThread().getName()
        // + " after execute and block");

        // Let things settle after we wake from the block, it's possible
        // all of the processing of releasing connections etc might
        // not be done when the block returns; this will not cause
        // problems for the users, but the connection counts might
        // not be right if we check right away
        Thread.sleep(100);

        if (_showStats)
        {
            HttpURLConnection.dumpAll();
        }

        if (_showStats)
        {
            System.out.println("Read OK count:          " + _readCountOk);
            System.out.println("Read auto retry count:  " + _readAutoRetry);
            System.out.println("Write auto retry count: " + _writeAutoRetry);
        }

        boolean results = true;
        if (_checkResult && !_stop)
            results = checkResults();

        // Test passed if we interrupt it
        if (_stop)
            return false;

        if (_checkConnections)
        {
            if (HttpTestBase.getActiveConns(url) != 0)
            {
                System.out.println("Remaining active connections");
                HttpURLConnection.dumpAll();
                setFailType(30, null, 0);
            }
        }
        return results;
    }

    public static void cleanup()
    {
        HttpURLConnection.setDefaultCallback(null);
        HttpURLConnection.setDefaultPipelining(false);
        HttpURLConnectInternal._addDiagSequence = false;
    }

    // returns true if failed
    public boolean checkCounts()
    {
        boolean fail = false;
        int totalResponses = 0;
        for (int i = 1; i <= _iterations; i++)
        {
            totalResponses += _responseCounts[i];
            if (_responseCounts[i] != 1)
            {
                System.out.println("Response count incorrect for req: "
                    + i
                    + " count is: "
                    + _responseCounts[i]);
                fail = true;
            }
        }

        if (_iterations != totalResponses)
        {
            System.out.println("totalResponses mismatch: "
                + _iterations
                + " read counts: "
                + totalResponses);
            fail = true;
        }
        return fail;

    }

    // Returns true if there is a failure
    public boolean checkResults()
    {
        boolean fail = false;

        // This is a GET - write is not called
        // assertEquals(number, _writeCount);
        if (_iterations != _readCountOk)
        {
            System.out.println("Iterations mismatch: "
                + _iterations
                + " read counts: "
                + _readCountOk);
            fail = true;
        }

        if (_failType != 0)
        {
            // Reason is printed at the time the failure is recorded
            fail = true;
        }

        if (_expectedResponse != 200)
            return fail;

        return checkCounts();
    }
}
