// Copyright 2007 Oakland Software Incorporated
//

package com.oaklandsw.http;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.logging.Log;

import com.oaklandsw.util.LogUtils;

/**
 * Handles the fixed streaming mode for the HttpURLConnection output. This just
 * passes the data through to the underlying stream and checks that the amount
 * equals the expected size.
 */
public class StreamingFixedOutputStream extends OutputStream
{
    private static final Log    _log = LogUtils.makeLogger();

    private boolean             _closed;

    private OutputStream        _stream;

    private int                 _bytesWritten;

    protected HttpURLConnection _urlCon;

    // The content-length
    protected int               _size;

    public StreamingFixedOutputStream(HttpURLConnection urlCon,
            OutputStream str,
            int size)
    {
        _size = size;
        _stream = str;
        _urlCon = urlCon;
    }

    public void write(int b) throws IOException
    {
        _stream.write(b);
        _bytesWritten++;
    }

    public void write(byte[] b) throws IOException
    {
        _stream.write(b, 0, b.length);
        _bytesWritten += b.length;
    }

    public void write(byte[] b, int off, int len) throws IOException
    {
        _stream.write(b, off, len);
        _bytesWritten += len;
    }

    public void flush() throws IOException
    {
        // Just pass this through
        _stream.flush();
    }

    public void close() throws IOException
    {
        if (!_closed)
        {
            _closed = true;
            _stream.flush();
            super.close();

            if (_bytesWritten != _size)
            {
                // Tell the URL connection we have a problem so it can clean
                // itself up
                _urlCon.streamWriteFinished(!HttpURLConnectInternal.OK);
                throw new HttpException("Streaming fixed length mismatch: bytes written: "
                    + _bytesWritten
                    + " requested size: "
                    + _size);
            }
            _urlCon.streamWriteFinished(HttpURLConnectInternal.OK);
        }

    }

}
