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
public class StreamingRawOutputStream extends OutputStream
{
    private static final Log  _log   = LogUtils.makeLogger();

    private boolean           _closed;

    private OutputStream      _stream;
    private HttpURLConnection _urlCon;

    public StreamingRawOutputStream(HttpURLConnection urlCon,
            OutputStream str)
    {
        _stream = str;
        _urlCon = urlCon;
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
        write(b, 0, b.length);
    }

    public void write(byte[] b, int off, int len) throws IOException
    {
        // Pass it through
        _stream.write(b, off, len);
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
            _stream.flush();
            try
            {
                _urlCon.streamWriteFinished(HttpURLConnectInternal.OK);
            }
            catch (IOException ioe)
            {
                _log.debug("Unexpected exception on closing raw output "
                    + " stream", ioe);
                _urlCon.streamWriteFinished(!HttpURLConnectInternal.OK);
                throw ioe;
            }
            finally
            {
                _closed = true;
                super.close();
            }
        }

    }

}
