/*
 * Copyright 2007 Oakland Software Incorporated. All rights Reserved.
 */
package com.oaklandsw.http;

import java.net.Socket;

/**
 * This is used to get direct access to all data on the socket connections
 * associated with HTTP.
 * 
 * 
 */
public interface ConnectionIOListener
{

    public void read(byte[] bytes,
                     int offset,
                     int length,
                     Socket socket,
                     HttpURLConnection urlCon);

    public void write(byte[] bytes,
                      int offset,
                      int length,
                      Socket socket,
                      HttpURLConnection urlCon);

}
