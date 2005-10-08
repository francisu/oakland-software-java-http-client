//
// Copyright 2002-3003, oakland software, all rights reserved.
//
// May not be used or redistributed without specific written
// permission from oakland software.
//

package com.oaklandsw.https;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class Handler extends URLStreamHandler
{
    public URLConnection openConnection(URL url) throws IOException
    {
        return new com.oaklandsw.http.HttpURLConnectInternal(url);
    }
}
