
import java.applet.Applet;
import java.awt.Button;
import java.awt.Event;
import java.net.URL;

import com.oaklandsw.http.HttpURLConnection;
import com.oaklandsw.util.Log;
import com.oaklandsw.util.LogUtils;

public class AppletExample extends Applet
{
    private static final Log _log = LogUtils.makeLogger();

    private Button           clear_button;

    // Called to initialize the applet.
    public void init()
    {
        clear_button = new Button("Send message");
        this.add(clear_button);
    }

    // Called when the user clicks the button or chooses a color
    public boolean action(Event event, Object arg)
    {
        if (event.target == clear_button)
        {
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
            urlStr = "http://www.google.com";

            URL url = new URL(urlStr);

            HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
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
