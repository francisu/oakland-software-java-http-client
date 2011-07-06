/*
 * Copyright 2002, 2003, Oakland Software Incorporated
 * 
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

package com.oaklandsw.http.ntlm;

import java.io.UnsupportedEncodingException;

import com.oaklandsw.utillog.Log;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.MD4Digest;
import org.bouncycastle.crypto.digests.MD5Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.DESParameters;
import org.bouncycastle.crypto.params.KeyParameter;

import com.oaklandsw.util.LogUtils;
import com.oaklandsw.util.SecurityHelper;
import com.oaklandsw.util.Util;

public class NtlmProviderImpl implements NtlmProvider
{
    private static final Log   _log                = LogUtils.makeLogger();

    protected static final int HASHED_PASSWORD_LEN = 21;

    /**
     * Computes the 24 byte ANSI password hash given the 8 byte server
     * challenge.
     */
    public byte[] getAnsiHash(int level,
                              String domain,
                              String username,
                              String password,
                              byte[] challenge,
                              byte[] clientChallenge)
    {
        switch (level)
        {
            case 0:
            case 1:
            case 2:
                return getLMResponse(password, challenge);
            case 3:
            case 4:
            case 5:
                return getLMv2Response(domain,
                                       username,
                                       password,
                                       challenge,
                                       clientChallenge);
            default:
                Util.impossible("Invalid LM level:" + level);
                return null;
        }
    }

    /**
     * Computes the 24 byte Unicode password hash given the 8 byte server
     * challenge.
     */
    public byte[] getUnicodeHash(int level,
                                 String domain,
                                 String username,
                                 String password,
                                 byte[] challenge,
                                 byte[] clientChallenge)
    {
        switch (level)
        {
            case 0:
            case 1:
            case 2:
                return getNTLMResponse(password, challenge);
            case 3:
            case 4:
            case 5:
                byte[] targetInfo = Ntlm.createTargetInfo(domain, null);
                return getNTLMv2Response(domain,
                                         username,
                                         password,
                                         targetInfo,
                                         challenge,
                                         clientChallenge);
            default:
                Util.impossible("Invalid LM level:" + level);
                return null;
        }
    }

    // 
    // Copyright © 2003 Eric Glass (eglass1 at comcast.net).
    //
    // Permission to use, copy, modify, and distribute this document
    // for any purpose and without any fee is hereby granted, provided
    // that the above copyright notice and this list of conditions
    // appear in all copies.
    //
    // Some parts modified by Oakland Software (8 Jul 2006)
    //

    /**
     * Creates a DES encryption key from the given key material.
     * 
     * @param bytes
     *            A byte array containing the DES key material.
     * @param offset
     *            The offset in the given byte array at which the 7-byte key
     *            material starts.
     * 
     * @return A DES encryption key created from the key material starting at
     *         the specified offset in the given byte array.
     */
    private byte[] createDESKey(byte[] bytes, int offset)
    {
        byte[] keyBytes = new byte[7];
        System.arraycopy(bytes, offset, keyBytes, 0, 7);
        byte[] material = new byte[8];
        material[0] = keyBytes[0];
        material[1] = (byte)(keyBytes[0] << 7 | (keyBytes[1] & 0xff) >>> 1);
        material[2] = (byte)(keyBytes[1] << 6 | (keyBytes[2] & 0xff) >>> 2);
        material[3] = (byte)(keyBytes[2] << 5 | (keyBytes[3] & 0xff) >>> 3);
        material[4] = (byte)(keyBytes[3] << 4 | (keyBytes[4] & 0xff) >>> 4);
        material[5] = (byte)(keyBytes[4] << 3 | (keyBytes[5] & 0xff) >>> 5);
        material[6] = (byte)(keyBytes[5] << 2 | (keyBytes[6] & 0xff) >>> 6);
        material[7] = (byte)(keyBytes[6] << 1);
        DESParameters.setOddParity(material);
        return material;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.oaklandsw.http.ntlm.NtlmProvider#getLMResponse(java.lang.String,
     *      byte[])
     */
    public byte[] getLMResponse(String password, byte[] challenge)
    {
        byte[] lmHash = lmHash(password);
        return lmResponse(lmHash, challenge);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.oaklandsw.http.ntlm.NtlmProvider#getNTLMResponse(java.lang.String,
     *      byte[])
     */
    public byte[] getNTLMResponse(String password, byte[] challenge)
    {
        byte[] ntlmHash = ntlmHash(password);
        return lmResponse(ntlmHash, challenge);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.oaklandsw.http.ntlm.NtlmProvider#getNTLMv2Response(java.lang.String,
     *      java.lang.String, java.lang.String, byte[], byte[], byte[])
     */
    public byte[] getNTLMv2Response(String target,
                                    String user,
                                    String password,
                                    byte[] targetInformation,
                                    byte[] challenge,
                                    byte[] clientChallenge)
    {
        byte[] ntlmv2Hash = ntlmv2Hash(target, user, password);
        byte[] blob = createBlob(targetInformation, clientChallenge);
        return lmv2Response(ntlmv2Hash, blob, challenge);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.oaklandsw.http.ntlm.NtlmProvider#getLMv2Response(java.lang.String,
     *      java.lang.String, java.lang.String, byte[], byte[])
     */
    public byte[] getLMv2Response(String target,
                                  String user,
                                  String password,
                                  byte[] challenge,
                                  byte[] clientChallenge)
    {
        byte[] ntlmv2Hash = ntlmv2Hash(target, user, password);
        return lmv2Response(ntlmv2Hash, clientChallenge, challenge);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.oaklandsw.http.ntlm.NtlmProvider#getNTLM2SessionResponse(java.lang.String,
     *      byte[], byte[])
     */
    public byte[] getNTLM2SessionResponse(String password,
                                          byte[] challenge,
                                          byte[] clientChallenge)
    {
        byte[] ntlmHash = ntlmHash(password);
        Digest md5 = new MD5Digest();
        byte[] md5out = new byte[md5.getDigestSize()];
        md5.update(challenge, 0, challenge.length);
        md5.update(clientChallenge, 0, clientChallenge.length);
        md5.doFinal(md5out, 0);
        byte[] sessionHash = new byte[8];
        System.arraycopy(md5out, 0, sessionHash, 0, 8);
        return lmResponse(ntlmHash, sessionHash);
    }

    /**
     * Creates the LM Hash of the user's password.
     * 
     * @param password
     *            The password.
     * 
     * @return The LM Hash of the given password, used in the calculation of the
     *         LM Response.
     */
    private byte[] lmHash(String password)
    {
        try
        {
            byte[] oemPassword = password.toUpperCase().getBytes(Util.ASCII_ENCODING);
            int length = Math.min(oemPassword.length, 14);
            byte[] keyBytes = new byte[14];
            System.arraycopy(oemPassword, 0, keyBytes, 0, length);
            byte[] magicConstant = "KGS!@#$%".getBytes(Util.ASCII_ENCODING);
            byte[] lmHash = new byte[16];
            byte[] key = createDESKey(keyBytes, 0);
            SecurityHelper.des(SecurityHelper.ENCRYPT,
                               magicConstant,
                               key,
                               lmHash,
                               0);
            key = createDESKey(keyBytes, 7);
            SecurityHelper.des(SecurityHelper.ENCRYPT,
                               magicConstant,
                               key,
                               lmHash,
                               8);
            return lmHash;
        }
        catch (UnsupportedEncodingException ex)
        {
            Util.impossible(ex);
            return null;
        }
    }

    /**
     * Creates the NTLM Hash of the user's password.
     * 
     * @param password
     *            The password.
     * 
     * @return The NTLM Hash of the given password, used in the calculation of
     *         the NTLM Response and the NTLMv2 and LMv2 Hashes.
     */
    private byte[] ntlmHash(String password)
    {
        byte[] unicodePassword = Util.getUnicodeLittleBytes(password);
        Digest md4 = new MD4Digest();
        byte[] digest = new byte[md4.getDigestSize()];
        md4.update(unicodePassword, 0, unicodePassword.length);
        md4.doFinal(digest, 0);

        byte[] outPassword = new byte[HASHED_PASSWORD_LEN];
        System.arraycopy(digest, 0, outPassword, 0, digest.length);

        return outPassword;
    }

    /**
     * Creates the NTLMv2 Hash of the user's password.
     * 
     * @param target
     *            The authentication target (i.e., domain).
     * @param user
     *            The username.
     * @param password
     *            The password.
     * 
     * @return The NTLMv2 Hash, used in the calculation of the NTLMv2 and LMv2
     *         Responses.
     */
    private byte[] ntlmv2Hash(String target, String user, String password)
    {
        byte[] ntlmHash = ntlmHash(password);
        String identity = user.toUpperCase()
            + (target != null ? target.toUpperCase() : "");

        HMac hmac = new HMac(new MD5Digest());
        byte[] hmacOut = new byte[hmac.getMacSize()];
        hmac.init(new KeyParameter(ntlmHash));
        byte[] identityBytes = Util.getUnicodeLittleBytes(identity);
        hmac.update(identityBytes, 0, identityBytes.length);
        hmac.doFinal(hmacOut, 0);
        return hmacOut;
    }

    /**
     * Creates the LM Response from the given hash and Type 2 challenge.
     * 
     * @param hash
     *            The LM or NTLM Hash.
     * @param challenge
     *            The server challenge from the Type 2 message.
     * 
     * @return The response (either LM or NTLM, depending on the provided hash).
     */
    private byte[] lmResponse(byte[] hash, byte[] challenge)
    {
        byte[] lmResponse = new byte[24];
        byte[] keyBytes = new byte[21];
        System.arraycopy(hash, 0, keyBytes, 0, 16);
        byte[] key = createDESKey(keyBytes, 0);
        SecurityHelper.des(SecurityHelper.ENCRYPT,
                           challenge,
                           key,
                           lmResponse,
                           0);
        key = createDESKey(keyBytes, 7);
        SecurityHelper.des(SecurityHelper.ENCRYPT,
                           challenge,
                           key,
                           lmResponse,
                           8);
        key = createDESKey(keyBytes, 14);
        SecurityHelper.des(SecurityHelper.ENCRYPT,
                           challenge,
                           key,
                           lmResponse,
                           16);
        return lmResponse;
    }

    /**
     * Creates the LMv2 Response from the given hash, client data, and Type 2
     * challenge.
     * 
     * @param hash
     *            The NTLMv2 Hash.
     * @param clientData
     *            The client data (blob or client challenge).
     * @param challenge
     *            The server challenge from the Type 2 message.
     * 
     * @return The response (either NTLMv2 or LMv2, depending on the client
     *         data).
     */
    private byte[] lmv2Response(byte[] hash, byte[] clientData, byte[] challenge)
    {
        byte[] data = new byte[challenge.length + clientData.length];
        System.arraycopy(challenge, 0, data, 0, challenge.length);
        System.arraycopy(clientData,
                         0,
                         data,
                         challenge.length,
                         clientData.length);

        HMac hmac = new HMac(new MD5Digest());
        byte[] hmacOut = new byte[hmac.getMacSize()];
        hmac.init(new KeyParameter(hash));
        hmac.update(data, 0, data.length);
        hmac.doFinal(hmacOut, 0);

        byte[] lmv2Response = new byte[hmacOut.length + clientData.length];
        System.arraycopy(hmacOut, 0, lmv2Response, 0, hmacOut.length);
        System.arraycopy(clientData,
                         0,
                         lmv2Response,
                         hmacOut.length,
                         clientData.length);
        return lmv2Response;
    }

    /**
     * Creates the NTLMv2 blob from the given target information block and
     * client challenge.
     * 
     * @param targetInformation
     *            The target information block from the Type 2 message.
     * @param clientChallenge
     *            The random 8-byte client challenge.
     * 
     * @return The blob, used in the calculation of the NTLMv2 Response.
     */
    private byte[] createBlob(byte[] targetInformation, byte[] clientChallenge)
    {
        byte[] blobSignature = new byte[] { (byte)0x01, (byte)0x01, (byte)0x00,
            (byte)0x00 };
        byte[] reserved = new byte[] { (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x00 };
        byte[] unknown1 = new byte[] { (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x00 };
        byte[] unknown2 = new byte[] { (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x00 };
        long time = System.currentTimeMillis();
        time += 11644473600000l; // milliseconds from January 1, 1601 ->
        // epoch.
        time *= 10000; // tenths of a microsecond.
        // convert to little-endian byte array.
        byte[] timestamp = new byte[8];
        for (int i = 0; i < 8; i++)
        {
            timestamp[i] = (byte)time;
            time >>>= 8;
        }
        byte[] blob = new byte[blobSignature.length
            + reserved.length
            + timestamp.length
            + clientChallenge.length
            + unknown1.length
            + targetInformation.length
            + unknown2.length];
        int offset = 0;
        System.arraycopy(blobSignature, 0, blob, offset, blobSignature.length);
        offset += blobSignature.length;
        System.arraycopy(reserved, 0, blob, offset, reserved.length);
        offset += reserved.length;
        System.arraycopy(timestamp, 0, blob, offset, timestamp.length);
        offset += timestamp.length;
        System.arraycopy(clientChallenge,
                         0,
                         blob,
                         offset,
                         clientChallenge.length);
        offset += clientChallenge.length;
        System.arraycopy(unknown1, 0, blob, offset, unknown1.length);
        offset += unknown1.length;
        System.arraycopy(targetInformation,
                         0,
                         blob,
                         offset,
                         targetInformation.length);
        offset += targetInformation.length;
        System.arraycopy(unknown2, 0, blob, offset, unknown2.length);
        return blob;
    }

    //
    // End of portion copyrighted by Eric Glass
    // 

}
