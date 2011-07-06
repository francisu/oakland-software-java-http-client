/*
 * Copyright 2004,2005 The Apache Software Foundation.
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
// Portions Copyright 2006-2007, oakland software, all rights reserved.
//
package com.oaklandsw.http.axis2;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.handlers.AbstractHandler;
import org.apache.axis2.i18n.Messages;
import org.apache.axis2.transport.MessageFormatter;
import org.apache.axis2.transport.OutTransportInfo;
import org.apache.axis2.transport.TransportSender;
import org.apache.axis2.transport.TransportUtils;
import org.apache.axis2.util.JavaUtils;
import org.apache.axis2.util.Utils;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.axis2.transport.http.*;

import com.oaklandsw.utillog.Log;

import com.oaklandsw.http.Cookie;
import com.oaklandsw.http.CookieContainer;
import com.oaklandsw.http.Credential;
import com.oaklandsw.http.HeaderElement;
import com.oaklandsw.http.HttpException;
import com.oaklandsw.http.HttpStatus;
import com.oaklandsw.http.HttpURLConnectInternal;
import com.oaklandsw.http.HttpURLConnection;
import com.oaklandsw.http.HttpUserAgent;
import com.oaklandsw.http.NameValuePair;
import com.oaklandsw.http.NtlmCredential;
import com.oaklandsw.http.UserCredential;
import com.oaklandsw.util.LogUtils;
import com.oaklandsw.util.Util;

import javax.xml.namespace.QName;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class OaklandHTTPTransportSender2 extends AbstractHandler
    implements
        TransportSender
{
    private static final Log          _log               = LogUtils
                                                                 .makeLogger();

    protected static final String     ANONYMOUS          = "anonymous";

    protected static final String     PROXY_HOST_NAME    = "proxy_host";
    protected static final String     PROXY_PORT         = "proxy_port";

    // Parameters defined in the transport
    String                            _protocol;
    int                               _soTimeout         = HTTPConstants.DEFAULT_SO_TIMEOUT;
    int                               _connectionTimeout = HTTPConstants.DEFAULT_CONNECTION_TIMEOUT;
    private boolean                   _chunked           = false;

    int                               CHUNK_LEN          = 1024;

    protected static Class            _transportUtils;

    protected static Method           _methodDoWriteMTOM;
    protected static Method           _methodDoWriteSwA;
    protected static Method           _methodIsDoingRest;
    protected static Method           _methodGetCharSetEncoding;

    static
    {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        try
        {
            // Versions prior to 1.5
            _transportUtils = cl
                    .loadClass("org.apache.axis2.transport.http.HTTPTransportUtils");
            // The method is here in these earlier versions
            _methodDoWriteMTOM = _transportUtils
                    .getMethod("doWriteMTOM",
                               new Class[] { MessageContext.class });
        }
        catch (Exception ex)
        {
            _transportUtils = null;
        }

        if (_transportUtils == null)
        {
            try
            {
                // 1.5 and higher
                _transportUtils = cl
                        .loadClass("org.apache.axis2.transport.TransportUtils");
            }
            catch (Exception ex)
            {

            }
        }
        if (_transportUtils == null)
            Util.impossible("TransportUtils not found");

        try
        {
            _methodDoWriteMTOM = _transportUtils
                    .getMethod("doWriteMTOM",
                               new Class[] { MessageContext.class });
            _methodDoWriteSwA = _transportUtils
                    .getMethod("doWriteSwA",
                               new Class[] { MessageContext.class });
            _methodIsDoingRest = _transportUtils
                    .getMethod("isDoingREST",
                               new Class[] { MessageContext.class });
            _methodGetCharSetEncoding = _transportUtils
                    .getMethod("getCharSetEncoding",
                               new Class[] { MessageContext.class });
        }
        catch (Exception e)
        {
            Util.impossible("Can't get TransportUtils method", e);
        }

    }

    /**
     * proxydiscription
     */
    protected TransportOutDescription proxyOutSetting    = null;

    public void cleanup(MessageContext msgContext) throws AxisFault
    {
        HttpURLConnection urlCon = (HttpURLConnection)msgContext
                .getProperty(HTTPConstants.HTTP_METHOD);

        // Make sure the input stream is closed since we use explicit close
        try
        {
            if (urlCon != null)
            {
                InputStream is = urlCon.getInputStream();
                if (is != null)
                    is.close();
            }
        }
        catch (IOException e)
        {
            _log.debug(e);
            throw new AxisFault(e.getMessage(), e);
        }
    }

    public void init(ConfigurationContext confContext,
                     TransportOutDescription transportOut) throws AxisFault
    {
        _log.debug("init");
        Parameter version = transportOut
                .getParameter(HTTPConstants.HTTP_PROTOCOL_VERSION);
        if (version != null)
        {
            _protocol = (String)version.getValue();

            // We don't care about the version
            if (_protocol.indexOf('/') > 0)
                _protocol = _protocol.substring(0, _protocol.indexOf('/'));

            if (!_protocol.toLowerCase().equals("http")
                && !_protocol.toLowerCase().equals("https"))
            {
                throw new AxisFault("Invalid protocol for HTTP transport: "
                    + _protocol);
            }
            if (_log.isDebugEnabled())
                _log.debug("init - setting PROTOCOL: " + _protocol);
        }

        Parameter transferEncoding = transportOut
                .getParameter(HTTPConstants.HEADER_TRANSFER_ENCODING);

        if ((transferEncoding != null)
            && HTTPConstants.HEADER_TRANSFER_ENCODING_CHUNKED
                    .equals(transferEncoding.getValue()))
        {
            _chunked = true;
            if (_log.isDebugEnabled())
                _log.debug("init - setting chunked");
        }

        // Get the timeout values from the configuration
        try
        {
            Parameter tempSoTimeoutParam = transportOut
                    .getParameter(HTTPConstants.SO_TIMEOUT);
            Parameter tempConnTimeoutParam = transportOut
                    .getParameter(HTTPConstants.CONNECTION_TIMEOUT);

            if (tempSoTimeoutParam != null)
            {
                _soTimeout = Integer.parseInt((String)tempSoTimeoutParam
                        .getValue());
                if (_log.isDebugEnabled())
                    _log.debug("init - setting SO_TIMEOUT: " + _soTimeout);
            }

            if (tempConnTimeoutParam != null)
            {
                _connectionTimeout = Integer
                        .parseInt((String)tempConnTimeoutParam.getValue());
                if (_log.isDebugEnabled())
                    _log.debug("init - setting CONNECTION_TIMEOUT: "
                        + _connectionTimeout);
            }
        }
        catch (NumberFormatException nfe)
        {
            // If there's a problem log it and use the default values
            _log.error("Invalid timeout value format: not a number", nfe);
        }
    }

    public void stop()
    {
        // Any code that , need to invoke when sender stop
    }

    public InvocationResponse invoke(MessageContext msgContext)
        throws AxisFault
    {
        try
        {
            OMOutputFormat format = new OMOutputFormat();
            try
            {
                msgContext.setDoingMTOM(((Boolean)_methodDoWriteMTOM
                        .invoke(null, new Object[] { msgContext }))
                        .booleanValue());
                msgContext.setDoingSwA(((Boolean)_methodDoWriteSwA
                        .invoke(null, new Object[] { msgContext }))
                        .booleanValue());
                msgContext.setDoingREST(((Boolean)_methodIsDoingRest
                        .invoke(null, new Object[] { msgContext }))
                        .booleanValue());
                format.setCharSetEncoding((String)_methodGetCharSetEncoding
                        .invoke(null, new Object[] { msgContext }));
            }
            catch (Exception e)
            {
                Util
                        .impossible("Problem with reflection method invocations",
                                    e);
            }

            format.setSOAP11(msgContext.isSOAP11());
            format.setDoOptimize(msgContext.isDoingMTOM());
            format.setDoingSWA(msgContext.isDoingSwA());

            Object mimeBoundaryProperty = msgContext
                    .getProperty(Constants.Configuration.MIME_BOUNDARY);
            if (mimeBoundaryProperty != null)
            {
                format.setMimeBoundary((String)mimeBoundaryProperty);
            }

            TransportOutDescription transportOut = msgContext
                    .getConfigurationContext().getAxisConfiguration()
                    .getTransportOut(Constants.TRANSPORT_HTTP);

            // if a parameter hs set been set, we will omit the SOAP action for
            // SOAP 1.2
            if (transportOut != null)
            {
                Parameter param = transportOut
                        .getParameter(HTTPConstants.OMIT_SOAP_12_ACTION);
                Object value = null;
                if (param != null)
                {
                    value = param.getValue();
                }

                if (value != null && JavaUtils.isTrueExplicitly(value))
                {
                    if (!msgContext.isSOAP11())
                    {
                        msgContext
                                .setProperty(Constants.Configuration.DISABLE_SOAP_ACTION,
                                             Boolean.TRUE);
                    }
                }
            }

            // Trasnport URL can be different from the WSA-To. So processing
            // that now.
            EndpointReference epr = null;
            String transportURL = (String)msgContext
                    .getProperty(Constants.Configuration.TRANSPORT_URL);

            if (transportURL != null)
            {
                epr = new EndpointReference(transportURL);
            }
            else if (msgContext.getTo() != null
                && !msgContext.getTo().hasAnonymousAddress())
            {
                epr = msgContext.getTo();
            }

            // Check for the REST behaviour, if you desire rest beahaviour
            // put a <parameter name="doREST" value="true"/> at the
            // server.xml/client.xml file
            // ######################################################
            // Change this place to change the wsa:toepr
            // epr = something
            // ######################################################

            if ((epr != null) && (!epr.hasNoneAddress()))
            {
                writeMessageWithCommons(msgContext, epr, format);
            }
            else if (msgContext.getProperty(MessageContext.TRANSPORT_OUT) != null)
            {
                sendUsingOutputStream(msgContext, format);
            }
            else
            {
                throw new AxisFault("Both the TO and MessageContext.TRANSPORT_OUT property are Null, No where to send");
            }

            if (msgContext.getOperationContext() != null)
            {
                msgContext.getOperationContext()
                        .setProperty(Constants.RESPONSE_WRITTEN,
                                     Constants.VALUE_TRUE);
            }
        }
        catch (XMLStreamException e)
        {
            _log.debug(e);
            throw new AxisFault(e.getMessage(), e);
        }
        catch (FactoryConfigurationError e)
        {
            _log.debug(e);
            throw new AxisFault(e.getMessage(), e);
        }
        catch (IOException e)
        {
            _log.debug(e);
            throw new AxisFault(e.getMessage(), e);
        }
        return InvocationResponse.CONTINUE;
    }

    private void sendUsingOutputStream(MessageContext msgContext,
                                       OMOutputFormat format)
        throws AxisFault,
            XMLStreamException
    {
        OutputStream out = (OutputStream)msgContext
                .getProperty(MessageContext.TRANSPORT_OUT);

        // I Don't thinik we need this check.. Content type needs to be set in
        // any case. (thilina)
        // if (msgContext.isServerSide()) {
        OutTransportInfo transportInfo = (OutTransportInfo)msgContext
                .getProperty(Constants.OUT_TRANSPORT_INFO);

        ServletBasedOutTransportInfo servletBasedOutTransportInfo = null;
        if (transportInfo != null
            && transportInfo instanceof ServletBasedOutTransportInfo)
        {
            servletBasedOutTransportInfo = (ServletBasedOutTransportInfo)transportInfo;
            List customHheaders = (List)msgContext
                    .getProperty(HTTPConstants.HTTP_HEADERS);
            if (customHheaders != null)
            {
                Iterator iter = customHheaders.iterator();
                while (iter.hasNext())
                {
                    HeaderElement header = (HeaderElement)iter.next();
                    if (header != null)
                    {
                        servletBasedOutTransportInfo
                                .addHeader(header.getName(), header.getValue());
                    }
                }
            }
        }

        format.setAutoCloseWriter(true);

        MessageFormatter messageFormatter = TransportUtils
                .getMessageFormatter(msgContext);
        transportInfo
                .setContentType(messageFormatter
                        .getContentType(msgContext,
                                        format,
                                        findSOAPAction(msgContext)));

        Object gzip = msgContext.getOptions()
                .getProperty(HTTPConstants.MC_GZIP_RESPONSE);
        if (gzip != null && JavaUtils.isTrueExplicitly(gzip))
        {
            servletBasedOutTransportInfo
                    .addHeader(HTTPConstants.HEADER_CONTENT_ENCODING,
                               HTTPConstants.COMPRESSION_GZIP);
            try
            {
                out = new GZIPOutputStream(out);
                out.write(messageFormatter.getBytes(msgContext, format));
                ((GZIPOutputStream)out).finish();
                out.flush();
            }
            catch (IOException e)
            {
                throw new AxisFault("Could not compress response");
            }
        }
        else
        {
            messageFormatter.writeTo(msgContext, format, out, false);
        }
    }

    public class UserAgent implements HttpUserAgent
    {
        Credential _credential;
        Credential _proxyCredential;

        public Credential getCredential(String realm, String url, int scheme)
        {
            return _credential;
        }

        public Credential getProxyCredential(String realm,
                                             String url,
                                             int scheme)
        {
            return _proxyCredential;
        }

    }

    private void writeMessageWithCommons(MessageContext msgContext,
                                         EndpointReference toEPR,
                                         OMOutputFormat format)
        throws AxisFault
    {
        try
        {
            if (msgContext.getProperty(HTTPConstants.CHUNKED) != null)
            {
                _chunked = JavaUtils.isTrueExplicitly(msgContext
                        .getProperty(HTTPConstants.CHUNKED));
            }

            MessageFormatter messageFormatter = TransportUtils
                    .getMessageFormatter(msgContext);

            URL url = new URL(toEPR.getAddress());
            url = messageFormatter.getTargetAddress(msgContext, format, url);

            // Initially the HTTP_METHOD is the type of request
            String httpMethod = (String)msgContext
                    .getProperty(Constants.Configuration.HTTP_METHOD);

            HttpURLConnection urlCon = HttpURLConnection.openConnection(url);
            CookieContainer cc = new CookieContainer();
            urlCon.setCookieSupport(cc, null);

            // Then, bizzarrely, we set the method to be the urlCon object
            // (this is the same thing as the HttpClient method of doing things)
            // This is referred to in clean() to close the InputStream if
            // necessary
            msgContext.setProperty(HTTPConstants.HTTP_METHOD, urlCon);

            if (httpMethod != null)
                urlCon.setRequestMethod(httpMethod);
            else
                urlCon.setRequestMethod(HttpURLConnection.HTTP_METHOD_POST);

            String soapActionString = findSOAPAction(msgContext);

            // Need to have this here because we can have soap action when using
            // the soap response MEP
            String quotedSoapAction = messageFormatter
                    .formatSOAPAction(msgContext, format, soapActionString);

            // The apperance of the SOAPAction header without a namespace does
            // not work when calling the microsoft web services. Unfortunately,
            // there is no easy way to add the namespace header
            if (quotedSoapAction != null)
            {
                urlCon.setRequestProperty(HTTPConstants.HEADER_SOAP_ACTION,
                                          quotedSoapAction);
            }

            // doAuthentication is on by default, let's just be explicit both
            // ways
            if (false)
            {
                if (msgContext.getProperty(HTTPConstants.AUTHENTICATE) != null)
                    urlCon.setDoAuthentication(true);
                else
                    urlCon.setDoAuthentication(false);
            }

            // Setup web services for dummy authentication startup for NTLM
            HttpURLConnectInternal
                    .setupAuthDummy(urlCon, msgContext.isSOAP11());

            // Content type
            String contentType = messageFormatter
                    .getContentType(msgContext, format, soapActionString);

            // Add action to the content type for SOAP 1.2
            if (!msgContext.isSOAP11()
                && (soapActionString != null)
                && !"".equals(soapActionString.trim())
                && !"\"\"".equals(soapActionString.trim()))
            {
                contentType = contentType
                    + ";action=\""
                    + soapActionString
                    + "\";";
            }

            urlCon.setRequestProperty(HTTPConstants.HEADER_CONTENT_TYPE,
                                      contentType);

            // Cookies
            Object cookieString = msgContext
                    .getProperty(HTTPConstants.COOKIE_STRING);

            if (cookieString != null)
            {
                StringBuffer buffer = new StringBuffer();
                buffer.append(Constants.SESSION_COOKIE_JSESSIONID);
                buffer.append("=");
                buffer.append(cookieString);
                urlCon.setRequestProperty(HTTPConstants.HEADER_COOKIE, buffer
                        .toString());
            }

            // Timeout
            long timeout = msgContext.getOptions().getTimeOutInMilliSeconds();
            if (timeout != 0)
            {
                urlCon.setTimeout((int)timeout);
            }

            // set the custom headers, if available
            addCustomHeaders(urlCon, msgContext);

            // add compression headers if needed
            if (Utils
                    .isExplicitlyTrue(msgContext, HTTPConstants.MC_ACCEPT_GZIP))
            {
                urlCon.addRequestProperty(HTTPConstants.HEADER_ACCEPT_ENCODING,
                                          HTTPConstants.COMPRESSION_GZIP);
            }

            if (Utils.isExplicitlyTrue(msgContext,
                                       HTTPConstants.MC_GZIP_REQUEST))
            {
                urlCon
                        .addRequestProperty(HTTPConstants.HEADER_CONTENT_ENCODING,
                                            HTTPConstants.COMPRESSION_GZIP);
            }

            if (_chunked)
                urlCon.setChunkedStreamingMode(CHUNK_LEN);

            UserAgent ua;

            if (isAuthenticationEnabled(msgContext))
            {
                ua = new UserAgent();
                setAuthenticationInfo(urlCon, msgContext, ua);
            }

            // proxy configuration
            if (isProxyListed(msgContext))
            {
                ua = new UserAgent();
                configProxyAuthentication(urlCon,
                                          proxyOutSetting,
                                          ua,
                                          msgContext);
            }

            urlCon.setDoOutput(true);

            OutputStream outStream = urlCon.getOutputStream();

            // Write the request

            final int REQ_AXIS = 1;
            final int REQ_REST = 2;
            // final int REQ_REST2 = 3;

            int requestType = REQ_AXIS;
            switch (requestType)
            {
                case REQ_AXIS:

                    // Write the message
                    Object gzip = msgContext.getOptions()
                            .getProperty(HTTPConstants.MC_GZIP_REQUEST);
                    if (gzip != null
                        && JavaUtils.isTrueExplicitly(gzip)
                        && _chunked)
                    {
                        outStream = new GZIPOutputStream(outStream);
                    }

                    try
                    {
                        if (_chunked)
                        {
                            messageFormatter.writeTo(msgContext,
                                                     format,
                                                     outStream,
                                                     true /* isAllowedRetry */);
                        }
                        else
                        {
                            byte[] bytes = messageFormatter
                                    .getBytes(msgContext, format);
                            outStream.write(bytes);
                        }
                        if (outStream instanceof GZIPOutputStream)
                        {
                            ((GZIPOutputStream)outStream).finish();
                        }
                    }
                    catch (FactoryConfigurationError e)
                    {
                        throw new AxisFault(e.getMessage(), e);
                    }
                    catch (IOException e)
                    {
                        throw new AxisFault(e.getMessage(), e);
                    }
                    break;

                case REQ_REST:
                    OMElement element = msgContext.getEnvelope();
                    if (_chunked)
                    {
                        format.setDoOptimize(format.isDoingSWA());
                        element.serializeAndConsume(outStream, format);
                    }
                    else
                    {
                        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
                        if (!format.isOptimized())
                        {
                            OMOutputFormat format2 = new OMOutputFormat();
                            format2.setCharSetEncoding(format
                                    .getCharSetEncoding());
                            element.serializeAndConsume(bytesOut, format2);
                        }
                        else
                        {
                            format.setDoOptimize(true);
                            element.serializeAndConsume(bytesOut, format);
                        }
                        outStream.write(bytesOut.toByteArray());
                    }

                    break;
                default:
                    Util.impossible("Invalid request type: " + requestType);
                    break;
            }

            outStream.flush();
            outStream.close();

            // Read the response
            int responseCode = urlCon.getResponseCode();

            switch (responseCode)
            {
                case HttpStatus.SC_OK:
                    processResponse(urlCon, msgContext);
                    break;
                case HttpStatus.SC_ACCEPTED:
                    // Nothing
                    break;
                case HttpStatus.SC_INTERNAL_SERVER_ERROR:
                    String contenttypeHeader = urlCon
                            .getHeaderField(HTTPConstants.HEADER_CONTENT_TYPE);
                    if (contenttypeHeader != null)
                    {
                        processResponse(urlCon, msgContext);
                    }
                    break;
                default:
                    throw new AxisFault(Messages
                            .getMessage("transportError", String.valueOf(urlCon
                                    .getResponseCode()), urlCon
                                    .getResponseMessage()));
            }
        }
        catch (MalformedURLException e)
        {
            _log.debug(e);
            throw new AxisFault(e.getMessage(), e);
        }
        catch (HttpException e)
        {
            _log.debug(e);
            throw new AxisFault(e.getMessage(), e);
        }
        catch (IOException e)
        {
            _log.debug(e);
            throw new AxisFault(e.getMessage(), e);
        }
        catch (XMLStreamException e)
        {
            _log.debug(e);
            throw new AxisFault(e.getMessage(), e);
        }
    }

    public void addCustomHeaders(HttpURLConnection urlCon,
                                 MessageContext msgContext)
    {

        boolean isCustomUserAgentSet = false;
        // set the custom headers, if available
        Object httpHeadersObj = msgContext
                .getProperty(HTTPConstants.HTTP_HEADERS);
        if (httpHeadersObj != null && httpHeadersObj instanceof ArrayList)
        {
            List httpHeaders = (List)httpHeadersObj;
            HeaderElement header;
            for (int i = 0; i < httpHeaders.size(); i++)
            {
                header = (HeaderElement)httpHeaders.get(i);
                if (HTTPConstants.HEADER_USER_AGENT.equals(header.getName()))
                {
                    isCustomUserAgentSet = true;
                }
                urlCon.addRequestProperty(header.getName(), header.getValue());
            }
        }

        if (!isCustomUserAgentSet)
        {
            String userAgentString = getUserAgent(msgContext);
            urlCon.setRequestProperty(HTTPConstants.HEADER_USER_AGENT,
                                      userAgentString);
        }

    }

    private String getUserAgent(MessageContext messageContext)
    {
        String userAgentString = "Axis2";
        boolean locked = false;
        if (messageContext.getParameter(HTTPConstants.USER_AGENT) != null)
        {
            OMElement userAgentElement = messageContext
                    .getParameter(HTTPConstants.USER_AGENT)
                    .getParameterElement();
            userAgentString = userAgentElement.getText().trim();
            OMAttribute lockedAttribute = userAgentElement
                    .getAttribute(new QName("locked"));
            if (lockedAttribute != null)
            {
                if (lockedAttribute.getAttributeValue()
                        .equalsIgnoreCase("true"))
                {
                    locked = true;
                }
            }
        }
        // Runtime overing part
        if (!locked)
        {
            if (messageContext.getProperty(HTTPConstants.USER_AGENT) != null)
            {
                userAgentString = (String)messageContext
                        .getProperty(HTTPConstants.USER_AGENT);
            }
        }

        return userAgentString;
    }

    private static String findSOAPAction(MessageContext messageContext)
    {
        String soapActionString = null;

        Object disableSoapAction = messageContext.getOptions()
                .getProperty(Constants.Configuration.DISABLE_SOAP_ACTION);

        if (!JavaUtils.isTrueExplicitly(disableSoapAction))
        {
            // first try to get the SOAP action from message context
            soapActionString = messageContext.getSoapAction();
            if ((soapActionString == null) || (soapActionString.length() == 0))
            {
                // now let's try to get WSA action
                soapActionString = messageContext.getWSAAction();
                if (messageContext.getAxisOperation() != null
                    && ((soapActionString == null) || (soapActionString
                            .length() == 0)))
                {
                    // last option is to get it from the axis operation
                    soapActionString = messageContext.getAxisOperation()
                            .getInputAction();
                }
            }
        }

        // Prepend the namespace to the action
        if (soapActionString != null)
        {
            SOAPEnvelope env = messageContext.getEnvelope();
            SOAPHeader header = env.getHeader();
            OMElement body = (OMElement)header.getNextOMSibling();
            OMElement op = (OMElement)body.getFirstOMChild();
            String namespaceURI = op.getNamespace().getNamespaceURI();

            // The namespace might already be there
            if (!soapActionString.startsWith(namespaceURI))
            {
                if (namespaceURI.endsWith("/"))
                {
                    soapActionString = namespaceURI + soapActionString;
                }
                else
                {
                    soapActionString = namespaceURI + "/" + soapActionString;
                }
            }
        }

        // Since action is optional for SOAP 1.2 we can return null here.
        if (soapActionString == null && messageContext.isSOAP11())
        {
            soapActionString = "\"\"";
        }

        return soapActionString;
    }

    protected void processResponse(HttpURLConnection urlCon,
                                   MessageContext msgContext)
        throws IOException
    {
        obtainHTTPHeaderInformation(urlCon, msgContext);

        InputStream in = urlCon.getInputStream();
        if (in == null)
        {
            throw new AxisFault(Messages.getMessage("canNotBeNull",
                                                    "InputStream"));
        }

        String contentEncoding = urlCon
                .getHeaderField(HTTPConstants.HEADER_CONTENT_ENCODING);
        if (contentEncoding != null)
        {
            if (contentEncoding
                    .equalsIgnoreCase(HTTPConstants.COMPRESSION_GZIP))
            {
                in = new GZIPInputStream(in);
            }
            else
            {
                throw new AxisFault("HTTP :"
                    + "unsupported content-encoding of '"
                    + contentEncoding
                    + "' found");
            }
        }

        if (msgContext.getOperationContext() != null)
        {
            msgContext.getOperationContext()
                    .setProperty(MessageContext.TRANSPORT_IN, in);
        }

    }

    /**
     * Collect the HTTP header information and set them in the message context
     * 
     * @param urlCon
     * @param msgContext
     */
    protected void obtainHTTPHeaderInformation(HttpURLConnection urlCon,
                                               MessageContext msgContext)
        throws AxisFault
    {
        msgContext.setProperty(MessageContext.TRANSPORT_HEADERS, urlCon
                .getHeaderFields());
        String header = urlCon
                .getHeaderField(HTTPConstants.HEADER_CONTENT_TYPE);

        if (header != null)
        {
            HeaderElement[] headers = HeaderElement.parseElements(header);
            MessageContext inMessageContext = msgContext.getOperationContext()
                    .getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE);

            Object contentType = header;
            Object charSetEnc = null;

            for (int i = 0; i < headers.length; i++)
            {
                NameValuePair charsetEnc = headers[i]
                        .getParameterByName(HTTPConstants.CHAR_SET_ENCODING);
                if (charsetEnc != null)
                {
                    charSetEnc = charsetEnc.getValue();
                }
            }

            if (inMessageContext != null)
            {
                inMessageContext
                        .setProperty(Constants.Configuration.CONTENT_TYPE,
                                     contentType);
                inMessageContext
                        .setProperty(Constants.Configuration.CHARACTER_SET_ENCODING,
                                     charSetEnc);
            }
            else
            {

                // Transport details will be stored in a HashMap so that anybody
                // interested can retriece them
                Map transportInfoMap = new HashMap();
                transportInfoMap.put(Constants.Configuration.CONTENT_TYPE,
                                     contentType);
                transportInfoMap
                        .put(Constants.Configuration.CHARACTER_SET_ENCODING,
                             charSetEnc);

                // the HashMap is stored in the outgoing message.
                msgContext
                        .setProperty(Constants.Configuration.TRANSPORT_INFO_MAP,
                                     transportInfoMap);
            }
        }

        CookieContainer cc = urlCon.getCookieContainer();
        String sessionCookie = null;

        Iterator it = cc.iterator();
        while (it.hasNext())
        {
            Cookie c = (Cookie)it.next();
            if (c.getName().equalsIgnoreCase(Constants.SESSION_COOKIE)
                || c.getName()
                        .equalsIgnoreCase(Constants.SESSION_COOKIE_JSESSIONID))
            {
                sessionCookie = c.getValue();
                break;
            }
        }

        if (sessionCookie != null)
        {
            msgContext.getServiceContext()
                    .setProperty(HTTPConstants.COOKIE_STRING, sessionCookie);
        }
    }

    private boolean isProxyListed(MessageContext msgCtx) throws AxisFault
    {
        boolean returnValue = false;
        Parameter par = null;

        proxyOutSetting = msgCtx.getConfigurationContext()
                .getAxisConfiguration()
                .getTransportOut(Constants.TRANSPORT_HTTP);

        if (proxyOutSetting != null)
        {
            par = proxyOutSetting.getParameter(HTTPConstants.PROXY);
        }

        OMElement hostElement = null;

        if (par != null)
        {
            hostElement = par.getParameterElement();
        }

        if (hostElement != null)
        {
            Iterator ite = hostElement.getAllAttributes();

            while (ite.hasNext())
            {
                OMAttribute attribute = (OMAttribute)ite.next();

                if (attribute.getLocalName().equalsIgnoreCase(PROXY_HOST_NAME))
                {
                    returnValue = true;
                }
            }
        }

        HttpTransportProperties.ProxyProperties proxyProperties;

        if ((proxyProperties = (HttpTransportProperties.ProxyProperties)msgCtx
                .getProperty(HTTPConstants.PROXY)) != null)
        {
            if (proxyProperties.getProxyHostName() != null)
            {
                returnValue = true;
            }
        }

        return returnValue;
    }

    protected boolean isAuthenticationEnabled(MessageContext msgCtx)
    {
        return (msgCtx.getProperty(HTTPConstants.AUTHENTICATE) != null);
    }

    /*
     * This will handle server Authentication, It could be either NTLM, Digest
     * or Basic Authentication. Apart from that user can change the priory or
     * add a custom authentication scheme.
     */
    protected void setAuthenticationInfo(HttpURLConnection urlCon,
                                         MessageContext msgCtx,
                                         UserAgent ua) throws AxisFault
    {
        HttpTransportProperties.Authenticator authenticator;
        Object obj = msgCtx.getProperty(HTTPConstants.AUTHENTICATE);
        if (obj != null)
        {
            if (obj instanceof HttpTransportProperties.Authenticator)
            {
                authenticator = (HttpTransportProperties.Authenticator)obj;

                String username = authenticator.getUsername();
                String password = authenticator.getPassword();
                String host = authenticator.getHost();
                String domain = authenticator.getDomain();

                int port = authenticator.getPort();
                if (port > 0)
                    throw new AxisFault("Authentication scope using port is not supported");
                String realm = authenticator.getRealm();
                if (realm != null)
                    throw new AxisFault("Authentication scope using realm is not supported");

                // We always retry automatically
                // if(authenticator.isAllowedRetry())

                // Don't override the default specification if they are using
                // the HttpUserAgent mechanism -- see the configuration
                // documentation
                if (urlCon.getUserAgent() == null)
                {
                    if (domain != null)
                    {
                        /* Credentials for NTLM Authentication */
                        ua._credential = new NtlmCredential(username,
                                                            password,
                                                            host,
                                                            domain);
                    }
                    else
                    {
                        /* Credentials for Digest and Basic Authentication */
                        ua._credential = new UserCredential(username, password);
                    }

                    if (_log.isDebugEnabled())
                    {
                        _log.debug("Using credential from Axis2: "
                            + ua._credential);
                    }

                    urlCon.setUserAgent(ua);
                }

                List schemes = authenticator.getAuthSchemes();
                if (schemes != null && schemes.size() > 0)
                {
                    if (schemes.size() != 1)
                        throw new AxisFault("Only one authentication scheme is presently supported");

                    String scheme = (String)schemes.get(0);
                    if (HttpTransportProperties.Authenticator.BASIC
                            .equals(scheme))
                    {
                        urlCon.setAuthenticationType(Credential.AUTH_BASIC);
                    }
                    else if (HttpTransportProperties.Authenticator.NTLM
                            .equals(scheme))
                    {
                        urlCon.setAuthenticationType(Credential.AUTH_NTLM);
                    }
                    else if (HttpTransportProperties.Authenticator.DIGEST
                            .equals(scheme))
                    {
                        urlCon.setAuthenticationType(Credential.AUTH_DIGEST);
                    }
                    else
                    {
                        throw new AxisFault("Unknown authentication scheme: "
                            + scheme);
                    }
                }
            }
            else
            {
                throw new AxisFault("HttpTransportProperties.Authenticator class cast exception");
            }
        }
    }

    protected void configProxyAuthentication(HttpURLConnection urlCon,
                                             TransportOutDescription proxySetting,
                                             UserAgent ua,
                                             MessageContext msgCtx)
        throws AxisFault
    {
        Parameter proxyParam = proxySetting.getParameter(HTTPConstants.PROXY);
        String usrName;
        String domain;
        String passwd;
        Credential proxyCred = null;
        String proxyHostName = null;

        if (proxyParam != null)
        {
            String value = (String)proxyParam.getValue();
            String split[] = value.split(":");

            // values being hard coded due best practise
            usrName = split[0];
            domain = split[1];
            passwd = split[2];

            OMElement proxyParamElement = proxyParam.getParameterElement();
            Iterator ite = proxyParamElement.getAllAttributes();

            while (ite.hasNext())
            {
                OMAttribute att = (OMAttribute)ite.next();

                if (att.getLocalName().equalsIgnoreCase(PROXY_HOST_NAME))
                {
                    urlCon.setConnectionProxyHost(att.getAttributeValue());
                }

                if (att.getLocalName().equalsIgnoreCase(PROXY_PORT))
                {
                    urlCon.setConnectionProxyPort(Integer.parseInt(att
                            .getAttributeValue()));
                }
            }

            if (domain.length() == 0 || domain.equals(ANONYMOUS))
            {
                if (usrName.equals(ANONYMOUS) && passwd.equals(ANONYMOUS))
                {
                    proxyCred = new UserCredential("", "");
                }
                else
                {
                    proxyCred = new UserCredential(usrName, passwd);
                }
            }
            else
            {
                proxyCred = new NtlmCredential(usrName,
                                               passwd,
                                               proxyHostName,
                                               domain);
            }
        }

        HttpTransportProperties.ProxyProperties proxyProperties = (HttpTransportProperties.ProxyProperties)msgCtx
                .getProperty(HTTPConstants.PROXY);

        if (proxyProperties != null)
        {
            if (proxyProperties.getProxyPort() != -1)
            {
                urlCon.setConnectionProxyPort(proxyProperties.getProxyPort());
            }

            proxyHostName = proxyProperties.getProxyHostName();
            if (proxyHostName == null || proxyHostName.length() == 0)
            {
                throw new AxisFault("Proxy Name is not valid");
            }
            urlCon.setConnectionProxyHost(proxyHostName);

            if (proxyProperties.getUserName().equals(ANONYMOUS)
                || proxyProperties.getPassWord().equals(ANONYMOUS))
            {
                proxyCred = new UserCredential("", "");
            }
            if (!proxyProperties.getUserName().equals(ANONYMOUS)
                && !proxyProperties.getPassWord().equals(ANONYMOUS))
            {
                proxyCred = new UserCredential(proxyProperties.getUserName()
                        .trim(), proxyProperties.getPassWord().trim()); // Basic
                // Authentication
            }
            if (!proxyProperties.getDomain().equals(ANONYMOUS))
            {
                if (!proxyProperties.getUserName().equals(ANONYMOUS)
                    && !proxyProperties.getPassWord().equals(ANONYMOUS)
                    && !proxyProperties.getDomain().equals(ANONYMOUS))
                {
                    proxyCred = new NtlmCredential(proxyProperties
                                                           .getUserName()
                                                           .trim(),
                                                   proxyProperties
                                                           .getPassWord()
                                                           .trim(),
                                                   proxyHostName,
                                                   proxyProperties.getDomain()
                                                           .trim()); // NTLM
                    // Authentication
                }
            }
        }

        if (proxyCred != null)
        {
            ua._proxyCredential = proxyCred;
        }
    }

}
