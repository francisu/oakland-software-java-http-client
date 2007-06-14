//
// Copyright 2002-3003, oakland software, all rights reserved.
//
// May not be used or redistributed without specific written
// permission from oakland software.
//

package com.oaklandsw.http.ntlm;

import org.apache.commons.logging.Log;

import com.oaklandsw.http.HttpException;
import com.oaklandsw.util.Dump;
import com.oaklandsw.util.HexString;
import com.oaklandsw.util.LogUtils;
import com.oaklandsw.util.Util;

public class Message implements Dump
{

    private static final Log      _log                               = LogUtils
                                                                             .makeLogger();

    protected byte[]              _msgBytes;

    protected int                 _msgLength;

    protected static final String NTLMSSP                            = "NTLMSSP";

    protected static final int    SECURITY_BUFFER_LEN                = 8;

    // Message types
    public static final int       MSG_NEGOTIATE                      = 1;

    public static final int       MSG_CHALLENGE                      = 2;

    public static final int       MSG_AUTHENTICATE                   = 3;

    public static final int       MSG_UNKNOWN                        = 4;

    protected int                 _type;

    // Message flags
    public static final long      NEGOTIATE_UNICODE                  = 0x00000001;

    public static final long      NEGOTIATE_OEM                      = 0x00000002;

    public static final long      REQUEST_TARGET                     = 0x00000004;

    public static final long      NEGOTIATE_SIGN                     = 0x00000010;

    public static final long      NEGOTIATE_SEAL                     = 0x00000020;

    public static final long      NEGOTIATE_DATAGRAM                 = 0x00000040;

    public static final long      NEGOTIATE_LM_KEY                   = 0x00000080;

    public static final long      NEGOTIATE_NETWARE                  = 0x00000100;

    public static final long      NEGOTIATE_NTLM                     = 0x00000200;

    public static final long      NEGOTIATE_OEM_DOMAIN_SUPPLIED      = 0x00001000;

    public static final long      NEGOTIATE_OEM_WORKSTATION_SUPPLIED = 0x00002000;

    public static final long      NEGOTIATE_LOCAL_CALL               = 0x00004000;

    public static final long      NEGOTIATE_ALWAYS_SIGN              = 0x00008000;

    public static final long      TARGET_TYPE_DOMAIN                 = 0x00010000;

    public static final long      TARGET_TYPE_SERVER                 = 0x00020000;

    public static final long      TARGET_TYPE_SHARE                  = 0x00040000;

    public static final long      NEGOTIATE_NTLM2                    = 0x00080000;

    public static final long      REQUEST_INIT_RESPONSE              = 0x00100000;

    public static final long      REQUEST_ACCEPT_RESPONSE            = 0x00200000;

    public static final long      REQUEST_NON_NT_SESSION_KEY         = 0x00400000;

    public static final long      NEGOTIATE_TARGET_INFO              = 0x00800000;

    public static final long      NEGOTIATE_128                      = 0x20000000;

    public static final long      NEGOTIATE_KEY_EXCH                 = 0x40000000;

    public static final long      NEGOTIATE_80000000                 = 0x80000000;

    protected long                _flags;

    protected byte[]              _nonce;

    protected static final int    NONCE_LENGTH                       = 8;

    public Message()
    {
    }

    public void setBytes(byte[] msgBytes)
    {
        _msgBytes = msgBytes;
    }

    public byte[] getBytes()
    {
        return _msgBytes;
    }

    public long getFlags()
    {
        return _flags;
    }

    public void setFlags(long flags)
    {
        _flags = flags;
    }

    public int getType()
    {
        return _type;
    }

    public void setType(int type)
    {
        _type = type;
    }

    public byte[] getNonce()
    {
        return _nonce;
    }

    public void setNonce(byte[] nonce) throws IllegalArgumentException
    {
        if (nonce.length != NONCE_LENGTH)
        {
            throw new IllegalArgumentException("Invalid nonce length must be "
                + NONCE_LENGTH);
        }
        _nonce = nonce;
    }

    protected void setMsgLength(int length)
    {
        _msgLength = length;
    }

    /**
     * Decodes a message from the previously set byte array. The subclass should
     * call this first because this decodes part of the message header.
     * 
     * @return the index where subsequent decoding begins
     */
    public int decode() throws HttpException
    {
        if (_log.isDebugEnabled())
        {
            _log.debug("Message Bytes:");
            _log.debug("\n" + HexString.dump(_msgBytes));
        }

        int index = 0;

        String magic = Util.fromByteAscii(7, _msgBytes, index);
        if (!magic.equals(NTLMSSP))
            throw new HttpException("NTLMSSP not present in NTLM message");
        // 7 + the null terminator
        index += 8;

        _type = (int)Util.fromByteLittle(4, _msgBytes, index);
        index += 4;

        return index;
    }

