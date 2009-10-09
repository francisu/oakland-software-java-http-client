//
// Copyright 2002-3003, oakland software, all rights reserved.
//
// May not be used or redistributed without specific written
// permission from oakland software.
//

package com.oaklandsw.http;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.oaklandsw.util.Log;

import com.oaklandsw.util.ExposedBufferInputStream;
import com.oaklandsw.util.LogUtils;
import com.oaklandsw.util.Util;

/**
 * This class keeps track of the a set of headers.
 */
public class Headers {
	private static final Log _log = LogUtils.makeLogger();

	private static final int INIT_HEADER_COUNT = 20;

	private static final int INIT_BUF_SIZE = 100;

	// Header keys are always encoded in ASCII, the values might or might
	// not be ASCII

	// The keys/values to the headers stored in the order in which they
	// were set/added. Note that remove does not do anything with this.
	protected byte[][] _headerKeys;

	// The actual length of the key, it is not the length of the byte array
	protected int[] _headerKeysLengths;

	// Lower case version of the key for comparison
	protected byte[][] _headerKeysLc;

	protected byte[][] _headerValues;

	// The actual length of the value, is it not the length of the byte array
	protected int[] _headerValuesLengths;

	protected int _currentIndex;

	// Used only to store the value of the header while it is being
	// read
	protected char[] _charBuf;

	protected HttpURLConnectInternal _urlCon;

	public Headers() {
		clear();
		_charBuf = new char[INIT_BUF_SIZE];
	}

	/**
	 * Add a header with the specified key and value.
	 */
	public final void add(String key, String value) {
		if (key == null || value == null)
			throw new IllegalArgumentException("Key or value null");

		add(key.getBytes(), value.getBytes());
	}

	/**
	 * Add a header with the specified key and value.
	 */
	public final void add(byte[] key, byte[] value) {
		if (_log.isTraceEnabled())
			_log.trace("add: " + new String(key) + ": " + new String(value));

		if (value == null) {
			throw new IllegalArgumentException("Header value for key: " + key
					+ " is null");
		}

		// Expand if necessary
		if (_currentIndex >= _headerKeys.length)
			grow();

		_headerKeys[_currentIndex] = key;
		_headerKeysLengths[_currentIndex] = key.length;
		_headerKeysLc[_currentIndex] = Util.bytesToLower(key);
		_headerValues[_currentIndex] = value;
		_headerValuesLengths[_currentIndex] = value.length;
		_currentIndex++;
	}

	/**
	 * Set the specified header to the specified key and value.
	 */
	public final void set(String key, String value) {
		if (key == null || value == null)
			throw new IllegalArgumentException("Key or value null");
		set(key.getBytes(), value.getBytes());
	}

	/**
	 * Set the specified header to the specified key and value.
	 */
	public final void set(byte[] key, byte[] value) {
		if (_log.isTraceEnabled())
			_log.trace("set: " + new String(key) + ": " + new String(value));

		if (value == null) {
			throw new IllegalArgumentException("Header value for key: " + key
					+ " is null");
		}

		// Replace if already exists
		int i = findIndex(key, _currentIndex - 1);
		if (i >= 0) {
			_headerValues[i] = value;
			_headerValuesLengths[i] = value.length;
			return;
		}

		// Newly adding
		add(key, value);
	}

	// Returns the index of the specified key, or -1 if it does not exist
	private int findIndex(byte[] key, int start) {
		byte[] checkKey = Util.bytesToLower(key);

		// Set the one with the highest index, if it matches
		for (int i = start; i >= 0; i--) {
			if (_headerKeys[i] != null
					&& Util.bytesEqual(checkKey, _headerKeysLc[i],
							_headerKeysLengths[i])) {
				return i;
			}
		}
		return -1;
	}

