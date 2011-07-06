/*
 * Copyright 2007 Oakland Software Incorporated. All rights Reserved.
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.oaklandsw.http;

import java.io.InterruptedIOException;

import com.oaklandsw.utillog.Log;

import com.oaklandsw.util.LogUtils;

import edu.emory.mathcs.backport.java.util.concurrent.BlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

/**
 * Thread that runs any executeAsync pipelined urlcons.
 */
public class HttpPipelineAsyncThread extends Thread
{
    private static final Log _log      = LogUtils.makeLogger();

    private static final Log _connLog  = LogUtils
                                               .makeLogger(HttpConnection.CONN_LOG);

    // Amount of time in milliseconds this thread will wait before terminating
    static final int         WAIT_TIME = 5000;

    private static int       _threadCount;

    HttpConnectionManager    _connectionManager;

    public HttpPipelineAsyncThread(HttpConnectionManager conMgr)
    {
        super();
        _connectionManager = conMgr;
        this.setDaemon(true);
        setName("PipelineAsync" + _threadCount++);
    }

    public void run()
    {
        HttpURLConnection urlCon;

        BlockingQueue queue = _connectionManager._asyncQueue;

        if (_connLog.isDebugEnabled())
            _connLog.debug("starting");

        try
        {
            while (true)
            {
                Object obj;

                // Don't wait with the lock
                obj = queue.poll(WAIT_TIME, TimeUnit.MILLISECONDS);

                // Nothing found after the wait
                if (obj == null)
                {
                    synchronized (_connectionManager)
                    {
                        // Make sure one did not slip in, do a non-blocking
                        // check under the lock
                        obj = queue.poll();

                        // OK, we are really done
                        if (obj == null)
                        {
                            if (_connLog.isDebugEnabled())
                            {
                                _connLog.debug(this
                                    + " pipelined async "
                                    + "timeout - terminating after "
                                    + WAIT_TIME
                                    + "ms");
                            }
                            return;
                        }
                    }
                }

                urlCon = (HttpURLConnection)obj;

                // This returns false if a retry is desired
                while (!urlCon.processPipelinedWrite())
                {
                    if (_log.isDebugEnabled())
                        _log.debug("retrying write on: " + urlCon);
                }
            }
        }
        catch (InterruptedException e)
        {
            if (_connLog.isDebugEnabled())
                _connLog.debug(this + " interrupted - exiting", e);
        }
        catch (InterruptedIOException e)
        {
            if (_connLog.isDebugEnabled())
                _connLog.debug(this + " interrupted - exiting", e);
        }
        finally
        {
            if (_connLog.isDebugEnabled())
                _connLog.debug("terminating");

            // Just terminate
            _connectionManager.asyncThreadTerminated();
        }
    }
}
