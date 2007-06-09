

import org.apache.commons.logging.Log;

import com.oaklandsw.http.Credential;
import com.oaklandsw.http.HttpUserAgent;
import com.oaklandsw.http.NtlmCredential;
import com.oaklandsw.http.UserCredential;
import com.oaklandsw.util.LogUtils;

public class SampleUserAgent implements HttpUserAgent
{
    private static final Log     _log = LogUtils.makeLogger();

    public static NtlmCredential _normalCredential;
    public static NtlmCredential _proxyCredential;

    static
    {
        _normalCredential = new NtlmCredential();
        _proxyCredential = new NtlmCredential();
    }

    public Credential getCredential(String realm, String url, int scheme)
    {

        _log.debug("getGred: " + realm + " url: " + url + " scheme: " + scheme);

        Credential cred = _normalCredential;

        _log.debug("Returning cred: " + ((UserCredential)cred).getUser());

        return cred;
    }

    public Credential getProxyCredential(String realm, String url, int scheme)
    {
        _log.debug("getProxyGred: "
            + realm
            + " url: "
            + url
            + " scheme: "
            + scheme);

        Credential cred = _proxyCredential;

        _log.debug("Returning cred: " + ((UserCredential)cred).getUser());

        return cred;
    }
}
