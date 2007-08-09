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
public class StreamingChunkedOutputStream extends AutoRetryOutputStream
{
    private static final Log  _log   = LogUtils.makeLogger();

    private static final byte ZERO[] = new byte[] { (byte)'0' };

    public StreamingChunkedOutputStream(OutputStream str,
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
            + " on a chunked stream; if you must, "
            + "wrap this stream in a BufferedOutputStream()");
    }

    public void write(byte[] b) throws IOException
    {
        write(b, 0, b.length);
    }

    public void write(byte[] b, int off, int len) throws IOException
    {
        try
        {
            // Write the chunk as the size of the buffer
            _outStr.write(Integer.toHexString(len)
                    .getBytes(Util.DEFAULT_ENCODING));
            _outStr.write(Util.CRLF_BYTES, 0, Util.CRLF_BYTES.length);
            _outStr.write(b, off, len);
            _outStr.write(Util.CRLF_BYTES, 0, Util.CRLF_BYTES.length);
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
        
        _outStr.write(ZERO, 0, ZERO.length);
        _outStr.write(Util.CRLF_BYTES, 0, Util.CRLF_BYTES.length);
        _outStr.write(Util.CRLF_BYTES, 0, Util.CRLF_BYTES.length);
    }

}
