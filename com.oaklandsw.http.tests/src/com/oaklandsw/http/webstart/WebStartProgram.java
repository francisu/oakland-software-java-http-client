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
package com.oaklandsw.http.webstart;

import com.oaklandsw.http.Cookie;
import com.oaklandsw.http.CookieContainer;
import com.oaklandsw.http.HttpURLConnection;

import java.io.InputStream;

import java.net.URL;


/*******************************************************************************
 *
 * Sample program for Oakland Software Java HTTP Client
 *
 * usage: java HttpGetSample <url> <options>
 *
 * url - the URL to get from options:
 *
 * -pass <password> - the password to send<br>
 * -user <userName> - the user name to send<br>
 * -host <hostName> - the host name to send (NTLM only)<br>
 * -dom <domainName> - the domain name to send (NTLM only)<br>
 * -pxpass <password> - the password to send to proxy server<br>
 * -pxuser <userName> - the user name to send to proxy server<br>
 * -pxhost <hostName> - the host name to send to proxy server<br>
 * -pxdom <domainName> - the domain name to send to proxy server<br>
 * -loop <count> - the number of times to iterate<br>
 * -proxy <host:port> - the proxy host/port to use
 *
 *
 ******************************************************************************/
public class WebStartProgram implements com.oaklandsw.http.HttpUserAgent {
    private static boolean _interactive;
    private static boolean _nooutput;
    private static int _loopCount;
    private static String _proxyHost;
    private static int _proxyPort;
    private static boolean _useConnectionProxy;
    private static boolean _skipSetProp;
    public static com.oaklandsw.http.NtlmCredential _normalCredential;
    public static com.oaklandsw.http.NtlmCredential _proxyCredential;

    static {
        _normalCredential = new com.oaklandsw.http.NtlmCredential();
        _normalCredential.setUser("not set");
        _normalCredential.setPassword("not set");
        _normalCredential.setHost("not set");
        _normalCredential.setDomain("not set");

        _proxyCredential = new com.oaklandsw.http.NtlmCredential();
        _proxyCredential.setUser("not set");
        _proxyCredential.setPassword("not set");
        _proxyCredential.setHost("not set");
        _proxyCredential.setDomain("not set");
    }

    public WebStartProgram() {
    }

    // HttpUserAgent Interface - get credentials for server
    public com.oaklandsw.http.Credential getCredential(String realm,
        String url, int scheme) {
        if (!_nooutput) {
            System.out.println("getGred: " + realm + " url: " + url +
                " scheme: " + scheme);
        }

        com.oaklandsw.http.NtlmCredential cred = _normalCredential;

        if (!_nooutput) {
            System.out.println("Returning normal cred: " + cred.getUser());
        }

        return cred;
    }

    // HttpUserAgent Interface - get credentials for proxy
    public com.oaklandsw.http.Credential getProxyCredential(String realm,
        String url, int scheme) {
        if (!_nooutput) {
            System.out.println("getProxyGred: " + realm + " url: " + url +
                " scheme: " + scheme);
        }

        com.oaklandsw.http.NtlmCredential cred = _proxyCredential;

        if (!_nooutput) {
            System.out.println("Returning proxy cred: " + cred.getUser());
        }

        return cred;
    }

    private static void extractProxy(String hostAndPort) {
        int ind = hostAndPort.indexOf(":");

        if (ind > 0) {
            _proxyHost = hostAndPort.substring(0, ind);

            try {
                _proxyPort = Integer.parseInt(hostAndPort.substring(ind + 1));
            } catch (Exception ex) {
                throw new RuntimeException("Invalid port: " + hostAndPort);
            }
        } else {
            _proxyHost = hostAndPort;
            _proxyPort = 80;
        }
    }

