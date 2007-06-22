
/**
 * Performance comparison test script.
 */
public class PerfComparisonTest
{

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
        "http://repoman/", //
        "http://host72.hrwebservices.net/~bigtivo/perftest/gfx/" };

    // Test types
    public static final int      TYPE_SMALL          = 0;

    public static final String[] _typeNames          = new String[] { "Small (3K)" };

    // Small file to read
    public static final String   _filesSmall[]       = { "tnet_wht.jpg", //
        "tnet_wht.jpg", //
        "tnet_wht.jpg"                              };

    // Configurations
    public static final int      CONF_OAKLANDSW      = 0;
    public static final int      CONF_OAKLANDSW_PIPE = 1;
    public static final int      CONF_SUN            = 2;
    public static final int      CONF_APACHE         = 3;

    public static final String[] _confNames          = new String[] {
        "Oaklandsw", "Oaklandsw pipeling", "Sun", "Apache" };

    public static final int      REPEAT_TIMES        = 4;

    // Runs a set of tests
    public void runSet(int conf, int location, int type, int count)
        throws Exception
    {
        TestPerf tp = new TestPerf();
        String url = _urls[location];

        switch (type)
        {
            case TYPE_SMALL:
                url += _filesSmall[location];
                break;
        }

        float min = 0;
        float max = 0;
        float times[] = new float[REPEAT_TIMES];
        float total = 0;

        for (int i = 0; i < REPEAT_TIMES; i++)
        {
            tp.run(new String[] { "-quiet", "-url", url,
                conf == CONF_SUN ? "-sun" : "",
                conf == CONF_OAKLANDSW_PIPE ? "-pipe" : "", "-times",
                Integer.toString(count) });

            if (tp._transTime > max)
                max = tp._transTime;
            if (min == 0 || tp._transTime < min)
                min = tp._transTime;
            times[i] = tp._transTime;
            total += tp._transTime;
        }

        System.out.println(_confNames[conf]
            + "/"
            + _scenarioNames[location]
            + "/"
            + _typeNames[type]
            + "/"
            + count
            + " - Repeated: "
            + REPEAT_TIMES
            + "x  Avg: "
            + total
            / REPEAT_TIMES
            + "  min: "
            + min
            + " max: "
            + max);
    }

    public void run(String args[]) throws Exception
    {
        //LogUtils.logAll();
        //runSet(CONF_OAKLANDSW_PIPE, SC_LOCAL, TYPE_SMALL, 1);
        runSet(CONF_SUN, SC_LOCAL, TYPE_SMALL, 5000);
        Thread.sleep(1000);
        runSet(CONF_OAKLANDSW, SC_LOCAL, TYPE_SMALL, 5000);
        
        // For profiler
        Thread.sleep(10000000);

    }

    public static void main(String args[]) throws Exception
    {
        PerfComparisonTest pt = new PerfComparisonTest();
        pt.run(args);
    }

}
