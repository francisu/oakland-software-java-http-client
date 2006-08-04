//
// Copyright 2002-3003, oakland software, all rights reserved.
//
// May not be used or redistributed without specific written
// permission from oakland software.
//

package com.oaklandsw.http;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Releases and closes the connection when the stream is closed.
 */

class ReleaseInputStream extends FilterInputStream
{

    public Log                _log = LogFactory
                                           .getLog(ReleaseInputStream.class);

    private HttpURLConnection _urlCon;

    private boolean           _shouldClose;

    /**
     * Create a stream that releases upon close.
     * 
     * @param inStr
     *            the input stream to read from
     * @param conn
     *            the connection to close when done reading
     */
    public ReleaseInputStream(InputStream inStr,
            HttpURLConnection urlCon,
            boolean shouldClose)
    {

        super(inStr);
        _urlCon = urlCon;
        _shouldClose = shouldClose;
    }

    public void close() throws IOException
    {
        _log.trace("close");

        _urlCon.releaseConnection(_shouldClose);
    }

}
