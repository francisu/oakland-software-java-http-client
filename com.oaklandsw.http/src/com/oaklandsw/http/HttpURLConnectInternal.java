//
// Copyright 2002-3003, oakland software, all rights reserved.
//
// May not be used or redistributed without specific written
// permission from oakland software.
//

package com.oaklandsw.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.util.Map;

import org.apache.commons.logging.Log;

import com.oaklandsw.http.cookie.MalformedCookieException;
import com.oaklandsw.util.URIUtil;
import com.oaklandsw.util.Util;

/**
 * This class provides the internal implementation of the URL connection.
 */
public class HttpURLConnectInternal
    extends
        com.oaklandsw.http.HttpURLConnection
{

    /**
     * @see java.net.HttpURLConnection#HttpURLConnection(URL)
     */
    public HttpURLConnectInternal(URL urlParam)
    {
        super(urlParam);
    }

    // For testing and tunnelling
    public HttpURLConnectInternal()
    {
    }

    /** My request path + query string. */
    private String      _pathQuery;

    /** Whether or not the request body has been sent. */
    private boolean     _bodySent;

    /**
     * The credential that was most recently sent to try to authenticate this
     * request. If we get another request for authentication and we had
     * previously sent a credential, then it's the wrong credential.
     */
    private Credential  _credentialSent;

    private Credential  _proxyCredentialSent;

    /**
     * The host/port to be sent on this if this is a connect request.
     */
    private String      _connectHostPort;

    /**
     * The input stream from which to read the request body. This is set before
     * the request is issued, if the source of the data is a stream.
     */
    private InputStream _requestBody;

    /**
     * The byte array of the request. If the request source is a byte array,
     * this is set. When the request is first written, the contents of the
     * request is saved here in case it needs to be resent for authentication.
     */
    private byte[]      _requestBytes;

    private boolean     _responseBodySetup;

    /**
     * Indicates that we are sending an NTLM negotiate message on this URL
     * request and the underlying connection (when obtained) should be marked
     * with the setNtlmLeaveOpen() flag.
     */
    private boolean     _ntlmAuth;

    // Header values we are interested in
    // NOTE: be sure to change resetBeforeRead if anything is added
    // to this list
    protected String    _hdrContentLength;
    protected String    _hdrConnection;
    protected String    _hdrProxyConnection;
    protected String    _hdrTransferEncoding;
    protected String    _hdrWWWAuth;
    protected String    _hdrLocation;
    protected String    _hdrProxyAuth;
    protected int       _hdrContentLengthInt;

    // We have read the first character of the next line after
    // the status line, and it is here. This only applies if
    // _singleEolChar is true
    protected int       _savedAfterStatusNextChar;

    protected boolean   _singleEolChar;

    /**
     * Maximum number of redirects to forward through.
     */
    private int         MAX_FORWARDS = 100;

    public Log getLog()
    {
        return _log;
    }
    
    public final String getName()
    {
        return method;
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
        if (_log.isTraceEnabled())
            _log.trace("setPathQuery");

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

    private final void resetSentCredentials()
    {
        _credentialSent = null;
        _proxyCredentialSent = null;
    }

    public final Credential getCredentialSent(boolean proxy)
    {
        if (proxy)
            return _proxyCredentialSent;
        return _credentialSent;
    }

    public final void setCredentialSent(boolean proxy, Credential cred)
    {
        if (_log.isDebugEnabled())
        {
            _log.debug("Credential sent: " + cred + " proxy: " + proxy);
        }

        if (proxy)
            _proxyCredentialSent = cred;
        else
            _credentialSent = cred;
    }

    protected final void addRequestHeaders() throws IOException
    {
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
            _reqHeaders.add(HDR_HOST, _connection.getHostPortURL());

        }

        if (_cookieContainer != null && !_cookieContainer.isEmpty())
        {
            Cookie[] cookies = _cookieSpec.match(_connection.getHost(),
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

        // add content length or chunking
        int len = 0;
        if (null != _requestBytes)
        {
            len = _requestBytes.length;
        }
        else if (_contentLength >= 0)
        {
            len = _contentLength;
        }

        if (!_hasContentLengthHeader)
        {
            if ((_methodProperties & METHOD_PROP_ADD_CL_HEADER) != 0)
            {
                _reqHeaders.add(HDR_CONTENT_LENGTH, String.valueOf(len));
                _hasContentLengthHeader = true;
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
        _responseStream = null;
        _responseBytes = null;
    }

    protected final void readResponse() throws IOException
    {
        // _log.trace("readResponse");

        resetBeforeRead();
        readStatusLine();
        readResponseHeaders();
        isResponseEmpty();
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
            + "server closed after being idle.  Consider using the set[Default]IdleConnectionTimeout "
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
                if (name.equalsIgnoreCase(Authenticator.PROXY_AUTH))
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

                    String host = _connection.getHost();
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
                if (name.equalsIgnoreCase(Authenticator.WWW_AUTH))
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

    }

    protected final void isResponseEmpty() throws IOException
    {
        // _log.trace("isResponseEmpty");

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
        // _log.trace("setupResponseBody");

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
                result = new ChunkedInputStream(_conInStream, this);
            }
        }
        else if (_hdrContentLength != null)
        {
            result = new ContentLengthInputStream(_conInStream,
                                                  this,
                                                  _hdrContentLengthInt);
        }
        else
        {
            // We expect some data, but don't have a content length,
            // this is valid for HTTP 1.0, or in cases where the
            // connection is to be closed. This type of stream will
            // release and close the underlying connection when close is
            // called on it.
            result = new ReleaseInputStream(_conInStream, this, shouldClose);
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
        _log.trace("writeRequest");
        try
        {
            writeRequestLine();
            writeRequestHeaders();
            _conOutStream.write(Util.CRLF_BYTES);
            _conOutStream.flush();
            _bodySent = writeRequestBody();
        }
        catch (HttpException ex)
        {
            // Show this through since this is a subclass of IOException
            // we have to catch this first
            throw ex;
        }
        catch (IOException ex)
        {
            // Ignore this, the connection could have been closed on
            // the write, but theoretically still open when it is
            // read. If the connection was closed, then we will get the
            // error on the read.
        }
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
                buf.append(_connection.getHostPortURL());
            }
            buf.append(_pathQuery);
        }
        else if ((_methodProperties & METHOD_PROP_REQ_LINE_STAR) != 0)
        {
            buf.append("*");
        }
        else if ((_methodProperties & METHOD_PROP_REQ_LINE_HOST_PORT) != 0)
        {
            // Put the host/port junk on the front if required
            buf.append(_connection.getHostPort());
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
        // _log.trace("writeRequestBody");

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
            _log.trace("Found continue - sending request body");
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

    private final void processRequest() throws IOException
    {
        int maxTries = _tries;

        // try to do the write
        _tryCount = 0;
        do
        {
            _tryCount++;

            if (_tryCount > 1 && _retryInterval != 0)
            {
                try
                {
                    if (_log.isDebugEnabled())
                        _log.debug("Sleep: " + _retryInterval);
                    Thread.sleep(_retryInterval);
                }
                catch (InterruptedException ex)
                {
                    // Ignore
                }
            }

            if (_log.isTraceEnabled())
            {
                _log.trace("Attempt number " + _tryCount + " to write request");
            }

            try
            {
                connect();

                // No retry allowed (POST)
                if ((_methodProperties & METHOD_PROP_RETRY) == 0)
                {
                    _connection.checkConnection();
                    // Don't allow any retries beyond this point
                    // for a POST as its not idempotent.
                    maxTries = 0;
                }

                writeRequest();

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
                    if (_responseCode >= 200)
                    {
                        if (isReleasedAfterExecute())
                            readEntireResponse();
                    }

                    // if SC_CONTINUE write the request body
                    writeRemainingRequestBody();
                }
                while (_responseCode < 200);

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
                _log
                        .info("HttpException when writing request/reading response: ",
                              httpe);
                // Handled at a higher level
                throw httpe;
            }
            catch (IOException ioe)
            {
                _log.warn("IOException when writing "
                    + "request/reading response: ", ioe);

                // This can be retried
                releaseConnection(CLOSE);

                if (_tryCount >= maxTries)
                {
                    _log.warn("Attempt to write request/read "
                        + "response has reached max retries: "
                        + maxTries);
                    throw ioe;
                }
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

        _log.trace("readEntireResponse");
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

    private boolean processAuthenticationResponse(int statusCode)
        throws HttpException
    {
        _log.trace("processAuthenticationResponse");

        String respType = null;
        String reqType = null;

        switch (statusCode)
        {
            case HttpStatus.SC_UNAUTHORIZED:
                if (_hdrWWWAuth == null)
                    return false;
                reqType = Authenticator.WWW_AUTH;
                respType = Authenticator.WWW_AUTH_RESP;
                break;

            case HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED:
                if (_hdrProxyAuth == null)
                    return false;
                reqType = Authenticator.PROXY_AUTH;
                respType = Authenticator.PROXY_AUTH_RESP;
                break;
        }

        boolean authenticated = false;

        // remove preemptive header and reauthenticate
        _reqHeaders.remove(respType);
        try
        {
            // parse the authenticate header
            Map challengeMap = Authenticator
                    .parseAuthenticateHeader(_respHeaders, reqType, this);

            authenticated = Authenticator.authenticate(this,
                                                       _respHeaders,
                                                       reqType,
                                                       challengeMap,
                                                       respType);

            if (authenticated && challengeMap.containsKey(Authenticator.NTLM))
                _ntlmAuth = true;
        }
        catch (HttpException ex)
        {
            // Don't show this to the user, just indicate that we are
            // authenticated
            _log.warn("Exception processing authentication response: ", ex);
            return true;
        }

        if (!authenticated)
        {
            // won't be able to authenticate to this challenge
            // without additional information
            _log.debug("execute(): Server demands "
                + "authentication credentials, but none are "
                + "available, so aborting.");
        }
        else
        {
            _log.debug("execute(): Server demanded "
                + "authentication credentials, will try again.");
            // let's try it again, using the credentials
        }

        return !authenticated; // finished processing if we aren't
        // authenticated
    }

    protected final void processRequestLoop() throws IOException
    {
        int forwardCount = 0; // protect from an infinite loop

        while (forwardCount++ < MAX_FORWARDS)
        {
            if (_log.isDebugEnabled())
            {
                _log.debug("Redirect loop try " + forwardCount);
            }

            // write the request and read the response, will retry
            processRequest();

            switch (_responseCode)
            {
                case HttpStatus.SC_UNAUTHORIZED:
                case HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED:
                    _log.debug("Authorization required");
                    if (_doAuthentication)
                    {
                        // Process authentication response
                        // If the authentication is successful,
                        // return the statusCode
                        // otherwise, drop through the switch and try again.
                        if (processAuthenticationResponse(_responseCode))
                            return;
                    }
                    else
                    { // let the client handle the authenticaiton
                        return;
                    }
                    break;

                case HttpStatus.SC_MOVED_TEMPORARILY:
                case HttpStatus.SC_MOVED_PERMANENTLY:
                case HttpStatus.SC_TEMPORARY_REDIRECT:
                    _log.debug("Redirect required");

                    resetSentCredentials();

                    // TODO: This block should be factored into a new
                    // method called processRedirectResponse
                    if (!getInstanceFollowRedirects())
                    {
                        _log.info("Redirect requested but followRedirects is "
                            + "disabled");
                        return;
                    }

                    if (_hdrLocation == null)
                    {
                        // got a redirect response, but no location header
                        _log.error("Received redirect response "
                            + _responseCode
                            + " but no location header");
                        return;
                    }
                    if (_log.isDebugEnabled())
                    {
                        _log.debug("Redirect requested to location '"
                            + _hdrLocation
                            + "'");
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
                            return;
                        }
                    }

                    // Get rid of the hold Host header since we are
                    // going somewhere else
                    _reqHeaders.remove(HDR_HOST);
                    _urlString = u.toExternalForm();

                    // change the path and query string to the redirect
                    String newPathQuery = URIUtil.getPathQuery(u
                            .toExternalForm());
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

                    break;
                default:
                    // neither an unauthorized nor a redirect response
                    return;

            } // end of switch

            if (shouldCloseConnection())
            {
                closeConnection();
            }
            else
            {
                if (_connection != null && _connection.isOpen())
                {
                    // throw away body to position the stream after the response
                    discardResponseBody();
                }

                // Force getting a new connection, like for the redirect
                // case where it goes to another host
                releaseConnection(!CLOSE);
            }

        } // end of loop

        _log.error("Giving up after " + MAX_FORWARDS + " forwards");
        throw new java.net.ProtocolException("Maximum redirects ("
            + MAX_FORWARDS
            + ") exceeded");

    }

    protected final void execute() throws IOException
    {
        if (_log.isTraceEnabled())
        {
            _log.trace("execute "
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

        // Handle the methods that allow data
        if (_outStream != null
            && ((_methodProperties & METHOD_PROP_CALCULATE_CONTENT_LEN) != 0))
        {
            byte[] outBytes = _outStream.toByteArray();
            setRequestBody(outBytes);
            if (_contentLength == UNINIT_CONTENT_LENGTH)
                _contentLength = outBytes.length;

            // To be compatible with the JDK
            if ((_methodProperties & METHOD_PROP_SEND_CONTENT_TYPE) != 0)
            {
                if (_reqHeaders.get(HDR_CONTENT_TYPE) == null)
                {
                    _reqHeaders.set(HDR_CONTENT_TYPE,
                                    "application/x-www-form-urlencoded");
                }
            }
        }

        // Get me a connection, if there is a problem here, it just throws
        connect();

        // We have a connection at this point that we must release
        // if there is a problem, or if we no longer need it when the
        // execute finishes

        try
        {
            _executing = true;

            // pre-emptively add the authorization header, if required.
            Authenticator.authenticate(this,
                                       null,
                                       null,
                                       null,
                                       Authenticator.WWW_AUTH_RESP);
            if (_connection.isProxied())
            {
                Authenticator.authenticate(this,
                                           null,
                                           null,
                                           null,
                                           Authenticator.PROXY_AUTH_RESP);
            }

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
            // Remember this in case we have to re-throw it
            _ioException = httpe;
            _dead = true;
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
        finally
        {
            _executing = false;
            _executed = true;

            // The connection is released in the explicit close case
            // when the stream is released
            if (_responseIsEmpty || isReleasedAfterExecute())
                releaseConnection(!CLOSE);
        }

    }

    void setConnection(HttpConnection conn, boolean release) throws IOException
    {
        super.setConnection(conn, release);

        if (_connection != null)
        {
            // For HTTP/1.0, leave this connection open
            if (_ntlmAuth)
                _connection.setNtlmLeaveOpen(true);
            if (_log.isDebugEnabled())
            {
                _log.debug("setConn: NTLMLeaveOpen: "
                    + _connection.isNtlmLeaveOpen());
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
                    + _connection.isNtlmLeaveOpen());
            }
        }

        if (_http11)
        {
            if (null != _hdrConnection
                && HDR_VALUE_CLOSE.equalsIgnoreCase(_hdrConnection))
            {
                _log.debug("HTTP/1.1 - Will CLOSE - "
                    + "\"Connection: close\" header found.");
                result = true;
            }
        }
        else
        {
            // HTTP 1.0
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
            else if (_connection != null && _connection.isNtlmLeaveOpen())
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
