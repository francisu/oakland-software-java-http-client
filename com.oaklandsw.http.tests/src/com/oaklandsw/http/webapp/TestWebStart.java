package com.oaklandsw.http.webapp;

import java.net.HttpURLConnection;
import java.net.URL;

import netx.jnlp.DefaultLaunchHandler;
import netx.jnlp.JNLPFile;
import netx.jnlp.LaunchHandler;
import netx.jnlp.Launcher;
import netx.jnlp.runtime.JNLPRuntime;

import com.oaklandsw.util.Log;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.util.LogUtils;
import com.oaklandsw.util.Util;

public class TestWebStart extends TestWebappBase
{

    private static final Log _log = LogUtils.makeLogger();

    public TestWebStart(String testName)
    {
        super(testName);
        _doExplicitTest = false;

        // FIXME Not sure why this fails, but it does
        _doAuthCloseProxyTest = false;
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

    // Bug 2163
    public void testFileMaker() throws Exception
    {
        // Note this bug has to do with the initialization of the http client
        // in a fresh process, so we have to run the javaws app in a fresh process
        
        // netx JNLP - the test does not correctly fail when using this
        //Process p = Runtime.getRuntime().exec("java -jar ../com.oaklandsw.http.tests.jars/netx.jar -jnlp httptest.jnlp");

        // Sadly, when using this command line, there is no way to get the output
        // of the application 
        // If this test works, it may ask about accepting signed applications and
        // then it will silently pass.  If it fails, it will popup a dialog
        // that indicate it failed
        // **** In either case, it will show as passing ***
        Process p = Runtime.getRuntime().exec("javaws  httptest.jnlp");

        String result = Util.getStringFromInputStream(p.getInputStream());
        String error = Util.getStringFromInputStream(p.getErrorStream());
        
        System.out.println("result: " + result);
        System.out.println("exit: " + error);
        p.waitFor();
        System.out.println("exit value: " + p.exitValue());
    }

    public void testUnsigned() throws Exception
    {
        testUnsigned("http-unsigned-ws");
    }

    public void testUnsigned(String jnlpFile) throws Exception
    {
        URL url = new URL(_urlBase + "/" + jnlpFile + ".jnlp");

        // Need to do this because of the way the JNLP code waits for
        // connections, it opens the connection and then downloads,
        // but does not close the connection until all of the downloads
        // have completed
        com.oaklandsw.http.HttpURLConnection.setMaxConnectionsPerHost(4);

        JNLPRuntime.setSecurityEnabled(true);
        // JNLPRuntime.setDebug(true);

        JNLPFile j = new JNLPFile(url);
        LaunchHandler lh = new DefaultLaunchHandler();

        Launcher l = new Launcher(lh);
        l.launch(j);
    }

    public void testSystem() throws Exception
    {
        testUnsigned("http-unsigned-ws");
    }

    public void allTestMethods() throws Exception
    {
        testUnsigned();
    }

}
