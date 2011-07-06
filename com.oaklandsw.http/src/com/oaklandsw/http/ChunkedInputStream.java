//
// Some portions copyright 2002-2007, oakland software, all rights reserved.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.oaklandsw.utillog.Log;

import com.oaklandsw.util.LogUtils;
import com.oaklandsw.util.Util;

/**
 * <p>
 * Transparently coalesces chunks of a HTTP stream that uses Transfer-Encoding
 * chunked.
 * </p>
 * 
 * @see ResponseInputStream
 * 
 * @author Ortwin Gl�ck
 * @author Sean C. Sullivan
 * @author Martin Elwin
 * 
 * @since 2.0
 * 
 */

public class ChunkedInputStream extends AutoRetryInputStream
{
    private static final Log    _log     = LogUtils.makeLogger();

    private int                 chunkSize, pos;

    private boolean             eof;

    private static final String HTTP_ENC = "US-ASCII";

    /**
     * 
     * 
     * @param inStr
     *            must be non-null
     * @param urlCon
     *            must be non-null
     * 
     * @throws java.io.IOException
     * @throws java.lang.NullPointerException
     * 
     */
    public ChunkedInputStream(final InputStream inStr,
            final HttpURLConnectInternal urlCon,
            boolean throwAutoRetry) throws IOException
    {
        super(inStr, urlCon, throwAutoRetry);
        this.chunkSize = getChunkSizeFromInputStream(inStr);
        if (chunkSize == 0)
        {
            eof = true;
            parseFooters();
        }
        this.pos = 0;
    }

    /**
     * Returns all the data in a chunked stream in coalesced form. A chunk is
     * followed by a CRLF. The method returns -1 as soon as a chunksize of 0 is
     * detected. Footers are read automcatically at the end of the stream and
     * can be obtained with the getFooters() method.
     * 
     * @return -1 of the end of the stream has been reached or the next data
     *         byte
     * @throws IOException
     */
    public int read() throws IOException
    {
        try
        {
            if (eof)
                return -1;
            if (pos >= chunkSize)
            {
                nextChunk();
                if (eof)
                    return -1;
            }
            pos++;
            return _inStr.read();

        }
        catch (IOException ex)
        {
            processIOException(ex);
            // Keep compiler happy
            return 0;
        }
    }

    public int read(byte[] b) throws java.io.IOException
    {
        return read(b, 0, b.length);
    }

    public int read(byte[] b, int off, int len) throws java.io.IOException
    {
        try
        {
            if (eof)
                return -1;
            if (pos >= chunkSize)
            {
                nextChunk();
                if (eof)
                    return -1;
            }
            len = Math.min(len, chunkSize - pos);
            int count = _inStr.read(b, off, len);
            pos += count;
            return count;
        }
        catch (IOException ex)
        {
            processIOException(ex);
            // Keep compiler happy
            return 0;
        }
    }

    private void nextChunk() throws IOException
    {
        int cr = _inStr.read();
        int lf = _inStr.read();

        // This handles cases where there is no CRLF after the chunk. This
        // was encountered with resin.
        if (cr == '0' && lf == '\r')
        {
            lf = _inStr.read();
            if (lf == '\n')
            {
                eof = true;
                parseFooters();
                return;
            }
            throw new IOException("Incorrect end of chunk after 0/CR sequence, expected LF, got "
                + lf);
        }

        if ((cr != '\r') || (lf != '\n'))
        {
            throw new IOException("CRLF expected at end of chunk: "
                + cr
                + "/"
                + lf);
        }

        chunkSize = getChunkSizeFromInputStream(_inStr);
        pos = 0;
        if (chunkSize == 0)
        {
            eof = true;
            parseFooters();
        }
    }

    /**
     * Expects the stream to start with a chunksize in hex with optional
     * comments after a semicolon. The line must end with a CRLF: "a3; some
     * comment\r\n" Positions the stream at the start of the next line.
     * 
     * @return the chunk size as integer
     * 
     * @throws IOException
     *             when the chunk size could not be parsed
     * @throws java.lang.RuntimeException
     * 
     * 
     */
    private int getChunkSizeFromInputStream(final InputStream in)
        throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int state = 0; // 0: normal, 1: \r was scanned, 2: inside quoted
        // string, -1: end
        while (state != -1)
        {
            int b = in.read();
            if (b == -1)
            {
                // This could be a close
                processIOException(new IOException("chunked stream ended unexpectedly"));
            }
            switch (state)
            {
                case 0:
                    switch (b)
                    {
                        case '\r':
                            state = 1;
                            break;
                        case '\"':
                            state = 2;
                            /* fall through */
                        default:
                            baos.write(b);
                    }
                    break;

                case 1:
                    if (b == '\n')
                    {
                        state = -1;
                    }
                    else
                    {
                        // this was not CRLF
                        throw new IOException("Protocol violation: Unexpected single "
                            + "newline character in chunk size");
                    }
                    break;

                case 2:
                    switch (b)
                    {
                        case '\\':
                            b = in.read();
                            baos.write(b);
                            break;
                        case '\"':
                            state = 0;
                            /* fall through */
                        default:
                            baos.write(b);
                    }
                    break;
                default:
                    Util.impossible("Invalid state: " + state);
            }
        }

        // parse data
        String dataString = new String(baos.toByteArray(), HTTP_ENC);
        int separator = dataString.indexOf(';');
        dataString = (separator > 0) ? dataString.substring(0, separator)
                .trim() : dataString.trim();

        int result;
        try
        {
            result = Integer.parseInt(dataString.trim(), 16);
        }
        catch (NumberFormatException e)
        {
            throw new IOException("Bad chunk size: " + dataString);
        }
        return result;
    }

    /**
     * Stores the footers into map of Headers
     */
    private void parseFooters() throws IOException
    {
        String line = readLine();
        while ((line != null) && (!line.equals("")))
        {
            int colonPos = line.indexOf(':');
            if (colonPos != -1)
            {
                String key = line.substring(0, colonPos).trim();
                String val = line.substring(colonPos + 1).trim();
                if (_urlCon != null)
                    _urlCon.addResponseFooter(key, val);
            }
            line = readLine();
        }

    }

    private String readLine() throws IOException
    {
        StringBuffer buf = new StringBuffer();
        for (;;)
        {
            int ch = _inStr.read();
            if (ch < 0)
            {
                if (buf.length() == 0)
                {
                    return null;
                }
                break;
            }
            else if (ch == '\r')
            {
                continue;
            }
            else if (ch == '\n')
            {
                break;
            }
            buf.append((char)ch);
        }
        return (buf.toString());
    }

    protected void closeSubclass(boolean closeConn) throws IOException
    {
        try
        {
            Util.flushStream(this);
        }
        catch (IOException ex)
        {
            // The stream must have been closed, or something went
            // wrong, so do close it
            if (_urlCon != null)
                _urlCon.releaseConnection(HttpURLConnection.CLOSE);
            return;
        }
    }
}
