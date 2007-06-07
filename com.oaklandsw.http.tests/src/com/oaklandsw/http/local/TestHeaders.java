package com.oaklandsw.http.local;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringBufferInputStream;
import java.util.List;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.oaklandsw.http.Headers;
import com.oaklandsw.http.HttpException;
import com.oaklandsw.http.HttpURLConnectInternal;

public class TestHeaders extends TestCase
{

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

    public void testBasic() throws Exception
    {
        Headers h = new Headers();
        h.add("one", "oneval");
        h.add("two", "twoval");
        assertEquals("oneval", h.get("one"));
        checkText("twoval", h);
    }

    public void testSet() throws Exception
    {
        Headers h = new Headers();
        h.set("one", "oneval");
        h.set("one", "onevalrevised");
        assertEquals("onevalrevised", h.get("one"));
        checkText("onevalrevised", h);
        checkTextEquals("one: onevalrevised\r\n", h);
    }

    public void testSetNull() throws Exception
    {
        Headers h = new Headers();
        try
        {
            h.set("one", null);
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
        h.add("one", "onevaloriginal");
        h.add("one", "onevalnew");
        h.add("one", "onevalrevised");
        // Should get the most recent one
        assertEquals("onevalrevised", h.get("one"));
        checkText("onevalrevised", h);
        checkText("onevalnew", h);
        checkText("onevaloriginal", h);

        h.add("one", "onevalnew2");
        // Should get the most recent one
        assertEquals("onevalnew2", h.get("one"));

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
        h.add("one", "onevaloriginal");
        h.add("one", "onevalnew");
        h.add("one", "onevalrevised");

        // Test by index
        assertEquals("onevaloriginal", h.get(0));
        assertEquals("onevalnew", h.get(1));
        assertEquals("onevalrevised", h.get(2));
        assertEquals(null, h.get(3));

        // Test by key
        assertEquals("one", h.getKey(0));
        assertEquals("one", h.getKey(1));
        assertEquals("one", h.getKey(2));
        assertEquals(null, h.getKey(3));
    }

    public void testSetIndex() throws Exception
    {
        Headers h = new Headers();
        h.set("One", "onevaloriginal");
        h.set("one", "onevalnew");
        h.set("One", "onevalrevised");

        // Test by index - should only have the
        // last one
        assertEquals("onevalrevised", h.get(0));
        assertEquals(null, h.get(1));

        // Test by key
        assertEquals("One", h.getKey(0));
        assertEquals(null, h.getKey(1));
    }

    public void testRemove() throws Exception
    {
        Headers h = new Headers();
        h.add("one", "onevaloriginal");
        h.remove("one");
        assertEquals(null, h.get("one"));

        h.add("one", "onevalnew");
        h.add("one", "onevalrevised");
        h.remove("one");
        assertEquals(null, h.get("one"));
    }

    public void testBig() throws Exception
    {
        Headers h = new Headers();
        for (int i = 0; i < 1000; i++)
        {
            h.add("one" + i, "oneval" + i);
        }

        assertEquals("oneval1", h.get("one1"));
        assertEquals("oneval200", h.get("one200"));
        assertEquals("oneval999", h.get("one999"));

        assertEquals("oneval1", h.get(1));
        assertEquals("oneval200", h.get(200));
        assertEquals("oneval999", h.get(999));

        assertEquals("one1", h.getKey(1));
        assertEquals("one200", h.getKey(200));
        assertEquals("one999", h.getKey(999));
    }

    public void testReadEmpty1() throws Exception
    {
        Headers h = new Headers();

        String str = "\n";
        StringBufferInputStream is = new StringBufferInputStream(str);
        h.read(is, new HttpURLConnectInternal(), true, 0);
    }

    public void testReadEmpty2() throws Exception
    {
        Headers h = new Headers();

        String str = "\n\n";
        StringBufferInputStream is = new StringBufferInputStream(str);
        h.read(is, new HttpURLConnectInternal(), true, 0);
    }

    public void testReadEmpty3() throws Exception
    {
        Headers h = new Headers();

        String str = "\r\n";
        StringBufferInputStream is = new StringBufferInputStream(str);
        h.read(is, new HttpURLConnectInternal());
    }

    public void testReadGood() throws Exception
    {
        Headers h = new Headers();

        String str = "head1: val1\r\n"
            + "head2:val2\r\n"
            + "head3 : val3\r\n"
            + "head4: val4   \r\n"
            + "\r\n";

        StringBufferInputStream is = new StringBufferInputStream(str);
        h.read(is, new HttpURLConnectInternal());

        assertEquals("val1", h.get(0));
        assertEquals("val1", h.get("head1"));
        assertEquals("val2", h.get(1));
        assertEquals("val2", h.get("head2"));
        assertEquals("val3", h.get(2));
        assertEquals("val3", h.get("head3"));
        assertEquals("val4", h.get(3));
        assertEquals("val4", h.get("head4"));
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

        StringBufferInputStream is = new StringBufferInputStream(str);
        h.read(is, new HttpURLConnectInternal());

        assertEquals("val1 more value 1", h.get(0));
        assertEquals("val1 more value 1", h.get("head1"));
        assertEquals("val2 more value 2", h.get(1));
        assertEquals("val2 more value 2", h.get("head2"));
        assertEquals(null, h.get(2));
        assertEquals(null, h.get("head3"));
        assertEquals("val4 more value 4 still more value 4", h.get(3));
        assertEquals("val4 more value 4 still more value 4", h.get("head4"));
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

        StringBufferInputStream is = new StringBufferInputStream(str);
        h.read(is, new HttpURLConnectInternal());

        assertEquals("val1", h.get(0));
        assertEquals("val1", h.get("head1"));
        assertEquals("val2:val2a", h.get(1));
        assertEquals("val2:val2a", h.get("head2"));
        assertEquals("val3", h.get(2));
        assertEquals("val3", h.get("head3"));
        assertEquals("val4", h.get(3));
        assertEquals("val4", h.get("head4"));
        assertEquals("val5", h.get("head5"));
        assertEquals("val6", h.get("head6"));
        assertEquals("val7", h.get("head7"));
        assertEquals("val8", h.get("head8"));
        assertEquals("val9", h.get("head9"));
        assertEquals("val10", h.get("head10"));
    }

    public void testReadGoodPermissive2() throws Exception
    {
        Headers h = new Headers();

        String str = "head1: val1\n" + "\n";

        StringBufferInputStream is = new StringBufferInputStream(str);
        h.read(is, new HttpURLConnectInternal());

        assertEquals("val1", h.get(0));
        assertEquals("val1", h.get("head1"));
    }

    public void testReadBad() throws Exception
    {
        Headers h = new Headers();

        String str = "head1: val1\r\n"
            + "head2:val2\r\n"
            + "head3 : val3\r"
            + "head4: val4   \r\n"
            + "\r\n";

        StringBufferInputStream is = new StringBufferInputStream(str);
        try
        {
            h.read(is, new HttpURLConnectInternal());
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
        h.add("One", "onevaloriginal");
        assertEquals("onevaloriginal", h.get("one"));
        checkText("onevaloriginal", h);
    }

}
