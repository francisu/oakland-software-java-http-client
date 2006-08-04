package com.oaklandsw.http.sa;

import java.applet.Applet;
import java.awt.Button;
import java.awt.Event;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

//import java.net.HttpURLConnection;
import com.oaklandsw.http.HttpURLConnection;

public class AppletTest extends Applet
{
    private static final Log          _log                     = LogFactory
    .getLog(AppletTest.class);

    private Button clear_button;
    
    // Called to initialize the applet.
    public void init() {

        clear_button = new Button("Send message");
        this.add(clear_button);
        Logger.getAnonymousLogger().setLevel(Level.ALL);
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
