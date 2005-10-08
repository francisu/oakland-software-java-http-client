//
// Copyright 2003, oakland software, all rights reserved.
//
// May not be used or redistributed without specific written
// permission from oakland software.
//
package com.oaklandsw.http;

import javax.net.ssl.SSLSession;

/**
 * An interface to allow the acceptance of SSL connections where the hostname
 * does not match the host on the certificate.
 * 
 */
public interface HostnameVerifier
{

    /**
     * Called when an SSL connection is established and the hostname on the
     * certificate does not match the hostname to which the connection is made.
     * 
     * @param hostName
     *            the host name to which the connection was made
     * @param session
     *            the SSLSession associated with this connection.
     * @return true, if the connection is allowed to proceed, false if it should
     *         fail.
     */
    public boolean verify(String hostName, SSLSession session);

}
