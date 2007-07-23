import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

import com.oaklandsw.http.AutomaticHttpRetryException;
import com.oaklandsw.http.Callback;
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

    int                   _dumpAllInterval;

    boolean               _warmUp          = true;

    // Careful with these, the implementation in PerfComparison test
    // depends on the order of this array
    static final int      IMP_OAKLAND      = 0;
    static final int      IMP_SUN          = 1;
    static final int      IMP_JAKARTA      = 2;
    static final int      IMP_OAKLAND_PIPE = 3;

    int                   _implementation;

    static final String[] _impNames        = { "Oakland", "Sun", "Jakarta",
        "Oakland"                         };

    boolean               _doClose;
    boolean               _quiet;

    int                   _pipeDepth;
    // Use pipelining round-robin connection allocation
    boolean               _pipeRr          = true;
    int                   _maxConn;
    int                   _reqsPerConn;

    int                   _actualPipeMaxDepth;

    // Append these in order to each url
    int                   _suffixIndex;
    String                _urlString;

    boolean               _ipv4            = true;

    static final String[] _suffixes        = new String[] { "a", "b", "c", "d",
        "e", "f", "g", "h", "i"           };

    boolean               _doLog;

    boolean               _useSuffixes;

    long                  _totalTime;
    float                 _transTime;

    HttpConnectionManager _jakartaConnMgr;
    HttpClient            _jakartaClient;

    public class PipelineCallback implements Callback
    {
        public int _responses;

        public void writeRequest(com.oaklandsw.http.HttpURLConnection urlCon,
                                 OutputStream os)
        {
            // Used only for a post request, write the data here
        }

        public void readResponse(com.oaklandsw.http.HttpURLConnection urlCon,
                                 InputStream is)
        {
            try
            {
                processStream(urlCon.getInputStream());

                incrementActualtimes();
            }
            catch (AutomaticHttpRetryException arex)
            {
                System.out.println("Pipeline automatic retry: " + urlCon);
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
            try
            {
                System.err.println("pipeline error method called: "
                    + urlCon
                    + ": "
                    + urlCon.getResponseCode());
            }
            catch (IOException e)
            {
                System.err.println("Error getting response code: " + e);
            }

            ex.printStackTrace();
        }
    }

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
            else if (args[i].equalsIgnoreCase("-suff"))
            {
                _useSuffixes = true;
            }
            else if (args[i].equalsIgnoreCase("-dump"))
            {
                _dumpAllInterval = Integer.parseInt(args[++i]);
            }
            else if (args[i].equalsIgnoreCase("-sun"))
            {
                _implementation = IMP_SUN;
            }
            else if (args[i].equalsIgnoreCase("-jakarta"))
            {
                _implementation = IMP_JAKARTA;
            }
            else if (args[i].equalsIgnoreCase("-ip4"))
            {
                _ipv4 = true;
            }
            else if (args[i].equalsIgnoreCase("-ip6"))
            {
                _ipv4 = false;
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
            else if (args[i].equalsIgnoreCase("-pipenorr"))
            {
                _pipeRr = false;
            }
            else if (args[i].equalsIgnoreCase("-reqsperconn"))
            {
                _reqsPerConn = Integer.parseInt(args[++i]);
            }
            else if (args[i].equalsIgnoreCase("-log"))
            {
                _doLog = true;
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

        if (_ipv4)
            System.setProperty("java.net.preferIPv4Stack", "true");
        else
            System.setProperty("java.net.preferIPv6Stack", "true");

        if (_totalTimes != 0)
            _timesPerThread = _totalTimes / _threads;

        // LogUtils.logAll();

        com.oaklandsw.http.HttpURLConnection.resetStatistics();
        com.oaklandsw.http.HttpURLConnection.closeAllPooledConnections();

        if (_doLog)
            LogUtils.logAll();
        // LogUtils.logNone();

        // Implementation specific setup
        switch (_implementation)
        {
            case IMP_OAKLAND:
            case IMP_OAKLAND_PIPE:

                // Make sure we don't get stuck for a long time if a connection
                // hangs
                com.oaklandsw.http.HttpURLConnection
                        .setDefaultRequestTimeout(10000);
                com.oaklandsw.http.HttpURLConnection.setDefaultMaxTries(10);

                if (_maxConn > 0)
                {
                    com.oaklandsw.http.HttpURLConnection
                            .setMaxConnectionsPerHost(_maxConn);
                }
                else
                {
                    com.oaklandsw.http.HttpURLConnection
                            .setMaxConnectionsPerHost(com.oaklandsw.http.HttpURLConnection.DEFAULT_MAX_CONNECTIONS);

                }

                if (_reqsPerConn > 0)
                {
                    com.oaklandsw.http.HttpURLConnection
                            .setDefaultConnectionRequestLimit(_reqsPerConn);
                }
                else
                {
                    com.oaklandsw.http.HttpURLConnection
                            .setDefaultConnectionRequestLimit();

                }

                if (_implementation == IMP_OAKLAND_PIPE)
                {
                    if (_pipeDepth != 0)
                    {
                        com.oaklandsw.http.HttpURLConnection
                                .setDefaultPipeliningMaxDepth(_pipeDepth);
                    }
                    else
                    {
                        com.oaklandsw.http.HttpURLConnection
                                .setDefaultPipeliningMaxDepth(0);
                    }

                    int options = com.oaklandsw.http.HttpURLConnection.PIPE_STANDARD_OPTIONS;
                    if (!_pipeRr)
                        options &= ~com.oaklandsw.http.HttpURLConnection.PIPE_MAX_CONNECTIONS;

                    com.oaklandsw.http.HttpURLConnection
                            .setDefaultPipeliningOptions(options);
                }
                else
                {
                    com.oaklandsw.http.HttpURLConnection
                            .setDefaultCallback(null);
                    com.oaklandsw.http.HttpURLConnection
                            .setDefaultPipelining(false);

                }
                break;

            case IMP_SUN:
                // Note this is only respected on JRE 5 and 6
                if (_maxConn > 0)
                {
                    System.setProperty("http.maxConnections", Integer
                            .toString(_maxConn));
                }
                else
                {
                    System.getProperties().remove("http.maxConnections");
                }
                System.setProperty("http.keepAlive", "true");
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

        if (!_quiet)
        {
            System.out.println("Using: "
                + _impNames[_implementation]
                + " implementation");
            System.out.println("URL: " + _urlString);
            System.out.println("Times/thread: " + _timesPerThread);
            System.out.println("Threads: " + _threads);
            if (_maxConn > 0)
                System.out.println("Using max: " + _maxConn + " connections.");
            if (_reqsPerConn > 0)
            {
                System.out.println("Using max: "
                    + _reqsPerConn
                    + " requests/connection.");
            }
            // System.out.println("http.maxConnections:
            // " +
            // System.getProperty("http.maxConnections"));
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

        _actualPipeMaxDepth = com.oaklandsw.http.HttpURLConnection
                .getConnectionManager()
                .getCount(com.oaklandsw.http.HttpConnectionManager.COUNT_PIPELINE_DEPTH_HIGH);

        if (!_quiet)
        {
            if (_implementation == IMP_OAKLAND
                || _implementation == IMP_OAKLAND_PIPE)
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
        System.out
                .println("  -pipenorr - don't use standard round-robin pipline connection alloc (oakland only)");
        System.out.println("  -pipedepth <depth> - set max pipeline depth");
        System.out
                .println("  -times <num> - total number of times (all threads) (def 1)");
        System.out
                .println("  -timesThread <num> - number of times per thread (def 1)");
        System.out.println("  -threads <num> - number of threads (def 1)");
        System.out.println("  -nowarm - include the initialization in timing");
        System.out
                .println("  -suff - append a suffix to each URL in a round robin fashion");
        System.out.println("  -ip4 - prefer the IPv4 stack (default)");
        System.out.println("  -ip6 - prefer the IPv6 stack");
        System.out.println("  -close - explicitly close all connections");
        System.out.println("  -quiet - don't print anything");
        System.out
                .println("  -dump <num> - dump the statistics at this interval");
    }

    void runOne(PipelineCallback cb) throws Exception
    {
        HttpURLConnection urlCon;

        String urlToUse = _urlString;

        if (_useSuffixes)
        {
            synchronized (this)
            {
                urlToUse += _suffixes[_suffixIndex++];
                if (_suffixIndex >= _suffixes.length)
                    _suffixIndex = 0;
            }
        }

        URL url = new URL(urlToUse);

        int responseCode = 0;
        switch (_implementation)
        {
            case IMP_OAKLAND:
                urlCon = com.oaklandsw.http.HttpURLConnection
                        .openConnection(url);
                responseCode = urlCon.getResponseCode();
                processStream(urlCon.getInputStream());
                incrementActualtimes();
                break;

            case IMP_OAKLAND_PIPE:
                urlCon = com.oaklandsw.http.HttpURLConnection
                        .openConnection(url);
                ((com.oaklandsw.http.HttpURLConnection)urlCon).setCallback(cb);
                ((com.oaklandsw.http.HttpURLConnection)urlCon)
                        .pipelineExecute();
                // This is executed later
                break;

            case IMP_SUN:
                urlCon = (HttpURLConnection)url.openConnection();
                responseCode = urlCon.getResponseCode();
                processStream(urlCon.getInputStream());
                incrementActualtimes();
                break;

            case IMP_JAKARTA:
                GetMethod method = new GetMethod(_urlString);
                _jakartaClient.executeMethod(method);
                responseCode = method.getStatusCode();

                // Read the stream
                InputStream is = method.getResponseBodyAsStream();
                processStream(is);
                method.releaseConnection();
                incrementActualtimes();
                break;

            default:
                Util.impossible("Invalid implementation: " + _implementation);
                // throws
        }

        if (_implementation != IMP_OAKLAND_PIPE && responseCode != 200)
        {
            throw new RuntimeException("Bad response code: " + responseCode);
        }
    }

    // Runs a thread for the test
    void runOneThread(int times)
    {
        try
        {
            PipelineCallback cb = null;
            if (_implementation == IMP_OAKLAND_PIPE)
            {
                cb = new PipelineCallback();
            }

            for (int i = 0; i < times; i++)
                runOne(cb);

            if (_implementation == IMP_OAKLAND_PIPE)
            {
                com.oaklandsw.http.HttpURLConnection.pipelineBlock();
            }
        }
        catch (Exception ex)
        {
            System.out.println("Thread exception: " + ex);
            ex.printStackTrace();
            _failed = 1;
        }
    }

    void incrementActualtimes()
    {
        synchronized (this)
        {
            _actualTimes++;
            if (_dumpAllInterval > 0 && (_actualTimes % _dumpAllInterval == 0))
            {
                System.out.println(_actualTimes
                    + " ---------------------------------------");
                com.oaklandsw.http.HttpURLConnection.dumpAll();
            }
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

    public static void processStream(InputStream inputStream)
        throws IOException
    {
        byte[] buffer = new byte[16384];
        int nb = 0;
        while (true)
        {
            nb = inputStream.read(buffer);
            if (nb == -1)
                break;
        }
        inputStream.close();
    }

    public static void main(String args[]) throws Exception
    {
        TestPerf tp = new TestPerf();
        tp.run(args);
    }

}
