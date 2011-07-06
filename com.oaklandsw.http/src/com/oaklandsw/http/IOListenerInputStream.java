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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.oaklandsw.util.Util;

public class IOListenerInputStream extends FilterInputStream
{

    protected ConnectionIOListener _listener;
    protected HttpConnection       _conn;
    protected HttpURLConnection    _urlCon;

    public IOListenerInputStream(InputStream str,
            ConnectionIOListener l,
            HttpConnection conn,
            HttpURLConnection urlCon)
    {
        super(str);
        _listener = l;
        _conn = conn;
        _urlCon = urlCon;
    }

    public int read() throws IOException
    {
        Util.impossible("This method should not be used");
        return 0;
    }

    public int read(byte b[]) throws IOException
    {
        return read(b, 0, b.length);
    }

    public int read(byte b[], int off, int len) throws IOException
    {
        int result = super.read(b, off, len);
        if (result > 0)
            _listener.read(b, off, result, _conn._socket, _urlCon);
        return result;
    }
}
