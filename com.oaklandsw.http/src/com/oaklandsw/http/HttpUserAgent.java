//
// Copyright 2002-3003, oakland software, all rights reserved.
//
// May not be used or redistributed without specific written
// permission from oakland software.
//

package com.oaklandsw.http;

/**
 * Represents the user agent for the HTTP client. This interface is optionally
 * implemented by the user if the user requires notification of certain HTTP
 * events.
 */
public interface HttpUserAgent
{

    /**
     * Get the authentication credentials. This is used when a request for
     * authentication has occurred.
     * 
     * @param url
     *            the URL of the request
     * @param scheme
     *            the value representing the authentication scheme as found in
     *            Credential
     */
    public Credential getCredential(String realm, String url, int scheme);

    /**
     * Get the proxy authentication credentials. This is used when a request for
     * authentication has occurred.
     * 
     * @param url
     *            the URL of the request
     * @param scheme
     *            the value representing the authentication scheme as found in
     *            Credential
     */
    public Credential getProxyCredential(String realm, String url, int scheme);

}
