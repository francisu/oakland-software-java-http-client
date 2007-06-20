import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.logging.Log;

import com.oaklandsw.util.LogUtils;

/**
 * HTTP client performance test example
 */
public class TestPerf
{
    static final Log _log    = LogUtils.makeLogger();

    static final int TIMES   = 1000;
    static int       _times;

    static boolean   _warmUp = true;
    static boolean   _doPipelining;
    static boolean   _useSun;
    static boolean   _doClose;

    static int       _pipeDepth;
    static int       _maxConn;

    static String    _urlString;
    static URL       _url;

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
            else if (args[i].equalsIgnoreCase("-nowarmup"))
            {
                _warmUp = false;
            }
            else if (args[i].equalsIgnoreCase("-close"))
            {
                _doClose = true;
            }
            else if (args[i].equalsIgnoreCase("-help"))
            {
                usage();
                return;
            }
            else
            {
                System.out.println("Arg: " + args[i] + " ignored");
            }
        }

        if (_urlString == null)
        {
            System.out.println("Please specify a URL using -url");
            return;
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
            com.oaklandsw.http.HttpURLConnection.dumpAll();
        }

        System.out.println("Time: " + totalTime + " MS/trans: " + transTime);
    }

    static void usage()
    {
        System.out.println("java TestPerf [options]");
        System.out.println("options: ");
        System.out
                .println("  -sun - use JRE implementation (oakland is default)");
        System.out.println("  -url <url> - url to send to");
        System.out.println("  -pipe - test with pipelining (oakland only)");
        System.out.println("  -pipedepth <depth> - set max pipeline depth");
        System.out.println("  -times <num> - number of times (def 1000)");
        System.out.println("  -nowarm - include the initialization in timing");
        System.out.println("  -close - explicitly close all connections");
        // System.out.println(" -dnsjava - use dnsjava name resolver");
    }

    static HttpURLConnection setupUrlCon() throws Exception
    {
        HttpURLConnection urlCon = (HttpURLConnection)_url.openConnection();
        return urlCon;
    }

    static void doConnect() throws Exception
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

        SamplePipelineCallback cb = new SamplePipelineCallback();
        cb._quiet = true;
        com.oaklandsw.http.HttpURLConnection.setDefaultCallback(cb);

        for (int i = 0; i < _times; i++)
        {
            com.oaklandsw.http.HttpURLConnection urlCon = (com.oaklandsw.http.HttpURLConnection)setupUrlCon();
            if (_pipeDepth != 0)
                urlCon.setPipeliningMaxDepth(_pipeDepth);
        }

        com.oaklandsw.http.HttpURLConnection.executeAndBlock();

        if (cb._responses != _times)
        {
            System.out.println("!!! response mismatch: " + cb._responses);
        }

    }
}
