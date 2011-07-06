/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oaklandsw.http.webapp;

import com.oaklandsw.http.HttpTestEnv;
import com.oaklandsw.http.HttpURLConnection;
import com.oaklandsw.http.TestUserAgent;
import com.oaklandsw.http.axis.OaklandHTTPSender;

import com.oaklandsw.utillog.Log;
import com.oaklandsw.util.LogUtils;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.axis.Constants;
import org.apache.axis.EngineConfiguration;
import org.apache.axis.Handler;
import org.apache.axis.Message;
import org.apache.axis.MessageContext;
import org.apache.axis.SimpleChain;
import org.apache.axis.SimpleTargetedChain;
import org.apache.axis.client.AxisClient;
import org.apache.axis.client.Call;
import org.apache.axis.configuration.SimpleProvider;
import org.apache.axis.encoding.ser.ElementDeserializer;
import org.apache.axis.encoding.ser.ElementDeserializerFactory;
import org.apache.axis.encoding.ser.ElementSerializerFactory;
import org.apache.axis.encoding.ser.SimpleDeserializer;
import org.apache.axis.handlers.SimpleSessionHandler;
import org.apache.axis.transport.http.HTTPTransport;
import org.apache.axis.wsdl.gen.Parser;
import org.apache.axis.wsdl.symbolTable.BaseType;
import org.apache.axis.wsdl.symbolTable.BindingEntry;
import org.apache.axis.wsdl.symbolTable.Parameter;
import org.apache.axis.wsdl.symbolTable.Parameters;
import org.apache.axis.wsdl.symbolTable.ServiceEntry;
import org.apache.axis.wsdl.symbolTable.SymTabEntry;
import org.apache.axis.wsdl.symbolTable.SymbolTable;
import org.apache.axis.wsdl.symbolTable.TypeEntry;

import java.io.File;

import java.net.URL;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.wsdl.Binding;
import javax.wsdl.Operation;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.extensions.soap.SOAPAddress;

import javax.xml.namespace.QName;
import javax.xml.rpc.encoding.Deserializer;
import javax.xml.rpc.encoding.DeserializerFactory;
import javax.xml.soap.MimeHeaders;


public class TestAxis1 extends TestWebappBase {
    private static final Log _log = LogUtils.makeLogger();
    private Parser _wsdlParser;
    private MessageContext _messageContext;
    protected String _user;
    protected String _password;

    public TestAxis1(String testName) {
        super(testName);

        // Netproxy cannot deal with this as it closes the connection after
        // the POST, probably because of a non-standard header.
        _doAuthCloseProxyTest = false;
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(TestAxis1.class);

        return suite;
    }

    public static void main(String[] args) {
        mainRun(suite(), args);
    }

    public void setUp() throws Exception {
        super.setUp();
        HttpURLConnection.setDefaultUserAgent(null);
        _password = null;
        _user = null;
    }

    // Returns the result
    public Map invokeService(String wsdlLocation, String operationName,
        String[] args) throws Exception {
        SimpleProvider clientConfig = new SimpleProvider();
        Handler sessionHandler = new SimpleSessionHandler();
        SimpleChain reqHandler = new SimpleChain();
        SimpleChain respHandler = new SimpleChain();
        reqHandler.addHandler(sessionHandler);
        respHandler.addHandler(sessionHandler);

        Handler pivot = new OaklandHTTPSender();

        Handler transport = new SimpleTargetedChain(reqHandler, pivot,
                respHandler);
        clientConfig.deployTransport(HTTPTransport.DEFAULT_TRANSPORT_NAME,
            transport);

        _wsdlParser = new Parser();
        // System.out.println("Reading WSDL document from '" + wsdlLocation +
        // "'");
        _wsdlParser.run(wsdlLocation);

        String portName = null;

        try {
            if (operationName.indexOf("(") > 0) {
                portName = operationName.substring(operationName.indexOf("(") +
                        1, operationName.indexOf(")"));
                operationName = operationName.substring(0,
                        operationName.indexOf("("));
            }
        } catch (Exception ignored) {
        }

        HashMap map = invokeMethod(clientConfig, operationName, portName, args);

        // for (Iterator it = map.entrySet().iterator(); it.hasNext();)
        // {
        // Map.Entry entry = (Map.Entry)it.next();
        // String key = (String)entry.getKey();
        // Object value = entry.getValue();
        // if (value instanceof Element)
        // {
        // System.out.println("====== " + key + " ======");
        // XMLUtils.ElementToStream((Element)value, System.out);
        // System.out.println("=========================");
        // }
        // else
        // {
        // System.out.println(key + "=" + value);
        // }
        // }
        return map;
    }

