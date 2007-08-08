/*
 * Copyright 2007 Oakland Software Incorporated. All rights Reserved.
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
