/*
 * Copyright 2007 Oakland Software Incorporated. All rights Reserved.
 */
package com.oaklandsw.http;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.oaklandsw.util.Util;

public class IOListenerOutputStream extends FilterOutputStream
{

    protected ConnectionIOListener _listener;
    protected HttpConnection       _conn;
    protected HttpURLConnection    _urlCon;

    public IOListenerOutputStream(OutputStream str,
            ConnectionIOListener l,
            HttpConnection conn,
            HttpURLConnection urlCon)
    {
        super(str);
        _listener = l;
        _conn = conn;
        _urlCon = urlCon;
    }

    public void write(int c) throws IOException
    {
        Util.impossible("This method should not be used");
    }

    public void write(byte b[]) throws IOException
    {
        write(b, 0, b.length);
    }

    public void write(byte b[], int off, int len) throws IOException
    {
        _listener.write(b, off, len, _conn._socket, _urlCon);
        out.write(b, off, len);
    }
}
