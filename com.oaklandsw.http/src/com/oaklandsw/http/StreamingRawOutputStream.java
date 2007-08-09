// Copyright 2007 Oakland Software Incorporated
//

package com.oaklandsw.http;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.logging.Log;

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
