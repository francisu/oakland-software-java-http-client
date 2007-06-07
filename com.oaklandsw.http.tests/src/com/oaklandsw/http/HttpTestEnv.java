package com.oaklandsw.http;

import java.security.Security;
import java.io.File;

/**
 * Environment setup for HTTP testing. This allows servers to be set up on
 * different computers for the http tests.
 */
public class HttpTestEnv
{

    // Properties that control overall testing configuration

    public static final String REQUIRE_NTLMV2        = System
                                                             .getProperty("oaklandsw.requirentlmv2");

    public static final String WINDOWS_HOST          = System
                                                             .getProperty("oaklandsw.windowshost",
                                                                          "repoman");

    public static final String LINUX_HOST            = System
                                                             .getProperty("oaklandsw.linuxhost",
                                                                          "berlioz");

    public static String       TEST_WEBEXT_SSL_HOST  = "www.verisign.com";

    // These are for the specific type of test servers, in general, anything
    // that can run on Linux will; the windows machine only gets things that
    // must run on windows

    public static String       ERROR_HOST            = System
                                                             .getProperty("oaklandsw.errorhost",
                                                                          LINUX_HOST);

    public static final String SQUID_HOST            = System
                                                             .getProperty("oaklandsw.squidhost",
                                                                          LINUX_HOST);

    public static final String APACHE_HOST           = System
                                                             .getProperty("oaklandsw.apachehost",
                                                                          LINUX_HOST);

    public static final String AUTH_PROXY_HOST       = System
                                                             .getProperty("oaklandsw.authproxyhost",
                                                                          LINUX_HOST);

    public static final String WEBDAV_HOST           = System
                                                             .getProperty("oaklandsw.webdavhost",
                                                                          LINUX_HOST);

    public static final String TOMCAT_HOST           = System
                                                             .getProperty("oaklandsw.tomcathost",
                                                                          LINUX_HOST);

    public static final String IIS_HOST              = System
                                                             .getProperty("oaklandsw.iishost",
                                                                          WINDOWS_HOST);

    public static final String NETPROXY_HOST         = System
                                                             .getProperty("oaklandsw.netproxyhost",
                                                                          WINDOWS_HOST);
    public static final String EXTERNAL_HOST         = System
                                                             .getProperty("oaklandsw.externalhost",
                                                                          "google.com");
    public static final String EXTERNAL_OPTIONS_HOST = System
                                                             .getProperty("oaklandsw.externaloptionshost",
                                                                          "sun.com");

    public static String getHttpTestRoot()
    {
        return com.oaklandsw.TestEnv.getRoot()
            + File.separator
            + "com.oaklandsw.http.tests";
    }

    public static String       HTTP_PROTOCOL                     = "http:";
    public static String       HTTPS_PROTOCOL                    = "https:";

    // IIS (windows)
    public static int          TEST_IIS_PORT                     = 80;

    // All of these are served through the same apache server
    public static int          TEST_WEBDAV_PORT                  = 80;
    public static int          TEST_WEBSERVER_PORT               = 80;
    public static int          TEST_PROXY_PORT                   = 80;

    // Tomcat
    public static int          TEST_WEBAPP_PORT                  = 8080;

    // Java error server program
    public static int          TEST_ERRORSVR_PORT                = 8086;

    // Apache authenticated proxy - virtual host
    public static int          TEST_AUTH_PROXY_PORT              = 8089;

    // Squid
    public static int          TEST_10_PROXY_PORT                = 3128;

    // Netproxy (windows)
    public static int          TEST_AUTH_PROXY_CLOSE_PORT        = 8088;

    public static String       TEST_URL_APP                      = "/oaklandsw-http";
    public static String       TEST_URL_APP_TOMCAT               = "oaklandsw-http";

    // For anything on the IIS machine, including NTLM
    public static String       TEST_IIS_USER                     = "httptest";
    public static String       TEST_IIS_PASSWORD                 = "httptestpw";
    public static String       TEST_IIS_DOMAIN                   = "oaklandsw";
    public static String       TEST_IIS_HOST                     = IIS_HOST;
    public static String       TEST_IIS_DOMAIN_USER              = TEST_IIS_DOMAIN
                                                                     + "\\"
                                                                     + TEST_IIS_USER;
    public static String       TEST_URL_APP_IIS                  = "";
    public static String       TEST_URL_APP_IIS_FORM             = "Form_JScript.asp";
    public static String       TEST_URL_APP_IIS_QUERY_STRING     = "QueryString_JScript.asp";

    // Page on local webserver
    public static String       TEST_WEBSERVER_PAGE               = "/int/index.html";

    public static final String TEST_WEBEXT_EXTERNAL_HOST         = EXTERNAL_HOST;
    public static final String TEST_WEBEXT_EXTERNAL_OPTIONS_HOST = EXTERNAL_OPTIONS_HOST;

    // This is Apache as proxy server
    public static String       TEST_PROXY_HOST                   = APACHE_HOST;

