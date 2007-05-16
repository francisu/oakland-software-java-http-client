// Copyright 2007 Oakland Software Incorporated
//

package com.oaklandsw.http;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.logging.Log;

import com.oaklandsw.util.LogUtils;
import com.oaklandsw.util.Util;

/**
 * Handles the chunked streaming mode for the HttpURLConnection output.
 */
public class StreamingChunkedOutputStream extends OutputStream
{
    private static final Log  _log   = LogUtils.makeLogger();

    private boolean           _closed;

    private OutputStream      _stream;
    private HttpURLConnection _urlCon;

    private static final byte ZERO[] = new byte[] { (byte)'0' };

    public StreamingChunkedOutputStream(HttpURLConnection urlCon,
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
            + " on a chunked stream; if you must, "
            + "wrap this stream in a BufferedOutputStream()");
    }

    public void write(byte[] b) throws IOException
    {
        write(b, 0, b.length);
    }

    public void write(byte[] b, int off, int len) throws IOException
    {
        // Write the chunk as the size of the buffer
        _stream.write(Integer.toHexString(len).getBytes(Util.DEFAULT_ENCODING));
        _stream.write(Util.CRLF_BYTES, 0, Util.CRLF_BYTES.length);
        _stream.write(b, off, len);
        _stream.write(Util.CRLF_BYTES, 0, Util.CRLF_BYTES.length);
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
            try
            {
                // Write the end marker
                _stream.write(ZERO, 0, ZERO.length);
                _stream.write(Util.CRLF_BYTES, 0, Util.CRLF_BYTES.length);
                _stream.write(Util.CRLF_BYTES, 0, Util.CRLF_BYTES.length);
                _stream.flush();
                _urlCon.streamWriteFinished(HttpURLConnectInternal.OK);
            }
            catch (IOException ioe)
            {
                _log.debug("Unexpected exception on closing chunked output "
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
