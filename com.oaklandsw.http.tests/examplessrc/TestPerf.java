import java.io.IOException;
import java.io.InputStream;
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

    int              _times;

    boolean          _warmUp = true;
    boolean          _doPipelining;
    boolean          _useSun;
    boolean          _doClose;
    boolean          _quiet;

    int              _pipeDepth;
    int              _maxConn;

    String           _urlString;
    URL              _url;

    long             _totalTime;
    float            _transTime;

    public TestPerf()
    {
    }

    // Read through the input stream
    public static void processStream(HttpURLConnection urlCon)
        throws IOException
    {
        InputStream inputStream = urlCon.getInputStream();
        byte[] buffer = new byte[10000];
        int nb = 0;
        while (true)
        {
            nb = inputStream.read(buffer);
            if (nb == -1)
                break;
        }
        inputStream.close();
    }

    public void run(String args[]) throws Exception
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
            else if (args[i].equalsIgnoreCase("-quiet"))
            {
                _quiet = true;
            }
            else if (args[i].equalsIgnoreCase("-help"))
            {
                usage();
                return;
            }
            else if (args[i].equalsIgnoreCase(""))
            {
                // Silently ignore empty args
            }
            else
            {
                System.out.println("Arg: " + args[i] + " ignored");
            }
        }
        run();
    }

    public void run() throws Exception
    {
        if (_urlString == null)
        {
            System.out.println("Please specify a URL using -url");
            return;
        }

        if (!_quiet)
        {
            System.out.println("Using: "
                + (_useSun ? "Sun" : "Oakland")
                + " implementation");
        }

        _url = new URL(_urlString);

        // LogUtils.logAll();

        com.oaklandsw.http.HttpURLConnection.resetStatistics();
        com.oaklandsw.http.HttpURLConnection.closeAllPooledConnections();

        // Make sure we don't get stuck for a long time if a connection hangs
        com.oaklandsw.http.HttpURLConnection.setDefaultRequestTimeout(10000);

        // Do one to initialize everything but don't count that
        // in the timing
        if (_warmUp)
            doConnect();

        // LogUtils.logNone();

        long startTime = System.currentTimeMillis();

        if (!_quiet)
            System.out.println("URL: " + _urlString + " times: " + _times);

        if (_maxConn > 0)
        {
            com.oaklandsw.http.HttpURLConnection
                    .setMaxConnectionsPerHost(_maxConn);
            if (!_quiet)
                System.out.println("Using max: " + _maxConn + " connections.");
        }

        testGetMethod();

        long endTime = System.currentTimeMillis();
        _totalTime = endTime - startTime;
        _transTime = (float)_totalTime / (float)_times;

        if (!_quiet)
        {
            System.out.println("connection pool before close");
            com.oaklandsw.http.HttpURLConnection.dumpAll();
            if (_doClose)
            {
                com.oaklandsw.http.HttpURLConnection
                        .closeAllPooledConnections();
                System.out.println("connection pool after close");
                com.oaklandsw.http.HttpURLConnection.dumpAll();
            }

            System.out.println("Total Time (ms): "
                + _totalTime
                + " Time/trans (ms): "
                + _transTime);
        }
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
        System.out.println("  -quiet - don't print anything");
    }

    public HttpURLConnection setupUrlCon() throws Exception
    {
        HttpURLConnection urlCon;
        if (_useSun)
            urlCon = (HttpURLConnection)_url.openConnection();
        else
            urlCon = com.oaklandsw.http.HttpURLConnection.openConnection(_url);
        return urlCon;
    }

    public void doConnect() throws Exception
    {
        // LogUtils.logAll();
        HttpURLConnection urlCon = setupUrlCon();
        if (urlCon.getResponseCode() != 200)
        {
            throw new RuntimeException("Bad response code: "
                + urlCon.getResponseCode());
        }

        processStream(urlCon);

    }

    public void testGetMethod() throws Exception
    {
        SamplePipelineCallback cb = new SamplePipelineCallback();
        cb._quiet = true;

        com.oaklandsw.http.HttpURLConnection
                .setDefaultPipelining(_doPipelining);
        if (_doPipelining)
        {
            com.oaklandsw.http.HttpURLConnection.setDefaultMaxTries(10);
            com.oaklandsw.http.HttpURLConnection.setDefaultCallback(cb);
        }
        else
        {
            com.oaklandsw.http.HttpURLConnection.setDefaultMaxTries(3);
            com.oaklandsw.http.HttpURLConnection.setDefaultCallback(null);
        }

        if (!_doPipelining)
        {
            for (int i = 0; i < _times; i++)
                doConnect();
            return;
        }

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

    public static void main(String args[]) throws Exception
    {
        TestPerf tp = new TestPerf();
        tp.run(args);
    }

}
