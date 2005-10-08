//
// Copyright 2002-3003, oakland software, all rights reserved.
//
// May not be used or redistributed without specific written
// permission from oakland software.
//

package com.oaklandsw.http.ntlm;

import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import com.oaklandsw.http.HttpException;
import com.oaklandsw.util.SecurityHelper;
import com.oaklandsw.util.Util;

public class AuthenticateMessage extends Message
{

    protected String              _host;

    protected String              _user;

    protected String              _domain;

    protected String              _password;

    protected byte[]              _ntResponse;

    protected byte[]              _lmResponse;

    protected boolean             _encodingOem;

    protected static final int    AUTHENTICATE_HEADER_LEN = 64;

    protected static final int    PASSWORD_LEN            = 14;

    protected static final int    RESPONSE_LEN            = 24;

    protected static final int    HASHED_PASSWORD_LEN     = 21;

    protected static final byte[] MAGIC                   = new byte[] { 0x4B,
        0x47, 0x53, 0x21, 0x40, 0x23, 0x24, 0x25         };

    // Default constructor
    public AuthenticateMessage()
    {
    }

    public void setHost(String host)
    {
        _host = host;
    }

    public void setUser(String user)
    {
        _user = user;
    }

    public void setDomain(String domain)
    {
        _domain = domain;
    }

    public void setPassword(String password)
    {
        _password = password;
    }

    public void setEncodingOem(boolean encodingOem)
    {
        _encodingOem = encodingOem;
    }

    protected byte[] calcLmPassword()
    {
        String password = _password.toUpperCase();
        if (password.length() > PASSWORD_LEN)
            password = password.substring(0, PASSWORD_LEN);

        // Get the first 14 bytes of the password, uppercase
        byte[] passwordBytes = new byte[PASSWORD_LEN];
        Util.toByteAscii(password.toUpperCase(), passwordBytes, 0);

        // 21 byte result, only the first 16 are filled in, the rest will
        // be zero
        byte[] hashedPassword = new byte[HASHED_PASSWORD_LEN];

        // Fill in the result
        SecurityHelper.desEncrypt(hashedPassword, passwordBytes, 0, 0, MAGIC);
        SecurityHelper.desEncrypt(hashedPassword, passwordBytes, 8, 7, MAGIC);

        return hashedPassword;
    }

    protected byte[] calcNtPassword()
    {
        byte[] passwordBytes = new byte[_password.length() * 2];
        // Always Unicode
        Util.toByteUnicodeLittle(_password, passwordBytes, 0);

        byte[] hashedPassword;

        // MD4
        MessageDigest md4 = null;
        try
        {
            md4 = MessageDigest.getInstance("MD4");
        }
        catch (NoSuchAlgorithmException ex)
        {
            throw new RuntimeException("(bug) - " + ex);
        }

        md4.update(passwordBytes);
        hashedPassword = md4.digest();

        byte[] outPassword = new byte[HASHED_PASSWORD_LEN];
        System.arraycopy(hashedPassword, 0, outPassword, 0,
                         hashedPassword.length);

        return outPassword;
    }

    // 
    // Copyright © 2003 Eric Glass (eglass1 at comcast.net).
    //
    // Permission to use, copy, modify, and distribute this document
    // for any purpose and without any fee is hereby granted, provided
    // that the above copyright notice and this list of conditions
    // appear in all copies.
    //
    /**
     * Calculates the various Type 3 responses.
     */

    /**
     * Calculates the LM Response for the given challenge, using the specified
     * password.
     * 
     * @param password
     *            The user's password.
     * @param challenge
     *            The Type 2 challenge from the server.
     * 
     * @return The LM Response.
     */
    public static byte[] getLMResponse(String password, byte[] challenge)
        throws Exception
    {
        byte[] lmHash = lmHash(password);
        return lmResponse(lmHash, challenge);
    }

    /**
     * Calculates the NTLM Response for the given challenge, using the specified
     * password.
     * 
     * @param password
     *            The user's password.
     * @param challenge
     *            The Type 2 challenge from the server.
     * 
     * @return The NTLM Response.
     */
    public static byte[] getNTLMResponse(String password, byte[] challenge)
        throws Exception
    {
        byte[] ntlmHash = ntlmHash(password);
        return lmResponse(ntlmHash, challenge);
    }

