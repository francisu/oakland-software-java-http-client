package com.oaklandsw.http.errorsvr;

import org.apache.commons.logging.Log;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.server.ErrorServer;
import com.oaklandsw.util.LogUtils;

public class TestTimeoutDuringStatus extends TestTimeout
{
    private static final Log _log = LogUtils.makeLogger();

    public TestTimeoutDuringStatus(String testName)
    {
        super(testName);
        _timeoutWhen = ErrorServer.ERROR_DURING_STATUS;
        _timeoutBeforeHeaders = true;
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestTimeoutDuringStatus.class);
        return suite;
    }


}
