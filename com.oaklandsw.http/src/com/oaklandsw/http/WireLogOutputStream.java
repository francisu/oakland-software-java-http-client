//
// Copyright 2002-3003, oakland software, all rights reserved.
//
// May not be used or redistributed without specific written
// permission from oakland software.
//

package com.oaklandsw.http;

import java.io.OutputStream;

import com.oaklandsw.util.Log;
import com.oaklandsw.util.LogUtils;

class WireLogOutputStream extends OutputStream
{

    private static final Log _wireLog = LogUtils
                                              .makeLogger(HttpConnection.WIRE_LOG);

    private StringBuffer     _traceBuff;

    protected OutputStream   _out;

    public WireLogOutputStream(OutputStream outStr)
    {
        super();
        _out = outStr;
        _traceBuff = new StringBuffer();
    }

    public void write(int b) throws java.io.IOException
    {
        traceChar(_traceBuff, (char)b);
        dumpBuff();
        _out.write(b);
    }

    public void write(byte[] b, int off, int len) throws java.io.IOException
    {
        for (int i = 0; i < len; i++)
            traceChar(_traceBuff, (char)b[i]);
        dumpBuff();
        _out.write(b, off, len);
    }

    public void flush() throws java.io.IOException
    {
        dumpBuff();
        _out.flush();
    }

    public void close() throws java.io.IOException
    {
        dumpBuff();
        _out.close();
    }

    public static void traceChar(StringBuffer traceBuff, char b)
    {
        if (b == '\n')
            traceBuff.append("[\\n]\n");
        else if (b == '\r')
            traceBuff.append("[\\r]");
        else if (b < ' ')
            traceBuff.append("[" + Integer.toHexString(b) + "]");
        else
            traceBuff.append(b);
    }

    private final void dumpBuff()
    {
        if (_traceBuff.length() != 0)
        {
            // Don't write the trailing newline
            String tbString = _traceBuff.toString();
            if (tbString.charAt(_traceBuff.length() - 1) == '\n')
                tbString = tbString.substring(0, _traceBuff.length() - 1);
            _wireLog.debug(">> " + tbString);
            _traceBuff = new StringBuffer();
        }
    }


}
