package com.oaklandsw.http.local;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.oaklandsw.http.AutoRetryInputStream;
import com.oaklandsw.http.ChunkedInputStream;
import com.oaklandsw.http.ContentLengthInputStream;
import com.oaklandsw.http.SimpleHttpMethod;

public class TestStreams extends TestCase
{

    public TestStreams(String testName)
    {
        super(testName);
    }

    public void testChunkedInputStream() throws IOException
    {
        String correctInput = "10;key=\"value\r\nnewline\"\r\n1234567890123456\r\n5\r\n12345\r\n0\r\nFooter1: abcde\r\nFooter2: fghij\r\n";
        String correctResult = "123456789012345612345";
        SimpleHttpMethod method = new SimpleHttpMethod();

        // Test for when buffer is larger than chunk size
        InputStream in = new ChunkedInputStream(new ByteArrayInputStream(correctInput
                                                        .getBytes()),
                                                method,
                                                !AutoRetryInputStream.THROW_AUTO_RETRY);
        byte[] buffer = new byte[300];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int len;
        while ((len = in.read(buffer)) > 0)
        {
            out.write(buffer, 0, len);
        }
        String result = new String(out.toByteArray());
        assertEquals(result, correctResult);

        /***********************************************************************
         * Header footer = method.getResponseFooter("footer1");
         * assertEquals(footer.getValue(), "abcde"); footer =
         * method.getResponseFooter("footer2"); assertEquals(footer.getValue(),
         * "fghij");
         **********************************************************************/

        // Test for when buffer is smaller than chunk size.
        in = new ChunkedInputStream(new ByteArrayInputStream(correctInput
                .getBytes()), method, !AutoRetryInputStream.THROW_AUTO_RETRY);

        buffer = new byte[7];
        out = new ByteArrayOutputStream();
        while ((len = in.read(buffer)) > 0)
        {
            out.write(buffer, 0, len);
        }
        result = new String(out.toByteArray());
        assertEquals(result, correctResult);
        /***********************************************************************
         * not right now footer = method.getResponseFooter("footer1");
         * assertEquals(footer.getValue(), "abcde"); footer =
         * method.getResponseFooter("footer2"); assertEquals(footer.getValue(),
         * "fghij");
         **********************************************************************/
    }

    public void testCorruptChunkedInputStream1() throws IOException
    {
        // missing \r\n at the end of the first chunk
        String corrupInput = "10;key=\"value\"\r\n123456789012345\r\n5\r\n12345\r\n0\r\nFooter1: abcde\r\nFooter2: fghij\r\n";
        SimpleHttpMethod method = new SimpleHttpMethod();

        InputStream in = new ChunkedInputStream(new ByteArrayInputStream(corrupInput
                                                        .getBytes()),
                                                method,
                                                !AutoRetryInputStream.THROW_AUTO_RETRY);
        byte[] buffer = new byte[300];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int len;
        try
        {
            while ((len = in.read(buffer)) > 0)
            {
                out.write(buffer, 0, len);
            }
            fail("Should have thrown exception");
        }
        catch (IOException e)
        {
            /* expected exception */
        }
    }

    public void testContentLengthInputStream() throws IOException
    {
        String correct = "1234567890123456";
        InputStream in = new ContentLengthInputStream(new ByteArrayInputStream(correct
                                                              .getBytes()),
                                                      null,
                                                      10,
                                                      !AutoRetryInputStream.THROW_AUTO_RETRY);
        byte[] buffer = new byte[50];
        int len = in.read(buffer);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(buffer, 0, len);
        String result = new String(out.toByteArray());
        assertEquals(result, "1234567890");
    }

    // ------------------------------------------------------- TestCase Methods

    public static Test suite()
    {
        return new TestSuite(TestStreams.class);
    }

    // ------------------------------------------------------------------- Main
    public static void main(String args[])
    {
        String[] testCaseName = { TestStreams.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }
}
