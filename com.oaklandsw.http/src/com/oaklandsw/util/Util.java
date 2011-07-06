/*
 * Copyright 2002, 2011, Oakland Software Incorporated
 * 
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

package com.oaklandsw.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import com.oaklandsw.utillog.Log;

/**
 * Utility methods.
 * 
 */
public class Util {

	private static final Log _log = LogUtils.makeLogger();

	public static final String ASCII_ENCODING = "US-ASCII";
	public static final String UTF8_ENCODING = "UTF-8";
	public static final String DEFAULT_ENCODING = ASCII_ENCODING;

	public static final String UNICODE_LE_ENCODING = "UTF-16LE";

	static public final byte[] CRLF_BYTES = "\r\n".getBytes();
	static public final byte[] LF_BYTES = "\n".getBytes();

	static public final byte[] COLON_BYTES = ":".getBytes();
	static public final byte[] COLON_SPACE_BYTES = ": ".getBytes();

	static public boolean _testDontLogImpossible;

	public static final int HASH_SEED = 17;
	public static final int HASH_OFFSET = 37;

	// Remove this when we drop support of JDK 1.2
	// JDK12
	public static final Map EMPTY_MAP = new HashMap();

	static private String BIGSTR;

	static {
		char c[] = new char[4096];
		for (int i = 0; i < c.length; i++) {
			c[i] = ' ';
		}
		BIGSTR = new String(c);
	}

	public static ImpossibleHandler _impossibleHandler;

	protected static String _lineSep;

	public static String _testHostName;

	public static Throwable _impossibleException;

	// Indicates we are running tests so do things like sort Map values
	public static boolean _testMode;

	public static void resetTest() {
		_testHostName = null;
		_impossibleException = null;
		_testDontLogImpossible = false;
		_testMode = false;
	}

	public static void setTestMode(boolean testMode) {
		_testMode = testMode;
	}

	// Used to hold a range of numbers that can be sorted
	public static class Range implements Comparable {
		public int _start;
		public int _end;

		public Range(int start, int end) {
			_start = start;
			_end = end;
		}

		public String toString() {
			return _start + "/" + _end;
		}

		public int compareTo(Object arg0) {
			Range r0 = (Range) arg0;
			if (_start > r0._start) {
				return 1;
			}
			if (_start < r0._start) {
				return -1;
			}
			return 0;
		}
	}

	public static void threadSleep(int millis) {
		long slept = 0;
		while (slept < millis) {
			long start = System.currentTimeMillis();
			try {
				Thread.sleep(millis - slept);
			} catch (InterruptedException e) {
			}
			slept = System.currentTimeMillis() - start;
		}
	}

	// Shorthand for object id
	public static int id(Object obj) {
		return System.identityHashCode(obj);
	}

	public interface ImpossibleHandler {
		void handleImpossible(String message, Throwable ex);
	}

	/**
	 * Used when an impossible condition is encountered (due to a bug).
	 */

	public static final void impossible(String message, Throwable ex) {
		// System.out.println("message: " + message);
		// Thread.dumpStack();
		if (message == null) {
			message = "Impossible";
		}
		if (ex instanceof InvocationTargetException) {
			ex = ((InvocationTargetException) ex).getTargetException();
		}
		// ex.fillInStackTrace();

		ImpossibleException rex = new ImpossibleException(message);
		if (ex != null) {
			if (!_testDontLogImpossible) {
				System.err.println("Bug - should not occur "
						+ "(Exception provided): " + message);
				ex.printStackTrace(System.err);
				if (_log != null) {
					_log.fatal("Bug - should not occur (Exception provided): "
							+ message, ex);
				}
				_impossibleException = ex;
				if (_impossibleHandler != null) {
					_impossibleHandler.handleImpossible(message, ex);
				}
			}
			rex.initCause(ex);
		} else {
			if (!_testDontLogImpossible) {
				System.err.println("Bug - should not occur " + message);
				Exception traceEx = new Exception(message);
				traceEx.fillInStackTrace();
				if (_log != null) {
					_log.fatal("Bug - should not occur (No Exception): "
							+ message, traceEx);
				} else {
					System.err.println("Bug - should not occur "
							+ "(No Exception): " + message);
					traceEx.printStackTrace(System.err);
				}
				_impossibleException = traceEx;
				if (_impossibleHandler != null) {
					_impossibleHandler.handleImpossible(message, traceEx);
				}
			}
		}
		throw rex;
	}

	public static final void impossible(Throwable ex) {
		impossible(null, ex);
	}

	public static final void impossible(String text) {
		impossible(text, null);
	}

	public static final void impossible() {
		impossible(null, null);
	}

	public static final void fixme(String message) {
		// System.err.println("FIXME: " + message);
	}

	public static final void fixmeDie(String message) {
		Util.impossible(message);
	}

	public static final void implementmeDie() {
		Util.impossible("Implement me");
	}

	public static void printStackTrace(PrintStream ps) {
		Exception ex = new Exception();
		ex.fillInStackTrace();
		ex.printStackTrace(ps);
	}

	public static final String UNKNOWN_HOST = "unknownHost";

	public static String getHostName() {
		if (_testHostName != null) {
			return _testHostName;
		}
		InetAddress addr;
		try {
			addr = InetAddress.getLocalHost();
			return addr.getHostName();
		} catch (UnknownHostException e) {
			// Try asking the horse's mouth for Unix systems
			Process proc = null;
			try {
				proc = Runtime.getRuntime().exec("hostname");
			} catch (IOException e2) {
				return UNKNOWN_HOST;
			}

			BufferedReader sr = new BufferedReader(new InputStreamReader(
					proc.getInputStream()));
			try {
				String hostName = sr.readLine();
				return hostName;
			} catch (IOException e2) {
				return UNKNOWN_HOST;
			}
		}
	}

	public static String calcHomeDir(String shortName, String longName) {
		String homeDir = System.getProperty("user.home") + File.separator;
		if (SystemUtils.IS_OS_WINDOWS) {
			homeDir += "Application Data" + File.separator + longName;
		} else if (SystemUtils.IS_OS_MAC) {
			homeDir += longName;
		} else {
			homeDir += "." + shortName;
		}
		return homeDir;
	}

