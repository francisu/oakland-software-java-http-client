//
// Copyright 2002-3003, oakland software, all rights reserved.
//
// May not be used or redistributed without specific written
// permission from oakland software.
//

package com.oaklandsw.http;

import com.oaklandsw.log.Log;
import com.oaklandsw.log.LogFactory;

/**
 * Handles the connection timeout mechanism.
 */
public class HttpConnectionTimeout
{
    private static final Log             _log          = LogFactory
                                                               .getLog(HttpConnectionTimeout.class);

    private static Thread                _timeoutThread;

    // The time at which the timeout thread should wake
    private static long                  _timeoutThreadWake;

    // Tells the timeout thread not to check for idle
    // connections next time it wakes - used to reset the
    // time to wake on the thread without having it go through
    // the overhead of checking
    private static boolean               _threadNoCheck;

    private static boolean               _shutdown;

    // Object only for synchronization
    private static HttpConnectionTimeout _lock;

    // If the time we are supposed to wait is less than the current
    // time, just add a little.
    private static final long            TIMEOUT_FUDGE = 10;

    static
    {
        _lock = new HttpConnectionTimeout();
    }

    static final void shutdown()
    {
        synchronized (_lock)
        {
            if (_timeoutThread != null)
            {
                _shutdown = true;
                _timeoutThread = null;
                _lock.notifyAll();
            }
        }
    }

    static final void startIdleTimeout(HttpConnection conn)
    {
        synchronized (_lock)
        {
            long killTime = System.currentTimeMillis() + conn.getIdleTimeout();

            // The timeout thread will wake before this
            // connection times out
            if (_timeoutThread != null && _timeoutThreadWake <= killTime)
            {
                _log.debug("No need to alter timeout thread wake time");
                return;
            }

            // Make it wake up sooner
            if (_timeoutThreadWake == 0 || _timeoutThreadWake > killTime)
            {
                _timeoutThreadWake = killTime;
                if (_log.isDebugEnabled())
                {
                    _log.debug("Altering timeout thread wake time to: "
                        + killTime);
                }
                // Avoid the check when waking this time
                if (_timeoutThread != null)
                {
                    _threadNoCheck = true;
                    _lock.notifyAll();
                }
            }

            if (_timeoutThread == null)
            {
                _timeoutThread = new Thread()
                {
                    public void run()
                    {
                        _log.debug("Timeout thread starting");
                        Thread.currentThread().setName("HttpConnectionTimeout");
                        synchronized (_lock)
                        {
                            while (true)
                            {
                                if (_shutdown)
                                {
                                    _log.debug("Timeout thread ending");
                                    _shutdown = false;
                                    return;
                                }
                                try
                                {
                                    _log.debug("Timeout wake time: "
                                        + _timeoutThreadWake);
                                    long currentTime = System
                                            .currentTimeMillis();
                                    while (currentTime >= _timeoutThreadWake)
                                        _timeoutThreadWake += TIMEOUT_FUDGE;
                                    _lock.wait(_timeoutThreadWake
                                        - System.currentTimeMillis());
                                }
                                catch (InterruptedException ex)
                                {
                                    // Ignore
                                }

                                if (!_threadNoCheck)
                                {
                                    _timeoutThreadWake = HttpConnectionManager
                                            .checkIdleConnections();
                                }
                                _threadNoCheck = false;
                            }
                        }
                    }
                };
                _timeoutThread.setDaemon(true);
                _timeoutThread.start();
            }
        }
    }
}
