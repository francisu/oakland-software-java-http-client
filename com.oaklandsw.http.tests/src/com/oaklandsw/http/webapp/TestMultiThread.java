package com.oaklandsw.http.webapp;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.apache.commons.logging.Log;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.servlet.HeaderServlet;
import com.oaklandsw.http.servlet.ParamServlet;
import com.oaklandsw.http.servlet.RedirectServlet;
import com.oaklandsw.util.LogUtils;

public class TestMultiThread extends TestWebappBase
{

    private static final Log   _log         = LogUtils.makeLogger();
    
    private static final int NUM_THREADS       = 50;

    private static final int TEXT_TIMES        = 100;

    private static final int MAX_OPEN_PER_HOST = 3;

    private boolean          _failed;

    private int              _threadDelay;

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

    public void tearDown() throws Exception
    {
        super.tearDown();
        _threadDelay = 0;
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

            HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
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
            if (!HttpURLConnection.getFollowRedirects() && response > 300)
            {
                if (com.oaklandsw.http.HttpURLConnection.getExplicitClose())
                    urlCon.getInputStream().close();
                return;
            }

            if (response != 200)
            {
                _log.debug(Thread.currentThread().getName()
                    + " failed status "
                    + response);
                _failed = true;
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
                _failed = true;
            }

            if (hasExtendedText)
            {
                localPassed = checkReplyNoAssert(reply, _textComp);
                if (!localPassed)
                {
                    _log.debug(Thread.currentThread().getName()
                        + " failed body text");
                    _failed = true;
                }
            }

            if (com.oaklandsw.http.HttpURLConnection.getExplicitClose())
                urlCon.getInputStream().close();

            _log.debug(Thread.currentThread().getName()
                + (_failed ? " FAILED " : " PASSED "));
        }
        catch (Exception ex)
        {
            _failed = true;
            ex.printStackTrace();
        }

    }

    public void testThreadsBase() throws Exception
    {

        Thread t;

        _failed = false;

        // Only set it if we are currently set to the default
        if (com.oaklandsw.http.HttpURLConnection.getMaxConnectionsPerHost() == 2)
        {
            com.oaklandsw.http.HttpURLConnection
                    .setMaxConnectionsPerHost(MAX_OPEN_PER_HOST);
        }

        ArrayList threads = new ArrayList();
        for (int i = 0; i < NUM_THREADS; i++)
        {
            t = new Thread()
            {
                public void run()
                {
                    try
                    {
                        Thread.currentThread().setName("TestMultiThread");
                        threadMethod(
                                     com.oaklandsw.http.HttpURLConnection.HTTP_METHOD_GET,
                                     HeaderServlet.NAME, "Header", true);

                        threadMethod(
                                     com.oaklandsw.http.HttpURLConnection.HTTP_METHOD_GET,
                                     RedirectServlet.NAME
                                         + "?to="
                                         + URLEncoder.encode("http://"
                                             + host
                                             + ":"
                                             + port
                                             + "/"
                                             + context
                                             + ParamServlet.NAME), "Param",
                                     false);
                        threadMethod(
                                     com.oaklandsw.http.HttpURLConnection.HTTP_METHOD_POST,
                                     HeaderServlet.NAME, "Header", true);
                    }
                    catch (Exception ex)
                    {
                        System.out.println("Unexpected exception: " + ex);
                        _failed = true;
                    }
                }
            };

            t.start();
            threads.add(t);
        }

        for (int i = 0; i < threads.size(); i++)
        {
            ((Thread)threads.get(i)).join(10000);
        }

        if (_failed)
            fail("One or more threads failed");
    }

    public void testThreads() throws Exception
    {
        testThreadsBase();
    }

    public void testThreadsDelay() throws Exception
    {
        _threadDelay = 500;
        testThreadsBase();
    }

    public void testThreadsNoRedirect() throws Exception
    {
        HttpURLConnection.setFollowRedirects(false);
        testThreadsBase();
        HttpURLConnection.setFollowRedirects(true);
    }

    public void allTestMethods() throws Exception
    {
        testThreads();
    }

}
