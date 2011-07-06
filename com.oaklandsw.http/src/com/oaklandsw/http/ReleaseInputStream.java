//
// Copyright 2002-3003, oakland software, all rights reserved.
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
 * Releases and closes the connection when the stream is closed.
 */

class ReleaseInputStream extends FilterInputStream
{
    private static final Log  _log = LogUtils.makeLogger();

    private HttpURLConnection _urlCon;

    private boolean           _shouldClose;

    /**
     * Create a stream that releases upon close.
     * 
     * @param inStr
     *            the input stream to read from
     * @param conn
     *            the connection to close when done reading
     */
    public ReleaseInputStream(InputStream inStr,
            HttpURLConnection urlCon,
            boolean shouldClose)
    {

        super(inStr);
        _urlCon = urlCon;
        _shouldClose = shouldClose;
    }

    public void close() throws IOException
    {
        _log.trace("close");

        _urlCon.releaseConnection(_shouldClose
            ? HttpURLConnection.CLOSE
            : HttpURLConnection.READING);
    }

}
