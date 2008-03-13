// Copyright 2002 oakland software, All rights reserved

package com.oaklandsw.http;

import org.apache.commons.logging.Log;

import com.oaklandsw.http.Credential;
import com.oaklandsw.http.HttpUserAgent;
import com.oaklandsw.http.NtlmCredential;
import com.oaklandsw.http.UserCredential;
import com.oaklandsw.util.LogUtils;

public class TestUserAgent implements HttpUserAgent
{
    private static final Log _log                 = LogUtils.makeLogger();

    public static int        _type;

    public static int        _proxyType;

    // The number of times getCredential was called
    public static int        _callCount;
    public static int        _callCountProxy;

    public static final int  GOOD                 = 1;
    public static final int  NO_DOMAIN            = 2;
    public static final int  NO_USER              = 3;
    public static final int  NO_PASSWORD          = 4;
    public static final int  NO_HOST              = 5;
    public static final int  NO_HOST_OR_DOMAIN    = 6;
    public static final int  BAD                  = 10;
    public static final int  NULL                 = 11;
    public static final int  LOCAL                = 12;
    public static final int  PROXY                = 13;
    public static final int  NETPROXY             = 14;
    public static final int  OFFICESHARE_XSO      = 20;
    public static final int  OFFICESHARE_ICEWEB   = 21;
    public static final int  WEBSERVER_BASIC      = 22;
    public static final int  WEBSERVER_DIGEST     = 23;
    public static final int  OAKLANDSWTEST_DOMAIN = 24;

    public Credential getCredential(String realm, String url, int scheme)
    {
        _callCount++;
        return getCredential(_type, realm, url, scheme);
    }

    protected Credential getCredential(int type,
                                       String realm,
                                       String url,
                                       int scheme)
    {
        _log.debug("getGred: realm: "
            + realm
            + " url: "
            + url
            + " scheme: "
            + scheme);

        if (type == NULL)
        {
            _log.debug("TestUserAgent - Returning null cred");
            return null;
        }

        Credential cred = null;

        switch (scheme)
        {
            case Credential.AUTH_NTLM:
                NtlmCredential ntlmCred = new NtlmCredential();

                switch (type)
                {
                    case GOOD:
                        ntlmCred.setUser(HttpTestEnv.TEST_IIS_USER);
                        ntlmCred.setPassword(HttpTestEnv.TEST_IIS_PASSWORD);
                        ntlmCred.setDomain(HttpTestEnv.TEST_IIS_DOMAIN);
                        ntlmCred.setHost(HttpTestEnv.TEST_IIS_HOST);
                        break;

                    case NO_DOMAIN:
                        ntlmCred.setUser(HttpTestEnv.TEST_IIS_USER);
                        ntlmCred.setPassword(HttpTestEnv.TEST_IIS_PASSWORD);
                        // ntlmCred.setDomain(HttpTestEnv.TEST_IIS_DOMAIN);
                        ntlmCred.setHost(HttpTestEnv.TEST_IIS_HOST);
                        break;

                    case NO_HOST:
                        ntlmCred.setUser(HttpTestEnv.TEST_IIS_USER);
                        ntlmCred.setPassword(HttpTestEnv.TEST_IIS_PASSWORD);
                        ntlmCred.setDomain(HttpTestEnv.TEST_IIS_DOMAIN);
                        // ntlmCred.setHost(HttpTestEnv.TEST_IIS_HOST);
                        break;

                    case NO_HOST_OR_DOMAIN:
                        ntlmCred.setUser(HttpTestEnv.TEST_IIS_USER);
                        ntlmCred.setPassword(HttpTestEnv.TEST_IIS_PASSWORD);
                        // ntlmCred.setDomain(HttpTestEnv.TEST_IIS_DOMAIN);
                        // ntlmCred.setHost(HttpTestEnv.TEST_IIS_HOST);
                        break;

                    case NO_USER:
                        // ntlmCred.setUser(HttpTestEnv.TEST_IIS_USER);
                        ntlmCred.setPassword(HttpTestEnv.TEST_IIS_PASSWORD);
                        ntlmCred.setDomain(HttpTestEnv.TEST_IIS_DOMAIN);
                        // ntlmCred.setHost(HttpTestEnv.TEST_IIS_HOST);
                        break;

                    case NO_PASSWORD:
                        ntlmCred.setUser(HttpTestEnv.TEST_IIS_USER);
                        // ntlmCred.setPassword(HttpTestEnv.TEST_IIS_PASSWORD);
                        // ntlmCred.setDomain(HttpTestEnv.TEST_IIS_DOMAIN);
                        // ntlmCred.setHost(HttpTestEnv.TEST_IIS_HOST);
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
                    case OFFICESHARE_XSO:
                        ntlmCred.setUser("test");
                        ntlmCred.setPassword("Xsolive2007");
                        ntlmCred.setDomain("demo");
                        ntlmCred.setHost("host");
                        break;
                    case OFFICESHARE_ICEWEB:
                        ntlmCred.setUser(HttpTestEnv.TEST_ICEWEB_USER);
                        ntlmCred.setPassword(HttpTestEnv.TEST_ICEWEB_PASSWORD);
                        ntlmCred.setDomain(HttpTestEnv.TEST_ICEWEB_DOMAIN);
                        ntlmCred.setHost("host");
                        break;
                    case OAKLANDSWTEST_DOMAIN:
                        ntlmCred.setUser(HttpTestEnv.TEST_IIS_USER);
                        ntlmCred.setPassword(HttpTestEnv.TEST_IIS_PASSWORD);
                        ntlmCred.setDomain("oaklandswtest.com");
                        ntlmCred.setHost(HttpTestEnv.TEST_IIS_HOST);
                        break;

                }
                cred = ntlmCred;
                break;

            case Credential.AUTH_BASIC:
            case Credential.AUTH_DIGEST:
                UserCredential basicCred = new UserCredential();
                switch (type)
                {
                    case GOOD:
                        basicCred.setUser("jakarta");
                        basicCred.setPassword("commons");
                        break;

                    case WEBSERVER_BASIC:
                        basicCred.setUser("basic");
                        basicCred.setPassword("basic");
                        break;

                    case WEBSERVER_DIGEST:
                        basicCred.setUser("digest");
                        basicCred.setPassword("digest");
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

        _log.debug("TestUserAgent - Returning cred: "
            + ((UserCredential)cred).getUser());

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
        else
        {
            basicCred.setUser("nameOther");
            basicCred.setPassword("passOther");
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

        _callCountProxy++;

        if (_proxyType == NULL)
        {
            _log.debug("TestUserAgent - Returning null proxy cred");
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
                        basicCred.setUser(HttpTestEnv.TEST_AUTH_PROXY_USER);
                        basicCred
                                .setPassword(HttpTestEnv.TEST_AUTH_PROXY_PASSWORD);
                        break;

                    case NETPROXY:
                        basicCred
                                .setUser(HttpTestEnv.TEST_AUTH_PROXY_CLOSE_USER);
                        basicCred
                                .setPassword(HttpTestEnv.TEST_AUTH_PROXY_CLOSE_PASSWORD);
                        break;

                    case LOCAL:
                        setLocalCreds(basicCred, realm);
                        break;
                }
                cred = basicCred;
                break;
            case Credential.AUTH_NTLM:
                cred = getCredential(_proxyType, realm, url, scheme);
                break;
        }

        _log.debug("TestUserAgent - Returning cred: "
            + ((UserCredential)cred).getUser());

        return cred;
    }
}
