/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oaklandsw.http;

import java.io.File;

import com.oaklandsw.util.SystemUtils;


/**
 * Environment setup for HTTP testing. This allows servers to be set up on
 * different computers for the http tests.
 */
public class HttpTestEnv {
    // Properties that control overall testing configuration
    public static final String REQUIRE_NTLMV2 = System.getProperty(
            "oaklandsw.requirentlmv2");

    // Configured for NTLM level 5
    public static final String WINDOWS_HOST = System.getProperty("oaklandsw.windowshost",
            "repoman"); // repoman

    // Configured for NTLM level 5
    public static final String WIN2K3_HOST = System.getProperty("oaklandsw.win2k3host",
            "win2k3");

    // Configured for NTLM level 0
    public static final String WIN2K3_2_HOST = System.getProperty("oaklandsw.win2k3_2host",
            "192.168.1.27");
    public static final String LINUX_HOST = System.getProperty("oaklandsw.linuxhost",
            "192.168.1.25");
    public static final String LINUX_HOST6 = System.getProperty("oaklandsw.linuxhost",
            "fe80::218:f3ff:fe5e:6538");
    public static String TEST_WEBEXT_SSL_HOST = "www.verisign.com";

    // These are for the specific type of test servers, in general, anything
    // that can run on Linux will; the windows machine only gets things that
    // must run on windows
    public static String ERROR_HOST = System.getProperty("oaklandsw.errorhost",
            LINUX_HOST);
    public static final String SQUID_HOST = System.getProperty("oaklandsw.squidhost",
            LINUX_HOST);
    public static final String APACHE_HOST = System.getProperty("oaklandsw.apachehost",
            LINUX_HOST);
    public static final String AUTH_PROXY_HOST = System.getProperty("oaklandsw.authproxyhost",
            LINUX_HOST);
    public static final String WEBDAV_HOST = System.getProperty("oaklandsw.webdavhost",
            LINUX_HOST);
    public static final String TOMCAT_HOST = System.getProperty("oaklandsw.tomcathost",
            LINUX_HOST);
    public static final String FTP_HOST = System.getProperty("oaklandsw.ftphost",
            LINUX_HOST);
    public static final String IIS_HOST_5 = System.getProperty("oaklandsw.iishost_5",
            WINDOWS_HOST);
    public static final String IIS_HOST_0 = System.getProperty("oaklandsw.iishost_0",
            WIN2K3_2_HOST);
    public static final String IIS_DIGEST_HOST = System.getProperty("oaklandsw.iisdigesthost",
            WIN2K3_HOST);
    public static final String ISA_HOST = System.getProperty("oaklandsw.iishost",
            WIN2K3_HOST);
    public static final String NETPROXY_HOST = System.getProperty("oaklandsw.netproxyhost",
            WINDOWS_HOST);
    public static final String EXTERNAL_HOST = System.getProperty("oaklandsw.externalhost",
            "google.com");
    public static final String EXTERNAL_OPTIONS_HOST = System.getProperty("oaklandsw.externaloptionshost",
            "sun.com");
    public static final String SOCKS_HOST = System.getProperty("oaklandsw.socksproxyhost",
            ISA_HOST);

    //
    // Windows ports
    // 

    // IIS (windows)
    public static int IIS_PORT = 80;

    // Netproxy (windows)
    public static int AUTH_PROXY_CLOSE_PORT = 8088;

    //
    // win2k3[_x] ports
    // 

    // ISA (win2k3 vm)
    public static int ISA_PORT = 8080;
    public static int ISA_SSL_PORT = 8443;

    // IIS (digest md5-sess auth)
    public static int IIS_DIGEST_PORT = 80;

    //
    // Linux host ports
    //

    // All of these are served through the same apache server
    public static int NORMAL_PROXY_PORT = 8091;
    public static int WEBDAV_PORT = NORMAL_PROXY_PORT;
    public static int WEBSERVER_PORT = NORMAL_PROXY_PORT;
    public static int SSL_PORT = 8443;

    // Apache authenticated proxy - virtual host
    public static int AUTH_PROXY_PORT = 8089;

    // Tomcat

    // Tomcat 1 has the webapps and the normal JCIFS config
    public static int TOMCAT_PORT_1 = 8080;

    // Tomcat 2-4 are not used (for JCIFS which is not used)

    // Tomcat 2 has Oaklandsw JCIFS NTLMv2 level 0 (win2k3-2)
    public static int TOMCAT_PORT_2 = 8182;

