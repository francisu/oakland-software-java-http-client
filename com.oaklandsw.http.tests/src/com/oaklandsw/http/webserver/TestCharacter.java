package com.oaklandsw.http.webserver;

import java.io.IOException;
import java.io.Reader;
import java.net.URL;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.HttpTestBase;
import com.oaklandsw.http.HttpTestEnv;
import com.oaklandsw.http.HttpURLConnection;

/**
 * Tests character I/O
 */
public class TestCharacter extends HttpTestBase
{

    public TestCharacter(String testName)
    {
        super(testName);
        _doProxyTest = true;
        _do10ProxyTest = true;
        _doExplicitTest = true;
    }

    public static void main(String args[])
    {
        mainRun(suite(), args);
    }

    public static Test suite()
    {
        return new TestSuite(TestCharacter.class);
    }

    static final int AGZIP    = 1;
    static final int ADEFLATE = 2;

    public void testRead(String file, int accept) throws IOException
    {
        URL url = new URL(HttpTestEnv.TEST_URL_WEBSERVER_DATA + file);

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        if (accept == ADEFLATE)
            urlCon.addRequestProperty("Accept-Encoding", "gzip");
        if (accept == AGZIP)
            urlCon.addRequestProperty("Accept-Encoding", "gzip");

        char cbuf[] = new char[1024];

        Reader r = urlCon.getReader();
        assertEquals(1024, r.read(cbuf));
        r.close();
        String str = new String(cbuf);

        assertEquals(1024, str.length());
        assertTrue(str.startsWith("12345"));

        if (accept == AGZIP)
            assertEquals("gzip", urlCon.getHeaderField("Content-Encoding"));
        if (accept == ADEFLATE)
            assertEquals("gzip", urlCon.getHeaderField("Content-Encoding"));
        
        checkNoActiveConns(url);
    }

    public void testReadNormal() throws IOException
    {
        testRead("1k", 0);
    }

    public void testReadGzipCompressed() throws IOException
    {
        testRead("1k", AGZIP);
    }

    public void testReadDeflateCompressed() throws IOException
    {
        // FIXME - Apparently Apache does not really use the "deflate" encoding
        // type, even with the DEFLATE filter set, it uses "gzip" encoding.
        // See the code above
        testRead("1k", ADEFLATE);
    }

    public void testReadEbcdic() throws IOException
    {
        URL url = new URL(HttpTestEnv.TEST_URL_WEBSERVER_DATA + "ebcdic.cp037");

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();

        char cbuf[] = new char[1024];

        Reader r = urlCon.getReader();
        r.read(cbuf);
        r.close();
        String str = new String(cbuf);

        // System.out.println(str);

        assertTrue(str.startsWith("69684558"));

        checkNoActiveConns(url);
    }

    public void allTestMethods() throws Exception
    {
        testReadNormal();
        // FIXME - this does not work properly because stupid apache
        // leaves off Content-Encoding after a few requests
        // testReadCompressed();
        testReadEbcdic();
    }

}
