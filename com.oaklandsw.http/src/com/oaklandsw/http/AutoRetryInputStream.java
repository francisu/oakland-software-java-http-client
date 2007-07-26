/*
 * Copyright 2007 Oakland Software Incorporated. All rights Reserved.
 */
package com.oaklandsw.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;

import org.apache.commons.logging.Log;

import com.oaklandsw.util.LogUtils;

/**
 * Used to throw an AutomaticHttpRetryException is something goes wrong.
 */
public abstract class AutoRetryInputStream extends InputStream
{
    private static final Log         _log             = LogUtils.makeLogger();

    protected HttpURLConnectInternal _urlCon;

    protected boolean                _throwAutoRetry;

    protected InputStream            _inStr;

    protected boolean                _closed;

    public static final boolean      THROW_AUTO_RETRY = true;

    public AutoRetryInputStream(InputStream inStr,
            HttpURLConnectInternal urlCon,
            boolean throwAutoRetry)
    {
        super();
        _inStr = inStr;
        _throwAutoRetry = throwAutoRetry;
        _urlCon = urlCon;
    }

    public void close() throws IOException
    {
        close(false);
    }

    public void close(boolean closeConn) throws IOException
    {
        _log.debug("close");

        if (_closed)
            return;
        _closed = true;

        if (closeConn)
        {
            _log.debug("Closing underlying connection due to error on read");
            _urlCon.releaseConnection(HttpURLConnection.CLOSE);
            return;
        }

        closeSubclass(closeConn);

        _urlCon.releaseConnection(HttpURLConnection.READING);
    }

    protected abstract void closeSubclass(boolean closeConn) throws IOException;

    protected void processIOException(IOException ex)
        throws IOException,
            InterruptedIOException
    {
        if (_log.isDebugEnabled())
            _log.debug("IOException on read", ex);

        close(true);

        if (_throwAutoRetry)
        {
            // Record this in the urlcon so the pipeline mechanism
            // knows it needs to retry
            _urlCon._ioException = new AutomaticHttpRetryException(ex
                    .getMessage());
            _urlCon._ioException.initCause(ex);
            throw _urlCon._ioException;
        }

        // Note we want to handle this as an HttpTimeoutException, because this
        // does not mean the thread was interrupted (despite it being a subclass
        // if InterruptedIOException)
        if (ex instanceof SocketTimeoutException)
        {
            _urlCon._dead = true;
            HttpTimeoutException htex = new HttpTimeoutException();
            htex.initCause(ex);
            if (_log.isDebugEnabled())
            {
                _log.debug("Converted to " + ex + " to HttpTimeoutException",
                           htex);
            }
            throw htex;
        }
        throw ex;

    }

    public long skip(long n) throws IOException
    {
        return _inStr.skip(n);
    }

    public int available() throws IOException
    {
        return _inStr.available();
    }

    public synchronized void mark(int readlimit)
    {
        _inStr.mark(readlimit);
    }

    public synchronized void reset() throws IOException
    {
        _inStr.reset();
    }

    public boolean markSupported()
    {
        return _inStr.markSupported();
    }

}
