//
// Copyright 2002-3003, oakland software, all rights reserved.
//
// May not be used or redistributed without specific written
// permission from oakland software.
//

package com.oaklandsw.http;

/**
 * Contains the credentials specific to the NTLM authentication protocol.
 */
public class NtlmCredential extends UserCredential implements Credential
{

    protected String _host;

    protected String _domain;

    public NtlmCredential()
    {
    }

    public NtlmCredential(String user,
            String password,
            String host,
            String domain)
    {
        super(user, password);
        _host = host;
        _domain = domain;
    }

    /**
     * Gets the value of host
     * 
     * @return the value of host
     */
    public String getHost()
    {
        return _host;
    }

    /**
     * Sets the value of host
     * 
     * @param argHost
     *            Value to assign to host
     */
    public void setHost(String argHost)
    {
        _host = argHost;
    }

    /**
     * Gets the value of domain
     * 
     * @return the value of domain
     */
    public String getDomain()
    {
        return _domain;
    }

    /**
     * Sets the value of domain
     * 
     * @param argDomain
     *            Value to assign to domain
     */
    public void setDomain(String argDomain)
    {
        _domain = argDomain;
    }

    public String toString()
    {
        return "Host: "
            + _host
            + " Domain: "
            + _domain
            + " "
            + super.toString();
    }

}
