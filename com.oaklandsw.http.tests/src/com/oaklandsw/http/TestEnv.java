package com.oaklandsw.http;

import java.security.Security;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class TestEnv

{

    public static String       HOST        = null;
    public static String       ERROR_HOST  = null;

    // FIXME - why is squid host different
    public static final String SQUID_HOST  = System
                                                   .getProperty("oaklandsw.squidhost",
                                                                "berlioz");

    public static final String WEBDAV_HOST = System
                                                   .getProperty("oaklandsw.webdavhost",
                                                                "berlioz");

    public static int          PORT;

    public static final String LOCALHOST   = "127.0.0.1";

    static
    {
        String defaultHost;
        try
        {
            defaultHost = InetAddress.getLocalHost().getHostName();
        }
        catch (UnknownHostException ex)
        {
            defaultHost = LOCALHOST;
        }

        HOST = System.getProperty("oaklandsw.localhost", defaultHost);

        // Error host defaults to localhost if not set
        ERROR_HOST = System.getProperty("oaklandsw.errorhost", HOST);

        String portString = System.getProperty("oaklandsw.localport", "8081");
        int tempPort = 8081;
        try
        {
            tempPort = Integer.parseInt(portString);
        }
        catch (Exception e)
        {
            tempPort = 8081;
        }
        PORT = tempPort;

        // setUp();
    }

    public static String getHttpTestRoot()
    {
        return com.oaklandsw.TestEnv.getRoot()
            + File.separator
            + "com.oaklandsw.http.tests";
    }

    public static String  _protocol                      = "http:";

    public static String  TEST_URL_APP_IIS               = "httptest/";

    public static int     TEST_IIS_PORT                  = 8080;
    public static int     TEST_TOMCAT_PORT               = PORT;
    public static int     TEST_TOMCAT3_PORT              = 8085;

    public static int     TEST_ERRORSVR_PORT             = 8086;

    public static String  TEST_URL_APP                   = "/oaklandsw";
    public static String  TEST_URL_APP_TOMCAT3           = "oaklandsw";
    public static String  TEST_URL_APP_TOMCAT            = "oaklandsw";

    // For anything on the IIS machine, including NTLM
    public static String  TEST_IIS_USER                  = "httptest";
    public static String  TEST_IIS_PASSWORD              = "httptestpw";
    public static String  TEST_IIS_DOMAIN                = "workgroup";
    public static String  TEST_IIS_HOST                  = HOST;
    public static String  TEST_IIS_DOMAIN_USER           = TEST_IIS_DOMAIN
                                                             + "\\"
                                                             + TEST_IIS_USER;

    // This is Apache as proxy server
    public static String  TEST_PROXY_HOST                = HOST;
    public static int     TEST_PROXY_PORT                = 80;

    // This is Squid as it uses HTTP 1.0
    public static String  TEST_10_PROXY_HOST             = SQUID_HOST;
    public static int     TEST_10_PROXY_PORT             = 3128;

    // This is NetProxy
    public static String  TEST_AUTH_PROXY_CLOSE_HOST     = HOST;
    public static int     TEST_AUTH_PROXY_CLOSE_PORT     = 8088;

    public static String  TEST_AUTH_PROXY_CLOSE_USER     = "netproxy";
    public static String  TEST_AUTH_PROXY_CLOSE_PASSWORD = "netproxy";

    // Try the Apache proxy from linux
    // public static int TEST_AUTH_PROXY_CLOSE_PORT = 80;

    // This is Apache as a proxy
    public static String  TEST_AUTH_PROXY_HOST           = HOST;
    public static int     TEST_AUTH_PROXY_PORT           = 8089;
    public static String  TEST_AUTH_PROXY_USER           = "testProxy";
    public static String  TEST_AUTH_PROXY_PASSWORD       = "test839Proxy";

    // Standard Apache
    public static String  TEST_WEBSERVER_HOST            = HOST;
    public static int     TEST_WEBSERVER_PORT            = 80;

    // WebDav server
    public static String  TEST_WEBDAV_HOST               = WEBDAV_HOST;
    public static int     TEST_WEBDAV_PORT               = 80;

    public static String  TEST_URL_WEBSERVER             = "http://"
                                                             + TEST_WEBSERVER_HOST
                                                             + ":"
                                                             + TEST_WEBSERVER_PORT;

    public static String  TEST_WEBEXT_SSL_HOST           = "www.verisign.com";
    public static String  TEST_WEBEXT_SSL_URL            = "https://"
                                                             + TEST_WEBEXT_SSL_HOST
                                                             + "/";

    public static String  TEST_WEBEXT_SSL_URL_PORT       = "https://"
                                                             + TEST_WEBEXT_SSL_HOST
                                                             + ":443/";

    // Tomcat 4.1
    public static String  TEST_WEBAPP_HOST               = HOST;
    public static int     TEST_WEBAPP_PORT               = 8081;

    // FIX this to use above constants
    public static String  TEST_URL_HOST_IIS              = "//"
                                                             + HOST
                                                             + ":"
                                                             + TEST_IIS_PORT
                                                             + "/";

    public static String  TEST_URL_IIS                   = TEST_URL_HOST_IIS
                                                             + TEST_URL_APP_IIS;

    public static String  TEST_URL_HOST_TOMCAT           = _protocol
                                                             + "//"
                                                             + HOST
                                                             + ":"
                                                             + TEST_TOMCAT_PORT
                                                             + "/";

    public static String  TEST_URL_HOST_TOMCAT3          = _protocol
                                                             + "//"
                                                             + HOST
                                                             + ":"
                                                             + +TEST_TOMCAT3_PORT
                                                             + "/";

    public static String  TEST_URL_TOMCAT                = System
                                                                 .getProperty("cat",
                                                                              TEST_URL_HOST_TOMCAT
                                                                                  + TEST_URL_APP_TOMCAT);

    public static String  ERROR_HOST_PORT                = ERROR_HOST
                                                             + ":"
                                                             + TEST_ERRORSVR_PORT;

    public static String  TEST_URL_HOST_ERROR            = _protocol
                                                             + "//"
                                                             + ERROR_HOST_PORT
                                                             + "/";

    public static String  TEST_URL_HOST_TIMEOUT          = _protocol
                                                             + "//"
                                                             + HOST
                                                             + ":8087/";

    public static boolean SIMULATED_TIMEOUT_ENABLED      = System
                                                                 .getProperty("java.version")
                                                                 .compareTo("1.4.0") < 0;

    public static boolean GETLOCALCERT_ENABLED           = System
                                                                 .getProperty("java.version")
                                                                 .compareTo("1.4.0") >= 0;

    public static boolean GETSERVERCERT_ENABLED          = System
                                                                 .getProperty("java.version")
                                                                 .compareTo("1.4.0") >= 0;

    // public static String TEST_URL_TOMCAT =
    // TEST_URL_HOST_TOMCAT3 + TEST_URL_APP_TOMCAT3;

    // public static String TEST_URL_TOMCAT =
    //    

    public static boolean _oaklandsw;

    public static void setUp()
    {
        // If nothing specified assume oaklandsw implementation
        if (System.getProperty("inno") == null
            && System.getProperty("sun") == null)
        {
            System.setProperty("java.protocol.handler.pkgs", "com.oaklandsw");

            com.oaklandsw.http.HttpURLConnection
                    .setDefaultUserAgent(new com.oaklandsw.http.TestUserAgent());
            // System.out.println("Using oaklandsw");
            _oaklandsw = true;
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
