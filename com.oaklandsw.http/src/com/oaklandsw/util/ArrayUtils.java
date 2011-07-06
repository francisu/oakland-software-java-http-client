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

/**
 * <p>
 * Operations on arrays, primitive arrays (like <code>int[]</code>) and
 * primitive wrapper arrays (like <code>Integer[]</code>).
 * </p>
 * 
 * <p>
 * This class tries to handle <code>null</code> input gracefully. An exception
 * will not be thrown for a <code>null</code> array input. However, an Object
 * array that contains a <code>null</code> element may throw an exception. Each
 * method documents its behaviour.
 * </p>
 * 
 * @author Stephen Colebourne
 * @author Moritz Petersen
 * @author <a href="mailto:fredrik@westermarck.com">Fredrik Westermarck </a>
 * @author Nikolay Metchev
 * @author Matthew Hawthorne
 * @author Tim O'Brien
 * @author Pete Gieser
 * @author Gary Gregory
 * @since 2.0
 * @version $Id: ArrayUtils.java,v 1.25 2003/08/22 17:25:33 ggregory Exp $
 */
public class ArrayUtils {

	/**
	 * An empty immutable <code>String</code> array.
	 */
	public static final String[] EMPTY_STRING_ARRAY = new String[0];

	// Reverse
	// -----------------------------------------------------------------------
	/**
	 * <p>
	 * Reverses the order of the given array.
	 * </p>
	 * 
	 * <p>
	 * There is no special handling for multi-dimensional arrays.
	 * </p>
	 * 
	 * <p>
	 * This method does nothing if <code>null</code> array input.
	 * </p>
	 * 
	 * @param array
	 *            the array to reverse, may be <code>null</code>
	 */
	public static void reverse(final Object[] array) {
		if (array == null) {
			return;
		}
		int i = 0;
		int j = array.length - 1;
		Object tmp;
		while (j > i) {
			tmp = array[j];
			array[j] = array[i];
			array[i] = tmp;
			j--;
			i++;
		}
	}

	/**
	 * <p>
	 * Reverses the order of the given array.
	 * </p>
	 * 
	 * <p>
	 * This method does nothing if <code>null</code> array input.
	 * </p>
	 * 
	 * @param array
	 *            the array to reverse, may be <code>null</code>
	 */
	public static void reverse(final long[] array) {
		if (array == null) {
			return;
		}
		int i = 0;
		int j = array.length - 1;
		long tmp;
		while (j > i) {
			tmp = array[j];
			array[j] = array[i];
			array[i] = tmp;
			j--;
			i++;
		}
	}

	/**
	 * <p>
	 * Reverses the order of the given array.
	 * </p>
	 * 
	 * <p>
	 * This method does nothing if <code>null</code> array input.
	 * </p>
	 * 
	 * @param array
	 *            the array to reverse, may be <code>null</code>
	 */
	public static void reverse(final int[] array) {
		if (array == null) {
			return;
		}
		int i = 0;
		int j = array.length - 1;
		int tmp;
		while (j > i) {
			tmp = array[j];
			array[j] = array[i];
			array[i] = tmp;
			j--;
			i++;
		}
	}

	/**
	 * <p>
	 * Reverses the order of the given array.
	 * </p>
	 * 
	 * <p>
	 * This method does nothing if <code>null</code> array input.
	 * </p>
	 * 
	 * @param array
	 *            the array to reverse, may be <code>null</code>
	 */
	public static void reverse(final short[] array) {
		if (array == null) {
			return;
		}
		int i = 0;
		int j = array.length - 1;
		short tmp;
		while (j > i) {
			tmp = array[j];
			array[j] = array[i];
			array[i] = tmp;
			j--;
			i++;
		}
	}

	/**
	 * <p>
	 * Reverses the order of the given array.
	 * </p>
	 * 
	 * <p>
	 * This method does nothing if <code>null</code> array input.
	 * </p>
	 * 
	 * @param array
	 *            the array to reverse, may be <code>null</code>
	 */
	public static void reverse(final char[] array) {
		if (array == null) {
			return;
		}
		int i = 0;
		int j = array.length - 1;
		char tmp;
		while (j > i) {
			tmp = array[j];
			array[j] = array[i];
			array[i] = tmp;
			j--;
			i++;
		}
	}

	/**
	 * <p>
	 * Reverses the order of the given array.
	 * </p>
	 * 
	 * <p>
	 * This method does nothing if <code>null</code> array input.
	 * </p>
	 * 
	 * @param array
	 *            the array to reverse, may be <code>null</code>
	 */
	public static void reverse(final byte[] array) {
		if (array == null) {
			return;
		}
		int i = 0;
		int j = array.length - 1;
		byte tmp;
		while (j > i) {
			tmp = array[j];
			array[j] = array[i];
			array[i] = tmp;
			j--;
			i++;
		}
	}

	/**
	 * <p>
	 * Reverses the order of the given array.
	 * </p>
	 * 
	 * <p>
	 * This method does nothing if <code>null</code> array input.
	 * </p>
	 * 
	 * @param array
	 *            the array to reverse, may be <code>null</code>
	 */
	public static void reverse(final double[] array) {
		if (array == null) {
			return;
		}
		int i = 0;
		int j = array.length - 1;
		double tmp;
		while (j > i) {
			tmp = array[j];
			array[j] = array[i];
			array[i] = tmp;
			j--;
			i++;
		}
	}

	/**
	 * <p>
	 * Reverses the order of the given array.
	 * </p>
	 * 
	 * <p>
	 * This method does nothing if <code>null</code> array input.
	 * </p>
	 * 
	 * @param array
	 *            the array to reverse, may be <code>null</code>
	 */
	public static void reverse(final float[] array) {
		if (array == null) {
			return;
		}
		int i = 0;
		int j = array.length - 1;
		float tmp;
		while (j > i) {
			tmp = array[j];
			array[j] = array[i];
			array[i] = tmp;
			j--;
			i++;
		}
	}

	/**
	 * <p>
	 * Reverses the order of the given array.
	 * </p>
	 * 
	 * <p>
	 * This method does nothing if <code>null</code> array input.
	 * </p>
	 * 
	 * @param array
	 *            the array to reverse, may be <code>null</code>
	 */
	public static void reverse(final boolean[] array) {
		if (array == null) {
			return;
		}
		int i = 0;
		int j = array.length - 1;
		boolean tmp;
		while (j > i) {
			tmp = array[j];
			array[j] = array[i];
			array[i] = tmp;
			j--;
			i++;
		}
	}

}
