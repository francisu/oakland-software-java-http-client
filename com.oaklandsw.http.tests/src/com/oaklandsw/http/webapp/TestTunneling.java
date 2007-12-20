package com.oaklandsw.http.webapp;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import org.apache.commons.logging.Log;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.HttpTestEnv;
import com.oaklandsw.http.HttpURLConnection;
import com.oaklandsw.util.LogUtils;
import com.oaklandsw.util.Util;

public class TestTunneling extends TestWebappBase
{

    private static final Log _log = LogUtils.makeLogger();

    public TestTunneling(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestTunneling.class);
        return suite;
    }

    public static void main(String args[])
    {
        mainRun(suite(), args);
    }

    // FIXME - more tests are necessary for this

    // Test using the connect method and tunneled streaming mode
    public void testTunnelConnect() throws Exception
    {
        if (!_in10ProxyTest)
            return;

        URL url = new URL("http://" + HttpTestEnv.FTP_HOST + ":21");
        int response = 0;

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("CONNECT");
        urlCon.setDoOutput(true);
        urlCon.setDoInput(true);
        urlCon.setTunneledStreamingMode(true);

        OutputStream outStr = urlCon.getOutputStream();

        response = urlCon.getResponseCode();
        assertEquals(200, response);

        InputStream is = urlCon.getInputStream();

        byte[] output;
        output = "USER anonymous\n".getBytes("ASCII");
        outStr.write(output);
        output = "PASS anonuser\n".getBytes("ASCII");
        outStr.write(output);
        output = "STAT\n".getBytes("ASCII");
        outStr.write(output);
        output = "QUIT\n".getBytes("ASCII");
        outStr.write(output);
        outStr.flush();
        outStr.close();

        byte[] bytes = Util.getBytesFromInputStream(is);
        String res = new String(bytes);
        // System.out.println(res);
        assertTrue(res.contains("221 Goodbye."));
    }

    // Test using the connect method and tunneled streaming mode
    public void testTunnelMethodCheck() throws Exception
    {
        if (!_in10ProxyTest)
            return;

        URL url = new URL("http://" + HttpTestEnv.FTP_HOST + ":21");

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("GET");
        urlCon.setDoOutput(true);
        urlCon.setDoInput(true);
        urlCon.setTunneledStreamingMode(true);

        try
        {
            urlCon.getOutputStream();
            fail("Did not get expected exception");
        }
        catch (IllegalStateException ex)
        {
            // Expected
        }
    }

    public void allTestMethods() throws Exception
    {
        testTunnelConnect();
        testTunnelMethodCheck();
    }

}