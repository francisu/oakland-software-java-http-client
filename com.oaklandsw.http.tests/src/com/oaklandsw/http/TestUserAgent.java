// Copyright 2002 oakland software, All rights reserved

package com.oaklandsw.http;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.oaklandsw.http.Credential;
import com.oaklandsw.http.HttpUserAgent;
import com.oaklandsw.http.NtlmCredential;
import com.oaklandsw.http.UserCredential;

public class TestUserAgent implements HttpUserAgent
{
    private static final Log _log     = LogFactory.getLog(TestUserAgent.class);

    public static int        _type;

    public static int        _proxyType;

    public static final int  GOOD     = 1;
    public static final int  BAD      = 2;
    public static final int  NULL     = 3;
    public static final int  LOCAL    = 4;
    public static final int  PROXY    = 5;
    public static final int  NETPROXY = 6;

    public Credential getCredential(String realm, String url, int scheme)
    {

        _log.debug("getGred: " + realm + " url: " + url + " scheme: " + scheme);

        if (!url.startsWith("http://"))
        {
            throw new RuntimeException("Invalid URL String: " + url);
        }

        if (_type == NULL)
        {
            _log.debug("Returning null cred");
            return null;
        }

        Credential cred = null;

        switch (scheme)
        {
            case Credential.AUTH_NTLM:
                NtlmCredential ntlmCred = new NtlmCredential();

                switch (_type)
                {
                    case GOOD:
                        ntlmCred.setUser(TestEnv.TEST_IIS_USER);
                        ntlmCred.setPassword(TestEnv.TEST_IIS_PASSWORD);
                        ntlmCred.setDomain(TestEnv.TEST_IIS_DOMAIN);
                        ntlmCred.setHost(TestEnv.TEST_IIS_HOST);
                        break;

                    case BAD:
                        ntlmCred.setUser("testuser");
                        ntlmCred.setPassword("testpass");
                        ntlmCred.setDomain("testdomain");
                        ntlmCred.setHost("testhost");
                        break;

                    case LOCAL:
                        ntlmCred.setUser("username");
                        ntlmCred.setPassword("password");
                        ntlmCred.setDomain("domain");
                        ntlmCred.setHost("host");
                        break;
                }
                cred = ntlmCred;
                break;

            case Credential.AUTH_BASIC:
            case Credential.AUTH_DIGEST:
                UserCredential basicCred = new UserCredential();
                switch (_type)
                {
                    case GOOD:
                        basicCred.setUser("jakarta");
                        basicCred.setPassword("commons");
                        break;

                    case BAD:
                        basicCred.setUser("bad");
                        basicCred.setPassword("creds");
                        break;

                    case LOCAL:
                        setLocalCreds(basicCred, realm);
                        break;
                }
                cred = basicCred;
                break;
        }

        _log.debug("Returning cred: " + ((UserCredential)cred).getUser());

        return cred;
    }

    private void setLocalCreds(UserCredential basicCred, String realm)
    {

        if (realm == null || realm.equals("realm") || realm.equals("realm1"))
        {
            basicCred.setUser("username");
            basicCred.setPassword("password");
        }
        else if (realm.equals("realm2"))
        {
            basicCred.setUser("uname2");
            basicCred.setPassword("password2");
        }
        else if (realm.equals("Protected"))
        {
            basicCred.setUser("name");
            basicCred.setPassword("pass");
        }

    }

    public Credential getProxyCredential(String realm, String url, int scheme)
    {
        _log.debug("getProxyGred: "
            + realm
            + " url: "
            + url
            + " scheme: "
            + scheme);

        if (!url.startsWith("http://"))
        {
            throw new RuntimeException("Invalid URL String: " + url);
        }

        if (_proxyType == NULL)
        {
            _log.debug("Returning null proxy cred");
            return null;
        }

        Credential cred = null;

        switch (scheme)
        {
            case Credential.AUTH_BASIC:
            case Credential.AUTH_DIGEST:
                UserCredential basicCred = new UserCredential();
                switch (_proxyType)
                {
                    case PROXY:
                        basicCred.setUser(TestEnv.TEST_AUTH_PROXY_USER);
                        basicCred.setPassword(TestEnv.TEST_AUTH_PROXY_PASSWORD);
                        break;

                    case NETPROXY:
                        basicCred.setUser(TestEnv.TEST_AUTH_PROXY_CLOSE_USER);
                        basicCred.setPassword(TestEnv.TEST_AUTH_PROXY_CLOSE_PASSWORD);
                        break;

                    case LOCAL:
                        setLocalCreds(basicCred, realm);
                        break;
                }
                cred = basicCred;
                break;
        }

        _log.debug("Returning cred: " + ((UserCredential)cred).getUser());

        return cred;
    }
}
