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

package com.oaklandsw;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.oaklandsw.util.LogUtils;
import com.oaklandsw.util.log.Log4JLogger;
import com.oaklandsw.utillog.Log;

public class TestCaseBase extends TestCase {
	private static final Log _log = LogUtils.makeLogger();

	public TestCaseBase(String testName) {
		super(testName);
	}

	static {
		LogUtils.checkInitialLogging(LogManager.class, Logger.class,
				Log4JLogger.class);
	}

	protected void setUp() throws Exception {
		Thread.currentThread().setContextClassLoader(
				getClass().getClassLoader());
	}

	protected void tearDown() throws Exception {
	}

	// True if the first arg contains the 2nd arg
	protected static void assertContains(String checkText, String containsText) {
		if (checkText == null) {
			fail("Text: " + containsText
					+ ", found null error message (text to check)");
		}

		if (checkText.indexOf(containsText) == -1) {
			fail("Text '" + checkText + "' does not contain '" + containsText
					+ "'");
		}
	}

	public static void mainRun(Test test, String[] args) {
		String testMethod = null;
		TestSuite suite = (TestSuite) test;

		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-m")) {
				testMethod = args[++i];
			}
		}

		// Select the test to run by method name
		if (testMethod != null) {
			int testCount = suite.testCount();
			boolean found = false;
			for (int i = 0; i < testCount; i++) {
				TestCase testCase = (TestCase) suite.testAt(i);
				if (testMethod.equals(testCase.getName())) {
					test = testCase;
					found = true;
					break;
				}
			}
			if (!found) {
				throw new IllegalArgumentException("Test method " + testMethod
						+ " not found");
			}
		}

		junit.textui.TestRunner.run(test);
	}

}
