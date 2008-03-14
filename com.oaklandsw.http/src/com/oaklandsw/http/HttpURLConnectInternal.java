//
// Copyright 2002-2007, oakland software, all rights reserved.
//
// May not be used or redistributed without specific written
// permission from oakland software.
//

package com.oaklandsw.http;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.oaklandsw.http.cookie.MalformedCookieException;
import com.oaklandsw.util.ExposedBufferInputStream;
import com.oaklandsw.util.LogUtils;
import com.oaklandsw.util.StringUtils;
import com.oaklandsw.util.URIUtil;
import com.oaklandsw.util.Util;

/**
 * This class provides the internal implementation of the URL connection.
 */
public class HttpURLConnectInternal
    extends
        com.oaklandsw.http.HttpURLConnection
{
    private static final Log _log                 = LogUtils.makeLogger();

    private static final Log _connLog             = LogFactory
                                                          .getLog(HttpConnection.CONN_LOG);

    private String           _scheme;

    private String           _pathQuery;

    /**
     * Whether or not the request body has been sent. Used to initiate sending
     * the body on receipt of a continue if it has not already been sent.
     */
    private boolean          _bodySent;

    /**
     * The input stream from which to read the request body. This is set before
     * the request is issued, if the source of the data is a stream.
     */
    private InputStream      _requestBody;

    /**
     * The byte array of the request. If the request source is a byte array,
     * this is set. When the request is first written, the contents of the
     * request is saved here in case it needs to be resent for authentication.
     */
    private byte[]           _requestBytes;

    // We have read the first character of the next line after
    // the status line, and it is here. This only applies if
    // _singleEolChar is true
    protected int            _savedAfterStatusNextChar;

    protected boolean        _singleEolChar;

    // Either AUTH_NORMAL or AUTH_PROXY, this is set when we get a 401 or a
    // 407, -1 indicates we have not started authentication so we don't know
    protected int            _currentAuthType     = -1;

    // We requested that this connection be closed by sending a Connection:
    // close header. In case that request is not honored, we will close the
    // connection anyway.
    protected boolean        _connectionCloseSent;

    // The credentials that were sent in the authentication. We have
    // to have them both here and in the HTTPConnection. They are here
    // because NTLM authentication closes the connection during
    // authentication and we thus have no where to get the credential
    // information that was sent.
    UserCredential[]         _credential          = new UserCredential[AUTH_PROXY + 1];

    // The authentication protocol associated with the above credential
    // This is how this urlcon is authenticated
    int[]                    _authProtocol        = new int[AUTH_PROXY + 1];

    // The values for _authState

    // No authentication is required for this urlcon
    static final int         AS_NONE              = 0;

    // We know authentication is required and have not yet done anything
    static final int         AS_NEEDS_AUTH        = 1;

    // We have sent the initial authentication reponse to the server
    static final int         AS_INITIAL_AUTH_SENT = 2;

    // We have sent the final authentication response to the server
    // (perhaps preemtively in the case of basic)
    static final int         AS_FINAL_AUTH_SENT   = 3;

    // We have received an OK status after authentication, we are authenticated
    static final int         AS_AUTHENTICATED     = 4;

    int[]                    _authState           = new int[AUTH_PROXY + 1];

    // True if we are reading from the connection on the pipeline
    // thread, this is used only if authentication is required, then we
    // want to process the authentication in place and then exit as if
    // we had done a write
    protected boolean        _pipelinedReading;

    // True if the last thing we sent was a dummy request for NTLM
    // authentication. Used to initiate a retry in the event of a good
    // response to that request (since we did not send the user's request
    protected boolean        _dummyAuthRequestSent;

    // Thread on which the connection was created (used for pipelining)
    protected Thread         _thread;

    // Used for testing
    protected static int     _diagSequence;
    public static boolean    _addDiagSequence;

    // Used for testing, see TestPipelineRough
    public static boolean    _ignoreObservedMaxCount;

    public HttpURLConnectInternal(URL urlParam)
    {
        super(urlParam);
        _thread = Thread.currentThread();

        if (_log.isDebugEnabled())
        {
            _log.debug(normalOrProxyToString(AUTH_NORMAL)
                + " Type: "
                + authTypeToString(_authenticationType[AUTH_NORMAL]));
            _log.debug(normalOrProxyToString(AUTH_PROXY)
                + " Type: "
                + authTypeToString(_authenticationType[AUTH_PROXY]));
        }
    }

    // For testing and tunneling
    public HttpURLConnectInternal()
    {
    }

    // Used for tunneling to make sure we have the same authentication
    // setup as the user connection
    void copyAuthParamsFrom(HttpURLConnection urlCon)
    {
        // Use the values in the connection if they exist, otherwise
        // whatever default values setup are used
        if (urlCon.getConnectionProxyHost() != null)
            setConnectionProxyHost(urlCon.getConnectionProxyHost());
        if (urlCon.getConnectionProxyPort() != -1)
            setConnectionProxyPort(urlCon.getConnectionProxyPort());
        if (urlCon.getConnectionProxyUser() != null)
            setConnectionProxyUser(urlCon.getConnectionProxyUser());
        if (urlCon.getConnectionProxyPassword() != null)
            setConnectionProxyPassword(urlCon.getConnectionProxyPassword());
        if (urlCon.getUserAgent() != null)
            setUserAgent(urlCon.getUserAgent());

        setHostnameVerifier(urlCon.getHostnameVerifier());
        setConnectionTimeout(urlCon.getConnectionTimeout());
        setSSLSocketFactory(urlCon.getSSLSocketFactory());
    }

    static String authStateToString(int authState)
    {
        switch (authState)
        {
            case AS_NONE:
                return "AS_NONE";
            case AS_NEEDS_AUTH:
                return "AS_NEEDS_AUTH";
            case AS_INITIAL_AUTH_SENT:
                return "AS_INITIAL_AUTH_SENT";
            case AS_FINAL_AUTH_SENT:
                return "AS_FINAL_AUTH_SENT";
            case AS_AUTHENTICATED:
                return "AS_AUTHENTICATED";
        }
        Util.impossible("Invalid authState: " + authState);
        return null;
    }

    static String authTypeToString(int authType)
    {
        switch (authType)
        {
            case -1:
                return "UNKNOWN (-1)";
            case 0:
                return "NONE (0)";
            case Credential.AUTH_BASIC:
                return "AUTH_BASIC";
            case Credential.AUTH_DIGEST:
                return "AUTH_DIGEST";
            case Credential.AUTH_NTLM:
                return "AUTH_NTLM";
        }
        Util.impossible("Invalid authType: " + authType);
        return null;
    }

    /**
     * Returns true if we are sending auth requests for NTLM and thus don't want
     * to send real data (to avoid issues with streaming or having to resend
     * large amounts of data).
     */
    protected boolean useDummyAuthContent()
    {
        return _currentAuthType >= 0
            && _authenticationType[_currentAuthType] == Credential.AUTH_NTLM
            && _authState[_currentAuthType] >= AS_NEEDS_AUTH
            && _authState[_currentAuthType] < AS_FINAL_AUTH_SENT;
    }

    Credential getCredentialSent(int normalOrProxy)
    {
        return _credential[normalOrProxy];
    }

    void setAuthState(int authState, int normalOrProxy)
    {
        if (_log.isDebugEnabled())
        {
            _log.debug("setAuthState: "
                + normalOrProxyToString(normalOrProxy)
                + " Type: "
                + authTypeToString(_authProtocol[normalOrProxy])
                + ": "
                + authStateToString(authState)
                + " (was "
                + authStateToString(_authState[normalOrProxy])
                + ")");
        }

        _authState[normalOrProxy] = authState;

    }

    // The lastAuthMessage parameter indicates the last authentication message
    // was sent on this connection, and thus the special authentication
    // processing is over
    void setCredentialSent(int authType,
                           int normalOrProxy,
                           UserCredential cred,
                           int newAuthState)
    {
        if (_log.isDebugEnabled())
        {
            _log.debug("setCredentialSent: " + cred);
        }

        setAuthState(newAuthState, normalOrProxy);

        _credential[normalOrProxy] = cred;
        _authProtocol[normalOrProxy] = authType;

        // authType < 0 means we don't want to set it on the connection because
        // we don't know the authType
        if (_connection != null && authType > 0)
        {
            _connection.setCredentialSent(authType, normalOrProxy, cred);
        }
    }

    public final void setRequestBody(byte[] bodydata)
    {
        _requestBytes = bodydata;
    }

    public final void setRequestBody(String bodydata)
    {
        setRequestBody(bodydata.getBytes());
    }

    public final void setRequestBody(InputStream body)
    {
        _requestBody = body;
    }

    public final void setPathQuery(String pathQuery)
    {
        if (_log.isDebugEnabled())
            _log.debug("setPathQuery");

        if (pathQuery == null || pathQuery.equals(""))
            _pathQuery = "/";
        else
            _pathQuery = pathQuery;
    }

    public final String getPathQuery()
    {
        return _pathQuery;
    }

    public final String getPath()
    {
        int ind = _pathQuery.indexOf("?");
        if (ind == -1)
            return _pathQuery;
        return _pathQuery.substring(0, ind);
    }

    protected final void setUrl(String urlParam) throws MalformedURLException
    {
        _scheme = URIUtil.getProtocol(urlParam);
        _pathQuery = URIUtil.getPathQuery(urlParam);
        if (_log.isDebugEnabled())
        {
            _log.debug("URL: " + urlParam + " pathQuery: " + _pathQuery);
        }
    }

    public final void addResponseFooter(String key, String value)
    {
        if (_respFooters == null)
            _respFooters = new Headers();
        _respFooters.add(key.getBytes(), value.getBytes());
    }

    protected final void addRequestHeaders() throws IOException
    {
        // For testing, works with ParamServlet
        if (_addDiagSequence)
        {
            synchronized (getClass())
            {
                _reqHeaders.remove(HDR_SEQUENCE_BYTES);
                _reqHeaders.add(HDR_SEQUENCE_BYTES, Util
                        .toBytesAsciiInt(_diagSequence));
            }
        }

        // Use add() instead of set() in this method cuz its faster
        // if we know the header is not present

        if (!_reqHeaders.find(HDR_USER_AGENT_BYTES))
            _reqHeaders.add(HDR_USER_AGENT_BYTES, USER_AGENT);

        // Per 19.6.1.1 of RFC 2616, it is legal for HTTP/1.0 based
        // applications to send the Host request-header.

        if (!_reqHeaders.find(HDR_HOST_BYTES))
        {
            _log.debug("Adding Host request header");

            String h = _connection._hostPortURL;
            _reqHeaders.add(HDR_HOST_BYTES, h.getBytes());

        }

        // Last request on the connection that has a limit, close it
        if (_connectionRequestLimit != 0
            && _connection._totalReqUrlConCount == _connectionRequestLimit)
        {
            if (_connection.isProxied())
            {
                _reqHeaders.add(HDR_PROXY_CONNECTION_BYTES,
                                HDR_VALUE_CLOSE_BYTES);
            }
            else
            {
                _reqHeaders.add(HDR_CONNECTION_BYTES, HDR_VALUE_CLOSE_BYTES);
            }
            _connectionCloseSent = true;
        }

        if (_cookieContainer != null && !_cookieContainer.isEmpty())
        {
            Cookie[] cookies = _cookieSpec.match(_connection._host,
                                                 _connection.getPort(),
                                                 getPath(),
                                                 _connection.isSecure(),
                                                 _cookieContainer.getCookies());
            if ((cookies != null) && (cookies.length > 0))
            {
                // FIXME - for now just assume all cookies go with a single
                // header. May want to parameterize this in the future
                if (true)
                {
                    // In strict mode put all cookies on the same header
                    String s = _cookieSpec.formatCookies(cookies);
                    _reqHeaders.add(HDR_COOKIE_BYTES, s.getBytes());
                }
                else
                {
                    // In non-strict mode put each cookie on a separate header
                    for (int i = 0; i < cookies.length; i++)
                    {
                        String s = _cookieSpec.formatCookie(cookies[i]);
                        _reqHeaders.add(HDR_COOKIE_BYTES, s.getBytes());
                    }
                }
            }
        }

        // This would happen if we failed to parse the content-length
        // header when it was set. We could not throw then though
        if (_contentLength == BAD_CONTENT_LENGTH)
        {
            throw new HttpException("Invalid Content-Length "
                + "request header value: "
                + _reqHeaders.getAsString(HDR_CONTENT_LENGTH_BYTES));
        }

        if (useDummyAuthContent())
        {
            // CL could have been sent out on an initial request and
            // then we get back a 401, so need to remove it
            _reqHeaders.remove(HDR_CONTENT_LENGTH_BYTES);
            _hasContentLengthHeader = false;

            // Add the content length only if there is content
            if (!StringUtils.equals(_authenticationDummyMethod,
                                    HTTP_METHOD_HEAD))
            {
                if (_authenticationDummyContent != null)
                {
                    _reqHeaders.add(HDR_CONTENT_LENGTH_BYTES, Util
                            .toBytesAsciiInt(_authenticationDummyContent
                                    .length()));
                    _dummyAuthRequestSent = true;
                }
            }
        }
        else
        {
            // Remove any previously added CL header
            if (_authenticationDummyContent != null && !_hasContentLengthHeader)
            {
                _reqHeaders.remove(HDR_CONTENT_LENGTH_BYTES);
            }

            if (_streamingMode == STREAM_CHUNKED)
            {
                if (!_reqHeaders.find(HDR_TRANSFER_ENCODING_BYTES))
                    _reqHeaders.add(HDR_TRANSFER_ENCODING_BYTES,
                                    HDR_VALUE_CHUNKED_BYTES);
            }
            else
            {
                if (!_hasContentLengthHeader)
                {
                    if ((_actualMethodPropsSent & METHOD_PROP_ADD_CL_HEADER) != 0)
                    {
                        // add content length
                        int len = 0;
                        if (null != _requestBytes)
                        {
                            len = _requestBytes.length;
                        }
                        else if (_contentLength >= 0)
                        {
                            len = _contentLength;
                        }

                        _reqHeaders.add(HDR_CONTENT_LENGTH_BYTES, Util
                                .toBytesAsciiInt(len));
                        _hasContentLengthHeader = true;
                    }
                }

            }
        }

        // TODO - in JRE 1.5 they removed this default, look this up
        // to see what to do about this, removing this breaks the IIS
        // tests for example
        if ((_actualMethodPropsSent & METHOD_PROP_SEND_CONTENT_TYPE) != 0)
        {
            if (!_reqHeaders.find(HDR_CONTENT_TYPE_BYTES))
            {
                _reqHeaders.set(HDR_CONTENT_TYPE_BYTES,
                                HDR_VALUE_DEFAULT_CONTENT_TYPE_BYTES);
            }
        }

        // Add headers for HTTP 1.0 support
        if (_globalState._use10KeepAlive && !_connectionCloseSent)
        {
            if (_connection.isProxied())
            {
                if (!_reqHeaders.find(HDR_PROXY_CONNECTION_BYTES))
                {
                    _log
                            .debug("Adding 1.0 Proxy_Connection: Keep-Alive request header");
                    _reqHeaders.add(HDR_PROXY_CONNECTION_BYTES,
                                    HDR_VALUE_KEEP_ALIVE_BYTES);
                }
            }
            else
            {
                if (!_reqHeaders.find(HDR_CONNECTION_BYTES))
                {
                    _log
                            .debug("Adding 1.0 Connection: Keep-Alive request header");
                    _reqHeaders.add(HDR_CONNECTION_BYTES,
                                    HDR_VALUE_KEEP_ALIVE_BYTES);
                }
            }
        }

    }

    protected final void resetBeforeRequest()
    {
        _hdrContentEncoding = null;
        _hdrContentLength = null;
        _hdrContentType = null;
        _hdrContentLengthInt = 0;
        _hdrConnection = null;
        _hdrProxyConnection = null;
        _hdrTransferEncoding = null;
        _hdrWWWAuth = null;
        _hdrProxyAuth = null;
        _hdrLocation = null;
        _responseCode = 0;
        _responseIsEmpty = false;
        _responseHeadersRead = false;
        _responseStream = null;
        _savedAfterStatusNextChar = 0;
        _singleEolChar = false;
        _shouldClose = false;
        _connectionCloseSent = false;
    }

    protected final void readResponse() throws IOException
    {
        _connection.startReadBugCheck();
        readStatusLine();
        readResponseHeaders();
        isResponseEmpty();

        _connection.setLastTimeUsed();

        _shouldClose = shouldCloseConnection();

        if (!_responseIsEmpty)
        {
            // This creates the InputStream for the user to read the response;
            // the connection is released when the InputStream finishes reading.
            // For a normal response, we don't release the connection, it is
            // done when the stream is read. We assume the user will always
            // read and close the InputStream on a normal response.
            setupResponseBody();

            // For a bad response, the user might not read the InputStream.
            // We thus buffer it and release the connection immediately
            // when it is executed. This way the user can read it or not.
            if (_responseCode >= HttpStatus.SC_400)
            {
                ByteArrayOutputStream baStream = new ByteArrayOutputStream();
                try
                {
                    int count = Util.copyStreams(_responseStream, baStream);
                    if (_connLog.isDebugEnabled())
                    {
                        _connLog.debug("Copied "
                            + count
                            + " bytes from connection "
                            + "because of >= 400 response");
                    }
                }
                catch (IOException e)
                {
                    // We tried
                    _connLog.debug("Error while copying stream from "
                        + "connection that's closing", e);
                }

                // The user can read from this
                _responseStream = new ByteArrayInputStream(baStream
                        .toByteArray());
            }
        }

        _connection.endReadBugCheck();
    }

    // Used only on the reading of the first bytes of the connection
    // This is the typical case where an idle connection was encountered.
    private final void readClosed() throws SocketException
    {
        // This can be retried
        throw new SocketException("Connection closed after request was "
            + "sent and before reply received (try #"
            + _tryCount
            + ").  This request was probably sent to a connection that the "
            + "server closed after being idle.  "
            + "Consider using the set[Default]IdleConnectionTimeout "
            + "to avoid using connections that the server closed.");
    }

    protected static final int    SL_NOTHING           = 0;
    protected static final int    SL_HTTP              = 1;
    protected static final int    SL_SKIP_WHITE_BEFORE = 2;
    protected static final int    SL_RESP_CODE         = 3;
    protected static final int    SL_SKIP_WHITE_AFTER  = 4;
    protected static final int    SL_RESP_MSG          = 5;
    protected static final int    SL_AFTER_RESP_MSG    = 6;
    protected static final int    SL_FINISHED          = 7;

    protected static final byte[] HTTP_HDR             = { 'H', 'T', 'T', 'P',
        '/', '1', '.'                                 };

    protected final void readStatusLine() throws IOException
    {
        _responseCode = 0;
        _responseTextLength = 0;

        int state = SL_NOTHING;
        int httpInd = 0;

        ExposedBufferInputStream is = _conInStream;
        byte[] buffer = _conInStream._buffer;

        int responseMult = 100;

        try
        {
            char ch;
            while (state != SL_FINISHED)
            {
                // See if we need to fill
                if (is._pos >= is._used)
                {
                    is.fill();
                    if (is._used == -1)
                    {
                        readClosed();
                        // Throws
                    }
                }

                ch = (char)buffer[is._pos++];

                switch (state)
                {
                    case SL_NOTHING:
                        if (ch == HTTP_HDR[0])
                        {
                            httpInd = 0;
                            state = SL_HTTP;
                            // Fall through
                        }
                        else
                        {
                            // Look further for the beginning
                            break;
                        }
                    case SL_HTTP:
                        if (httpInd == HTTP_HDR.length)
                        {
                            if (ch == '1')
                                _http11 = true;
                            else if (ch == '0')
                                _http11 = false;
                            else
                            {
                                // This is not recoverable
                                throw new HttpException("Unrecognized server protocol version");
                            }
                            state = SL_SKIP_WHITE_BEFORE;
                        }
                        else
                        {
                            if (ch != HTTP_HDR[httpInd++])
                                state = SL_NOTHING;
                        }
                        break;

                    case SL_SKIP_WHITE_BEFORE:
                        if (ch == ' ' || ch == '\t')
                            break;
                        state = SL_RESP_CODE;
                        // Fall through

                    case SL_RESP_CODE:
                        // Check there is something in the response code so
                        // we don't bail the first time though this
                        if (_responseCode > 0 && (ch < '0' || ch > '9'))
                        {
                            state = SL_SKIP_WHITE_AFTER;
                            break;
                        }

                        _responseCode += (ch - '0') * responseMult;
                        responseMult /= 10;
                        break;

                    case SL_SKIP_WHITE_AFTER:
                        if (ch == '\r' || ch == '\n')
                        {
                            state = SL_AFTER_RESP_MSG;
                            break;
                        }

                        if (ch == ' ' || ch == '\t')
                            break;
                        state = SL_RESP_MSG;
                        // Fall through

                    case SL_RESP_MSG:
                        if (ch == '\r' || ch == '\n')
                        {
                            state = SL_AFTER_RESP_MSG;
                            break;
                        }

                        if (_responseTextLength < MAX_RESPONSE_TEXT)
                        {
                            _responseTextBytes[_responseTextLength++] = (byte)ch;
                        }
                        break;

                    case SL_AFTER_RESP_MSG:
                        /*
                         * At this point we have read either /r or /n, try to
                         * read the next EOL character which should be either /r
                         * or /n. In the case where only a single EOL character
                         * is used, this is where we detect this (by the fact
                         * that the next character is not /r or /n); we save
                         * that character because that must be the result of the
                         * next read.
                         */

                        // This means there is a single EOL char
                        if (ch != '\n' && ch != '\r')
                        {
                            _savedAfterStatusNextChar = ch;
                            _singleEolChar = true;
                        }
                        state = SL_FINISHED;
                        break;
                }
            }
        }
        catch (SocketException sex)
        {
            readClosed();
            // throws
        }

        if (_responseCode < 100 || _responseCode > 999)
        {
            throw new HttpException("Invalid response code: " + _responseCode);
        }

    }

    // This is called by the Headers class when parsing each header
    // for every header value it sees
    final void getHeadersWeNeed(byte[] name,
                                int nameLen,
                                byte[] value,
                                int valueLen)
    {
        char nameChar = (char)name[0];

        // Remember to use the _LC constants
        switch (nameChar)
        {
            case 'c':
                if (Util.bytesEqual(HDR_CONTENT_LENGTH_LC, name, nameLen))
                {
                    _hdrContentLength = value;
                    _hdrContentLengthLen = valueLen;
                }
                else if (Util.bytesEqual(HDR_CONTENT_TYPE_LC, name, nameLen))
                {
                    _hdrContentType = value;
                    _hdrContentTypeLen = valueLen;
                }
                else if (Util
                        .bytesEqual(HDR_CONTENT_ENCODING_LC, name, nameLen))
                {
                    _hdrContentEncoding = value;
                    _hdrContentEncodingLen = valueLen;
                }
                else if (Util.bytesEqual(HDR_CONNECTION_LC, name, nameLen))
                {
                    _hdrConnection = value;
                    _hdrConnectionLen = valueLen;
                }
                break;

            case 'l':
                if (Util.bytesEqual(HDR_LOCATION_LC, name, nameLen))
                {
                    _hdrLocation = value;
                    _hdrLocationLen = valueLen;
                }
                break;

            case 'p':
                if (Util.bytesEqual(Authenticator.REQ_HEADERS_LC[AUTH_PROXY],
                                    name,
                                    nameLen))
                {
                    _hdrProxyAuth = value;
                    _hdrProxyAuthLen = valueLen;
                }
                else if (Util
                        .bytesEqual(HDR_PROXY_CONNECTION_LC, name, nameLen))
                {
                    _hdrProxyConnection = value;
                    _hdrProxyConnectionLen = valueLen;
                }
                break;

            case 's':
                if (_cookieContainer != null)
                {
                    // Take either type of cookie header
                    if (!Util.bytesEqual(HDR_SET_COOKIE_LC, name, nameLen)
                        && !Util.bytesEqual(HDR_SET_COOKIE2_LC, name, nameLen))
                        break;

                    recordCookie(value, valueLen);
                }
                break;
            case 't':
                if (Util.bytesEqual(HDR_TRANSFER_ENCODING_LC, name, nameLen))
                {
                    _hdrTransferEncoding = value;
                    _hdrTransferEncodingLen = valueLen;
                }
                break;

            case 'w':
                if (Util.bytesEqual(Authenticator.REQ_HEADERS_LC[AUTH_NORMAL],
                                    name,
                                    nameLen))
                {
                    _hdrWWWAuth = value;
                    _hdrWWWAuthLen = valueLen;
                }
                break;
        }

    }

    protected void recordCookie(byte[] cookieValue, int cookieValueLen)
    {
        String host = _connection._host;
        int port = _connection.getPort();
        boolean isSecure = _connection.isSecure();
        Cookie[] cookies = null;
        try
        {
            cookies = _cookieSpec.parse(host, port, getPath(), isSecure, Util
                    .bytesToString(cookieValue, cookieValueLen));
        }
        catch (MalformedCookieException e)
        {
            if (_log.isWarnEnabled())
            {
                _log.warn("Invalid cookie header: \""
                    + Util.bytesToString(cookieValue, cookieValueLen)
                    + "\". "
                    + e.getMessage());
            }
        }

        if (cookies != null)
        {
            for (int j = 0; j < cookies.length; j++)
            {
                Cookie cookie = cookies[j];
                try
                {
                    _cookieSpec.validate(host,
                                         port,
                                         getPath(),
                                         isSecure,
                                         cookie);
                    _cookieContainer.addCookie(cookie);
                    if (_log.isDebugEnabled())
                    {
                        _log.debug("Cookie accepted: \""
                            + _cookieSpec.formatCookie(cookie)
                            + "\"");
                    }
                }
                catch (MalformedCookieException e)
                {
                    if (_log.isWarnEnabled())
                    {
                        _log.warn("Cookie rejected: \""
                            + _cookieSpec.formatCookie(cookie)
                            + "\". "
                            + e.getMessage());
                    }

                }
            }
        }
    }

    protected final void readResponseHeaders() throws IOException
    {
        _respHeaders.read(_conInStream,
                          this,
                          _singleEolChar,
                          _savedAfterStatusNextChar);
        _responseHeadersRead = true;
    }

    protected final void isResponseEmpty() throws IOException
    {
        // We will use the response at some point, for tunneling
        if ((_actualMethodPropsSent & METHOD_PROP_SUPPORTS_TUNNELED) != 0)
        {
            _responseIsEmpty = false;
            return;
        }

        // Does not have a response body, even though the headers suggest
        // that it does
        if ((_actualMethodPropsSent & METHOD_PROP_IGNORE_RESPONSE_BODY) != 0)
        {
            _responseIsEmpty = true;
            return;
        }

        if (null != _hdrContentLength)
        {
            try
            {
                _hdrContentLengthInt = Util
                        .fromBytesAsciiInt(_hdrContentLength,
                                           _hdrContentLengthLen);
                // Don't bother allocating a stream if there is no data
                if (_hdrContentLengthInt == 0)
                {
                    _responseIsEmpty = true;
                }
                else if (_responseCode == HttpStatus.SC_NO_CONTENT
                    || _responseCode == HttpStatus.SC_NOT_MODIFIED)
                {
                    _connLog.warn("Response code 204/304 sent and non-zero "
                        + "Content-Length was specified - ignoring");
                    _responseIsEmpty = true;
                }
            }
            catch (NumberFormatException e)
            {
                _connLog.warn("Invalid Content-Length response value read: "
                    + _hdrContentLength);
                throw new HttpException("Invalid Content-Length "
                    + "response header value: "
                    + _hdrContentLength);
            }
        }
        else if ((_responseCode >= HttpStatus.SC_CONTINUE && _responseCode <= 199)
            || _responseCode == HttpStatus.SC_NO_CONTENT
            || _responseCode == HttpStatus.SC_NOT_MODIFIED)
        {
            // There is not supposed to be any data
            _responseIsEmpty = true;
        }

    }

    // This is called only when we need the actual stream
    // to read the response
    protected final void setupResponseBody() throws IOException
    {
        // In case there have been retries
        _responseStream = null;

        InputStream result = null;

        // We use Transfer-Encoding if present and ignore Content-Length.
        // RFC2616, 4.4 item number 3
        if (null != _hdrTransferEncoding)
        {
            if (Util.bytesEqual(HDR_VALUE_CHUNKED_BYTES,
                                _hdrTransferEncoding,
                                _hdrTransferEncodingLen))
            {
                result = new ChunkedInputStream(_conInStream,
                                                this,
                                                (_pipeliningOptions & PIPE_PIPELINE) != 0);
            }
        }
        else if (_hdrContentLength != null)
        {
            result = new ContentLengthInputStream(_conInStream,
                                                  this,
                                                  _hdrContentLengthInt,
                                                  (_pipeliningOptions & PIPE_PIPELINE) != 0);
        }
        else
        {
            // We expect some data, but don't have a content length,
            // this is valid for HTTP 1.0, or in cases where the
            // connection is to be closed. This type of stream will
            // release and close the underlying connection when close is
            // called on it.
            _responseStream = new ReleaseInputStream(_conInStream,
                                                     this,
                                                     _shouldClose);
            // Don't wrap this in the auto close stream since it will
            // already handle that
            return;
        }

        if (result == null)
            return;

        // This closes the connection when the end of the data is reached
        if (_shouldClose)
            result = new AutoCloseInputStream(result, this);

        _responseStream = result;
    }

    protected final void writeRequest() throws IOException
    {
        // Resets all of the read state so we are not looking
        // at stale state while processing the write
        resetBeforeRequest();

        _log.trace("writeRequest");

        if (_streamingMode != STREAM_RAW)
        {
            writeRequestLine();
            writeRequestHeaders();
            _conOutStream.write(Util.CRLF_BYTES);
        }
        // No flush until we have written all we are going to write

        if (useDummyAuthContent())
        {
            if (_authenticationDummyContent != null)
            {
                _connLog.debug("writeRequest - writing dummy auth content: "
                    + _authenticationDummyContent);
                _conOutStream.write(_authenticationDummyContent.getBytes());
                _dummyAuthRequestSent = true;
            }
            _conOutStream.flush();
        }

        // We don't write the body in streaming mode, the user does
        // that
        if (_streamingMode > STREAM_NONE)
        {
            _log.debug("writeRequest - streaming request header sent");
            _requestSent = true;
            if (_streamingMode > STREAM_DEFER_RESPONSE_END)
                _conOutStream.flush();
            return;
        }

        _bodySent = writeRequestBody();
    }

    protected final void writeRequestLine() throws IOException
    {
        // request, spaces, etc
        final int OTHER = 50;

        byte[] bytes = new byte[_pathQuery.length()
            + (_connection._hostPortURL.length() * 2)
            + OTHER];

        int index = 0;
        int len = 0;
        String str;

        // Yes, we use the deprecated String.getBytes() here, but that's
        // what we want because it's fast and we know we are always
        // working with ASCII

        _dummyAuthRequestSent = false;

        // We are expecting to do authentication and not actually
        // write the data
        if (useDummyAuthContent() && _authenticationDummyMethod != null)
        {
            _connLog.debug("writeRequestLine - setting dummy auth method to: "
                + _authenticationDummyMethod);
            _actualMethodPropsSent = getMethodProperties(_authenticationDummyMethod);
            _dummyAuthRequestSent = true;
            len = _authenticationDummyMethod.length();
            _authenticationDummyMethod.getBytes(0, len, bytes, index);
            index += len;
        }
        else
        {
            // Reset this incase it was changed from a previous request
            _actualMethodPropsSent = _methodProperties;
            len = method.length();
            method.getBytes(0, len, bytes, index);
            index += len;
        }

        bytes[index++] = ' ';

        if ((_actualMethodPropsSent & METHOD_PROP_REQ_LINE_URL) != 0)
        {
            // Even for SSL (transparent), if the protocol is not HTTP
            // need to send the full request line
            if (_connection.isProxied()
                && (!_connection.isTransparent() || !_scheme.toLowerCase()
                        .startsWith("http")))
            {
                str = _scheme;
                len = str.length();
                str.getBytes(0, len, bytes, index);
                index += len;

                str = "://";
                len = str.length();
                str.getBytes(0, len, bytes, index);
                index += len;

                len = _connection._hostPortURL.length();
                _connection._hostPortURL.getBytes(0, len, bytes, index);
                index += len;
            }

            len = _pathQuery.length();
            _pathQuery.getBytes(0, len, bytes, index);
            index += len;

            // For testing, works with ParamServlet
            if (_addDiagSequence)
            {
                synchronized (getClass())
                {
                    // Don't care if this is slow
                    index = Util.toByteAscii("&Sequence=" + (++_diagSequence),
                                             bytes,
                                             index);
                }
            }
        }
        else if ((_actualMethodPropsSent & METHOD_PROP_REQ_LINE_STAR) != 0)
        {
            bytes[index++] = '*';
        }
        else if ((_actualMethodPropsSent & METHOD_PROP_REQ_LINE_HOST_PORT) != 0)
        {
            // Put the host/port junk on the front if required
            len = _connection._hostPort.length();
            _connection._hostPort.getBytes(0, len, bytes, index);
            index += len;
        }

        str = _http11 ? " HTTP/1.1\r\n" : " HTTP/1.0\r\n";
        len = str.length();
        str.getBytes(0, len, bytes, index);
        index += len;

        _conOutStream.write(bytes, 0, index);
    }

    protected final void writeRequestHeaders() throws IOException
    {
        addRequestHeaders();
        _reqHeaders.write(_conOutStream);
    }

    // Returns true if the request body was sent
    // This is required to flush the output stream even if there is nothing
    // to send, as the output stream has not been flushed before this point
    protected final boolean writeRequestBody() throws IOException
    {
        // In authentication, the body has not been sent
        if (useDummyAuthContent())
        {
            return false;
        }

        // Nothing to write
        if (_contentLength == 0)
        {
            _connection.conditionalFlush(this);
            return true;
        }

        if (_expectContinue && _responseCode != HttpStatus.SC_CONTINUE)
        {
            return false;
        }

        if (_requestBytes != null)
        {
            // We already have the bytes
            if (_contentLength > _requestBytes.length)
            {
                throw new HttpException("Data length ("
                    + _requestBytes.length
                    + ") does not match specified "
                    + "Content-Length of ("
                    + _contentLength
                    + ")");
            }

            int size = Math.min(_contentLength, _requestBytes.length);
            _conOutStream.write(_requestBytes, 0, size);
            if (_log.isDebugEnabled())
                _log.debug("wrote (from saved bytes): " + size);
        }
        else if (_requestBody != null)
        {
            // We have to read from the stream, this will be the typical
            // case the first time
            int total;

            if (_log.isDebugEnabled())
                _log.debug("should write (Content-Length): " + _contentLength);

            // The content length may be smaller than the size of
            // the data
            _requestBytes = new byte[_contentLength];
            total = Util.copyStreams(_requestBody,
                                     _conOutStream,
                                     _requestBytes,
                                     _contentLength);

            if (_log.isDebugEnabled())
                _log.debug("wrote: " + total);

            _requestBody.close();

            if (total != _contentLength)
            {
                throw new HttpException("Data length ("
                    + total
                    + ") does not match specified "
                    + "Content-Length of ("
                    + _contentLength
                    + ")");
            }
        }

        _connection.conditionalFlush(this);
        return true;
    }

    private final void writeRemainingRequestBody() throws IOException
    {
        if (HttpStatus.SC_CONTINUE == _responseCode)
        {
            _log.debug("Found continue - sending request body");
            if (!_bodySent)
            {
                _bodySent = writeRequestBody();
            }
            else
            {
                _log.debug("Received status CONTINUE but the body has already "
                    + "been sent");
                // According to RFC 2616 this respose should be ignored
            }
        }
    }

    protected void processResponse() throws IOException
    {
        // Loop to read any informational responses, they
        // don't have any bodies and we are supposed to
        // ignore them, except for 100 which means we can
        // write the remainder of the request body
        do
        {
            readResponse();

            // Make sure we have read the entire response before the
            // connection is released. This also ensures a retry if
            // there is a problem getting the response
            if (_responseCode >= HttpStatus.SC_OK)
            {
                // We got an unexpected good response from a dummy
                // authentication attempt, indicate we are authenticated and go
                // around again to do the user's request
                if (_responseCode < HttpStatus.SC_REDIRECTION
                    && _dummyAuthRequestSent)
                {
                    String str = "Retrying request because of positive reponse to dummy auth request";
                    _connLog.debug(str);

                    setAuthState(AS_AUTHENTICATED, _currentAuthType);
                    _tryCount = 0;
                    // IOException causes retry
                    throw new IOException(str);
                }

                if (_responseCode >= HttpStatus.SC_REDIRECTION
                    && readSeparateFromWrite())
                {
                    // Doing a pipelined read, we don't own the connection and
                    // got a retryable response
                    if (_pipelinedReading
                        && (_responseCode < HttpStatus.SC_400
                            || _responseCode == HttpStatus.SC_UNAUTHORIZED || _responseCode == HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED))
                    {
                        String str = "Unexpected "
                            + _responseCode
                            + " during pipelined read on: "
                            + this;
                        _connLog.debug(str);

                        // Resent authentication state based on response
                        if (_responseCode == HttpStatus.SC_UNAUTHORIZED)
                        {
                            _currentAuthType = AUTH_NORMAL;
                            setAuthState(AS_NEEDS_AUTH, AUTH_NORMAL);
                        }
                        else if (_responseCode == HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED)
                        {
                            _currentAuthType = AUTH_PROXY;
                            setAuthState(AS_NEEDS_AUTH, AUTH_PROXY);
                        }
                        else
                        {
                            _currentAuthType = -1;
                            // Redirect, we don't know what's going to happen
                            setAuthState(AS_NONE, AUTH_NORMAL);
                        }

                        // Force a retry at a higher level for redirect or auth
                        _tryCount = 0;
                        HttpException ex = new HttpException(str);
                        ex._retryable = true;
                        throw ex;
                    }
                    break;
                }
            }

            // if SC_CONTINUE write the request body
            writeRemainingRequestBody();

            // Reset everything before we read again, the first
            // resetBeforeRead() is called after we write the request
            if (_responseCode < HttpStatus.SC_OK)
                resetBeforeRequest();
        }
        while (_responseCode < HttpStatus.SC_OK);

        if (_responseCode >= HttpStatus.SC_OK)
        {
            // Completely good response, record the statistics
            if (_responseCode < HttpStatus.SC_REDIRECTION)
            {
                _connManager.recordCount(HttpConnectionManager.COUNT_SUCCESS);
                if (_connection != null)
                    _connection._totalResUrlConCount++;
            }

            // Good and redirect, the authentication process is complete
            if (_responseCode < HttpStatus.SC_400 && _currentAuthType >= 0)
                setAuthState(AS_AUTHENTICATED, _currentAuthType);
        }

    }

    // Returns true if we need to start authentication processing
    private boolean startAuthProcessing(int urlConAuthType, int connAuthType)
    {
        switch (urlConAuthType)
        {
            case 0:
            case Credential.AUTH_BASIC:
                // Never, since basic is always done preemptively
                return false;
            case Credential.AUTH_DIGEST:
                // Always, since we can't do this preemptively
                if ((_pipeliningOptions & PIPE_PIPELINE) != 0)
                {
                    throw new IllegalStateException("Using Digest authentication with "
                        + "pipeling is not allowed (because it makes no sense), "
                        + "if you must use Digest authentication, turn off pipelining");
                }
                // This is useful with streaming though
                return true;
            case Credential.AUTH_NTLM:
                // Only if the socket connection has not been autheticated
                if (connAuthType == -1)
                    return true;
                return false;
            default:
                Util.impossible("Invalid auth type: " + urlConAuthType);
                return false;
        }
    }

    protected void processPreemptiveAuth(int normalOrProxy)
        throws HttpException,
            InterruptedIOException
    {
        if ((_forwardAuthCount == 1 || _authState[normalOrProxy] == AS_FINAL_AUTH_SENT)
            && _authState[normalOrProxy] != AS_AUTHENTICATED)
        {
            if (_log.isDebugEnabled())
            {
                _log.debug("processPreemptiveAuth - "
                    + normalOrProxyToString(normalOrProxy)
                    + " checking for preemptive authentication");
            }

            boolean sent = callAuthenticate(null, null, normalOrProxy);

            // The first time, initiate authentication processing
            // if required, this bypasses pipelining/streaming until
            // the authentication is complete
            if (startAuthProcessing(_authenticationType[normalOrProxy],
                                    _connection._authProtocol[normalOrProxy]))
            {
                setAuthState(AS_NEEDS_AUTH, normalOrProxy);
                sent = true;
            }

            if (sent)
                _currentAuthType = normalOrProxy;

        }

    }

    private final void processRequest() throws IOException
    {
        // Inner retry loop, this retries to the same destination
        // in the case of connection failures and timeouts to the _maxTries
        // limit
        do
        {
            try
            {
                // Bypass this when we have already done it in the
                // streaming/pipelining case
                if (!_requestSent)
                {
                    _tryCount++;

                    if (_log.isDebugEnabled())
                    {
                        _log.debug("Attempt number "
                            + _tryCount
                            + " (forward/auth count: "
                            + _forwardAuthCount
                            + ") to write request");
                    }

                    if (_tryCount > 1)
                    {
                        // Get rid of previous responses because we don't want
                        // them considered for authentication
                        _respHeaders.clear();

                        if (_globalState._retryInterval != 0)
                        {
                            try
                            {
                                if (_connLog.isDebugEnabled())
                                {
                                    _connLog.debug("Before retry sleep: "
                                        + _globalState._retryInterval);
                                }
                                Thread.sleep(_globalState._retryInterval);
                            }
                            catch (InterruptedException ex)
                            {
                                // Ignore
                            }
                        }
                    }

                    HttpConnection oldConnection = _connection;

                    // We are going to write, so we have to connect
                    connect();

                    // No retry allowed (POST)
                    if ((_actualMethodPropsSent & METHOD_PROP_RETRY) == 0)
                    {
                        _connection.checkConnection();
                        // Don't allow any retries beyond this point
                        // for a POST as its not idempotent.
                        _maxTries = 1;
                    }

                    // Should we initiate authentication? This can happen
                    // at the beginning, or if we are resending after
                    // a successful authentication (because the connection
                    // closed after the authentication)
                    processPreemptiveAuth(AUTH_NORMAL);
                    if (_connection.isProxied())
                        processPreemptiveAuth(AUTH_PROXY);

                    // We switched connections, if we got a connection that's
                    // already authenticated or that has pipelined reads pending
                    // then just have the pipeline mechanism retry

                    if (oldConnection != _connection
                        && (_pipeliningOptions & PIPE_PIPELINE) != 0
                        && _currentAuthType >= 0
                        && _authState[_currentAuthType] >= AS_NEEDS_AUTH
                        && _authState[_currentAuthType] < AS_FINAL_AUTH_SENT
                        && (_connection._authProtocol[AUTH_NORMAL] > 0
                            || _connection._authProtocol[AUTH_PROXY] > 0 || _connection
                                .getPipelinedUrlConCount() > 1))
                    {
                        setAuthState(AS_NEEDS_AUTH, _currentAuthType);
                        String str = "Authentication sequence interrupted "
                            + "because found authenticated connection - retry";
                        _connLog.debug(str);
                        // FIXME we should throw something different because
                        // this will cause the connection to close and that's
                        // not what we want, we just cannot use this connection
                        throw new IOException(str);
                    }

                    // Do the start/end prevent close only for the pipelined
                    // case, since that's the only case where we can have
                    // multiple threads access the same connection
                    try
                    {
                        if ((_pipeliningOptions & PIPE_PIPELINE) != 0)
                            _connection.startPreventClose();
                        writeRequest();
                    }
                    finally
                    {
                        if ((_pipeliningOptions & PIPE_PIPELINE) != 0)
                            _connection.endPreventClose();
                    }

                    // The user writes the request in streaming/pipelining mode
                    if (readSeparateFromWrite())
                    {
                        _log.debug("processRequest - "
                            + "streaming/pipelining exiting "
                            + "after request written");
                        return;
                    }
                }

                processResponse();

                break;
            }
            catch (SocketTimeoutException ioe)
            {
                // SocketTimeoutException is a subclass of
                // InterruptedIOException;
                // but we want to handle as a timeout
                HttpTimeoutException htex = new HttpTimeoutException();
                htex.initCause(ioe);
                if (_log.isDebugEnabled())
                {
                    _log.debug("Converted to "
                        + ioe
                        + " to HttpTimeoutException", htex);
                }
                throw htex;
            }
            catch (InterruptedIOException ioiex)
            {
                // Handled at a higher level
                throw ioiex;
            }
            catch (HttpException httpe)
            {
                // This is non-recoverable, throw it up to the user
                _connLog.debug("HttpException when writing "
                    + "request/reading response: ", httpe);
                // Handled at a higher level
                _dead = true;
                throw httpe;
            }
            catch (IOException ioe)
            {
                handleIOException(ioe);
            }
        }
        while (true);

    }

    private void handleIOException(IOException ioe) throws IOException
    {
        if (_connLog.isDebugEnabled())
        {
            _connLog.debug("IOException when writing "
                + "request/reading response: ", ioe);
        }

        if (_tryCount >= _maxTries)
        {
            _connManager
                    .recordCount(HttpConnectionManager.COUNT_FAIL_MAX_RETRY);
            throw maxRetryExceededWrapper(ioe);
        }

        // This can be retried
        releaseConnection(CLOSE);

        // Pipelining and before write sent, retry here
        if ((_pipeliningOptions & PIPE_PIPELINE) != 0 && !_requestSent)
        {
            _connLog.debug("Retrying pipelining before request sent");
            // fall through and retry
        }
        else if (readSeparateFromWrite())
        {
            if (_connLog.isWarnEnabled())
            {
                _connLog.warn("Retry not allowed for a streaming urlcon: "
                    + this);
            }
            // For pipelining in the read, retry happens at a higher
            // level
            throw ioe;
        }

        // So we resend the request on a retry
        _requestSent = false;

        _connManager.recordRetry(_connManager._retryCounts, _tryCount);
    }

    // Returns true if retry needed
    private boolean processRedirectResponse()
    {
        _log.debug("Redirect required");

        if (!getInstanceFollowRedirects())
        {
            _log.debug("Redirect requested but "
                + "followRedirects is "
                + "disabled");
            return false;
        }

        if (_hdrLocation == null)
        {
            // got a redirect response, but no location header
            _connLog.error("Received redirect response "
                + _responseCode
                + " but no location header");
            return false;
        }

        if (_connLog.isDebugEnabled())
        {
            _connLog.debug("Redirect requested to location '"
                + _hdrLocation
                + "'");
        }

        // rfc2616 demands the location value be a complete URI
        // Location = "Location" ":" absoluteURI
        URL u = null; // the new url
        try
        {
            u = new URL(new String(_hdrLocation));
        }
        catch (Exception ex)
        {
            // Fall through and try to make a URL based on
            // what we know
        }

        if (u == null)
        {
            // try to construct the new url based on the current url
            try
            {
                URL currentUrl = new URL(_urlString);
                u = new URL(currentUrl, new String(_hdrLocation));
            }
            catch (Exception ex)
            {
                _connLog.error("Redirected location '"
                    + _hdrLocation
                    + "' is malformed");
                return false;
            }
        }

        // Get rid of the hold Host header since we are
        // going somewhere else
        _reqHeaders.remove(HDR_HOST_BYTES);
        _urlString = u.toExternalForm();

        // change the path and query string to the redirect
        String newPathQuery = URIUtil.getPathQuery(u.toExternalForm());
        if (_connLog.isDebugEnabled())
        {
            _connLog.debug("Changing path/query from \""
                + _pathQuery
                + "\" to \""
                + newPathQuery
                + "\" in response to "
                + _responseCode
                + " response.");
        }
        _pathQuery = newPathQuery;
        // Do a retry
        return true;
    }

    private boolean callAuthenticate(String reqType,
                                     Map challengeMap,
                                     int normalOrProxy)
        throws HttpException,
            InterruptedIOException
    {

        // See if it's in the cache
        if (!HttpURLConnection.getMultiCredentialsPerAddress())
        {
            if (getConnection() != null
                && getCredentialSent(normalOrProxy) == null)
            {
                UserCredential cred = null;

                cred = (UserCredential)HttpURLConnection.getConnectionManager()
                        .getCachedCredential(getConnection()._connectionInfo,
                                             normalOrProxy);

                setCredentialSent(-1,
                                  normalOrProxy,
                                  cred,
                                  _authState[normalOrProxy]);

            }
        }

        boolean close = Authenticator.isNtlm(challengeMap);

        boolean sent = false;

        try
        {
            sent = Authenticator.authenticate(this, (challengeMap != null
                ? _respHeaders
                : null), reqType, challengeMap, normalOrProxy);

            if (!sent)
                return sent;

            // Successful authentication
            if (!HttpURLConnection.getMultiCredentialsPerAddress())
            {
                HttpURLConnection.getConnectionManager()
                        .setCachedCredential(getConnection()._connectionInfo,
                                             normalOrProxy,
                                             getCredentialSent(normalOrProxy));
            }
        }
        catch (HttpException ex)
        {
            authenticationFailed(close, normalOrProxy);
            throw ex;
        }
        return sent;
    }

    static final boolean AUTH_FAILED_CLOSE = true;

    void authenticationFailed(boolean close, int normalOrProxy)
        throws InterruptedIOException
    {
        _log.debug("Authentication failed");

        if (close)
        {
            _log.debug("Authentication failed - closing connection");
            releaseConnection(CLOSE);
        }
        if (!HttpURLConnection.getMultiCredentialsPerAddress())
        {
            HttpURLConnection.getConnectionManager()
                    .resetCachedCredential(getConnection()._connectionInfo,
                                           normalOrProxy);
        }

    }

    // Returns true if retry needed
    private boolean processAuthenticationResponse()
        throws HttpException,
            InterruptedIOException
    {
        _log.debug("processAuthenticationResponse");

        switch (_responseCode)
        {
            case HttpStatus.SC_UNAUTHORIZED:
                if (_hdrWWWAuth == null)
                    return false;
                _currentAuthType = AUTH_NORMAL;
                break;

            case HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED:
                if (_hdrProxyAuth == null)
                    return false;
                _currentAuthType = AUTH_PROXY;
                break;
        }

        boolean authenticated = false;

        // remove preemptive header and reauthenticate
        _reqHeaders.remove(Authenticator.RESP_HEADERS_LC[_currentAuthType]);
        try
        {

            String reqType = Authenticator.REQ_HEADERS[_currentAuthType];

            // parse the authenticate header
            Map challengeMap = Authenticator
                    .parseAuthenticateHeader(_respHeaders, reqType, this);

            authenticated = callAuthenticate(reqType,
                                             challengeMap,
                                             _currentAuthType);
        }
        catch (HttpException ex)
        {
            // Don't show this to the user, just indicate that we are
            // authenticated
            _connLog.warn("Exception processing authentication response: ", ex);
            return false;
        }

        if (!authenticated)
        {
            // won't be able to authenticate to this challenge
            // without additional information
            _connLog.debug("Server demands "
                + "authentication credentials, but none are "
                + "available, so aborting.");
        }
        else
        {
            _log.debug("Server demanded "
                + "authentication credentials, will try again.");
            // let's try it again, using the credentials
        }

        // Do a retry if true
        return authenticated;
    }

    // Handles processing after the request is sent for streaming/pipelining
    private void processAfterRequestStreamPipe() throws IOException
    {
        // Either a redirection or error, we can't continue
        // For Raw, always pass the response code through
        if (_responseCode > HttpStatus.SC_REDIRECTION
            && _streamingMode != STREAM_RAW)
        {
            HttpRetryException rte = new HttpRetryException(Util
                                                                    .bytesToString(_responseTextBytes,
                                                                                   _responseTextLength),
                                                            _responseCode);
            rte._location = Util.bytesToString(_hdrLocation, _hdrLocationLen);
            if (_connLog.isDebugEnabled())
            {
                _connLog.debug("Streaming/pipelining non-OK "
                    + "response, throwing", rte);
            }
            throw rte;
        }
        _log.debug("Streaming/pipelining - returning");
    }

    protected final void processRequestLoop() throws IOException
    {
        int maxForwards = _connManager._globalState._defaultMaxForwards;

        // Loop for redirection and authentication retries
        while (_forwardAuthCount++ < maxForwards)
        {
            boolean redirect = false;

            // Write the request and read the response, will retry if
            // necessary
            processRequest();

            // We let the user send the bytes if streaming
            if (doOutput && readSeparateFromWrite())
            {
                processAfterRequestStreamPipe();
            }

            // Because of pipelining or streaming we have not read
            // the response yet, so just get out
            if (!_responseHeadersRead)
                return;

            if (_responseCode >= HttpStatus.SC_400)
            {
                _connManager
                        .recordCount(HttpConnectionManager.COUNT_FAIL_GE_400);
            }

            switch (_responseCode)
            {
                case HttpStatus.SC_UNAUTHORIZED:
                case HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED:
                    if (_doAuthentication)
                    {
                        // processAuthenticationResponse will set the
                        // authState

                        // Returns true if request should be retried
                        if (!processAuthenticationResponse())
                            return;
                    }
                    else
                    {
                        // Let the client handle the authenticaiton
                        _log.debug("Authorization required - "
                            + "client handles authentication");
                        return;
                    }
                    _requestSent = false;
                    _tryCount = 0;
                    break;

                case HttpStatus.SC_MOVED_TEMPORARILY:
                case HttpStatus.SC_MOVED_PERMANENTLY:
                case HttpStatus.SC_TEMPORARY_REDIRECT:
                    if (!processRedirectResponse())
                        return;
                    _requestSent = false;
                    _tryCount = 0;
                    redirect = true;
                    break;

                case HttpStatus.SC_OK:

                    // This happens if we expect an authentication challenge
                    // but none is forthcoming, in this case we just retry
                    // the request so the normal pipelining mechanism can be
                    // used
                    if (_forwardAuthCount == 1
                        && (streamingDeferResponse() || (_pipeliningOptions & PIPE_PIPELINE) != 0))
                    {
                        _connLog.warn("setAuthenticationType() specified with "
                            + "pipelining/streaming, but no authorization "
                            + "is required - retrying");

                        // Go around again
                        break;
                    }

                    // Fall through

                default:
                    // Neither an unauthorized nor a redirect response,
                    // no retry possible, or this is just normal
                    return;

            }

            if (_shouldClose)
            {
                releaseConnection(CLOSE);
            }
            else
            {
                // Any data left unread on the connection will be read
                // automatically before the next connecion uses this
                // because of the _unfinishedResponseStream

                // For redirect, force getting a new connection, for auth
                // we way on the same connection
                if (redirect)
                    releaseConnection(READING);
            }

        }

        _connLog.debug("Giving up after "
            + maxForwards
            + " forwards/authentication restarts");
        throw new HttpException("Maximum redirects/authentication restarts ("
            + maxForwards
            + ") exceeded for "
            + this);
    }

    // Called by execute() and also called in getOutputStream() in the streaming
    // case, this starts the request and leaves it executing.
    protected final void executeStart()
        throws IOException,
            InterruptedIOException
    {
        if (_executed)
            return;

        // Default is GET
        if ((_actualMethodPropsSent & METHOD_PROP_UNSPECIFIED_METHOD) != 0)
        {
            _log.debug("Method not specified, setting to GET");
            setRequestMethodInternal(HTTP_METHOD_GET);
        }

        if (_log.isDebugEnabled())
        {
            _log.debug("executeStart " + method);
        }

        try
        {
            _executing = true;
            processRequestLoop();
        }
        catch (SocketTimeoutException httpe)
        {
            // SocketTimeoutException is a subclass of InterruptedIOException,
            // but we should not get it here because it it converted to an
            // HttpTimeoutException at a lower level
            Util.impossible("SocketTimeoutException should not be here", httpe);
            // throws
        }
        catch (InterruptedIOException ioiex)
        {
            // Timeout
            _connLog.debug("Interrupted when reading response: ", ioiex);
            releaseConnection(CLOSE);
            _dead = true;
            throw ioiex;
        }
        catch (HttpTimeoutException ioiex)
        {
            // Timeout
            _connLog.debug("HttpTimeoutException when reading response: ",
                           ioiex);
            releaseConnection(CLOSE);
            _dead = true;
            throw ioiex;
        }
        catch (IOException httpe)
        {
            releaseConnection(CLOSE);
            // Remember this in case we have to re-throw it, also sets _dead
            recordIOException(httpe);
            throw httpe;
        }
        catch (RuntimeException re)
        {
            _connLog.debug("Unexpected exception processing request ", re);
            releaseConnection(CLOSE);
            _dead = true;
            throw re;
        }
        catch (Error e)
        {
            _connLog.debug("Unexpected error processing request ", e);
            releaseConnection(CLOSE);
            _dead = true;
            throw e;
        }

    }

    // Executes the entire HTTP request and response, used for the
    // non-streaming case
    protected final void execute() throws IOException
    {
        _log.debug("execute");

        if (_dead)
        {
            if (_connLog.isDebugEnabled())
                _connLog.debug("execute - ignoring dead urlcon");
            return;
        }

        if (_streamingMode == STREAM_TUNNELED
            && (_actualMethodPropsSent & METHOD_PROP_SUPPORTS_TUNNELED) == 0)
        {
            throw new IllegalStateException("Can only using tunneled streaming "
                + "mode with the an HTTP method that supports is (e.g. CONNECT)");
        }

        // Reset to the real method we want, this might have been changed
        // earlier during authentication
        _actualMethodPropsSent = _methodProperties;

        boolean streaming = _streamingMode > STREAM_NONE;

        // Handle the methods that allow data
        if (!streaming
            && _outStream != null
            && ((_actualMethodPropsSent & METHOD_PROP_CALCULATE_CONTENT_LEN) != 0))
        {
            byte[] outBytes = ((ByteArrayOutputStream)_outStream).toByteArray();
            setRequestBody(outBytes);
            if (_contentLength == UNINIT_CONTENT_LENGTH)
                _contentLength = outBytes.length;
        }

        try
        {
            if (!_streamingWritingFinished)
            {
                if (streamingDeferResponse())
                {
                    streamWriteFinished(!OK);
                    String msg = "Cannot execute the HTTP request "
                        + "until the output stream is closed.";
                    IllegalStateException ex = new IllegalStateException(msg);
                    _log.debug("execute: ", ex);
                    throw ex;
                }

                if ((_pipeliningOptions & PIPE_PIPELINE) != 0)
                {
                    streamWriteFinished(!OK);
                    String msg = "This is a pipelined request, use the pipeline Callback to access the urlCon.";
                    IllegalStateException ex = new IllegalStateException(msg);
                    _log.debug("execute: ", ex);
                    throw ex;
                }
            }
            executeStart();
        }
        finally
        {
            _log.debug("execute - finally");

            _executing = false;
            _executed = true;

            // We can release if there is nothing to read, otherwise
            // the connection is released when the stream is read
            // For a bad response, we buffer the output so we can
            // release the connection, this way the user is not
            // required to read it
            if ((_streamingMode <= STREAM_DEFER_RESPONSE_END)
                && (_responseIsEmpty || _responseCode >= HttpStatus.SC_400))
                releaseConnection(READING);
        }

    }

    // Does the write for a pipelined connection. This can happen
    // on any thread, but is synchronized because it must obtain
    // the connection exclusively from the connection manager
    // Returns false if the write should be retried
    protected boolean processPipelinedWrite()
        throws InterruptedException,
            InterruptedIOException
    {
        if (_log.isDebugEnabled())
        {
            _log.debug("processPipelinedWrite " + this);
        }

        Exception ex = null;

        try
        {
            OutputStream os = null;
            if (doOutput)
            {
                // This sends the request and then allows the user to
                // write to the stream, the connection is released when
                // this output stream is closed
                // Note that we allow only the streaming output streams
                // for pipelining, which call streamWriteFinished() when
                // their connection is closed
                os = getOutputStream();

                HttpConnection connection = _connection;
                try
                {
                    connection.startPreventClose();
                    _callback.writeRequest(this, os);
                    _connManager
                            .recordCount(HttpConnectionManager.COUNT_PIPELINE_WRITE_REQ);
                }
                finally
                {
                    connection.endPreventClose();
                }
            }
            else
            {
                // Nothing for the user to write, send the request out
                // (the startPreventClose stuff is called inside of here for the
                // pipelined case)
                executeStart();
                // Indicate we have sent the request out
                _requestSent = true;
                // This releases the connection
                streamWriteFinished(true);
            }
        }
        catch (IOException iex)
        {
            _ioException = iex;
        }
        catch (Exception sex)
        {
            ex = sex;
        }

        // The _ioException could happen without a throw above if
        // it happened inside of the AutoRetryOutputStream
        if (_ioException != null)
            ex = _ioException;

        if (ex != null)
        {
            // Some problem writing, pass this to the user, we have
            // already retried if possible
            if (_connLog.isDebugEnabled())
            {
                _connLog.debug("processPipelineWrite " + "exception writing: ",
                               ex);
            }

            // The write is not going to happen for this time (it may
            // with a retry, but then the count will be incremented again
            try
            {
                _connManager.urlConWasWritten(this);
            }
            catch (IOException e)
            {
                if (_connLog.isDebugEnabled())
                {
                    _connLog.debug("processPipelinedWrite: "
                        + "IOException when recording write - ignoring", e);
                }
                // Ignore it, this was caused by a problem flushing
                // something, possibly on an unrelated connection. The
                // urlcon involved will get the appropriate exception
                // it tries to read
            }

            // We got so far as to get a connection, we need to indicate
            // we are no longer using that connection
            if (_connection != null)
                _connection.adjustPipelinedUrlConCount(-1);

            if (_ioException != null)
            {
                if (_connLog.isDebugEnabled())
                {
                    _connLog.debug("processPipelinedWrite "
                        + "AutomaticHttpRetryException thrown while writing "
                        + "Callback - attempting retry ", _ioException);
                }

                // Returns null if we can retry
                Exception ioex = attemptPipelinedRetry(_ioException);
                if (ioex == null)
                    return false;
                ex = ioex;
                // Continue
            }

            _dead = true;

            // Tell the user what went wrong
            callPipelineError(null, ex);

            // We are done with this urlcon
            if (_connLog.isDebugEnabled())
                _connLog.debug("urlConWasRead (write error): " + this);
            if (_pipelineExpectBlock)
            {
                _connManager.urlConWasRead(_thread);
            }

            if (ex instanceof InterruptedIOException)
                throw (InterruptedIOException)ex;
            if (ex instanceof InterruptedException)
                throw (InterruptedException)ex;
        }

        // All OK
        return true;
    }

    // Process the pipelined read, this is executed on the HttpConnectionThread,
    // that guarantees that each connection is accessed by only one thread for
    // reading.
    // Returns true if it worked, false if a retry is required which is a call
    // to processPipelinedWrite()
    protected boolean processPipelinedRead()
        throws InterruptedException,
            InterruptedIOException
    {
        Exception ex = null;
        InputStream is = null;

        // Save the connection going in, the connection might change
        // in the event of initiating a retry, and we need to know to
        // reduce the count on the original connection
        HttpConnection connection = _connection;

        boolean readSuccessful = false;

        // Make sure we have only one read at a time for the connection,
        // since enqueueing work happens in the middle of writing (if we
        // have to write for authentication), we don't want to be
        // reentered
        synchronized (this)
        {
            try
            {
                if (_log.isDebugEnabled())
                {
                    _log.debug("processPipelinedRead "
                        + "for HttpURLConnection: "
                        + this);
                }

                // Gets or blocks for the connection, could throw
                // if there is a problem
                try
                {
                    _pipelinedReading = true;

                    connection.startReadBugCheck();

                    // Don't allow a close while we have the input stream
                    connection.startPreventClose();

                    if (!_executed)
                        execute();

                    // We have written authentication, now return so the
                    // response may be read on the new connection
                    if (_currentAuthType >= 0
                        && _authState[_currentAuthType] == AS_FINAL_AUTH_SENT)
                    {
                        _requestSent = true;

                        if (_connLog.isDebugEnabled())
                        {
                            _connLog.debug("processPipelinedRead "
                                + "unexpected authentication - "
                                + "wrote authentication and returning");
                        }
                        // Will retry
                        return true;
                    }

                    // Gets the input stream if we actually read
                    is = getResponseStream();
                }
                catch (IOException iex)
                {
                    // This represents an actual thread interrupt, since
                    // this specific exception was thrown from below. Note
                    // that this has to be the exact class because
                    // there are exceptions that are a subclass
                    // (SocketTimeoutException)
                    // which should be treated as a normal IOException
                    if (iex.getClass() == InterruptedIOException.class)
                        throw (InterruptedIOException)iex;

                    // We are not reading during a retry
                    _pipelinedReading = false;

                    // Returns the exception to use, or null if we can retry
                    ex = attemptPipelinedRetry(iex);
                    if (ex == null)
                        return false;
                }
                catch (Exception sex)
                {
                    ex = sex;
                }
                finally
                {
                    connection.endReadBugCheck();
                    _pipelinedReading = false;
                }

                if (_log.isDebugEnabled())
                {
                    _log.debug("processPipelinedRead "
                        + "calling callback "
                        + "for HttpURLConnection: "
                        + this
                        + " is: "
                        + is);
                    if (ex != null)
                        _connLog.debug("processPipelinedRead: EXCEPTION", ex);
                }

                Exception saveExc = _ioException;
                try
                {
                    if (ex != null && ex instanceof IOException)
                        _ioException = (IOException)ex;
                    // Prevent any kind of re-execution
                    _executed = true;

                    // Call the user
                    if (ex != null
                        || _responseCode >= HttpStatus.SC_REDIRECTION)
                    {
                        callPipelineError(is, ex);
                    }
                    else
                    {
                        _callback.readResponse(this, is);
                        _connManager
                                .recordCount(HttpConnectionManager.COUNT_PIPELINE_READ_RESP);
                    }

                    // Inside of the call to the user a stream got closed and
                    // caused an exception reading, it will have thrown an
                    // AutomaticHttpRetryException to the user, if this happens
                    // then do the retry
                    if (_ioException instanceof AutomaticHttpRetryException)
                    {
                        if (_connLog.isDebugEnabled())
                        {
                            _connLog.debug("processPipelinedRead "
                                + "AutomaticHttpRetryException thrown in "
                                + "Callback - attempting retry ", _ioException);
                        }

                        // Returns null if we can retry
                        Exception ioex = attemptPipelinedRetry(_ioException);
                        if (ioex == null)
                            return false;
                        if (ioex instanceof IOException)
                            _ioException = (IOException)ioex;
                    }

                }
                catch (Throwable t)
                {
                    // Ignore this
                    _connLog.error("Exception thrown out of "
                        + "Callback.readResponse() for: "
                        + this
                        + " conn: "
                        + _connection, t);
                }
                finally
                {
                    _ioException = (IOException)saveExc;
                }

                readSuccessful = true;
            }
            finally
            {
                connection.endPreventClose();

                // Make sure this gets closed even if the user doesn't
                try
                {
                    if (is != null)
                        is.close();
                }
                catch (IOException iex)
                {
                    // We don't care about exception during close
                    if (_connLog.isDebugEnabled())
                    {
                        _connLog
                                .debug("Exception during close of InputStream for: "
                                           + this
                                           + " conn: "
                                           + _connection,
                                       iex);
                    }
                }

                // Regardless of what happened, if we are on a different
                // connection now, then adjust the count for the former
                // connection (which is probably dead anyways)
                if (connection != _connection)
                    connection.adjustPipelinedUrlConCount(-1);

                if (readSuccessful)
                {
                    _log.debug("processPipelineRead - SUCCESS");
                    _connection.adjustPipelinedUrlConCount(-1);
                    if (_log.isDebugEnabled())
                    {
                        _log.debug("urlConWasRead (read success): "
                            + this
                            + " total reqs/con: "
                            + _connection._totalReqUrlConCount);
                    }
                    if (_pipelineExpectBlock)
                        _connManager.urlConWasRead(_thread);
                }
            }
        }
        return true;
    }

    protected void callPipelineError(InputStream is, Exception ex)
    {
        if (_connLog.isDebugEnabled())
        {
            _connLog.debug("pipeling error callback: " + this, ex);
        }
        _callback.error(this, is, ex);
        _connManager.recordCount(HttpConnectionManager.COUNT_PIPELINE_ERROR);
    }

    // Returns null if retry is possible, otherwise returns the exception to
    // throw
    protected Exception attemptPipelinedRetry(IOException iex)
        throws InterruptedException,
            InterruptedIOException
    {
        if (_connManager != _globalState._connManager)
            return new HttpException("Not retrying after immediateShutdown()");

        if (_connLog.isDebugEnabled())
        {
            _connLog.debug("pipelined exception - checking retry attempt "
                + this
                + " trycount: "
                + _tryCount
                + " maxTries: "
                + _maxTries, iex);
        }

        if (iex instanceof HttpException)
        {
            if (!((HttpException)iex)._retryable)
                return iex;
        }

        // If allowed, give it another go
        if (_tryCount < _maxTries)
        {
            if (_connLog.isDebugEnabled())
            {
                _connLog.debug("pipelined RETRY attempt " + this);
            }

            releaseConnection(CLOSE);

            // FIXME - make this cleanup a little more sane
            _dead = false;
            _requestSent = false;
            _executed = false;
            _ioException = null;
            _connManager.recordRetry(_connManager._pipelineReadRetryCounts,
                                     _tryCount);
            _connManager.addUrlConToWrite(this);
            return null;
        }

        return maxRetryExceededWrapper(iex);
    }

    // Give it a wrapper to show how hard we tried
    protected IOException maxRetryExceededWrapper(IOException ex)
    {
        // If we did not retry, just give the underlying exception
        // Or if not pipelining. This exception is intended to provide
        // extra information useful for the pipelining case
        if (_tryCount == 1 || (_pipeliningOptions & PIPE_PIPELINE) == 0)
            return ex;

        String str = "Request "
            + this
            + " failed after trying "
            + _tryCount
            + " times. ";
        if ((_pipeliningOptions & PIPE_PIPELINE) != 0)
            str += "Consider increasing maxTries. ";

        _connLog.warn(str);

        // Give it a wrapper to show how hard we tried
        HttpException retryFailed = new HttpException(str);
        retryFailed.initCause(ex);
        return retryFailed;
    }

    void recordIOException(IOException ex)
    {
        _ioException = ex;
        _dead = true;
    }

    void pipelinedConnectionClosed() throws IOException
    {
        IOException ex = new SocketException("The socket connection ["
            + this
            + "] was closed "
            + "while waiting for a pipelined response.");
        recordIOException(ex);
        throw ex;
    }

    // Called when the streaming I/O is either done or if there
    // was a problem and it can never be done.

    static final boolean OK = true;

    void streamWriteFinished(boolean ok)
        throws IOException,
            InterruptedIOException
    {
        if (ok)
        {
            _log.debug("streaming write finished");

            if (doOutput)
                _connection.conditionalFlush(this);

            _streamingWritingFinished = true;
            if ((_pipeliningOptions & PIPE_PIPELINE) != 0)
            {
                try
                {
                    _connManager.urlConWasWritten(this);
                }
                catch (IOException ex)
                {
                    // We don't care about this, since this could apply to many
                    // urlcons, so just continue and do the read. The exception
                    // will show up in the read.
                    _connManager
                            .recordCount(HttpConnectionManager.COUNT_FLUSH_IO_ERRORS);
                }

                try
                {
                    _connManager.enqueueWorkForConnection(this);
                }
                catch (InterruptedException e)
                {
                    _connLog.debug("streamWriteFinished - thread interrupted");
                    _dead = true;
                    InterruptedIOException ex = new InterruptedIOException();
                    ex.initCause(e);
                    throw ex;
                }
            }
        }
        else
        {
            _connLog.debug("streaming write failed");
            // Release and close the connection since there is something wrong
            releaseConnection(CLOSE);
            _dead = true;
        }
    }

    // The response is read in a separate call from the write, either due
    // to pipelining or streaming
    protected boolean readSeparateFromWrite()
    {
        return (streamingDeferResponse() || (_pipeliningOptions & PIPE_PIPELINE) != 0)
            && (_currentAuthType == -1
                || _authState[_currentAuthType] == AS_NONE || _authState[_currentAuthType] >= AS_FINAL_AUTH_SENT);
    }

    // Streaming mode, but we don't get the response until the streaming is done
    // by the user. TUNNELED streaming mode is not like this.
    protected boolean streamingDeferResponse()
    {
        return _streamingMode > STREAM_NONE
            && _streamingMode <= STREAM_DEFER_RESPONSE_END;
    }

    void setConnection(HttpConnection conn, boolean release) throws IOException
    {
        super.setConnection(conn, release);

        if (_connection != null)
        {
            // For HTTP/1.0, leave this connection open
            if (_authProtocol[AUTH_NORMAL] == Credential.AUTH_NTLM
                || _authProtocol[AUTH_PROXY] == Credential.AUTH_NTLM)
            {
                _connection._ntlmLeaveOpen = true;
            }

            // FIXME - maybe replace this in the connection stuff with the use
            // of authProtocol there
            if (_log.isDebugEnabled())
            {
                _log.debug("setConn: NTLMLeaveOpen: "
                    + _connection._ntlmLeaveOpen);
            }
        }
    }

    // Returns true if should close the connection
    protected final boolean shouldCloseConnection()
    {
        boolean result = false;

        if (_connection != null)
        {
            if (_log.isDebugEnabled())
            {
                _log.debug("shouldClose: NTLMLeaveOpen: "
                    + _connection._ntlmLeaveOpen);
            }
        }

        if (_connectionCloseSent
            || (_hdrConnection != null && Util
                    .bytesEqual(HDR_VALUE_CLOSE_BYTES,
                                _hdrConnection,
                                _hdrConnectionLen))
            || (_hdrProxyConnection != null && Util
                    .bytesEqual(HDR_VALUE_CLOSE_BYTES,
                                _hdrProxyConnection,
                                _hdrProxyConnectionLen)))
        {
            _connLog.debug("Will CLOSE - "
                + "\"[Proxy-]Connection: close\" header found or sent.");

            if (_connection != null)
            {
                // Don't set the observed limits for connection closings related
                // to authentication
                if (_currentAuthType < 0
                    || _authState[_currentAuthType] >= AS_AUTHENTICATED)
                {
                    // Record the number of urlcons allowed before this got
                    // closed, don't count it if only one urlcon happened, as
                    // this is the case for NTLM authentication and it's not
                    // really the limit. Note that we can't test that we are
                    // actually in NTLM authentication here, since this is
                    // called before the NTLM authentication state is setup.
                    // Also, only update the observed max if it's less, some
                    // sites have been known to vary; let's be conservative
                    if (_connection._totalResUrlConCount > 1
                        && !_ignoreObservedMaxCount
                        && (_connection._totalResUrlConCount <= _connection._connectionInfo._observedMaxUrlCons || _connection._connectionInfo._observedMaxUrlCons == 0))
                    {
                        // Add one because this urlcon's response is not in the
                        // count yet (it will be later)
                        _connection._connectionInfo._observedMaxUrlCons = _connection._totalResUrlConCount + 1;
                        if (_connLog.isDebugEnabled())
                        {
                            _connLog
                                    .debug("Recording observed max: "
                                        + _connection._connectionInfo._observedMaxUrlCons
                                        + " for "
                                        + _connection);
                        }

                    }
                }
            }

            result = true;
        }

        // Still might stay open and this is HTTP 1.0
        if (!result && !_http11)
        {
            if (null != _hdrConnection
                && Util.bytesEqual(HDR_VALUE_KEEP_ALIVE_BYTES,
                                   _hdrConnection,
                                   _hdrConnectionLen))
            {
                _log.debug("HTTP/1.0 - Leave OPEN - Keep-Alive");
                result = false;
            }
            else if (null != _hdrProxyConnection
                && _connection != null
                && _connection.isProxied()
                && Util.bytesEqual(HDR_VALUE_KEEP_ALIVE_BYTES,
                                   _hdrProxyConnection,
                                   _hdrProxyConnectionLen))
            {
                _log.debug("HTTP/1.0 - Leave OPEN - Proxy Keep-Alive");
                result = false;
            }
            else if (((_actualMethodPropsSent & METHOD_PROP_LEAVE_OPEN) != 0)
                && (_responseCode == HttpStatus.SC_OK))
            {
                _log.debug("HTTP/1.0 - Leave OPEN - tunneling");
                result = false;
            }
            else if (_connection != null && _connection._ntlmLeaveOpen)
            {
                _log.debug("HTTP/1.0 - Leave OPEN - NTLM is used");
                result = false;
            }
            else
            {
                _connLog.debug("HTTP/1.0 - Will CLOSE connection");
                result = true;
            }
        }

        if (_log.isDebugEnabled())
            _log.debug("shouldCloseConnection: " + result);
        return result;
    }

    /**
     * Reads the entire response from the responseInputStream so that we can
     * close the connection.
     */
    protected final void discardResponseBody(InputStream is) throws IOException
    {
        if (is == null)
            return;

        int len = 0;
        try
        {
            len = Util.flushStream(is);
        }
        catch (IOException ex)
        {
            // Get rid of the connection
            releaseConnection(CLOSE);
        }

        _connLog.debug("Discarding response body - flushed " + len + " bytes ");
    }

    public static void setupAuthDummy(HttpURLConnection urlCon, boolean soap11)
    {
        // Setup web services for dummy authentication startup for NTLM
        String ns;
        if (soap11)
            ns = "http://schemas.xmlsoap.org/soap/envelope/";
        else
            ns = "http://www.w3.org/2003/05/soap-envelope";

        urlCon
                .setAuthenticationDummyContent("<?xml version='1.0' encoding='UTF-8'?>"
                    + "<soapenv:Envelope xmlns:soapenv=\""
                    + ns
                    + "\">"
                    + "<soapenv:Body></soapenv:Body></soapenv:Envelope>");
        urlCon.setAuthenticationDummyMethod(urlCon.getRequestMethod());
    }

}