    // Tomcat 3 has Oaklandsw JCIFS NTLMv2 level 5 (win2k3)
    public static int TOMCAT_PORT_3 = 8183;

    // Tomcat 4 has Oaklandsw JCIFS NTLMv2 level 5 (repoman)
    public static int TOMCAT_PORT_4 = 8184;

    // Java error server program
    public static int ERRORSVR_PORT = 8086;

    // Squid
    public static int TEST_10_PROXY_PORT = 3128;
    public static int SOCKS_PROXY_PORT = 1080;
    public static String HTTP_PROTOCOL = "http:";
    public static String HTTPS_PROTOCOL = "https:";
    public static String TEST_URL_APP_TOMCAT_1 = "oaklandsw-http";
    public static String TEST_URL_APP_TOMCAT_2 = "oaklandsw-http2";
    public static String TEST_URL_APP_TOMCAT_3 = "oaklandsw-http3";
    public static String TEST_URL_APP_TOMCAT_4 = "oaklandsw-http4";

    // For anything on the IIS machine, including NTLM
    public static String TEST_IIS_USER = "httptest";
    public static String TEST_IIS_PASSWORD = "httptestpw";
    public static String TEST_IIS_USER2 = "httptest2";
    public static String TEST_IIS_PASSWORD2 = "httptestpw2";
    public static String TEST_IIS_DOMAIN = "oaklandsw";
    public static String TEST_IIS_HOST_0 = IIS_HOST_0;
    public static String TEST_IIS_HOST_5 = IIS_HOST_5;
    public static String TEST_IIS_DOMAIN_USER = TEST_IIS_DOMAIN + "\\" +
        TEST_IIS_USER;
    public static String TEST_IIS_DIGEST_USER = "httptestdom";
    public static String TEST_IIS_DIGEST_PASSWORD = "httptestdompw";
    public static String TEST_OAKLANDSW_TEST_DOMAIN = "oaklandswtest.com";
    public static String TEST_OAKLANDSW_TEST_USER = "http_oaklandswtest";
    public static String TEST_OAKLANDSW_TEST_PASSWORD = TEST_IIS_PASSWORD;
    public static String TEST_URL_APP_IIS = "";
    public static String TEST_URL_APP_IIS_FORM = "Form_JScript.asp";
    public static String TEST_URL_APP_IIS_QUERY_STRING = "QueryString_JScript.asp";

    // For ISA on win2k3
    public static String TEST_ISA_USER = "httptest";
    public static String TEST_ISA_PASSWORD = "httptestpw";

    // Officeshare iceweb
    public static String TEST_ICEWEB_URL = "http://sharepoint.iceweb.com/sites/demo/_vti_bin/Lists.asmx";
    public static String TEST_ICEWEB_USER = "demo";
    public static String TEST_ICEWEB_PASSWORD = "demo";
    public static String TEST_ICEWEB_DOMAIN = "icemail";

    // XsoLive
    public static String TEST_XSOLIVE_URL = "http://74.218.125.36/_vti_bin/Lists.asmx";
    public static String TEST_XSO_USER = "test";
    public static String TEST_XSO_PASSWORD = "Xsolive2007";
    public static String TEST_XSO_DOMAIN = "demo";

    // Page on local webserver
    public static String TEST_WEBSERVER_PAGE = "/int/index.html";
    public static final String TEST_WEBEXT_EXTERNAL_HOST = EXTERNAL_HOST;
    public static final String TEST_WEBEXT_EXTERNAL_OPTIONS_HOST = EXTERNAL_OPTIONS_HOST;

    // This is Apache as proxy server
    public static String TEST_PROXY_HOST = APACHE_HOST;

    // This is Squid as it uses HTTP 1.0
    public static String TEST_10_PROXY_HOST = SQUID_HOST;
    public static String TEST_SOCKS_PROXY_HOST = SOCKS_HOST;

    // This is NetProxy
    public static String TEST_AUTH_PROXY_CLOSE_HOST = NETPROXY_HOST;
    public static String TEST_AUTH_PROXY_CLOSE_USER = "netproxy";
    public static String TEST_AUTH_PROXY_CLOSE_PASSWORD = "netproxy";

    // Try the Apache proxy from linux
    // public static int TEST_AUTH_PROXY_CLOSE_PORT = 80;

    // This is Apache as a proxy
    public static String TEST_AUTH_PROXY_HOST = AUTH_PROXY_HOST;
    public static String TEST_AUTH_PROXY_USER = "testProxy";
    public static String TEST_AUTH_PROXY_PASSWORD = "test839Proxy";

