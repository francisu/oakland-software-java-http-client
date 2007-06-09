/*
 * Copyright 2007 Oakland Software Incorporated. All rights Reserved.
 */
package com.oaklandsw.http;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Used for pipelined and non-blocking HttpURLConnections.
 * 
 * This notifies the user that the response is ready to read (pipelined and
 * non-blocking) or the request is ready to be written (non-blocking).
 * 
 * The order that the HttpURLConnections are presented to this Callback is
 * determined by the order in which the connections are associated with the
 * callback. If you are using a default callback (setDefaultCallback()) this is
 * the order in which the HttpURLConnections are created.
 */
public interface Callback
{

    /**
     * This connection is ready to have its data written. This is used only for
     * a non-blocking connection when using streaming and is called after the
     * underlying socket connection has been established and the request headers
     * have been sent.
     * 
     * @param urlCon
     *            the connection.
     * @param os
     *            the OutputStream on which the data is written. Call the
     *            close() method on this when done.
     */
    public void writeRequest(HttpURLConnection urlCon, OutputStream os);

    /**
     * This connection is ready to have its data read, and has completed
     * successfully. This is used either for a pipelined or a non-blocking
     * connection, and is called after the response has been received. This will
     * be called only once per connection, and in the same order as writeRequest
     * is called.
     * 
     * @param urlCon
     *            the connection.
     * @param is
     *            the InputStream on which to read the data. Call the close()
     *            method on this when done. This will be provided even in the
     *            exception case if there is anything to read.
     */
    public void readResponse(HttpURLConnection urlCon,
                             InputStream is);

    /**
     * Some error happened that caused the connection to fail. This could either
     * be an IOException or one of its subclasses or an invalid response code
     * from the server.
     * 
     * @param urlCon
     *            the connection.
     * @param is
     *            the InputStream on which to read the data, if there is any
     *            data to report.
     * @param ex
     *            an Exception.
     */
    public void error(HttpURLConnection urlCon, InputStream is, Exception ex);

}
