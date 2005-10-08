//
// Copyright 2002-3003, oakland software, all rights reserved.
//
// May not be used or redistributed without specific written
// permission from oakland software.
//

package com.oaklandsw.http;

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

    public String toString()
    {
        return "User: " + _user + " Password: " + _password;
    }

}
