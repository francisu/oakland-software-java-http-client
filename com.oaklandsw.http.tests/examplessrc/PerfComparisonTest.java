import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.oaklandsw.util.LogUtils;

/**
 * Performance comparison test script.
 */
public class PerfComparisonTest
{

    public static final int         REPEAT_TIMES           = 1;

    public static final String      INTRANET_3K            = "http://repoman:8081/3k";

    // Network scenarios
    public static final int         SC_LOCAL               = 0;
    public static final int         SC_INTRANET_APACHE     = 1;
    public static final int         SC_INTRANET_IIS        = 2;
    public static final int         SC_INTERNET            = 3;
    public static final int         SC_LAST                = SC_INTERNET;

    public static final String[]    _locationNames         = new String[] {
        "Local", "Intranet/Apache", "Intranet/IIS", "Internet" };

    // Each array element corresponds to one of the scenarios
    public static final String[]    _sizeNames             = new String[] {
        "0K", "3K", "20K", "100K", /* "240K" */           };

    // IIS Location: "http://repoman/noauth/wwwroot/latency.txt", //

    // Size X location
    public static final String[][]  _sizeUrls              = new String[][] {
                                                           // 0K (really 10
        // bytes)
        { "http://berlioz/oaklandsw-http/latency.txt", //
        "http://repoman:8081/latency.txt", //
        "http://repoman/noauth/wwwroot/latency.txt", //
        "http://sfo.speakeasy.net/speedtest/latency.txt" },

        // 3K
        { "http://berlioz/oaklandsw-http/3k", //
        INTRANET_3K, //
        "http://repoman/noauth/wwwroot/3k", //
        "http://host72.hrwebservices.net/~bigtivo/perftest/gfx/tnet_wht.jpg" },

        // 20K
        { "http://berlioz/oaklandsw-http/20k", //
        "http://repoman:8081/20k", //
        "http://repoman/noauth/wwwroot/20k", //
        "http://i.dslr.net/speedtest/v2/bottom.jpg" },

        // 100K
        { "http://berlioz/oaklandsw-http/100k", //
        "http://repoman:8081/100k", //
        "http://repoman/noauth/wwwroot/100k", //
        "http://i.dslr.net/SpeedTests/185/v2/100k" },//  

                                                           // 240K
                                                           // {
                                                           // "http://berlioz/oaklandsw-http/random350x350.jpg",
                                                           // //
                                                           // "http://repoman/noauth/wwwroot/random350x350.jpg",
                                                           // //
                                                           // "http://sfo.speakeasy.net/speedtest/random350x350.jpg"
                                                           // },//

                                                           };

    // Size x location
    public static final boolean[][] _useSuffixes           = new boolean[][] {
        { false, false, false, false },//
        { false, true, true, false },//
        { false, false, false, false },//
        { false, false, false, false },//
                                                           };

    // Size x location
    // Don't use these limits for now
    public static final int[][]     _sizeConnLimits        = new int[][] {
        { 0, 0, 0, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 } };

    // Size x location
    public static final int[][]     _sizeCounts            = new int[][] {
        { 1000, 1000, 1000, 200 }, { 1000, 1000, 1000, 200 },
        { 1000, 1000, 1000, 200 }, { 1000, 1000, 1000, 200 } };

    // Product Pipe depths
    // Product x pipeline depth
    // WARNING - the order of these is defined by the values in TestPerf
    public static final int[][]     _pipeDepths            = new int[][] {
        { 0 }, // Oakland
        { 0 }, // Sun
        { 0 }, // Apache
        { 1, 2, 50, 100, 200, 0 }, // Oakland Pipe
                                                           };
    // Product Connections Limits
    // Product x connection limit
    // WARNING - the order of these is defined by the values in TestPerf
    public static final int[][]     _productConnections    = new int[][] {
        { 1, 2, 5, 10 }, // Oakland
        { 1, 2, 5, 10 }, // Sun
        { 1, 2, 5, 10 }, // Apache
        { 2, 5, 10 }, // Oakland Pipe
                                                           };
    // Counts of the number of threads to use
    public static final int[]       _threadCounts          = new int[] { 1, 5,
        10                                                };

    public PrintWriter              _pw;

    public boolean                  _noPrint;

    public int                      _currentMaxConnections;
    public int                      _currentPipeDepth;
    public int                      _currentProduct;
    public int                      _currentSizeIndex;
    public int                      _currentLocation;
    public int                      _currentCount;
    public int                      _currentCountPerThread;
    public int                      _currentThreads;

    public boolean                 _currentUseSuffixes;

    public int                      _singleSize            = -1;
    public int                      _singleLocation        = -1;
    public int                      _singleConnectionLimit = -1;
    public int                      _singleProduct         = -1;

