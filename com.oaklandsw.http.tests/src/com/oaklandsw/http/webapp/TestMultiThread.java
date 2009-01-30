package com.oaklandsw.http.webapp;

import java.io.OutputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import com.oaklandsw.util.Log;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.HttpTestEnv;
import com.oaklandsw.http.HttpURLConnection;
import com.oaklandsw.http.servlet.HeaderServlet;
import com.oaklandsw.http.servlet.ParamServlet;
import com.oaklandsw.http.servlet.RedirectServlet;
import com.oaklandsw.util.LogUtils;
import com.oaklandsw.util.Util;

public class TestMultiThread extends TestWebappBase
{

    private static final Log _log              = LogUtils.makeLogger();

    private static final int NUM_THREADS       = 50;
    private static final int NUM_TIMES         = 10;

    private static final int TEXT_TIMES        = 100;

    private static final int MAX_OPEN_PER_HOST = 3;

    private int              _failed;
    private Exception        _failedException;
    private int              _threadDelay;
    private int              _times            = NUM_TIMES;

    private static String    _textComp;

    static
    {
        StringBuffer textBuf = new StringBuffer(TEXT_TIMES
            * HeaderServlet.TEXT_CONST.length());

        for (int i = 0; i < TEXT_TIMES; i++)
        {
            textBuf.append(HeaderServlet.TEXT_CONST);
        }
        _textComp = textBuf.toString();

    }

    public TestMultiThread(String testName)
    {
        super(testName);

        // Poor Netproxy does not seem to be able to handle this test
        _doAuthCloseProxyTest = false;
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestMultiThread.class);
        return suite;
    }

    public static void main(String args[])
    {
        mainRun(suite(), args);
    }

    public void setUp() throws Exception
    {
        super.setUp();
        _times = NUM_TIMES;
        _threadDelay = 0;
    }

    public void tearDown() throws Exception
    {
        super.tearDown();
    }

    protected void threadMethod(String method,
                                String urlSuffix,
                                String servletName,
                                boolean hasExtendedText)
    {

        try
        {

            URL url = new URL(_urlBase + urlSuffix);
            int response = 0;

            _log.debug(Thread.currentThread().getName()
                + " "
                + method
                + " "
                + urlSuffix
                + " "
                + servletName);

            HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
            urlCon.setRequestMethod(method);

            urlCon.setRequestProperty("emit-text", String.valueOf(TEXT_TIMES));

            if (method
                    .equals(com.oaklandsw.http.HttpURLConnection.HTTP_METHOD_POST))
            {
                urlCon.setDoOutput(true);
                OutputStream os = urlCon.getOutputStream();
                os.write(_textComp.getBytes("ASCII"));
                os.close();
            }

            urlCon.connect();
            Thread.sleep(_threadDelay);
            response = urlCon.getResponseCode();

            // Don't bother checking the rest if this is a redirect
            // and we are not following them
            if (!java.net.HttpURLConnection.getFollowRedirects()
                && response > 300)
            {
                urlCon.getInputStream().close();
                return;
            }

            if (response != 200)
            {
                _log.debug(Thread.currentThread().getName()
                    + " failed status "
                    + response);
                testFailed(1, null);
            }

            String reply = getReply(urlCon);
            boolean localPassed = checkReplyNoAssert(reply, "<title>"
                + servletName
                + " Servlet: "
                + method
                + "</title>");
            if (!localPassed)
            {
                _log.debug(Thread.currentThread().getName()
                    + " failed Header check");
                testFailed(2, null);
            }

            if (hasExtendedText)
            {
                localPassed = checkReplyNoAssert(reply, _textComp);
                if (!localPassed)
                {
                    _log.debug(Thread.currentThread().getName()
                        + " failed body text");
                    testFailed(3, null);
                }
            }

            _log.debug(Thread.currentThread().getName()
                + (_failed > 0 ? " FAILED " : " PASSED "));
        }
        catch (Exception ex)
        {
            testFailed(4, ex);
        }

    }

    protected void testFailed(int reason, Exception ex)
    {
        _log.debug("TEST FAILED: " + reason);
        _failed = reason;
        _failedException = ex;
        if (ex != null)
            ex.printStackTrace();
    }

    protected void threadsBase() throws Exception
    {
        Thread t;

        _failed = 0;

        // Only set it if we are currently set to the default
        if (com.oaklandsw.http.HttpURLConnection.getMaxConnectionsPerHost() == 2)
        {
            com.oaklandsw.http.HttpURLConnection
                    .setMaxConnectionsPerHost(MAX_OPEN_PER_HOST);
        }

        ArrayList threads = new ArrayList();
        for (int i = 0; i < NUM_THREADS; i++)
        {
            final int threadNum = i;
            t = new Thread()
            {
                public void run()
                {
                    try
                    {
                        Thread.currentThread().setName("TestMultiThread"
                            + threadNum);
                        for (int j = 0; j < _times; j++)
                        {
                            threadMethod(com.oaklandsw.http.HttpURLConnection.HTTP_METHOD_GET,
                                         HeaderServlet.NAME,
                                         "Header",
                                         true);

                            threadMethod(com.oaklandsw.http.HttpURLConnection.HTTP_METHOD_GET,
                                         RedirectServlet.NAME
                                             + "?to="
                                             + URLEncoder
                                                     .encode("http://"
                                                                 + HttpTestEnv.TOMCAT_HOST
                                                                 + ":"
                                                                 + HttpTestEnv.TOMCAT_PORT_1
                                                                 + "/"
                                                                 + context
                                                                 + ParamServlet.NAME,
                                                             Util.DEFAULT_ENCODING),
                                         "Param",
                                         false);
                            threadMethod(com.oaklandsw.http.HttpURLConnection.HTTP_METHOD_POST,
                                         HeaderServlet.NAME,
                                         "Header",
                                         true);
                        }
                    }
                    catch (Exception ex)
                    {
                        testFailed(5, ex);
                    }
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
            fail("One or more threads failed");
    }

    public void testThreads() throws Exception
    {
        threadsBase();
    }

    public void testThreadsDelay() throws Exception
    {
        _threadDelay = 100;
        _times = 2;
        threadsBase();
    }

    public void testThreadsNoRedirect() throws Exception
    {
        java.net.HttpURLConnection.setFollowRedirects(false);
        threadsBase();
        java.net.HttpURLConnection.setFollowRedirects(true);
    }

    public void allTestMethods() throws Exception
    {
        testThreads();
    }

}
