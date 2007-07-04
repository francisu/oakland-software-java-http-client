import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.logging.Log;

import com.oaklandsw.util.LogUtils;
import com.oaklandsw.util.Util;

/**
 * HTTP client performance test example
 */
public class TestPerf
{
    static final Log      _log             = LogUtils.makeLogger();

    int                   _timesPerThread;
    int                   _totalTimes;
    int                   _threads         = 1;

    int                   _actualTimes;

    int                   _failed;

    boolean               _warmUp          = true;

    // Careful with these, the implementation in PerfComparison test
    // depends on the order of this array
    static final int      IMP_OAKLAND      = 0;
    static final int      IMP_SUN          = 1;
    static final int      IMP_JAKARTA      = 2;
    static final int      IMP_OAKLAND_PIPE = 3;

    int                   _implementation;

    static final String[] _impNames        = { "Oakland", "Sun", "Jakarta",
        "Oakland Pipeline"                };

    boolean               _doClose;
    boolean               _quiet;

    int                   _pipeDepth;
    int                   _maxConn;

    String                _urlString;
    URL                   _url;

    long                  _totalTime;
    float                 _transTime;

    HttpConnectionManager _jakartaConnMgr;
    HttpClient            _jakartaClient;

    public TestPerf()
    {
    }

    public void run(String args[]) throws Exception
    {
        for (int i = 0; i < args.length; i++)
        {
            if (args[i].equalsIgnoreCase("-timesThread"))
            {
                _timesPerThread = Integer.parseInt(args[++i]);
            }
            else if (args[i].equalsIgnoreCase("-times"))
            {
                _totalTimes = Integer.parseInt(args[++i]);
            }
            else if (args[i].equalsIgnoreCase("-threads"))
            {
                _threads = Integer.parseInt(args[++i]);
            }
            else if (args[i].equalsIgnoreCase("-sun"))
            {
                _implementation = IMP_SUN;
            }
            else if (args[i].equalsIgnoreCase("-jakarta"))
            {
                _implementation = IMP_JAKARTA;
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
                _implementation = IMP_OAKLAND_PIPE;
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
        // For profiling
        // Thread.sleep(1000000);
    }

    public void run() throws Exception
    {
        if (_urlString == null)
        {
            System.out.println("Please specify a URL using -url");
            return;
        }

        if (_timesPerThread != 0 && _totalTimes != 0)
        {
            System.out
                    .println("Specify either -times or -timesThread, but not both");
            return;
        }

        if (_totalTimes != 0)
            _timesPerThread = _totalTimes / _threads;

        _url = new URL(_urlString);

        // LogUtils.logAll();

        com.oaklandsw.http.HttpURLConnection.resetStatistics();
        com.oaklandsw.http.HttpURLConnection.closeAllPooledConnections();

        // Make sure we don't get stuck for a long time if a connection hangs
        com.oaklandsw.http.HttpURLConnection.setDefaultRequestTimeout(10000);

        // In case these things were previously set
        com.oaklandsw.http.HttpURLConnection
                .setMaxConnectionsPerHost(com.oaklandsw.http.HttpURLConnection.DEFAULT_MAX_CONNECTIONS);
        com.oaklandsw.http.HttpURLConnection.setDefaultCallback(null);
        com.oaklandsw.http.HttpURLConnection.setDefaultPipelining(false);

        // LogUtils.logNone();

        if (!_quiet)
        {
            System.out.println("Using: "
                + _impNames[_implementation]
                + " implementation");
            System.out.println("URL: " + _urlString);
            System.out.println("Times/thread: " + _timesPerThread);
            System.out.println("Threads: " + _threads);
        }

        if (_maxConn > 0)
        {
            com.oaklandsw.http.HttpURLConnection
                    .setMaxConnectionsPerHost(_maxConn);
            if (!_quiet)
                System.out.println("Using max: " + _maxConn + " connections.");
        }

        // Implementation specific setup
        switch (_implementation)
        {
            case IMP_OAKLAND_PIPE:
                if (_pipeDepth != 0)
                {
                    com.oaklandsw.http.HttpURLConnection
                            .setDefaultPipeliningMaxDepth(_pipeDepth);
                }

                com.oaklandsw.http.HttpURLConnection.setDefaultPipelining(true);
                com.oaklandsw.http.HttpURLConnection.setDefaultMaxTries(10);
                break;
            case IMP_JAKARTA:
                HttpConnectionManagerParams params = new HttpConnectionManagerParams();
                if (_maxConn > 0)
                    params.setDefaultMaxConnectionsPerHost(_maxConn);
                params.setStaleCheckingEnabled(false);
                if (_threads == 1)
                    _jakartaConnMgr = new SimpleHttpConnectionManager();
                else
                    _jakartaConnMgr = new MultiThreadedHttpConnectionManager();
                _jakartaConnMgr.setParams(params);

                _jakartaClient = new HttpClient(_jakartaConnMgr);
                break;
        }

        // Do one to initialize everything but don't count that
        // in the timing
        if (_warmUp)
        {
            runOneThread(1);
        }

        _actualTimes = 0;

        long startTime = System.currentTimeMillis();

        runAllThreads();

        long endTime = System.currentTimeMillis();
        _totalTime = endTime - startTime;

        if (_actualTimes != _timesPerThread * _threads)
        {
            System.out.println("!!!Expected "
                + (_timesPerThread * _threads)
                + " got: "
                + _actualTimes);
            // Make it a high number since the test results are invalid
            _transTime = 999999;
        }
        else
        {
            _transTime = (float)_totalTime / (float)_actualTimes;
        }

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
        System.out.println("  -url <url> - url to send to");
        System.out
                .println("  -sun - use JRE implementation (oakland is default)");
        System.out
                .println("  -jakarta - use Jakarta implementation (oakland is default)");
        System.out.println("  -pipe - test with pipelining (oakland only)");
        System.out.println("  -pipedepth <depth> - set max pipeline depth");
        System.out
                .println("  -times <num> - number of times (per thread) (def 1000)");
        System.out.println("  -threads <num> - number of threads (def 1)");
        System.out.println("  -nowarm - include the initialization in timing");
        System.out.println("  -close - explicitly close all connections");
        System.out.println("  -quiet - don't print anything");
    }

    void runOne(SamplePipelineCallback cb) throws Exception
    {
        HttpURLConnection urlCon;

        switch (_implementation)
        {
            case IMP_OAKLAND:
                urlCon = com.oaklandsw.http.HttpURLConnection
                        .openConnection(_url);
                if (urlCon.getResponseCode() != 200)
                {
                    throw new RuntimeException("Bad response code: "
                        + urlCon.getResponseCode());
                }
                SamplePipelineCallback.processStream(urlCon);
                _actualTimes++;
                break;

            case IMP_OAKLAND_PIPE:
                urlCon = com.oaklandsw.http.HttpURLConnection
                        .openConnection(_url);
                ((com.oaklandsw.http.HttpURLConnection)urlCon).setCallback(cb);
                ((com.oaklandsw.http.HttpURLConnection)urlCon)
                        .pipelineExecute();
                // This is executed later
                break;

            case IMP_SUN:
                urlCon = (HttpURLConnection)_url.openConnection();
                if (urlCon.getResponseCode() != 200)
                {
                    throw new RuntimeException("Bad response code: "
                        + urlCon.getResponseCode());
                }
                SamplePipelineCallback.processStream(urlCon);
                _actualTimes++;
                break;

            case IMP_JAKARTA:
                GetMethod method = new GetMethod(_urlString);
                _jakartaClient.executeMethod(method);
                if (method.getStatusCode() != 200)
                {
                    throw new RuntimeException("Bad response code: "
                        + method.getStatusCode());
                }

                // Read the stream
                InputStream is = method.getResponseBodyAsStream();
                Util.getStringFromInputStream(is);
                method.releaseConnection();
                _actualTimes++;
                break;

            default:
                Util.impossible("Invalid implementation: " + _implementation);
                // throws
        }
    }

    // Runs a thread for the test
    void runOneThread(int times)
    {
        try
        {
            SamplePipelineCallback cb = null;
            if (_implementation == IMP_OAKLAND_PIPE)
            {
                cb = new SamplePipelineCallback();
                cb._quiet = true;
            }

            for (int i = 0; i < times; i++)
                runOne(cb);

            if (_implementation == IMP_OAKLAND_PIPE)
            {
                com.oaklandsw.http.HttpURLConnection.pipelineBlock();
                synchronized (this)
                {
                    _actualTimes += cb._responses;
                }
            }
        }
        catch (Exception ex)
        {
            System.out.println("Thread exception: " + ex);
            ex.printStackTrace();
            _failed = 1;
        }
    }

    void runAllThreads() throws Exception
    {
        Thread t;

        _failed = 0;

        List threads = new ArrayList();
        for (int i = 0; i < _threads; i++)
        {
            final int threadNum = i;
            t = new Thread()
            {
                public void run()
                {
                    Thread.currentThread().setName("TestMultiThread"
                        + threadNum);
                    runOneThread(_timesPerThread);
                }
            };

            t.start();
            threads.add(t);
        }

        for (int i = 0; i < threads.size(); i++)
        {
            ((Thread)threads.get(i)).join(100000);
        }

        if (_failed > 0)
            System.out.println("One or more threads failed");
    }

    public static void main(String args[]) throws Exception
    {
        TestPerf tp = new TestPerf();
        tp.run(args);
    }

}
