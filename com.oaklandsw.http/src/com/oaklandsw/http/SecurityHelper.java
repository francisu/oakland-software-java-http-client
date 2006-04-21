// Copyright 2002 (c) oakland software, All rights reserved

package com.oaklandsw.http;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

public class SecurityHelper
{

    // Takes a 7-byte quantity and returns a valid 8-byte DES key.
    // The input and output bytes are big-endian, where the most significant
    // byte is in element 0.
    public static byte[] addDesParity(byte[] in)
    {
        byte[] result = new byte[8];

        // Keeps track of the bit position in the result
        int resultIx = 1;

        // Used to keep track of the number of 1 bits in each 7-bit chunk
        int bitCount = 0;

        // Process each of the 56 bits
        for (int i = 0; i < 56; i++)
        {
            // Get the bit at bit position i
            boolean bit = (in[6 - i / 8] & (1 << (i % 8))) > 0;

            // If set, set the corresponding bit in the result
            if (bit)
            {
                result[7 - resultIx / 8] |= (1 << (resultIx % 8)) & 0xFF;
                bitCount++;
            }

            // Set the parity bit after every 7 bits
            if ((i + 1) % 7 == 0)
            {
                if (bitCount % 2 == 0)
                {
                    // Set low-order bit (parity bit) if bit count is even
                    result[7 - resultIx / 8] |= 1;
                }
                resultIx++;
                bitCount = 0;
            }
            resultIx++;
        }
        return result;
    }

    /**
     * Compute a 8 byte DES key based on a 7 byte input, adjusting the parity as
     * necessary.
     */
    public static KeySpec makeDesKeySpec(byte[] key_56)
    {
        byte[] key = addDesParity(key_56);

        /***********************************************************************
         * 
         * key[0] = key_56[0]; key[1] = (byte)(((key_56[0] < < 7) & 0xFF) |
         * (key_56[1] >> 1)); key[2] = (byte)(((key_56[1] < < 6) & 0xFF) |
         * (key_56[2] >> 2)); key[3] = (byte)(((key_56[2] < < 5) & 0xFF) |
         * (key_56[3] >> 3)); key[4] = (byte)(((key_56[3] < < 4) & 0xFF) |
         * (key_56[4] >> 4)); key[5] = (byte)(((key_56[4] < < 3) & 0xFF) |
         * (key_56[5] >> 5)); key[6] = (byte)(((key_56[5] < < 2) & 0xFF) |
         * (key_56[6] >> 6)); key[7] = (byte)((key_56[6] < < 1) & 0xFF);
         **********************************************************************/

        DESKeySpec keySpec = null;
        try
        {
            keySpec = new DESKeySpec(key);
        }
        catch (InvalidKeyException ex)
        {
            throw new RuntimeException("(bug) - " + ex);
        }

        // Check to see that the parity was set correctly, this
        // should never fail
        /**
         * try { DESKeySpec.isParityAdjusted(key, 0); } catch
         * (InvalidKeyException ex) { throw new RuntimeException("(bug) - " +
         * ex); }
         */

        // FIXME - need to makes sure the parity is right
        return keySpec;
    }

    /**
     * Uses DES to encrypt a key and data.
     * 
     * @param outBytes
     *            the output array where the 8 byte encrypted value is stored.
     * @param inKey
     *            the input array that contains the 7 byte key is found
     * @param outOffset
     *            the offset in outBytes where the encrypted result is stored.
     * @param inOffset
     *            the offser in inKey where the key is found.
     * @param data
     *            8 bytes of data to be encrypted.
     */
    public static void desEncrypt(byte[] outBytes,
                                  byte[] inKey,
                                  int outOffset,
                                  int inOffset,
                                  byte[] data)
    {
        // Get the bytes we are using for the key
        byte[] partInBytes = new byte[7];
        System.arraycopy(inKey, inOffset, partInBytes, 0, 7);

        Cipher desCipher = null;
        try
        {
            desCipher = Cipher.getInstance("DES");
        }
        catch (NoSuchPaddingException ex)
        {
            throw new RuntimeException("(bug) - " + ex);
        }
        catch (NoSuchAlgorithmException ex)
        {
            throw new RuntimeException("(bug) - " + ex);
        }

        KeySpec keySpec = SecurityHelper.makeDesKeySpec(partInBytes);
        SecretKey key = null;
        try
        {
            key = SecretKeyFactory.getInstance("DES").generateSecret(keySpec);
        }
        catch (NoSuchAlgorithmException ex)
        {
            throw new RuntimeException("(bug) - " + ex);
        }
        catch (InvalidKeySpecException ex)
        {
            throw new RuntimeException("(bug) - " + ex);
        }

        try
        {
            desCipher.init(Cipher.ENCRYPT_MODE, key);
        }
        catch (InvalidKeyException ex)
        {
            throw new RuntimeException("(bug) - " + ex);
        }

        // Get the encrypted 8 byte result
        byte[] partOutBytes = null;
        try
        {
            partOutBytes = desCipher.doFinal(data);
        }
        catch (BadPaddingException ex)
        {
            throw new RuntimeException("(bug) - " + ex);
        }
        catch (IllegalBlockSizeException ex)
        {
            throw new RuntimeException("(bug) - " + ex);
        }

        // Put it where it wants to go
        System.arraycopy(partOutBytes, 0, outBytes, outOffset, 8);
    }

}
