//
// Copyright 2002-3003, oakland software, all rights reserved.
//
// May not be used or redistributed without specific written
// permission from oakland software.
//

package com.oaklandsw.http;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;

import com.oaklandsw.util.ExposedBufferInputStream;
import com.oaklandsw.util.LogUtils;
import com.oaklandsw.util.Util;

/**
 * This class keeps track of the a set of headers.
 */
public class Headers
{
    private static final Log         _log              = LogUtils.makeLogger();

    private static final int         INIT_HEADER_COUNT = 20;

    // Header keys are always encoded in ASCII, the values might or might
    // not be ASCII

    // The keys/values to the headers stored in the order in which they
    // were set/added. Note that remove does not do anything with this.
    protected byte[][]               _headerKeys;

    // Lower case version of the key for comparison
    protected byte[][]               _headerKeysLc;

    protected byte[][]               _headerValues;

    protected int                    _currentIndex;

    // Used only to store the value of the header while it is being
    // read
    protected char[]                 _charBuf;

    protected HttpURLConnectInternal _urlCon;

    public Headers()
    {
        clear();
        _charBuf = new char[INIT_BUF_SIZE];
    }

    /**
     * Add a header with the specified key and value.
     */
    public final void add(byte[] key, byte[] value)
    {
        if (_log.isTraceEnabled())
            _log.trace("add: " + new String(key) + ": " + new String(value));

        if (value == null)
        {
            throw new IllegalArgumentException("Header value for key: "
                + key
                + " is null");
        }

        // Expand if necessary
        if (_currentIndex >= _headerKeys.length)
            grow();

        _headerKeys[_currentIndex] = key;
        _headerKeysLc[_currentIndex] = Util.bytesToLower(key);
        _headerValues[_currentIndex] = value;
        _currentIndex++;
    }

    /**
     * Set the specified header to the specified key and value.
     */
    public final void set(byte[] key, byte[] value)
    {
        if (_log.isTraceEnabled())
            _log.trace("set: " + new String(key) + ": " + new String(value));

        if (value == null)
        {
            throw new IllegalArgumentException("Header value for key: "
                + key
                + " is null");
        }

        // Replace if already exists
        int i = findIndex(key, _currentIndex - 1);
        if (i >= 0)
        {
            _headerValues[i] = value;
            return;
        }

        // Newly adding
        add(key, value);
    }

    // Returns the index of the specified key, or -1 if it does not exist
    private int findIndex(byte[] key, int start)
    {
        byte[] checkKey = Util.bytesToLower(key);

        // Set the one with the highest index, if it matches
        for (int i = start; i >= 0; i--)
        {
            if (_headerKeys[i] != null
                && Util.bytesEqual(_headerKeysLc[i], checkKey))
            {
                return i;
            }
        }
        return -1;
    }

    private final void grow()
    {
        byte temp[][] = new byte[_headerKeys.length * 2][];
        System.arraycopy(_headerKeys, 0, temp, 0, _headerKeys.length);
        _headerKeys = temp;

        temp = new byte[_headerKeysLc.length * 2][];
        System.arraycopy(_headerKeysLc, 0, temp, 0, _headerKeysLc.length);
        _headerKeysLc = temp;

        temp = new byte[_headerValues.length * 2][];
        System.arraycopy(_headerValues, 0, temp, 0, _headerValues.length);
        _headerValues = temp;
    }

    /**
     * Get the key of the header given the order in which it was set.
     */
    public final byte[] getKey(int order)
    {
        if (order >= _currentIndex)
            return null;
        return _headerKeys[order];
    }

    /**
     * Get the value of the header given the order in which it was set.
     */
    public final byte[] get(int order)
    {
        if (order >= _currentIndex)
            return null;
        return _headerValues[order];
    }

    /**
     * Return the value associated with the specified key. If there is more than
     * one value, the most recently added one is returned to be compatible with
     * the JDK.
     */
    public final byte[] get(byte[] key)
    {
        if (key == null)
            return null;

        int index = findIndex(key, _currentIndex - 1);
        if (index >= 0)
            return _headerValues[index];
        return null;
    }

