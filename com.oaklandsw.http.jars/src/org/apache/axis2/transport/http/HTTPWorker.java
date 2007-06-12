/*
* Copyright 2004,2005 The Apache Software Foundation.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.apache.axis2.transport.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.axis2.Constants;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.deployment.DeploymentConstants;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.engine.Handler.InvocationResponse;
import org.apache.axis2.transport.RequestResponseTransport;
import org.apache.axis2.transport.http.server.AxisHttpRequest;
import org.apache.axis2.transport.http.server.AxisHttpResponse;
import org.apache.axis2.transport.http.server.HttpUtils;
import org.apache.axis2.transport.http.server.Worker;
import org.apache.axis2.transport.http.util.RESTUtil;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpStatus;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EncodingUtils;
import org.apache.ws.commons.schema.XmlSchema;

public class HTTPWorker implements Worker {

    public HTTPWorker() {
    }

    public void service(
            final AxisHttpRequest request,
            final AxisHttpResponse response,
            final MessageContext msgContext) throws HttpException, IOException {

        ConfigurationContext configurationContext = msgContext.getConfigurationContext();
        final String servicePath = configurationContext.getServiceContextPath();
        final String contextPath =
                (servicePath.startsWith("/") ? servicePath : "/" + servicePath) + "/";

        String uri = request.getRequestURI();
        String method = request.getMethod();
        String soapAction = HttpUtils.getSoapAction(request);
        InvocationResponse pi;

        if (method.equals(HTTPConstants.HEADER_GET)) {
            if (uri.equals("/favicon.ico")) {
                response.setStatus(HttpStatus.SC_MOVED_PERMANENTLY);
                response.addHeader(new BasicHeader("Location", "http://ws.apache.org/favicon.ico"));
                return;
            }
            if (!uri.startsWith(contextPath)) {
                response.setStatus(HttpStatus.SC_MOVED_PERMANENTLY);
                response.addHeader(new BasicHeader("Location", contextPath));
                return;
            }
            if (uri.endsWith("axis2/services/")) {
                String s = HTTPTransportReceiver.getServicesHTML(configurationContext);
                response.setStatus(HttpStatus.SC_OK);
                response.setContentType("text/html");
                OutputStream out = response.getOutputStream();
                out.write(EncodingUtils.getBytes(s, HTTP.ISO_8859_1));
                return;
            }
            if (uri.indexOf("?") < 0) {
                if (!uri.endsWith(contextPath)) {
                    String serviceName = uri.replaceAll(contextPath, "");
                    if (serviceName.indexOf("/") < 0) {
                        String s = HTTPTransportReceiver
                                .printServiceHTML(serviceName, configurationContext);
                        response.setStatus(HttpStatus.SC_OK);
                        response.setContentType("text/html");
                        OutputStream out = response.getOutputStream();
                        out.write(EncodingUtils.getBytes(s, HTTP.ISO_8859_1));
                        return;
                    }
                }
            }
            if (uri.endsWith("?wsdl2")) {
                String serviceName = uri.substring(uri.lastIndexOf("/") + 1, uri.length() - 6);
                HashMap services = configurationContext.getAxisConfiguration().getServices();
                AxisService service = (AxisService) services.get(serviceName);
                if (service != null) {
                    String ip = getHostAddress(request);
                    response.setStatus(HttpStatus.SC_OK);
                    response.setContentType("text/xml");
                    service.printWSDL2(response.getOutputStream(), ip, servicePath);
                    return;
                }
            }
            if (uri.endsWith("?wsdl")) {
                String serviceName = uri.substring(uri.lastIndexOf("/") + 1, uri.length() - 5);
                HashMap services = configurationContext.getAxisConfiguration().getServices();
                AxisService service = (AxisService) services.get(serviceName);
                if (service != null) {
                    String ip = getHostAddress(request);
                    response.setStatus(HttpStatus.SC_OK);
                    response.setContentType("text/xml");
                    service.printWSDL(response.getOutputStream(), ip, servicePath);
                    return;
                }
            }
            if (uri.endsWith("?xsd")) {
                String serviceName = uri.substring(uri.lastIndexOf("/") + 1, uri.length() - 4);
                HashMap services = configurationContext.getAxisConfiguration().getServices();
                AxisService service = (AxisService) services.get(serviceName);
                if (service != null) {
                    response.setStatus(HttpStatus.SC_OK);
                    response.setContentType("text/xml");
                    service.printSchema(response.getOutputStream());
                    return;
                }
            }
            //cater for named xsds - check for the xsd name
            if (uri.indexOf("?xsd=") > 0) {
                String serviceName =
                        uri.substring(uri.lastIndexOf("/") + 1, uri.lastIndexOf("?xsd="));
                String schemaName = uri.substring(uri.lastIndexOf("=") + 1);

                HashMap services = configurationContext.getAxisConfiguration().getServices();
                AxisService service = (AxisService) services.get(serviceName);
                if (service != null) {
                    //run the population logic just to be sure
                    service.populateSchemaMappings();
                    //write out the correct schema
                    Map schemaTable = service.getSchemaMappingTable();
                    XmlSchema schema = (XmlSchema) schemaTable.get(schemaName);
                    //schema found - write it to the stream
                    if (schema != null) {
                        response.setStatus(HttpStatus.SC_OK);
                        response.setContentType("text/xml");
                        schema.write(response.getOutputStream());
                        return;
                    } else {
                        InputStream instream = service.getClassLoader()
                            .getResourceAsStream(DeploymentConstants.META_INF + "/" + schemaName);
                        
                        if (instream != null) {
                            response.setStatus(HttpStatus.SC_OK);
                            response.setContentType("text/xml");
                            OutputStream outstream = response.getOutputStream();
                            boolean checkLength = true;
                            int length = Integer.MAX_VALUE;
                            int nextValue = instream.read();
                            if (checkLength) length--;
                            while (-1 != nextValue && length >= 0) {
                                outstream.write(nextValue);
                                nextValue = instream.read();
                                if (checkLength) length--;
                            }
                            outstream.flush();
                            return;
                        } else {
                            // no schema available by that name  - send 404
                            response.sendError(HttpStatus.SC_NOT_FOUND, "Schema Not Found!");
                            return;
                        }
                    }
                }
            }

            String contentType = null;
            Header[] headers = request.getHeaders(HTTPConstants.HEADER_CONTENT_TYPE);
            if (headers != null && headers.length > 0) {
                contentType = headers[0].getValue();
                int index = contentType.indexOf(';');
                if (index > 0) {
                    contentType = contentType.substring(0, index);
                }
            }
            
            // deal with GET request
            pi = RESTUtil.processURLRequest(
                    msgContext, 
                    response.getOutputStream(), 
                    contentType);

        } else if (method.equals(HTTPConstants.HEADER_POST)) {
            // deal with POST request

            String contentType = request.getContentType();
            
            if (HTTPTransportUtils.isRESTRequest(contentType)) {
                pi = RESTUtil.processXMLRequest(
                        msgContext, 
                        request.getInputStream(),
                        response.getOutputStream(), 
                        contentType);
            } else {
                String ip = (String)msgContext.getProperty(MessageContext.TRANSPORT_ADDR);
                if (ip != null){
                    uri = ip + uri;
                }
                pi = HTTPTransportUtils.processHTTPPostRequest(
                        msgContext, 
                        request.getInputStream(),
                        response.getOutputStream(),
                        contentType, 
                        soapAction, 
                        uri);
            }

        } else if (method.equals(HTTPConstants.HEADER_PUT)) {

            String contentType = request.getContentType();
            msgContext.setProperty(Constants.Configuration.CONTENT_TYPE, contentType);
            
            pi = RESTUtil.processXMLRequest(
                    msgContext, 
                    request.getInputStream(),
                    response.getOutputStream(), 
                    contentType);

        } else if (method.equals(HTTPConstants.HEADER_DELETE)) {

            pi = RESTUtil.processURLRequest(
                    msgContext, 
                    response.getOutputStream(), 
                    null);

        } else {
            throw new MethodNotSupportedException(method + " method not supported");
        }
        
        Boolean holdResponse =
            (Boolean) msgContext.getProperty(RequestResponseTransport.HOLD_RESPONSE);
        if (pi.equals(InvocationResponse.SUSPEND) ||
                (holdResponse != null && Boolean.TRUE.equals(holdResponse))) {
            try {
                ((RequestResponseTransport) msgContext
                        .getProperty(RequestResponseTransport.TRANSPORT_CONTROL)).awaitResponse();
            }
            catch (InterruptedException e) {
                throw new IOException("We were interrupted, so this may not function correctly:" +
                        e.getMessage());
            }
        }
        
        // Finalize response
        OperationContext operationContext = msgContext.getOperationContext();
        Object contextWritten = null;
        Object isTwoChannel = null;
        if (operationContext != null) {
            contextWritten = operationContext.getProperty(Constants.RESPONSE_WRITTEN);
            isTwoChannel = operationContext.getProperty(Constants.DIFFERENT_EPR);
        }


        if ((contextWritten != null) && Constants.VALUE_TRUE.equals(contextWritten)) {
            if ((isTwoChannel != null) && Constants.VALUE_TRUE.equals(isTwoChannel)) {
                response.setStatus(HttpStatus.SC_ACCEPTED);
            }
        } else {
            response.setStatus(HttpStatus.SC_ACCEPTED);
        }
    }

    public String getHostAddress(AxisHttpRequest request) throws java.net.SocketException {
        try {
            Header hostHeader = request.getFirstHeader("host");
            if (hostHeader != null) {
                String host = hostHeader.getValue();
                return new URI("http://" + host).getHost();
            }
        } catch (Exception e) {

        }
        return HttpUtils.getIpAddress();
    }

}