	private final void grow() {
		byte temp[][];
		int tempInt[];

		temp = new byte[_headerKeys.length * 2][];
		System.arraycopy(_headerKeys, 0, temp, 0, _headerKeys.length);
		_headerKeys = temp;

		tempInt = new int[_headerKeysLengths.length * 2];
		System.arraycopy(_headerKeysLengths, 0, tempInt, 0,
				_headerKeysLengths.length);
		_headerKeysLengths = tempInt;

		temp = new byte[_headerKeysLc.length * 2][];
		System.arraycopy(_headerKeysLc, 0, temp, 0, _headerKeysLc.length);
		_headerKeysLc = temp;

		temp = new byte[_headerValues.length * 2][];
		System.arraycopy(_headerValues, 0, temp, 0, _headerValues.length);
		_headerValues = temp;

		tempInt = new int[_headerValuesLengths.length * 2];
		System.arraycopy(_headerValuesLengths, 0, tempInt, 0,
				_headerValuesLengths.length);
		_headerValuesLengths = tempInt;

	}

	/**
	 * Get the key of the header given the order in which it was set.
	 */
	public final String getKeyAsString(int order) {
		if (order >= _currentIndex)
			return null;
		try {
			return new String(_headerKeys[order], 0, _headerKeysLengths[order],
					Util.ASCII_ENCODING);
		} catch (UnsupportedEncodingException e) {
			Util.impossible(e);
			return null;
		}
	}

	/**
	 * Get the value of the header given the order in which it was set.
	 */
	public final String getAsString(int order) {
		if (order >= _currentIndex)
			return null;
		try {
			if (_headerValuesLengths[order] == 0)
				return null;
			return new String(_headerValues[order], 0,
					_headerValuesLengths[order], Util.ASCII_ENCODING);
		} catch (UnsupportedEncodingException e) {
			Util.impossible(e);
			return null;
		}
	}

	/**
	 * Returns true if the header exists.
	 */
	public final boolean find(byte[] key) {
		if (key == null)
			return false;

		int index = findIndex(key, _currentIndex - 1);
		if (index >= 0)
			return true;
		return false;
	}

	/**
	 * Return the value associated with the specified key. If there is more than
	 * one value, the most recently added one is returned to be compatible with
	 * the JDK.
	 */
	public final String getAsString(byte[] key) {
		if (key == null)
			return null;

		int index = findIndex(key, _currentIndex - 1);
		if (index >= 0)
			return getAsString(index);
		return null;
	}

	/**
	 * Return the value associated with the specified key. If there is more than
	 * one value, the most recently added one is returned to be compatible with
	 * the JDK.
	 */
	public final String getAsString(String key) {
		if (key == null)
			return null;
		return getAsString(key.getBytes());
	}

	/**
	 * Removes all headers with the specified key.
	 */
	public final void remove(String key) {
		remove(key.getBytes());
	}

	/**
	 * Removes all headers with the specified key.
	 */
	public final void remove(byte[] key) {
		if (_log.isTraceEnabled())
			_log.trace("remove: " + new String(key));

		int index = _currentIndex - 1;

		while ((index = findIndex(key, index)) >= 0) {
			_headerKeys[index] = null;
			_headerKeysLc[index] = null;
			_headerValues[index] = null;
		}
	}

	/**
	 * Returns the length, which is essentially the highest index for which
	 * values might be stored.
	 */
	public final int length() {
		return _currentIndex;
	}

	public final void clear() {
		// Initial size
		_headerKeys = new byte[INIT_HEADER_COUNT][];
		if (_headerKeysLengths == null)
			_headerKeysLengths = new int[INIT_HEADER_COUNT];
		_headerKeysLc = new byte[INIT_HEADER_COUNT][];
		_headerValues = new byte[INIT_HEADER_COUNT][];
		if (_headerValuesLengths == null)
			_headerValuesLengths = new int[INIT_HEADER_COUNT];

		_currentIndex = 0;
	}

	// States
	private static final int ST_HEADER_START = 0;
	private static final int ST_HEADER = 1;
	private static final int ST_VALUE = 2;
	private static final int ST_BEFORE_VALUE = 3;
	private static final int ST_VALUE_NL = 4;
	private static final int ST_FINISHED = 5;