    // This is Squid as it uses HTTP 1.0
    public static String       TEST_10_PROXY_HOST                = SQUID_HOST;

    // This is NetProxy
    public static String       TEST_AUTH_PROXY_CLOSE_HOST        = NETPROXY_HOST;

    public static String       TEST_AUTH_PROXY_CLOSE_USER        = "netproxy";
    public static String       TEST_AUTH_PROXY_CLOSE_PASSWORD    = "netproxy";

    // Try the Apache proxy from linux
    // public static int TEST_AUTH_PROXY_CLOSE_PORT = 80;

    // This is Apache as a proxy
    public static String       TEST_AUTH_PROXY_HOST              = AUTH_PROXY_HOST;
    public static String       TEST_AUTH_PROXY_USER              = "testProxy";
    public static String       TEST_AUTH_PROXY_PASSWORD          = "test839Proxy";

    // Standard Apache
    public static String       TEST_WEBSERVER_HOST               = APACHE_HOST;

    // WebDav server
    public static String       TEST_WEBDAV_HOST                  = WEBDAV_HOST;

    public static String       TEST_URL_WEBSERVER                = HTTP_PROTOCOL
                                                                     + "//"
                                                                     + TEST_WEBSERVER_HOST
                                                                     + ":"
                                                                     + TEST_WEBSERVER_PORT;

    // We expect these to be protected by the user names "basic" and "digest"
    // with the password the same as the username
    public static String       TEST_URL_AUTH_BASIC               = TEST_URL_WEBSERVER
                                                                     + "/httptest/basic/index.html";
    public static String       TEST_URL_AUTH_DIGEST              = TEST_URL_WEBSERVER
                                                                     + "/httptest/digest/index.html";

    public static String       TEST_WEBEXT_SSL_URL               = HTTPS_PROTOCOL
                                                                     + "//"
                                                                     + TEST_WEBEXT_SSL_HOST
                                                                     + "/";

    public static String       TEST_WEBEXT_SSL_URL_PORT          = HTTPS_PROTOCOL
                                                                     + "//"
                                                                     + TEST_WEBEXT_SSL_HOST
                                                                     + ":443/";

    // Tomcat 4.1
    public static String       TEST_WEBAPP_HOST                  = TOMCAT_HOST;

    public static String       TEST_URL_HOST_IIS                 = HTTP_PROTOCOL
                                                                     + "//"
                                                                     + TEST_IIS_HOST
                                                                     + ":"
                                                                     + TEST_IIS_PORT
                                                                     + "/";

    public static String       TEST_URL_IIS                      = TEST_URL_HOST_IIS
                                                                     + TEST_URL_APP_IIS;

    public static String       TEST_URL_HOST_WEBAPP              = HTTP_PROTOCOL
                                                                     + "//"
                                                                     + TEST_WEBAPP_HOST
                                                                     + ":"
                                                                     + TEST_WEBAPP_PORT
                                                                     + "/";

    public static String       TEST_URL_WEBAPP                   = TEST_URL_HOST_WEBAPP
                                                                     + TEST_URL_APP_TOMCAT;

    public static String       ERRORSVR_HOST_PORT                = ERROR_HOST
                                                                     + ":"
                                                                     + TEST_ERRORSVR_PORT;

    public static String       TEST_URL_HOST_ERRORSVR            = HTTP_PROTOCOL
                                                                     + "//"
                                                                     + ERRORSVR_HOST_PORT
                                                                     + "/";

    // This is an unused port designed to fail
    public static String       TEST_URL_HOST_TIMEOUT             = HTTP_PROTOCOL
                                                                     + "//"
                                                                     + TOMCAT_HOST
                                                                     + ":8087/";

    public static boolean      SIMULATED_TIMEOUT_ENABLED         = System
                                                                         .getProperty("java.version")
                                                                         .compareTo("1.4.0") < 0;

    public static boolean      GETLOCALCERT_ENABLED              = System
                                                                         .getProperty("java.version")
                                                                         .compareTo("1.4.0") >= 0;

    public static boolean      GETSERVERCERT_ENABLED             = System
                                                                         .getProperty("java.version")
                                                                         .compareTo("1.4.0") >= 0;

    public static void setUp()
    {
        // If nothing specified assume oaklandsw implementation
        if (System.getProperty("inno") == null
            && System.getProperty("sun") == null)
        {
            System.setProperty("java.protocol.handler.pkgs", "com.oaklandsw");

            com.oaklandsw.http.HttpURLConnection
                    .setDefaultUserAgent(new com.oaklandsw.http.TestUserAgent());
        }
        /***********************************************************************
         * else if (System.getProperty("inno") != null) {
         * HTTPClient.HTTPConnection.removeDefaultModule
         * (HTTPClient.CookieModule.class); com.oaklandsw.ntlm.Ntlm.init(new
         * com.oaklandsw.ntlm.TestUserAgent()); //System.out.println("Using
         * inno"); }
         **********************************************************************/
        else
        {
            System.out.println("Using sun");
        }

        Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
        // HTTPClient.Log.setLogging(-1, true);
    }

}
