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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.oaklandsw.utillog.Log;

import com.oaklandsw.util.LogUtils;

/**
 * Closes a HttpConnection as soon as the end of the stream is reached.
 * 
 * @author Ortwin Gl�ck
 * 
 * @since 2.0
 */

class AutoCloseInputStream extends FilterInputStream
{
    private static final Log        _log = LogUtils.makeLogger();

    private HttpURLConnection _urlCon;

    /**
     * Create a new auto closing stream for the provided connection
     * 
     * @param inStr
     *            the input stream to read from
     * @param conn
     *            the connection to close when done reading
     */
    public AutoCloseInputStream(InputStream inStr, HttpURLConnection urlCon)
    {
        super(inStr);
        _urlCon = urlCon;

    }

    /**
     * Reads the next byte of data from the input stream.
     * 
     * @throws IOException
     *             when there is an error reading
     * @return the character read, or -1 for EOF
     */
    public int read() throws IOException
    {
        int l = super.read();
        if (l == -1)
        {
            close();
        }
        return l;
    }

    /**
     * Reads up to <code>len</code> bytes of data from the stream.
     * 
     * @param b
     *            a <code>byte</code> array to read data into
     * @param off
     *            an offset within the array to store data
     * @param len
     *            the maximum number of bytes to read
     * @return the number of bytes read or -1 for EOF
     * @throws IOException
     *             if there are errors reading
     */
    public int read(byte[] b, int off, int len) throws IOException
    {
        int l = super.read(b, off, len);
        if (l == -1)
        {
            close();
        }
        return l;
    }

    /**
     * Reads some number of bytes from the input stream and stores them into the
     * buffer array b.
     * 
     * @param b
     *            a <code>byte</code> array to read data into
     * @return the number of bytes read or -1 for EOF
     * @throws IOException
     *             if there are errors reading
     */
    public int read(byte[] b) throws IOException
    {
        int l = super.read(b);
        if (l == -1)
        {
            close();
        }
        return l;
    }

    public void close() throws IOException
    {
        _log.trace("close");
        super.close();
        _urlCon.releaseConnection(HttpURLConnection.CLOSE);
    }

}