    public String[] getCookies() {
        Message m = _messageContext.getResponseMessage();
        MimeHeaders mh = m.getMimeHeaders();

        return mh.getHeader("Set-Cookie");
    }

    public HashMap invokeMethod(EngineConfiguration eConfig,
        String operationName, String portName, String[] args)
        throws Exception {
        String serviceNS = null;
        String serviceName = null;

        SimpleProvider clientConfig = new SimpleProvider();
        Handler sessionHandler = new SimpleSessionHandler();
        SimpleChain reqHandler = new SimpleChain();
        SimpleChain respHandler = new SimpleChain();
        reqHandler.addHandler(sessionHandler);
        respHandler.addHandler(sessionHandler);

        Handler pivot = new OaklandHTTPSender();
        Handler transport = new SimpleTargetedChain(reqHandler, pivot,
                respHandler);
        clientConfig.deployTransport(HTTPTransport.DEFAULT_TRANSPORT_NAME,
            transport);

        Service service = selectService(serviceNS, serviceName);
        Operation operation = null;

        org.apache.axis.client.Service dpf = new org.apache.axis.client.Service(_wsdlParser,
                service.getQName());

        dpf.setEngineConfiguration(clientConfig);
        dpf.setEngine(new AxisClient(clientConfig));

        Vector inputs = new Vector();
        Port port1 = selectPort(service.getPorts(), portName);

        if (portName == null) {
            portName = port1.getName();
        }

        Binding binding = port1.getBinding();
        Call call = (Call) dpf.createCall(QName.valueOf(portName),
                QName.valueOf(operationName));

        if (_user != null) {
            call.setUsername(_user);
        }

        if (_password != null) {
            call.setPassword(_password);
        }

        call.setTimeout(new Integer(15 * 1000));
        call.setProperty(ElementDeserializer.DESERIALIZE_CURRENT_ELEMENT,
            Boolean.TRUE);

        // Output types and names
        Vector outNames = new Vector();

        // Input types and names
        Vector inNames = new Vector();
        Vector inTypes = new Vector();
        SymbolTable symbolTable = _wsdlParser.getSymbolTable();
        BindingEntry bEntry = symbolTable.getBindingEntry(binding.getQName());
        Parameters parameters = null;
        Iterator i = bEntry.getParameters().keySet().iterator();

        while (i.hasNext()) {
            Operation o = (Operation) i.next();

            if (o.getName().equals(operationName)) {
                operation = o;
                parameters = (Parameters) bEntry.getParameters().get(o);

                break;
            }
        }

        if ((operation == null) || (parameters == null)) {
            throw new RuntimeException(operationName + " was not found.");
        }

        // loop over paramters and set up in/out params
        for (int j = 0; j < parameters.list.size(); ++j) {
            Parameter p = (Parameter) parameters.list.get(j);

            if (p.getMode() == 1) { // IN
                inNames.add(p.getQName().getLocalPart());
                inTypes.add(p);
            } else if (p.getMode() == 2) { // OUT
                outNames.add(p.getQName().getLocalPart());
            } else if (p.getMode() == 3) { // INOUT
                inNames.add(p.getQName().getLocalPart());
                inTypes.add(p);
                outNames.add(p.getQName().getLocalPart());
            }
        }

        // set output type
        if (parameters.returnParam != null) {
            if (!parameters.returnParam.getType().isBaseType()) {
                call.registerTypeMapping(org.w3c.dom.Element.class,
                    parameters.returnParam.getType().getQName(),
                    new ElementSerializerFactory(),
                    new ElementDeserializerFactory());
            }

            // Get the QName for the return Type
            // QName returnType1 = org.apache.axis.wsdl.toJava.Utils
            // .getXSIType(parameters.returnParam);
            QName returnQName = parameters.returnParam.getQName();

            outNames.add(returnQName.getLocalPart());
        }

        if (inNames.size() != args.length) {
            throw new RuntimeException("Need " + inNames.size() +
                " arguments!!!");
        }

        for (int pos = 0; pos < inNames.size(); ++pos) {
            String arg = args[pos];
            Parameter p = (Parameter) inTypes.get(pos);
            inputs.add(getParamData(call, p, arg));
        }

        // System.out.println("Executing operation "
        // + operationName
        // + " with parameters:");
        // for (int j = 0; j < inputs.size(); j++)
        // {
        // System.out.println(inNames.get(j) + "=" + inputs.get(j));
        // }
        Object ret = call.invoke(inputs.toArray());

        _messageContext = call.getMessageContext();

        Map outputs = call.getOutputParams();
        HashMap map = new HashMap();

        for (int pos = 0; pos < outNames.size(); ++pos) {
            String name = (String) outNames.get(pos);
            Object value = outputs.get(name);

            if ((value == null) && (pos == 0)) {
                map.put(name, ret);
            } else {
                map.put(name, value);
            }
        }

        return map;
    }

