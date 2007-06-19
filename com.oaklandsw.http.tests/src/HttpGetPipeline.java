import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import com.oaklandsw.http.AutomaticHttpRetryException;
import com.oaklandsw.http.Callback;
import com.oaklandsw.http.HttpURLConnection;

public class HttpGetPipeline
{

    public HttpGetPipeline()
    {
    }

    public static class TestCallback implements Callback
    {

        public void writeRequest(HttpURLConnection urlCon, OutputStream os)
        {
            System.out.println("!!! should not be called");
        }

        public void readResponse(HttpURLConnection urlCon, InputStream is)
        {
            try
            {
                System.out.println("Response: " + urlCon.getResponseCode());

                // Print the output stream
                InputStream inputStream = urlCon.getInputStream();
                byte[] buffer = new byte[10000];
                int nb = 0;
                while (true)
                {
                    nb = inputStream.read(buffer);
                    if (nb == -1)
                        break;
                    if (false)
                        System.out.write(buffer, 0, nb);
                }
            }
            catch (AutomaticHttpRetryException arex)
            {
                System.out.println("Automatic retry: " + urlCon);
                // The read will be redriven
                return;
            }
            catch (IOException e)
            {
                System.out.println("ERROR - IOException: " + urlCon);
                e.printStackTrace();
            }
        }

        public void error(HttpURLConnection urlCon, InputStream is, Exception ex)
        {
            System.out.println("ERROR: " + urlCon);
            ex.printStackTrace();
        }

        public String toString()
        {
            return "Callback" + Thread.currentThread().getName();
        }
    }

    public static final void main(String[] args) throws Exception
    {
        System.setProperty("java.protocol.handler.pkgs", "com.oaklandsw");

        String urlStr;
        if (args.length == 0)
            urlStr = "http://www.oaklandsoftware.com";
        else
            urlStr = args[0];

        Callback cb = new TestCallback();

        HttpURLConnection.setDefaultCallback(cb);
        HttpURLConnection.setDefaultPipelining(true);

        URL url = new URL(urlStr);

        HttpURLConnection urlCon;

        int count = 10;

        for (int i = 0; i < count; i++)
        {
            urlCon = (HttpURLConnection)url.openConnection();
            urlCon.setRequestMethod("GET");
        }

        HttpURLConnection.executeAndBlock();
        
        HttpURLConnection.dumpAll();

    }
}