	public static boolean isSourceControlDir(String dir) {
		if (dir.equals(".svn") || dir.equals(".cvs")) {
			return true;
		}
		return false;
	}

	// Returns true if any part of the range is visible
	public static boolean inLineRange(int visStartLine, int visNumberLines,
			int startLine, int endLine) {
		int visEndLine = visStartLine + visNumberLines - 1;

		if (startLine >= visStartLine && startLine < visEndLine) {
			return true;
		}
		if (endLine >= visStartLine && endLine <= visEndLine) {
			return true;
		}
		if (startLine <= visStartLine && endLine >= visEndLine) {
			return true;
		}
		return false;
	}

	public static String escapeForCsv(String in) {
		String out = StringUtils.replace(in, "\"", "\"\"");
		if (out.indexOf(",") != -1) {
			return "\"" + out + "\"";
		}
		return out;
	}

	public static String escapeQuotes(String in) {
		return StringUtils.replace(in, "\"", "\\\"");
	}

	public static String unescapeQuotes(String in) {
		return StringUtils.replace(in, "\\\"", "\"");
	}

	public static String unescapeNewline(String in) {
		in = StringUtils.replace(in, "\\n", "\n");
		return StringUtils.replace(in, "\\r", "\r");
	}

	public static String escapeForPrint(String in) {
		if (in == null) {
			return "null";
		}
		in = StringUtils.replace(in, "\u0000", "");
		in = StringUtils.replace(in, "\n", "\\n");
		in = StringUtils.replace(in, "\t", "\\t");
		return StringUtils.replace(in, "\r", "\\r");
	}

	/**
	 * The string data may actually be binary data, which we determine by
	 * heuristic by checking for a binary zero in the data.
	 */
	public static String printMaybeBinaryString(String str) {
		char[] ch = str.toCharArray();
		int length = ch.length;

		boolean binary = false;
		for (int i = 0; i < length; i++) {
			if (ch[i] == 0) {
				binary = true;
				break;

			}
		}
		if (binary) {
			byte[] bytes = new byte[length];
			for (int strInd = 0; strInd < length; strInd++) {
				bytes[strInd] = (byte) ch[strInd];
			}
			return "\n" + HexFormatter.dump(bytes);
		}
		return Util.escapeForPrint(str);
	}

	public static boolean equalsWithNull(Object obj, Object other) {
		if (obj == null && other == null) {
			return true;
		}
		if (obj != null && other == null || obj == null && other != null) {
			return false;
		}
		return obj.equals(other);
	}

	public static String normalizeSpace(String str) {
		StringBuffer sb = new StringBuffer();
		StringTokenizer st = new StringTokenizer(str);
		while (st.hasMoreTokens()) {
			sb.append(st.nextToken());
			sb.append(' ');
		}
		return StringUtils.trim(sb.toString());
	}

	public static final Class primClassToWrapClass(Class cls) {
		if (cls == Boolean.TYPE) {
			return Boolean.class;
		}
		if (cls == Integer.TYPE) {
			return Integer.class;
		}
		if (cls == Byte.TYPE) {
			return Byte.class;
		}
		if (cls == Character.TYPE) {
			return Character.class;
		}
		if (cls == Long.TYPE) {
			return Long.class;
		}
		if (cls == Short.TYPE) {
			return Short.class;
		}
		if (cls == Float.TYPE) {
			return Float.class;
		}
		if (cls == Double.TYPE) {
			return Double.class;
		}
		if (cls == Void.TYPE) {
			return Void.class;
		}
		return cls;
	}

	public static final Class wrapClassToPrimClass(Class cls) {
		if (cls == Boolean.class) {
			return Boolean.TYPE;
		}
		if (cls == Integer.class) {
			return Integer.TYPE;
		}
		if (cls == Byte.class) {
			return Byte.TYPE;
		}
		if (cls == Character.class) {
			return Character.TYPE;
		}
		if (cls == Long.class) {
			return Long.TYPE;
		}
		if (cls == Short.class) {
			return Short.TYPE;
		}
		if (cls == Float.class) {
			return Float.TYPE;
		}
		if (cls == Double.class) {
			return Double.TYPE;
		}
		if (cls == Void.class) {
			return Void.TYPE;
		}
		return cls;
	}

	public static boolean isDefault(Object value) {
		if (value instanceof Integer && ((Integer) value).intValue() == 0) {
			return true;
		}

		if (value instanceof Boolean
				&& ((Boolean) value).booleanValue() == false) {
			return true;
		}

		if (value instanceof Short && ((Short) value).intValue() == 0) {
			return true;
		}

		if (value instanceof Long && ((Long) value).intValue() == 0) {
			return true;
		}

		if (value instanceof Float && ((Float) value).floatValue() == 0) {
			return true;
		}

		if (value instanceof Double && ((Double) value).floatValue() == 0) {
			return true;
		}

		if (value instanceof Byte && ((Byte) value).byteValue() == 0) {
			return true;
		}

		if (value instanceof Character && ((Character) value).charValue() == 0) {
			return true;
		}

		return false;
	}

	// Parse the properties that we emitted in the valueToString()
	public static Map parseProperties(String input) {
		Map props = new HashMap();

		StringTokenizer st = new StringTokenizer(input, "=,");
		while (true) {
			// First thing
			String name = st.nextToken();
			// After the equals
			String value = st.nextToken();
			// Remove the quotes at each end
			value = value.substring(1);
			value = value.substring(0, value.length() - 1);
			props.put(name, value);
			if (!st.hasMoreTokens()) {
				break;
			}
		}
		return props;
	}

	public static Object valueFromString(Class type, String value) {
		if (String.class.isAssignableFrom(type)) {
			return value;
		}
		if (long.class.isAssignableFrom(type)) {
			return Long.valueOf(value);
		}
		if (int.class.isAssignableFrom(type)) {
			return Integer.valueOf(value);
		}
		if (short.class.isAssignableFrom(type)) {
			return Short.valueOf(value);
		}
		if (boolean.class.isAssignableFrom(type)) {
			return Boolean.valueOf(value);
		}
		if (float.class.isAssignableFrom(type)) {
			return Float.valueOf(value);
		}
		if (double.class.isAssignableFrom(type)) {
			return Double.valueOf(value);
		}
		if (byte.class.isAssignableFrom(type)) {
			return Byte.valueOf(value);
		}
		if (Properties.class.isAssignableFrom(type)) {
			return parseProperties(value);
		}
		if (Map.class.isAssignableFrom(type)) {
			return parseProperties(value);
		}
		Util.impossible("valueFromString: " + type + " value: " + value);
		return null;
	}

