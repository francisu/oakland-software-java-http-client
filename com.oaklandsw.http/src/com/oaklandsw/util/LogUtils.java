/*
 * Copyright 2006 oakland software, incorporated. All rights Reserved.
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.security.AccessControlException;
import java.util.Enumeration;
import java.util.Properties;

import com.oaklandsw.utillog.Log;

/**
 * Log level utility methods for logging.
 * 
 * NOTE - we can't directly reference log4j from here because this class is used
 * by the HTTP client and log4j may not be present; so we have to reference it
 * using reflection.
 */
public class LogUtils {

	public static final String LOG_PREFIX = "com.oaklandsw";
	public static final String LOG_WIRE_PREFIX = ".http.wireLog";
	public static final String LOG_CONN_PREFIX = ".http.connLog";

	public static final String DEBUG_PATTERN = "%-4r %5p [%t] %30.30c - %m%n";

	protected static Log _logToNullInstance = new LogToNull();

	// If the logging is to show passwords, by default we don't
	public static boolean _logShowPasswords;

	protected static Method _getLoggerMethod;

	// This is the log4j LogManager
	protected static Class _logManagerClass;

	protected static Constructor _log4jLoggerConst;

	// If no appenders, then we are not getting logging configured
	// from the environment, so turn off logging. If we don't do
	// this, the root logger is set to debug, so we will always
	// go through the debug path in logging, which is slower
	public static void checkInitialLogging(Class logManagerClass,
			Class loggerClass, Class logWrapperClass) {
		if (logManagerClass != null) {
			_logManagerClass = logManagerClass;
		}

		ClassLoader cl = LogUtils.class.getClassLoader();
		try {
			if (_logManagerClass == null) {
				_logManagerClass = cl.loadClass("org.apache.log4j.LogManager");
				if (_logManagerClass == null) {
					logNone();
					return;
				}
			}

			if (logWrapperClass == null) {
				logNone();
				return;
			}

			if (loggerClass == null) {
				loggerClass = cl.loadClass("org.apache.log4j.Logger");
			}

			_log4jLoggerConst = logWrapperClass
					.getConstructor(new Class[] { loggerClass });

			_getLoggerMethod = _logManagerClass.getMethod("getLogger",
					new Class[] { String.class });

			Method getCurrentMethod = _logManagerClass.getMethod(
					"getCurrentLoggers", new Class[] {});
			Enumeration enLoggers = (Enumeration) getCurrentMethod.invoke(null,
					new Object[] {});
			if (!enLoggers.hasMoreElements()) {
				logNone();
				return;
			}
			Method getAllMethod = loggerClass.getMethod("getAllAppenders",
					new Class[] {});
			while (enLoggers.hasMoreElements()) {
				Object logger = enLoggers.nextElement();
				Enumeration en = (Enumeration) getAllMethod.invoke(logger,
						new Object[] {});
				// Found an appender
				if (en.hasMoreElements()) {
					return;
				}
			}

			Method getRootMethod = _logManagerClass.getMethod("getRootLogger",
					new Class[] {});
			Object logger = getRootMethod.invoke(null, new Object[] {});
			Enumeration en = (Enumeration) getAllMethod.invoke(logger,
					new Object[] {});
			// Found an appender
			if (en.hasMoreElements()) {
				return;
			}

			// No appenders in any loggers
			logNone();
		} catch (ClassNotFoundException e) {
			// Ignored, means no log4j is present
		} catch (AccessControlException e) {
			// Ignored, means in an environment where we can't do this
		} catch (RuntimeException e) {
			// Ignored, can happen in a javaws situation where the jar file
			// depends on the HTTP client (which is set by the system property)
			// to load (for the classloader). We will get an NPE in our stuff
			// because the logging stuff is not initialized correctly. But
			// we don't care.
			if (false) {
				System.out.println("runtime exception: " + e);
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		} catch (Exception e) {
			System.out.println("exception: " + e);
			e.printStackTrace();
			// Something else is wrong
			// Util.impossible(e);
		}
	}

	public static void configureLog(Properties props) {
		String appender = props.getProperty("appender");

		Enumeration keys = props.keys();
		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			String propValue = props.getProperty(key);
			if (propValue != null) {
				propValue = StringUtils.replace(propValue, "@user.home",
						System.getProperty("user.home"));

				props.setProperty(key, propValue);
				if (appender != null) {
					propValue = StringUtils.replace(propValue, "@appender",
							appender);
					props.setProperty(key, propValue);
				}
			}
		}

		ClassLoader cl = LogUtils.class.getClassLoader();
		if (_logManagerClass != null) {
			cl = _logManagerClass.getClassLoader();
		}
		Class loggerClass = null;
		try {
			loggerClass = cl.loadClass("org.apache.log4j.PropertyConfigurator");
			Method configMethod = loggerClass.getMethod("configure",
					new Class[] { Properties.class });
			configMethod.invoke(null, new Object[] { props });
		} catch (ClassNotFoundException e) {
			// Ignored, means no log4j is present
		} catch (RuntimeException e) {
			throw new RuntimeException();
		} catch (Exception e) {
			// Something else is wrong
			Util.impossible(e);
		}
	}

