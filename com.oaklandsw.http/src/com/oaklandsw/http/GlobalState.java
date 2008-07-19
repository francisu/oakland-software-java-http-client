//
// Copyright 2007, oakland software, all rights reserved.
//
// May not be used or redistributed without specific written
// permission from oakland software.
//
package com.oaklandsw.http;

import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.regexp.RE;
import org.apache.regexp.RESyntaxException;

import com.oaklandsw.http.cookie.CookieSpec;
import com.oaklandsw.util.LogUtils;
import com.oaklandsw.util.StringUtils;

/**
 * Maintains the global persistent state for the HTTP client. This is the state
 * that is set by the user that only changes when the user wants it to: things
 * like the proxy host/port, non proxy hosts, etc.
 */
public class GlobalState
{
    private static final Log       _log                                  = LogUtils
                                                                                 .makeLogger();

    private static final Log       _connLog                              = LogFactory
                                                                                 .getLog(HttpConnection.CONN_LOG);
    // From RFC 2616 section 8.1.4
    public static int              DEFAULT_MAX_CONNECTIONS               = 2;

    // public for tests
    public int                     _maxConns                             = HttpURLConnection.DEFAULT_MAX_CONNECTIONS;

    // The current conn manager - public so tests can see this
    public HttpConnectionManager   _connManager;

    // public for tests
    public String                  _proxyHost;
    public int                     _proxyPort                            = -1;
    String                         _proxyUser;
    String                         _proxyPassword;
    boolean                        _proxySsl;

    String                         _nonProxyHostsString;

    private ArrayList              _nonProxyHosts;

    public static final boolean    DEFAULT_MULTI_CREDENTIALS_PER_ADDRESS = true;
    boolean                        _multiCredentialsPerAddress           = DEFAULT_MULTI_CREDENTIALS_PER_ADDRESS;

    int                            _defaultConnectionTimeout;
    int                            _defaultRequestTimeout;

    Callback                       _defaultCallback;

    public static final int        DEFAULT_PIPELINING_OPTIONS            = 0;
    int                            _defaultPipeliningOptions             = DEFAULT_PIPELINING_OPTIONS;

    public static final int        DEFAULT_PIPELINING_MAX_DEPTH          = 0;
    int                            _defaultPipeliningMaxDepth            = DEFAULT_PIPELINING_MAX_DEPTH;

    // For tests
    public static final int        DEFAULT_IDLE_TIMEOUT                  = 14000;
    public static final int        DEFAULT_IDLE_PING                     = 0;
    public static final int        DEFAULT_CONNECTION_REQUEST_LIMIT      = 0;

    int                            _defaultIdleTimeout                   = DEFAULT_IDLE_TIMEOUT;

    int                            _defaultIdlePing                      = DEFAULT_IDLE_PING;

    int                            _defaultConnectionRequestLimit        = DEFAULT_CONNECTION_REQUEST_LIMIT;

    protected static final boolean DEFAULT_USE_10_KEEPALIVE              = false;
    boolean                        _use10KeepAlive                       = DEFAULT_USE_10_KEEPALIVE;

    int                            _ntlmPreferredEncoding                = HttpURLConnection.NTLM_ENCODING_UNICODE;

    /**
     * The maximum number of attempts to attempt recovery from a recoverable
     * IOException.
     */
    public static int              MAX_TRIES                             = 3;
    int                            _defaultMaxTries                      = MAX_TRIES;

    public static int              MAX_FORWARDS                          = 100;
    int                            _defaultMaxForwards                   = MAX_FORWARDS;

    private static int             DEFAULT_RETRY_INTERVAL                = 0;
    int                            _retryInterval                        = DEFAULT_RETRY_INTERVAL;

    private static int             DEFAULT_AUTHENTICATION_TYPE           = 0;
    int                            _defaultAuthenticationType;
    int                            _defaultProxyAuthenticationType;

    // Don't access this directory because it is lazily set to avoid the high
    // cost of SSL startup when that is not needed
    private SSLSocketFactory       _defaultSSLSocketFactory;

    HostnameVerifier               _defaultHostnameVerifier;

    boolean                        _defaultVerifySsl;
    boolean                        _defaultForceSsl;

    CookieContainer                _defaultCookieContainer;

    CookieSpec                     _defaultCookieSpec;

    HttpUserAgent                  _defaultUserAgent;

    AbstractSocketFactory          _defaultSocketFactory;

    int                            _writeBufferSize                      = HttpURLConnection.DEFAULT_SEND_BUFFER_SIZE;
    int                            _readBufferSize                       = HttpURLConnection.DEFAULT_RECEIVE_BUFFER_SIZE;

    boolean                        _fileNotFoundOn404;

    private class DefaultHostnameVerifier implements HostnameVerifier
    {
        public boolean verify(String hostName, SSLSession session)
        {
            return false;
        }
    }

