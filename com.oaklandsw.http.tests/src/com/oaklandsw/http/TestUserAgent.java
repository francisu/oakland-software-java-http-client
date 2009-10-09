// Copyright 2002 oakland software, All rights reserved
package com.oaklandsw.http;

import com.oaklandsw.http.Credential;
import com.oaklandsw.http.HttpUserAgent;
import com.oaklandsw.http.NtlmCredential;
import com.oaklandsw.http.UserCredential;

import com.oaklandsw.util.Log;
import com.oaklandsw.util.LogUtils;


public class TestUserAgent implements HttpUserAgent {
    private static final Log _log = LogUtils.makeLogger();
    public static int _type;
    public static int _proxyType;

    // The number of times getCredential was called
    public static int _callCount;
    public static int _callCountProxy;
    public static final int GOOD = 1;
    public static final int GOOD2 = 2;
    public static final int NO_DOMAIN = 3;
    public static final int NO_USER = 4;
    public static final int NO_PASSWORD = 5;
    public static final int NO_HOST = 6;
    public static final int NO_HOST_OR_DOMAIN = 7;
    public static final int BAD = 10;
    public static final int NULL = 11;
    public static final int LOCAL = 12;
    public static final int PROXY = 13;
    public static final int NETPROXY = 14;
    public static final int OFFICESHARE_XSO = 20;
    public static final int OFFICESHARE_ICEWEB = 21;
    public static final int WEBSERVER_BASIC = 22;
    public static final int WEBSERVER_DIGEST = 23;
    public static final int WEBSERVER_IIS_DIGEST = 24;
    public static final int OAKLANDSWTEST_DOMAIN = 25;
    public static int _delayTime;

    // Overrides type if specified
    public int _localType;

    public static void resetTest() {
        _delayTime = 0;
    }

    public Credential getCredential(String realm, String url, int scheme) {
        _callCount++;

        int type = _localType;

        if (type == 0) {
            type = _type;
        }

        return getCredential(type, realm, url, scheme);
    }

    protected Credential getCredential(int type, String realm, String url,
        int scheme) {
        _log.debug("getGred: realm: " + realm + " url: " + url + " scheme: " +
            scheme);

        if (type == NULL) {
            _log.debug("TestUserAgent - Returning null cred");

            return null;
        }

        Credential cred = null;

        if (_delayTime != 0) {
            try {
                _log.debug("TestUserAgent - Sleeping for: " + _delayTime);
                Thread.sleep(_delayTime);
            } catch (InterruptedException e) {
            }
        }

        switch (scheme) {
        case Credential.AUTH_NTLM:

            NtlmCredential ntlmCred = new NtlmCredential();

            switch (type) {
            case GOOD:
                ntlmCred.setUser(HttpTestEnv.TEST_IIS_USER);
                ntlmCred.setPassword(HttpTestEnv.TEST_IIS_PASSWORD);
                ntlmCred.setDomain(HttpTestEnv.TEST_IIS_DOMAIN);
                ntlmCred.setHost(HttpTestEnv.TEST_IIS_HOST_5);

                break;

            case GOOD2:
                ntlmCred.setUser(HttpTestEnv.TEST_IIS_USER2);
                ntlmCred.setPassword(HttpTestEnv.TEST_IIS_PASSWORD2);
                ntlmCred.setDomain(HttpTestEnv.TEST_IIS_DOMAIN);
                ntlmCred.setHost(HttpTestEnv.TEST_IIS_HOST_5);

                break;

            case NO_DOMAIN:
                ntlmCred.setUser(HttpTestEnv.TEST_IIS_USER);
                ntlmCred.setPassword(HttpTestEnv.TEST_IIS_PASSWORD);
                // ntlmCred.setDomain(HttpTestEnv.TEST_IIS_DOMAIN);
                ntlmCred.setHost(HttpTestEnv.TEST_IIS_HOST_5);

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
                ntlmCred.setUser(HttpTestEnv.TEST_OAKLANDSW_TEST_USER);
                ntlmCred.setPassword(HttpTestEnv.TEST_IIS_PASSWORD);
                ntlmCred.setDomain(HttpTestEnv.TEST_OAKLANDSW_TEST_DOMAIN);
                ntlmCred.setHost(HttpTestEnv.TEST_IIS_HOST_5);

                break;
            }

            cred = ntlmCred;

            break;

        case Credential.AUTH_BASIC:
        case Credential.AUTH_DIGEST:

            UserCredential basicCred = new UserCredential();

            switch (type) {
            case GOOD:
                basicCred.setUser("jakarta");
                basicCred.setPassword("commons");

                break;

            case GOOD2:
                basicCred.setUser("uname");
                basicCred.setPassword("passwd");

                break;

            case WEBSERVER_BASIC:
                basicCred.setUser("basic");
                basicCred.setPassword("basic");

                break;

            case WEBSERVER_DIGEST:
                basicCred.setUser("digest");
                basicCred.setPassword("digest");

                break;

            case WEBSERVER_IIS_DIGEST:
                basicCred.setUser("httptestdom");
                basicCred.setPassword("httptestdompw");

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

        _log.debug("TestUserAgent - Returning cred: " +
            ((UserCredential) cred).getUser());

        return cred;
    }

    private void setLocalCreds(UserCredential basicCred, String realm) {
        if ((realm == null) || realm.equals("realm") || realm.equals("realm1")) {
            basicCred.setUser("username");
            basicCred.setPassword("password");
        } else if (realm.equals("realm2")) {
            basicCred.setUser("uname2");
            basicCred.setPassword("password2");
        } else if (realm.equals("Protected")) {
            basicCred.setUser("name");
            basicCred.setPassword("pass");
        } else {
            basicCred.setUser("nameOther");
            basicCred.setPassword("passOther");
        }
    }

    public Credential getProxyCredential(String realm, String url, int scheme) {
        _log.debug("getProxyGred: " + realm + " url: " + url + " scheme: " +
            scheme);

        _callCountProxy++;

        if (_proxyType == NULL) {
            _log.debug("TestUserAgent - Returning null proxy cred");

            return null;
        }

        if (_delayTime != 0) {
            try {
                _log.debug("TestUserAgent - Sleeping for: " + _delayTime);
                Thread.sleep(_delayTime);
            } catch (InterruptedException e) {
            }
        }

        Credential cred = null;

        switch (scheme) {
        case Credential.AUTH_BASIC:
        case Credential.AUTH_DIGEST:

            UserCredential basicCred = new UserCredential();

            switch (_proxyType) {
            case PROXY:
                basicCred.setUser(HttpTestEnv.TEST_AUTH_PROXY_USER);
                basicCred.setPassword(HttpTestEnv.TEST_AUTH_PROXY_PASSWORD);

                break;

            case NETPROXY:
                basicCred.setUser(HttpTestEnv.TEST_AUTH_PROXY_CLOSE_USER);
                basicCred.setPassword(HttpTestEnv.TEST_AUTH_PROXY_CLOSE_PASSWORD);

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

        _log.debug("TestUserAgent - Returning cred: " +
            ((UserCredential) cred).getUser());

        return cred;
    }
}