	// We allow CR or LF alone, if singleEolChar is set, LF alone at all times,
	// but we don't allow LFCR if singleEolChar is not set
	private void absorbNl(int ch, ExposedBufferInputStream is)
			throws IOException {
		// We have read CR, we always expect an NL after that

		// See if we need to fill
		if (is._pos >= is._used) {
			is.fill();
			if (is._used == -1) {
				throw new IOException("Unexpected EOF in processing headers");
			}
		}
		// Make sure we don't get a negative value
		ch = 0xff & is._buffer[is._pos++];
		if (ch != '\n') {
			throw new HttpException(Util
					.escapeForPrint("Expected LF for CRLF pair but got: '"
							+ Character.toString((char) ch) + "' (0x"
							+ Integer.toString(ch, 16) + ")"));
		}
	}

	public final void read(ExposedBufferInputStream is,
			HttpURLConnectInternal urlCon) throws IOException {
		read(is, urlCon, !SINGLE_EOL_CHAR, 0);
	}

	public static final boolean SINGLE_EOL_CHAR = true;

	public final void read(ExposedBufferInputStream is,
			HttpURLConnectInternal urlCon, boolean singleEolChar, int savedChar)
			throws IOException {
		clear();

		int ch = 0;
		int ind = 0;
		int state = ST_HEADER_START;
		boolean lastCharWasWs = false;
		boolean valueIsAscii = true;

		// The buffer pointing to the current key or value
		// being written
		byte[] keyValueBuffer = null;

		_urlCon = urlCon;
		byte[] buffer = is._buffer;

		while (true) {
			if (savedChar == 0) {
				// See if we need to fill
				if (is._pos >= is._used) {
					is.fill();
					if (is._used == -1) {
						// We allow the end to be marked with a single EOL
						if (state == ST_VALUE_NL)
							return;
						throw new IOException(
								"Unexpected EOF in processing headers");
					}
				}

				// Make sure we don't get a negative value
				ch = 0xff & buffer[is._pos++];
			} else {
				// We want to process this char
				ch = savedChar;
				savedChar = 0;
			}

			switch (state) {
			case ST_HEADER_START:
				if (ch == '\n' || ch == '\r') {
					// No headers at all
					if (!singleEolChar)
						absorbNl(ch, is);
					return;
				}

				if (_currentIndex >= _headerKeys.length)
					grow();

				// Set up to read the header
				ind = 0;
				state = ST_HEADER;
				keyValueBuffer = _headerKeys[_currentIndex] = new byte[INIT_BUF_SIZE];

				// Fall through

			case ST_HEADER:
				// Skip any white space
				if (ch == ' ' || ch == '\t')
					break;

				if (ch == ':') {
					// The value starts
					_headerKeysLc[_currentIndex] = Util
							.bytesToLower(_headerKeys[_currentIndex]);
					_headerKeysLengths[_currentIndex] = ind;
					ind = 0;
					state = ST_BEFORE_VALUE;
					// Assume so unless proven otherwise
					valueIsAscii = true;
					keyValueBuffer = _headerValues[_currentIndex] = new byte[INIT_BUF_SIZE];
					break;
				}

				// Save the header byte
				keyValueBuffer[ind++] = (byte) ch;
				if (ind >= keyValueBuffer.length) {
					byte[] newBuf = new byte[keyValueBuffer.length * 2];
					System.arraycopy(keyValueBuffer, 0, newBuf, 0,
							keyValueBuffer.length);
					keyValueBuffer = _headerKeys[_currentIndex] = newBuf;
				}
				break;

			case ST_BEFORE_VALUE:
				// Skip leading white space
				if (ch == ' ' || ch == '\t')
					break;

				state = ST_VALUE;
				// Fall through

			case ST_VALUE:
				if (ch == '\r' || ch == '\n') {
					// Remove any trailing blank on the line
					if (ind > 0) {
						if (keyValueBuffer[ind - 1] == ' '
								|| keyValueBuffer[ind - 1] == '\t')
							ind -= 1;
					}

					// This may terminate the value (multi-line values are
					// allowed), or this may be the start of termination of
					// the headers
					if (!singleEolChar)
						absorbNl(ch, is);
					state = ST_VALUE_NL;
					break;
				}

				// Eliminate redundant white space in value
				if (ch == ' ' || ch == '\t') {
					if (lastCharWasWs)
						break;
					lastCharWasWs = true;
				} else {
					lastCharWasWs = false;
				}

				// Save the value byte
				keyValueBuffer[ind++] = (byte) ch;
				if (ch > 0x7f)
					valueIsAscii = false;
				if (ind >= keyValueBuffer.length) {
					byte[] newBuf = new byte[keyValueBuffer.length * 2];
					System.arraycopy(keyValueBuffer, 0, newBuf, 0,
							keyValueBuffer.length);
					keyValueBuffer = _headerValues[_currentIndex] = newBuf;
				}
				break;

			case ST_VALUE_NL:
				if (ch == ' ' || ch == '\t') {
					// Indicates a multi-line value, emit a single
					// separator and continue processing the value
					// Emit a single space as a separator
					keyValueBuffer[ind++] = ' ';
					if (ind >= keyValueBuffer.length) {
						byte[] newBuf = new byte[keyValueBuffer.length * 2];
						System.arraycopy(keyValueBuffer, 0, newBuf, 0,
								keyValueBuffer.length);
						keyValueBuffer = _headerValues[_currentIndex] = newBuf;
					}
					state = ST_BEFORE_VALUE;
					lastCharWasWs = true;
					break;
				}

				// The value is finished at this point
				if (keyValueBuffer != null) {
					// There can be only one trailing space since they are
					// condensed
					if (ind > 0) {
						if (keyValueBuffer[ind - 1] == ' '
								|| keyValueBuffer[ind - 1] == '\t')
							ind -= 1;
					}

					if (!valueIsAscii) {
						String strValue = new String(keyValueBuffer, 0, ind);
						_headerValues[_currentIndex] = strValue.getBytes();
						_headerValuesLengths[_currentIndex] = _headerValues[_currentIndex].length;
					} else {
						_headerValuesLengths[_currentIndex] = ind;
					}

					// Let the urlCon get the header values it wants
					_urlCon.getHeadersWeNeed(_headerKeysLc[_currentIndex],
							_headerKeysLengths[_currentIndex],
							_headerValues[_currentIndex],
							_headerValuesLengths[_currentIndex]);

					_currentIndex++;
				}

				if (ch == '\r' || ch == '\n') {
					// This terminates everything
					if (!singleEolChar)
						absorbNl(ch, is);
					// We are done
					return;
				}

				// A new header
				state = ST_HEADER_START;
				// Reprocess this char for the header
				savedChar = ch;
				break;
			}
		}
	}

