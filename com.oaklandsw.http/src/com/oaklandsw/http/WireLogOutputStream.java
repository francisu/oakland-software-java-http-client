//
// Copyright 2002-3003, oakland software, all rights reserved.
//
// May not be used or redistributed without specific written
// permission from oakland software.
//

package com.oaklandsw.http;

import java.io.BufferedOutputStream;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Logs all data read to the wire log.
 * 
 */

class WireLogOutputStream extends BufferedOutputStream
{

    private Log          _wireLog = LogFactory.getLog(HttpConnection.WIRE_LOG);

    private StringBuffer _traceBuff;

    public WireLogOutputStream(OutputStream in)
    {
        super(in);
        _traceBuff = new StringBuffer();
    }

    public void write(byte b) throws java.io.IOException
    {
        traceChar(_traceBuff, (char)b);
        super.write(b);
    }

    public void write(byte[] b, int off, int len) throws java.io.IOException
    {
        for (int i = 0; i < len; i++)
            traceChar(_traceBuff, (char)b[i]);
        super.write(b, off, len);
    }

    public void flush() throws java.io.IOException
    {
        dumpBuff();
        super.flush();
    }

    public void close() throws java.io.IOException
    {
        dumpBuff();
        super.close();
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
