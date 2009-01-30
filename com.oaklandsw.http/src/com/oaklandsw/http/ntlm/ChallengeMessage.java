//
// Copyright 2002-2008, oakland software, all rights reserved.
//
// May not be used or redistributed without specific written
// permission from oakland software.
//

package com.oaklandsw.http.ntlm;

import com.oaklandsw.util.Log;

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

    protected static final String NO_TARGET_MESSAGE = "The client has requested the authentication "
                                                        + "target which is either a domain name "
                                                        + "(if part of a domain) or workstation name (if not).  "
                                                        + "This must be set using the jcifs.smb.client.domain "
                                                        + "or jcifs.smb.client.server property";

    // This is used for the JCIFS integration
    public void setupOutgoing(byte[] nonce,
                              NegotiateMessage nmsg,
                              String domain,
                              String host)
    {
        if (_log.isDebugEnabled())
        {
            _log.debug("setupOutgoing: nonce: " + HexFormatter.dump(nonce));
        }

        setFlags(Ntlm._challengeMessageFlags | nmsg.getFlags());

        setNonce(nonce);

        if ((nmsg._flags & Message.REQUEST_TARGET) != 0)
        {
            if (domain != null)
            {
                _targetName = domain;
                _flags |= Message.TARGET_TYPE_DOMAIN;
                if (_log.isDebugEnabled())
                    _log.debug("targetName (domain): " + _targetName);
            }
            else if (host != null)
            {
                _targetName = host;
                _flags |= Message.TARGET_TYPE_SERVER;
                if (_log.isDebugEnabled())
                    _log.debug("targetName (host): " + _targetName);
            }
            else
            {
                throw new IllegalStateException(NO_TARGET_MESSAGE);
            }
        }

        if (true || (nmsg._flags & Message.NEGOTIATE_TARGET_INFO) != 0)
        {
            if (domain == null && host == null)
            {
                throw new IllegalStateException(NO_TARGET_MESSAGE);
            }
            _targetInfo = Ntlm.createTargetInfo(domain, host);
            _flags |= Message.NEGOTIATE_TARGET_INFO;
            if (_log.isDebugEnabled())
            {
                _log.debug("targetInfo: \n" + HexFormatter.dump(_targetInfo));
            }
        }

        // We prefer unicode, so turn of OEM if that is requested.
        // Otherwise, OEM was requested
        if ((nmsg._flags & Message.NEGOTIATE_UNICODE) != 0)
            _flags &= ~Message.NEGOTIATE_OEM;
    }

    public int decode() throws HttpException
    {
        int index = super.decode();
        decodeAfterHeader(index);
        return index;
    }

    public int decodeAfterHeader(int index) throws HttpException
    {
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
            sb.append("  targetName: ");
            sb.append(_targetName);
        }

        if (_targetInfo != null)
        {
            sb.append("  targetInfo: \n");
            sb.append(HexFormatter.dump(_targetInfo));
        }
    }

}