    public GlobalState()
    {
        // Might be null in cases were initialization is being reentered
        if (_log != null)
            _log.info("Oakland Software Java HTTP Client " + Version.VERSION);

        _defaultHostnameVerifier = new DefaultHostnameVerifier();
        // This one is created only if needed because it can take a long time
        _defaultSSLSocketFactory = null;
        _defaultSocketFactory = new AbstractSocketFactory();
        _defaultVerifySsl = true;
    }

    void setConnManager(HttpConnectionManager connManager)
    {
        _connManager = connManager;
    }

    // Public for tests
    public void setProxyHost(String proxyHost)
    {
        _proxyHost = proxyHost;
        // Get rid of all current connections as they are not
        // going to the right place.
        try
        {
            _connManager.resetConnectionPool(!HttpConnectionManager.IMMEDIATE);
        }
        catch (InterruptedException e)
        {
            // Not much we can do about this, as the calling
            // API cannot tolerate being interrupted
            _log.warn("setProxyHost - interrupted", e);
        }
    }

    // public for tests
    public void setProxyPort(int proxyPort)
    {
        _proxyPort = proxyPort;
        // Get rid of all current connections as they are not
        // going to the right place.
        try
        {
            _connManager.resetConnectionPool(!HttpConnectionManager.IMMEDIATE);
        }
        catch (InterruptedException e)
        {
            // Not much we can do about this, as the calling
            // API cannot tolerate being interrupted
            _log.warn("setProxyPort - interrupted", e);
            // Continue
        }
    }

    // public for tests
    public void setProxySsl(boolean isSsl)
    {
        _proxySsl = isSsl;
        // Get rid of all current connections as they are not
        // going to the right place.
        try
        {
            _connManager.resetConnectionPool(!HttpConnectionManager.IMMEDIATE);
        }
        catch (InterruptedException e)
        {
            // Not much we can do about this, as the calling
            // API cannot tolerate being interrupted
            _log.warn("setProxySsl - interrupted", e);
            // Continue
        }
    }

    /**
     * Set the proxy host to use for all connections.
     * 
     * @param proxyHost
     *            - the proxy host name
     */
    void setNonProxyHosts(String hosts)
    {
        synchronized (this)
        {
            _nonProxyHostsString = hosts;
            if (_nonProxyHostsString == null)
            {
                _nonProxyHosts = null;
                return;
            }

            _nonProxyHosts = new ArrayList();
            StringTokenizer stringtokenizer = new StringTokenizer(_nonProxyHostsString,
                                                                  "|",
                                                                  false);
            while (stringtokenizer.hasMoreTokens())
            {
                String host = stringtokenizer.nextToken().toLowerCase().trim();

                // The syntax for a non-proxy host only allows a "*" as
                // wildcard,
                // so we need to fix it up to be a correct RE.
                host = StringUtils.replace(host, ".", "\\.");
                host = StringUtils.replace(host, "*", ".*");

                RE re;
                try
                {
                    re = new RE(host);
                }
                catch (RESyntaxException rex)
                {
                    throw new RuntimeException("Invalid syntax for nonProxyHosts: '"
                        + hosts
                        + "' on host '"
                        + host
                        + "': "
                        + rex.getMessage());
                }

                re.setMatchFlags(RE.MATCH_CASEINDEPENDENT);
                if (_connLog.isDebugEnabled())
                    _connLog.debug("Non proxy host: " + host);
                _nonProxyHosts.add(re);
            }
        }
    }

    String getNonProxyHosts()
    {
        synchronized (this)
        {
            return _nonProxyHostsString;
        }
    }

    /**
     * Returns the proxy host for the specified host.
     */
    String getProxyHost(String host)
    {
        if (isNonProxyHost(host))
            return null;
        return _proxyHost;
    }

    boolean isProxySsl()
    {
        return _proxySsl;
    }

    boolean isNonProxyHost(String host)
    {
        // This should be OK, as we never lock the connection
        // info while we have the lock on this.
        synchronized (this)
        {
            // Look for hosts to not proxy for
            if (_nonProxyHosts != null)
            {
                int len = _nonProxyHosts.size();
                for (int i = 0; i < len; i++)
                {
                    RE re = (RE)_nonProxyHosts.get(i);
                    if (re.match(host))
                    {
                        if (_log.isDebugEnabled())
                        {
                            _log.debug("Not proxying host: "
                                + host
                                + " because of host rule: "
                                + re.toString());
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    SSLSocketFactory getDefaultSSLSocketFactory()
    {
        // Do this only when requested
        if (_defaultSSLSocketFactory != null)
            return _defaultSSLSocketFactory;
        _defaultSSLSocketFactory = ((SSLSocketFactory)SSLSocketFactory
                .getDefault());
        return _defaultSSLSocketFactory;
    }

    void setDefaultSSLSocketFactory(SSLSocketFactory factory)
    {
        _defaultSSLSocketFactory = factory;
    }

}
