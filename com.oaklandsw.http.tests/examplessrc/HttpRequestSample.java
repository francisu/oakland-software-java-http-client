import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import com.oaklandsw.http.Cookie;
import com.oaklandsw.http.CookieContainer;
import com.oaklandsw.http.HttpURLConnection;
import com.oaklandsw.http.NtlmCredential;
import com.oaklandsw.util.LogUtils;

/**
 * Sample HTTP request program for Java HTTP client
 */
public class HttpRequestSample
{

    public HttpRequestSample()
    {
    }

    static boolean               _interactive;
    static boolean               _nooutput;

    static int                   _loopCount;

    static String                _proxyHost;
    static int                   _proxyPort;

    static boolean               _useConnectionProxy;

    static boolean               _doLogging;

    static boolean               _doPost;

    public static NtlmCredential _normalCredential;
    public static NtlmCredential _proxyCredential;

    static
    {
        _normalCredential = new NtlmCredential();
        _normalCredential.setUser("not set");
        _normalCredential.setPassword("not set");
        _normalCredential.setHost("not set");
        _normalCredential.setDomain("not set");

        _proxyCredential = new NtlmCredential();
        _proxyCredential.setUser("not set");
        _proxyCredential.setPassword("not set");
        _proxyCredential.setHost("not set");
        _proxyCredential.setDomain("not set");
    }

    static void extractProxy(String hostAndPort)
    {
        int ind = hostAndPort.indexOf(":");
        if (ind > 0)
        {
            _proxyHost = hostAndPort.substring(0, ind);
            try
            {
                _proxyPort = Integer.parseInt(hostAndPort.substring(ind + 1));
            }
            catch (Exception ex)
            {
                throw new RuntimeException("Invalid port: " + hostAndPort);
            }
        }
        else
        {
            _proxyHost = hostAndPort;
            _proxyPort = 80;
        }
    }

    public static void usage()
    {
        System.out.println("java HttpRequestSample <url> [options]");
        System.out.println("options: ");
        System.out.println(" -pass <password> - the password to send");
        System.out.println(" -user <userName> - the user name to send");
        System.out
                .println(" -host <hostName> - the host name to send (NTLM only)");
        System.out
                .println(" -dom <domainName> - the domain name to send (NTLM only)");
        System.out
                .println(" -pxpass <password> - the password to send to proxy server");
        System.out
                .println(" -pxuser <userName> - the user name to send to proxy server");
        System.out
                .println(" -pxhost <hostName> - the host name to send to proxy server");
        System.out
                .println(" -pxdom <domainName> - the domain name to send to proxy server");
        System.out.println(" -loop <count> - the number of times to iterate");
        System.out.println(" -proxy <host:port> - the proxy host/port to use ");
        System.out.println(" -log - turn on logging");
        System.out.println(" -post - emit a POST request instead of a GET");
    }

