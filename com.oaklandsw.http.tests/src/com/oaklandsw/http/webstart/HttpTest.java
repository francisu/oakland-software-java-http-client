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

import com.oaklandsw.http.HostnameVerifier;
import com.oaklandsw.http.HttpURLConnection;

import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import java.util.StringTokenizer;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


// Test from customer for bug 2163
public final class HttpTest {
    public HttpTest() throws Exception {
        // Register Oakland Software HttpClient
        System.out.println(
            "AdminConsoleJnlpLauncher - Register HttpURLConnection");
        URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory() {
                public URLStreamHandler createURLStreamHandler(String protocol) {
                    // XXX
                    System.out.println("createURLStreamHandler for " +
                        protocol);

                    if (protocol.equalsIgnoreCase("http")) {
                        return new com.oaklandsw.http.Handler();
                    } else if (protocol.equalsIgnoreCase("https")) {
                        return new com.oaklandsw.https.Handler();
                    }

                    return null;
                }
            });

        System.out.println(
            "AdminConsoleJnlpLauncher - using allCertsTrustManager ");

        TrustManager[] allCertsTrustManager = new TrustManager[] {
                new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        public void checkClientTrusted(
                            X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(
                            X509Certificate[] certs, String authType) {
                        }
                    }
            };

        SSLContext sc = SSLContext.getInstance("SSL");
        sc.getClientSessionContext();
        sc.init(null, allCertsTrustManager, new SecureRandom());

        // Register the SocketFactory / Hostname Verifier for Oakland HttpClient
        HttpURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        HttpURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                public boolean verify(String string, SSLSession sSLSession) {
                    return true;
                }
            });
    }

    private static String getValueFromArg(String string) {
        String value = null;

        StringTokenizer token = new StringTokenizer(string, "=");

        while (token.hasMoreElements()) {
            value = (String) token.nextElement();
        }

        return value;
    }

    public static final void main(String[] args) throws Exception {
        try {
            new HttpTest();

            System.out.println(
                "HttpTest: The application has been correctly initialized");
            System.exit(0);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
