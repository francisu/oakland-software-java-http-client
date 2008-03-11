/*
 * Copyright 2008 Oakland Software Incorporated. All rights Reserved.
 */
package com.oaklandsw.http;

import java.net.Socket;

import org.apache.commons.logging.Log;

import com.oaklandsw.util.LogUtils;

/**
 * Creates the Socket to be used by the HTTP connection.
 */
public class AbstractSocketFactory
{
    private static final Log   _log             = LogUtils.makeLogger();

    /**
     * Creates a socket to be used by an HTTP connection.
     * 
     * The default implementation returns a standard socket. This can be
     * overridden as required for things like a SOCKS connection.
     * 
     * Don't include the host/port on the constructor for the socket, as a
     * connect() call is always issued after the socket is created. These are
     * provided only for information.
     * 
     * @param urlCon
     *            the HttpURLConnection that is using this socket
     * @param host
     *            the hostname of the socket
     * @param port
     *            the port number of the socket
     * @return
     */
    public Socket createSocket(HttpURLConnection urlCon, String host, int port)
    {
        _log.debug("createSocket (default socket factory)");
        return new Socket();
    }

}