    /**
     * Calculates the NTLMv2 Response for the given challenge, using the
     * specified authentication target, username, password, target information
     * block, and client challenge.
     * 
     * @param target
     *            The authentication target (i.e., domain).
     * @param user
     *            The username.
     * @param password
     *            The user's password.
     * @param targetInformation
     *            The target information block from the Type 2 message.
     * @param challenge
     *            The Type 2 challenge from the server.
     * @param clientChallenge
     *            The random 8-byte client challenge.
     * 
     * @return The NTLMv2 Response.
     */
    public static byte[] getNTLMv2Response(String target,
                                           String user,
                                           String password,
                                           byte[] targetInformation,
                                           byte[] challenge,
                                           byte[] clientChallenge)
        throws Exception
    {
        byte[] ntlmv2Hash = ntlmv2Hash(target, user, password);
        byte[] blob = createBlob(targetInformation, clientChallenge);
        return lmv2Response(ntlmv2Hash, blob, challenge);
    }

    /**
     * Calculates the LMv2 Response for the given challenge, using the specified
     * authentication target, username, password, and client challenge.
     * 
     * @param target
     *            The authentication target (i.e., domain).
     * @param user
     *            The username.
     * @param password
     *            The user's password.
     * @param challenge
     *            The Type 2 challenge from the server.
     * @param clientChallenge
     *            The random 8-byte client challenge.
     * 
     * @return The LMv2 Response.
     */
    public static byte[] getLMv2Response(String target,
                                         String user,
                                         String password,
                                         byte[] challenge,
                                         byte[] clientChallenge)
        throws Exception
    {
        byte[] ntlmv2Hash = ntlmv2Hash(target, user, password);
        return lmv2Response(ntlmv2Hash, clientChallenge, challenge);
    }

