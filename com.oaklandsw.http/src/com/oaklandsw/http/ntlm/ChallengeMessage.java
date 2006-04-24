//
// Copyright 2002-3003, oakland software, all rights reserved.
//
// May not be used or redistributed without specific written
// permission from oakland software.
//

package com.oaklandsw.http.ntlm;

import com.oaklandsw.http.HttpException;
import com.oaklandsw.util.HexString;
import com.oaklandsw.util.Util;

public class ChallengeMessage extends Message
{

    protected static final int CHALLENGE_HEADER_LEN = 24;

    // Provided if NEG_LOCAL_CALL is set (this is actually just skipped)
    protected static final int CONTEXT_LENGTH       = 8;
    protected byte[]           _context;

    // Provided if NEG_TARGET_INFO is set
    protected byte[]           _targetBlock;

    // Default constructor
    public ChallengeMessage()
    {
    }
    
    public byte[] getTargetBlock()
    {
        return _targetBlock;
    }

    public int decode() throws HttpException
    {
        int index = super.decode();

        if (_type != MSG_CHALLENGE)
            throw new HttpException("Invalid message type");

        // session key - skip
        index += 4;

        _msgLength = (int)Util.fromByteLittle(4, _msgBytes, index);
        if (_msgLength < CHALLENGE_HEADER_LEN + NONCE_LENGTH + 8)
        {
            throw new RuntimeException("Invalid message length "
                + _msgLength
                + " in NTLM message");
        }
        index += 4;

        _flags = Util.fromByteLittle(4, _msgBytes, index);
        index += 4;

        _nonce = new byte[NONCE_LENGTH];
        System.arraycopy(_msgBytes,
                         CHALLENGE_HEADER_LEN,
                         _nonce,
                         0,
                         _nonce.length);
        index += NONCE_LENGTH;

        _context = new byte[CONTEXT_LENGTH];
        System.arraycopy(_msgBytes, index, _context, 0, _context.length);
        index += CONTEXT_LENGTH;

        if ((_flags & NEGOTIATE_TARGET_INFO) != 0)
        {
            // Skip over used length
            index += 2;
            int targetLen = (int)Util.fromByteLittle(2, _msgBytes, index);
            index += 2;
            
            // Offset of target block from start of message
            int targetOffset = (int)Util.fromByteLittle(4, _msgBytes, index);

            _targetBlock = new byte[targetLen];
            System.arraycopy(_msgBytes, targetOffset, _targetBlock, 0, targetLen);
        }

        log();

        return 0;
    }

    // Creates a challenge message
    // nonce is assume to be 8 bytes
    public int encode() throws IllegalArgumentException
    {
        _msgLength = CHALLENGE_HEADER_LEN + _nonce.length + 8;

        _type = MSG_CHALLENGE;
        int index = super.encode();

        // Session key? - not used
        index = Util.toByteLittle(0, 4, _msgBytes, index);
        index = Util.toByteLittle(_msgLength, 4, _msgBytes, index);
        index = Util.toByteLittle(_flags, 4, _msgBytes, index);

        System.arraycopy(_nonce,
                         0,
                         _msgBytes,
                         CHALLENGE_HEADER_LEN,
                         _nonce.length);

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
