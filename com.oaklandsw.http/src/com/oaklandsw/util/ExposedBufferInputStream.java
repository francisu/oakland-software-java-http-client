/*
 * Copyright 2007 Oakland Software Incorporated. All rights Reserved.
 */
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

/**
 * A simple buffered input stream that allows direct access to the buffer. The
 * user reads bytes from the buffer and calls fill() when the buffer is empty.
 */
public class ExposedBufferInputStream extends InputStream {

	public byte[] _buffer;

	// Pointer to the next byte to be read
	public int _pos;

	// Number of bytes actually in the buffer, can be less than
	// the buffer size in the event of a short read.
	public int _used;

	public InputStream _parent;

	public ExposedBufferInputStream(InputStream parent, int size) {
		_parent = parent;
		_buffer = new byte[size];
	}

	public void fill() throws IOException {
		if (_buffer == null) {
			throw new IOException("Stream closed");
		}
		fillInternal();
	}

	private void fillInternal() throws IOException {
		_used = _parent.read(_buffer);
		_pos = 0;
	}

	public int read() throws IOException {
		if (_buffer == null) {
			throw new IOException("Stream closed");
		}
		if (_pos >= _used) {
			fillInternal();
		}
		if (_used == -1) {
			_buffer = null;
			return -1;
		}

		// Make sure we don't get a negative value
		return 0xff & _buffer[_pos++];
	}

	public int read(byte b[]) throws IOException {
		return read(b, 0, b.length);
	}

	public int read(byte b[], int off, int len) throws IOException {
		if (_buffer == null) {
			throw new IOException("Stream closed");
		}

		// We are empty
		if (_pos >= _used) {
			// Bypass our buffer
			if (len >= _buffer.length) {
				return _parent.read(b, off, len);
			}
			fillInternal();
			if (_used == -1) {
				_buffer = null;
				return -1;
			}
		}

		// Not empty, return what we have
		int avail = _used - _pos;
		int count = (avail > len) ? len : avail;
		System.arraycopy(_buffer, _pos, b, off, count);
		_pos += count;
		return count;
	}

	public void close() throws IOException {
		_buffer = null;
		_parent.close();
	}

}
