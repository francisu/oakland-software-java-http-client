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
 * Used to tell the user that a failure occurred in processing connection, but
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
        _retryable = true;
    }
}
