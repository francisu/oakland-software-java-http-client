import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.logging.Log;

import com.oaklandsw.util.LogUtils;

/**
 * Performance comparison test script.
 */
public class PerfComparisonTest
{
    static final Log             _log                   = LogUtils
                                                                .makeLogger("com.oaklandsw.http.TestPerf");

    public static final int      REPEAT_TIMES           = 3;

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
        4, 8, 16, 32, 64, 128, 256, 512, 1024          };

    // Location
    public static final String[] _locationUrls          = new String[] {
      "http://berlioz/oaklandsw-http/", //
//      "http://berlioz/cgi-bin/httptest.pl?size=", //
//      "http://localhost:8080/oaklandsw-http/perf?size=", //
        "http://repoman:8081/", //
        "http://216.120.249.245/~oakland/", //
                                                        // "http://67.121.125.19/oaklandsw-http/",
                                                        // //
                                                        // "http://oaklandswint.page.us/oaklandsw-http/"
                                                        // //
                                                        // "http://francisupton.com/"
                                                        // //
                                                        };

    // Size x location
    // Don't use these limits for now
    public static final int[][]  _sizeConnLimits        = new int[][] {
        { 0, 0, 0 }, { 0, 0, 0 }, { 0, 0, 0 }, { 0, 0, 0 }, { 0, 0, 0 },
        { 0, 0, 0 }, { 0, 0, 0 }, { 0, 0, 0 }, { 0, 0, 0 }, { 0, 0, 0 },
        { 0, 0, 0 }, { 0, 0, 0 },                      };

    // Size x location
    public static final int[][]  _sizeCounts            = new int[][] {
        { 2000, 2000, 100 }, { 2000, 2000, 100 }, { 2000, 2000, 100 },
        { 2000, 2000, 100 }, { 2000, 2000, 100 }, { 2000, 2000, 100 },
        { 2000, 2000, 100 }, { 2000, 2000, 100 }, { 2000, 2000, 100 },
        { 2000, 2000, 100 }, { 2000, 2000, 100 }, { 2000, 2000, 100 } };

    // Product Pipe depths (only apply to pipelining)
    // location x pipeline depth
    public static final int[][]  _pipeDepths            = new int[][] {//
        { 1, 2, 50, 100, 200, 500, 1000, 0 },
        { 1, 2, 50, 100, 200, 500, 1000, 0 }, { 1, 2, 5, 10, 20, 0 }, };

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
        10, 20,                                        };

    // One per each product, so we warm up the classes associated with the
    // product
    // for the first run
    boolean                      _productFirstTimes[]   = { true, true, true,
        true                                           };

    int                          _prevThreadCount;

    public PrintWriter           _pwAverage;
    public PrintWriter           _pwDetails;

    public boolean               _noPrint;

    // Just have the tests calculate the amount of required bandwidth
    public boolean               _calculateBandWidth    = !true;

    public int                   _calcBytes;
    public int                   _calcConnections;

    public int                   _currentMaxConnections;
    public int                   _currentPipeDepth;
    public int                   _currentProduct;
    public int                   _currentSizeIndex;
    public int                   _currentLocation;
    public int                   _currentCount;
    public int                   _currentCountPerThread;
    public int                   _currentThreads;

    public int                   _singleSize            = -1;
    public int                   _singleLocation        = -1;
    public int                   _singleConnectionLimit = -1;
    public int                   _singleProduct         = -1;

    public int                   _startSize             = 0;
    public int                   _startLocation         = 1;
    public int                   _startConnectionLimit  = 0;
    public int                   _startThreads          = 0;
    public int                   _startProduct          = 0;
    public int                   _startPipeDepth        = 0;

    protected void warmUpRun(TestPerf tp) throws Exception
    {
        int times = 1;
        if (_currentLocation == SC_INTERNET)
            times = 1;
        for (int j = 0; j < times; j++)
            tp.run();
    }

    // Runs a set of tests
    public void runSet(int logCounter) throws Exception
    {
        DecimalFormat format = new DecimalFormat("00000");

        if (!true)
        {
            String textCount = format.format(logCounter++);
            LogUtils.logConnFile("/home/francis/logPerf" + textCount + ".txt");
        }

        String url;

        url = _locationUrls[_currentLocation] + _sizes[_currentSizeIndex] + "k";
        if (url == null)
            return;

        float minPerTrans = 0;
        float maxPerTrans = 0;
        float times[] = new float[REPEAT_TIMES];
        float totalPerTrans = 0;
        long totalTime = 0;
        int totalPipeDepth = 0;

        int totalAvoidedFlushes = 0;
        int totalBufferFlushes = 0;
        int totalForcedFlushes = 0;
        int totalRetries = 0;
        int totalPipelineRetries = 0;

        // We make a timestamp for the run, everything for that run gets
        // that stamp in all logs
        DateFormat df = new SimpleDateFormat("HH:mm:ss.SSS");
        String startTime = df.format(new Date());

        for (int i = 0; i < REPEAT_TIMES; i++)
        {
            TestPerf tp = new TestPerf();
            tp._warmUp = false;
            tp._urlString = url;
            tp._pipeDepth = _currentPipeDepth;
            tp._implementation = _currentProduct;
            tp._totalTimes = _currentCount;
            tp._threads = _currentThreads;
            tp._maxConn = _currentMaxConnections;
            tp._quiet = false;
            tp._startTime = startTime;
            tp._expectedSize = _sizes[_currentSizeIndex] * 1024;
            
            tp._requestType = TestPerf.REQ_ID;

            // Do a warm-up by running the tests a few times, as the numbers
            // associated with the first tests are artificially high
            // We need to get the classes all loaded for each product
            if (_productFirstTimes[_currentProduct])
            {
                warmUpRun(tp);
                _productFirstTimes[_currentProduct] = false;
            }

            // Seems for Jakarta and Oakland when we go over into 20 and 50
            // threads it takes a huge hit, not sure why but this will remove it
            // This is probably something on the web server
            if (_prevThreadCount < _currentThreads
                && _currentThreads >= 20
                && _currentThreads == _currentMaxConnections)
            {
                warmUpRun(tp);
            }
            _prevThreadCount = _currentThreads;

            com.oaklandsw.http.HttpURLConnection
                    .setDefaultConnectionRequestLimit(_sizeConnLimits[_currentSizeIndex][_currentLocation]);

            int bytes = _currentCount * _sizes[_currentSizeIndex];
            _calcBytes += bytes;
            // Rough estimate assuming 100 bytes per connection
            int conns = Math.max(_currentMaxConnections, _currentCount / 100);
            _calcConnections += conns;

            log("");

            if (!_calculateBandWidth)
            {
                tp.run();
            }

            log("Total run bytes: " + bytes);
            log("Total run conns: " + conns);

            if (tp._transTime > maxPerTrans)
                maxPerTrans = tp._transTime;
            if (minPerTrans == 0 || tp._transTime < minPerTrans)
                minPerTrans = tp._transTime;
            times[i] = tp._transTime;
            totalPerTrans += tp._transTime;
            totalTime += tp._totalTime;

            totalPipeDepth += _currentPipeDepth;
            // Compute the depth based on what was observed
            if (_currentPipeDepth == 0
                && _currentProduct == TestPerf.IMP_OAKLAND_PIPE)
            {
                totalPipeDepth += tp._actualPipeMaxDepth;
            }

            totalForcedFlushes += tp._actualForcedFlushes;
            totalBufferFlushes += tp._actualBufferFlushes;
            totalAvoidedFlushes += tp._actualAvoidFlushes;
            totalRetries += tp._actualRetries;
            totalPipelineRetries += tp._actualPipelineRetries;

            String str = _locationNames[_currentLocation]
                + ","
                + TestPerf._impNames[_currentProduct]
                + ","
                + _sizes[_currentSizeIndex]
                + ","
                + tp._transTime
                + ","
                + _currentMaxConnections
                + ","
                + _currentThreads
                + ","
                + tp._actualPipeMaxDepth
                + ","
                + _currentPipeDepth
                + ","
                + _currentCount
                + ","
                + REPEAT_TIMES
                + ","
                + tp._totalTime
                + ","
                + tp._actualForcedFlushes
                + ","
                + tp._actualBufferFlushes
                + ","
                + tp._actualAvoidFlushes
                + ","
                + tp._actualRetries
                + ","
                + tp._actualPipelineRetries
                + ","
                + startTime;
            println(str, _pwDetails);

        }

        String str = _locationNames[_currentLocation]
            + ","
            + TestPerf._impNames[_currentProduct]
            + ","
            + _sizes[_currentSizeIndex]
            + ","
            + totalPerTrans / REPEAT_TIMES
            + ","
            + _currentMaxConnections
            + ","
            + _currentThreads
            + ","
            + (totalPipeDepth == 0 ? "" : Integer
                    .toString((totalPipeDepth / REPEAT_TIMES)))
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
            + totalTime
            + ","
            + totalForcedFlushes
            + ","
            + totalBufferFlushes
            + ","
            + totalAvoidedFlushes
            + ","
            + totalRetries
            + ","
            + totalPipelineRetries
            + ","
            + startTime;
        println(str, _pwAverage);
    }

    public void printHeader()
    {
        String str = "Where"
            + ",Product"
            + ",SizeK"
            + ",ms/TransAvg"
            + ",Connections"
            + ",Threads"
            + ",PipeDepthObsAvg"
            + ",PipeDepthLim"
            + ",Count"
            + ",Repeats"
            + ",ms/TransMin"
            + ",ms/TransMax"
            + ",msTotal"
            + ",FlushesForced"
            + ",FlushesBuffFull"
            + ",FlushesAvoided"
            + ",Retry"
            + ",RetryPL"
            + ",StartTime";
        println(str, _pwAverage);

        str = "Where"
            + ",Product"
            + ",SizeK"
            + ",ms/Trans"
            + ",Connections"
            + ",Threads"
            + ",PipeDepthObs"
            + ",PipeDepthLim"
            + ",Count"
            + ",Repeats"
            + ",msTotal"
            + ",FlushesForced"
            + ",FlushesBuffFull"
            + ",FlushesAvoided"
            + ",Retry"
            + ",RetryPL"
            + ",StartTime";
        println(str, _pwDetails);
    }

    public void println(String str, PrintWriter pw)
    {
        if (_noPrint)
            return;
        pw.println(str);
        pw.flush();
        log(str);
    }

    public void log(String str)
    {
        _log.info(str);
        System.out.println(str);
    }

    public void run(String args[]) throws Exception
    {
        String dir = "/home/francis/d/com.oaklandsw.http.tests/perfResults/";

        DateFormat df = new SimpleDateFormat("yyyyMMMdd-HHmm");

        String fileTime = df.format(new Date());
        String fileStart = "run" + fileTime;
        File avgfile = new File(dir + fileStart + ".csv");
        File detfile = new File(dir + fileStart + "_details.csv");

        _pwAverage = new PrintWriter(new BufferedOutputStream(new FileOutputStream(avgfile)));
        _pwDetails = new PrintWriter(new BufferedOutputStream(new FileOutputStream(detfile)));

        if (true)
        {
            Properties logProps = new Properties();
            logProps.setProperty("log4j.logger.com.oaklandsw.http.TestPerf",
                                 "INFO, A1");
            logProps.setProperty("log4j.appender.A1",
                                 "org.apache.log4j.FileAppender");
            logProps.setProperty("log4j.appender.A1.File", dir
                + fileStart
                + "_log.txt");
            logProps.setProperty("log4j.appender.A1.Append", "false");
            logProps.setProperty("log4j.appender.A1.layout",
                                 "org.apache.log4j.PatternLayout");
            logProps.setProperty("log4j.appender.A1.layout.ConversionPattern",
                                 LogUtils.DEBUG_PATTERN);
            LogUtils.configureLog(logProps);
        }

        printHeader();

        int logCounter = 1;

        // Overall looping
        for (int i = 0; i < 1; i++)
        {
            for (_currentLocation = _startLocation; _currentLocation < _locationNames.length; _currentLocation++)
            {
                if (_singleLocation != -1
                    && _currentLocation != _singleLocation)
                    continue;

                for (_currentSizeIndex = _startSize; _currentSizeIndex < _sizes.length; _currentSizeIndex++)
                {
                    if (_singleSize != -1 && _currentSizeIndex != _singleSize)
                        continue;

                    for (_currentProduct = _startProduct; _currentProduct < TestPerf._impNames.length; _currentProduct++)
                    {
                        if (_singleProduct != -1
                            && _currentProduct != _singleProduct)
                            continue;

                        for (int connIndex = _startConnectionLimit; connIndex < _productConnections[_currentProduct].length; connIndex++)
                        {
                            if (_singleConnectionLimit != -1
                                && connIndex != _singleConnectionLimit)
                                continue;

                            _currentMaxConnections = _productConnections[_currentProduct][connIndex];

                            for (int threadIndex = _startThreads; threadIndex < _threadCounts.length; threadIndex++)
                            {
                                _currentThreads = _threadCounts[threadIndex];

                                // Sun implementation always as many
                                // connections needed by the number
                                // of threads, regardless of the setting of
                                // the http.maxConnections
                                if (_currentProduct == TestPerf.IMP_SUN)
                                    _currentMaxConnections = _currentThreads;

                                // There is never any benefit to having more
                                // threads than connections
                                // (the data shows this)
                                if (_currentThreads != _currentMaxConnections)
                                    continue;

                                // With non-pipelined implementations,
                                // thread count should equal connections (no
                                // benefit otherwise)
                                if (_currentThreads != _currentMaxConnections
                                    && _currentProduct != TestPerf.IMP_OAKLAND_PIPE)
                                    continue;

                                _currentCountPerThread = _currentCount
                                    / _currentThreads;

                                _currentCount = _sizeCounts[_currentSizeIndex][_currentLocation];

                                if (_currentProduct == TestPerf.IMP_OAKLAND_PIPE)
                                {
                                    for (int pipeIndex = _startPipeDepth; pipeIndex < _pipeDepths[_currentLocation].length; pipeIndex++)
                                    {
                                        _currentPipeDepth = _pipeDepths[_currentLocation][pipeIndex];
                                        runSet(logCounter++);
                                    }
                                }
                                else
                                {
                                    runSet(logCounter++);
                                }
                            }
                        }
                    }
                }
            }
        }
        _pwAverage.close();

        log("Total bandwidth: " + _calcBytes);
        log("Total conns:     " + _calcConnections);

        // For profiler
        // Thread.sleep(10000000);

    }

    public static void main(String args[]) throws Exception
    {
        PerfComparisonTest pt = new PerfComparisonTest();
        pt.run(args);
    }

}
