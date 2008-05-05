// Copyright 2008 Oakland Software Incorporated
package com.oaklandsw.http.sso;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import jcifs.smb.NtlmPasswordAuthentication;

/**
 * Provides mechanism to get the principal for SSO requests.
 */
public class SsoRequestWrapper extends HttpServletRequestWrapper
{

    /**
     * Request was authenticated using NTLM.
     */
    public static final String           NTLM_AUTH = "NTLM";

    protected NtlmPasswordAuthentication _principal;

    public SsoRequestWrapper(HttpServletRequest request,
            NtlmPasswordAuthentication principal)
    {
        super(request);
        _principal = principal;
    }

    public String getRemoteUser()
    {
        return _principal.getName();
    }

    public String getRemoteDomain()
    {
        return _principal.getDomain();
    }

    public Principal getUserPrincipal()
    {
        return _principal;
    }

    public String getAuthType()
    {
        return NTLM_AUTH;
    }

}
