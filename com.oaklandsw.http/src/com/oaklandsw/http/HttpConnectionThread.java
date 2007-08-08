/*
 * Copyright 2007 Oakland Software Incorporated. All rights Reserved.
 */
package com.oaklandsw.http;

import java.io.InterruptedIOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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

    private static final Log _connLog             = LogFactory
                                                          .getLog(HttpConnection.CONN_LOG);

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
        this.setDaemon(true);
        setName("ConnReader: " + _connection);
    }

    public void run()
    {
        HttpURLConnection urlCon;

        BlockingQueue queue = _connection._queue;

        if (_connLog.isDebugEnabled())
            _connLog.debug("starting");

        try
        {
            while (true)
            {
                Object obj;

                // Don't wait with the lock
                obj = queue.poll(CONNECTION_WAIT_TIME, TimeUnit.MILLISECONDS);
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
                if (!urlCon.processPipelinedRead())
                {
                    // If the above returned false, we need to retry, 
                    // we keep retrying here until we get it right
                    while (!urlCon.processPipelinedWrite())
                    {
                        if (_log.isDebugEnabled())
                            _log.debug("retrying write on: " + urlCon);
                    }
                }
                if (_log.isDebugEnabled())
                    _log.debug("end: " + _connection);
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
            synchronized (_connectionManager)
            {
                // Just terminate
                _connectionManager.connectionThreadTerminated(_connection);
            }
        }
    }

}