	public static void logAll() {
		logConsole(LOG_PREFIX);
	}

	public static void logWireOnly() {
		logConsole(LOG_PREFIX + LOG_WIRE_PREFIX);
	}

	public static void logConnOnly() {
		logConsole(LOG_PREFIX + LOG_CONN_PREFIX);
	}

	public static void logConsole(String logWhat) {
		Properties logProps = new Properties();
		logProps.setProperty("log4j.logger." + logWhat, "TRACE, A1");
		setConsoleProps(logProps);
	}

	public static void setConsoleProps(Properties logProps) {
		logProps.setProperty("log4j.appender.A1",
				"org.apache.log4j.ConsoleAppender");
		logProps.setProperty("log4j.appender.A1.layout",
				"org.apache.log4j.PatternLayout");
		logProps.setProperty("log4j.appender.A1.layout.ConversionPattern",
				DEBUG_PATTERN);
		configureLog(logProps);
	}

	public static void logFile(String fileName) {
		logFile(fileName, LOG_PREFIX);
	}

	public static void logConnFile(String fileName) {
		logFile(fileName, LOG_PREFIX + LOG_CONN_PREFIX);
	}

	public static void logWireFile(String fileName) {
		logFile(fileName, LOG_PREFIX + LOG_WIRE_PREFIX);
	}

	public static void logFile(String fileName, String logWhat) {
		Properties logProps = new Properties();
		logProps.setProperty("log4j.logger." + logWhat, "TRACE, A1");
		logProps.setProperty("log4j.appender.A1",
				"org.apache.log4j.FileAppender");
		logProps.setProperty("log4j.appender.A1.File", fileName);
		logProps.setProperty("log4j.appender.A1.Append", "false");
		logProps.setProperty("log4j.appender.A1.layout",
				"org.apache.log4j.PatternLayout");
		logProps.setProperty("log4j.appender.A1.layout.ConversionPattern",
				DEBUG_PATTERN);
		configureLog(logProps);
	}

	public static void logNone() {
		Properties logProps = new Properties();
		// This may turn of other people's logging and we don't want that
		// REMOVEME
		// logProps.setProperty("log4j.rootLogger", "OFF");
		logProps.setProperty("log4j.logger." + LOG_PREFIX, "OFF");
		configureLog(logProps);
	}

	public static Log makeLogger(String logName) {
		if (_logManagerClass == null || _log4jLoggerConst == null
				|| _getLoggerMethod == null) {
			return _logToNullInstance;
		}

		Object logger;
		try {
			logger = _getLoggerMethod.invoke(null, new Object[] { logName });
			return (Log) _log4jLoggerConst.newInstance(new Object[] { logger });
		} catch (Exception e) {
			System.err.println("Error initializing logger for: " + logName);
			e.printStackTrace();
		}
		return _logToNullInstance;
	}

	public static Log makeLogger() {
		String className = Util.getCallingMethodClassName(1);
		return makeLogger(className);
	}

	public static Log makeLogger(Class cls) {
		return makeLogger(cls.getName());
	}

}
