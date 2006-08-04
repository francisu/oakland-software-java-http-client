//
// Copyright 2002-3003, oakland software, all rights reserved.
//
// May not be used or redistributed without specific written
// permission from oakland software.
//

package com.oaklandsw.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.oaklandsw.util.Util;

/**
 * This class keeps track of the a set of headers.
 */
public class Headers
{

    private Log                      _log      = LogFactory
                                                       .getLog(Headers.class);

    private static final int         INIT_SIZE = 10;

    // The keys/values to the headers stored in the order in which they
    // were set/added. Note that remove does not do anything with this.
    protected String[]               _headerKeys;
    protected String[]               _headerValues;

    protected int                    _currentIndex;

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
    public final void add(String key, String value)
    {
        if (_log.isTraceEnabled())
            _log.trace("add: " + key + ": " + value);

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
        _headerValues[_currentIndex] = value;
        _currentIndex++;
    }

    /**
     * Set the specified header to the specified key and value.
     */
    public final void set(String key, String value)
    {
        if (_log.isTraceEnabled())
            _log.trace("set: " + key + ": " + value);

        if (value == null)
        {
            throw new IllegalArgumentException("Header value for key: "
                + key
                + " is null");
        }

        // Set the one with the highest index, if it matches
        for (int i = _currentIndex - 1; i >= 0; i--)
        {
            if (_headerKeys[i] != null && _headerKeys[i].equalsIgnoreCase(key))
            {
                _headerValues[i] = value;
                return;
            }
        }

        // Newly adding
        add(key, value);
    }

    private final void grow()
    {
        String temp[] = new String[_headerKeys.length * 2];
        System.arraycopy(_headerKeys, 0, temp, 0, _headerKeys.length);
        _headerKeys = temp;

        temp = new String[_headerValues.length * 2];
        System.arraycopy(_headerValues, 0, temp, 0, _headerValues.length);
        _headerValues = temp;
    }

    /**
     * Get the key of the header given the order in which it was set.
     */
    public final String getKey(int order)
    {
        if (order >= _currentIndex)
            return null;
        return _headerKeys[order];
    }

    /**
     * Get the value of the header given the order in which it was set.
     */
    public final String get(int order)
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
    public final String get(String key)
    {
        if (key == null)
            return null;

        // Get the one with the highest index
        for (int i = _currentIndex - 1; i >= 0; i--)
        {
            if (_headerKeys[i] != null && _headerKeys[i].equalsIgnoreCase(key))
                return _headerValues[i];
        }
        return null;
    }

    /**
     * Remove the header with the specified key.
     */
    public final void remove(String key)
    {
        if (_log.isTraceEnabled())
            _log.trace("remove: " + key);

        for (int i = 0; i < _currentIndex; i++)
        {
            if (_headerKeys[i] != null && _headerKeys[i].equalsIgnoreCase(key))
            {
                _headerKeys[i] = null;
                _headerValues[i] = null;
            }
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
        _headerKeys = new String[INIT_SIZE];
        _headerValues = new String[INIT_SIZE];

        _currentIndex = 0;
    }

    private static final int INIT_BUF_SIZE  = 20;

    // States
    private static final int HEADER         = 0;
    private static final int AFTER_HEADER   = 1;
    private static final int VALUE          = 2;
    private static final int VALUE_CONTINUE = 3;

    public final void read(InputStream is, HttpURLConnectInternal urlCon)
        throws IOException
    {
        read(is, urlCon, false, 0);
    }

    public final void read(InputStream is,
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

        while (true)
        {
            if (!singleEolChar || savedFirstChar == 0)
            {
                ch = is.read();
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
                        _headerKeys[_currentIndex] = String
                                .copyValueOf(_charBuf, 0, ind);
                        // System.out.println("Added header key: "
                        // + _headerKeys[_currentIndex]);
                        ind = 0;
                        state = AFTER_HEADER;
                        continue;
                    }

                    // Fall through - save character
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
                                // Read the next line terminator (if CRLF seq)
                                ch = is.read();
                                // Connection dropped - will retry
                                if (ch < 0)
                                    throw new IOException("Premature EOF reading headers");
                                if (ch != '\n')
                                {
                                    throw new HttpException("Expected LF after CR character in header: "
                                        + _headerKeys[_currentIndex]);
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
            _headerValues[_currentIndex] = String.copyValueOf(_charBuf, 0, ind)
                    .trim();
            // System.out.println("FINAL - Added header val: "
            // + _headerValues[_currentIndex]);
            _urlCon.getHeadersWeNeed(_headerKeys[_currentIndex],
                                     _headerValues[_currentIndex]);
        }
        _currentIndex++;
    }

    private void dumpHeaders()
    {
        for (int i = 0; i < _currentIndex; i++)
        {
            _log.debug(_headerKeys[i] + ": " + _headerValues[i]);
        }
    }

    public Map getMap()
    {
        if (_currentIndex == 0)
            return Collections.EMPTY_MAP;
        Map map = new HashMap(_currentIndex);
        for (int i = 0; i < _currentIndex; i++)
        {
            String key = _headerKeys[i];
            List values = new ArrayList();

            // Get each value for the specified key
            for (int j = 0; j < _currentIndex; j++)
            {
                if (_headerKeys[j].equals(key))
                    values.add(_headerValues[j]);
            }
            map.put(key, values);
        }
        return map;
    }

    private final void growCharBuf()
    {
        char[] newBuf = new char[_charBuf.length * 2];
        System.arraycopy(_charBuf, 0, newBuf, 0, _charBuf.length);
        _charBuf = newBuf;
    }

    private final void writeKeyValue(OutputStream os, String key, String value)
        throws IOException
    {
        if (_log.isTraceEnabled())
            _log.trace("writing: " + key + ": " + value);
        os.write(key.getBytes());
        os.write(Util.COLON_SPACE_BYTES);
        os.write(value.getBytes());
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
