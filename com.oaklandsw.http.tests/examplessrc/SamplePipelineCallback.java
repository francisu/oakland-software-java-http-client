import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.oaklandsw.http.AutomaticHttpRetryException;
import com.oaklandsw.http.Callback;
import com.oaklandsw.http.HttpURLConnection;

/*
 * Example callback for pipeline processing
 */

public class SamplePipelineCallback implements Callback
{

    public boolean _quiet = true;

    public int     _responses;

    public void writeRequest(HttpURLConnection urlCon, OutputStream os)
    {
        System.out.println("!!! should not be called");
    }

    public void readResponse(HttpURLConnection urlCon, InputStream is)
    {
        try
        {
            if (!_quiet)
                System.out.println("Response: " + urlCon.getResponseCode());

            // In case we are multi-threaded
            synchronized (this)
            {
                _responses++;
            }
            
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
