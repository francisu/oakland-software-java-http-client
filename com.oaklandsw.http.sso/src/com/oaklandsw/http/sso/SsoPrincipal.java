package com.oaklandsw.http.sso;

import java.security.Principal;

public class SsoPrincipal implements Principal
{

    protected String _userName;
    protected String _domain;

    protected byte[] _nonce;
    protected byte[] _ntHash;
    protected byte[] _lmHash;

    public SsoPrincipal(String userName,
            String domain,
            byte[] nonce,
            byte[] lmHash,
            byte[] ntHash)
    {
        _userName = userName;
        _domain = domain;
        _nonce = nonce;
        _lmHash = lmHash;
        _ntHash = ntHash;
    }

    public String getName()
    {
        return _userName;
    }

    public String getDomain()
    {
        return _domain;
    }

    byte[] getNonce()
    {
        return _nonce;
    }

    byte[] getNtHash()
    {
        return _ntHash;
    }

    byte[] getLmHash()
    {
        return _lmHash;
    }

}