    /**
     * Calculates the NTLM2 Session Response for the given challenge, using the
     * specified password and client challenge.
     * 
     * @param password
     *            The user's password.
     * @param challenge
     *            The Type 2 challenge from the server.
     * @param clientChallenge
     *            The random 8-byte client challenge.
     * 
     * @return The NTLM2 Session Response. This is placed in the NTLM response
     *         field of the Type 3 message; the LM response field contains the
     *         client challenge, null-padded to 24 bytes.
     */
    public static byte[] getNTLM2SessionResponse(String password,
                                                 byte[] challenge,
                                                 byte[] clientChallenge)
        throws Exception
    {
        byte[] ntlmHash = ntlmHash(password);
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(challenge);
        md5.update(clientChallenge);
        byte[] sessionHash = new byte[8];
        System.arraycopy(md5.digest(), 0, sessionHash, 0, 8);
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
    private static byte[] lmHash(String password) throws Exception
    {
        byte[] oemPassword = password.toUpperCase().getBytes("US-ASCII");
        int length = Math.min(oemPassword.length, 14);
        byte[] keyBytes = new byte[14];
        System.arraycopy(oemPassword, 0, keyBytes, 0, length);
        Key lowKey = createDESKey(keyBytes, 0);
        Key highKey = createDESKey(keyBytes, 7);
        byte[] magicConstant = "KGS!@#$%".getBytes("US-ASCII");
        Cipher des = Cipher.getInstance("DES/ECB/NoPadding");
        des.init(Cipher.ENCRYPT_MODE, lowKey);
        byte[] lowHash = des.doFinal(magicConstant);
        des.init(Cipher.ENCRYPT_MODE, highKey);
        byte[] highHash = des.doFinal(magicConstant);
        byte[] lmHash = new byte[16];
        System.arraycopy(lowHash, 0, lmHash, 0, 8);
        System.arraycopy(highHash, 0, lmHash, 8, 8);
        return lmHash;
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
    private static byte[] ntlmHash(String password) throws Exception
    {
        byte[] unicodePassword = password.getBytes("UnicodeLittleUnmarked");
        MessageDigest md4 = MessageDigest.getInstance("MD4");
        return md4.digest(unicodePassword);
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
    private static byte[] ntlmv2Hash(String target, String user, String password)
        throws Exception
    {
        byte[] ntlmHash = ntlmHash(password);
        String identity = user.toUpperCase() + target.toUpperCase();
        return hmacMD5(identity.getBytes("UnicodeLittleUnmarked"), ntlmHash);
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
    private static byte[] lmResponse(byte[] hash, byte[] challenge)
        throws Exception
    {
        byte[] keyBytes = new byte[21];
        System.arraycopy(hash, 0, keyBytes, 0, 16);
        Key lowKey = createDESKey(keyBytes, 0);
        Key middleKey = createDESKey(keyBytes, 7);
        Key highKey = createDESKey(keyBytes, 14);
        Cipher des = Cipher.getInstance("DES/ECB/NoPadding");
        des.init(Cipher.ENCRYPT_MODE, lowKey);
        byte[] lowResponse = des.doFinal(challenge);
        des.init(Cipher.ENCRYPT_MODE, middleKey);
        byte[] middleResponse = des.doFinal(challenge);
        des.init(Cipher.ENCRYPT_MODE, highKey);
        byte[] highResponse = des.doFinal(challenge);
        byte[] lmResponse = new byte[24];
        System.arraycopy(lowResponse, 0, lmResponse, 0, 8);
        System.arraycopy(middleResponse, 0, lmResponse, 8, 8);
        System.arraycopy(highResponse, 0, lmResponse, 16, 8);
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
    private static byte[] lmv2Response(byte[] hash,
                                       byte[] clientData,
                                       byte[] challenge) throws Exception
    {
        byte[] data = new byte[challenge.length + clientData.length];
        System.arraycopy(challenge, 0, data, 0, challenge.length);
        System.arraycopy(clientData, 0, data, challenge.length,
                         clientData.length);
        byte[] mac = hmacMD5(data, hash);
        byte[] lmv2Response = new byte[mac.length + clientData.length];
        System.arraycopy(mac, 0, lmv2Response, 0, mac.length);
        System.arraycopy(clientData, 0, lmv2Response, mac.length,
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
    private static byte[] createBlob(byte[] targetInformation,
                                     byte[] clientChallenge)
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
        System.arraycopy(clientChallenge, 0, blob, offset,
                         clientChallenge.length);
        offset += clientChallenge.length;
        System.arraycopy(unknown1, 0, blob, offset, unknown1.length);
        offset += unknown1.length;
        System.arraycopy(targetInformation, 0, blob, offset,
                         targetInformation.length);
        offset += targetInformation.length;
        System.arraycopy(unknown2, 0, blob, offset, unknown2.length);
        return blob;
    }

    /**
     * Calculates the HMAC-MD5 hash of the given data using the specified
     * hashing key.
     * 
     * @param data
     *            The data for which the hash will be calculated.
     * @param key
     *            The hashing key.
     * 
     * @return The HMAC-MD5 hash of the given data.
     */
    private static byte[] hmacMD5(byte[] data, byte[] key) throws Exception
    {
        byte[] ipad = new byte[64];
        byte[] opad = new byte[64];
        for (int i = 0; i < 64; i++)
        {
            ipad[i] = (byte)0x36;
            opad[i] = (byte)0x5c;
        }
        for (int i = key.length - 1; i >= 0; i--)
        {
            ipad[i] ^= key[i];
            opad[i] ^= key[i];
        }
        byte[] content = new byte[data.length + 64];
        System.arraycopy(ipad, 0, content, 0, 64);
        System.arraycopy(data, 0, content, 64, data.length);
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        data = md5.digest(content);
        content = new byte[data.length + 64];
        System.arraycopy(opad, 0, content, 0, 64);
        System.arraycopy(data, 0, content, 64, data.length);
        return md5.digest(content);
    }

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
    private static Key createDESKey(byte[] bytes, int offset)
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
        oddParity(material);
        return new SecretKeySpec(material, "DES");
    }

    /**
     * Applies odd parity to the given byte array.
     * 
     * @param bytes
     *            The data whose parity bits are to be adjusted for odd parity.
     */
    private static void oddParity(byte[] bytes)
    {
        for (int i = 0; i < bytes.length; i++)
        {
            byte b = bytes[i];
            boolean needsParity = (((b >>> 7)
                ^ (b >>> 6)
                ^ (b >>> 5)
                ^ (b >>> 4)
                ^ (b >>> 3)
                ^ (b >>> 2) ^ (b >>> 1)) & 0x01) == 0;
            if (needsParity)
            {
                bytes[i] |= (byte)0x01;
            }
            else
            {
                bytes[i] &= (byte)0xfe;
            }
        }
    }

    //
    // End of portion copyrighted by Eric Glass
    // 

    /**
     * Encrypts the hash password using the nonce and returns the response.
     */
    protected byte[] encryptResponse(byte[] hashedPassword)
    {
        byte[] response = new byte[RESPONSE_LEN];

        SecurityHelper.desEncrypt(response, hashedPassword, 0, 0, _nonce);
        SecurityHelper.desEncrypt(response, hashedPassword, 8, 7, _nonce);
        SecurityHelper.desEncrypt(response, hashedPassword, 16, 14, _nonce);

        return response;
    }

    protected void calcResponses()
    {
        byte[] lmPassword = calcLmPassword();
        byte[] ntPassword = calcNtPassword();

        _lmResponse = encryptResponse(lmPassword);
        _ntResponse = encryptResponse(ntPassword);

    }

    public int decode() throws HttpException
    {
        int index = super.decode();

        if (_type != MSG_AUTHENTICATE)
            throw new HttpException("Invalid message type");

        int lmResponseLen;
        int lmResponseOffset;
        int ntResponseLen;
        int ntResponseOffset;
        int domainLen;
        int domainOffset;
        int userLen;
        int userOffset;
        int hostLen;
        int hostOffset;

        lmResponseLen = (int)Util.fromByteLittle(2, _msgBytes, index);
        index += 4; // skip over 2nd length
        lmResponseOffset = (int)Util.fromByteLittle(4, _msgBytes, index);
        index += 4;

        ntResponseLen = (int)Util.fromByteLittle(2, _msgBytes, index);
        index += 4; // skip over 2nd length
        ntResponseOffset = (int)Util.fromByteLittle(4, _msgBytes, index);
        index += 4;

        domainLen = (int)Util.fromByteLittle(2, _msgBytes, index);
        index += 4; // skip over 2nd length
        domainOffset = (int)Util.fromByteLittle(4, _msgBytes, index);
        index += 4;

        userLen = (int)Util.fromByteLittle(2, _msgBytes, index);
        index += 4; // skip over 2nd length
        userOffset = (int)Util.fromByteLittle(4, _msgBytes, index);
        index += 4;

        hostLen = (int)Util.fromByteLittle(2, _msgBytes, index);
        index += 4; // skip over 2nd length
        hostOffset = (int)Util.fromByteLittle(4, _msgBytes, index);
        index += 4;

        // Session key - not used
        index += 4;

        _msgLength = (int)Util.fromByteLittle(4, _msgBytes, index);
        index += 4;

        _flags = (int)Util.fromByteLittle(4, _msgBytes, index);
        index += 4;

        if (domainLen != 0)
        {
            if ((_flags & NEGOTIATE_OEM) != 0)
            {
                _domain = Util
                        .fromByteAscii(domainLen, _msgBytes, domainOffset);
            }
            else
            {
                _domain = Util.fromByteUnicodeLittle(domainLen, _msgBytes,
                                                     domainOffset);
            }
        }
        index += domainLen;

        if (hostLen != 0)
        {
            if ((_flags & NEGOTIATE_OEM) != 0)
            {
                _host = Util.fromByteAscii(hostLen, _msgBytes, hostOffset);
            }
            else
            {
                _host = Util.fromByteUnicodeLittle(hostLen, _msgBytes,
                                                   hostOffset);
            }
        }
        index += hostLen;

        if (userLen != 0)
        {
            if ((_flags & NEGOTIATE_OEM) != 0)
            {
                _user = Util.fromByteAscii(userLen, _msgBytes, userOffset);
            }
            else
            {
                _user = Util.fromByteUnicodeLittle(userLen, _msgBytes,
                                                   userOffset);
            }
        }
        index += userLen;

        // Nonsense to avoid compiler warnings, remove this if the responses
        // are to be read
        lmResponseLen = lmResponseLen + 77;
        lmResponseOffset = lmResponseOffset + 77;
        ntResponseLen = ntResponseLen + 77;
        ntResponseOffset = ntResponseOffset + 77;

        return index;
    }

    public int encode()
    {
        calcResponses();

        int domainLen = _domain.length();
        int hostLen = _host.length();
        int userLen = _user.length();

        // Unicode is 2 bytes per char
        if (!_encodingOem)
        {
            domainLen = _domain.length() * 2;
            hostLen = _host.length() * 2;
            userLen = _user.length() * 2;
        }

        _msgLength = AUTHENTICATE_HEADER_LEN
            + hostLen
            + userLen
            + domainLen
            + _ntResponse.length
            + _lmResponse.length;

        _type = MSG_AUTHENTICATE;
        int index = super.encode();

        _flags = NEGOTIATE_NTLM | NEGOTIATE_ALWAYS_SIGN;

        if (_encodingOem)
            _flags |= NEGOTIATE_OEM;
        else
            _flags |= NEGOTIATE_UNICODE;

        int domainOffset = AUTHENTICATE_HEADER_LEN;
        int userOffset = domainOffset + domainLen;
        int hostOffset = userOffset + userLen;
        int lmResponseOffset = hostOffset + hostLen;
        int ntResponseOffset = lmResponseOffset + _lmResponse.length;

        if (domainLen == 0)
            domainOffset = 0;

        index = Util.toByteLittle(_lmResponse.length, 2, _msgBytes, index);
        index = Util.toByteLittle(_lmResponse.length, 2, _msgBytes, index);
        index = Util.toByteLittle(lmResponseOffset, 4, _msgBytes, index);

        index = Util.toByteLittle(_ntResponse.length, 2, _msgBytes, index);
        index = Util.toByteLittle(_ntResponse.length, 2, _msgBytes, index);
        index = Util.toByteLittle(ntResponseOffset, 4, _msgBytes, index);

        index = Util.toByteLittle(domainLen, 2, _msgBytes, index);
        index = Util.toByteLittle(domainLen, 2, _msgBytes, index);
        index = Util.toByteLittle(domainOffset, 4, _msgBytes, index);

        index = Util.toByteLittle(userLen, 2, _msgBytes, index);
        index = Util.toByteLittle(userLen, 2, _msgBytes, index);
        index = Util.toByteLittle(userOffset, 4, _msgBytes, index);

        index = Util.toByteLittle(hostLen, 2, _msgBytes, index);
        index = Util.toByteLittle(hostLen, 2, _msgBytes, index);
        index = Util.toByteLittle(hostOffset, 4, _msgBytes, index);

        // Session key - not used
        index = Util.toByteLittle(0, 4, _msgBytes, index);

        index = Util.toByteLittle(_msgLength, 4, _msgBytes, index);
        index = Util.toByteLittle(_flags, 4, _msgBytes, index);

        if (domainLen != 0)
        {
            if (_encodingOem)
            {
                index = Util.toByteAscii(_domain.toUpperCase(), _msgBytes,
                                         index);
            }
            else
            {
                index = Util.toByteUnicodeLittle(_domain.toUpperCase(),
                                                 _msgBytes, index);
            }
        }

        if (_encodingOem)
        {
            index = Util.toByteAscii(_user.toUpperCase(), _msgBytes, index);
            index = Util.toByteAscii(_host.toUpperCase(), _msgBytes, index);
        }
        else
        {
            index = Util.toByteUnicodeLittle(_user, _msgBytes, index);
            index = Util.toByteUnicodeLittle(_host.toUpperCase(), _msgBytes,
                                             index);
        }

        System.arraycopy(_lmResponse, 0, _msgBytes, lmResponseOffset,
                         _lmResponse.length);
        System.arraycopy(_ntResponse, 0, _msgBytes, ntResponseOffset,
                         _ntResponse.length);

        log();

        return 0;
    }

    public void getMessageInfo(StringBuffer sb)
    {
        sb.append("  host: ");
        sb.append(_host);
        sb.append("  user: ");
        sb.append(_user);
        sb.append("  domain: ");
        sb.append(_domain);
        sb.append("  password: ");
        sb.append(_password);
        sb.append("\n");
    }

}
