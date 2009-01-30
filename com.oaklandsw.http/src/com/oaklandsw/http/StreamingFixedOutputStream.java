// Copyright 2007 Oakland Software Incorporated
//

package com.oaklandsw.http;

import java.io.IOException;
import java.io.OutputStream;

import com.oaklandsw.util.Log;

import com.oaklandsw.util.LogUtils;

/**
 * Handles the fixed streaming mode for the HttpURLConnection output. This just
 * passes the data through to the underlying stream and checks that the amount
 * equals the expected size.
 */
public class StreamingFixedOutputStream extends AutoRetryOutputStream
{
    private static final Log _log = LogUtils.makeLogger();

    private int              _bytesWritten;

    // The content-length
    protected int            _size;

    public StreamingFixedOutputStream(OutputStream str,
            HttpURLConnection urlCon,
            boolean throwAutoRetry,
            int size)
    {
        super(str, urlCon, throwAutoRetry);
        _size = size;
    }

    public void write(int b) throws IOException
    {
        try
        {
            _outStr.write(b);
            _bytesWritten++;
        }
        catch (IOException ex)
        {
            processIOException(ex);
        }
    }

    public void write(byte[] b) throws IOException
    {
        try
        {
            _outStr.write(b, 0, b.length);
            _bytesWritten += b.length;
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
            _outStr.write(b, off, len);
            _bytesWritten += len;
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
        
        // No flush here, it's done in streamWriteFinished so that things
        // are batched

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
    }

}
