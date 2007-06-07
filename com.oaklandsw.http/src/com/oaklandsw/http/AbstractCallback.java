/*
 * Copyright 2007 Oakland Software Incorporated.  All rights Reserved.
 */
package com.oaklandsw.http;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Default implementation of the Callback interface that does nothing.
 */
public class AbstractCallback implements Callback
{

    public void readResponse(HttpURLConnection urlCon, InputStream is)
    {
    }

    public void writeRequest(HttpURLConnection urlCon, OutputStream os)
    {
    }

    public void error(HttpURLConnection urlCon, InputStream is, Exception ex)
    {
    }


}
