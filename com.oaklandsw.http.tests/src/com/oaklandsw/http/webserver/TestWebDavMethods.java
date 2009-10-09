package com.oaklandsw.http.webserver;

import com.oaklandsw.http.HttpTestBase;
import com.oaklandsw.http.HttpTestEnv;
import com.oaklandsw.http.HttpURLConnection;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.IOException;
import java.io.OutputStream;

import java.net.URL;


/**
 * Test webdav methods
 */
public class TestWebDavMethods extends HttpTestBase {
    private static String _host = HttpTestEnv.TEST_WEBDAV_HOST;
    private static int _port = HttpTestEnv.WEBDAV_PORT;

    public TestWebDavMethods(String testName) {
        super(testName);
        _doProxyTest = true;
        _do10ProxyTest = true;
        _doExplicitTest = true;
    }

    public static void main(String[] args) {
        mainRun(suite(), args);
    }

    public static Test suite() {
        return new TestSuite(TestWebDavMethods.class);
    }

    public void testMkCol() throws IOException {
        URL collectionUrl = new URL("http://" + _host + ":" + _port +
                "/dav/newcol/");
        URL resourceUrl = new URL("http://" + _host + ":" + _port +
                "/dav/newcol/resource");
        URL movedUrl = new URL("http://" + _host + ":" + _port +
                "/dav/newcol/resource1");
        int response = 0;
        String resourceContent = "This is a resource";
        String data;
        String msg;
        String lockToken;
        OutputStream outStr;

        HttpURLConnection urlCon;

        // Make sure we delete it the first time in case for some reason it's
        // there, don't care about the result
        urlCon = HttpURLConnection.openConnection(collectionUrl);
        urlCon.setRequestMethod("DELETE");
        urlCon.connect();
        response = urlCon.getResponseCode();

        // Create the collection
        urlCon = HttpURLConnection.openConnection(collectionUrl);
        urlCon.setRequestMethod("MKCOL");
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(201, response);

        data = HttpTestBase.getReply(urlCon);
        assertTrue("No data returned.", (data.length() > 0));
        assertTrue((data.indexOf("created") > 0));

        // Create a resource
        urlCon = HttpURLConnection.openConnection(resourceUrl);
        urlCon.setRequestMethod("PUT");
        urlCon.setDoOutput(true);
        outStr = urlCon.getOutputStream();
        msg = resourceContent;
        outStr.write(msg.getBytes());
        outStr.close();
        response = urlCon.getResponseCode();
        assertEquals(201, response);

        data = HttpTestBase.getReply(urlCon);
        assertTrue("No data returned.", (data.length() > 0));
        assertTrue((data.indexOf("created") > 0));

        // Set a property
        urlCon = HttpURLConnection.openConnection(resourceUrl);
        urlCon.setRequestMethod("PROPPATCH");
        urlCon.setDoOutput(true);
        outStr = urlCon.getOutputStream();
        msg = "<?xml version='1.0' ?>" +
            "<D:propertyupdate xmlns:D='DAV:' xmlns:Z='http://www.w3.com/standards/z39.50/'>" +
            " <D:set>" + "<D:prop>" + "<Z:authors>" +
            "<Z:Author>Jim Whitehead</Z:Author>" + "</Z:authors>" +
            "</D:prop>" + "</D:set>" + "</D:propertyupdate>";
        outStr.write(msg.getBytes());
        outStr.close();
        response = urlCon.getResponseCode();
        assertEquals(207, response);

        data = HttpTestBase.getReply(urlCon);
        assertTrue("No data returned.", (data.length() > 0));
        assertTrue((data.indexOf("200 OK") >= 0));

        // Get a property
        urlCon = HttpURLConnection.openConnection(resourceUrl);
        urlCon.setRequestMethod("PROPFIND");
        urlCon.setDoOutput(true);
        outStr = urlCon.getOutputStream();
        msg = "<?xml version='1.0' ?>" +
            "<D:propfind xmlns:D='DAV:' xmlns:Z='http://www.w3.com/standards/z39.50/'>" +
            "<D:prop>" + "<Z:authors/>" + "</D:prop>" + "</D:propfind>";
        outStr.write(msg.getBytes());
        outStr.close();
        response = urlCon.getResponseCode();
        assertEquals(207, response);

        data = HttpTestBase.getReply(urlCon);
        assertTrue("No data returned.", (data.length() > 0));
        assertTrue((data.indexOf("Jim Whitehead") >= 0));

        // Move the resource
        urlCon = HttpURLConnection.openConnection(resourceUrl);
        urlCon.setRequestMethod("MOVE");
        urlCon.setRequestProperty("Destination", movedUrl.toString());
        response = urlCon.getResponseCode();
        assertEquals(201, response);

        data = HttpTestBase.getReply(urlCon);
        assertTrue("No data returned.", (data.length() > 0));

        // Lock the moved resource
        urlCon = HttpURLConnection.openConnection(movedUrl);
        urlCon.setRequestMethod("LOCK");
        urlCon.setRequestProperty("Depth", "0");
        msg = "<?xml version='1.0' ?>" + "<lockinfo xmlns='DAV:'>" +
            "<lockscope><exclusive/></lockscope>" +
            "<locktype><write/></locktype>" +
            "<owner><href>http://oaklandsoftware.com</href></owner>" +
            "</lockinfo>";
        urlCon.setDoOutput(true);
        outStr = urlCon.getOutputStream();
        outStr.write(msg.getBytes());
        outStr.close();
        response = urlCon.getResponseCode();
        assertEquals(200, response);
        lockToken = urlCon.getHeaderField("Lock-Token");
        data = HttpTestBase.getReply(urlCon);
        assertTrue("No data returned.", (data.length() > 0));

        // Get the moved resource
        urlCon = HttpURLConnection.openConnection(movedUrl);
        urlCon.setRequestMethod("GET");
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        data = HttpTestBase.getReply(urlCon);
        assertTrue("No data returned.", (data.length() > 0));
        assertTrue((data.indexOf(resourceContent) >= 0));

        // Unlock the moved resource
        urlCon = HttpURLConnection.openConnection(movedUrl);
        urlCon.setRequestMethod("UNLOCK");
        urlCon.setRequestProperty("Lock-Token", lockToken);
        response = urlCon.getResponseCode();
        assertEquals(204, response);

        data = HttpTestBase.getReply(urlCon);
        assertTrue("Data returned.", (data.length() == 0));

        // Get the original resource - should not be found
        urlCon = HttpURLConnection.openConnection(resourceUrl);
        urlCon.setRequestMethod("GET");
        urlCon.setRequestProperty("Cache-Control", "no-cache");
        response = urlCon.getResponseCode();
        assertEquals(404, response);

        data = HttpTestBase.getReply(urlCon);
        assertTrue("No data returned.", (data.length() > 0));

        // Copy back to original
        urlCon = HttpURLConnection.openConnection(movedUrl);
        urlCon.setRequestMethod("COPY");
        urlCon.setRequestProperty("Destination", resourceUrl.toString());
        response = urlCon.getResponseCode();
        assertEquals(201, response);

        data = HttpTestBase.getReply(urlCon);
        assertTrue("No data returned.", (data.length() > 0));

        // Get the original resource - should be there
        urlCon = HttpURLConnection.openConnection(resourceUrl);
        urlCon.setRequestMethod("GET");
        response = urlCon.getResponseCode();
        assertEquals(200, response);

        data = HttpTestBase.getReply(urlCon);
        assertTrue("No data returned.", (data.length() > 0));
        assertTrue((data.indexOf(resourceContent) >= 0));

        // Delete the collection
        urlCon = HttpURLConnection.openConnection(collectionUrl);
        urlCon.setRequestMethod("DELETE");
        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(204, response);

        data = HttpTestBase.getReply(urlCon);
        assertTrue("Unexpected data returned.", (data.length() == 0));

        checkNoActiveConns(collectionUrl);
    }

    public void testPropfind() throws IOException {
        URL url = new URL("http://" + _host + ":" + _port + "/svn/main");
        int response = 0;

        HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
        urlCon.setRequestMethod("PROPFIND");
        urlCon.setRequestProperty("Depth", "1");

        urlCon.setDoOutput(true);

        OutputStream outStr = urlCon.getOutputStream();
        String msg = "<?xml version='1.0' ?>" + "<propfind xmlns='DAV:'>" +
            "<propname/>" + "</propfind>";
        outStr.write(msg.getBytes());
        outStr.close();

        urlCon.connect();
        response = urlCon.getResponseCode();
        assertEquals(207, response);

        String data = HttpTestBase.getReply(urlCon);
        assertTrue("No data returned.", (data.length() > 0));
        assertTrue((data.indexOf("multistatus") > 0));

        checkNoActiveConns(url);
    }

    public void allTestMethods() throws Exception {
        testMkCol();
        testPropfind();
    }
}
