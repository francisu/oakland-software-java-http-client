

import java.applet.Applet;
import java.awt.Button;
import java.awt.Event;
import java.net.URL;

import org.apache.commons.logging.Log;

//import java.net.HttpURLConnection;
import com.oaklandsw.http.HttpURLConnection;
import com.oaklandsw.util.LogUtils;

public class AppletTest extends Applet
{
    private static final Log   _log         = LogUtils.makeLogger();

    private Button clear_button;
    
    // Called to initialize the applet.
    public void init() {

        clear_button = new Button("Send message");
        this.add(clear_button);
        // Does not compile with 1.2
        //Logger.getAnonymousLogger().setLevel(Level.ALL);
    }
    

    // Called when the user clicks the button or chooses a color
    public boolean action(Event event, Object arg) {

        if (event.target == clear_button) {
            sendMessage();
            return true;
        }
        return super.action(event, arg);
    }


    public void sendMessage()
    {
        try
        {
            showStatus("HTTP applet test started");
            String urlStr;
            urlStr = "http://berlioz";

            URL url = new URL(urlStr);

            HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
            //HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
            urlCon.setRequestMethod("GET");
            urlCon.getResponseCode();
            showStatus("HTTP applet connected OK");
            _log.info("connected OK");
        }
        catch (Exception ex)
        {
            showStatus("got exception: " + ex);
            throw new RuntimeException("from applet (frank)" + ex);
        }

    }

}
