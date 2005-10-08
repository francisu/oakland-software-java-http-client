package com.oaklandsw.http.webext;

import javax.net.ssl.SSLSession;

public class TestHostnameVerifier
    implements
        com.oaklandsw.http.HostnameVerifier
{

    // Indicates this was called
    public boolean _used;

    // Should throw an exception
    public boolean _doThrow;

    // Tells it how to vote
    public boolean _shouldPass;

    public boolean verify(String hostName, SSLSession sess)
    {
        if (hostName == null)
            throw new IllegalArgumentException("Null hostname");
        if (sess == null)
            throw new IllegalArgumentException("Null session");

        if (_doThrow)
            throw new RuntimeException("expected exception (_doThrow)");

        _used = true;
        return _shouldPass;
    }
}
