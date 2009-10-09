package com.oaklandsw.http.errorsvr;

import com.oaklandsw.http.HttpTestBase;
import com.oaklandsw.http.HttpTestEnv;
import com.oaklandsw.http.HttpURLConnection;
import com.oaklandsw.http.server.ErrorServer;

import com.oaklandsw.util.Log;
import com.oaklandsw.util.LogUtils;
import com.oaklandsw.util.Util;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.URL;


public class TestDisconnect extends HttpTestBase {
    private static final Log _log = LogUtils.makeLogger();
    protected static String _errorUrl = HttpTestEnv.TEST_URL_HOST_ERRORSVR;

    public TestDisconnect(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(TestDisconnect.class);

        return suite;
    }

    public static void main(String[] args) {
        mainRun(suite(), args);
    }

    private URL makeUrl(String when, int lines) throws Exception {
        String urlStr = _errorUrl + "?error=disconnect" + "&when=" + when +
            _errorDebug;

        if (lines > 0) {
            urlStr += ("&lines=" + lines);
        }

        return new URL(urlStr);
    }

    public void testTryResponse(String when, int lines)
        throws Exception {
        try {
            URL url = makeUrl(when, lines);

            HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
            urlCon.setRequestMethod("POST");
            urlCon.setDoOutput(true);
            urlCon.connect();

            String postData = "abcdefghi";

            OutputStream outStr = urlCon.getOutputStream();
            outStr.write(postData.getBytes("ASCII"));
            outStr.close();

            try {
                urlCon.getResponseCode();
                fail("expected exception not received");
            } catch (Exception ex) {
                // System.out.println("Expected exception: " + ex);
                boolean beforeRead = when.equals(ErrorServer.ERROR_BEFORE_READ);
                // If the error server is local, then everything looks like a
                // close before the read
                // This does not seem to work regardless of where the error
                // server is
                // so it's not possible to really test this case
                // if (TestEnv.ERROR_HOST.equals(TestEnv.LOCALHOST))
                beforeRead = true;

                if (beforeRead) {
                    assertTrue(ex.getMessage().indexOf("after request was sent") >= 0);
                } else {
                    assertTrue(ex.getMessage().indexOf("in the middle") >= 0);
                }

                // Make sure no retries on a post
                assertTrue(ex.getMessage().indexOf("try #1") >= 0);

                // expected
            }

            checkNoActiveConns(url);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void testTryRead(String when, int lines) throws Exception {
        URL url = makeUrl(when, lines);

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");
        urlCon.connect();

        // Should work
        urlCon.getResponseCode();

        try {
            InputStream is = urlCon.getInputStream();
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            // Should fail when trying to read the stream
            Util.copyStreams(is, os);
            fail("Did not get expected exception");
        } catch (IOException ex) {
            // System.out.println("testTryRead Expected exception: " + ex);
            assertTrue(ex.getMessage().indexOf("before all content") >= 0);
            // There should be no retry since we can't retry once
            // the input stream has been given to the user
            assertTrue(ex.getMessage().indexOf("try #1") >= 0);

            // Expcitied IO Exception
        }

        checkNoActiveConns(url);
    }

    public void testNoRead(String when, int lines) throws Exception {
        URL url = makeUrl(when, lines);

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");
        urlCon.connect();

        // Should work
        urlCon.getResponseCode();

        // Should also work, since we are just closing the connection
        // we don't care if there are errors
        urlCon.getInputStream().close();

        checkNoActiveConns(url);
    }

    public void testServerCloseBR() throws Exception {
        testTryResponse(ErrorServer.ERROR_BEFORE_READ, 0);
    }

    public void testServerCloseDR() throws Exception {
        testTryResponse(ErrorServer.ERROR_DURING_READ, 0);
    }

    public void testServerCloseGetBCRead() throws Exception {
        testTryRead(ErrorServer.ERROR_BEFORE_CONTENT, 0);
    }

    public void testServerCloseGetBCClose() throws Exception {
        testNoRead(ErrorServer.ERROR_BEFORE_CONTENT, 0);
    }

    public void testServerCloseGetDCRead() throws Exception {
        testTryRead(ErrorServer.ERROR_DURING_CONTENT, 10);
    }

    public void testServerCloseGetDCClose() throws Exception {
        testNoRead(ErrorServer.ERROR_DURING_CONTENT, 20);
    }

    // Bug 1433 - bad input stream assigned if connection closed
    public void testServerCloseReadHeaderStream() throws Exception {
        URL url = makeUrl(ErrorServer.ERROR_BEFORE_HEADERS, 10);

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        com.oaklandsw.http.HttpURLConnection.setDefaultMaxTries(1);

        // Connection will happen here and fail
        urlCon.getHeaderField(1);

        // This should throw since the connection has died
        try {
            urlCon.getInputStream();
            fail("Should get exception");
        } catch (IllegalStateException ex) {
            // Expected
        }
    }

    // Bug 1433 - bad response code if connection closed
    public void testServerCloseReadHeaderResponse() throws Exception {
        URL url = makeUrl(ErrorServer.ERROR_BEFORE_HEADERS, 10);

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        com.oaklandsw.http.HttpURLConnection.setDefaultMaxTries(1);

        // Connection will happen here and fail
        urlCon.getHeaderField(1);

        try {
            urlCon.getResponseCode();
            fail("Did not get expected IOException");
        } catch (IOException ex) {
            // This is expected
        }
    }

    public void allTestMethods() throws Exception {
        testServerCloseBR();
        testServerCloseDR();
        testServerCloseGetBCRead();
        testServerCloseGetBCClose();
        testServerCloseGetDCRead();
        testServerCloseGetDCClose();
    }
}
