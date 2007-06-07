/*
 * Copyright 2007 Oakland Software Incorporated. All rights Reserved.
 */
package com.oaklandsw.http;

import org.apache.commons.logging.Log;

import com.oaklandsw.util.LogUtils;

import edu.emory.mathcs.backport.java.util.concurrent.BlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

/**
 * Thread that services an HttpConnection, there can be at most one thread per
 * connection.
 */
public class HttpConnectionThread extends Thread
{
    private static final Log _log                 = LogUtils.makeLogger();

    // Amount of time in milliseconds this connection will wait for work before
    // exiting
    static final int         CONNECTION_WAIT_TIME = 5000;

    HttpConnection           _connection;

    HttpConnectionManager    _connectionManager;

    public HttpConnectionThread(HttpConnection connection,
            HttpConnectionManager conMgr)
    {
        super();
        _connection = connection;
        _connection._connectionThread = this;
        _connectionManager = conMgr;
        setName("ConnReader: " + _connection);
    }

    public void run()
    {
        HttpURLConnection urlCon;

        BlockingQueue queue = _connection._queue;

        if (_log.isDebugEnabled())
            _log.debug("starting");

        try
        {
            while (true)
            {
                Object obj;

                // Don't wait with the lock
                obj = queue.poll(CONNECTION_WAIT_TIME, TimeUnit.MILLISECONDS);
                if (obj instanceof HttpConnectionManager.CloseMarker)
                {
                    _log.debug("Exiting due to receipt of CloseMarker");
                    return;
                }

                // Nothing found after the wait
                if (obj == null)
                {
                    synchronized (_connectionManager)
                    {
                        // Make sure one did not slip in, do a non-blocking
                        // check under the lock
                        obj = queue.poll();

                        // OK, we are really done
                        if (obj == null
                            || obj instanceof HttpConnectionManager.CloseMarker)
                        {
                            if (_log.isDebugEnabled())
                            {
                                _log.debug(this
                                    + " pipelined pool connection "
                                    + "timeout (or close) - terminating after "
                                    + CONNECTION_WAIT_TIME
                                    + "ms");
                            }
                            return;
                        }
                    }
                }

                urlCon = (HttpURLConnection)obj;

                if (_log.isDebugEnabled())
                    _log.debug("starting: " + _connection);
                urlCon.processPipelinedRead();
                if (_log.isDebugEnabled())
                    _log.debug("end: " + _connection);
            }
        }
        catch (InterruptedException e)
        {
            if (_log.isDebugEnabled())
                _log.debug(this + " interrupted - exiting");
        }
        finally
        {
            synchronized (_connectionManager)
            {
                // Just terminate
                _connectionManager.connectionThreadTerminated(_connection);
            }
        }
    }

}
