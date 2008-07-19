//
// Copyright 2002-3003, oakland software, all rights reserved.
//
// May not be used or redistributed without specific written
// permission from oakland software.
//

package com.oaklandsw.http.ntlm;

import com.oaklandsw.http.HttpException;
import com.oaklandsw.http.HttpURLConnection;
import com.oaklandsw.util.Util;

public class NegotiateMessage extends Message
{

    protected String           _host;

    protected String           _domain;

    protected static final int NEGOTIATE_HEADER_LEN = 32;

    // Used for testing
    public static boolean      _testForceV1         = false;

    // Default constructor
    public NegotiateMessage()
    {
    }

    public void setHost(String host)
    {
        _host = host;
    }

    public String getHost()
    {
        return _host;
    }

    public void setDomain(String domain)
    {
        _domain = domain;
    }

    public String getDomain()
    {
        return _domain;
    }

    public int decode() throws HttpException
    {
        int index = super.decode();
        decodeAfterHeader(index);
        return 0;
    }

    public int decodeAfterHeader(int index) throws HttpException
    {
        if (_type != MSG_NEGOTIATE)
            throw new HttpException("Invalid message type");

        _flags = Util.fromByteLittle(4, _msgBytes, index);
        index += 4;

        // Domain
        int domainLen = (int)Util.fromByteLittle(2, _msgBytes, index);
        // Skip second domain length
        index += 4;
        int domainOffset = (int)Util.fromByteLittle(4, _msgBytes, index);
        index += 4;

        // Host
        int hostLen = (int)Util.fromByteLittle(2, _msgBytes, index);
        // Skip second host length
        index += 4;
        int hostOffset = (int)Util.fromByteLittle(4, _msgBytes, index);

        if (hostLen != 0)
            _host = Util.fromByteAscii(hostLen, _msgBytes, hostOffset);
        if (domainLen != 0)
            _domain = Util.fromByteAscii(domainLen, _msgBytes, domainOffset);

        log();

        return 0;
    }

    public int encode()
    {
        int domainLen;
        if (_domain != null)
            domainLen = _domain.length();
        else
            domainLen = 0;

        int hostLen;
        if (_host != null)
            hostLen = _host.length();
        else
            hostLen = 0;

        _msgLength = NEGOTIATE_HEADER_LEN + hostLen + domainLen;

        _type = MSG_NEGOTIATE;
        int index = super.encode();

        int hostOffset = NEGOTIATE_HEADER_LEN;
        int domainOffset = NEGOTIATE_HEADER_LEN + hostLen;

        if (!false)
        {
            // The real values
            _flags = NEGOTIATE_OEM
                | NEGOTIATE_NTLM
                | NEGOTIATE_OEM_DOMAIN_SUPPLIED
                | NEGOTIATE_OEM_WORKSTATION_SUPPLIED
                | NEGOTIATE_ALWAYS_SIGN;

            // Offer Unicode if we prefer it
            if (HttpURLConnection.getNtlmPreferredEncoding() == HttpURLConnection.NTLM_ENCODING_UNICODE)
                _flags |= NEGOTIATE_UNICODE;

            // These flags request NTLM V2 authentication if supported on the
            // server
            if (!_testForceV1)
                _flags |= NEGOTIATE_NTLM2
                    | REQUEST_TARGET
                    | NEGOTIATE_TARGET_INFO;
        }
        else
        {
            // For testing
            _flags = NEGOTIATE_128
                | 0x02000000
                | NEGOTIATE_56
                | NEGOTIATE_NTLM
                | NEGOTIATE_NTLM2
                | NEGOTIATE_ALWAYS_SIGN
                | NEGOTIATE_UNICODE
                | REQUEST_TARGET
                | NEGOTIATE_OEM;
        }

        if (domainLen == 0)
            domainOffset = 0;
        if (hostLen == 0)
            hostOffset = 0;

        index = Util.toByteLittle(_flags, 4, _msgBytes, index);

        index = Util.toByteLittle(domainLen, 2, _msgBytes, index);
        index = Util.toByteLittle(domainLen, 2, _msgBytes, index);
        index = Util.toByteLittle(domainOffset, 4, _msgBytes, index);

        index = Util.toByteLittle(hostLen, 2, _msgBytes, index);
        index = Util.toByteLittle(hostLen, 2, _msgBytes, index);
        index = Util.toByteLittle(hostOffset, 4, _msgBytes, index);

        if (hostLen != 0)
            index = Util.toByteAscii(_host.toUpperCase(), _msgBytes, index);

        if (domainLen != 0)
            index = Util.toByteAscii(_domain.toUpperCase(), _msgBytes, index);

        log();

        return 0;
    }

    public void getMessageInfo(StringBuffer sb)
    {
        sb.append("  host: ");
        sb.append(_host);
        sb.append("  domain: ");
        sb.append(_domain);
        sb.append("\n");
    }

}
