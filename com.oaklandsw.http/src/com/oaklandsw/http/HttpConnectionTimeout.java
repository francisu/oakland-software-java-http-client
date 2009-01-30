//
// Copyright 2002-2007, oakland software, all rights reserved.
//
// May not be used or redistributed without specific written
// permission from oakland software.
//

package com.oaklandsw.http;

import com.oaklandsw.util.Log;

import com.oaklandsw.util.LogUtils;
import com.oaklandsw.util.Util;

/**
 * Handles the connection timeout mechanism.
 */
public class HttpConnectionTimeout
{
    private static final Log      _log = LogUtils.makeLogger();

    private TimeoutThread         _timeoutThread;

    // The time at which the timeout thread should wake
    private long                  _timeoutThreadWake;

    // Tells the timeout thread not to check for idle
    // connections next time it wakes - used to reset the
    // time to wake on the thread without having it go through
    // the overhead of checking
    private boolean               _threadNoCheck;

    private boolean               _shutdown;

    // Object only for synchronization, this lock may be locked
    // when the connection manager lock is locked, however, you may
    // not call the connection manager when you have this lock
    private Object                _lock;

    private HttpConnectionManager _connManager;

    HttpConnectionTimeout(HttpConnectionManager connManager)
    {
        // Don't use the connection manager for a lock, it gets too
        // many notifies
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
                if (_log.isDebugEnabled())
                {
                    _log.debug("Timeout thread starting: "
                        + _timeoutThreadWake
                        + " current: "
                        + System.currentTimeMillis()
                        + " delta: "
                        + delta);
                }
                if (delta < 0)
                    Util.impossible("Negative delta: " + delta);
            }

            try
            {
                boolean checkConnections;
                long localTimeoutThreadWake = 0;
                while (true)
                {
                    checkConnections = false;
                    synchronized (_lock)
                    {
                        if (localTimeoutThreadWake != 0)
                            _timeoutThreadWake = localTimeoutThreadWake;

                        long currentTime = System.currentTimeMillis();
                        if (_timeoutThreadWake > currentTime)
                        {
                            if (_shutdown)
                            {
                                _log.debug("Timeout thread ending (shutdown)");
                                return;
                            }
                            try
                            {
                                long waitTime = _timeoutThreadWake
                                    - currentTime;

                                if (_log.isDebugEnabled())
                                {
                                    _log.debug("Timeout wake time: "
                                        + _timeoutThreadWake
                                        + " waitTime: "
                                        + waitTime);
                                }
                                _lock.wait(waitTime);
                            }
                            catch (InterruptedException ex)
                            {
                                if (_log.isDebugEnabled())
                                {
                                    _log.debug("Interrupted at: "
                                        + System.currentTimeMillis());
                                }
                                // Ignore
                            }

                            if (!_threadNoCheck)
                                checkConnections = true;
                            _threadNoCheck = false;
                        }
                        else
                        {
                            _log.debug("Timeout thread ending - nothing to do");
                            break;
                        }
                    }

                    // Have to check the connections outside of the lock
                    if (checkConnections)
                    {
                        localTimeoutThreadWake = _connManager
                                .checkIdleConnections();
                    }
                    else
                    {
                        localTimeoutThreadWake = 0;
                    }
                }
            }
            catch (InterruptedException e)
            {
                _log.debug("Interrupted - exiting", e);
                return;
            }
            finally
            {
                synchronized (_lock)
                {
                    _timeoutThread = null;
                    // Shutdown may have been requested while this thread is
                    // up
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
            long killTime = System.currentTimeMillis() + conn._idleTimeout;

            if (_log.isDebugEnabled())
            {
                _log.debug("startIdleTimeout: timeout: "
                    + conn._idleTimeout
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
                    if (_log.isDebugEnabled())
                    {
                        _log.debug("Altering timeout thread wake time to: "
                            + killTime);
                    }
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
