package org.bouncycastle.crypto.params;

import org.bouncycastle.crypto.CipherParameters;

public class KeyParameter implements CipherParameters {
	private byte[] key;

	public KeyParameter(byte[] key1) {
		this(key1, 0, key1.length);
	}

	public KeyParameter(byte[] key1, int keyOff, int keyLen) {
		this.key = new byte[keyLen];

		System.arraycopy(key1, keyOff, this.key, 0, keyLen);
	}

	public byte[] getKey() {
		return key;
	}
}