    /**
     * Removes all headers with the specified key.
     */
    public final void remove(byte[] key)
    {
        if (_log.isTraceEnabled())
            _log.trace("remove: " + new String(key));

        int index = _currentIndex - 1;

        while ((index = findIndex(key, index)) >= 0)
        {
            _headerKeys[index] = null;
            _headerKeysLc[index] = null;
            _headerValues[index] = null;
        }
    }

    /**
     * Returns the length, which is essentially the highest index for which
     * values might be stored.
     */
    public final int length()
    {
        return _currentIndex;
    }

    public final void clear()
    {
        // Initial size
        _headerKeys = new byte[INIT_HEADER_COUNT][];
        _headerKeysLc = new byte[INIT_HEADER_COUNT][];
        _headerValues = new byte[INIT_HEADER_COUNT][];

        _currentIndex = 0;
    }

    private static final int INIT_BUF_SIZE  = 250;

    // States
    private static final int HEADER         = 0;
    private static final int AFTER_HEADER   = 1;
    private static final int VALUE          = 2;
    private static final int VALUE_CONTINUE = 3;

    public final void read(ExposedBufferInputStream is, HttpURLConnectInternal urlCon)
        throws IOException
    {
        read(is, urlCon, false, 0);
    }

    public final void read(ExposedBufferInputStream is,
                           HttpURLConnectInternal urlCon,
                           boolean singleEolChar,
                           int savedFirstChar) throws IOException
    {
        int ch = 0;
        int ind = 0;
        boolean atNewLine = true;
        boolean seenSpace = false;
        int state = HEADER;

        _urlCon = urlCon;
        byte[] buffer = is._buffer;

        while (true)
        {
            if (!singleEolChar || savedFirstChar == 0)
            {
                // See if we need to fill
                if (is._pos >= is._used)
                {
                    is.fill();
                    if (is._used == -1)
                        break;
                }
                ch = buffer[is._pos++];
            }
            else
            {
                // The first character was previously read
                ch = savedFirstChar;
                savedFirstChar = 0;
            }

            if (ch < 0)
                break;
            // System.out.println("main read: " + String.valueOf((char)ch));

            // Handles the case where there are no headers
            if ((ch == '\n' || ch == '\r') && state == HEADER)
                state = VALUE;

            switch (state)
            {
                case HEADER:
                    // Skip any white space
                    if (ch == ' ')
                        continue;
                    if (ch == ':')
                    {
                        if (_currentIndex >= _headerKeys.length)
                            grow();

                        // Copy directly from the char buf as the key is
                        // always
                        // ASCII
                        byte[] key = new byte[ind];
                        for (int i = 0; i < ind; i++)
                            key[i] = (byte)_charBuf[i];
                        _headerKeys[_currentIndex] = key;
                        _headerKeysLc[_currentIndex] = Util.bytesToLower(key);

                        // System.out.println("Added header key: "
                        // + _headerKeys[_currentIndex]);
                        ind = 0;
                        state = AFTER_HEADER;
                        continue;
                    }
                    break;

                case AFTER_HEADER:
                case VALUE_CONTINUE:
                    // Skip leading white space
                    if (ch == ' ' || ch == '\t')
                    {
                        atNewLine = false;
                        continue;
                    }

                    state = VALUE;
                    // Fall through to VALUE

                case VALUE:
                    if (ch == '\r' || ch == '\n')
                    {
                        // System.out.println("VLAUE - newline");

                        if (!singleEolChar)
                        {
                            if (ch == '\r')
                            {
                                // Read the next line terminator (if CRLF
                                // seq)
                                ch = is.read();
                                // Connection dropped - will retry
                                if (ch < 0)
                                    throw new IOException("Premature EOF reading headers");
                                if (ch != '\n')
                                {
                                    throw new HttpException("Expected LF after CR character in header: "
                                        + new String(_headerKeys[_currentIndex]));
                                }
                            }
                        }

                        // 2nd new line, we are done
                        if (atNewLine)
                        {
                            setValue(ind);
                            if (_log.isDebugEnabled())
                                dumpHeaders();
                            return;
                        }

                        atNewLine = true;
                        continue;
                    }

                    if (atNewLine)
                    {
                        // Continuation of value
                        if (ch == ' ' || ch == '\t')
                        {
                            state = VALUE_CONTINUE;
                            // Add the one white space
                            ch = ' ';
                            // Fall through - save character
                            break;
                        }

                        // End of value - start new header
                        setValue(ind);
                        ind = 0;
                        state = HEADER;
                        // Fall through - save character
                        break;
                    }
                    // Fall through - save character
                    break;
            }

            // Collapse consecutive spaces
            if (ch == ' ')
            {
                if (seenSpace)
                    continue;
                seenSpace = true;
            }
            else
            {
                seenSpace = false;
            }

            // Save character
            atNewLine = false;
            if (ind >= _charBuf.length)
                growCharBuf();
            _charBuf[ind++] = (char)ch;
        }
    }

