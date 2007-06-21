package com.oaklandsw.http.local;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.Headers;
import com.oaklandsw.http.HttpException;
import com.oaklandsw.http.HttpTestBase;
import com.oaklandsw.http.HttpURLConnectInternal;
import com.oaklandsw.util.ExposedBufferInputStream;
import com.oaklandsw.util.LogUtils;
import com.oaklandsw.util.Util;

public class TestHeaders extends HttpTestBase
{
    private static final Log _log = LogUtils.makeLogger();

    public TestHeaders(String testName)
    {
        super(testName);
    }

    public static void main(String args[])
    {
        String[] testCaseName = { TestHeaders.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }

    public static Test suite()
    {
        return new TestSuite(TestHeaders.class);
    }

    protected void checkText(String text, Headers headers) throws IOException
    {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        headers.write(os);
        os.close();

        String outText = new String(os.toByteArray());
        assertTrue(outText.indexOf(text) >= 0);
    }

    protected void checkTextEquals(String text, Headers headers)
        throws IOException
    {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        headers.write(os);
        os.close();

        String outText = new String(os.toByteArray());
        // System.out.println(text);
        // System.out.println(outText);
        assertEquals(text, outText);
    }

    protected ExposedBufferInputStream getInputStream(String str)
    {
        return new ExposedBufferInputStream(new ByteArrayInputStream(str
                .getBytes()), 1000);
    }

    public void testBasic() throws Exception
    {
        Headers h = new Headers();
        h.add("one".getBytes(), "oneval".getBytes());
        h.add("two".getBytes(), "twoval".getBytes());
        Util.bytesEqual("oneval".getBytes(), h.get("one".getBytes()));
        checkText("twoval", h);
    }

    public void testSet() throws Exception
    {
        Headers h = new Headers();
        h.set("one".getBytes(), "oneval".getBytes());
        h.set("one".getBytes(), "onevalrevised".getBytes());
        Util.bytesEqual("onevalrevised".getBytes(), h.get("one".getBytes()));
        checkText("onevalrevised", h);
        checkTextEquals("one: onevalrevised\r\n", h);
    }

    public void testSetNull() throws Exception
    {
        Headers h = new Headers();
        try
        {
            h.set("one".getBytes(), null);
            fail("Expected exception for null value");
        }
        catch (IllegalArgumentException ex)
        {
            // Expected
        }
    }

    public void testAdd() throws Exception
    {
        Headers h = new Headers();
        h.add("one".getBytes(), "onevaloriginal".getBytes());
        h.add("one".getBytes(), "onevalnew".getBytes());
        h.add("one".getBytes(), "onevalrevised".getBytes());
        // Should get the most recent one
        Util.bytesEqual("onevalrevised".getBytes(), h.get("one".getBytes()));
        checkText("onevalrevised", h);
        checkText("onevalnew", h);
        checkText("onevaloriginal", h);

        h.add("one".getBytes(), "onevalnew2".getBytes());
        // Should get the most recent one
        Util.bytesEqual("onevalnew2".getBytes(), h.get("one".getBytes()));

        // Bug 1440 getHeaderFields() not implemented
        Map map = h.getMap();
        List list = (List)map.get("one");
        assertEquals(4, list.size());
        assertEquals("onevaloriginal", list.get(0));
        assertEquals("onevalnew", list.get(1));
        assertEquals("onevalrevised", list.get(2));
        assertEquals("onevalnew2", list.get(3));

        assertEquals(null, map.get("two"));

    }

    public void testAddIndex() throws Exception
    {
        Headers h = new Headers();
        h.add("one".getBytes(), "onevaloriginal".getBytes());
        h.add("one".getBytes(), "onevalnew".getBytes());
        h.add("one".getBytes(), "onevalrevised".getBytes());

        // Test by index
        Util.bytesEqual("onevaloriginal".getBytes(), h.get(0));
        Util.bytesEqual("onevalnew".getBytes(), h.get(1));
        Util.bytesEqual("onevalrevised".getBytes(), h.get(2));
        assertEquals(null, h.get(3));

        // Test by key
        Util.bytesEqual("one".getBytes(), h.getKey(0));
        Util.bytesEqual("one".getBytes(), h.getKey(1));
        Util.bytesEqual("one".getBytes(), h.getKey(2));
        assertEquals(null, h.getKey(3));
    }

    public void testSetIndex() throws Exception
    {
        Headers h = new Headers();
        h.set("One".getBytes(), "onevaloriginal".getBytes());
        h.set("one".getBytes(), "onevalnew".getBytes());
        h.set("One".getBytes(), "onevalrevised".getBytes());

        // Test by index - should only have the
        // last one
        Util.bytesEqual("onevalrevised".getBytes(), h.get(0));
        assertEquals(null, h.get(1));

        // Test by key
        Util.bytesEqual("One".getBytes(), h.getKey(0));
        assertEquals(null, h.getKey(1));
    }

    public void testRemove() throws Exception
    {
        Headers h = new Headers();
        h.add("one".getBytes(), "onevaloriginal".getBytes());
        h.remove("one".getBytes());
        assertEquals(null, h.get("one".getBytes()));

        h.add("one".getBytes(), "onevalnew".getBytes());
        h.add("one".getBytes(), "onevalrevised".getBytes());
        h.remove("one".getBytes());
        assertEquals(null, h.get("one".getBytes()));
    }

    public void testBig() throws Exception
    {
        Headers h = new Headers();
        for (int i = 0; i < 1000; i++)
        {
            h.add(("one" + i).getBytes(), ("oneval" + i).getBytes());
        }

        Util.bytesEqual("oneval1".getBytes(), h.get("one1".getBytes()));
        Util.bytesEqual("oneval200".getBytes(), h.get("one200".getBytes()));
        Util.bytesEqual("oneval999".getBytes(), h.get("one999".getBytes()));

        Util.bytesEqual("oneval1".getBytes(), h.get(1));
        Util.bytesEqual("oneval200".getBytes(), h.get(200));
        Util.bytesEqual("oneval999".getBytes(), h.get(999));

        Util.bytesEqual("one1".getBytes(), h.getKey(1));
        Util.bytesEqual("one200".getBytes(), h.getKey(200));
        Util.bytesEqual("one999".getBytes(), h.getKey(999));
    }

    public void testReadEmpty1() throws Exception
    {
        Headers h = new Headers();

        String str = "\n";
        h.read(getInputStream(str), new HttpURLConnectInternal(), true, 0);
    }

    public void testReadEmpty2() throws Exception
    {
        Headers h = new Headers();

        String str = "\n\n";
        h.read(getInputStream(str), new HttpURLConnectInternal(), true, 0);
    }

    public void testReadEmpty3() throws Exception
    {
        Headers h = new Headers();

        String str = "\r\n";
        h.read(getInputStream(str), new HttpURLConnectInternal());
    }

    public void testReadGood() throws Exception
    {
        Headers h = new Headers();

        String str = "head1: val1\r\n"
            + "head2:val2\r\n"
            + "head3 : val3\r\n"
            + "head4: val4   \r\n"
            + "\r\n";

        h.read(getInputStream(str), new HttpURLConnectInternal());

        Util.bytesEqual("val1".getBytes(), h.get(0));
        Util.bytesEqual("val1".getBytes(), h.get("head1".getBytes()));
        Util.bytesEqual("val2".getBytes(), h.get(1));
        Util.bytesEqual("val2".getBytes(), h.get("head2".getBytes()));
        Util.bytesEqual("val3".getBytes(), h.get(2));
        Util.bytesEqual("val3".getBytes(), h.get("head3".getBytes()));
        Util.bytesEqual("val4".getBytes(), h.get(3));
        Util.bytesEqual("val4".getBytes(), h.get("head4".getBytes()));
    }

    public void testReadGoodCont() throws Exception
    {
        Headers h = new Headers();

        String str = "head1: val1\r\n"
            + " more value 1\r\n"
            + "head2:val2\r\n"
            + "       more value 2\r\n"
            + "head3 : \r\n"
            + "head4: val4   \r\n"
            + "       more value 4\r\n"
            + "       still more value 4\r\n"
            + "\r\n";

        h.read(getInputStream(str), new HttpURLConnectInternal());

        Util.bytesEqual("val1 more value 1".getBytes(), h.get(0));
        Util.bytesEqual("val1 more value 1".getBytes(), h.get("head1"
                .getBytes()));
        Util.bytesEqual("val2 more value 2".getBytes(), h.get(1));
        Util.bytesEqual("val2 more value 2".getBytes(), h.get("head2"
                .getBytes()));
        assertEquals(null, h.get(2));
        assertEquals(null, h.get("head3".getBytes()));
        Util.bytesEqual("val4 more value 4 still more value 4".getBytes(), h
                .get(3));
        Util.bytesEqual("val4 more value 4 still more value 4".getBytes(), h
                .get("head4".getBytes()));
    }

    public void testReadGoodPermissive() throws Exception
    {
        Headers h = new Headers();

        String str = "head1: val1\n"
            + "head2:val2:val2a\r\n"
            + "head3 : val3\r\n"
            + "head4: val4   \n"
            + "head5: val5   \r\n"
            + "head6    :     val6    \r\n"
            + "head7 :     val7\t\r\n"
            + "head8 :     val8\t\t\r\n"
            + "head9 :     val9\t \t\r\n"
            + "head10 :     val10  \t \t  \r\n"
            + "\r\n";

        h.read(getInputStream(str), new HttpURLConnectInternal());

        Util.bytesEqual("val1".getBytes(), h.get(0));
        Util.bytesEqual("val1".getBytes(), h.get("head1".getBytes()));
        Util.bytesEqual("val2:val2a".getBytes(), h.get(1));
        Util.bytesEqual("val2:val2a".getBytes(), h.get("head2".getBytes()));
        Util.bytesEqual("val3".getBytes(), h.get(2));
        Util.bytesEqual("val3".getBytes(), h.get("head3".getBytes()));
        Util.bytesEqual("val4".getBytes(), h.get(3));
        Util.bytesEqual("val4".getBytes(), h.get("head4".getBytes()));
        Util.bytesEqual("val5".getBytes(), h.get("head5".getBytes()));
        Util.bytesEqual("val6".getBytes(), h.get("head6".getBytes()));
        Util.bytesEqual("val7".getBytes(), h.get("head7".getBytes()));
        Util.bytesEqual("val8".getBytes(), h.get("head8".getBytes()));
        Util.bytesEqual("val9".getBytes(), h.get("head9".getBytes()));
        Util.bytesEqual("val10".getBytes(), h.get("head10".getBytes()));
    }

    public void testReadGoodPermissive2() throws Exception
    {
        Headers h = new Headers();

        String str = "head1: val1\n" + "\n";

        h.read(getInputStream(str), new HttpURLConnectInternal());

        Util.bytesEqual("val1".getBytes(), h.get(0));
        Util.bytesEqual("val1".getBytes(), h.get("head1".getBytes()));
    }

    public void testReadBad() throws Exception
    {
        Headers h = new Headers();

        String str = "head1: val1\r\n"
            + "head2:val2\r\n"
            + "head3 : val3\r"
            + "head4: val4   \r\n"
            + "\r\n";

        try
        {
            h.read(getInputStream(str), new HttpURLConnectInternal());
        }
        catch (HttpException ex)
        {
            assertTrue("Wrong exception: " + ex.getMessage(), ex.getMessage()
                    .indexOf("LF after CR") >= 0);
        }
    }

    public void testCase() throws Exception
    {
        Headers h = new Headers();
        h.add("One".getBytes(), "onevaloriginal".getBytes());
        assertTrue(Util.bytesEqual("onevaloriginal".getBytes(), h.get("one"
                .getBytes())));
        checkText("onevaloriginal", h);
    }

}
