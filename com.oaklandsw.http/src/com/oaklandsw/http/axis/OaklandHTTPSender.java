/*
 * Copyright 2001-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
//
// Portions Copyright 2006, oakland software, all rights reserved.
//
// May not be used or redistributed without specific written
// permission from oakland software.
//
package com.oaklandsw.http.axis;

import org.apache.axis.AxisFault;
import org.apache.axis.Constants;
import org.apache.axis.Message;
import org.apache.axis.MessageContext;
import org.apache.axis.components.net.CommonsHTTPClientProperties;
import org.apache.axis.components.net.CommonsHTTPClientPropertiesFactory;
import org.apache.axis.handlers.BasicHandler;
import org.apache.axis.soap.SOAP12Constants;
import org.apache.axis.soap.SOAPConstants;
import org.apache.axis.transport.http.HTTPConstants;
import org.apache.axis.utils.JavaUtils;
import org.apache.axis.utils.Messages;
import org.apache.commons.logging.Log;

import com.oaklandsw.http.Cookie;
import com.oaklandsw.http.CookieContainer;
import com.oaklandsw.http.CookiePolicy;
import com.oaklandsw.http.Credential;
import com.oaklandsw.http.HttpURLConnection;
import com.oaklandsw.http.HttpUserAgent;
import com.oaklandsw.http.UserCredential;
import com.oaklandsw.util.LogUtils;
import com.oaklandsw.util.Util;

import javax.xml.soap.MimeHeader;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class OaklandHTTPSender extends BasicHandler implements HttpUserAgent
{
    private static final Log              _log = LogUtils.makeLogger();

    protected CommonsHTTPClientProperties _clientProperties;
    boolean                               httpChunkStream;

    protected Credential                  _normalCredential;
    protected Credential                  _proxyCredential;

    public OaklandHTTPSender()
    {
        initialize();
    }

    protected void initialize()
    {
        _clientProperties = CommonsHTTPClientPropertiesFactory.create();
        HttpURLConnection.setMaxConnectionsPerHost(_clientProperties
                .getMaximumConnectionsPerHost());

        if (_clientProperties.getDefaultConnectionTimeout() > 0)
        {
            HttpURLConnection.setDefaultConnectionTimeout(_clientProperties
                    .getDefaultConnectionTimeout());
        }

        if (_clientProperties.getConnectionPoolTimeout() > 0)
        {
            HttpURLConnection.setDefaultConnectionTimeout(_clientProperties
                    .getDefaultConnectionTimeout());
        }

        if (_clientProperties.getDefaultSoTimeout() > 0)
        {
            HttpURLConnection.setDefaultRequestTimeout(_clientProperties
                    .getDefaultSoTimeout());
        }
    }

    /**
     * invoke creates a socket connection, sends the request SOAP message and
     * then reads the response SOAP message back from the SOAP server
     * 
     * @param msgContext
     *            the messsage context
     * 
     * @throws AxisFault
     */
    public void invoke(MessageContext msgContext) throws AxisFault
    {
        if (_log.isDebugEnabled())
            _log.debug("OaklandHTTPSender::invoke");

        try
        {
            String targetURLString = msgContext
                    .getStrProp(MessageContext.TRANS_URL);
            URL targetURL = new URL(targetURLString);

            HttpURLConnection urlCon = HttpURLConnection
                    .openConnection(targetURL);

            urlCon.setConnectionTimeout(_clientProperties
                    .getConnectionPoolTimeout());

            // Don't interfere if the user has provided a user agent, but
            // if not, then we get the authentication information from
            // Axis
            if (HttpURLConnection.getDefaultUserAgent() == null)
            {
                urlCon.setUserAgent(this);

                // Set the proxy based on the environment
                if (urlCon.getConnectionProxyUser() != null)
                {
                    _proxyCredential = UserCredential.createCredential(urlCon
                            .getConnectionProxyUser(), urlCon
                            .getConnectionProxyPassword());
                }
            }

            boolean posting = true;

            // If we're SOAP 1.2, allow the web method to be set from the
            // MessageContext.
            if (msgContext.getSOAPConstants() == SOAPConstants.SOAP12_CONSTANTS)
            {
                String webMethod = msgContext
                        .getStrProp(SOAP12Constants.PROP_WEBMETHOD);
                if (webMethod != null)
                {
                    posting = webMethod
                            .equals(HttpURLConnection.HTTP_METHOD_POST);
                }
            }

            if (posting)
            {
                urlCon.setRequestMethod(HttpURLConnection.HTTP_METHOD_POST);
                urlCon.setDoOutput(true);
            }

            addContextInfo(urlCon, msgContext, targetURL);

            // don't forget the cookies!
            // Cookies need to be set on HttpState, since HttpMethodBase
            // overwrites the cookies from HttpState
            if (msgContext.getMaintainSession())
            {
                CookieContainer cc = new CookieContainer();
                urlCon.setCookieSupport(cc, CookiePolicy.BROWSER_COMPATIBILITY);

                String host = targetURL.getHost();
                String path = targetURL.getPath();
                boolean secure = targetURL.getProtocol()
                        .equalsIgnoreCase("https");
                fillHeaders(msgContext,
                            cc,
                            HTTPConstants.HEADER_COOKIE,
                            host,
                            path,
                            secure);
                fillHeaders(msgContext,
                            cc,
                            HTTPConstants.HEADER_COOKIE2,
                            host,
                            path,
                            secure);
            }

            if (posting)
            {
                Message reqMessage = msgContext.getRequestMessage();
                OutputStream out = urlCon.getOutputStream();
                try
                {
                    if (msgContext
                            .isPropertyTrue(HTTPConstants.MC_GZIP_REQUEST))
                    {
                        GZIPOutputStream gzStream = new GZIPOutputStream(out);
                        reqMessage.writeTo(gzStream);
                        gzStream.finish();
                    }
                    else
                    {
                        reqMessage.writeTo(out);
                    }

                }
                catch (SOAPException e)
                {
                    throw new IOException(e.getMessage());
                }
            }

            int returnCode = urlCon.getResponseCode();

            String contentType = urlCon
                    .getHeaderField(HTTPConstants.HEADER_CONTENT_TYPE);
            String contentLocation = urlCon
                    .getHeaderField(HTTPConstants.HEADER_CONTENT_LOCATION);
            String contentLength = urlCon
                    .getHeaderField(HTTPConstants.HEADER_CONTENT_LENGTH);

            if ((returnCode > 199) && (returnCode < 300))
            {

                // SOAP return is OK - so fall through
            }
            else if (msgContext.getSOAPConstants() == SOAPConstants.SOAP12_CONSTANTS)
            {
                // For now, if we're SOAP 1.2, fall through, since the range of
                // valid result codes is much greater
            }
            else if ((contentType != null)
                && !contentType.equals("text/html")
                && ((returnCode > 499) && (returnCode < 600)))
            {

                // SOAP Fault should be in here - so fall through
            }
            else
            {
                String statusMessage = urlCon.getHeaderField(0);
                AxisFault fault = new AxisFault("HTTP", "("
                    + returnCode
                    + ")"
                    + statusMessage, null, null);

                fault.setFaultDetailString(Messages.getMessage("return01", ""
                    + returnCode, Util.getStringFromInputStream(urlCon
                        .getInputStream())));
                fault.addFaultDetail(Constants.QNAME_FAULTDETAIL_HTTPERRORCODE,
                                     Integer.toString(returnCode));
                throw fault;
            }

            // wrap the response body stream so that close() also releases
            // the connection back to the pool.
            InputStream inStr = urlCon.getInputStream();

            String contentEncoding = urlCon
                    .getHeaderField(HTTPConstants.HEADER_CONTENT_ENCODING);
            if (contentEncoding != null)
            {
                if (contentEncoding
                        .equalsIgnoreCase(HTTPConstants.COMPRESSION_GZIP))
                {
                    inStr = new GZIPInputStream(inStr);
                }
                else
                {
                    AxisFault fault = new AxisFault("HTTP",
                                                    "unsupported content-encoding of '"
                                                        + contentEncoding
                                                        + "' found",
                                                    null,
                                                    null);
                    throw fault;
                }

            }

            Message outMsg = new Message(inStr,
                                         false,
                                         contentType,
                                         contentLocation);

            int len = urlCon.getHeadersLength();
            MimeHeaders responseMimeHeaders = outMsg.getMimeHeaders();
            // Skip header 0, that's the HTTP status line
            for (int i = 1; i < len; i++)
            {
                responseMimeHeaders.addHeader(urlCon.getHeaderFieldKey(i),
                                              urlCon.getHeaderField(i));
            }

            outMsg.setMessageType(Message.RESPONSE);
            msgContext.setResponseMessage(outMsg);
            if (_log.isDebugEnabled())
            {
                if (null == contentLength)
                    _log.debug("\n no Content-Length");
                _log.debug("\n XML received:");
                _log.debug("-----------------------------------------------");
                _log.debug(outMsg.getSOAPPartAsString());
            }

            // if we are maintaining session state,
            // handle cookies (if any)
            if (msgContext.getMaintainSession())
            {
                int hl = urlCon.getHeadersLength();

                for (int i = 1; i < hl; i++)
                {
                    String headerName = urlCon.getHeaderFieldKey(i);

                    if (headerName
                            .equalsIgnoreCase(HTTPConstants.HEADER_SET_COOKIE))
                    {
                        handleCookie(HTTPConstants.HEADER_COOKIE, urlCon
                                .getHeaderField(i), msgContext);
                    }
                    else if (headerName
                            .equalsIgnoreCase(HTTPConstants.HEADER_SET_COOKIE2))
                    {
                        handleCookie(HTTPConstants.HEADER_COOKIE2, urlCon
                                .getHeaderField(i), msgContext);
                    }
                }
            }
        }
        catch (Exception e)
        {
            _log.debug(e);
            throw AxisFault.makeFault(e);
        }
    }

    /**
     * little helper function for cookies. fills up the message context with a
     * string or an array of strings (if there are more than one Set-Cookie)
     * 
     * @param cookieName
     * @param setCookieName
     * @param cookie
     * @param msgContext
     */
    public static void handleCookie(String cookieName,
                             String cookie,
                             MessageContext msgContext)
    {

        cookie = cleanupCookie(cookie);
        int keyIndex = cookie.indexOf("=");
        String key = (keyIndex != -1) ? cookie.substring(0, keyIndex) : cookie;

        ArrayList cookies = new ArrayList();
        Object oldCookies = msgContext.getProperty(cookieName);
        boolean alreadyExist = false;
        if (oldCookies != null)
        {
            if (oldCookies instanceof String[])
            {
                String[] oldCookiesArray = (String[])oldCookies;
                for (int i = 0; i < oldCookiesArray.length; i++)
                {
                    String anOldCookie = oldCookiesArray[i];
                    if (key != null && anOldCookie.indexOf(key) == 0)
                    { // same cookie key
                        anOldCookie = cookie; // update to new one
                        alreadyExist = true;
                    }
                    cookies.add(anOldCookie);
                }
            }
            else
            {
                String oldCookie = (String)oldCookies;
                if (key != null && oldCookie.indexOf(key) == 0)
                { // same cookie key
                    oldCookie = cookie; // update to new one
                    alreadyExist = true;
                }
                cookies.add(oldCookie);
            }
        }

        if (!alreadyExist)
        {
            cookies.add(cookie);
        }

        if (cookies.size() == 1)
        {
            msgContext.setProperty(cookieName, cookies.get(0));
        }
        else if (cookies.size() > 1)
        {
            msgContext.setProperty(cookieName, cookies
                    .toArray(new String[cookies.size()]));
        }
    }

    private void fillHeaders(MessageContext msgContext,
                             CookieContainer cc,
                             String header,
                             String host,
                             String path,
                             boolean secure)
    {
        Object ck1 = msgContext.getProperty(header);
        if (ck1 != null)
        {
            if (ck1 instanceof String[])
            {
                String[] cookies = (String[])ck1;
                for (int i = 0; i < cookies.length; i++)
                {
                    addCookie(cc, cookies[i], host, path, secure);
                }
            }
            else
            {
                addCookie(cc, (String)ck1, host, path, secure);
            }
        }
    }

    /**
     * add cookie to state
     * 
     * @param state
     * @param cookie
     */
    private void addCookie(CookieContainer cc,
                           String cookie,
                           String host,
                           String path,
                           boolean secure)
    {
        int index = cookie.indexOf('=');
        cc.addCookie(new Cookie(host, cookie.substring(0, index), cookie
                .substring(index + 1), path, null, secure));
    }

    /**
     * cleanup the cookie value.
     * 
     * @param cookie
     *            initial cookie value
     * 
     * @return a cleaned up cookie value.
     */
    private static String cleanupCookie(String cookie)
    {
        cookie = cookie.trim();
        // chop after first ; a la Apache SOAP (see HTTPUtils.java there)
        int index = cookie.indexOf(';');
        if (index != -1)
        {
            cookie = cookie.substring(0, index);
        }
        return cookie;
    }

    private void addContextInfo(HttpURLConnection urlCon,
                                MessageContext msgContext,
                                URL tmpURL) throws Exception
    {
        // optionally set a timeout for the request
        if (msgContext.getTimeout() != 0)
        {
            urlCon.setRequestTimeout(msgContext.getTimeout());
            urlCon.setConnectionTimeout(msgContext.getTimeout());
        }

        // Get SOAPAction, default to ""
        String action = msgContext.useSOAPAction() ? msgContext
                .getSOAPActionURI() : "";

        if (action == null)
        {
            action = "";
        }

        Message msg = msgContext.getRequestMessage();
        if (msg != null)
        {
            urlCon.setRequestProperty(HTTPConstants.HEADER_CONTENT_TYPE, msg
                    .getContentType(msgContext.getSOAPConstants()));
        }
        urlCon.setRequestProperty(HTTPConstants.HEADER_SOAP_ACTION, "\""
            + action
            + "\"");
        urlCon.setRequestProperty(HTTPConstants.HEADER_USER_AGENT, Messages
                .getMessage("axisUserAgent"));
        String userID = msgContext.getUsername();
        String passwd = msgContext.getPassword();

        // if UserID is not part of the context, but is in the URL, use
        // the one in the URL.
        String userInfo = tmpURL.getUserInfo();

        if ((userID == null) && (userInfo != null))
        {
            int sep = userInfo.indexOf(':');

            if ((sep >= 0) && (sep + 1 < userInfo.length()))
            {
                userID = userInfo.substring(0, sep);
                passwd = userInfo.substring(sep + 1);
            }
            else
            {
                userID = userInfo;
            }
        }

        if (userID != null)
        {
            _normalCredential = UserCredential.createCredential(userID, passwd);
        }

        // add compression headers if needed
        if (msgContext.isPropertyTrue(HTTPConstants.MC_ACCEPT_GZIP))
        {
            urlCon.addRequestProperty(HTTPConstants.HEADER_ACCEPT_ENCODING,
                                      HTTPConstants.COMPRESSION_GZIP);
        }
        if (msgContext.isPropertyTrue(HTTPConstants.MC_GZIP_REQUEST))
        {
            urlCon.addRequestProperty(HTTPConstants.HEADER_CONTENT_ENCODING,
                                      HTTPConstants.COMPRESSION_GZIP);
        }

        // Transfer MIME headers of SOAPMessage to HTTP headers.
        MimeHeaders mimeHeaders = msg.getMimeHeaders();
        if (mimeHeaders != null)
        {
            for (Iterator i = mimeHeaders.getAllHeaders(); i.hasNext();)
            {
                MimeHeader mimeHeader = (MimeHeader)i.next();
                // HEADER_CONTENT_TYPE and HEADER_SOAP_ACTION are already set.
                // Let's not duplicate them.
                String headerName = mimeHeader.getName();
                if (headerName.equals(HTTPConstants.HEADER_CONTENT_TYPE)
                    || headerName.equals(HTTPConstants.HEADER_SOAP_ACTION))
                {
                    continue;
                }
                urlCon.addRequestProperty(mimeHeader.getName(), mimeHeader
                        .getValue());
            }
        }

        // process user defined headers for information.
        Hashtable userHeaderTable = (Hashtable)msgContext
                .getProperty(HTTPConstants.REQUEST_HEADERS);

        if (userHeaderTable != null)
        {
            for (Iterator e = userHeaderTable.entrySet().iterator(); e
                    .hasNext();)
            {
                Map.Entry me = (Map.Entry)e.next();
                Object keyObj = me.getKey();

                if (null == keyObj)
                {
                    continue;
                }
                String key = keyObj.toString().trim();
                String value = me.getValue().toString().trim();

                if (key
                        .equalsIgnoreCase(HTTPConstants.HEADER_TRANSFER_ENCODING_CHUNKED))
                {
                    String val = me.getValue().toString();
                    if (null != val)
                    {
                        httpChunkStream = JavaUtils.isTrue(val);
                    }
                }
                else
                {
                    urlCon.addRequestProperty(key, value);
                }
            }
        }
    }

    public com.oaklandsw.http.Credential getCredential(String realm,
                                                       String url,
                                                       int scheme)
    {
        return _normalCredential;
    }

    public com.oaklandsw.http.Credential getProxyCredential(String realm,
                                                            String url,
                                                            int scheme)
    {
        return _proxyCredential;
    }

}
