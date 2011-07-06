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

import com.oaklandsw.utillog.Log;

public class LogToNull implements Log {

	public void debug(Object message) {
	}

	public void debug(Object message, Throwable t) {
	}

	public void error(Object message) {
	}

	public void error(Object message, Throwable t) {
	}

	public void fatal(Object message) {
	}

	public void fatal(Object message, Throwable t) {
	}

	public void info(Object message) {
	}

	public void info(Object message, Throwable t) {
	}

	public boolean isDebugEnabled() {
		return false;
	}

	public boolean isErrorEnabled() {
		return false;
	}

	public boolean isFatalEnabled() {
		return false;
	}

	public boolean isInfoEnabled() {
		return false;
	}

	public boolean isTraceEnabled() {
		return false;
	}

	public boolean isWarnEnabled() {
		return false;
	}

	public void trace(Object message) {
	}

	public void trace(Object message, Throwable t) {
	}

	public void warn(Object message) {
	}

	public void warn(Object message, Throwable t) {
	}

}
