import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.logging.Log;

import com.oaklandsw.http.AutomaticHttpRetryException;
import com.oaklandsw.http.Callback;
import com.oaklandsw.http.HttpTestEnv;
import com.oaklandsw.http.servlet.HeaderServlet;
import com.oaklandsw.util.LogUtils;

public class TestPerf
{

    private static final Log _log       = LogUtils.makeLogger();

    private static final int TEXT_TIMES = 1000;
    private static int       _textTimes = TEXT_TIMES;

    private static final int TIMES      = 1000;
    private static int       _times;

    private static boolean   _readText;
    private static boolean   _writeText;

    private static boolean   _warmUp    = true;

    private static boolean   _doPipelining;

    private static boolean   _useSun;

    private static boolean   _doClose;

    private static int       _pipeDepth;

    private static int       _maxConn;

    private static int       _pipeResponses;

    private static String    _urlString = HttpTestEnv.TEST_URL_WEBAPP
                                            + HeaderServlet.NAME;

    private static URL       _url;

    public static class TestPipelineCallback implements Callback
    {

        public void writeRequest(com.oaklandsw.http.HttpURLConnection urlCon,
                                 OutputStream os)
        {
            System.out.println("!!! should not be called");
        }

        public void readResponse(com.oaklandsw.http.HttpURLConnection urlCon,
                                 InputStream is)
        {
            try
            {
                // System.out.println("Response: " + urlCon.getResponseCode());
                _pipeResponses++;

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

        public void error(com.oaklandsw.http.HttpURLConnection urlCon,
                          InputStream is,
                          Exception ex)
        {
            System.out.println("ERROR: " + urlCon);
            ex.printStackTrace();
        }

        public String toString()
        {
            return "Callback" + Thread.currentThread().getName();
        }
    }

    public TestPerf()
    {
    }

    public static void main(String args[]) throws Exception
    {
        _times = TIMES;

        for (int i = 0; i < args.length; i++)
        {
            if (args[i].equalsIgnoreCase("-times"))
            {
                try
                {
                    _times = Integer.parseInt(args[++i]);
                }
                catch (Exception e)
                {
                    _times = TIMES;
                }
            }
            else if (args[i].equalsIgnoreCase("-sun"))
            {
                _useSun = true;
            }
            else if (args[i].equalsIgnoreCase("-url"))
            {
                _urlString = args[++i];
            }
            else if (args[i].equalsIgnoreCase("-maxconn"))
            {
                _maxConn = Integer.parseInt(args[++i]);
            }
            else if (args[i].equalsIgnoreCase("-pipe"))
            {
                _doPipelining = true;
            }
            else if (args[i].equalsIgnoreCase("-pipedepth"))
            {
                _pipeDepth = Integer.parseInt(args[++i]);
            }
            else if (args[i].equalsIgnoreCase("-log"))
            {
                LogUtils.logAll();
            }
            else if (args[i].equalsIgnoreCase("-readtext"))
            {
                _readText = true;
            }
            else if (args[i].equalsIgnoreCase("-writetext"))
            {
                _writeText = true;
            }
            else if (args[i].equalsIgnoreCase("-texttimes"))
            {
                try
                {
                    _textTimes = Integer.parseInt(args[++i]);
                }
                catch (Exception e)
                {
                    _textTimes = TIMES;
                }
            }
            else if (args[i].equalsIgnoreCase("-webserver"))
            {
                _urlString = HttpTestEnv.TEST_URL_WEBSERVER + "/";
            }
            else if (args[i].equalsIgnoreCase("-nowarmup"))
            {
                _warmUp = false;
            }
            else if (args[i].equalsIgnoreCase("-close"))
            {
                _doClose = true;
            }
            // else if (args[i].equalsIgnoreCase("-dnsjava"))
            // {
            // com.oaklandsw.http.HttpURLConnection.setUseDnsJava(true);
            // }
            else if (args[i].equalsIgnoreCase("-help"))
            {
                usage();
                return;
            }
            else
            {
                usage();
                return;
            }
        }

        if (!_useSun)
        {
            System.setProperty("java.protocol.handler.pkgs", "com.oaklandsw");
        }

        System.out.println("Using: "
            + (_useSun ? "Sun" : "Oakland")
            + " implementation");

        _url = new URL(_urlString);

        // Do one to initialize everything but don't count that
        // in the timing
        if (_warmUp)
            doConnect();

        long startTime = System.currentTimeMillis();

        System.out.println("Times: " + _times);
        System.out.println("Text size: "
            + (_textTimes * HeaderServlet.TEXT_CONST.length()));

        if (_writeText)
            System.out.println("Writetext: Server sending text");

        if (_maxConn > 0)
        {
            com.oaklandsw.http.HttpURLConnection
                    .setMaxConnectionsPerHost(_maxConn);
            System.out.println("Using max: " + _maxConn + " connections.");
        }

        testGetMethod();

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        float transTime = (float)totalTime / (float)_times;

        System.out.println("connection pool before close");
        com.oaklandsw.http.HttpURLConnection.dumpAll();
        if (_doClose)
        {
            com.oaklandsw.http.HttpURLConnection.closeAllPooledConnections();
            System.out.println("connection pool after close");
            com.oaklandsw.http.HttpURLConnection.dumpConnectionPool();
        }

        System.out.println("Time: " + totalTime + " MS/trans: " + transTime);
    }

    private static void usage()
    {
        System.out.println("java TestPerf [options]");
        System.out.println("options: ");
        System.out
                .println("  -sun - use JRE implementation (oakland is default)");
        System.out.println("  -url - url to send to");
        System.out.println("  -pipe - test with pipelining (oakland only)");
        System.out.println("  -pipedepth - set max pipeline depth");
        System.out
                .println("  -writetext - server writes text (oaklandsw internal use only)");
        // System.out.println(" -readtext - does getInputStream() to read");
        System.out.println("  -times num - number of times (def 1000)");
        System.out
                .println("  -texttimes num - number of 36 byte text blocks (oaklandsw internal use only)");
        System.out.println("  -nowarm - include the initialization in timing");
        System.out
                .println("  -webserver - use localhost:80 instead of cat (oaklandsw internal use only)");
        System.out.println("  -close - explicitly close all connections");
        // System.out.println(" -dnsjava - use dnsjava name resolver");
    }

    private static HttpURLConnection setupUrlCon() throws Exception
    {
        HttpURLConnection urlCon = (HttpURLConnection)_url.openConnection();
        urlCon.setRequestMethod("GET");
        if (_writeText)
        {
            urlCon.setRequestProperty("emit-text", String.valueOf(_textTimes));
        }
        return urlCon;
    }

    private static void doConnect() throws Exception
    {
        HttpURLConnection urlCon = setupUrlCon();
        if (urlCon.getResponseCode() != 200)
            throw new RuntimeException("Bad response code");

        urlCon.getInputStream().close();
    }

    public static void testGetMethod() throws Exception
    {

        if (!_doPipelining)
        {
            for (int i = 0; i < _times; i++)
                doConnect();
            return;
        }

        com.oaklandsw.http.HttpURLConnection.setDefaultMaxTries(10);
        com.oaklandsw.http.HttpURLConnection.setDefaultPipelining(true);
        com.oaklandsw.http.HttpURLConnection
                .setDefaultCallback(new TestPipelineCallback());

        for (int i = 0; i < _times; i++)
        {
            com.oaklandsw.http.HttpURLConnection urlCon = (com.oaklandsw.http.HttpURLConnection)setupUrlCon();
            if (_pipeDepth != 0)
                urlCon.setPipeliningMaxDepth(_pipeDepth);
        }

        com.oaklandsw.http.HttpURLConnection.executeAndBlock();

        if (_pipeResponses != _times)
        {
            System.out.println("!!! response mismatch: " + _pipeResponses);
        }

    }
}
