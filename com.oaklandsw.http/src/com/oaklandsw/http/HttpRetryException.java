/*
 * Copyright 2007 Oakland Software Incorporated. All rights Reserved.
 */
package com.oaklandsw.http;

/**
 * Used when an HTTP request requires a retry, but cannot be retried
 * automatically because streaming mode
 * (HttpURLConnection.set[...]StreamingMode()) was enabled.
 * <p>
 * This is functionally identical to java.net.HttpRetryException introduced in
 * 1.5.
 * 
 */
public class HttpRetryException extends HttpException
{
    /**
     * The response code of the request, if a response was received from the
     * server.
     */
    protected int     _code;
    protected String  _location;

    /**
     * This is a request that could normally be retried (i.e. not a POST) and
     * the connection failed such that no response is available from the server.
     * This would normally be automatically retried, were it // not a streaming
     * request.
     */
    protected boolean _retryDueToFailure;

    public HttpRetryException(String message, int code)
    {
        this(message, code, null);
    }

    public HttpRetryException(String message, int code, String location)
    {
        super(message);
        _code = code;
        _location = location;
    }

    public boolean isRetryDueToFailure()
    {
        return _retryDueToFailure;
    }

    public void setRetryDueToFailure(boolean rdtf)
    {
        _retryDueToFailure = rdtf;
    }

    public int responseCode()
    {
        return _code;
    }

    public String getLocation()
    {
        return _location;
    }

    public String getReason()
    {
        // FIXME - is this right?
        return getMessage();
    }

    public String toString()
    {
        return getMessage()
            + " responseCode: "
            + _code
            + " location: "
            + _location;
    }

}
