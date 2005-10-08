//
// Copyright 2002-3003, oakland software, all rights reserved.
//
// May not be used or redistributed without specific written
// permission from oakland software.
//
package com.oaklandsw.http;

/**
 * Indicates a timeout has occurred.
 * 
 * A timeout may occur:
 * <ul>
 * <li>Connection Establishment - When an initial TCP connection is being
 * established to the specified host.
 * <li>Connection Pool - When waiting for a connection to the host to become
 * available. By default, only 2 connections are allowed to a given host and if
 * more requests for connections to that host are made, they are queued until
 * one of the connections becomes available.
 * <li>Response - When waiting for the response to a request.
 * </ul>
 */
public class HttpTimeoutException extends RuntimeException
{
    /**
     * Creates a new HttpTimeoutException.
     */
    public HttpTimeoutException()
    {
        super();
    }

    /**
     * Creates a new HttpTimeoutException with the specified message.
     * 
     * @param message
     *            exception message
     */
    public HttpTimeoutException(String message)
    {
        super(message);
    }
}