    public static final void main(String[] args) throws Exception {
        WebStartProgram userAgent = new WebStartProgram();

        _loopCount = 1;

        // Skip the first arg which is the URL
        int index = 1;

        while (index < args.length) {
            if (args[index].equalsIgnoreCase("-user")) {
                _normalCredential.setUser(args[++index]);
            } else if (args[index].equalsIgnoreCase("-pass")) {
                _normalCredential.setPassword(args[++index]);
            } else if (args[index].equalsIgnoreCase("-dom")) {
                _normalCredential.setDomain(args[++index]);
            } else if (args[index].equalsIgnoreCase("-host")) {
                _normalCredential.setHost(args[++index]);
            } else if (args[index].equalsIgnoreCase("-pxuser")) {
                _proxyCredential.setUser(args[++index]);
            } else if (args[index].equalsIgnoreCase("-pxpass")) {
                _proxyCredential.setPassword(args[++index]);
            } else if (args[index].equalsIgnoreCase("-pxdom")) {
                _proxyCredential.setDomain(args[++index]);
            } else if (args[index].equalsIgnoreCase("-pxhost")) {
                _proxyCredential.setHost(args[++index]);
            } else if (args[index].equalsIgnoreCase("-interactive")) {
                _interactive = true;
            } else if (args[index].equalsIgnoreCase("-nooutput")) {
                _nooutput = true;
            } else if (args[index].equalsIgnoreCase("-skipsetprop")) {
                _skipSetProp = true;
            } else if (args[index].equalsIgnoreCase("-loop")) {
                _loopCount = Integer.parseInt(args[++index]);
            } else if (args[index].equalsIgnoreCase("-proxy")) {
                extractProxy(args[++index]);
            } else if (args[index].equalsIgnoreCase("-conproxy")) {
                _useConnectionProxy = true;
            }

            index++;
        }

        // SSL - Uncomment this if you are < JDK 1.4
        // Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());

        // Skipping this is for testing unsigned JWS application
        if (!_skipSetProp) {
            // Tell Java to use the oaklandsw implementation
            System.setProperty("java.protocol.handler.pkgs", "com.oaklandsw");
        }

        if ((_proxyHost != null) && !_useConnectionProxy) {
            System.setProperty("http.proxyPort", Integer.toString(_proxyPort));
            System.setProperty("http.proxyHost", _proxyHost);
            System.setProperty("https.proxyPort", Integer.toString(_proxyPort));
            System.setProperty("https.proxyHost", _proxyHost);
        }

        // Tells the oaklandsw implementation the object that will
        // resolve the credentials when requested by IIS/NTLM
        com.oaklandsw.http.HttpURLConnection.setDefaultUserAgent(userAgent);

        String urlStr;

        if (args.length == 0) {
            urlStr = "http://www.oaklandsoftware.com";
        } else {
            urlStr = args[0];
        }

        CookieContainer cc = new CookieContainer();
        URL url = new URL(urlStr);

        while (true) {
            // Wait for a return to be typed
            if (_interactive) {
                System.out.print("Please press enter to connect: ");
                System.in.read();
                System.in.read();
            }

            try {
                HttpURLConnection urlCon;

                // This way should be used to force the Oakland Software
                // implementation
                //urlCon = com.oaklandsw.http.HttpURLConnection
                //        .openConnection(url);

                // This way can also be used, but if an HTTP request
                // happened before you set the java.protocol.handler.pkgs
                // property (see above), you will get the Sun implementation
                urlCon = (HttpURLConnection) url.openConnection();

                if ((_proxyHost != null) && _useConnectionProxy) {
                    urlCon.setConnectionProxyHost(_proxyHost);
                    urlCon.setConnectionProxyPort(_proxyPort);
                }

                urlCon.setRequestMethod("GET");
                urlCon.setCookieSupport(cc, null);
                urlCon.connect();

                if (!_nooutput) {
                    System.out.println("Response: " + urlCon.getResponseCode());

                    for (int i = 0; i < cc.getCookies().length; i++) {
                        Cookie cookie = cc.getCookies()[i];
                        System.out.println(cookie);
                    }
                }

                // Print the output stream
                InputStream inputStream = urlCon.getInputStream();
                byte[] buffer = new byte[10000];
                int nb = 0;

                while (true) {
                    nb = inputStream.read(buffer);

                    if (nb == -1) {
                        break;
                    }

                    if (!_nooutput) {
                        System.out.write(buffer, 0, nb);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            if (!_interactive) {
                if (--_loopCount == 0) {
                    break;
                }
            }
        }
    }
}
