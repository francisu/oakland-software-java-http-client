import java.net.URL;

import com.oaklandsw.http.HttpURLConnection;

/**
 * Simple example of using pipelining for HTTP GET requests
 */
public class HttpGetPipeline
{

    public HttpGetPipeline()
    {
    }

    public static final void main(String[] args) throws Exception
    {
        System.setProperty("java.protocol.handler.pkgs", "com.oaklandsw");

        String urlStr;
        if (args.length == 0)
        {
            System.out.println("usage: java HttpGetPipeline <url>");
            return;
        }

        urlStr = args[0];

        SamplePipelineCallback cb = new SamplePipelineCallback();
        cb._quiet = false;

        HttpURLConnection.setDefaultCallback(cb);
        HttpURLConnection.setDefaultPipelining(true);

        URL url = new URL(urlStr);

        int count = 10;

        for (int i = 0; i < count; i++)
        {
            // Creates and HttpURLConnection which is automatically
            // associated with the thread
            HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();

            // Begins the execution
            urlCon.pipelineExecute();
        }

        // Blocks for all connections to complete
        HttpURLConnection.pipelineBlock();
        
        // Just for diagnostic purposes
        HttpURLConnection.dumpAll();

    }
}
