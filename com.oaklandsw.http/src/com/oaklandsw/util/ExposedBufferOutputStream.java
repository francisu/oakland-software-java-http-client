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
import java.io.OutputStream;

public class ExposedBufferOutputStream extends OutputStream {
	public byte _buffer[];

	protected int _count;

	protected OutputStream _outStr;

	public ExposedBufferOutputStream(OutputStream outStr, int size) {
		super();
		_buffer = new byte[size];
		_outStr = outStr;
	}

	protected void flushBuffer() throws IOException {
		if (_count > 0) {
			_outStr.write(_buffer, 0, _count);
			_count = 0;
		}
	}

	public void write(int b) throws IOException {
		Util.impossible("Don't use this one");
	}

	public void write(byte b[]) throws IOException {
		write(b, 0, b.length);
	}

	public void write(byte b[], int off, int len) throws IOException {
		if (len >= _buffer.length) {
			countFlush();
			flushBuffer();
			_outStr.write(b, off, len);
			return;
		}
		if (len > _buffer.length - _count) {
			countFlush();
			flushBuffer();
		}
		System.arraycopy(b, off, _buffer, _count, len);
		_count += len;
	}

	// Counts unforced buffer flushes
	protected void countFlush() {
		// may be subclassed
	}

	public void flush() throws IOException {
		flushBuffer();
		_outStr.flush();
	}

}