	public static String valueToString(Object value) {
		if (value instanceof Map) {
			Map props = (Map) value;

			StringBuffer out = new StringBuffer();
			Set keySet = props.keySet();
			Iterator it;
			if (_testMode) {
				List keys = new ArrayList();
				keys.addAll(keySet);
				Collections.sort(keys);
				it = keys.iterator();
			} else {
				it = keySet.iterator();
			}
			boolean needComma = false;
			while (it.hasNext()) {
				if (needComma) {
					out.append(",");
				}
				String key = (String) it.next();
				out.append(key);
				out.append("=\"");
				out.append(props.get(key).toString());
				out.append("\"");
				needComma = true;
			}
			return out.toString();
		}
		return value.toString();
	}

	// Handles printing Collections and embedded arrays nicely
	public static String collectionToString(Collection col) {
		StringBuffer sb = new StringBuffer();
		Iterator it = col.iterator();
		sb.append("[");
		int i = 0;
		while (it.hasNext()) {
			Object obj = it.next();
			if (i++ > 0) {
				sb.append(", ");
			}
			if (obj instanceof Collection) {
				sb.append(collectionToString((Collection) obj));
			} else if (obj.getClass().isArray()) {
				sb.append(arrayToString((Object[]) obj));
			} else if (obj != null) {
				sb.append(obj.toString());
			} else {
				sb.append("null");
			}
		}
		sb.append("]");
		return sb.toString();
	}

	public static String arrayToString(Object[] array) {
		StringBuffer sb = new StringBuffer();
		sb.append("[");
		for (int i = 0; i < array.length; i++) {
			if (i > 0) {
				sb.append(", ");
			}
			if (array[i] != null) {
				sb.append(array[i].toString());
			} else {
				sb.append("null");
			}
		}
		sb.append("]");
		return sb.toString();
	}

	public static String arrayToStringId(Object[] array) {
		StringBuffer sb = new StringBuffer();
		sb.append("[");
		for (int i = 0; i < array.length; i++) {
			if (i > 0) {
				sb.append(", ");
			}
			if (array[i] != null) {
				sb.append(array[i].toString() + "(" + id(array[i]) + ")");
			} else {
				sb.append("null");
			}
		}
		sb.append("]");
		return sb.toString();
	}