    // Standard Apache
    public static String TEST_WEBSERVER_HOST = APACHE_HOST;

    // WebDav server
    public static String TEST_WEBDAV_HOST = WEBDAV_HOST;
    public static String TEST_URL_WEBSERVER = HTTP_PROTOCOL + "//" +
        TEST_WEBSERVER_HOST + ":" + WEBSERVER_PORT;

    // Various webserver data files
    public static String TEST_URL_WEBSERVER_DATA = TEST_URL_WEBSERVER +
        "/oaklandsw-http/";
    public static String TEST_URL_HOST_IIS_DIGEST = HTTP_PROTOCOL + "//" +
        IIS_DIGEST_HOST + ":" + IIS_DIGEST_PORT + "/";

    // We expect these to be protected by the user names "basic" and "digest"
    // with the password the same as the username
    public static String TEST_URL_AUTH_BASIC = TEST_URL_WEBSERVER +
        "/httptest/basic/index.html";
    public static String TEST_URL_AUTH_DIGEST = TEST_URL_WEBSERVER +
        "/httptest/digest/index.html";
    public static String TEST_URL_AUTH_IIS_DIGEST = TEST_URL_HOST_IIS_DIGEST +
        "iisstart.htm";
    public static String TEST_WEBEXT_SSL_URL = HTTPS_PROTOCOL + "//" +
        TEST_WEBEXT_SSL_HOST + "/";
    public static String TEST_WEBEXT_SSL_URL_PORT = HTTPS_PROTOCOL + "//" +
        TEST_WEBEXT_SSL_HOST + ":443/";
    public static String TEST_LOCAL_SSL_URL = HTTPS_PROTOCOL + "//" +
        APACHE_HOST + ":" + SSL_PORT + "/";
    public static String TEST_ISA_URL = HTTP_PROTOCOL + "//" + ISA_HOST + ":" +
        IIS_PORT + "/";

    // Tomcat 4.1
    public static String TEST_WEBAPP_HOST = TOMCAT_HOST;
    public static String TEST_URL_HOST_IIS_0 = HTTP_PROTOCOL + "//" +
        TEST_IIS_HOST_0 + ":" + IIS_PORT + "/";
    public static String TEST_URL_HOST_IIS_5 = HTTP_PROTOCOL + "//" +
        TEST_IIS_HOST_5 + ":" + IIS_PORT + "/";
    public static String TEST_URL_IIS_0 = TEST_URL_HOST_IIS_0 +
        TEST_URL_APP_IIS;
    public static String TEST_URL_IIS_5 = TEST_URL_HOST_IIS_5 +
        TEST_URL_APP_IIS;
    public static String TEST_URL_HOST_WEBAPP = HTTP_PROTOCOL + "//" +
        TEST_WEBAPP_HOST + ":" + TOMCAT_PORT_1 + "/";

    /*
     * WEBAPP_2-4 are only used for JCIFS which is not used.
     */
    public static String TEST_URL_HOST_WEBAPP_2 = HTTP_PROTOCOL + "//" +
        TEST_WEBAPP_HOST + ":" + TOMCAT_PORT_2 + "/";
    public static String TEST_URL_HOST_WEBAPP_3 = HTTP_PROTOCOL + "//" +
        TEST_WEBAPP_HOST + ":" + TOMCAT_PORT_3 + "/";
    public static String TEST_URL_HOST_WEBAPP_4 = HTTP_PROTOCOL + "//" +
        TEST_WEBAPP_HOST + ":" + TOMCAT_PORT_4 + "/";
    public static String TEST_URL_WEBAPP = TEST_URL_HOST_WEBAPP +
        TEST_URL_APP_TOMCAT_1;
    public static String ERRORSVR_HOST_PORT = ERROR_HOST + ":" + ERRORSVR_PORT;
    public static String TEST_URL_HOST_ERRORSVR = HTTP_PROTOCOL + "//" +
        ERRORSVR_HOST_PORT + "/";

    // This is an unused port designed to fail
    public static String TEST_URL_HOST_TIMEOUT = HTTP_PROTOCOL + "//" +
        TOMCAT_HOST + ":8087/";

	public static final String ROOT = "OAKLANDSW_ROOT";

	public static String getRoot() {
		String root = System.getProperty(ROOT);
		if (root == null) {
			return SystemUtils.USER_HOME + File.separator + "workspace";
		}
		return root;
	}

    public static String getHttpTestRoot() {
        return getRoot() + File.separator +
        "com.oaklandsw.http.tests";
    }
}
