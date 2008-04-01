//
// Copyright 2002-3003, oakland software, all rights reserved.
//
// May not be used or redistributed without specific written
// permission from oakland software.
//

package com.oaklandsw.http.ntlm;

import org.apache.commons.logging.Log;

import com.oaklandsw.http.HttpException;
import com.oaklandsw.util.HexFormatter;
import com.oaklandsw.util.LogUtils;
import com.oaklandsw.util.Util;

public class ChallengeMessage extends Message
{
    private static final Log      _log                = LogUtils.makeLogger();

    protected static final int    CHALLENGE_FIXED_LEN = 56;

    // Provided if NEG_LOCAL_CALL is set (this is actually just skipped)
    protected static final int    CONTEXT_LENGTH      = 8;
    protected byte[]              _context;

    protected static final byte[] EMPTY_CONTEXT       = new byte[CONTEXT_LENGTH];

    // The server authentication realm
    protected String              _targetName;

    // Provided if NEG_TARGET_INFO is set
    protected byte[]              _targetInfo;

    // Default constructor
    public ChallengeMessage()
    {
    }

    public byte[] getTargetInfo()
    {
        return _targetInfo;
    }

    // This is used for the JCIFS integration
    public void setupOutgoing(byte[] nonce,
                              NegotiateMessage nmsg,
                              byte[] targetInfo,
                              String targetName)
    {
        if (targetName == null)
        {
            throw new IllegalArgumentException("Must specify targetName - "
                + "make sure the jcifs.smb.client.domain parameter is set");
        }

        if (_log.isDebugEnabled())
        {
            _log.debug("setupOutgoing: nonce: " + HexFormatter.dump(nonce));
            _log.debug("targetInfo (defaultTargetInfo): \n"
                + HexFormatter.dump(targetInfo));
            _log.debug("targetName (defaultDomain): \n" + targetName);
        }

        setFlags(Ntlm._challengeMessageFlags);

        setNonce(nonce);

        // Per SMB Spec target info is not used
        // However the spec does not seem to match what is done
        if ((nmsg._flags & Message.NEGOTIATE_TARGET_INFO) != 0)
        {
            if (targetInfo == null)
                throw new IllegalStateException("Negotiate message specifies NEG_TARGET_INFO, but no targetInfo supplied");

        }

        // Per SMB Spec (3.2.4.2.3) target info is not used
        // However the spec does not seem to match what is done
        if (targetInfo != null)
        {
            _flags |= Message.NEGOTIATE_TARGET_INFO;
            _targetInfo = targetInfo;
        }

        // Per SMB spec target name is host
        // However the spec does not seem to match what is done
        if (targetName != null)
        {
            _targetName = targetName;
            // Per SMB spec this flag is not set
            _flags |= Message.TARGET_TYPE_DOMAIN;
        }

        // Per SMB spec encoding is OEM
        // However we will follow the negotiate message

        if ((nmsg._flags & Message.NEGOTIATE_UNICODE) != 0)
        {
            _flags |= Message.NEGOTIATE_UNICODE;
        }
        else
        {
            // Assume OEM if nothing specified
            _flags |= Message.NEGOTIATE_OEM;
        }
    }

