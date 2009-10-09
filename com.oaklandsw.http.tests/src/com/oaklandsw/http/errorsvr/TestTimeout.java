package com.oaklandsw.http.errorsvr;

import com.oaklandsw.http.HttpTestBase;
import com.oaklandsw.http.HttpTestEnv;
import com.oaklandsw.http.HttpTimeoutException;
import com.oaklandsw.http.HttpURLConnection;
import com.oaklandsw.http.server.ErrorServer;

import com.oaklandsw.util.Log;
import com.oaklandsw.util.LogUtils;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.InputStream;

import java.net.URL;


public class TestTimeout extends HttpTestBase {
    private static final Log _log = LogUtils.makeLogger();
    protected String _timeoutWhen;

    // Indicates the timeout happens sometime before the headers are complete
    protected boolean _timeoutBeforeHeaders;

    public TestTimeout(String testName) {
        super(testName);
        _timeoutWhen = ErrorServer.ERROR_BEFORE_CONTENT;
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(TestTimeout.class);

        return suite;
    }

    public static void main(String[] args) {
        mainRun(suite(), args);
    }

    // Test read timeout
    public void testReadTimeout(int type) throws Exception {
        boolean connectTimeout = (type == CONN_CONNECT) ||
            (type == DEF_CONNECT);

        int timeout = 500;

        // 1 second timeout
        setupDefaultTimeout(type, timeout);

        // Delay the content for 3 seconds
        URL url = new URL(HttpTestEnv.TEST_URL_HOST_ERRORSVR +
                "?error=timeout" + "&when=" + _timeoutWhen + "&sec=3" +
                _errorDebug);

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        setupConnTimeout(urlCon, type, timeout);

        urlCon.setRequestMethod("GET");
        urlCon.connect();

        if (_timeoutBeforeHeaders) {
            try {
                urlCon.getInputStream().close();

                if (!connectTimeout) {
                    fail("Should have timed out on the read");
                }
            } catch (HttpTimeoutException ex) {
                if (connectTimeout) {
                    fail("These timeouts should have no effect");
                }

                // Expected
            }
        } else {
            // Should work
            urlCon.getResponseCode();

            InputStream is = urlCon.getInputStream();

            try {
                is.read();

                if (!connectTimeout) {
                    fail("Should have timed out on the read");
                } else {
                    is.close();
                }
            } catch (HttpTimeoutException ex) {
                if (connectTimeout) {
                    fail("These timeouts should have no effect");
                }

                // Expected
            }
        }

        // Make sure we never return a null InputStream
        if (!connectTimeout) {
            try {
                assertNotNull(urlCon.getInputStream());
                fail("Did not get expected exception");
            } catch (IllegalStateException ex) {
                // Expected
            }
        }

        checkNoActiveConns(url);
    }

    public void testReadTimeoutDef() throws Exception {
        testReadTimeout(DEF);
    }

    public void testReadTimeoutDefConnect() throws Exception {
        testReadTimeout(DEF_CONNECT);
    }

    public void testReadTimeoutDefRequest() throws Exception {
        testReadTimeout(DEF_REQUEST);
    }

    public void testReadTimeoutConn() throws Exception {
        testReadTimeout(CONN);
    }

    public void testReadTimeoutConnConnect() throws Exception {
        testReadTimeout(CONN_CONNECT);
    }

    public void testReadTimeoutConnRequest() throws Exception {
        testReadTimeout(CONN_REQUEST);
    }

    public void allTestMethods() throws Exception {
        testReadTimeoutDef();
        testReadTimeoutDefConnect();
        testReadTimeoutDefRequest();
        testReadTimeoutConn();
        testReadTimeoutConnConnect();
        testReadTimeoutConnRequest();
    }
}