    public static final void main(String[] args) throws Exception
    {
        _loopCount = 1;

        if (args.length == 0 || !args[0].toLowerCase().startsWith("http"))
        {
            usage();
            return;
        }

        int index = 1;
        while (index < args.length)
        {
            if (args[index].equalsIgnoreCase("-user"))
                _normalCredential.setUser(args[++index]);
            else if (args[index].equalsIgnoreCase("-pass"))
                _normalCredential.setPassword(args[++index]);
            else if (args[index].equalsIgnoreCase("-dom"))
                _normalCredential.setDomain(args[++index]);
            else if (args[index].equalsIgnoreCase("-host"))
                _normalCredential.setHost(args[++index]);

            else if (args[index].equalsIgnoreCase("-pxuser"))
                _proxyCredential.setUser(args[++index]);
            else if (args[index].equalsIgnoreCase("-pxpass"))
                _proxyCredential.setPassword(args[++index]);
            else if (args[index].equalsIgnoreCase("-pxdom"))
                _proxyCredential.setDomain(args[++index]);
            else if (args[index].equalsIgnoreCase("-pxhost"))
                _proxyCredential.setHost(args[++index]);
            else if (args[index].equalsIgnoreCase("-interactive"))
                _interactive = true;
            else if (args[index].equalsIgnoreCase("-nooutput"))
                _nooutput = true;
            else if (args[index].equalsIgnoreCase("-loop"))
                _loopCount = Integer.parseInt(args[++index]);
            else if (args[index].equalsIgnoreCase("-proxy"))
                extractProxy(args[++index]);
            else if (args[index].equalsIgnoreCase("-conproxy"))
                _useConnectionProxy = true;
            else if (args[index].equalsIgnoreCase("-log"))
                _doLogging = true;
            else if (args[index].equalsIgnoreCase("-post"))
                _doPost = true;
            else if (args[index].equalsIgnoreCase("-help")
                || args[index].equalsIgnoreCase("-h"))
            {
                usage();
            }
            else
            {
                System.out.println("Invalid argument: "
                    + args[index]
                    + " ignored");
            }
            index++;
        }

        if (_doLogging)
            LogUtils.logAll();

        // Tell Java to use the oaklandsw implementation
        System.setProperty("java.protocol.handler.pkgs", "com.oaklandsw");

        if (_proxyHost != null && !_useConnectionProxy)
        {
            System.setProperty("http.proxyPort", Integer.toString(_proxyPort));
            System.setProperty("http.proxyHost", _proxyHost);
            System.setProperty("https.proxyPort", Integer.toString(_proxyPort));
            System.setProperty("https.proxyHost", _proxyHost);
        }

        // Tells the oaklandsw implementation the object that will
        // resolve the credentials when requested by IIS/NTLM

        SampleUserAgent userAgent = new SampleUserAgent();
        userAgent._normalCredential = _normalCredential;
        userAgent._proxyCredential = _proxyCredential;

        com.oaklandsw.http.HttpURLConnection.setDefaultUserAgent(userAgent);

        // First arg is the URL
        String urlStr = args[0];

        CookieContainer cc = new CookieContainer();
        URL url = new URL(urlStr);
        while (true)
        {
            // Wait for a return to be typed
            if (_interactive)
            {
                System.out.print("Please press enter to connect: ");
                System.in.read();
                System.in.read();
            }

            try
            {
                HttpURLConnection urlCon;

                // This way should be used to force the Oakland Software
                // implementation
                urlCon = com.oaklandsw.http.HttpURLConnection
                        .openConnection(url);

                // This way can also be used, but if an HTTP request
                // happened before you set the java.protocol.handler.pkgs
                // property (see above), you will get the Sun implementation
                // urlCon = (HttpURLConnection)url.openConnection();

                if (_proxyHost != null && _useConnectionProxy)
                {
                    urlCon.setConnectionProxyHost(_proxyHost);
                    urlCon.setConnectionProxyPort(_proxyPort);
                }

                urlCon.setCookieSupport(cc, null);
                urlCon.connect();

                if (_doPost)
                {
                    urlCon.setRequestMethod("POST");
                    urlCon.setDoOutput(true);

                    // Here is where you write the data to post
                    OutputStream outStr = urlCon.getOutputStream();
                    outStr.write("this is the data to post".getBytes("ascii"));
                    outStr.close();
                }

                if (!_nooutput)
                {
                    System.out.println("Response: " + urlCon.getResponseCode());
                    for (int i = 0; i < cc.getCookies().length; i++)
                    {
                        Cookie cookie = cc.getCookies()[i];
                        System.out.println(cookie);
                    }
                }

                // Read the response
                InputStream inputStream = urlCon.getInputStream();
                byte[] buffer = new byte[10000];
                int nb = 0;
                while (true)
                {
                    nb = inputStream.read(buffer);
                    if (nb == -1)
                        break;
                    if (!_nooutput)
                        System.out.write(buffer, 0, nb);
                }

            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }

            if (!_interactive)
            {
                if (--_loopCount == 0)
                    break;
            }
        }

    }

}
