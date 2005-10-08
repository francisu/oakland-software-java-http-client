package com.oaklandsw.http.webapp;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.oaklandsw.http.TestEnv;
import com.oaklandsw.http.servlet.HeaderServlet;
import com.oaklandsw.log.Log;
import com.oaklandsw.log.LogFactory;
import com.oaklandsw.util.Util;

public class TestPerf
{

    private static final Log _log = LogFactory.getLog(TestPerf.class);

    public TestPerf()
    {
    }

    private static final int TEXT_TIMES = 1000;
    private static int       _textTimes = TEXT_TIMES;

    private static final int TIMES      = 1000;
    private static int       _times;

    private static boolean   _readText;
    private static boolean   _writeText;

    private static boolean   _warmUp    = true;

    private static boolean   _doClose;

    private static String    _urlBase   = TestEnv.TEST_URL_TOMCAT;
    private static String    _url       = _urlBase + HeaderServlet.NAME;

    public static void main(String args[]) throws Exception
    {
        _times = TIMES;

        for (int i = 0; i < args.length; i++)
        {
            if (args[i].equalsIgnoreCase("-times"))
            {
                try
                {
                    _times = Integer.parseInt(args[++i]);
                }
                catch (Exception e)
                {
                    _times = TIMES;
                }
            }
            else if (args[i].equalsIgnoreCase("-readtext"))
            {
                _readText = true;
            }
            else if (args[i].equalsIgnoreCase("-writetext"))
            {
                _writeText = true;
            }
            else if (args[i].equalsIgnoreCase("-texttimes"))
            {
                try
                {
                    _textTimes = Integer.parseInt(args[++i]);
                }
                catch (Exception e)
                {
                    _textTimes = TIMES;
                }
            }
            else if (args[i].equalsIgnoreCase("-webserver"))
            {
                _url = TestEnv.TEST_URL_WEBSERVER + "/";
            }
            else if (args[i].equalsIgnoreCase("-explicit"))
            {
                com.oaklandsw.http.HttpURLConnection.setExplicitClose(true);
            }
            else if (args[i].equalsIgnoreCase("-nowarmup"))
            {
                _warmUp = false;
            }
            else if (args[i].equalsIgnoreCase("-close"))
            {
                _doClose = true;
            }
//            else if (args[i].equalsIgnoreCase("-dnsjava"))
//            {
//                com.oaklandsw.http.HttpURLConnection.setUseDnsJava(true);
//            }
            else if (args[i].equalsIgnoreCase("-help"))
            {
                usage();
                return;
            }
            else
            {
                usage();
                return;
            }
        }

        // Do one to initialize everything but don't count that
        // in the timing
        if (_warmUp)
            doConnect();

        long startTime = System.currentTimeMillis();

        testGetMethod();

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        float transTime = (float)totalTime / (float)_times;

        System.out.println("Time: " + totalTime + " MS/trans: " + transTime);
    }

    private static void usage()
    {
        System.out
                .println("java [-Dsun] com.oaklandsw.http.webapp.TestPerf [options]");
        System.out.println("options: ");
        System.out.println("  -writetext - server writes text");
        System.out.println("  -readtext - does getInputStream() to read");
        System.out.println("  -times num - number of times (def 1000)");
        System.out.println("  -texttimes num - number of 36 byte text blocks");
        System.out.println("  -nowarm - include the initialization in timing");
        System.out.println("  -webserver - use localhost:80 instead of cat");
        System.out.println("  -close - explicitly close all connections");
        System.out.println("  -dnsjava - use dnsjava name resolver");
    }

    private static void doConnect() throws Exception
    {
        URL url = new URL(_url);

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("GET");
        if (_writeText)
        {
            urlCon.setRequestProperty("emit-text", String.valueOf(_textTimes));
        }

        if (urlCon.getResponseCode() != 200)
            throw new RuntimeException("Bad response code");

        if (_readText)
        {
            InputStream is = urlCon.getInputStream();
            Util.flushStream(is);
            is.close();
        }

        if (com.oaklandsw.http.HttpURLConnection.getExplicitClose())
            urlCon.getInputStream().close();
    }

    public static void testGetMethod() throws Exception
    {
        System.out.println("Times: " + _times);
        System.out.println("Text size: "
            + (_textTimes * HeaderServlet.TEXT_CONST.length()));
        for (int i = 0; i < _times; i++)
            doConnect();

        System.out.println("connection pool before close");
        com.oaklandsw.http.HttpURLConnection.dumpConnectionPool();
        if (_doClose)
        {
            com.oaklandsw.http.HttpURLConnection.closeAllPooledConnections();
            System.out.println("connection pool after close");
            com.oaklandsw.http.HttpURLConnection.dumpConnectionPool();
        }

    }

}
