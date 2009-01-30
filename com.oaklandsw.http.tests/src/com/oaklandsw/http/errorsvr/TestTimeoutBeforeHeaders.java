package com.oaklandsw.http.errorsvr;

import com.oaklandsw.util.Log;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.server.ErrorServer;
import com.oaklandsw.util.LogUtils;

//Bug 1997 - SocketTimeoutException shows through in reading headers
public class TestTimeoutBeforeHeaders extends TestTimeout
{
    private static final Log _log = LogUtils.makeLogger();

    public TestTimeoutBeforeHeaders(String testName)
    {
        super(testName);
        _timeoutWhen = ErrorServer.ERROR_BEFORE_HEADERS;
        _timeoutBeforeHeaders = true;
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestTimeoutBeforeHeaders.class);
        return suite;
    }


}
