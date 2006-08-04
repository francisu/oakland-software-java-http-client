//
// Copyright 2002-3003, oakland software, all rights reserved.
//
// May not be used or redistributed without specific written
// permission from oakland software.
//

package com.oaklandsw.http;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Handles the connection timeout mechanism.
 */
public class HttpConnectionTimeout
{
    private Log                   _log          = LogFactory
                                                        .getLog(HttpConnectionTimeout.class);

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
                _shutdown = true;
                _lock.notifyAll();
            }
        }
    }

    class TimeoutThread extends Thread
    {
        public void run()
        {
            _log.debug("Timeout thread starting");
            // System.out
            // .println("Timeout thread starting: " + _timeoutThreadWake);
            // System.out.println(" current: " + System.currentTimeMillis());
            // long delta = _timeoutThreadWake - System.currentTimeMillis();
            // System.out.println(" delta: " + delta);
            // if (delta < 0)
            // System.out.println("!!!!!");
            Thread.currentThread().setName("HttpConnectionTimeout");
            synchronized (_lock)
            {
                long currentTime = System.currentTimeMillis();
                while (_timeoutThreadWake > currentTime)
                {
                    if (_shutdown)
                    {
                        _log.debug("Timeout thread ending (shutdown)");
                        _shutdown = false;
                        _timeoutThread = null;
                        // System.out.println("exit - shutdown");
                        return;
                    }
                    try
                    {
                        _log.debug("Timeout wake time: " + _timeoutThreadWake);
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

                _timeoutThread = null;
                _log.debug("Timeout thread ending - nothing to do");
                // System.out.println("Timeout thread ending - nothing to do");
            }
        }
    }

    final void startIdleTimeout(HttpConnection conn)
    {
        synchronized (_lock)
        {
            long killTime = System.currentTimeMillis() + conn.getIdleTimeout();

            // System.out.println("startIdleTimeout: timeout: "
            // + conn.getIdleTimeout()
            // + " current: "
            // + System.currentTimeMillis()
            // + " kill: "
            // + killTime
            // + " current Wake: "
            // + _timeoutThreadWake
            // + " current wake delta: "
            // + (_timeoutThreadWake - System.currentTimeMillis())
            // + " kill delta: "
            // + (killTime - System.currentTimeMillis()));

            if (_timeoutThread != null)
            {
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
                _timeoutThreadWake = killTime;
                _timeoutThread = new TimeoutThread();
                _timeoutThread.setDaemon(true);
                _timeoutThread.start();
            }
        }
    }
}
