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

    public static final int      REPEAT_TIMES           = 3;

    public static final String   INTRANET_3K            = "http://repoman:8081/3k";

    // Network scenarios
    public static final int      SC_LOCAL               = 0;
    public static final int      SC_INTRANET_APACHE     = 1;
    public static final int      SC_INTERNET            = 2;
    public static final int      SC_LAST                = SC_INTERNET;

    public static final String[] _locationNames         = new String[] {
        "Machine", "LAN", "Internet"                   };

    // Each array element corresponds to one of the scenarios, these
    // are sizes in K
    public static final int[]    _sizes                 = new int[] { 0, 1, 2,
        4, 8, 16, 32, 64, 128                          };

    // Location
    public static final String[] _locationUrls          = new String[] {
        "http://berlioz/oaklandsw-http/", //
        "http://repoman:8081/", //
        "http://francisupton.com/"                     };

    // Size x location
    // Don't use these limits for now
    public static final int[][]  _sizeConnLimits        = new int[][] {
        { 0, 0, 0 }, { 0, 0, 0 }, { 0, 0, 0 }, { 0, 0, 0 }, { 0, 0, 0 },
        { 0, 0, 0 }, { 0, 0, 0 }, { 0, 0, 0 }, { 0, 0, 0 } };

    // Size x location
    public static final int[][]  _sizeCounts            = new int[][] {
        { 2000, 2000, 200 }, { 2000, 2000, 200 }, { 2000, 2000, 200 },
        { 2000, 2000, 200 }, { 2000, 2000, 200 }, { 2000, 2000, 200 },
        { 2000, 2000, 200 }, { 2000, 2000, 200 }, { 2000, 2000, 200 } };

    // Product Pipe depths
    // Product x pipeline depth
    // WARNING - the order of these is defined by the values in TestPerf
    public static final int[][]  _pipeDepths            = new int[][] {//
                                                        { 0 }, // Oakland
        { 0 }, // Sun
        { 0 }, // Apache
        { 1, 2, 50, 100, 200, 0 }, // Oakland Pipe
                                                        };
    // Product Connections Limits
    // Product x connection limit
    // WARNING - the order of these is defined by the values in TestPerf
    public static final int[][]  _productConnections    = new int[][] {
        { 1, 2, 5, 10, 20 }, // Oakland
        { 1 }, // Sun (Note, in the Sun implementation the number of
        // connections is the number of threads)
        { 1, 2, 5, 10, 20 }, // Apache
        { 1, 2, 5, 10, 20 }, // Oakland Pipe
                                                        };

    // Counts of the number of threads to use
    public static final int[]    _threadCounts          = new int[] { 1, 2, 5,
        10, 20                                         };

    public PrintWriter           _pw;

    public boolean               _noPrint;

    public int                   _currentMaxConnections;
    public int                   _currentPipeDepth;
    public int                   _currentProduct;
    public int                   _currentSizeIndex;
    public int                   _currentLocation;
    public int                   _currentCount;
    public int                   _currentCountPerThread;
    public int                   _currentThreads;

    public int                   _singleSize            = -1;
    public int                   _singleLocation        = 1;
    public int                   _singleConnectionLimit = -1;
    public int                   _singleProduct         = -1;

    // Runs a set of tests
    public void runSet() throws Exception
    {
        String url;

        url = _locationUrls[_currentLocation] + _sizes[_currentSizeIndex] + "k";
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
            tp._useSuffixes = false;
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
            + _sizes[_currentSizeIndex]
            + ","
            + totalPerTrans
            / REPEAT_TIMES
            + ","
            + _currentMaxConnections
            + ","
            + _currentThreads
            + ","
            + _currentPipeDepth
            + ","
            + _currentCount
            + ","
            + REPEAT_TIMES
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
            + ",Product"
            + ",Size (K)"
            + ",Avg ms/Trans"
            + ",Connections"
            + ",Threads"
            + ",Pipe Depth"
            + ",Count"
            + ",Repeats"
            + ",Min ms/Trans"
            + ",Max ms/Trans"
            + ",Total ms";
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

                for (_currentSizeIndex = 0; _currentSizeIndex < _sizes.length; _currentSizeIndex++)
                {
                    if (_singleSize != -1 && _currentSizeIndex != _singleSize)
                        continue;

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

                                    // Sun implementation always as many
                                    // connections needed by the number
                                    // of threads, regardless of the setting of
                                    // the http.maxConnections
                                    if (_currentProduct == TestPerf.IMP_SUN)
                                        _currentMaxConnections = _currentThreads;

                                    if (!true)
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
