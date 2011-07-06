/*
 * Copyright 2002, 2008, Oakland Software Incorporated
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

import com.oaklandsw.utillog.Log;

import com.oaklandsw.http.HttpException;
import com.oaklandsw.util.HexFormatter;
import com.oaklandsw.util.LogUtils;
import com.oaklandsw.util.Util;

public class AuthenticateMessage extends Message
{
    private static final Log   _log                    = LogUtils.makeLogger();

    public String              _host;
    public String              _user;
    public String              _domain;
    public String              _password;

    public byte[]              _ntResponse;
    public byte[]              _lmResponse;

    protected boolean          _encodingOem;

    protected ChallengeMessage _challenge;

    protected static final int AUTHENTICATE_HEADER_LEN = 64+8;

    protected static final int PASSWORD_LEN            = 14;
    protected static final int RESPONSE_LEN            = 24;

    protected static NtlmProvider _provider = new NtlmProviderImpl(); 
    
    protected static final int HASHED_PASSWORD_LEN     = 21;

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

    public void setChallenge(ChallengeMessage challenge)
    {
        _challenge = challenge;
    }

    protected void calcResponses()
    {
        try
        {
            byte[] chalBytes = new byte[_challenge._msgLength];
            System.arraycopy(_challenge.getBytes(),
                             0,
                             chalBytes,
                             0,
                             _challenge._msgLength);

            byte[] clientChallenge = new byte[8];
            Util
                    .toByteLittle(System.currentTimeMillis(),
                                  8,
                                  clientChallenge,
                                  0);

            switch (Ntlm._authMessageLmResponse)
            {
                case Ntlm.NONE:
                    break;
                case Ntlm.V1:
                    _lmResponse = _provider.getLMResponse(_password, _nonce);
                    break;
                case Ntlm.V2:
                    _lmResponse = _provider.getLMv2Response(_domain,
                                                  _user,
                                                  _password,
                                                  _nonce,
                                                  clientChallenge);
                    break;
                case Ntlm.AS_NEGOTIATED:
                    if ((_challenge.getFlags() & NEGOTIATE_TARGET_INFO) != 0)
                    {
                        _lmResponse = _provider.getLMv2Response(_domain,
                                                      _user,
                                                      _password,
                                                      _nonce,
                                                      clientChallenge);
                    }
                    else
                    {
                        _lmResponse = _provider.getLMResponse(_password, _nonce);
                    }
                    break;
            }

            switch (Ntlm._authMessageNtResponse)
            {
                case Ntlm.NONE:
                    break;
                case Ntlm.V1:
                    _ntResponse = _provider.getNTLMResponse(_password, _nonce);
                    break;
                case Ntlm.V2:
                    _ntResponse = _provider.getNTLMv2Response(_domain,
                                                    _user,
                                                    _password,
                                                    _challenge.getTargetInfo(),
                                                    _nonce,
                                                    clientChallenge);
                    _flags |= NEGOTIATE_NTLM2;
                    break;
                case Ntlm.AS_NEGOTIATED:
                    if ((_challenge.getFlags() & NEGOTIATE_TARGET_INFO) != 0)
                    {
                        _ntResponse = _provider.getNTLMv2Response(_domain,
                                                        _user,
                                                        _password,
                                                        _challenge
                                                                .getTargetInfo(),
                                                        _nonce,
                                                        clientChallenge);
                        _flags |= NEGOTIATE_NTLM2;
                    }
                    else
                    {
                        _ntResponse = _provider.getNTLMResponse(_password, _nonce);
                    }
                    break;
            }
        }
        catch (Exception e)
        {
            Util.impossible(e);
        }
    }

    public int decode() throws HttpException
    {
        int index = super.decode();
        decodeAfterHeader(index);
        return 0;
    }

    public int decodeAfterHeader(int index) throws HttpException
    {
        if (_type != MSG_AUTHENTICATE)
        {
            throw new HttpException("Invalid message type - expected: "
                + MSG_AUTHENTICATE
                + " got: "
                + _type);
        }

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
                _domain = Util.fromByteUnicodeLittle(domainLen,
                                                     _msgBytes,
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
                _host = Util.fromByteUnicodeLittle(hostLen,
                                                   _msgBytes,
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
                _user = Util.fromByteUnicodeLittle(userLen,
                                                   _msgBytes,
                                                   userOffset);
            }
        }
        index += userLen;

        if (lmResponseLen > 0)
        {
            _lmResponse = new byte[lmResponseLen];
            System.arraycopy(_msgBytes,
                             lmResponseOffset,
                             _lmResponse,
                             0,
                             lmResponseLen);
        }

        if (ntResponseLen > 0)
        {
            _ntResponse = new byte[ntResponseLen];
            System.arraycopy(_msgBytes,
                             ntResponseOffset,
                             _ntResponse,
                             0,
                             ntResponseLen);
        }

        log();

        return index;
    }

    public int encode()
    {
        calcResponses();

        int domainLen = _domain == null ? 0 : _domain.length();
        int hostLen = _host == null ? 0 : _host.length();
        int userLen = _user == null ? 0 : _user.length();
        int ntResponseLen = _ntResponse == null ? 0 : _ntResponse.length;
        int lmResponseLen = _lmResponse == null ? 0 : _lmResponse.length;

        // Unicode is 2 bytes per char
        if (!_encodingOem)
        {
            domainLen = domainLen * 2;
            hostLen = hostLen * 2;
            userLen = userLen * 2;
        }

        _msgLength = AUTHENTICATE_HEADER_LEN
            + hostLen
            + userLen
            + domainLen
            + ntResponseLen
            + lmResponseLen;

        _type = MSG_AUTHENTICATE;
        int index = super.encode();

        _flags |= Ntlm._authMessageFlags;

        if (_encodingOem)
            _flags |= NEGOTIATE_OEM;
        else
            _flags |= NEGOTIATE_UNICODE;

        int domainOffset = AUTHENTICATE_HEADER_LEN;
        int userOffset = domainOffset + domainLen;
        int hostOffset = userOffset + userLen;
        int lmResponseOffset = hostOffset + hostLen;
        int ntResponseOffset = lmResponseOffset + lmResponseLen;

        if (domainLen == 0)
            domainOffset = 0;

        index = Util.toByteLittle(lmResponseLen, 2, _msgBytes, index);
        index = Util.toByteLittle(lmResponseLen, 2, _msgBytes, index);
        index = Util.toByteLittle(lmResponseOffset, 4, _msgBytes, index);

        index = Util.toByteLittle(ntResponseLen, 2, _msgBytes, index);
        index = Util.toByteLittle(ntResponseLen, 2, _msgBytes, index);
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

        // Space for version - not used
        index = Util.toByteLittle(0, 4, _msgBytes, index);
        index = Util.toByteLittle(0, 4, _msgBytes, index);

        if (domainLen != 0)
        {
            if (_encodingOem)
            {
                index = Util.toByteAscii(_domain.toUpperCase(),
                                         _msgBytes,
                                         index);
            }
            else
            {
                index = Util.toByteUnicodeLittle(_domain.toUpperCase(),
                                                 _msgBytes,
                                                 index);
            }
        }

        if (_encodingOem)
        {
            if (_user != null)
                index = Util.toByteAscii(_user.toUpperCase(), _msgBytes, index);
            if (_host != null)
                index = Util.toByteAscii(_host.toUpperCase(), _msgBytes, index);
        }
        else
        {
            if (_user != null)
            {
                index = Util.toByteUnicodeLittle(_user, _msgBytes, index);
            }
            if (_host != null)
            {
                index = Util.toByteUnicodeLittle(_host.toUpperCase(),
                                                 _msgBytes,
                                                 index);
            }
        }

        if (_lmResponse != null)
        {
            System.arraycopy(_lmResponse,
                             0,
                             _msgBytes,
                             lmResponseOffset,
                             _lmResponse.length);
        }
        if (_ntResponse != null)
        {
            System.arraycopy(_ntResponse,
                             0,
                             _msgBytes,
                             ntResponseOffset,
                             _ntResponse.length);
        }

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
        if (LogUtils._logShowPasswords)
            sb.append(_password);
        else
            sb.append("<suppressed>");
        sb.append("\n");
        if (_lmResponse != null)
        {
            sb.append("  lmResponse: \n");
            sb.append(HexFormatter.dump(_lmResponse));
        }
        if (_ntResponse != null)
        {
            sb.append("  ntResponse: \n");
            sb.append(HexFormatter.dump(_ntResponse));
        }
    }

}
