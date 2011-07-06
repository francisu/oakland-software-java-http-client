// Copyright 2003, 2010 oakland software, All rights reserved

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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;

import org.xml.sax.InputSource;

import com.oaklandsw.utillog.Log;

public class FileUtils {
	private static final Log _log = LogUtils.makeLogger();

	// Works around a problem in Windows where a file might not
	// be deleted the first time, but it can be deleted a bit later
	private static final int NUM_TRIES = 3;

	public static boolean deleteFile(File file) {
		int count = 0;
		if (SystemUtils.IS_OS_WINDOWS) {
			count = NUM_TRIES;
		}
		boolean deleted = false;

		if (file.isDirectory()) {
			File[] members = file.listFiles();
			for (int i = 0; i < members.length; i++) {
				deleteFile(members[i]);
			}
		}

		while (!(deleted = file.delete())) {
			// It may not have existed, deleted will still return false
			if (!file.exists()) {
				return true;
			}

			if (count <= 0) {
				/*
				 * This seems to happen when running with the Mule IDE so we
				 * don't want to make a big deal of it.
				 */
				_log.debug("FAILED - Delete of: " + file
						+ " failed - giving up after " + NUM_TRIES + " tries");
				break;
			}

			_log.debug("Count: " + count + " Delete of: " + file
					+ " failed - waiting");

			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// Just go on
			}
			count--;
		}
		return deleted;
	}

	public static void emptyDirectory(File file) {
		if (file.isDirectory()) {
			File[] members = file.listFiles();
			for (int i = 0; i < members.length; i++) {
				deleteFile(members[i]);
			}
		}
	}

	// Works around Sun bug #6213298
	public static boolean renameFileTo(File file, File toFile) {
		boolean renamed = false;

		if (!(renamed = file.renameTo(toFile))) {
			// Try coping and deleting
			try {
				copyFile(file, toFile);
				renamed = deleteFile(file);
			} catch (IOException e1) {
				_log.error("FAILED - Rename of: " + file
						+ " failed - copy failed", e1);
				return false;
			}
		}
		return renamed;
	}

	/**
	 * Count the number of lines in a file.
	 */
	public static int countLines(File file) throws FileNotFoundException,
			IOException {
		InputStream is = new BufferedInputStream(new FileInputStream(file));

		int lineCount = 0;
		while (Util.readLine(is) != null) {
			lineCount++;
		}

		is.close();
		return lineCount;
	}

	public static String removeExtension(String fileName) {
		int dot = fileName.lastIndexOf('.');
		// Handles unix style filenames that start with a dot (index 0)
		if (dot <= 0) {
			return fileName;
		}
		return fileName.substring(0, dot);
	}

	// Timeout is in ms
	public static void waitForFile(File file, int timeout) {
		long endTime = System.currentTimeMillis() + timeout;
		while (true) {
			if (file.exists())
				return;
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
			if (System.currentTimeMillis() > endTime)
				throw new RuntimeException("Timed out waiting for file: "
						+ file);
		}
	}

	public static String getStringFromFile(File file) throws IOException {
		InputStream is = new BufferedInputStream(new FileInputStream(file));
		String ret = Util.getStringFromInputStream(is);
		is.close();
		return ret;
	}

	public static byte[] getBytesFromFile(File file) throws IOException {
		InputStream is = new BufferedInputStream(new FileInputStream(file));
		byte[] ret = Util.getBytesFromInputStream(is);
		is.close();
		return ret;
	}

	public static InputSource getInputSource(File file) throws IOException {
		return new InputSource(new BufferedReader(new InputStreamReader(
				new FileInputStream(file))));
	}

	public static void putStringToFilePlatformEncoding(File file, String string)
			throws IOException {
		putStringToFile(file, string, null);
	}

	public static void putStringToFileDefaultEncoding(File file, String string)
			throws IOException {
		putStringToFile(file, string, Util.UTF8_ENCODING);
	}

	public static void putStringToFile(File file, String string, String encoding)
			throws IOException {
		OutputStream outStream = new BufferedOutputStream(new FileOutputStream(
				file));
		OutputStreamWriter outWriter;
		if (encoding != null) {
			outWriter = new OutputStreamWriter(outStream, encoding);
		} else {
			outWriter = new OutputStreamWriter(outStream);
		}
		// Allow an empty string
		if (string != null) {
			outWriter.write(string, 0, string.length());
		}
		outWriter.close();
		outStream.close();
	}

	public static void putBytesToFile(File file, byte[] bytes)
			throws IOException {
		OutputStream outStream = new BufferedOutputStream(new FileOutputStream(
				file));
		// Allow an empty string
		if (bytes != null) {
			outStream.write(bytes, 0, bytes.length);
		}
		outStream.close();
	}

	/**
	 * Compare two files and return true if they are equal.
	 */
	public static boolean compareFiles(File file1, File file2)
			throws IOException {
		String f1 = Util.getStringFromInputStream(new BufferedInputStream(
				new FileInputStream(file1)));
		String f2 = Util.getStringFromInputStream(new BufferedInputStream(
				new FileInputStream(file2)));
		// Fixup for windows strings
		// TODO - find a better way
		f1 = StringUtils.replace(f1, "\r", "");
		f2 = StringUtils.replace(f2, "\r", "");
		return f1.equals(f2);
	}

	/**
	 * Compare a string to the contents of a file.
	 */
	public static boolean compareStringToFile(String s1, File file2)
			throws IOException {
		return s1.equals(getStringFromFile(file2));
	}

	/**
	 * Copies the first file to the second.
	 */
	public static void copyFile(File fromFile, File toFile) throws IOException {
		InputStream is = new BufferedInputStream(new FileInputStream(fromFile));
		OutputStream os = new BufferedOutputStream(new FileOutputStream(toFile));
		Util.copyStreams(is, os);
		is.close();
		os.close();
	}

	/**
	 * Copies the InputStream to the file
	 */
	public static void copyFile(InputStream fromStream, File toFile)
			throws IOException {
		InputStream is = new BufferedInputStream(fromStream);
		OutputStream os = new BufferedOutputStream(new FileOutputStream(toFile));
		Util.copyStreams(is, os);
		is.close();
		os.close();
	}

	// Copies all files under srcDir to dstDir.
	// If dstDir does not exist, it will be created.
	public static void copyDirectory(File srcDir, File dstDir)
			throws IOException {
		srcDir = srcDir.getCanonicalFile();
		dstDir = dstDir.getCanonicalFile();
		if (srcDir.isDirectory()) {
			if (!dstDir.exists()) {
				dstDir.mkdir();
			}

			String[] children = srcDir.list();
			for (int i = 0; i < children.length; i++) {
				copyDirectory(new File(srcDir, children[i]), new File(dstDir,
						children[i]));
			}
		} else {
			copyFile(srcDir, dstDir);
		}
	}

	// Abstracted to fix a bug in < JRE 1.4 systems of handling
	// spaces in URLs
	public static InputStream openStream(URL url) throws IOException {
		if (SystemUtils.isJavaVersionAtLeast(1.41f)) {
			return url.openStream();
		}

		String path = url.getFile();
		if (url.getProtocol().toLowerCase().startsWith("file") && path != null
				&& path.indexOf("%20") >= 0) {
			// This is a bug in the earlier versions of the
			// JRE for file URLs
			path = StringUtils.replace(path, "%20", " ");
			File file = new File(path);
			return new BufferedInputStream(new FileInputStream(file));
		}

		return url.openStream();
	}

}
