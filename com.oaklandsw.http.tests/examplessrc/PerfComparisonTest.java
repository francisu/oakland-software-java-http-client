import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.oaklandsw.http.HttpURLConnection;
import com.oaklandsw.util.Util;

/**
 * Performance comparison test script.
 */
public class PerfComparisonTest
{
    public static final int      REPEAT_TIMES        = 4;

    // Network scenarios
    public static final int      SC_LOCAL            = 0;
    public static final int      SC_INTRANET         = 1;
    public static final int      SC_INTERNET         = 2;
    public static final int      SC_LAST             = SC_INTERNET;

    public static final String[] _scenarioNames      = new String[] { "Local",
        "Intranet", "Internet"                      };

    // These correspond to the above scenarios
    public static final String   _urls[]             = {
        "http://berlioz/oaklandsw-http/", //
        "http://repoman/noauth/wwwroot/", //
"http://www.oaklandsoftware.com/"           };
//    "http://host72.hrwebservices.net/~bigtivo/perftest/gfx/" };

    // Test types
    public static final int      TYPE_3K             = 0;
    public static final int      TYPE_100K           = 1;

    public static final String[] _typeNames          = new String[] { "3K",
        "100K"                                      };

    // Small file to read
    public static final String   _filesSmall[]       = { "tnet_wht.jpg", //
        "tnet_wht.jpg", //
        "tnet_wht.jpg"                              };

    public static final String   _files100[]         = { "inspApp.jpg", //
        "inspApp.jpg", //
        "inspApp.jpg"                               };

    // Configurations
    public static final int      CONF_OAKLANDSW      = 0;
    public static final int      CONF_OAKLANDSW_PIPE = 1;
    public static final int      CONF_SUN            = 2;
    public static final int      CONF_APACHE         = 3;

    public static final String[] _confNames          = new String[] {
        "Oakland", "Oakland Pipe", "Sun", "Apache" };

    public PrintWriter           _pw;

    public boolean               _noPrint;

    // Runs a set of tests
    public void runSet(int conf, int location, int type, int count, int pdepth)
        throws Exception
    {
        runSet(conf, location, type, count, pdepth, null);
    }

    // Runs a set of tests
    public void runSet(int conf,
                       int location,
                       int type,
                       int count,
                       int pdepth,
                       String args[]) throws Exception
    {
        TestPerf tp = new TestPerf();
        String url = _urls[location];

        switch (type)
        {
            case TYPE_3K:
                url += _filesSmall[location];
                break;
            case TYPE_100K:
                url += _files100[location];
                break;
        }

        float minPerTrans = 0;
        float maxPerTrans = 0;
        float times[] = new float[REPEAT_TIMES];
        float totalPerTrans = 0;
        long totalTime = 0;

        String[] baseRunArgs = new String[] { "-nowarmup", "-quiet", "-url",
            url, "-pipedepth", Integer.toString(pdepth),
            conf == CONF_SUN ? "-sun" : "",
            conf == CONF_OAKLANDSW_PIPE ? "-pipe" : "", "-times",
            Integer.toString(count) };

        String[] runArgs;

        if (args == null)
        {
            runArgs = baseRunArgs;
        }
        else
        {
            runArgs = new String[baseRunArgs.length + args.length];
            System.arraycopy(baseRunArgs, 0, runArgs, 0, baseRunArgs.length);
            System.arraycopy(args, 0, runArgs, baseRunArgs.length, args.length);
        }

        for (int i = 0; i < REPEAT_TIMES; i++)
        {
            tp = new TestPerf();
            tp.run(runArgs);

            if (tp._transTime > maxPerTrans)
                maxPerTrans = tp._transTime;
            if (minPerTrans == 0 || tp._transTime < minPerTrans)
                minPerTrans = tp._transTime;
            times[i] = tp._transTime;
            totalPerTrans += tp._transTime;
            totalTime += tp._totalTime;
        }

        String str = _scenarioNames[location]
            + ","
            + _confNames[conf]
            + (pdepth > 0 ? "-" + pdepth : "")
            + ","
            + _typeNames[type]
            + ","
            + count
            + ","
            + (args != null ? "\"" + Util.arrayToString(args) + "\"" : "")
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
            + "Config"
            + ","
            + "Data"
            + ","
            + "Count"
            + ","
            + "Args"
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

        HttpURLConnection.setMaxConnectionsPerHost(2);

        // LogUtils.logAll();
        if (false)
        {

            if (false)
            {
                _noPrint = true;
                runSet(CONF_SUN, SC_INTERNET, TYPE_3K, 10, 0);
                _noPrint = false;
                runSet(CONF_SUN, SC_INTERNET, TYPE_3K, 50, 0);
            }

            // Warm up
            if (true)
            {
                _noPrint = true;
                runSet(CONF_OAKLANDSW, SC_INTRANET, TYPE_100K, 10, 0);
                _noPrint = false;
            }

            runSet(CONF_SUN, SC_INTRANET, TYPE_100K, 200, 0);
            runSet(CONF_OAKLANDSW, SC_INTRANET, TYPE_100K, 200, 0);

        }

        if (true)
        {
            println(HttpURLConnection.getMaxConnectionsPerHost()
                + " connections/host");
            printHeader();

            println("");

            testSet(SC_LOCAL, TYPE_100K, 3000);

            println("");

            testSet(SC_INTRANET, TYPE_100K, 3000);

            println("");

            testSet(SC_INTERNET, TYPE_100K, 1000);
        }

        _pw.close();

        // For profiler
        // Thread.sleep(10000000);

    }

    public void testSet(int where, int type, int count) throws Exception
    {
        if (false)
        {
            runSet(CONF_OAKLANDSW_PIPE, where, type, count, 2);
            runSet(CONF_OAKLANDSW_PIPE, where, type, count, 10);
            runSet(CONF_OAKLANDSW_PIPE, where, type, count, 50);
            runSet(CONF_OAKLANDSW_PIPE, where, type, count, 100);
            runSet(CONF_OAKLANDSW_PIPE, where, type, count, 200);
            runSet(CONF_OAKLANDSW_PIPE, where, type, count, 0);
        }

        // Warm up
        _noPrint = true;
        runSet(CONF_SUN, where, type, 10, 0);
        _noPrint = false;

        runSet(CONF_SUN, where, type, count, 0);

        // Warm up
        _noPrint = true;
        runSet(CONF_OAKLANDSW, where, type, 10, 0);
        _noPrint = false;

        runSet(CONF_OAKLANDSW, where, type, count, 0);

    }

    public static void main(String args[]) throws Exception
    {
        PerfComparisonTest pt = new PerfComparisonTest();
        pt.run(args);
    }

}
