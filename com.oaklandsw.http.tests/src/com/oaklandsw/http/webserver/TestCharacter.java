package com.oaklandsw.http.webserver;

import com.oaklandsw.http.HttpTestBase;
import com.oaklandsw.http.HttpTestEnv;
import com.oaklandsw.http.HttpURLConnection;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.IOException;
import java.io.Reader;

import java.net.URL;


/**
 * Tests character I/O
 */
public class TestCharacter extends HttpTestBase {
    static final int AGZIP = 1;
    static final int ADEFLATE = 2;

    public TestCharacter(String testName) {
        super(testName);
        _doProxyTest = true;
        _do10ProxyTest = true;
        _doExplicitTest = true;
    }

    public static void main(String[] args) {
        mainRun(suite(), args);
    }

    public static Test suite() {
        return new TestSuite(TestCharacter.class);
    }

    public void testRead(String file, int accept) throws IOException {
        URL url = new URL(HttpTestEnv.TEST_URL_WEBSERVER_DATA + file);

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);

        if (accept == ADEFLATE) {
            urlCon.addRequestProperty("Accept-Encoding", "gzip");
        }

        if (accept == AGZIP) {
            urlCon.addRequestProperty("Accept-Encoding", "gzip");
        }

        char[] cbuf = new char[1024];

        Reader r = urlCon.getReader();
        assertEquals(1024, r.read(cbuf));
        r.close();

        String str = new String(cbuf);

        assertEquals(1024, str.length());
        assertTrue(str.startsWith("12345"));

        if (accept == AGZIP) {
            assertEquals("gzip", urlCon.getHeaderField("Content-Encoding"));
        }

        if (accept == ADEFLATE) {
            assertEquals("gzip", urlCon.getHeaderField("Content-Encoding"));
        }

        checkNoActiveConns(url);
    }

    public void testReadNormal() throws IOException {
        testRead("1k", 0);
    }

    public void testReadGzipCompressed() throws IOException {
        testRead("1k", AGZIP);
    }

    public void testReadDeflateCompressed() throws IOException {
        // FIXME - Apparently Apache does not really use the "deflate" encoding
        // type, even with the DEFLATE filter set, it uses "gzip" encoding.
        // See the code above
        testRead("1k", ADEFLATE);
    }

    /*
     * OK for some readon the AddCharset IBM037 .cp037 does not work for this
     * If you set the AddDefaultCharset IBM037 this works fine.  This seems like a bug
     * in apache, but I can't find any bug report.  Turning this off for now.  24 Sep 09 FRU
     */
    public void XXtestReadEbcdic() throws IOException {
        URL url = new URL(HttpTestEnv.TEST_URL_WEBSERVER_DATA + "ebcdic.cp037");

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.addRequestProperty("Accept-Charset", "IBM037");

        char[] cbuf = new char[1024];

        Reader r = urlCon.getReader();
        r.read(cbuf);
        r.close();

        String str = new String(cbuf);

        // System.out.println(str);
        assertTrue(str.startsWith("69684558"));

        checkNoActiveConns(url);
    }

    public void allTestMethods() throws Exception {
        testReadNormal();
        // FIXME - this does not work properly because stupid apache
        // leaves off Content-Encoding after a few requests
        // Seems to work OK 24 Sep 09 FRU
        testReadGzipCompressed();

        //testReadEbcdic();
    }
}
