package com.oaklandsw.http.webstart;

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

import com.oaklandsw.http.HttpURLConnection;
import com.oaklandsw.http.HostnameVerifier;

// Test from customer for bug 2163

public final class HttpTest
{

    public HttpTest() throws Exception
    {
        // Register Oakland Software HttpClient
        System.out
                .println("AdminConsoleJnlpLauncher - Register HttpURLConnection");
        URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory()
        {
            public URLStreamHandler createURLStreamHandler(String protocol)
            {
                // XXX
                System.out.println("createURLStreamHandler for " + protocol);

                if (protocol.equalsIgnoreCase("http"))
                    return new com.oaklandsw.http.Handler();
                else if (protocol.equalsIgnoreCase("https"))
                    return new com.oaklandsw.https.Handler();
                return null;
            }

        });

        System.out
                .println("AdminConsoleJnlpLauncher - using allCertsTrustManager ");

        TrustManager[] allCertsTrustManager = new TrustManager[] { new X509TrustManager()
        {
            public X509Certificate[] getAcceptedIssuers()
            {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs,
                                           String authType)
            {
            }

            public void checkServerTrusted(X509Certificate[] certs,
                                           String authType)
            {
            }
        } };

        SSLContext sc = SSLContext.getInstance("SSL");
        sc.getClientSessionContext();
        sc.init(null, allCertsTrustManager, new SecureRandom());
        
        // Register the SocketFactory / Hostname Verifier for Oakland HttpClient
        HttpURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        HttpURLConnection.setDefaultHostnameVerifier(new HostnameVerifier()
        {
            public boolean verify(String string, SSLSession sSLSession)
            {
                return true;
            }
        });
    }

    private static String getValueFromArg(String string)
    {
        String value = null;

        StringTokenizer token = new StringTokenizer(string, "=");
        while (token.hasMoreElements())
        {
            value = (String)token.nextElement();
        }

        return value;
    }

    public static final void main(String[] args) throws Exception
    {
        try
        {
            new HttpTest();

            System.out
                    .println("HttpTest: The application has been correctly initialized");
            System.exit(0);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }

    }
}