    // Runs a set of tests
    public void runSet() throws Exception
    {
        String url;

        url = _sizeUrls[_currentSizeIndex][_currentLocation];
        if (url == null)
            return;

        float minPerTrans = 0;
        float maxPerTrans = 0;
        float times[] = new float[REPEAT_TIMES];
        float totalPerTrans = 0;
        long totalTime = 0;

        for (int i = 0; i < REPEAT_TIMES; i++)
        {
            TestPerf tp = new TestPerf();

            tp._warmUp = false;
            tp._urlString = url;
            tp._pipeDepth = _currentPipeDepth;
            tp._implementation = _currentProduct;
            tp._totalTimes = _currentCount;
            tp._threads = _currentThreads;
            tp._useSuffixes = _currentUseSuffixes;
            tp._maxConn = _currentMaxConnections;
            tp._quiet = false;

            com.oaklandsw.http.HttpURLConnection
                    .setDefaultConnectionRequestLimit(_sizeConnLimits[_currentSizeIndex][_currentLocation]);

            tp.run();

            if (tp._transTime > maxPerTrans)
                maxPerTrans = tp._transTime;
            if (minPerTrans == 0 || tp._transTime < minPerTrans)
                minPerTrans = tp._transTime;
            times[i] = tp._transTime;
            totalPerTrans += tp._transTime;
            totalTime += tp._totalTime;
        }

        String str = _locationNames[_currentLocation]
            + ","
            + TestPerf._impNames[_currentProduct]
            + ","
            + _currentMaxConnections
            + ","
            + _sizeNames[_currentSizeIndex]
            + ","
            + _currentPipeDepth
            + ","
            + _currentCount
            + ","
            + _currentThreads
            + ","
            + REPEAT_TIMES
            + ","
            + totalPerTrans
            / REPEAT_TIMES
            + ","
            + minPerTrans
            + ","
            + maxPerTrans
            + ","
            + totalTime;
        println(str);
    }

    public void println(String str)
    {
        if (_noPrint)
            return;
        _pw.println(str);
        _pw.flush();
        System.out.println(str);
    }

    public void printHeader()
    {
        String str = "Where"
            + ","
            + "Product"
            + ","
            + "Connections"
            + ","
            + "Size"
            + ","
            + "Pipe Depth"
            + ","
            + "Count"
            + ","
            + "Threads"
            + ","
            + "Repeats"
            + ",Avg/Trans,Min/Trans,Max/Trans,Total";
        println(str);
    }

    public void run(String args[]) throws Exception
    {
        String dir = "/tmp/perf/";

        DateFormat df = new SimpleDateFormat("yyyyMMMdd-HHmm");
        File file = new File(dir + "run" + df.format(new Date()) + ".csv");
        _pw = new PrintWriter(new BufferedOutputStream(new FileOutputStream(file)));

        printHeader();

        DecimalFormat format = new DecimalFormat("00000");

        int logCounter = 1;

        if (!true)
            LogUtils.logConnFile("/home/francis/logPerf" + "big" + ".txt");

        // Overall looping
        for (int i = 0; i < 1; i++)
        {
            for (_currentLocation = 0; _currentLocation < _locationNames.length; _currentLocation++)
            {
                if (_singleLocation != -1
                    && _currentLocation != _singleLocation)
                    continue;

                println("");

                for (_currentSizeIndex = 0; _currentSizeIndex < _sizeNames.length; _currentSizeIndex++)
                {
                    if (_singleSize != -1 && _currentSizeIndex != _singleSize)
                        continue;

                    _currentUseSuffixes = _useSuffixes[_currentLocation][_currentSizeIndex];

                    println("");

                    for (_currentProduct = 0; _currentProduct < TestPerf._impNames.length; _currentProduct++)
                    {
                        if (_singleProduct != -1
                            && _currentProduct != _singleProduct)
                            continue;

                        for (int connIndex = 0; connIndex < _productConnections[_currentProduct].length; connIndex++)
                        {
                            if (_singleConnectionLimit != -1
                                && connIndex != _singleConnectionLimit)
                                continue;

                            _currentMaxConnections = _productConnections[_currentProduct][connIndex];

                            for (int pipeIndex = 0; pipeIndex < _pipeDepths[_currentProduct].length; pipeIndex++)
                            {
                                _currentPipeDepth = _pipeDepths[_currentProduct][pipeIndex];

                                _currentCount = _sizeCounts[_currentSizeIndex][_currentLocation];

                                for (int threadIndex = 0; threadIndex < _threadCounts.length; threadIndex++)
                                {
                                    _currentThreads = _threadCounts[threadIndex];
                                    _currentCountPerThread = _currentCount
                                        / _currentThreads;

                                    if (true)
                                    {
                                        String textCount = format
                                                .format(logCounter++);
                                        LogUtils
                                                .logConnFile("/home/francis/logPerf"
                                                    + textCount
                                                    + ".txt");
                                    }

                                    runSet();
                                }
                            }
                        }
                    }
                }
            }
        }
        _pw.close();

        // For profiler
        // Thread.sleep(10000000);

    }

    public static void main(String args[]) throws Exception
    {
        PerfComparisonTest pt = new PerfComparisonTest();
        pt.run(args);
    }

}
