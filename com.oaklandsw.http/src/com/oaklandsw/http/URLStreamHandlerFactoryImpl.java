/*
 * Copyright 2004 oakland software, incorporated. All rights Reserved.
 */
package com.oaklandsw.http;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

public class URLStreamHandlerFactoryImpl implements URLStreamHandlerFactory
{

    public URLStreamHandler createURLStreamHandler(String protocol)
    {
        if (protocol.equalsIgnoreCase("http"))
            return new com.oaklandsw.http.Handler();
        else if (protocol.equalsIgnoreCase("https"))
            return new com.oaklandsw.https.Handler();
        return null;
    }

}
