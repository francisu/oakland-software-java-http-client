//
// Copyright 2002-3003, oakland software, all rights reserved.
//
// May not be used or redistributed without specific written
// permission from oakland software.
//

package com.oaklandsw.http.ntlm;

import org.apache.commons.logging.Log;

import com.oaklandsw.http.HttpException;
import com.oaklandsw.util.HexString;
import com.oaklandsw.util.LogUtils;
import com.oaklandsw.util.Util;

public class ChallengeMessage extends Message
{
    private static final Log      _log                 = LogUtils.makeLogger();

    protected static final int    CHALLENGE_HEADER_LEN = 24;

    // Provided if NEG_LOCAL_CALL is set (this is actually just skipped)
    protected static final int    CONTEXT_LENGTH       = 8;
    protected byte[]              _context;

    protected static final byte[] EMPTY_CONTEXT        = new byte[CONTEXT_LENGTH];

    protected String              _targetName;

    // Provided if NEG_TARGET_INFO is set
    protected byte[]              _targetBlock;

    protected boolean             _encodingOem;

    // Default constructor
    public ChallengeMessage()
    {
    }

    public byte[] getTargetBlock()
    {
        return _targetBlock;
    }

    public void setupOutgoing(byte[] nonce,
                              NegotiateMessage nmsg,
                              byte[] targetInfo,
                              String targetName)
    {
        if (_log.isDebugEnabled())
        {
            _log.debug("setupOutgoing: nonce: " + HexString.dump(nonce));
            _log.debug("targetInfo: \n" + HexString.dump(targetInfo));
            _log.debug("targetName: \n" + targetName);
        }

        setFlags(Message.NEGOTIATE_NTLM | Message.NEGOTIATE_NTLM2);

        // target is server or domain
        setNonce(nonce);

        if ((nmsg._flags & Message.REQUEST_TARGET) != 0)
        {
            // FIXME Indicate what type of target, server or domain
            //_targetName = targetName;
            //_flags |= Message.TARGET_TYPE_SERVER;
            if ((nmsg._flags & Message.NEGOTIATE_UNICODE) != 0)
            {
                _flags |= Message.NEGOTIATE_UNICODE;
                _encodingOem = false;
            }
            else
            {
                // Assume OEM if nothing specified
                _flags |= Message.NEGOTIATE_OEM;
                _encodingOem = true;
            }
        }

        if ((nmsg._flags & Message.REQUEST_TARGET) != 0)
        {
            _flags |= Message.NEGOTIATE_TARGET_INFO;
            _targetBlock = targetInfo;
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

        // target name - skip
        index += 8;

        _flags = Util.fromByteLittle(4, _msgBytes, index);
        index += 4;

        _nonce = new byte[NONCE_LENGTH];
        System.arraycopy(_msgBytes,
                         CHALLENGE_HEADER_LEN,
                         _nonce,
                         0,
                         _nonce.length);
        index += NONCE_LENGTH;

        if (index < _msgBytes.length)
        {
            _context = new byte[CONTEXT_LENGTH];
            System.arraycopy(_msgBytes, index, _context, 0, _context.length);
            index += CONTEXT_LENGTH;
        }

        if ((_flags & NEGOTIATE_TARGET_INFO) != 0)
        {
            // Skip over used length
            index += 2;
            int targetLen = (int)Util.fromByteLittle(2, _msgBytes, index);
            index += 2;

            // Offset of target block from start of message
            int targetOffset = (int)Util.fromByteLittle(4, _msgBytes, index);

            _targetBlock = new byte[targetLen];
            System.arraycopy(_msgBytes,
                             targetOffset,
                             _targetBlock,
                             0,
                             targetLen);
        }

        log();

        return 0;
    }

    // Creates a challenge message
    // nonce is assume to be 8 bytes
    public int encode() throws IllegalArgumentException
    {
        boolean writeTarget = (_flags & NEGOTIATE_TARGET_INFO) != 0
            && _targetBlock != null;

        int targetNameLen = 0;
        if (_targetName != null)
        {
            if (_encodingOem)
                targetNameLen = _targetName.length();
            else
                targetNameLen = _targetName.length() * 2;
        }

        _msgLength = CHALLENGE_HEADER_LEN
            + _nonce.length
            + (writeTarget ? EMPTY_CONTEXT.length
                + _targetBlock.length
                + SECURITY_BUFFER_LEN : 0)
            + targetNameLen;

        _type = MSG_CHALLENGE;

        // Encode header and message type
        int index = super.encode();

        // Next 8 bytes is targetName (or a space for it)
        if (_targetName != null)
        {
            index = Util.toByteLittle(targetNameLen, 2, _msgBytes, index);
            index = Util.toByteLittle(targetNameLen, 2, _msgBytes, index);
            // This is written in the last part of the message
            index = Util.toByteLittle(_msgLength - targetNameLen,
                                      4,
                                      _msgBytes,
                                      index);
        }
        else
        {
            index += 8;
        }

        index = Util.toByteLittle(_flags, 4, _msgBytes, index);

        // End of challenge header

        System.arraycopy(_nonce,
                         0,
                         _msgBytes,
                         CHALLENGE_HEADER_LEN,
                         _nonce.length);
        index += _nonce.length;

        if (writeTarget)
        {
            // We never write anything for the context, but must include it
            // as padding if we write the target info
            System.arraycopy(EMPTY_CONTEXT,
                             0,
                             _msgBytes,
                             index,
                             EMPTY_CONTEXT.length);
            index += EMPTY_CONTEXT.length;

            index = Util.toByteLittle(_targetBlock.length, 2, _msgBytes, index);
            index = Util.toByteLittle(_targetBlock.length, 2, _msgBytes, index);
            // This is the next thing
            index = Util.toByteLittle(index + 4, 4, _msgBytes, index);

            System.arraycopy(_targetBlock,
                             0,
                             _msgBytes,
                             index,
                             _targetBlock.length);
            index += _targetBlock.length;
        }

        if (_targetName != null)
        {
            if (_encodingOem)
            {
                index = Util.toByteAscii(_targetName.toUpperCase(),
                                         _msgBytes,
                                         index);
            }
            else
            {
                index = Util.toByteUnicodeLittle(_targetName.toUpperCase(),
                                                 _msgBytes,
                                                 index);
            }
        }

        log();

        return 0;
    }

    public void getMessageInfo(StringBuffer sb)
    {
        if (_context != null)
        {
            sb.append("  context: \n");
            sb.append(HexString.dump(_context));
        }
        if (_targetBlock != null)
        {
            sb.append("  targetBlock: \n");
            sb.append(HexString.dump(_targetBlock));
        }
    }

}