    private void setValue(int ind)
    {
        // Save buffer as value
        if (ind > 0)
        {
            // Attempt to copy the value as ASCII
            byte[] value = new byte[ind];
            int i = 0;
            for (; i < ind; i++)
            {
                if (_charBuf[i] > 0x7f)
                    break;
                value[i] = (byte)_charBuf[i];
            }

            if (i != ind)
            {
                // The value is other than ASCII, convert it
                String strValue = String.copyValueOf(_charBuf, 0, ind).trim();
                value = strValue.getBytes();
            }
            else
            {
                // Trim
                int len = ind;
                int st = 0;

                while ((st < len) && (value[st] <= ' '))
                    st++;
                while ((st < len) && (value[len - 1] <= ' '))
                    len--;
                if ((st > 0) || (len < ind + 1))
                {
                    byte[] inValue = value;
                    value = new byte[len];
                    System.arraycopy(inValue, st, value, 0, len);
                }
            }

            _headerValues[_currentIndex] = value;

            // System.out.println("FINAL - Added header val: "
            // + _headerValues[_currentIndex]);
            _urlCon.getHeadersWeNeed(_headerKeysLc[_currentIndex],
                                     _headerValues[_currentIndex]);
        }
        _currentIndex++;
    }

    public void dumpHeaders()
    {
        for (int i = 0; i < _currentIndex; i++)
        {
            _log.debug(new String(_headerKeys[i])
                + " ("
                + new String(_headerKeysLc[i])
                + ") : "
                + new String(_headerValues[i]));
        }
    }

    // Returns K(String of key) V(List of String of value)
    public Map getMap()
    {
        if (_currentIndex == 0)
            return Collections.EMPTY_MAP;
        Map map = new HashMap(_currentIndex);
        for (int i = 0; i < _currentIndex; i++)
        {
            byte[] key = _headerKeys[i];
            List values = new ArrayList();

            // Get each value for the specified key
            for (int j = 0; j < _currentIndex; j++)
            {
                if (Util.bytesEqual(_headerKeys[j], key))
                    values.add(new String(_headerValues[j]));
            }
            map.put(new String(key), values);
        }
        return map;
    }

    private final void growCharBuf()
    {
        char[] newBuf = new char[_charBuf.length * 2];
        System.arraycopy(_charBuf, 0, newBuf, 0, _charBuf.length);
        _charBuf = newBuf;
    }

    private final void writeKeyValue(OutputStream os, byte[] key, byte[] value)
        throws IOException
    {
        if (_log.isTraceEnabled())
        {
            _log
                    .trace("writing: "
                        + new String(key)
                        + ": "
                        + new String(value));
        }
        os.write(key);
        os.write(Util.COLON_SPACE_BYTES);
        os.write(value);
        os.write(Util.CRLF_BYTES);
    }

    /**
     * Write to an output stream.
     */
    public final void write(OutputStream os) throws IOException
    {
        for (int i = 0; i < _currentIndex; i++)
        {
            if (_headerKeys[i] == null)
                continue;
            writeKeyValue(os, _headerKeys[i], _headerValues[i]);
        }
    }

}
