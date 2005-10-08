//
// Copyright 2002-3003, oakland software, all rights reserved.
//
// May not be used or redistributed without specific written
// permission from oakland software.
//

package com.oaklandsw.http;

/**
 * The Credential interface is implemented by classes that contain credential
 * (user/password) information for an authentication protocol.
 */
public interface Credential
{

    /**
     * Basic authentication protocol.
     */
    public static final int AUTH_BASIC  = 1;

    /**
     * Digest authentication protocol.
     */
    public static final int AUTH_DIGEST = 2;

    /**
     * NTLM authentication protocol.
     */
    public static final int AUTH_NTLM   = 3;

}