    /**
     * Method getParamData
     *
     * @param c
     * @param arg
     */
    private Object getParamData(org.apache.axis.client.Call c, Parameter p,
        String arg) throws Exception {
        // Get the QName representing the parameter type
        QName paramType = org.apache.axis.wsdl.toJava.Utils.getXSIType(p);

        TypeEntry type = p.getType();

        if (type instanceof BaseType && ((BaseType) type).isBaseType()) {
            DeserializerFactory factory = c.getTypeMapping()
                                           .getDeserializer(paramType);
            Deserializer deserializer = factory.getDeserializerAs(Constants.AXIS_SAX);

            if (deserializer instanceof SimpleDeserializer) {
                return ((SimpleDeserializer) deserializer).makeValue(arg);
            }
        }

        throw new RuntimeException("not know how to convert '" + arg +
            "' into " + c);
    }

    /**
     * Method selectService
     *
     * @param def
     * @param serviceNS
     * @param serviceName
     *
     * @return
     *
     * @throws Exception
     */
    public Service selectService(String serviceNS, String serviceName)
        throws Exception {
        QName serviceQName = (((serviceNS != null) && (serviceName != null))
            ? new QName(serviceNS, serviceName) : null);
        ServiceEntry serviceEntry = (ServiceEntry) getSymTabEntry(serviceQName,
                ServiceEntry.class);

        return serviceEntry.getService();
    }

