package com.oaklandsw.http.webext;

import java.io.IOException;
import java.net.Socket;

import javax.net.ssl.SSLSocketFactory;

public class TestSSLSocketFactory extends SSLSocketFactory
{

    // Indicates this factory was used to create the socket
    public boolean                    _used;

    // Used to test handling of null return
    public boolean                    _returnNull;

    protected static SSLSocketFactory _default;

    static
    {
        _default = (SSLSocketFactory)SSLSocketFactory.getDefault();
    }

    public Socket createSocket(String host, int port) throws IOException
    {
        _used = true;
        return _default.createSocket(host, port);
    }

    public Socket createSocket(java.net.InetAddress addr, int port)
        throws IOException
    {
        _used = true;
        return _default.createSocket(addr, port);
    }

    public Socket createSocket(java.net.InetAddress addr1,
                               int port1,
                               java.net.InetAddress addr2,
                               int port2) throws IOException
    {
        _used = true;
        return _default.createSocket(addr1, port1, addr2, port2);
    }

    public Socket createSocket(String addr1,
                               int port1,
                               java.net.InetAddress addr2,
                               int port2) throws IOException
    {
        _used = true;
        return _default.createSocket(addr1, port1, addr2, port2);
    }

    public Socket createSocket(Socket s,
                               String host,
                               int port,
                               boolean autoclose) throws IOException
    {
        _used = true;
        if (_returnNull)
            return null;
        return _default.createSocket(s, host, port, autoclose);
    }

    public String[] getDefaultCipherSuites()
    {
        return _default.getDefaultCipherSuites();
    }

    public String[] getSupportedCipherSuites()
    {
        return _default.getSupportedCipherSuites();
    }

}