    /**
     * Encodes a message into the byte array based on the properties of this
     * object. The subclass should call this first becuase it encodes the
     * header.
     * 
     * @return the index where subsequent encoding begins
     */
    public int encode() throws IllegalArgumentException
    {
        int index;

        _msgBytes = new byte[_msgLength];

        index = Util.toByteAscii(NTLMSSP, _msgBytes, 0);
        // null terminate the string
        _msgBytes[index++] = 0;

        return Util.toByteLittle(_type, 4, _msgBytes, index);
    }

    public static String messageTypeToString(int msgType)
    {
        switch (msgType)
        {
            case MSG_NEGOTIATE:
                return "Negotiate";
            case MSG_CHALLENGE:
                return "Challenge";
            case MSG_AUTHENTICATE:
                return "Authenticate";
            default:
                return "Unknown";
        }
    }

    public static String flagsToString(long flags)
    {
        StringBuffer ret = new StringBuffer();
        if ((flags & NEGOTIATE_UNICODE) != 0)
            ret.append("NEG_UNICODE ");
        if ((flags & NEGOTIATE_OEM) != 0)
            ret.append("NEG_OEM ");
        if ((flags & REQUEST_TARGET) != 0)
            ret.append("REQ_TARGET ");
        if ((flags & NEGOTIATE_SIGN) != 0)
            ret.append("NEG_SIGN ");
        if ((flags & NEGOTIATE_SEAL) != 0)
            ret.append("NEG_SEAL ");
        if ((flags & NEGOTIATE_DATAGRAM) != 0)
            ret.append("NEG_DATAGRAM ");
        if ((flags & NEGOTIATE_LM_KEY) != 0)
            ret.append("NEG_LM_KEY ");
        if ((flags & NEGOTIATE_NETWARE) != 0)
            ret.append("NEG_NETWARE ");
        if ((flags & NEGOTIATE_NTLM) != 0)
            ret.append("NEG_NTLM ");
        if ((flags & NEGOTIATE_OEM_DOMAIN_SUPPLIED) != 0)
            ret.append("NEG_OEM_DOMAIN_SUPPLIED ");
        if ((flags & NEGOTIATE_OEM_WORKSTATION_SUPPLIED) != 0)
            ret.append("NEG_OEM_WORKSTATION_SUPPLIED ");
        if ((flags & NEGOTIATE_LOCAL_CALL) != 0)
            ret.append("NEG_LOCAL_CALL ");
        if ((flags & NEGOTIATE_ALWAYS_SIGN) != 0)
            ret.append("NEG_ALWAYS_SIGN ");
        if ((flags & TARGET_TYPE_DOMAIN) != 0)
            ret.append("TAR_TYPE_DOMAIN ");
        if ((flags & TARGET_TYPE_SERVER) != 0)
            ret.append("TAR_TYPE_SERVER ");
        if ((flags & TARGET_TYPE_SHARE) != 0)
            ret.append("TAR_TYPE_SHARE ");
        if ((flags & NEGOTIATE_NTLM2) != 0)
            ret.append("NEG_NTLM2 ");
        if ((flags & REQUEST_INIT_RESPONSE) != 0)
            ret.append("REQ_INIT_RESPONSE ");
        if ((flags & REQUEST_ACCEPT_RESPONSE) != 0)
            ret.append("REQ_ACCEPT_RESPONSE ");
        if ((flags & REQUEST_NON_NT_SESSION_KEY) != 0)
            ret.append("REQ_NON_NT_SESSION_KEY ");
        if ((flags & NEGOTIATE_TARGET_INFO) != 0)
            ret.append("NEG_TARGET_INFO ");
        if ((flags & NEGOTIATE_128) != 0)
            ret.append("NEG_128 ");
        if ((flags & NEGOTIATE_KEY_EXCH) != 0)
            ret.append("NEG_KEY_EXCH ");
        if ((flags & NEGOTIATE_80000000) != 0)
            ret.append("NEG_80000000 ");
        return ret.toString();
    }

    public String toStringDump(int indent)
    {
        StringBuffer ret = new StringBuffer();
        ret.append("NTLM type: ");
        ret.append(messageTypeToString(_type));
        ret.append(" len: ");
        ret.append(Integer.toString(_msgLength));
        ret.append("\n  Flags: ");
        ret.append(flagsToString(_flags));
        ret.append("\n");

        if (_nonce != null)
        {
            ret.append("  Nonce: \n");
            ret.append(HexString.dump(_nonce));
        }

        // Allow the subclasses to put their stuff here
        getMessageInfo(ret);

        ret.append("Full message:\n");
        ret.append(HexString.dump(_msgBytes));
        return ret.toString();
    }

    public void getMessageInfo(StringBuffer sb)
    {
        // subclassed
    }

    public void log()
    {
        _log.debug(toStringDump(0));
    }

}
