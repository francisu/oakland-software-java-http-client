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
import java.io.InputStream;

import com.oaklandsw.utillog.Log;
import com.oaklandsw.util.LogUtils;

/**
 * Logs all data read to the wire log.
 * 
 * @author Ortwin Gl�ck
 * 
 * @since 2.0
 */

class WireLogInputStream extends FilterInputStream
{

    private static final Log _wireLog = LogUtils
                                              .makeLogger(HttpConnection.WIRE_LOG);

    private StringBuffer     _traceBuff;

    public WireLogInputStream(InputStream inStr)
    {
        super(inStr);
        _traceBuff = new StringBuffer();
    }

    private void traceChar(char b)
    {
        WireLogOutputStream.traceChar(_traceBuff, b);
        if (b == '\n')
            dumpBuff();
    }

    public int read() throws java.io.IOException
    {
        int b = super.read();
        traceChar((char)b);
        dumpBuff();
        return b;
    }

    public int read(byte[] b, int off, int len) throws java.io.IOException
    {
        int l = super.read(b, off, len);
        if (l > 0)
        {
            for (int i = 0; i < l; i++)
                traceChar((char)b[off + i]);
            dumpBuff();
        }
        return l;
    }

    private final void dumpBuff()
    {
        if (_traceBuff.length() != 0)
        {
            // Don't write the trailing newline
            String tbString = _traceBuff.toString();
            if (tbString.charAt(_traceBuff.length() - 1) == '\n')
                tbString = tbString.substring(0, _traceBuff.length() - 1);
            _wireLog.debug("<< " + tbString);
            _traceBuff = new StringBuffer();
        }
    }

    public void close() throws java.io.IOException
    {
        dumpBuff();
        super.close();
    }

}
