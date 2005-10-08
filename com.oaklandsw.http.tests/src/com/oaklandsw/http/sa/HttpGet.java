// Copyright 2002 (c) oakland software, All rights reserved

package com.oaklandsw.http.sa;

import java.net.HttpURLConnection;
import java.net.URL;

import com.oaklandsw.http.TestEnv;
import com.oaklandsw.log.Log;
import com.oaklandsw.log.LogFactory;
import com.oaklandsw.util.Util;

public class HttpGet
{

    private static final Log _log = LogFactory.getLog(HttpGet.class);

    public HttpGet()
    {
    }

    public static final void main(String[] args) throws Exception
    {
        TestEnv.setUp();

        String urlStr;
        if (args.length == 0)
            urlStr = "http://www.oaklandsoftware.com";
        else
            urlStr = args[0];

        URL url = new URL(urlStr);

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("GET");
        urlCon.connect();
        System.out.println(urlCon.getRequestProperty("User-Agent"));
        System.out.println("Response: " + urlCon.getResponseCode());
        Util.copyStreams(urlCon.getInputStream(), System.out);
    }

}
