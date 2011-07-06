/*
 * Copyright 2007 Oakland Software Incorporated. All rights Reserved.
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