	public void dumpHeaders() {
		_log.debug(toString());
	}

	// Returns K(String of key) V(List of String of value)
	public Map getMap() {
		if (_currentIndex == 0)
			return Collections.EMPTY_MAP;
		Map map = new HashMap(_currentIndex);
		for (int i = 0; i < _currentIndex; i++) {
			byte[] key = _headerKeys[i];
			List values = new ArrayList();

			// Get each value for the specified key
			for (int j = 0; j < _currentIndex; j++) {
				if (Util.bytesEqual(key, _headerKeys[j], _headerKeysLengths[j]))
					values.add(new String(_headerValues[j], 0,
							_headerValuesLengths[j]));
			}
			map.put(new String(key, 0, _headerKeysLengths[i]), values);
		}
		return map;
	}

	private final void growCharBuf() {
		char[] newBuf = new char[_charBuf.length * 2];
		System.arraycopy(_charBuf, 0, newBuf, 0, _charBuf.length);
		_charBuf = newBuf;
	}

	private final void writeKeyValue(OutputStream os, byte[] key, byte[] value)
			throws IOException {
		if (_log.isTraceEnabled()) {
			_log
					.trace("writing: " + new String(key) + ": "
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
	public final void write(OutputStream os) throws IOException {
		for (int i = 0; i < _currentIndex; i++) {
			if (_headerKeys[i] == null)
				continue;
			writeKeyValue(os, _headerKeys[i], _headerValues[i]);
		}
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < _currentIndex; i++) {
			if (i > 0)
				sb.append("\n");
			sb.append(new String(_headerKeys[i]) + " ("
					+ new String(_headerKeysLc[i]) + ") : "
					+ new String(_headerValues[i]));
		}
		return sb.toString();
	}

}
