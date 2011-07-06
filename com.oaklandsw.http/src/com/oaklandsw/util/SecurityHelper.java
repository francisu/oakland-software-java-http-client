// Copyright 2002 (c) oakland software, All rights reserved

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

import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.DESEngine;
import org.bouncycastle.crypto.engines.RijndaelEngine;
import org.bouncycastle.crypto.params.DESParameters;
import org.bouncycastle.crypto.params.KeyParameter;

public class SecurityHelper {
	public static final boolean ENCRYPT = true;

	/**
	 * Uses DES to encrypt a key and data.
	 * 
	 * @param outBytes
	 *            the output array where the 8 byte encrypted value is stored.
	 * @param inKey
	 *            the input array that contains the 7 byte key is found
	 * @param outOffset
	 *            the offset in outBytes where the encrypted result is stored.
	 * @param inBytesdata
	 *            8 bytes of data to be encrypted.
	 */
	public static void des(boolean encrypt, byte[] inBytes, byte[] key,
			byte[] outBytes, int outOffset) {
		BufferedBlockCipher cipher = new BufferedBlockCipher(new DESEngine());

		cipher.init(encrypt, new DESParameters(key));

		byte[] partOutBytes = new byte[inBytes.length];

		int len1 = cipher.processBytes(inBytes, 0, inBytes.length,
				partOutBytes, 0);

		try {
			cipher.doFinal(partOutBytes, len1);
		} catch (DataLengthException e) {
			Util.impossible(e);
		} catch (IllegalStateException e) {
			Util.impossible(e);
		} catch (InvalidCipherTextException e) {
			Util.impossible(e);
		}

		// Put it where it wants to go
		System.arraycopy(partOutBytes, 0, outBytes, outOffset, 8);
	}

	/**
	 * Uses Rijndael to encrypt/decrypt a key and data.
	 * 
	 * @param outBytes
	 *            the output value
	 * @param key
	 *            the key
	 * @param inBytes
	 *            the input value
	 */
	public static void rijndael(boolean encrypt, byte[] inBytes, byte[] key,
			byte[] outBytes) {
		BufferedBlockCipher cipher = new BufferedBlockCipher(
				new RijndaelEngine(128));

		cipher.init(encrypt, new KeyParameter(key));

		int len1 = cipher.processBytes(inBytes, 0, inBytes.length, outBytes, 0);

		try {
			cipher.doFinal(outBytes, len1);
		} catch (DataLengthException e) {
			Util.impossible(e);
		} catch (IllegalStateException e) {
			Util.impossible(e);
		} catch (InvalidCipherTextException e) {
			Util.impossible(e);
		}
	}

	public static byte[] fillToBlockSize(byte[] buffer, int size) {
		int extra = buffer.length % size;
		if (extra == 0) {
			return buffer;
		}
		byte[] retBuff = new byte[buffer.length + (size - extra)];
		System.arraycopy(buffer, 0, retBuff, 0, buffer.length);
		return retBuff;
	}

	public static int getRijndaelBlockSize() {
		BufferedBlockCipher cipher = new BufferedBlockCipher(
				new RijndaelEngine(128));
		return cipher.getBlockSize();
	}

}