    /**
     * Method getSymTabEntry
     *
     * @param qname
     * @param cls
     *
     * @return
     */
    public SymTabEntry getSymTabEntry(QName qname, Class cls) {
        HashMap map = _wsdlParser.getSymbolTable().getHashMap();
        Iterator iterator = map.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();

            // QName key = (QName)entry.getKey();
            Vector v = (Vector) entry.getValue();

            if ((qname == null) || qname.equals(qname)) {
                for (int i = 0; i < v.size(); ++i) {
                    SymTabEntry symTabEntry = (SymTabEntry) v.elementAt(i);

                    if (cls.isInstance(symTabEntry)) {
                        return symTabEntry;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Method selectPort
     *
     * @param ports
     * @param portName
     *
     * @return
     *
     * @throws Exception
     */
    public Port selectPort(Map ports, String portName)
        throws Exception {
        Iterator valueIterator = ports.keySet().iterator();

        while (valueIterator.hasNext()) {
            String name = (String) valueIterator.next();

            if ((portName == null) || (portName.length() == 0)) {
                Port port1 = (Port) ports.get(name);
                List list = port1.getExtensibilityElements();

                for (int i = 0; (list != null) && (i < list.size()); i++) {
                    Object obj = list.get(i);

                    if (obj instanceof SOAPAddress) {
                        return port1;
                    }
                }
            } else if ((name != null) && name.equals(portName)) {
                return (Port) ports.get(name);
            }
        }

        return null;
    }

    public void testWsWebappVersion() throws Exception {
        Map map = invokeService(HttpTestEnv.TEST_URL_HOST_WEBAPP +
                "axis/services/Version?wsdl", "getVersion", new String[] {  });
        assertEquals(1, map.size());
    }

    public void testWsWebappVerison3x() throws Exception {
        HttpURLConnection.setMaxConnectionsPerHost(1);

        URL url = new URL(HttpTestEnv.TEST_URL_HOST_WEBAPP);
        testWsWebappVersion();
        testWsWebappVersion();
        checkNoActiveConns(url);
    }

    public void testWsWebappList() throws Exception {
        Map map = invokeService(HttpTestEnv.TEST_URL_HOST_WEBAPP +
                "axis/EchoHeaders.jws?wsdl", "list", new String[] {  });
        assertEquals(1, map.size());

        String[] cookie = getCookies();
        assertTrue(cookie[0].startsWith("JSESSIONID"));
    }

    public void testWsBasic() throws Exception {
        String msg = "Hello World!!!";
        Map map = invokeService("http://mssoapinterop.org/asmx/xsd/round4XSD.wsdl",
                "echoString", new String[] { msg });
        String response = (String) map.get(">echoStringResponse>return");
        assertEquals(msg, response);
        assertEquals(1, map.size());
    }

    protected String getIISUrl() {
        File wFile = new File(HttpTestEnv.getHttpTestRoot() + File.separator +
                "iiswebservice" + File.separator + "HelloWorld.wsdl");

        return wFile.toURI().toString();
    }

    protected Map invokeWindowsService() throws Exception {
        return invokeService(getIISUrl(), "sayHello", new String[] { "Francis" });
    }

    public void testWsIISNtlmOk() throws Exception {
        // Use Axis authentication
        _user = HttpTestEnv.TEST_IIS_DOMAIN_USER;
        _password = HttpTestEnv.TEST_IIS_PASSWORD;

        // logAll();
        Map map = invokeWindowsService();
        assertEquals(1, map.size());
    }

    public void testWsIISNtlmOkUserAgent() throws Exception {
        // User the HttpUserAgent authentication
        com.oaklandsw.http.HttpURLConnection.setDefaultUserAgent(new com.oaklandsw.http.TestUserAgent());
        TestUserAgent._type = TestUserAgent.GOOD;

        Map map = invokeWindowsService();
        assertEquals(1, map.size());
    }

    // This test is flaky, not sure why, but it's not our problem
    public void DISABLEDtestWsGoogle() throws Exception {
        invokeService("http://api.google.com/GoogleSearch.wsdl",
            "doGoogleSearch",
            new String[] {
                "Ywx8jV1QFHIbfnz/MxfxdPfNHsdxtvQN", "oakland software", "0",
                "10", "", "", "", "", "", ""
            });

        // Don't care about checking the result
    }

    public void testWsAmazon() throws Exception {
        // Not used yet
        if (true) {
            return;
        }

        invokeService("http://webservices.amazon.com/AWSECommerceService/AWSECommerceService.wsdl",
            "Help", new String[] {  });

        // Don't care about checking the result
    }

    public void testWsIISNtlmFail() throws Exception {
        try {
            _user = null;
            _password = null;
            com.oaklandsw.http.HttpURLConnection.setDefaultUserAgent(null);
            com.oaklandsw.http.HttpURLConnection.closeAllPooledConnections();
            invokeWindowsService();
            fail("This was supposed to fail due to auth problems");
        } catch (Exception ex) {
            assertTrue(ex.getMessage().indexOf("401") >= 0);
        }
    }

    public void allTestMethods() throws Exception {
        testWsBasic();

        // testWsGoogle();
        if (!isAllowNtlmProxy()) {
            return;
        }

        testWsIISNtlmOk();
        testWsIISNtlmFail();
    }
}