	public static String arrayToStringLine(Object[] array) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < array.length; i++) {
			if (array[i] != null) {
				sb.append(array[i].toString());
			} else {
				sb.append("null");
			}
			sb.append(SystemUtils.LINE_SEPARATOR);
		}
		return sb.toString();
	}

	public static String enumToStringLong(Object[] e, long enumValue) {
		for (int i = 0; i < e.length; i += 2) {
			long val = ((Long) e[i + 1]).longValue();
			if (val == enumValue) {
				return (String) e[i];
			}
		}
		return "Unknown enum val: " + enumValue;
	}

	public static String enumToStringInt(Object[] e, int enumValue) {
		for (int i = 0; i < e.length; i += 2) {
			int val = ((Integer) e[i + 1]).intValue();
			if (val == enumValue) {
				return (String) e[i];
			}
		}
		return "Unknown enum val: " + enumValue;
	}

	public static int intFromObject(Object obj) {
		if (obj == null) {
			return 0;
		} else if (obj instanceof String) {
			if (((String) obj).length() == 0) {
				return 0;
			}
			try {
				return Integer.parseInt((String) obj);
			} catch (NumberFormatException ex) {
				IllegalArgumentException iea = new IllegalArgumentException(
						"Object: " + obj);
				iea.initCause(ex);
				throw iea;
			}
		} else if (obj instanceof Integer) {
			return ((Integer) obj).intValue();
		} else {
			IllegalArgumentException iea = new IllegalArgumentException(
					"Invalid datatype: " + obj.getClass());
			throw iea;
		}
	}

	public static Integer[] intArrayToIntegerArray(int[] in) {
		Integer[] out = new Integer[in.length];
		for (int i = 0; i < in.length; i++) {
			out[i] = new Integer(in[i]);
		}
		return out;
	}

	public static ArrayList intArrayToArrayList(int[] in) {
		ArrayList al = new ArrayList();
		for (int i = 0; i < in.length; i++) {
			al.add(new Integer(in[i]));
		}
		return al;
	}

	public static int[] integerArrayToIntArray(Integer[] in) {
		int[] out = new int[in.length];
		for (int i = 0; i < in.length; i++) {
			out[i] = in[i].intValue();
		}
		return out;
	}

	public static int[] arrayListToIntArray(ArrayList mc) {
		int[] nums = new int[mc.size()];
		for (int i = 0; i < nums.length; i++) {
			nums[i] = ((Integer) mc.get(i)).intValue();
		}
		return nums;
	}

	public static String[] listToStringArray(List mc) {
		String[] nums = new String[mc.size()];
		for (int i = 0; i < nums.length; i++) {
			nums[i] = ((String) mc.get(i)).toString();
		}
		return nums;
	}

	public static String indent(int num) {
		return BIGSTR.substring(0, num);
	}

	public static String lineNumberString(String string) {
		return lineNumberString(string, !NUMBER_ONLY);
	}

	public static final boolean NUMBER_ONLY = true;

	// Returns a string with a prefix of the line numbers, or
	// just the line numbers
	public static String lineNumberString(String string, boolean numberOnly) {
		if (string == null) {
			return null;
		}

		int numLines = StringUtils.countMatches(string, "\n");
		String numLinesStr = Integer.toString(numLines);
		String mask = "";
		for (int i = 0; i < numLinesStr.length(); i++) {
			mask += "0";
		}
		mask += " ";
		DecimalFormat df = new DecimalFormat(mask);

		StringBuffer sb = new StringBuffer(string.length()
				+ (numLines * mask.length()));
		int pos = 0;
		int prevPos = 0;
		int lineNum = 0;
		while (true) {
			sb.append(df.format(++lineNum));
			prevPos = pos;
			pos = string.indexOf("\n", pos) + 1;
			if (pos == 0) {
				if (numberOnly) {
					sb.append("\n");
				} else {
					sb.append(string.substring(prevPos));
				}
				return sb.toString();
			}
			if (numberOnly) {
				sb.append("\n");
			} else {
				sb.append(string.substring(prevPos, pos));
			}
		}
	}

	public final static long toUnsigned(byte b) {
		if (b < 0) {
			return 0x100 + b;
		}
		return b;
	}

	public final static long toUnsigned(short b) {
		if (b < 0) {
			return 0x10000 + b;
		}
		return b;
	}

	public final static long toUnsigned(int b) {
		if (b < 0) {
			return 0x100000000L + b;
		}
		return b;
	}

	/**
	 * Gets the int value of the specified property
	 * 
	 * @return an int, -1 if the property is not defined
	 */
	public final static int getIntProperty(String propName) {
		String str = System.getProperty(propName);
		if (str == null) {
			return -1;
		}
		try {
			return Integer.parseInt(str);
		} catch (Exception e) {
			return -1;
		}
	}

	public static String getDefaultEncoding() {
		return System.getProperty("file.encoding");
	}

	public static String getEncodingOrDefault(String encoding) {
		if (encoding != null) {
			return encoding;
		}
		return System.getProperty("file.encoding");
	}

	public static String getLineSep() {
		if (_lineSep != null) {
			return _lineSep;
		}
		_lineSep = System.getProperty("line.separator");
		return _lineSep;
	}

	public static boolean isEbcdic(String encoding) {
		int offset = 0;
		if (encoding == null) {
			return false;
		}
		encoding = encoding.toLowerCase();
		if (encoding.startsWith("cp")) {
			offset = 2;
		} else if (encoding.startsWith("ibm")) {
			offset = 3;
		} else {
			return false;
		}

		int cpNumber = 0;
		try {
			cpNumber = Integer.parseInt(encoding.substring(offset));
		} catch (Exception ex) {
			// Must not be a real code page encoding
			return false;
		}

		if (cpNumber == 37 || cpNumber == 256 || cpNumber == 260
				|| cpNumber == 273 || cpNumber == 277 || cpNumber == 278
				|| cpNumber == 280 || cpNumber == 281 || cpNumber == 284
				|| cpNumber == 285 || cpNumber == 290 || cpNumber == 297
				|| cpNumber == 420 || cpNumber == 423 || cpNumber == 424
				|| cpNumber == 435 || cpNumber == 500 || cpNumber == 833
				|| cpNumber == 836 || cpNumber == 838 || cpNumber == 871
				|| cpNumber == 875 || cpNumber == 880 || cpNumber == 905
				|| cpNumber == 918 || cpNumber == 1024 || cpNumber == 1025
				|| cpNumber == 1026 || cpNumber == 1027 || cpNumber == 1046
				|| cpNumber == 1047 || cpNumber == 1048 || cpNumber == 1097
				|| cpNumber == 1112 || cpNumber == 1122 || cpNumber == 1123
				|| cpNumber == 1148) {
			return true;
		}
		return false;

	}

	// Assumes the bytes represent ASCII characters
	public final static byte[] bytesToLower(byte[] inBytes) {
		byte[] outBytes = new byte[inBytes.length];

		for (int i = 0; i < inBytes.length; i++) {
			// A=65 Z=90 a=97
			if (inBytes[i] >= 65 && inBytes[i] <= 90) {
				outBytes[i] = (byte) (inBytes[i] + 32);
			} else {
				outBytes[i] = inBytes[i];
			}
		}
		return outBytes;
	}

	public final static String bytesToString(byte[] bytes) {
		if (bytes == null) {
			return null;
		}
		return new String(bytes);
	}

	public final static String bytesToString(byte[] bytes, int length) {
		if (bytes == null) {
			return null;
		}
		return new String(bytes, 0, length);
	}

	// Converts an int to ASCII bytes
	public final static byte[] toBytesAsciiInt(int number) {
		// For now, we can optimize this further
		return Integer.toString(number).getBytes();
	}

	// Converts from ASCII bytes to an int
	public final static int fromBytesAsciiInt(byte[] bytes) {
		// For now, we can optimize this further
		return Integer.parseInt(new String(bytes));
	}

	// Converts from ASCII bytes to an int
	public final static int fromBytesAsciiInt(byte[] bytes, int len) {
		// For now, we can optimize this further
		return Integer.parseInt(new String(bytes, 0, len));
	}

	public final static boolean bytesEqual(byte[] b1, byte[] b2) {
		if (b1 == b2) {
			return true;
		}
		if (b1 == null || b2 == null) {
			return false;
		}
		if (b1.length != b2.length) {
			return false;
		}
		for (int i = 0; i < b1.length; i++) {
			if (b1[i] != b2[i]) {
				return false;
			}
		}
		return true;
	}

	// Used when the b2 array might have data that is shorter than
	// the number of bytes in the array
	public final static boolean bytesEqual(byte[] b1, byte[] b2, int b2length) {
		if (b1 == b2) {
			return true;
		}
		if (b1 == null || b2 == null) {
			return false;
		}
		if (b1.length != b2length) {
			return false;
		}
		for (int i = 0; i < b1.length; i++) {
			if (b1[i] != b2[i]) {
				return false;
			}
		}
		return true;
	}

	// As above, but allows an offset into b2 to be specified
	public final static boolean bytesEqual(byte[] b1, byte[] b2, int b2offset,
			int b2length) {
		if (b1 == b2) {
			return true;
		}
		if (b1 == null || b2 == null) {
			return false;
		}
		for (int b1ind = 0, b2ind = b2offset; b1ind < b1.length
				&& b2ind < b2length; b1ind++, b2ind++) {
			if (b1[b1ind] != b2[b2ind]) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Puts the ASCII encoding of the specified string into the specified byte
	 * array at the specified offset. No null termination is added to the
	 * string.
	 */
	public final static int toByteAscii(String str, byte[] byteArr, int offset) {
		char[] chars = str.toCharArray();
		// Since this is Ascii, a char is a byte
		int len = chars.length;
		for (int i = 0; i < len; i++) {
			byteArr[offset + i] = (byte) chars[i];
		}
		return offset + len;
	}

	/**
	 * Returns the Ascii string at the specified position in the byte array.
	 */
	public final static String fromByteAscii(int length, byte[] byteArr,
			int offset) {
		char[] strBytes = new char[length];

		// Since its ASCII, we can just copy the characters from
		// the bytes (don't use the byte constructor because it will do
		// a codeset conversion which is expensive)
		for (int i = 0; i < length; i++) {
			strBytes[i] = (char) byteArr[offset + i];
		}

		return String.valueOf(strBytes);
	}

	/**
	 * Puts the Unicode encoding of the specified string into the specified byte
	 * array at the specified offset. No null termination is added to the
	 * string.
	 */
	public final static int toByteUnicodeLittle(String str, byte[] byteArr,
			int offset) {
		byte[] strBytes = getUnicodeLittleBytes(str);
		System.arraycopy(strBytes, 0, byteArr, offset, strBytes.length);
		return offset + strBytes.length;
	}

	public final static byte[] getUnicodeLittleBytes(String str) {
		byte[] strBytes = null;
		try {
			strBytes = str.getBytes(UNICODE_LE_ENCODING);
		} catch (UnsupportedEncodingException ex) {
			// This better not happen
			throw new RuntimeException("(bug) - unsupported encoding "
					+ UNICODE_LE_ENCODING);
		}
		return strBytes;
	}

	/**
	 * Returns the Unicode string at the specified position in the byte array.
	 */
	public final static String fromByteUnicodeLittle(int length,
			byte[] byteArr, int offset) {
		try {
			return new String(byteArr, offset, length, UNICODE_LE_ENCODING);
		} catch (UnsupportedEncodingException e) {
			Util.impossible(e);
			return null;
		}
	}

	/**
	 * Puts the specified value into the byte array at the specified offset for
	 * the specified length in little endian format. Returns the index of the
	 * next byte after the location of the value. Length has to be 1, 2, 4 or 8.
	 */
	public final static int toByteLittle(long value, int length,
			byte[] byteArr, int offset) {
		byteArr[offset] = (byte) (value & 0xff);
		if (length == 1) {
			return offset + 1;
		}

		byteArr[offset + 1] = (byte) ((value >> 0x8) & 0xff);
		if (length == 2) {
			return offset + 2;
		}

		byteArr[offset + 2] = (byte) ((value >> 0x10) & 0xff);
		byteArr[offset + 3] = (byte) ((value >> 0x18) & 0xff);
		if (length == 4) {
			return offset + 4;
		}

		byteArr[offset + 4] = (byte) ((value >> 0x20) & 0xff);
		byteArr[offset + 5] = (byte) ((value >> 0x28) & 0xff);
		byteArr[offset + 6] = (byte) ((value >> 0x30) & 0xff);
		byteArr[offset + 7] = (byte) ((value >> 0x38) & 0xff);
		return offset + 8;
	}

	/**
	 * Puts the specified value into the byte array at the specified offset for
	 * the specified length in big endian format. Returns the index of the next
	 * byte after the location of the value. Length has to be 1, 2, 4 or 8.
	 */
	public final static int toByteBig(long value, int length, byte[] byteArr,
			int offset) {
		if (length == 1) {
			byteArr[offset] = (byte) (value & 0xff);
			return offset + 1;
		}

		if (length == 2) {
			byteArr[offset] = (byte) ((value >> 0x8) & 0xff);
			byteArr[offset + 1] = (byte) (value & 0xff);
			return offset + 2;
		}

		if (length == 4) {
			byteArr[offset] = (byte) ((value >> 0x18) & 0xff);
			byteArr[offset + 1] = (byte) ((value >> 0x10) & 0xff);
			byteArr[offset + 2] = (byte) ((value >> 0x8) & 0xff);
			byteArr[offset + 3] = (byte) (value & 0xff);
			return offset + 4;
		}

		if (length == 8) {
			byteArr[offset] = (byte) ((value >> 0x38) & 0xff);
			byteArr[offset + 1] = (byte) ((value >> 0x30) & 0xff);
			byteArr[offset + 2] = (byte) ((value >> 0x28) & 0xff);
			byteArr[offset + 3] = (byte) ((value >> 0x20) & 0xff);
			byteArr[offset + 4] = (byte) ((value >> 0x18) & 0xff);
			byteArr[offset + 5] = (byte) ((value >> 0x10) & 0xff);
			byteArr[offset + 6] = (byte) ((value >> 0x8) & 0xff);
			byteArr[offset + 7] = (byte) (value & 0xff);
			return offset + 8;
		}
		throw new IllegalArgumentException("invalid length specified");
	}

	/**
	 * As above, but char array.
	 */
	public final static int toCharLittle(long value, int length,
			char[] charArr, int offset) {
		charArr[offset] = (char) (value & 0xff);
		if (length == 1) {
			return offset + 1;
		}

		charArr[offset + 1] = (char) ((value >> 0x8) & 0xff);
		if (length == 2) {
			return offset + 2;
		}

		charArr[offset + 2] = (char) ((value >> 0x10) & 0xff);
		charArr[offset + 3] = (char) ((value >> 0x18) & 0xff);
		if (length == 4) {
			return offset + 4;
		}

		charArr[offset + 4] = (char) ((value >> 0x20) & 0xff);
		charArr[offset + 5] = (char) ((value >> 0x28) & 0xff);
		charArr[offset + 6] = (char) ((value >> 0x30) & 0xff);
		charArr[offset + 7] = (char) ((value >> 0x38) & 0xff);
		return offset + 8;
	}

	/**
	 * As above, but char array.
	 */
	public final static int toCharBig(long value, int length, char[] charArr,
			int offset) {
		if (length == 1) {
			charArr[offset] = (char) (value & 0xff);
			return offset + 1;
		}

		if (length == 2) {
			charArr[offset] = (char) ((value >> 0x8) & 0xff);
			charArr[offset + 1] = (char) (value & 0xff);
			return offset + 2;
		}

		if (length == 4) {
			charArr[offset] = (char) ((value >> 0x18) & 0xff);
			charArr[offset + 1] = (char) ((value >> 0x10) & 0xff);
			charArr[offset + 2] = (char) ((value >> 0x8) & 0xff);
			charArr[offset + 3] = (char) (value & 0xff);
			return offset + 4;
		}

		if (length == 8) {
			charArr[offset] = (char) ((value >> 0x38) & 0xff);
			charArr[offset + 1] = (char) ((value >> 0x30) & 0xff);
			charArr[offset + 2] = (char) ((value >> 0x28) & 0xff);
			charArr[offset + 3] = (char) ((value >> 0x20) & 0xff);
			charArr[offset + 4] = (char) ((value >> 0x18) & 0xff);
			charArr[offset + 5] = (char) ((value >> 0x10) & 0xff);
			charArr[offset + 6] = (char) ((value >> 0x8) & 0xff);
			charArr[offset + 7] = (char) (value & 0xff);
			return offset + 8;
		}
		throw new IllegalArgumentException("invalid length specified");
	}

	/**
	 * As above, but writes to the specified stream.
	 */
	public final static void toStreamLittle(OutputStream out, long value,
			int length) throws IOException {
		out.write((byte) (value & 0xff));
		if (length == 1) {
			return;
		}

		out.write((byte) ((value >> 0x8) & 0xff));
		if (length == 2) {
			return;
		}

		out.write((byte) ((value >> 0x10) & 0xff));
		out.write((byte) ((value >> 0x18) & 0xff));
		if (length == 4) {
			return;
		}

		out.write((byte) ((value >> 0x20) & 0xff));
		out.write((byte) ((value >> 0x28) & 0xff));
		out.write((byte) ((value >> 0x30) & 0xff));
		out.write((byte) ((value >> 0x38) & 0xff));
	}

	/**
	 * As above, but writes to the specified stream.
	 */
	public final static void toStreamBig(OutputStream out, long value,
			int length) throws IOException {
		if (length == 1) {
			out.write((byte) (value & 0xff));
			return;
		}

		if (length == 2) {
			out.write((byte) ((value >> 0x8) & 0xff));
			out.write((byte) (value & 0xff));
			return;
		}

		if (length == 4) {
			out.write((byte) ((value >> 0x18) & 0xff));
			out.write((byte) ((value >> 0x10) & 0xff));
			out.write((byte) ((value >> 0x8) & 0xff));
			out.write((byte) (value & 0xff));
			return;
		}

		if (length == 8) {
			out.write((byte) ((value >> 0x38) & 0xff));
			out.write((byte) ((value >> 0x30) & 0xff));
			out.write((byte) ((value >> 0x28) & 0xff));
			out.write((byte) ((value >> 0x20) & 0xff));
			out.write((byte) ((value >> 0x18) & 0xff));
			out.write((byte) ((value >> 0x10) & 0xff));
			out.write((byte) ((value >> 0x8) & 0xff));
			out.write((byte) (value & 0xff));
			return;
		}
		throw new IllegalArgumentException("invalid length");
	}

	/**
	 * Gets the value in the byte array at the specified offset. The value is
	 * returned.
	 */
	public final static long fromByteLittle(int length, byte[] byteArr,
			int offset) {
		long value = 0;

		value = toUnsigned(byteArr[offset]);
		if (length == 1) {
			return value;
		}

		value += toUnsigned(byteArr[offset + 1]) << 0x8;
		if (length == 2) {
			return value;
		}

		value += toUnsigned(byteArr[offset + 2]) << 0x10;
		value += toUnsigned(byteArr[offset + 3]) << 0x18;
		if (length == 4) {
			return value;
		}

		value += toUnsigned(byteArr[offset + 4]) << 0x20;
		value += toUnsigned(byteArr[offset + 5]) << 0x28;
		value += toUnsigned(byteArr[offset + 6]) << 0x30;
		value += toUnsigned(byteArr[offset + 7]) << 0x38;
		return value;
	}

	public final static long fromCharLittle(int length, char[] byteArr,
			int offset) {
		long value = 0;

		value = toUnsigned(byteArr[offset]);
		if (length == 1) {
			return value;
		}

		value += toUnsigned(byteArr[offset + 1]) << 0x8;
		if (length == 2) {
			return value;
		}

		value += toUnsigned(byteArr[offset + 2]) << 0x10;
		value += toUnsigned(byteArr[offset + 3]) << 0x18;
		if (length == 4) {
			return value;
		}

		value += toUnsigned(byteArr[offset + 4]) << 0x20;
		value += toUnsigned(byteArr[offset + 5]) << 0x28;
		value += toUnsigned(byteArr[offset + 6]) << 0x30;
		value += toUnsigned(byteArr[offset + 7]) << 0x38;
		return value;
	}

	/**
	 * Gets the value in the byte array at the specified offset. The value is
	 * returned.
	 */
	public final static long fromByteBig(int length, byte[] byteArr, int offset) {
		long value = 0;

		if (length == 1) {
			value = toUnsigned(byteArr[offset]);
			return value;
		}

		if (length == 2) {
			value = toUnsigned(byteArr[offset]) << 0x8;
			value += toUnsigned(byteArr[offset + 1]);
			return value;
		}

		if (length == 4) {
			value = toUnsigned(byteArr[offset]) << 0x18;
			value += toUnsigned(byteArr[offset + 1]) << 0x10;
			value += toUnsigned(byteArr[offset + 2]) << 0x8;
			value += toUnsigned(byteArr[offset + 3]);
			return value;
		}

		if (length == 8) {
			value += toUnsigned(byteArr[offset]) << 0x38;
			value += toUnsigned(byteArr[offset + 1]) << 0x30;
			value += toUnsigned(byteArr[offset + 2]) << 0x28;
			value += toUnsigned(byteArr[offset + 3]) << 0x20;
			value += toUnsigned(byteArr[offset + 4]) << 0x18;
			value += toUnsigned(byteArr[offset + 5]) << 0x10;
			value += toUnsigned(byteArr[offset + 6]) << 0x8;
			value += toUnsigned(byteArr[offset + 7]);
			return value;
		}

		throw new IllegalArgumentException("Invalid length");
	}

	/**
	 * Gets the value in the char array at the specified offset. The value is
	 * returned. Uses the byte portion of each char[]
	 */
	public final static long fromCharBig(int length, char[] byteArr, int offset) {
		long value = 0;

		if (length == 1) {
			value = toUnsigned(byteArr[offset]);
			return value;
		}

		if (length == 2) {
			value = toUnsigned(byteArr[offset]) << 0x8;
			value += toUnsigned(byteArr[offset + 1]);
			return value;
		}

		if (length == 4) {
			value = toUnsigned(byteArr[offset]) << 0x18;
			value += toUnsigned(byteArr[offset + 1]) << 0x10;
			value += toUnsigned(byteArr[offset + 2]) << 0x8;
			value += toUnsigned(byteArr[offset + 3]);
			return value;
		}

		if (length == 8) {
			value += toUnsigned(byteArr[offset]) << 0x38;
			value += toUnsigned(byteArr[offset + 1]) << 0x30;
			value += toUnsigned(byteArr[offset + 2]) << 0x28;
			value += toUnsigned(byteArr[offset + 3]) << 0x20;
			value += toUnsigned(byteArr[offset + 4]) << 0x18;
			value += toUnsigned(byteArr[offset + 5]) << 0x10;
			value += toUnsigned(byteArr[offset + 6]) << 0x8;
			value += toUnsigned(byteArr[offset + 7]);
			return value;
		}

		throw new IllegalArgumentException("Invalid length");
	}

	/**
	 * Gets the value in the input stream for the specified length. The value is
	 * returned
	 */
	public final static long fromStreamLittle(InputStream in, int length)
			throws IOException {
		long value = 0;

		value = in.read();
		if (length == 1) {
			return value;
		}

		value += toUnsigned((byte) in.read()) << 0x8;
		if (length == 2) {
			return value;
		}

		value += toUnsigned((byte) in.read()) << 0x10;
		value += toUnsigned((byte) in.read()) << 0x18;
		if (length == 4) {
			return value;
		}

		value += toUnsigned((byte) in.read()) << 0x20;
		value += toUnsigned((byte) in.read()) << 0x28;
		value += toUnsigned((byte) in.read()) << 0x30;
		value += toUnsigned((byte) in.read()) << 0x38;
		return value;
	}

	/**
	 * Gets the value in the input stream for the specified length. The value is
	 * returned
	 */
	public final static long fromStreamBig(InputStream in, int length)
			throws IOException {
		long value = 0;

		if (length == 1) {
			return value;
		}

		if (length == 2) {
			value = toUnsigned((byte) in.read()) << 0x8;
			value += in.read();
			return value;
		}

		if (length == 4) {
			value = toUnsigned((byte) in.read()) << 0x18;
			value += toUnsigned((byte) in.read()) << 0x10;
			value += toUnsigned((byte) in.read()) << 0x8;
			value += in.read();
			return value;
		}

		if (length == 8) {
			value = toUnsigned((byte) in.read()) << 0x38;
			value += toUnsigned((byte) in.read()) << 0x30;
			value += toUnsigned((byte) in.read()) << 0x28;
			value += toUnsigned((byte) in.read()) << 0x20;
			value += toUnsigned((byte) in.read()) << 0x18;
			value += toUnsigned((byte) in.read()) << 0x10;
			value += toUnsigned((byte) in.read()) << 0x8;
			value += in.read();
			return value;
		}
		throw new IllegalArgumentException("invalid length");
	}

	public final static String sortStringLines(String input) throws IOException {
		BufferedReader reader = new BufferedReader(new StringReader(input));
		TreeSet map = new TreeSet();
		String line = "";
		while ((line = reader.readLine()) != null) {
			map.add(line);
		}
		StringBuffer output = new StringBuffer();
		Iterator it = map.iterator();
		while (it.hasNext()) {
			output.append(it.next());
			output.append('\n');
		}
		return output.toString();
	}

	public final static String getStringFromInputStream(InputStream inStr)
			throws IOException {
		return getStringFromInputStream(inStr, null);
	}

	/**
	 * Gets a string from the specified InputStream.
	 */
	public final static String getStringFromInputStream(InputStream inStr,
			String encoding) throws IOException {
		ByteArrayOutputStream outStr = new ByteArrayOutputStream();
		copyStreams(inStr, outStr);
		String ret;
		if (encoding == null) {
			ret = new String(outStr.toByteArray());
		} else {
			ret = new String(outStr.toByteArray(), encoding);
		}
		inStr.close();
		outStr.close();
		return ret;
	}

	/**
	 * Gets a string from the specified Reader
	 */
	public final static String getStringFromReader(Reader inStr)
			throws IOException {
		StringWriter sw = new StringWriter();
		char[] buffer = new char[BUF_SIZE];
		int nb = 0;
		while (true) {
			nb = inStr.read(buffer);
			if (nb == -1) {
				break;
			}
			sw.write(buffer, 0, nb);
		}
		return sw.toString();
	}

	/**
	 * Gets a byte array from the specified InputStream.
	 */
	public final static byte[] getBytesFromInputStream(InputStream inStr)
			throws IOException {
		ByteArrayOutputStream outStr = new ByteArrayOutputStream();
		copyStreams(inStr, outStr);
		inStr.close();
		outStr.close();
		return outStr.toByteArray();
	}

	// This is just a shared place to toss bytes when flushing
	// a stream out
	private static byte[] _bitBucket = new byte[16384];

	/**
	 * Reads the rest of the stream into the bit bucket.
	 */
	public final static int flushStream(InputStream inputStream)
			throws IOException {
		int nb = 0;
		int total = 0;
		while (true) {
			nb = inputStream.read(_bitBucket);
			if (nb == -1) {
				break;
			}
			total += nb;
		}
		return total;
	}

	private static final int BUF_SIZE = 16384;

	/**
	 * Copies the contents of the specified input stream to the output stream.
	 */
	public final static int copyStreams(InputStream inputStream,
			OutputStream outputStream) throws IOException {
		byte[] buffer = new byte[BUF_SIZE];
		int nb = 0;
		int total = 0;
		while (true) {
			nb = inputStream.read(buffer);
			if (nb == -1) {
				break;
			}
			total += nb;
			outputStream.write(buffer, 0, nb);
		}
		return total;
	}

	/**
	 * Copies the contents of the specified reader to the output stream.
	 */
	public final static int copyReaderToStream(Reader reader,
			OutputStream outputStream) throws IOException {
		OutputStreamWriter osw = new OutputStreamWriter(outputStream);
		char[] buffer = new char[BUF_SIZE];
		int nb = 0;
		int total = 0;
		while (true) {
			nb = reader.read(buffer);
			if (nb == -1) {
				break;
			}
			total += nb;
			osw.write(buffer, 0, nb);
		}
		osw.close();
		return total;
	}

	/**
	 * Copies the contents of the specified input stream to the output stream,
	 * but limited to the requested number of bytes. Saves the contents of the
	 * input stream into the specified byte array.
	 */
	public final static int copyStreams(InputStream inputStream,
			OutputStream outputStream, byte[] outBytes, int limit)
			throws IOException {
		int nb = 0;
		int total = 0;
		int bufPos = 0;
		while (true) {
			// Read into the buffer
			nb = inputStream.read(outBytes, bufPos, outBytes.length - bufPos);
			if (nb == -1) {
				break;
			}
			total += nb;
			if (total > limit) {
				nb -= (total - limit);
				outputStream.write(outBytes, bufPos, nb);
				return limit;
			}
			outputStream.write(outBytes, bufPos, nb);
			bufPos = total;
		}
		return total;
	}

	/**
	 * Read up to <tt>"\r\n"</tt> from an input stream.
	 * 
	 * @throws IOException
	 *             if an I/O problem occurs
	 * @return a line from the response
	 */
	public static String readLine(InputStream is) throws IOException {
		StringBuffer buf = new StringBuffer();
		for (;;) {
			int ch = is.read();
			if (ch < 0) {
				if (buf.length() == 0) {
					return null;
				}
				break;
			} else if (ch == '\r') {
				continue;
			} else if (ch == '\n') {
				break;
			}
			buf.append((char) ch);
		}
		return (buf.toString());
	}

	/**
	 * Prints a sorted list of properties
	 */
	public static void printProperties(Properties props, PrintWriter pw) {
		Collection keys = props.keySet();
		List keyList = new ArrayList(keys);
		Collections.sort(keyList);
		int len = keyList.size();
		for (int i = 0; i < len; i++) {
			pw.println(keyList.get(i) + "=" + props.get(keyList.get(i)));
		}
	}

	public static boolean FOR_USER = true;

	/**
	 * Returns the messages of all of the exceptions in the stack.
	 * 
	 * @param forUser
	 *            TODO
	 */
	public static String getExceptionMessage(Throwable ex, boolean forUser) {
		String message = "";
		Throwable cause = ex;

		while (cause != null) {
			if (message.length() > 0) {
				message += "\n\nCaused by: ";
			}
			if (!forUser) {
				message += cause.getClass().getName();
				message += "\n";
			}
			String localMessage = cause.getMessage();
			if (localMessage == null || localMessage.equals("")) {
				localMessage = cause.getClass().getName();
			}
			message += localMessage;
			cause = cause.getCause();
		}
		return message;
	}

	// Requires 1.4
	public static Throwable getExceptionCause(Throwable ex) {
		return getCause(ex);
	}

	public void initCause(Throwable ex, Throwable cause) {
		ex.initCause(cause);
	}

	public static Throwable getCause(Throwable ex) {
		return ex.getCause();
	}

	// Requires 1.4
	public static String getExceptionString(Throwable ex) {
		StringBuffer sb = new StringBuffer(1000);

		while (ex != null) {
			sb.append(ex.toString());
			StackTraceElement st[] = ex.getStackTrace();
			for (int i = 0; i < st.length; i++) {
				// Must have "at" to be compatible with things that
				// look at the stack trace, like ZKM
				sb.append("\nat ");
				sb.append(st[i].toString());
			}
			ex = ex.getCause();
			if (ex != null) {
				sb.append("\n caused by: ");
			}
		}

		return sb.toString();
	}

	// Requires 1.4
	public static StackTraceElement getCallingMethodElement(int depth) {
		Throwable t = new Throwable();
		StackTraceElement elements[] = t.getStackTrace();
		return elements[depth];
	}

	public static String getCallingMethodName(int depth) {
		return getCallingMethodElement(depth + 2).getMethodName();
	}

	public static int getCallingMethodLineNumber(int depth) {
		return getCallingMethodElement(depth + 2).getLineNumber();
	}

	public static String getCallingMethodClassName(int depth) {
		return getCallingMethodElement(depth + 2).getClassName();
	}

	public static File fileFromURIString(String uri) throws URISyntaxException {
		return new File(new java.net.URI(uri));
	}

	/*
	 * The hashCode methods below are subject to this license.
	 * ====================================================================
	 * 
	 * Copyright 1999-2004 The Apache Software Foundation
	 * 
	 * Licensed under the Apache License, Version 2.0 (the "License"); you may
	 * not use this file except in compliance with the License. You may obtain a
	 * copy of the License at
	 * 
	 * http://www.apache.org/licenses/LICENSE-2.0
	 * 
	 * Unless required by applicable law or agreed to in writing, software
	 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
	 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
	 * License for the specific language governing permissions and limitations
	 * under the License.
	 * ====================================================================
	 * 
	 * This software consists of voluntary contributions made by many
	 * individuals on behalf of the Apache Software Foundation. For more
	 * information on the Apache Software Foundation, please see
	 * <http://www.apache.org/>.
	 */

	public static int hashCode(final int seed, final int hashcode) {
		return seed * HASH_OFFSET + hashcode;
	}

	public static int hashCode(final int seed, final Object obj) {
		return hashCode(seed, obj != null ? obj.hashCode() : 0);
	}

	public static int hashCode(final int seed, final boolean b) {
		return hashCode(seed, b ? 1 : 0);
	}

	public String toString() {
		return getClass().toString();
	}

	public String toStringDump(int indent) {
		return toString();
	}

}
