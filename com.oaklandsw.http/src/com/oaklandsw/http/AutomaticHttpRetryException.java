/*
 * Copyright 2007 Oakland Software Incorporated. All rights Reserved.
 */
package com.oaklandsw.http;

/**
 * Used to tell the user that a failure occured in processing connection, but
 * that it will be automatically retried.
 * <p>
 * This situation can occur in the pipelined connection handling when a user is
 * reading the response and the underlying connection closes. If this is thrown,
 * the HttpURLConnection will be automatically retried. Thus the user should
 * discard the processing for that HttpURLConnection as the callback will be
 * called again.
 * 
 */
public class AutomaticHttpRetryException extends HttpException
{

    public AutomaticHttpRetryException(String message)
    {
        super(message);
    }
}