    public int decode() throws HttpException
    {
        int index = super.decode();

        if (_type != MSG_CHALLENGE)
        {
            throw new HttpException("Invalid message type - expected: "
                + MSG_CHALLENGE
                + " got: "
                + _type);
        }

        // Target name
        int targetNameLen = (int)Util.fromByteLittle(2, _msgBytes, index);
        index += 2;
        // Skip max len
        index += 2;
        int targetNameOffset = (int)Util.fromByteLittle(4, _msgBytes, index);
        index += 4;

        _flags = Util.fromByteLittle(4, _msgBytes, index);
        index += 4;

        _nonce = new byte[NONCE_LENGTH];
        System.arraycopy(_msgBytes, index, _nonce, 0, _nonce.length);
        index += NONCE_LENGTH;

        // Message may end here?
        if (index >= _msgBytes.length)
        {
            _targetInfo = new byte[] {};
            return 0;
        }

        // This is not used for anything
        index += CONTEXT_LENGTH;

        // Target Info
        int targetInfoLen = (int)Util.fromByteLittle(2, _msgBytes, index);
        index += 2;
        // Skip max len
        index += 2;
        // Offset of target block from start of message
        int targetInfoOffset = (int)Util.fromByteLittle(4, _msgBytes, index);
        index += 4;

        // This is where the version is (used only for debugging)
        index += 8;

        //
        // Payload portion of message
        //

        if ((_flags & NEGOTIATE_OEM) != 0)
        {
            _targetName = Util.fromByteAscii(targetNameLen,
                                             _msgBytes,
                                             targetNameOffset);
        }
        else
        {
            _targetName = Util.fromByteUnicodeLittle(targetNameLen,
                                                     _msgBytes,
                                                     targetNameOffset);
        }

        _targetInfo = new byte[targetInfoLen];
        System.arraycopy(_msgBytes,
                         targetInfoOffset,
                         _targetInfo,
                         0,
                         targetInfoLen);
        log();

        return 0;
    }

    // Creates a challenge message
    // nonce is assume to be 8 bytes
    public int encode() throws IllegalArgumentException
    {
        if (_targetInfo == null)
            _targetInfo = new byte[] {};

        int targetNameLen = 0;
        if (_targetName != null)
        {
            if ((_flags & Message.NEGOTIATE_OEM) != 0)
                targetNameLen = _targetName.length();
            else
                targetNameLen = _targetName.length() * 2;
        }

        _msgLength = CHALLENGE_FIXED_LEN + _targetInfo.length + targetNameLen;

        int targetNameOffset = CHALLENGE_FIXED_LEN;
        int targetInfoOffset = CHALLENGE_FIXED_LEN + targetNameLen;

        _type = MSG_CHALLENGE;

        // Encode header and message type
        int index = super.encode();

        // Next 8 bytes is targetName (or a space for it)
        if (_targetName != null)
        {
            index = Util.toByteLittle(targetNameLen, 2, _msgBytes, index);
            index = Util.toByteLittle(targetNameLen, 2, _msgBytes, index);
            // This is written in the last part of the message
            index = Util.toByteLittle(targetNameOffset, 4, _msgBytes, index);
        }
        else
        {
            index += 8;
        }

        index = Util.toByteLittle(_flags, 4, _msgBytes, index);

        System.arraycopy(_nonce, 0, _msgBytes, index, _nonce.length);
        index += _nonce.length;

        // Context is always empty
        index += EMPTY_CONTEXT.length;

        // Target info security block
        index = Util.toByteLittle(_targetInfo.length, 2, _msgBytes, index);
        index = Util.toByteLittle(_targetInfo.length, 2, _msgBytes, index);
        index = Util.toByteLittle(targetInfoOffset, 4, _msgBytes, index);

        //
        // Payload portion of the message
        //

        if (_targetName != null)
        {
            if ((_flags & Message.NEGOTIATE_OEM) != 0)
            {
                index = Util.toByteAscii(_targetName.toUpperCase(),
                                         _msgBytes,
                                         targetNameOffset);
            }
            else
            {
                index = Util.toByteUnicodeLittle(_targetName.toUpperCase(),
                                                 _msgBytes,
                                                 targetNameOffset);
            }
        }

        System.arraycopy(_targetInfo,
                         0,
                         _msgBytes,
                         targetInfoOffset,
                         _targetInfo.length);

        log();

        return 0;
    }

    public void getMessageInfo(StringBuffer sb)
    {
        if (_targetName != null)
        {
            sb.append("  targetName: \n");
            sb.append(_targetName);
        }

        if (_targetInfo != null)
        {
            sb.append("  targetInfo: \n");
            sb.append(HexFormatter.dump(_targetInfo));
        }
    }

}
