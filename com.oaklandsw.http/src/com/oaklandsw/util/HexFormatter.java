//
// Copyright 2006-2007 Oakland Software Incorporated
//
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

package com.oaklandsw.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;

import com.oaklandsw.util.Util.Range;

public class HexFormatter {

	protected final static char[] hexChars = { '0', '1', '2', '3', '4', '5',
			'6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

	public static final boolean LINE_POSITIONS = true;

	public boolean _linePositions;

	// The number of characters consumed by the line positions
	public int _linePositionSize;

	public String _formattedString;

	public byte[] _inputBytes;

	public String _encoding;

	public static HexFormatter fromInputStream(InputStream is,
			boolean linePositions, String encoding) throws IOException {
		HexFormatter hf = new HexFormatter();
		hf._inputBytes = Util.getBytesFromInputStream(is);
		hf._encoding = encoding;
		hf.format(linePositions);
		return hf;
	}

	// public static String fromInputStreamLinePositions(InputStream is)
	// throws IOException
	// {
	// byte[] bytes = Util.getBytesFromInputStream(is);
	// return dumpOnlyLinePositions(bytes, 0, bytes.length);
	// }

	public static String dump(byte[] data) {
		return dump(data, LINE_POSITIONS);
	}

	public static String dump(byte[] data, boolean linePositions) {
		return dump(data, linePositions, null);
	}

	public static String dump(byte[] data, boolean linePositions,
			String encoding) {
		HexFormatter hf = new HexFormatter();
		hf._encoding = encoding;
		hf._inputBytes = data;
		hf.format(0, data.length, linePositions);
		return hf._formattedString;
	}

	protected static final int BYTES_PER_ROW = 16;

	// Bytes for each section of the hex display
	protected static final int BYTES_PER_HEX_CHUNK = 4;

	// Gap between the hex and char display portion
	protected static final int ROW_HEX_CHAR_GAP = 2;

	// Space and nl at the end
	protected static final int ROW_END_CHARS = 2;

	// The number of characters in the char display portion
	protected static final int ROW_CHAR_SIZE = BYTES_PER_ROW;

	// Total number of characters in each row
	public static final int ROW_DISPLAY_SIZE = BYTES_PER_ROW * 3 // 2 for
			// the
			// hex, 1 for
			// the ASCII
			+ ROW_HEX_CHAR_GAP + 3 // spaces between
			// chunks
			+ ROW_END_CHARS;

	// The number of characters in the HEX portion of the display
	protected static final int ROW_HEX_SIZE = ROW_DISPLAY_SIZE - ROW_CHAR_SIZE
			- ROW_HEX_CHAR_GAP - ROW_END_CHARS;

	public void format(boolean linePositions) {
		format(0, _inputBytes.length, linePositions);
	}

	public void format(int offset, int len, boolean linePositions) {
		byte data[] = _inputBytes;
		if (_encoding == null) {
			_encoding = Util.ASCII_ENCODING;
		}
		linePositionSize();
		try {
			_formattedString = null;

			if (data == null) {
				_formattedString = "null";
				return;
			}

			char[] chars = new char[BYTES_PER_ROW];

			StringBuffer out = new StringBuffer(256);

			int lineNumber = 0;

			for (int i = offset; i < offset + len;) {
				// offset
				if (linePositions) {
					out.append(getLinePositionString(lineNumber++));
					out.append(":  ");
				}

				// hexbytes
				for (int j = 0; j < BYTES_PER_ROW; j++, i++) {
					if (i < data.length) {
						out.append(hexChars[(data[i] >> 4) & 0x0f]);
						out.append(hexChars[data[i] & 0x0f]);

						char c;
						String encodedStr;
						encodedStr = new String(data, i, 1, _encoding);

						// Gracefully handle the case where something went
						// wrong in the encoding
						if (encodedStr.length() == 1) {
							c = encodedStr.charAt(0);
							chars[j] = !Character.isISOControl(c) ? c : '.';
						} else {
							chars[j] = '.';
						}
					} else {
						out.append("  ");
						chars[j] = ' ';
					}

					if (j > 0 && ((j + 1) % 4) == 0) {
						out.append(' ');
					}
				}

				// Text
				out.append(' ').append(chars).append(" \n");
			}

			_formattedString = out.toString();
		} catch (UnsupportedEncodingException ex) {
			Util.impossible(ex);
		}
	}

	// Number of digits in the position indicator column
	public int linePositionSize() {
		int size = _inputBytes.length;

		int digits = 2;
		while (size > Math.pow(16, digits) - 1) {
			++digits;
		}

		return _linePositionSize = digits;
	}

	// The string showing the offset position of the specified line
	public String getLinePositionString(int lineNumber) {
		char[] out = new char[_linePositionSize];
		Arrays.fill(out, ' ');

		int offset = lineNumber * BYTES_PER_ROW;

		for (int i = _linePositionSize - 1; i >= 0; i -= 1) {
			// No leading zeros
			if (offset == 0 && i != _linePositionSize - 1) {
				out[i] = ' ';
			} else {
				out[i] = hexChars[(offset) & 0x0f];
				offset >>= 4;
			}
		}
		return new String(out);
	}

	// The string showing the offset position of the specified line
	public String getLineOffsetPositionString(int lineNumber, int columnNumber) {
		int colOffset = columnToOffset(columnNumber);
		if (colOffset == -1) {
			return "";
		}

		char[] out = new char[_linePositionSize];

		int offset = lineNumber * BYTES_PER_ROW + colOffset;
		for (int i = _linePositionSize - 1; i >= 0; i -= 1) {
			// No leading zeros
			if (offset == 0 && i != _linePositionSize - 1) {
			} else {
				out[i] = hexChars[(offset) & 0x0f];
				offset >>= 4;
			}
		}

		int startInd = 0;
		for (; startInd < out.length; startInd++) {
			if (out[startInd] > 0) {
				break;
			}
		}

		return "0x" + new String(out, startInd, out.length - startInd);
	}

	// Converts the zero based column number to the offset
	// offset is -1 if outside of display
	protected int columnToOffset(int col) {
		int adjustedCol = col;

		// In the gaps
		if (col == BYTES_PER_HEX_CHUNK * 2
				|| col == BYTES_PER_HEX_CHUNK * 4 + 1
				|| col == BYTES_PER_HEX_CHUNK * 6 + 2) {
			return -1;
		}

		// Adjust the column number based on the spacing
		if (col >= BYTES_PER_HEX_CHUNK * 2 + 1) {
			adjustedCol--;
		}
		if (col >= BYTES_PER_HEX_CHUNK * 4 + 1) {
			adjustedCol--;
		}
		if (col >= BYTES_PER_HEX_CHUNK * 6 + 1) {
			adjustedCol--;
		}
		if (col < ROW_HEX_SIZE) {
			return adjustedCol / 2;
		}

		if (col >= ROW_HEX_SIZE && col < ROW_HEX_SIZE + ROW_HEX_CHAR_GAP) {
			return -1;
		}
		if (col >= ROW_HEX_SIZE + ROW_HEX_CHAR_GAP
				&& col < ROW_HEX_SIZE + ROW_HEX_CHAR_GAP + ROW_CHAR_SIZE) {
			return (col - ROW_HEX_SIZE - ROW_HEX_CHAR_GAP);
		}
		return -1;
	}

	// Returns the offset into the hex display corresponding to the
	// specified byteOffset. This assumes that the display was rendered
	// without LINE_POSITIONS.
	public int getHexDisplayOffset(int byteOffset) {
		// The number of full lines before this one
		int lineNumber = byteOffset / BYTES_PER_ROW;

		// The number of characters represented by these full lines
		int lineNumberDisplay = lineNumber * ROW_DISPLAY_SIZE;

		// The position in the line
		int lineOffset = byteOffset % BYTES_PER_ROW;

		// Number of the chunk in the line
		int chunkOffset = lineOffset / BYTES_PER_HEX_CHUNK;

		// Number of characters in this line
		int lineOffsetDisplay = lineOffset * 2 + chunkOffset;

		return lineNumberDisplay + lineOffsetDisplay;
	}

	// Returns the offset into the ascii display corresponding to the
	// specified byteOffset. This assumes that the display was rendered
	// without LINE_POSITIONS.
	public int getAsciiDisplayOffset(int byteOffset) {
		// The number of full lines before this one
		int lineNumber = byteOffset / BYTES_PER_ROW;

		// The number of characters represented by these full lines
		int lineNumberDisplay = lineNumber * ROW_DISPLAY_SIZE;

		// The position in the line
		int lineOffset = byteOffset % BYTES_PER_ROW;

		// Number of characters in this line
		int lineOffsetDisplay = BYTES_PER_ROW * 2 + 3 + 2 + lineOffset;

		return lineNumberDisplay + lineOffsetDisplay;
	}

	public int getFirstLineNumber(int byteOffset) {
		return byteOffset / BYTES_PER_ROW;
	}

	// Returns the list of ranges to highlight between the two byte
	// offset values
	public void getHighlightRanges(List ranges, String displayString,
			int startOffset, int endOffset, int visibleStartLine,
			int visibleNumLines) {
		int startLine = startOffset / BYTES_PER_ROW;
		int endLine = endOffset / BYTES_PER_ROW;
		if (visibleStartLine > startLine) {
			startLine = visibleStartLine;
			startOffset = startLine * BYTES_PER_ROW;
		}
		if (endLine > visibleStartLine + visibleNumLines) {
			endLine = visibleStartLine + visibleNumLines;
			endOffset = endLine * BYTES_PER_ROW;
		}
		if (startLine > endLine) {
			return;
		}

		calcRanges(displayString, ranges, getHexDisplayOffset(startOffset),
				getHexDisplayOffset(endOffset), "  ");
		// System.out.println("hex ranges: " + ranges);
		calcRanges(displayString, ranges, getAsciiDisplayOffset(startOffset),
				getAsciiDisplayOffset(endOffset), " \n");
		// System.out.println("char ranges: " + ranges);
	}

	// Given these display offsets, determine the ranges to highlight
	protected void calcRanges(String displayString, List ranges,
			int startDisplayOffset, int endDisplayOffset, String lineEnd) {
		int displayStringLen = displayString.length();
		while (startDisplayOffset < endDisplayOffset) {
			// End of hex or ascii section
			int endSection = displayString.indexOf(lineEnd, startDisplayOffset);
			endSection = Math.min(endSection, endDisplayOffset);
			ranges.add(new Range(startDisplayOffset, endSection));

			// End of the line
			int nl = displayString.indexOf("\n", startDisplayOffset);
			// Something's wrong, this can be caused by bad encoding
			if (nl == -1) {
				break;
			}
			// Past the new line
			nl++;
			if (nl >= displayStringLen) {
				break;
			}
			// If we are doing ascii, go past the hex stuff
			if (lineEnd.equals(" \n")) {
				nl += ROW_HEX_SIZE + 2;
			}
			startDisplayOffset = nl;
		}
	}

	// Simple format of a byte array into hex
	public static String formatSimple(byte[] data) {
		if (data == null) {
			return "null";
		}

		StringBuffer out = new StringBuffer(256);
		int n = 0;

		for (int i = 0; i < data.length; i++) {
			if (n > 0) {
				out.append(' ');
			}

			out.append(hexChars[(data[i] >> 4) & 0x0f]);
			out.append(hexChars[data[i] & 0x0f]);

			if (++n == 16) {
				out.append('\n');
				n = 0;
			}
		}

		return out.toString();
	}

}
