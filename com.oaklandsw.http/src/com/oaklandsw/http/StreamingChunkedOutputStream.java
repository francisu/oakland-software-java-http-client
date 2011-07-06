// Copyright 2007 Oakland Software Incorporated
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
import java.io.OutputStream;

import com.oaklandsw.utillog.Log;

import com.oaklandsw.util.LogUtils;
import com.oaklandsw.util.Util;

/**
 * Handles the chunked streaming mode for the HttpURLConnection output.
 */
public class StreamingChunkedOutputStream extends AutoRetryOutputStream
{
    private static final Log  _log   = LogUtils.makeLogger();

    private static final byte ZERO[] = new byte[] { (byte)'0' };

    public StreamingChunkedOutputStream(OutputStream str,
            HttpURLConnection urlCon,
            boolean throwAutoRetry)
    {
        super(str, urlCon, throwAutoRetry);
    }

    // Using a this method precludes writing an array
    public void write(int b) throws IOException
    {
        _urlCon.streamWriteFinished(!HttpURLConnectInternal.OK);
        throw new IllegalStateException("Cannot use write(int) "
            + " on a chunked stream; if you must, "
            + "wrap this stream in a BufferedOutputStream()");
    }

    public void write(byte[] b) throws IOException
    {
        write(b, 0, b.length);
    }

    public void write(byte[] b, int off, int len) throws IOException
    {
        try
        {
            // Write the chunk as the size of the buffer
            _outStr.write(Integer.toHexString(len)
                    .getBytes(Util.DEFAULT_ENCODING));
            _outStr.write(Util.CRLF_BYTES, 0, Util.CRLF_BYTES.length);
            _outStr.write(b, off, len);
            _outStr.write(Util.CRLF_BYTES, 0, Util.CRLF_BYTES.length);
        }
        catch (IOException ex)
        {
            processIOException(ex);
        }
    }

    public void closeSubclass(boolean closeConn) throws IOException
    {
        if (closeConn)
            return;
        
        _outStr.write(ZERO, 0, ZERO.length);
        _outStr.write(Util.CRLF_BYTES, 0, Util.CRLF_BYTES.length);
        _outStr.write(Util.CRLF_BYTES, 0, Util.CRLF_BYTES.length);
    }

}
