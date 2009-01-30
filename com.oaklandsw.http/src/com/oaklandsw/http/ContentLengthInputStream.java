//
// Some portions copyright 2002-3003, oakland software, all rights reserved.
//
// May not be used or redistributed without specific written
// permission from oakland software.
//
/*
 * ====================================================================
 * 
 * The Apache Software License, Version 1.1
 * 
 * Copyright (c) 1999-2002 The Apache Software Foundation. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any, must
 * include the following acknowlegement: "This product includes software
 * developed by the Apache Software Foundation (http://www.apache.org/)."
 * Alternately, this acknowlegement may appear in the software itself, if and
 * wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "The Jakarta Project", "HttpClient", and "Apache Software
 * Foundation" must not be used to endorse or promote products derived from this
 * software without prior written permission. For written permission, please
 * contact apache@apache.org.
 * 
 * 5. Products derived from this software may not be called "Apache" nor may
 * "Apache" appear in their names without prior written permission of the Apache
 * Group.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE APACHE
 * SOFTWARE FOUNDATION OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many individuals on
 * behalf of the Apache Software Foundation. For more information on the Apache
 * Software Foundation, please see <http://www.apache.org/>.
 * 
 * [Additional notices, if required by prior licensing conditions]
 * 
 */

package com.oaklandsw.http;

import java.io.IOException;
import java.io.InputStream;

import com.oaklandsw.util.Log;

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
