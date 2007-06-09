

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.oaklandsw.http.HttpUserAgent;

//
// A sample program to post to the specified URL:
// 
// java HttpPostSample <url>
//

public class HttpPostSample implements HttpUserAgent
{

    public HttpPostSample()
    {
    }

    // Implements the user agent for normal credentials
    public com.oaklandsw.http.Credential getCredential(String realm,
                                                    String url,
                                                    int scheme)
    {
        switch (scheme)
        {
        case com.oaklandsw.http.Credential.AUTH_NTLM:
            com.oaklandsw.http.NtlmCredential ntlmCred = 
                new com.oaklandsw.http.NtlmCredential();
        
            ntlmCred.setUser("user");
            ntlmCred.setPassword("password");
            ntlmCred.setDomain("workgroup");
            ntlmCred.setHost("host");
            return ntlmCred;

        case com.oaklandsw.http.Credential.AUTH_BASIC:
        case com.oaklandsw.http.Credential.AUTH_DIGEST:
            com.oaklandsw.http.UserCredential basicCred = 
                new com.oaklandsw.http.UserCredential();
            basicCred.setUser("user");
            basicCred.setPassword("password");
            return basicCred;
        }

        return null;
    }

    // Implements the user agent for credentials to the proxy server
    public com.oaklandsw.http.Credential getProxyCredential(String realm,
                                                         String url,
                                                         int scheme)
    {
        return null;
    }


    // Main sample program
    public static final void main(String[] args)
        throws Exception
    {
        HttpPostSample postObjSample = new HttpPostSample();


        // Tell Java to use the oaklandsw implementation
        System.setProperty("java.protocol.handler.pkgs", "com.oaklandsw");

        // Tells the oaklandsw implementation the object that will
        // resolve the credentials when requested by IIS/NTLM
        com.oaklandsw.http.HttpURLConnection.
            setDefaultUserAgent(postObjSample);

        // Note that all of the code below is standard Java code
        // for HTTP access

        URL url = new URL(args[0]);
        int response = 0;

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod("POST");
        urlCon.setDoOutput(true);

        // Here is where you write the data to post
        OutputStream outStr = urlCon.getOutputStream();
        outStr.write("this is the data to post".getBytes("ascii"));
        outStr.close();

        // This does the connect and gets the response code
        response = urlCon.getResponseCode();
        if (response != 200)
            throw new Exception("Invalid response: " + response);
            
        // Read any output from the post here
        InputStream inStr = urlCon.getInputStream();
        ByteArrayOutputStream resultStr = new ByteArrayOutputStream();
        byte[] buffer = new byte[10000];
        int nb = 0;
        while (true)
        {
            nb = inStr.read(buffer);
            if (nb == -1)
                break;
            resultStr.write(buffer, 0, nb);
        }
        System.out.println(new String(resultStr.toByteArray()));
        inStr.close();

    }

}

 
