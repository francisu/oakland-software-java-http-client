package com.oaklandsw.http.webapp;

import java.net.HttpURLConnection;
import java.net.URL;

import netx.jnlp.DefaultLaunchHandler;
import netx.jnlp.JNLPFile;
import netx.jnlp.LaunchHandler;
import netx.jnlp.Launcher;
import netx.jnlp.runtime.JNLPRuntime;

import org.apache.commons.logging.Log;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.util.LogUtils;

public class TestWebStart extends TestWebappBase
{

    private static final Log _log = LogUtils.makeLogger();

    public TestWebStart(String testName)
    {
        super(testName);
        _doExplicitTest = false;
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestWebStart.class);
        return suite;
    }

    public static void main(String args[])
    {
        mainRun(suite(), args);
    }

    public void testClassLoaderRecursion() throws Exception
    {
        ClassLoader cl = new ClassLoader()
        {
            public Class findClass(String name) throws ClassNotFoundException
            {
                try
                {
                    URL url = new URL("http://doesnot.matter");
                    HttpURLConnection urlCon = (HttpURLConnection)url
                            .openConnection();
                    urlCon.connect();
                }
                catch (Exception ex)
                {
                    fail("Got exception: " + ex);
                }

                return super.findClass(name);
            }
        };

        Thread.currentThread().setContextClassLoader(cl);
        doGetLikeMethod("GET", CHECK_CONTENT);
    }

    public void testUnsigned() throws Exception
    {
        URL url = new URL(_urlBase + "/http-unsigned-ws.jnlp");

        // Need to do this because of the way the JNLP code waits for
        // connections, it opens the connection and then downloads,
        // but does not close the connection until all of the downloads
        // have completed
        com.oaklandsw.http.HttpURLConnection.setMaxConnectionsPerHost(4);

        JNLPRuntime.setSecurityEnabled(true);
        //JNLPRuntime.setDebug(true);

        JNLPFile j = new JNLPFile(url);
        LaunchHandler lh = new DefaultLaunchHandler();

        Launcher l = new Launcher(lh);
        l.launch(j);
    }

    public void allTestMethods() throws Exception
    {
        testUnsigned();
    }

}
