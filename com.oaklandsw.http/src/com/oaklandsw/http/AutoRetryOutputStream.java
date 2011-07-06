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

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.SocketTimeoutException;

import com.oaklandsw.utillog.Log;

import com.oaklandsw.util.LogUtils;

/**
 * Used to throw an AutomaticHttpRetryException is something goes wrong.
 */
public abstract class AutoRetryOutputStream extends OutputStream
{
    private static final Log    _log             = LogUtils.makeLogger();

    protected HttpURLConnection _urlCon;

    protected boolean           _throwAutoRetry;

    protected OutputStream      _outStr;

    protected boolean           _closed;

    public static final boolean THROW_AUTO_RETRY = true;

    public AutoRetryOutputStream(OutputStream outStr,
            HttpURLConnection urlCon,
            boolean throwAutoRetry)
    {
        super();
        _outStr = outStr;
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

        try
        {
            closeSubclass(closeConn);

            // No flush here, it's done in streamWriteFinished so that things
            // are batched, but this might throw and we want to do the auto
            // retry thing if it does
            _urlCon.streamWriteFinished(HttpURLConnectInternal.OK);
            _closed = true;
        }
        catch (IOException ex)
        {
            // Mark closed so we don't reenter this method
            _closed = true;
            processIOException(ex);
        }
    }

    protected abstract void closeSubclass(boolean closeConn) throws IOException;

    protected void processIOException(IOException ex)
        throws IOException,
            InterruptedIOException
    {
        if (_log.isDebugEnabled())
            _log.debug("IOException on write", ex);

        close(true);

        _urlCon.streamWriteFinished(!HttpURLConnectInternal.OK);

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
                _log
                        .debug("Converted " + ex + " to HttpTimeoutException",
                               htex);
            }
            throw htex;
        }
        throw ex;
    }

    // This is only used if the user really wants to flush
    public void flush() throws IOException
    {
        try
        {
            _outStr.flush();
        }
        catch (IOException ex)
        {
            processIOException(ex);
        }
    }

}
