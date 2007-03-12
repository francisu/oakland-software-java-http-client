package com.oaklandsw.http.sa;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.oaklandsw.util.Util;
import com.oaklandsw.http.HttpTestEnv;

public class CallMethod 
{

    public static void main(String args[])
    throws Exception
    {
        HttpTestEnv.setUp();

        URL url = new URL("http://localhost:8085/oaklandsw/params");

        HttpURLConnection urlCon = 
            (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("DELETE");
        urlCon.connect();

        System.out.println("code: " + urlCon.getResponseCode());

        InputStream is = urlCon.getInputStream();
        if (is == null)
        {
            System.out.println("response: " + 
                               Util.getStringFromInputStream(is, null));
        }
    }

}
