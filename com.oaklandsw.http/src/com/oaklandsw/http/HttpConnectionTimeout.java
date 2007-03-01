//
// Copyright 2002-3003, oakland software, all rights reserved.
//
// May not be used or redistributed without specific written
// permission from oakland software.
//

package com.oaklandsw.http;

import org.apache.commons.logging.Log;

import com.oaklandsw.util.LogUtils;

/**
 * Handles the connection timeout mechanism.
 */
public class HttpConnectionTimeout
{
    private static final Log            _log          = LogUtils.makeLogger();

    private TimeoutThread         _timeoutThread;

    // The time at which the timeout thread should wake
    private long                  _timeoutThreadWake;

    // Tells the timeout thread not to check for idle
    // connections next time it wakes - used to reset the
    // time to wake on the thread without having it go through
    // the overhead of checking
    private boolean               _threadNoCheck;

    private boolean               _shutdown;

    // Object only for synchronization
    private HttpConnectionTimeout _lock;

    private HttpConnectionManager _connManager;

    // If the time we are supposed to wait is less than the current
    // time, just add a little.
    private static final long     TIMEOUT_FUDGE = 10;

    HttpConnectionTimeout(HttpConnectionManager connManager)
    {
        _lock = this;
        _connManager = connManager;
    }

    final void shutdown()
    {
        synchronized (_lock)
        {
            if (_timeoutThread != null)
            {
                _log.debug("Timeout shutdown requested");
                _shutdown = true;
                _lock.notifyAll();
            }
        }
    }

    class TimeoutThread extends Thread
    {
        public void run()
        {
            Thread.currentThread().setName("HttpConnectionTimeout");

            if (_log.isDebugEnabled())
            {
                long delta = _timeoutThreadWake - System.currentTimeMillis();
                _log.debug("Timeout thread starting: "
                    + _timeoutThreadWake
                    + " current: "
                    + System.currentTimeMillis()
                    + " delta: "
                    + delta);
                if (delta < 0)
                    _log.debug("!!!!! NEG DELTA");
            }

            synchronized (_lock)
            {
                try
                {
                    long currentTime = System.currentTimeMillis();
                    while (_timeoutThreadWake > currentTime)
                    {
                        if (_shutdown)
                        {
                            _log.debug("Timeout thread ending (shutdown)");
                            return;
                        }
                        try
                        {
                            _log.debug("Timeout wake time: "
                                + _timeoutThreadWake);
                            long waitTime = _timeoutThreadWake - currentTime;

                            // System.out.println("WAITING: currentTime: "
                            // + currentTime
                            // + " threadWake: "
                            // + _timeoutThreadWake
                            // + " waitTime: "
                            // + waitTime);
                            _lock.wait(waitTime);
                        }
                        catch (InterruptedException ex)
                        {
                            // Ignore
                        }

                        if (!_threadNoCheck)
                        {
                            _timeoutThreadWake = _connManager
                                    .checkIdleConnections();
                        }
                        _threadNoCheck = false;
                        currentTime = System.currentTimeMillis();
                    }
                    _log.debug("Timeout thread ending - nothing to do");
                }
                finally
                {
                    _timeoutThread = null;
                    // Shutdown may have been requested while this thread is up
                    _shutdown = false;

                }
            }
        }
    }

    final void startIdleTimeout(HttpConnection conn)
    {
        synchronized (_lock)
        {
            _log.debug("startIdleTimeout");
            long killTime = System.currentTimeMillis() + conn.getIdleTimeout();

            if (_log.isDebugEnabled())
            {
                _log.debug("startIdleTimeout: timeout: "
                    + conn.getIdleTimeout()
                    + " current: "
                    + System.currentTimeMillis()
                    + " kill: "
                    + killTime
                    + " current Wake: "
                    + _timeoutThreadWake
                    + " current wake delta: "
                    + (_timeoutThreadWake - System.currentTimeMillis())
                    + " kill delta: "
                    + (killTime - System.currentTimeMillis()));
            }

            if (_timeoutThread != null)
            {
                _log.debug("startIdleTimeout - existing timeout thread");

                // The timeout thread will wake before this
                // connection times out
                if (_timeoutThreadWake <= killTime)
                {
                    _log.debug("No need to alter timeout thread wake time");
                    return;
                }

                // Make it wake up sooner
                _timeoutThreadWake = killTime;
                if (_log.isDebugEnabled())
                {
                    // System.out.println("Altering timeout thread wake time to:
                    // "
                    // + killTime);
                    _log.debug("Altering timeout thread wake time to: "
                        + killTime);
                }
                // Avoid the check when waking this time
                _threadNoCheck = true;
                _lock.notifyAll();
            }
            else
            {
                _log.debug("startIdleTimeout - creating new timeout thread");
                _timeoutThreadWake = killTime;
                _timeoutThread = new TimeoutThread();
                _timeoutThread.setDaemon(true);
                _timeoutThread.start();
            }
        }
    }
}
