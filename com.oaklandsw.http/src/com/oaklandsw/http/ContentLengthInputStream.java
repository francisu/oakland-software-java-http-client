//
// Some portions copyright 2002-3003, oakland software, all rights reserved.
//
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
import java.io.InputStream;

import com.oaklandsw.utillog.Log;

import com.oaklandsw.util.LogUtils;
import com.oaklandsw.util.Util;

/**
 * Cuts the wrapped InputStream off after a specified number of bytes.
 * 
 * @author Ortwin Gl�ck
 * @since 2.0
 */

public class ContentLengthInputStream extends AutoRetryInputStream
{
    private static final Log _log = LogUtils.makeLogger();

    private int              _contentLength;

    private int              _pos;

    /**
     * Creates a new length limited stream
     * 
     * @param inStr
     *            The stream to wrap
     * @param cl
     *            The maximum number of bytes that can be read from the stream.
     *            Subsequent read operations will return -1.
     * @param throwAutoRetry
     *            used when an AutomaticHttpRetryException should be thrown if
     *            there is an IOException on a read.
     */
    public ContentLengthInputStream(InputStream inStr,
            HttpURLConnectInternal urlCon,
            int cl,
            boolean throwAutoRetry)
    {
        super(inStr, urlCon, throwAutoRetry);
        _contentLength = cl;
    }

    public int read() throws IOException
    {
        if (_pos >= _contentLength)
            return -1;
        _pos++;
        int c;
        try
        {
            c = _inStr.read();
        }
        catch (IOException ex)
        {
            processIOException(ex);
            // Keeps compiler happy
            return 0;
        }

        if (c == -1 && _pos < _contentLength)
            throwShort();
        return c;
    }

    public int read(byte[] b) throws IOException
    {
        return read(b, 0, b.length);
    }

    public int read(byte[] b, int off, int len) throws IOException
    {
        if (_pos >= _contentLength)
            return -1;
        if (_pos + len > _contentLength)
            len = _contentLength - _pos;

        int count;

        try
        {
            count = _inStr.read(b, off, len);
        }
        catch (IOException ex)
        {
            processIOException(ex);
            // Keeps compiler happy
            return 0;
        }

        if (count == -1 && _pos < _contentLength)
            throwShort();
        else
            _pos += count;
        return count;
    }

    private void throwShort() throws IOException
    {
        String warn = "Connection closed before all content (of "
            + _contentLength
            + " bytes) was read (try #"
            + _urlCon.getActualTries()
            + ")";
        _log.warn(warn);
        close(true);
        processIOException(new IOException(warn));
    }

    protected void closeSubclass(boolean closeConn) throws IOException
    {
        if (_pos < _contentLength)
        {
            try
            {
                if (_log.isDebugEnabled())
                {
                    _log.debug("Closing stream before all content read "
                        + "- flushing "
                        + (_contentLength - _pos)
                        + " bytes");
                }
                Util.flushStream(this);
            }
            catch (IOException ex)
            {
                // Something wrong during flushing, so close it
                _urlCon.releaseConnection(HttpURLConnection.CLOSE);
                return;
            }
        }

        // Prevent trying to flush again
        _pos = _contentLength;
    }

}
