//
// Copyright 2002-2007, oakland software, all rights reserved.
//
// May not be used or redistributed without specific written
// permission from oakland software.
//

package com.oaklandsw.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.util.Map;

import org.apache.commons.logging.Log;

import com.oaklandsw.http.cookie.MalformedCookieException;
import com.oaklandsw.util.LogUtils;
import com.oaklandsw.util.URIUtil;
import com.oaklandsw.util.Util;

/**
 * This class provides the internal implementation of the URL connection.
 */
public class HttpURLConnectInternal
    extends
        com.oaklandsw.http.HttpURLConnection
{
    private static final Log _log = LogUtils.makeLogger();

    /**
     * @see java.net.HttpURLConnection#HttpURLConnection(URL)
     */
    public HttpURLConnectInternal(URL urlParam)
    {
        super(urlParam);
        _thread = Thread.currentThread();
    }

    // For testing and tunnelling
    public HttpURLConnectInternal()
    {
    }

    /** My request path + query string. */
    private String        _pathQuery;

    /** Whether or not the request body has been sent. */
    private boolean       _bodySent;

    /**
     * The host/port to be sent on this if this is a connect request.
     */
    private String        _connectHostPort;

    /**
     * The input stream from which to read the request body. This is set before
     * the request is issued, if the source of the data is a stream.
     */
    private InputStream   _requestBody;

    /**
     * The byte array of the request. If the request source is a byte array,
     * this is set. When the request is first written, the contents of the
     * request is saved here in case it needs to be resent for authentication.
     */
    private byte[]        _requestBytes;

    private boolean       _responseBodySetup;

    /**
     * Indicates that we are sending an NTLM negotiate message on this URL
     * request and the underlying connection (when obtained) should be marked
     * with the setNtlmLeaveOpen() flag.
     */
    // private boolean _ntlmAuth;
    // Header values we are interested in
    // NOTE: be sure to change resetBeforeRead if anything is added
    // to this list
    protected String      _hdrContentLength;
    protected String      _hdrConnection;
    protected String      _hdrProxyConnection;
    protected String      _hdrTransferEncoding;
    protected String      _hdrWWWAuth;
    protected String      _hdrLocation;
    protected String      _hdrProxyAuth;
    protected int         _hdrContentLengthInt;

    // We have read the first character of the next line after
    // the status line, and it is here. This only applies if
    // _singleEolChar is true
    protected int         _savedAfterStatusNextChar;

    protected boolean     _singleEolChar;

    // Either AUTH_NORMAL or AUTH_PROXY, this is set when we get a 401 or a
    // 407, -1 indicates we have not started authentication so we don't know
    protected int         _currentAuthType     = -1;

    // The credentials that were sent in the authentication. We have
    // to have them both here and in the HTTPConnection. They are here
    // because NTLM authentication closes the connection during
    // authentication and we thus have no where to get the credential
    // information that was sent.
    UserCredential[]      _credential          = new UserCredential[AUTH_PROXY + 1];

    // The authentication protocol associated with the above credential
    // This is how this urlcon is authenticated
    int[]                 _authProtocol        = new int[AUTH_PROXY + 1];

    // The values for _authState

    // No authentication is required for this urlcon
    static final int      AS_NONE              = 0;

    // We know authentication is required and have not yet done anything
    static final int      AS_NEEDS_AUTH        = 1;

    // We have sent the initial authentication reponse to the server
    static final int      AS_INITIAL_AUTH_SENT = 2;

    // We have sent the final authentication response to the server
    // (perhaps preemtively in the case of basic)
    static final int      AS_FINAL_AUTH_SENT   = 3;

    // We have received an OK status after authentication, we are authenticated
    static final int      AS_AUTHENTICATED     = 4;

    int[]                 _authState           = new int[AUTH_PROXY + 1];

    // True if we are reading from the connection on the pipeline
    // thread, this is used only if authentication is required, then we
    // want to process the authentication in place and then exit as if
    // we had done a write
    protected boolean     _pipelinedReading;

    // Thread on which the connection was created (used for pipelining)
    protected Thread      _thread;

    // Used for testing
    protected static int  _diagSequence;
    public static boolean _addDiagSequence;

    /**
     * Maximum number of redirects to forward through.
     */
    private int           MAX_FORWARDS         = 100;

    public Log getLog()
    {
        return _log;
    }

    public final String getName()
    {
        return method;
    }

    static String authStateToString(int authState)
    {
        switch (authState)
        {
            case AS_NONE:
                return "AS_NONE";
            case AS_NEEDS_AUTH:
                return "AS_NEEDS_AUTH";
                // case AS_INITIAL_AUTH_RECD:
                // return "AS_INITIAL_AUTH_RECD";
            case AS_INITIAL_AUTH_SENT:
                return "AS_INITIAL_AUTH_SENT";
                // case AS_FINAL_AUTH_RECD:
                // return "AS_FINAL_AUTH_RECD";
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

        if (_connection != null)
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
        _respFooters.add(key, value);
    }

    protected final void addRequestHeaders() throws IOException
    {
        // For testing, works with ParamServlet
        if (_addDiagSequence)
        {
            synchronized (getClass())
            {
                _reqHeaders.remove("Sequence");
                _reqHeaders.add("Sequence", Integer.toString(_diagSequence));
            }
        }

        // Use add() instead of set() in this method cuz its faster
        // if we know the header is not present

        if (_reqHeaders.get(HDR_USER_AGENT) == null)
            _reqHeaders.add(HDR_USER_AGENT, USER_AGENT);

        // Per 19.6.1.1 of RFC 2616, it is legal for HTTP/1.0 based
        // applications to send the Host request-header.
        // TODO: Add the ability to disable the sending of this header for
        // HTTP/1.0 requests.

        if (_reqHeaders.get(HDR_HOST) == null)
        {
            _log.debug("Adding Host request header");
            _reqHeaders.add(HDR_HOST, _connection._hostPortURL);

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
                    _reqHeaders.add(HDR_COOKIE, s);
                }
                else
                {
                    // In non-strict mode put each cookie on a separate header
                    for (int i = 0; i < cookies.length; i++)
                    {
                        String s = _cookieSpec.formatCookie(cookies[i]);
                        _reqHeaders.add(HDR_COOKIE, s);
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
                + _reqHeaders.get(HDR_CONTENT_LENGTH));
        }

        if (_streamingChunked)
        {
            if (_reqHeaders.get(HDR_TRANSFER_ENCODING) == null)
                _reqHeaders.add(HDR_TRANSFER_ENCODING, HDR_VALUE_CHUNKED);
        }
        else
        {
            if (!_hasContentLengthHeader)
            {
                if ((_methodProperties & METHOD_PROP_ADD_CL_HEADER) != 0)
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

                    _reqHeaders.add(HDR_CONTENT_LENGTH, String.valueOf(len));
                    _hasContentLengthHeader = true;
                }
            }

        }

        // Add headers for HTTP 1.0 support
        if (_use10KeepAlive)
        {
            if (_connection.isProxied())
            {
                if (_reqHeaders.get(HDR_PROXY_CONNECTION) == null)
                {
                    _log
                            .debug("Adding 1.0 Proxy_Connection: Keep-Alive request header");
                    _reqHeaders.add(HDR_PROXY_CONNECTION, HDR_VALUE_KEEP_ALIVE);
                }
            }
            else
            {
                if (_reqHeaders.get(HDR_CONNECTION) == null)
                {
                    _log
                            .debug("Adding 1.0 Connection: Keep-Alive request header");
                    _reqHeaders.add(HDR_CONNECTION, HDR_VALUE_KEEP_ALIVE);
                }
            }
        }

    }

    protected final void resetBeforeRead()
    {
        _hdrContentLength = null;
        _hdrContentLengthInt = 0;
        _hdrConnection = null;
        _hdrProxyConnection = null;
        _hdrTransferEncoding = null;
        _hdrWWWAuth = null;
        _hdrProxyAuth = null;
        _hdrLocation = null;
        _responseIsEmpty = false;
        _responseBodySetup = false;
        _responseHeadersRead = false;
        _responseStream = null;
        _responseBytes = null;
        _savedAfterStatusNextChar = 0;
        _singleEolChar = false;
    }

    protected final void readResponse() throws IOException
    {
        _connection.startReadBugCheck();
        readStatusLine();
        readResponseHeaders();
        isResponseEmpty();
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

    private final void readClosedPartial() throws SocketException
    {
        // This can be retried
        throw new SocketException("Connection closed in the middle of sending reply "
            + "(try #"
            + _tryCount
            + ")");
    }

    private final int skipSpaces() throws IOException
    {
        int ch = 0;
        do
        {
            ch = _conInStream.read();
            if (ch < 0)
                readClosedPartial();
            // System.out.println("sp: " + String.valueOf((char)ch));
        }
        while (ch == ' ');
        return ch;
    }

    protected final void readStatusLine() throws IOException
    {
        _responseCode = 0;

        // Read HTTP/1.
        char[] httpHdr = { 'H', 'T', 'T', 'P', '/', '1', '.' };
        int httpInd = 0;
        int ch = -1;
        while (httpInd < httpHdr.length)
        {
            try
            {
                ch = _conInStream.read();
            }
            catch (SocketException sex)
            {
                readClosed();
                // throws
            }

            if (ch < 0)
            {
                readClosed();
            }
            // System.out.println("hdr: " + String.valueOf((char)ch));

            // Start looking at the beginning again
            if (ch != httpHdr[httpInd])
                httpInd = 0;
            else
                httpInd++;
        }

        // Check the number following the point
        ch = _conInStream.read();
        if (ch < 0)
            readClosedPartial();
        // System.out.println("hdr1: " + String.valueOf((char)ch));
        if (ch == '1')
            _http11 = true;
        else if (ch == '0')
            _http11 = false;
        else
        {
            // This is not recoverable
            throw new HttpException("Unrecognized server protocol version");
        }

        ch = skipSpaces();

        // Read response code
        int responseMult = 100;
        do
        {
            // System.out.println("resp: " + String.valueOf((byte)ch));
            _responseCode += (ch - '0') * responseMult;
            responseMult /= 10;
            // System.out.println("resp code: " + _responseCode);
            ch = _conInStream.read();
            if (ch < 0)
                readClosedPartial();
        }
        while (ch != ' ' && ch != '\r' && ch != '\n');

        if (_responseCode < 100 || _responseCode > 999)
        {
            throw new HttpException("Invalid response code: " + _responseCode);
        }

        if (ch != '\r' && ch != '\n')
            ch = skipSpaces();

        _responseTextLength = 0;

        // We have some message
        if (ch != '\r' && ch != '\n')
        {
            // Read response text - up to the max size that we care about
            do
            {
                if (_responseTextLength < MAX_RESPONSE_TEXT)
                    _responseText[_responseTextLength++] = (char)ch;
                ch = _conInStream.read();
                // System.out.println("msg: 0x" + Integer.toHexString((int)ch));
                if (ch < 0)
                    readClosedPartial();
            }
            while (ch != '\r' && ch != '\n');
        }

        // At this point we have read either /r or /n, try to read
        // the next EOL character which should be either /r or /n.
        // In the case where only a single EOL character is used,
        // this is where we detect this (by the fact that the next
        // character is not /r or /n); we save that character because
        // that must be the result of the next read.

        ch = _conInStream.read();
        if (ch < 0)
            readClosedPartial();

        // System.out.println("Response text length: " + _responseTextLength);

        // This means there is a single EOL char
        if (ch != '\n' && ch != '\r')
        {
            _savedAfterStatusNextChar = ch;
            _singleEolChar = true;
        }

    }

    // This is called by the Headers class when parsing each header
    // for every header value it sees
    final void getHeadersWeNeed(String name, String value)
    {
        char nameChar = Character.toUpperCase(name.charAt(0));
        switch (nameChar)
        {
            case 'C':
                if (name.equalsIgnoreCase(HDR_CONTENT_LENGTH))
                    _hdrContentLength = value;
                else if (name.equalsIgnoreCase(HDR_CONNECTION))
                    _hdrConnection = value;
                break;

            case 'L':
                if (name.equalsIgnoreCase(HDR_LOCATION))
                    _hdrLocation = value;
                break;

            case 'P':
                if (name
                        .equalsIgnoreCase(Authenticator.REQ_HEADERS[AUTH_PROXY]))
                    _hdrProxyAuth = value;
                else if (name.equalsIgnoreCase(HDR_PROXY_CONNECTION))
                    _hdrProxyConnection = value;
                break;
            case 'S':
                if (_cookieContainer != null)
                {
                    // Take either type of cookie header
                    if (!name.equalsIgnoreCase("set-cookie")
                        && !name.equalsIgnoreCase("set-cookie2"))
                        break;

                    String host = _connection._host;
                    int port = _connection.getPort();
                    boolean isSecure = _connection.isSecure();
                    Cookie[] cookies = null;
                    try
                    {
                        cookies = _cookieSpec.parse(host,
                                                    port,
                                                    getPath(),
                                                    isSecure,
                                                    value);
                    }
                    catch (MalformedCookieException e)
                    {
                        if (_log.isWarnEnabled())
                        {
                            _log.warn("Invalid cookie header: \""
                                + value
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
                break;
            case 'T':
                if (name.equalsIgnoreCase(HDR_TRANSFER_ENCODING))
                    _hdrTransferEncoding = value;
                break;
            case 'W':
                if (name
                        .equalsIgnoreCase(Authenticator.REQ_HEADERS[AUTH_NORMAL]))
                    _hdrWWWAuth = value;
                break;
        }

    }

    protected final void readResponseHeaders() throws IOException
    {
        _respHeaders.clear();
        _respHeaders.read(_conInStream,
                          this,
                          _singleEolChar,
                          _savedAfterStatusNextChar);
        _responseHeadersRead = true;
    }

    protected final void isResponseEmpty() throws IOException
    {
        // Does not have a response body, even though the headers suggest
        // that it does
        if ((_methodProperties & METHOD_PROP_IGNORE_RESPONSE_BODY) != 0)
        {
            _responseIsEmpty = true;
            return;
        }

        if (null != _hdrContentLength)
        {
            try
            {
                _hdrContentLengthInt = Integer.parseInt(_hdrContentLength);
                // Don't bother allocating a stream if there is no data
                if (_hdrContentLengthInt == 0)
                {
                    _responseIsEmpty = true;
                }
                else if (_responseCode == HttpStatus.SC_NO_CONTENT
                    || _responseCode == HttpStatus.SC_NOT_MODIFIED)
                {
                    _log.warn("Response code 204/304 sent and non-zero "
                        + "Content-Length was specified - ignoring");
                    _responseIsEmpty = true;
                }
            }
            catch (NumberFormatException e)
            {
                _log.warn("Invalid Content-Length response value read: "
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
        if (_responseIsEmpty || _responseBodySetup)
            return;
        _responseBodySetup = true;

        // In case there have been retries
        _responseStream = null;
        _responseBytes = null;

        boolean shouldClose = shouldCloseConnection();
        InputStream result = null;

        // We use Transfer-Encoding if present and ignore Content-Length.
        // RFC2616, 4.4 item number 3
        if (null != _hdrTransferEncoding)
        {
            if ("chunked".equalsIgnoreCase(_hdrTransferEncoding))
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
                                                     shouldClose);
            // Don't wrap this in the auto close stream since it will
            // already handle that
            return;
        }

        if (result == null)
            return;

        // This closes the connection when the end of the data is reached
        if (shouldClose)
            result = new AutoCloseInputStream(result, this);

        _responseStream = result;
    }

    protected final void writeRequest() throws IOException
    {
        _log.debug("writeRequest");
        writeRequestLine();
        writeRequestHeaders();
        _conOutStream.write(Util.CRLF_BYTES);
        _conOutStream.flush();

        // Resets all of the read state so we are not looking
        // at stale state while processing the write
        resetBeforeRead();

        // We don't write the body in streaming mode, the user does
        // that
        if (isStreaming())
        {
            _log.debug("writeRequest - streaming request header sent");
            _requestSent = true;
            return;
        }

        _bodySent = writeRequestBody();
    }

    protected final void writeRequestLine() throws IOException
    {
        StringBuffer buf = new StringBuffer();

        buf.append(method);
        buf.append(" ");

        if ((_methodProperties & METHOD_PROP_REQ_LINE_URL) != 0)
        {
            if (_connection.isProxied() && !_connection.isTransparent())
            {
                if (_connection.isSecure())
                    buf.append("https://");
                else
                    buf.append("http://");
                buf.append(_connection._hostPortURL);
            }
            buf.append(_pathQuery);

            // For testing, works with ParamServlet
            if (_addDiagSequence)
            {
                synchronized (getClass())
                {
                    buf.append("&Sequence=" + (++_diagSequence));
                }
            }
        }
        else if ((_methodProperties & METHOD_PROP_REQ_LINE_STAR) != 0)
        {
            buf.append("*");
        }
        else if ((_methodProperties & METHOD_PROP_REQ_LINE_HOST_PORT) != 0)
        {
            // Put the host/port junk on the front if required
            buf.append(_connection._hostPort);
        }

        buf.append(_http11 ? " HTTP/1.1\r\n" : " HTTP/1.0\r\n");

        String reqLine = buf.toString();
        _conOutStream.write(reqLine.getBytes("ASCII"), 0, reqLine.length());
    }

    protected final void writeRequestHeaders() throws IOException
    {
        addRequestHeaders();
        _reqHeaders.write(_conOutStream);
    }

    protected final boolean writeRequestBody() throws IOException
    {
        // Nothing to write
        if (_contentLength == 0)
            return true;

        if (_expectContinue && _responseCode != HttpStatus.SC_CONTINUE)
        {
            return false;
        }

        // We already have the bytes
        if (_requestBytes != null)
        {
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
            _conOutStream.flush();
            return true;
        }

        // We have to read from the stream, this will be the typical
        // case the first time
        InputStream inputStream = null;

        if (_requestBody != null)
            inputStream = _requestBody;
        else
            return true;

        int total;

        if (_log.isDebugEnabled())
            _log.debug("should write (Content-Length): " + _contentLength);

        // The content length may be smaller than the size of
        // the data
        _requestBytes = new byte[_contentLength];
        total = Util.copyStreams(inputStream,
                                 _conOutStream,
                                 _requestBytes,
                                 _contentLength);

        if (_log.isDebugEnabled())
            _log.debug("wrote: " + total);

        inputStream.close();
        _conOutStream.flush();

        if (total != _contentLength)
        {
            throw new HttpException("Data length ("
                + total
                + ") does not match specified "
                + "Content-Length of ("
                + _contentLength
                + ")");
        }

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
                _log.warn("Received status CONTINUE but the body has already "
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
                if (isReleasedAfterExecute())
                    readEntireResponse();

                if (_responseCode >= 300 && readSeparateFromWrite())
                {
                    // Doing a pipelined read, we don't own the connection and
                    // got a retryable
                    // response
                    if (_pipelinedReading
                        && (_responseCode <= 399
                            || _responseCode == HttpStatus.SC_UNAUTHORIZED || _responseCode == HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED))
                    {
                        String str = "Unexpected "
                            + _responseCode
                            + " during pipelined read on: "
                            + this;
                        _log.debug(str);

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
                        throw new HttpException(str);
                    }
                    break;
                }
            }

            // if SC_CONTINUE write the request body
            writeRemainingRequestBody();

            // Reset everything before we read again, the first
            // resetBeforeRead() is called after we write the request
            if (_responseCode < HttpStatus.SC_OK)
                resetBeforeRead();
        }
        while (_responseCode < HttpStatus.SC_OK);

        if (_responseCode >= HttpStatus.SC_OK)
        {
            // Completely good response, record the statistics
            if (_responseCode < 300)
            {
                _connManager.recordCount(HttpConnectionManager.COUNT_SUCCESS);
                if (_connection != null)
                    _connection._totalResUrlConCount++;
            }

            // Good and redirect, the authentication process is complete
            if (_responseCode < 400 && _currentAuthType >= 0)
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
        throws HttpException
    {
        if (_forwardAuthCount == 1
            || _authState[normalOrProxy] == AS_FINAL_AUTH_SENT)
        {
            if (_log.isDebugEnabled())
            {
                _log.debug("processPreemptiveAuth - "
                    + normalOrProxyToString(normalOrProxy)
                    + " checking for preemptive authentication");
            }

            boolean sent = Authenticator.authenticate(this,
                                                      null,
                                                      null,
                                                      null,
                                                      normalOrProxy);

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

                        if (_retryInterval != 0)
                        {
                            try
                            {
                                if (_log.isDebugEnabled())
                                {
                                    _log.debug("Before retry sleep: "
                                        + _retryInterval);
                                }
                                Thread.sleep(_retryInterval);
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
                    if ((_methodProperties & METHOD_PROP_RETRY) == 0)
                    {
                        _connection.checkConnection();
                        // Don't allow any retries beyond this point
                        // for a POST as its not idempotent.
                        _maxTries = 0;
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
                        _log.debug(str);
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

                        // Make sure it's still open at this point, can't close
                        // until end prevent close
                        if (false && !_connection.isOpen())
                        {
                            throw new IOException("Connection: "
                                + _connection
                                + " closed");
                        }
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
            catch (InterruptedIOException ioiex)
            {
                // Handled at a higher level
                throw ioiex;
            }
            catch (HttpException httpe)
            {
                // This is non-recoverable, throw it up to the user
                _log.info("HttpException when writing "
                    + "request/reading response: ", httpe);
                // Handled at a higher level
                throw httpe;
            }
            catch (IOException ioe)
            {
                if (_log.isDebugEnabled())
                {
                    _log.debug("IOException when writing "
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
                    _log.debug("Retrying pipelining before request sent");
                    // fall through and retry
                }
                else if (readSeparateFromWrite())
                {
                    if (_log.isWarnEnabled())
                    {
                        _log.warn("Retry not allowed for a streaming urlcon: "
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
        }
        while (true);

    }

    /**
     * Reads the entire response from the responseInputStream so that we can
     * release the connection. Used only when _explicitClose is not specified.
     */
    private final void readEntireResponse() throws IOException
    {
        _log.debug("readEntireResponse");
        setupResponseBody();

        if (_log.isDebugEnabled())
        {
            _log.debug("responseStream: "
                + (_responseStream == null ? "null" : _responseStream
                        .getClass().getName().toString()));
        }

        InputStream is = _responseStream;
        if (is == null)
        {
            _responseBytes = new byte[0];
            return;
        }

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Util.copyStreams(is, os);
        _responseBytes = os.toByteArray();
    }

    // Returns true if retry needed
    private boolean processRedirectResponse()
    {
        _log.debug("Redirect required");

        if (!getInstanceFollowRedirects())
        {
            _log.info("Redirect requested but "
                + "followRedirects is "
                + "disabled");
            return false;
        }

        if (_hdrLocation == null)
        {
            // got a redirect response, but no location header
            _log.error("Received redirect response "
                + _responseCode
                + " but no location header");
            return false;
        }

        if (_log.isDebugEnabled())
        {
            _log.debug("Redirect requested to location '" + _hdrLocation + "'");
        }

        // rfc2616 demands the location value be a complete URI
        // Location = "Location" ":" absoluteURI
        URL u = null; // the new url
        try
        {
            u = new URL(_hdrLocation);
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
                u = new URL(currentUrl, _hdrLocation);
            }
            catch (Exception ex)
            {
                _log.error("Redirected location '"
                    + _hdrLocation
                    + "' is malformed");
                return false;
            }
        }

        // Get rid of the hold Host header since we are
        // going somewhere else
        _reqHeaders.remove(HDR_HOST);
        _urlString = u.toExternalForm();

        // change the path and query string to the redirect
        String newPathQuery = URIUtil.getPathQuery(u.toExternalForm());
        if (_log.isDebugEnabled())
        {
            _log.debug("Changing path/query from \""
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

    // Returns true if retry needed
    private boolean processAuthenticationResponse() throws HttpException
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
        _reqHeaders.remove(Authenticator.RESP_HEADERS[_currentAuthType]);
        try
        {
            String reqType = Authenticator.REQ_HEADERS[_currentAuthType];

            // parse the authenticate header
            Map challengeMap = Authenticator
                    .parseAuthenticateHeader(_respHeaders, reqType, this);

            authenticated = Authenticator.authenticate(this,
                                                       _respHeaders,
                                                       reqType,
                                                       challengeMap,
                                                       _currentAuthType);
        }
        catch (HttpException ex)
        {
            // Don't show this to the user, just indicate that we are
            // authenticated
            _log.warn("Exception processing authentication response: ", ex);
            return false;
        }

        if (!authenticated)
        {
            // won't be able to authenticate to this challenge
            // without additional information
            _log.debug("Server demands "
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
        if (_responseCode > 300)
        {
            HttpRetryException rte = new HttpRetryException(new String(_responseText,
                                                                       0,
                                                                       _responseTextLength),
                                                            _responseCode);
            rte._location = _hdrLocation;
            if (_log.isDebugEnabled())
            {
                _log.debug("Streaming/pipelining non-OK "
                    + "response, throwing", rte);
            }
            throw rte;
        }
        _log.debug("Streaming/pipelining - returning");
    }

    protected final void processRequestLoop() throws IOException
    {
        // Loop for redirection and authentication retries
        while (_forwardAuthCount++ < MAX_FORWARDS)
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
                        && (isStreaming() || (_pipeliningOptions & PIPE_PIPELINE) != 0))
                    {
                        _log.warn("setAuthenticationType() specified with "
                            + "pipelining/streaming, but no authorization "
                            + "is required - retrying");

                        // Set up the response body so it can be flushed
                        // below, and indicate it's not setup so that the
                        // next time around it will setup again
                        setupResponseBody();
                        _responseBodySetup = false;

                        // Go around again
                        break;
                    }

                    // Fall through

                default:
                    // Neither an unauthorized nor a redirect response,
                    // no retry possible, or this is just normal
                    return;

            }

            if (shouldCloseConnection())
            {
                closeConnection();
            }
            else
            {
                if (_connection.isOpen())
                {
                    // Throw away body to position the stream after the response
                    discardResponseBody();
                }

                // For redirect, force getting a new connection, for auth
                // we way on the same connection
                if (redirect)
                    releaseConnection(NORMAL);
            }

        }

        _log.error("Giving up after "
            + MAX_FORWARDS
            + " forwards/authentication restarts");
        throw new HttpException("Maximum redirects/authentication restarts ("
            + MAX_FORWARDS
            + ") exceeded for "
            + this);

    }

    // Called by execute() and also called in getOutputStream() in the streaming
    // case, this starts the request and leaves it executing.
    protected final void executeStart() throws IOException
    {
        if (_log.isDebugEnabled())
        {
            _log.debug("executeStart "
                + method
                + " ExplicitClose: "
                + _explicitClose);
        }

        if (_executed)
            return;

        // Default is GET
        if ((_methodProperties & METHOD_PROP_UNSPECIFIED_METHOD) != 0)
        {
            _log.debug("Method not specified, setting to GET");
            setRequestMethodInternal(HTTP_METHOD_GET);
        }

        try
        {
            _executing = true;
            processRequestLoop();
        }
        catch (InterruptedIOException ioiex)
        {
            // Timeout
            _log.info("Timeout when reading response: ", ioiex);
            releaseConnection(CLOSE);
            _dead = true;
            throw new HttpTimeoutException();
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
            _log.error("Unexpected exception processing request ", re);
            releaseConnection(CLOSE);
            _dead = true;
            throw re;
        }
        catch (Error e)
        {
            _log.error("Unexpected error processing request ", e);
            releaseConnection(CLOSE);
            _dead = true;
            throw e;
        }

    }

    // Executes the entire HTTP request and response, used for the
    // non-streaming case
    protected final void execute() throws IOException
    {
        boolean streaming = isStreaming();

        // Handle the methods that allow data
        if (!streaming
            && _outStream != null
            && ((_methodProperties & METHOD_PROP_CALCULATE_CONTENT_LEN) != 0))
        {
            byte[] outBytes = ((ByteArrayOutputStream)_outStream).toByteArray();
            setRequestBody(outBytes);
            if (_contentLength == UNINIT_CONTENT_LENGTH)
                _contentLength = outBytes.length;

            // TODO - in JRE 1.5 they removed this default, look this up
            // to see what to do about this, removing this breaks the IIS
            // tests for example
            if ((_methodProperties & METHOD_PROP_SEND_CONTENT_TYPE) != 0)
            {
                if (_reqHeaders.get(HDR_CONTENT_TYPE) == null)
                {
                    _reqHeaders.set(HDR_CONTENT_TYPE,
                                    "application/x-www-form-urlencoded");
                }
            }
        }

        try
        {
            if (streaming && !_streamingWritingFinished)
            {
                streamWriteFinished(!OK);
                String msg = "Cannot execute the HTTP request "
                    + "until the output stream is closed.";
                IllegalStateException ex = new IllegalStateException(msg);
                _log.debug("execute: ", ex);
                throw ex;
            }

            executeStart();
        }
        finally
        {
            _log.debug("execute - finally");

            _executing = false;
            _executed = true;

            // The connection is released in the explicit close case
            // when the stream is released
            if (_responseIsEmpty || isReleasedAfterExecute())
                releaseConnection(NORMAL);
        }

    }

    // Does the write for a pipelined connection. This can happen
    // on any thread, but is synchronized because it must obtain
    // the connection exclusively from the connection manager
    protected void processPipelinedWrite() throws InterruptedException
    {
        if (_log.isDebugEnabled())
        {
            _log.debug("processPipelinedWrite " + this);
        }

        // We want direct access to the output stream
        // FIXME - this is set for everything, is that fair?
        HttpURLConnection.setExplicitClose(true);

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

                    // Make sure it's still open at this point, can't close
                    // until end prevent close
                    if (!connection.isOpen())
                    {
                        throw new IOException("Connection: "
                            + _connection
                            + " closed");
                    }

                    _callback.writeRequest(this, os);
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
        catch (Exception ex)
        {
            // Some problem writing, pass this to the user, we have
            // already retried if possible
            if (_log.isDebugEnabled())
            {
                _log
                        .debug("processPipelineWrite "
                            + "exception writing: "
                            + ex);
            }

            // We got so far as to get a connection, we need to indicate
            // we are no longer using that connection
            if (_connection != null)
                _connection.adjustPipelinedUrlConCount(-1);

            if (ex instanceof AutomaticHttpRetryException)
            {
                if (_log.isDebugEnabled())
                {
                    _log.debug("processPipelinedWrite "
                        + "AutomaticHttpRetryException thrown while writing "
                        + "Callback - attempting retry ", ex);
                }

                // Returns null if we can retry
                IOException ioex = (IOException)attemptPipelinedRetry(_ioException);
                if (ioex == null)
                    return;
                ex = ioex;
                // Continue
            }

            // We are done with this urlcon
            if (_log.isDebugEnabled())
                _log.debug("urlConWasRead (write error): " + this);
            _connManager.urlConWasRead(_thread);

            // Something is really wrong, blow out of here, so the main
            // caller sees the problem
            if (ex instanceof RuntimeException)
            {
                _log.warn("Unexpected RuntimeException, not calling callback: "
                    + this, ex);
                throw (RuntimeException)ex;
            }

            // Tell the user what went wrong
            _callback.error(this, null, ex);
            return;
        }
    }

    // Process the pipelined read, this is executed on the HttpConnectionThread,
    // that guarantees that each connection is accessed by only one thread for
    // reading.
    protected void processPipelinedRead() throws InterruptedException
    {
        Exception ex = null;
        InputStream is = null;

        // Save the connection going in, the connection might change
        // in the event of initiating a retry, and we need to know to
        // reduce the count on the original connection
        HttpConnection connection = _connection;

        boolean readSuccessful = false;

        // Make sure we have only one read at a time for the connection,
        // since enqueing work happens in the middle of writing (if we
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

                // Don't allow a close while we have the input stream
                connection.startPreventClose();

                // Gets or blocks for the connection, could throw
                // if there is a problem
                try
                {
                    _pipelinedReading = true;

                    connection.startReadBugCheck();

                    // Do this check inside of the startPreventClose() block
                    // (above)
                    // the connection cannot be closed by another thread after
                    // that
                    // point, but we want to make sure it is still open before
                    // we
                    // try to read it
                    if (!connection.isOpen())
                        throw new IOException("Connection is closed when reading response");

                    if (!_executed)
                        execute();

                    // We have written authentication, now return so the
                    // response may be read on the new connection
                    if (_currentAuthType >= 0
                        && _authState[_currentAuthType] == AS_FINAL_AUTH_SENT)
                    {
                        _requestSent = true;

                        if (_log.isDebugEnabled())
                        {
                            _log.debug("processPipelinedRead "
                                + "unexpected authentication - "
                                + "wrote authentication and returning");
                        }
                        // Will retry
                        return;
                    }

                    // Gets the input stream if we actually read
                    is = getResponseStream();
                }
                catch (IOException iex)
                {
                    // We are not reading during a retry
                    _pipelinedReading = false;

                    // Returns the exception to use, or null if we can retry
                    ex = attemptPipelinedRetry(iex);
                    if (ex == null)
                        return;
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
                        _log.debug("processPipelinedRead: EXCEPTION", ex);
                }

                Exception saveExc = _ioException;
                try
                {
                    // Call the user
                    if (ex != null
                        || _responseCode >= HttpStatus.SC_REDIRECTION)
                        _callback.error(this, is, ex);
                    else
                        _callback.readResponse(this, is);

                    // Inside of the call to the user a stream got closed and
                    // caused an exception reading, it will have thrown an
                    // AutomaticHttpRetryException to the user, if this happens
                    // then do the retry
                    if (_ioException instanceof AutomaticHttpRetryException)
                    {
                        if (_log.isDebugEnabled())
                        {
                            _log.debug("processPipelinedRead "
                                + "AutomaticHttpRetryException thrown in "
                                + "Callback - attempting retry ", _ioException);
                        }

                        // Returns null if we can retry
                        IOException ioex = (IOException)attemptPipelinedRetry(_ioException);
                        if (ioex == null)
                            return;
                        _ioException = ioex;
                    }

                }
                catch (Throwable t)
                {
                    // Ignore this
                    _log.error("Exception thrown out of "
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
                    if (_log.isWarnEnabled())
                    {
                        _log.warn("Exception during close of InputStream for: "
                            + this
                            + " conn: "
                            + _connection, iex);
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
                        _log.debug("urlConWasRead (read success): " + this);
                    _connManager.urlConWasRead(_thread);
                }
            }
        }
    }

    // Returns null if retry is possible, otherwise returns the exception to
    // throw
    protected Exception attemptPipelinedRetry(IOException iex)
        throws InterruptedException
    {
        // If allowed, give it another go
        if (_tryCount < _maxTries)
        {
            if (_log.isDebugEnabled())
            {
                _log.debug("pipelined RETRY (exception) " + this, iex);
            }
            // System.out.println("processPipelineRead RETRY " + this);
            // FIXME - make this cleanup a little more sane
            _dead = false;
            _requestSent = false;
            _executed = false;
            _ioException = null;
            _connManager.recordRetry(_connManager._pipelineReadRetryCounts,
                                     _tryCount);
            processPipelinedWrite();
            if (_log.isDebugEnabled())
            {
                _log.debug("pipelined FINISHED RETRY " + this);
            }
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

        _log.warn(str);

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
    // was a problem.

    static final boolean OK = true;

    void streamWriteFinished(boolean ok)
    {
        if (ok)
        {
            _log.debug("streaming write finished");
            _streamingWritingFinished = true;
            if ((_pipeliningOptions & PIPE_PIPELINE) != 0)
            {
                try
                {
                    _connManager.enqueueWorkForConnection(this);
                }
                catch (InterruptedException e)
                {
                    _dead = true;
                    _log.warn("Thread interrupted", e);
                    return;
                }
            }
        }
        else
        {
            _log.debug("streaming write failed");
            // Release and close the connection since there is something wrong
            releaseConnection(CLOSE);
            _dead = true;
        }
    }

    // The response is read in a separate call from the write, either due
    // to pipelining or streaming
    protected boolean readSeparateFromWrite()
    {
        return (isStreaming() || (_pipeliningOptions & PIPE_PIPELINE) != 0)
            && (_currentAuthType == -1
                || _authState[_currentAuthType] == AS_NONE || _authState[_currentAuthType] >= AS_FINAL_AUTH_SENT);
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

    protected final boolean isReleasedAfterExecute()
    {
        // On any kind of error condition, we release the connection (and
        // therefore don't require an explicit close)
        boolean result;
        if (!_explicitClose || _responseCode >= 300)
            result = true;
        else
            result = false;
        if (_log.isDebugEnabled())
            _log.debug("isReleasedAfterExecute: " + result);
        return result;
    }

    private final void closeConnection()
    {
        if (shouldCloseConnection())
        {
            releaseConnection(CLOSE);
        }
    }

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

        if ((null != _hdrConnection && HDR_VALUE_CLOSE
                .equalsIgnoreCase(_hdrConnection))
            || (_hdrProxyConnection != null && HDR_VALUE_CLOSE
                    .equalsIgnoreCase(_hdrProxyConnection)))
        {
            _log.debug("Will CLOSE - "
                + "\"[Proxy-]Connection: close\" header found.");

            if (_connection != null)
            {
                if (false)
                {
                    System.out.println("Setting max url cons at close: "
                        + _connection._totalResUrlConCount);
                }
                // Record the number of urlcons allowed before this got closed
                _connection._connectionInfo._observedMaxUrlCons = _connection._totalResUrlConCount;
            }

            result = true;
        }

        // Still might stay open and this is HTTP 1.0
        if (!result && !_http11)
        {
            if (null != _hdrConnection
                && HDR_VALUE_KEEP_ALIVE.equalsIgnoreCase(_hdrConnection))
            {
                _log.debug("HTTP/1.0 - Leave OPEN - Keep-Alive");
                result = false;
            }
            else if (null != _hdrProxyConnection
                && _connection != null
                && _connection.isProxied()
                && HDR_VALUE_KEEP_ALIVE.equalsIgnoreCase(_hdrProxyConnection))
            {
                _log.debug("HTTP/1.0 - Leave OPEN - Proxy Keep-Alive");
                result = false;
            }
            else if (((_methodProperties & METHOD_PROP_LEAVE_OPEN) != 0)
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
                _log.debug("HTTP/1.0 - Will CLOSE connection");
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
    protected final void discardResponseBody() throws IOException
    {
        InputStream is = _responseStream;
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

        _log.info("Discarding response body - flushed " + len + " bytes ");
    }

}
