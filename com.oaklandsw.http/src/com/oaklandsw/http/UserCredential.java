//
// Copyright 2002-3003, oakland software, all rights reserved.
//
// May not be used or redistributed without specific written
// permission from oakland software.
//

package com.oaklandsw.http;

import com.oaklandsw.util.Util;

/**
 * Records the user/password credentials required by authentication protocols.
 */
public class UserCredential implements Credential
{

    protected String _user;

    protected String _password;

    public UserCredential()
    {
    }

    public UserCredential(String user, String password)
    {
        _user = user;
        _password = password;
    }

    /**
     * Gets the value of user
     * 
     * @return the value of user
     */
    public String getUser()
    {
        return _user;
    }

    /**
     * Sets the value of user
     * 
     * @param argUser
     *            Value to assign to user
     */
    public void setUser(String argUser)
    {
        _user = argUser;
    }

    /**
     * Gets the value of password
     * 
     * @return the value of password
     */
    public String getPassword()
    {
        return _password;
    }

    /**
     * Sets the value of password
     * 
     * @param argPassword
     *            Value to assign to password
     */
    public void setPassword(String argPassword)
    {
        _password = argPassword;
    }

    public static Credential createCredential(String userID, String passwd)
    {
        Credential cred = null;

        // if the username is in the form "user\domain"
        // then use NTCredentials instead.
        int domainIndex = userID.indexOf("\\");
        if (domainIndex > 0)
        {
            String domain = userID.substring(0, domainIndex);
            if (userID.length() > domainIndex + 1)
            {
                String user = userID.substring(domainIndex + 1);
                cred = new NtlmCredential(user,
                                          passwd,
                                          Util.getHostName(),
                                          domain);
            }
        }
        else
        {
            cred = new UserCredential(userID, passwd);

        }
        return cred;
    }

    // The part of the credential that needs to be compared to determine
    // if the associated session matches
    String getKey()
    {
        return _user;
    }

    // Does this auth type support session (connection) level authentication?
    // Only NTLM does for now.
    static boolean useConnectionAuthentication(int authType)
    {
        switch (authType)
        {
            // Zero is valid here because there is no auth for this connection
            case 0:
            case AUTH_BASIC:
            case AUTH_DIGEST:
                return false;
            case AUTH_NTLM:
                return true;
            default:
                Util.impossible("Invalid auth type: " + authType);
                return false;
        }
    }

    public String toString()
    {
        return "User: " + _user + " Password: " + _password;
    }

}
