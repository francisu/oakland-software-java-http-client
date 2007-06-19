

import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.logging.Log;

import com.oaklandsw.util.LogUtils;
import com.oaklandsw.util.Util;

public class HttpGet
{

    private static final Log   _log         = LogUtils.makeLogger();

    public HttpGet()
    {
    }

    public static final void main(String[] args) throws Exception
    {
        System.setProperty("java.protocol.handler.pkgs", "com.oaklandsw");

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
