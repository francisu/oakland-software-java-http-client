/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oaklandsw.http.local;

import com.oaklandsw.http.Headers;
import com.oaklandsw.http.HttpException;
import com.oaklandsw.http.HttpTestBase;
import com.oaklandsw.http.HttpURLConnectInternal;

import com.oaklandsw.util.ExposedBufferInputStream;
import com.oaklandsw.utillog.Log;
import com.oaklandsw.util.LogUtils;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.util.List;
import java.util.Map;


public class TestHeaders extends HttpTestBase {
    private static final Log _log = LogUtils.makeLogger();

    public TestHeaders(String testName) {
        super(testName);
    }

    public static void main(String[] args) {
        String[] testCaseName = { TestHeaders.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }

    public static Test suite() {
        return new TestSuite(TestHeaders.class);
    }

    protected void checkText(String text, Headers headers)
        throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        headers.write(os);
        os.close();

        String outText = new String(os.toByteArray());
        assertTrue(outText.indexOf(text) >= 0);
    }

    protected void checkTextEquals(String text, Headers headers)
        throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        headers.write(os);
        os.close();

        String outText = new String(os.toByteArray());
        // System.out.println(text);
        // System.out.println(outText);
        assertEquals(text, outText);
    }

    protected ExposedBufferInputStream getInputStream(String str) {
        return new ExposedBufferInputStream(new ByteArrayInputStream(
                str.getBytes()), 1000);
    }

    public void testBasic() throws Exception {
        Headers h = new Headers();
        h.add("one", "oneval");
        h.add("two", "twoval");
        assertEquals("oneval", h.getAsString("one"));
        checkText("twoval", h);
    }

    public void testSet() throws Exception {
        Headers h = new Headers();
        h.set("one", "oneval");
        h.set("one", "onevalrevised");
        assertEquals("onevalrevised", h.getAsString("one"));
        checkText("onevalrevised", h);
        checkTextEquals("one: onevalrevised\r\n", h);
    }

    public void testSetNull() throws Exception {
        Headers h = new Headers();

        try {
            h.set("one", null);
            fail("Expected exception for null value");
        } catch (IllegalArgumentException ex) {
            // Expected
        }
    }

    public void testAdd() throws Exception {
        Headers h = new Headers();
        h.add("one", "onevaloriginal");
        h.add("one", "onevalnew");
        h.add("one", "onevalrevised");
        // Should get the most recent one
        assertEquals("onevalrevised", h.getAsString("one"));
        checkText("onevalrevised", h);
        checkText("onevalnew", h);
        checkText("onevaloriginal", h);

        h.add("one", "onevalnew2");
        // Should get the most recent one
        assertEquals("onevalnew2", h.getAsString("one"));

        // Bug 1440 getHeaderFields() not implemented
        Map map = h.getMap();
        List list = (List) map.get("one");
        assertEquals(4, list.size());
        assertEquals("onevaloriginal", list.get(0));
        assertEquals("onevalnew", list.get(1));
        assertEquals("onevalrevised", list.get(2));
        assertEquals("onevalnew2", list.get(3));

        assertEquals(null, map.get("two"));
    }

    public void testAddIndex() throws Exception {
        Headers h = new Headers();
        h.add("one", "onevaloriginal");
        h.add("one", "onevalnew");
        h.add("one", "onevalrevised");

        // Test by index
        assertEquals("onevaloriginal", h.getAsString(0));
        assertEquals("onevalnew", h.getAsString(1));
        assertEquals("onevalrevised", h.getAsString(2));
        assertEquals(null, h.getAsString(3));

        // Test by key
        assertEquals("one", h.getKeyAsString(0));
        assertEquals("one", h.getKeyAsString(1));
        assertEquals("one", h.getKeyAsString(2));
        assertEquals(null, h.getKeyAsString(3));
    }

    public void testSetIndex() throws Exception {
        Headers h = new Headers();
        h.set("One", "onevaloriginal");
        h.set("one", "onevalnew");
        h.set("One", "onevalrevised");

        // Test by index - should only have the
        // last one
        assertEquals("onevalrevised", h.getAsString(0));
        assertEquals(null, h.getAsString(1));

        // Test by key
        assertEquals("One", h.getKeyAsString(0));
        assertEquals(null, h.getKeyAsString(1));
    }

    public void testRemove() throws Exception {
        Headers h = new Headers();
        h.add("one", "onevaloriginal");
        h.remove("one");
        assertEquals(null, h.getAsString("one"));

        h.add("one", "onevalnew");
        h.add("one", "onevalrevised");
        h.remove("one");
        assertEquals(null, h.getAsString("one"));
    }

    public void testBig() throws Exception {
        Headers h = new Headers();

        for (int i = 0; i < 1000; i++) {
            h.add(("one" + i), ("oneval" + i));
        }

        assertEquals("oneval1", h.getAsString("one1"));
        assertEquals("oneval200", h.getAsString("one200"));
        assertEquals("oneval999", h.getAsString("one999"));

        assertEquals("oneval1", h.getAsString(1));
        assertEquals("oneval200", h.getAsString(200));
        assertEquals("oneval999", h.getAsString(999));

        assertEquals("one1", h.getKeyAsString(1));
        assertEquals("one200", h.getKeyAsString(200));
        assertEquals("one999", h.getKeyAsString(999));
    }

    public void testReadEmpty1() throws Exception {
        Headers h = new Headers();

        String str = "\n";
        h.read(getInputStream(str), new HttpURLConnectInternal(), true, 0);
    }

    public void testReadEmpty2() throws Exception {
        Headers h = new Headers();

        String str = "\n\n";
        h.read(getInputStream(str), new HttpURLConnectInternal(), true, 0);
    }

    public void testReadEmpty3() throws Exception {
        Headers h = new Headers();

        String str = "\r\n";
        h.read(getInputStream(str), new HttpURLConnectInternal());
    }

    public void testReadGood() throws Exception {
        Headers h = new Headers();

        String str = "head1: val1\r\n" + "head2:val2\r\n" + "head3 : val3\r\n" +
            "head4: val4   \r\n" + "head5: val5\t\r\n" + "head6:\tval6\t \r\n" +
            "head7: val7 \t \r\n" + "\r\n";

        h.read(getInputStream(str), new HttpURLConnectInternal());

        assertEquals("val1", h.getAsString(0));
        assertEquals("val1", h.getAsString("head1"));
        assertEquals("val2", h.getAsString(1));
        assertEquals("val2", h.getAsString("head2"));
        assertEquals("val3", h.getAsString(2));
        assertEquals("val3", h.getAsString("head3"));
        assertEquals("val4", h.getAsString(3));
        assertEquals("val4", h.getAsString("head4"));
        assertEquals("val5", h.getAsString(4));
        assertEquals("val5", h.getAsString("head5"));
        assertEquals("val6", h.getAsString(5));
        assertEquals("val6", h.getAsString("head6"));
        assertEquals("val7", h.getAsString(6));
        assertEquals("val7", h.getAsString("head7"));
    }

    public void testReadGoodCont() throws Exception {
        Headers h = new Headers();

        String str = "head1: val1\r\n" + " more value 1\r\n" +
            "head2:val2\r\n" + "       more value 2\r\n" + "head3 : \r\n" +
            "head4: val4   \r\n" + "       more value 4\r\n" +
            "       still more value 4\r\n" +
            "head5:  val with    lots of spaces    in it   \r\n" + "\r\n";

        h.read(getInputStream(str), new HttpURLConnectInternal());

        assertEquals("val1 more value 1", h.getAsString(0));
        assertEquals("val1 more value 1", h.getAsString("head1"));
        assertEquals("val2 more value 2", h.getAsString(1));
        assertEquals("val2 more value 2", h.getAsString("head2"));
        assertEquals(null, h.getAsString(2));
        assertEquals(null, h.getAsString("head3"));
        assertEquals("val4 more value 4 still more value 4", h.getAsString(3));
        assertEquals("val4 more value 4 still more value 4",
            h.getAsString("head4"));
        assertEquals("val with lots of spaces in it", h.getAsString(4));
        assertEquals("val with lots of spaces in it", h.getAsString("head5"));
    }

    public void testReadGoodPermissive() throws Exception {
        Headers h = new Headers();

        String str = "head1: val1\r\n" + "head2:val2:val2a\r\n" +
            "head3 : val3\r\n" + "head4: val4   \r\n" + "head5: val5   \r\n" +
            "head6    :     val6    \r\n" + "head7 :     val7\t\r\n" +
            "head8 :     val8\t\t\r\n" + "head9 :     val9\t \t\r\n" +
            "head10 :     val10  \t \t  \r\n" + "\r\n";

        h.read(getInputStream(str), new HttpURLConnectInternal());

        assertEquals("val1", h.getAsString(0));
        assertEquals("val1", h.getAsString("head1"));
        assertEquals("val2:val2a", h.getAsString(1));
        assertEquals("val2:val2a", h.getAsString("head2"));
        assertEquals("val3", h.getAsString(2));
        assertEquals("val3", h.getAsString("head3"));
        assertEquals("val4", h.getAsString(3));
        assertEquals("val4", h.getAsString("head4"));
        assertEquals("val5", h.getAsString("head5"));
        assertEquals("val6", h.getAsString("head6"));
        assertEquals("val7", h.getAsString("head7"));
        assertEquals("val8", h.getAsString("head8"));
        assertEquals("val9", h.getAsString("head9"));
        assertEquals("val10", h.getAsString("head10"));
    }

    public void testReadGoodPermissive2() throws Exception {
        Headers h = new Headers();

        String str = "head1: val1\n\n";

        h.read(getInputStream(str), new HttpURLConnectInternal(),
            Headers.SINGLE_EOL_CHAR, 0);

        assertEquals("val1", h.getAsString(0));
        assertEquals("val1", h.getAsString("head1"));
    }

    public void testReadGoodPermissive3() throws Exception {
        Headers h = new Headers();

        String str = "head1: val1\n" + "head2: val2   \r" + "head3: val3   \n" +
            "\n";

        h.read(getInputStream(str), new HttpURLConnectInternal(),
            Headers.SINGLE_EOL_CHAR, 0);

        assertEquals("val1", h.getAsString(0));
        assertEquals("val1", h.getAsString("head1"));
        assertEquals("val2", h.getAsString(1));
        assertEquals("val2", h.getAsString("head2"));
        assertEquals("val3", h.getAsString(2));
        assertEquals("val3", h.getAsString("head3"));
    }

    public void testReadBad() throws Exception {
        Headers h = new Headers();

        String str = "head1: val1\r\n" + "head2:val2\r\n" + "head3 : val3\r" +
            "head4: val4   \r\n" + "\r\n";

        try {
            h.read(getInputStream(str), new HttpURLConnectInternal());
        } catch (HttpException ex) {
            assertTrue("Wrong exception: " + ex.getMessage(),
                ex.getMessage().indexOf("'h'") >= 0);
        }
    }

    public void testCase() throws Exception {
        Headers h = new Headers();
        h.add("One", "onevaloriginal");
        assertEquals("onevaloriginal", h.getAsString("one"));
        checkText("onevaloriginal", h);
    }
}
