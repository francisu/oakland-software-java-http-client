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

/**
 * Handles the raw streaming mode for the HttpURLConnection output.
 */
public class StreamingRawOutputStream extends AutoRetryOutputStream
{
    private static final Log _log = LogUtils.makeLogger();

    public StreamingRawOutputStream(OutputStream str,
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
            + " on a raw stream; if you must, "
            + "wrap this stream in a BufferedOutputStream()");
    }

    public void write(byte[] b) throws IOException
    {
        try
        {
            write(b, 0, b.length);
        }
        catch (IOException ex)
        {
            processIOException(ex);
        }
    }

    public void write(byte[] b, int off, int len) throws IOException
    {
        try
        {
            // Pass it through
            _outStr.write(b, off, len);
        }
        catch (IOException ex)
        {
            processIOException(ex);
        }
    }

    public void closeSubclass(boolean closeConn) throws IOException
    {
        // Nothing
    }

}
