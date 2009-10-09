package com.oaklandsw.http.webapp;

import com.oaklandsw.http.Credential;
import com.oaklandsw.http.HttpTestEnv;
import com.oaklandsw.http.HttpURLConnection;
import com.oaklandsw.http.TestUserAgent;
import com.oaklandsw.http.axis2.OaklandHTTPTransportSender2;

import com.oaklandsw.util.Log;
import com.oaklandsw.util.LogUtils;
import com.oaklandsw.util.Util;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.transport.TransportSender;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.transport.http.HttpTransportProperties;

import java.io.File;
import java.io.StringWriter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import java.net.URL;

import javax.xml.stream.XMLOutputFactory;

public class TestAxis2 extends TestWebappBase
{
    private static final Log          _log = LogUtils.makeLogger();
    protected static Class            _stubClass;
    protected static Class            _stubClassGetListCollectionResult;
    protected static Class            _stubClassGetListCollection;
    protected static Class            _stubClassGetListCollectionResponse;
    protected static Constructor      _stubConstructor;
    protected static Constructor      _stubGetListCollection;
    protected static Method           _stubGetServiceClient;
    protected static Method           _stubGetListCollectionResult;
    protected static Method           _stubGetListCollectionReq;
    protected static Method           _stubGetExtraElement;

    protected static Method           _cleanup;

    static
    {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        try
        {
            _stubClass = cl.loadClass("sharepoint.ListsStub");
        }
        catch (ClassNotFoundException e)
        {
            try
            {
                // Try another one
                _stubClass = cl.loadClass("sharepoint.ListsListsSoapStub");
            }
            catch (Exception ex)
            {
                Util.impossible(ex);
            }
        }

        try
        {
            Class[] clss = _stubClass.getDeclaredClasses();

            for (int i = 0; i < clss.length; i++)
            {
                // System.out.println(clss[i].getName());

                if (clss[i].getName().indexOf("GetListCollectionResult_type") > 0)
                {
                    _stubClassGetListCollectionResult = clss[i];
                }
                else if (clss[i].getName().endsWith("GetListCollection"))
                {
                    _stubClassGetListCollection = clss[i];
                }
                else if (clss[i].getName()
                        .endsWith("GetListCollectionResponse"))
                {
                    _stubClassGetListCollectionResponse = clss[i];
                }
            }

            _stubConstructor = _stubClass
                    .getConstructor(new Class[] { String.class });
            _stubGetServiceClient = _stubClass.getMethod("_getServiceClient",
                                                         new Class[] {});
            _stubGetListCollection = _stubClass.getConstructor(new Class[] {});
            _stubGetListCollectionResult = _stubClassGetListCollectionResponse
                    .getMethod("getGetListCollectionResult", new Class[] {});
            try
            {
                _stubGetListCollectionReq = _stubClass
                        .getMethod("GetListCollection",
                                   new Class[] { _stubClassGetListCollection });
            }
            catch (NoSuchMethodException ex)
            {
                _stubGetListCollectionReq = _stubClass
                        .getMethod("getListCollection",
                                   new Class[] { _stubClassGetListCollection });

            }

            _stubGetExtraElement = _stubClassGetListCollectionResult
                    .getMethod("getExtraElement", new Class[] {});

            try
            {
                _cleanup = Options.class
                        .getMethod("setCallTransportCleanup",
                                   new Class[] { boolean.class });
            }
            catch (Exception ex)
            {
                // Not found in Axis 1.2
            }
        }
        catch (Exception ex)
        {
            Util.impossible(ex);
        }
    }

    protected String                  _user;
    protected String                  _password;

    // True if SOAP 1.1 is used, false if SOAP 1.2 is used
    protected boolean                 _soap11;
    protected ServiceContext          _serviceContext;
    protected TransportOutDescription _transportOut;
    protected AxisConfiguration       _axisConfig;
    protected ServiceClient           _serviceClient;
    protected AxisService             _axisService;
    protected Options                 _options;
    protected ConfigurationContext    _configContext;
    Object                            _stub;

    public TestAxis2(String testName)
    {
        super(testName);

        // Netproxy cannot deal with this as it closes the connection after
        // the POST, probably because of a non-standard header.
        _doAuthCloseProxyTest = false;
    }

    protected ServiceClient getStubServiceClient() throws Exception
    {
        _stub = _stubConstructor
                .newInstance(new Object[] { HttpTestEnv.TEST_ICEWEB_URL });

        return (ServiceClient)_stubGetServiceClient.invoke(_stub,
                                                           new Object[] {});
    }

    protected void validateStubResponse() throws Exception
    {
        Object req = _stubClassGetListCollection.newInstance();
        Object resp = _stubGetListCollectionReq.invoke(_stub,
                                                       new Object[] { req });

        Object colResult = _stubGetListCollectionResult.invoke(resp,
                                                               new Object[] {});
        Object extraElem = _stubGetExtraElement.invoke(colResult,
                                                       new Object[] {});

        // System.out.println(resp);
        // System.out.println(HexString.dump(output.getBytes()));
        assertContains(extraElem.toString(), "DefaultViewUrl");
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestAxis2.class);

        return suite;
    }

    public static void main(String[] args)
    {
        mainRun(suite(), args);
    }

    public void setUp() throws Exception
    {
        super.setUp();
    }

    public void tearDown() throws Exception
    {
        super.tearDown();
        _axisConfig = null;
        _serviceClient = null;
        _soap11 = false;
    }

    protected TransportOutDescription setupOaklandXport(Options opt,
                                                        AxisConfiguration axisConfig)
        throws Exception
    {
        // Setup up Oakland HTTP Client for transport
        TransportOutDescription transportOut = new TransportOutDescription(Constants.TRANSPORT_HTTP);
        TransportSender transportSender = (TransportSender)OaklandHTTPTransportSender2.class
                .newInstance();
        transportOut.setSender(transportSender);
        axisConfig.addTransportOut(transportOut);
        // This is called below, and by the test case if necessary
        // transportSender.init(_configContext, _transportOut);
        opt.setSenderTransport(Constants.TRANSPORT_HTTP, axisConfig);

        return transportOut;
    }

    protected void initTransport() throws Exception
    {
        _configContext = ConfigurationContextFactory
                .createDefaultConfigurationContext();
        _axisConfig = _configContext.getAxisConfiguration();
        _axisService = new AxisService();

        _options = new Options();

        // Turn this off to get the Commons HTTP transport
        if (true)
        {
            // Setup up Oakland HTTP Client for transport
            _transportOut = setupOaklandXport(_options, _axisConfig);
        }

        // This initializes the transport (among other things)
        _serviceClient = new ServiceClient(_configContext, null);
    }

    // Returns the result
    // args - an array of pairs, first is the arg name, second is the value
    public String invokeService(String endpoint,
                                String namespace,
                                String operationName,
                                String[] args) throws Exception
    {
        // Optionally call this to allow the test to initialize the transport
        // first
        if (_serviceClient == null)
        {
            initTransport();
            _transportOut.getSender().init(_configContext, _transportOut);
        }

        EndpointReference epr = new EndpointReference(endpoint);

        _options.setTo(epr);
        _options.setAction(operationName);

        // _options.setCallTransportCleanup(true);
        if (_cleanup != null)
            _cleanup.invoke(_options, new Object[] { Boolean.TRUE });

        _serviceClient.setOptions(_options);

        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMNamespace omNs = fac.createOMNamespace(namespace, null);
        OMElement method = fac.createOMElement(operationName, omNs);
        OMElement param;

        // This is a pair of name/values
        for (int i = 0; i < args.length; i += 2)
        {
            param = fac.createOMElement(args[i], omNs);
            param.setText(args[i + 1]);
            method.addChild(param);
        }

        _serviceContext = _serviceClient.getServiceContext();

        OMElement result = _serviceClient.sendReceive(method);

        // System.out.println(result);
        StringWriter writer = new StringWriter();
        result.serialize(XMLOutputFactory.newInstance()
                .createXMLStreamWriter(writer));
        writer.flush();

        return writer.toString();
    }

    public void testBadProtocol() throws Exception
    {
        initTransport();

        _transportOut
                .addParameter(new Parameter(HTTPConstants.HTTP_PROTOCOL_VERSION,
                                            "bad"));

        try
        {
            _transportOut.getSender().init(_configContext, _transportOut);
            fail("Did not get expected exception");
        }
        catch (AxisFault ex)
        {
            // Expected
        }
    }

    public void testWsWebappVersion() throws Exception
    {
        String output = invokeService(HttpTestEnv.TEST_URL_HOST_WEBAPP
                                          + "axis/services/Version",
                                      "http://axis.apache.org",
                                      "getVersion",
                                      new String[] {});
        assertContains(output, "Apache Axis version");
    }

    public void testWsWebappVerison3x() throws Exception
    {
        if (_cleanup == null)
        {
            System.out.println("Skipping for Axis 1.2 - method not there");
            return;
        }

        HttpURLConnection.setMaxConnectionsPerHost(1);

        URL url = new URL(HttpTestEnv.TEST_URL_HOST_WEBAPP);

        testWsWebappVersion();
        testWsWebappVersion();
        checkNoActiveConns(url);
    }

    public void testWsWebappList() throws Exception
    {
        String output = invokeService(HttpTestEnv.TEST_URL_HOST_WEBAPP
                                          + "axis/EchoHeaders.jws",
                                      "http://axis.apache.org",
                                      "list",
                                      new String[] {});

        // System.out.println(output);
        assertContains(output, "user-agent:Axis");

        String sessionCookie = (String)_serviceContext.getProperty("Cookie");
        // System.out.println("cookie: " + sessionCookie);
        assertNotNull(sessionCookie);
    }

    public void testWsBasic() throws Exception
    {
        String msg = "Hello World!!!";
        String output = invokeService(HttpTestEnv.HTTP_PROTOCOL
                                          + "//mssoapinterop.org/asmx/xsd/round4xsd.asmx",
                                      "http://soapinterop.org/",
                                      "echoString",
                                      new String[] { "inputString", msg });
        assertContains(output, msg);
    }

    protected String getIISUrl()
    {
        File wFile = new File(HttpTestEnv.getHttpTestRoot()
            + File.separator
            + "iiswebservice"
            + File.separator
            + "HelloWorld.wsdl");

        return wFile.toURI().toString();
    }

    protected String invokeWindowsService() throws Exception
    {
        return invokeService(HttpTestEnv.HTTP_PROTOCOL
            + "//"
            + HttpTestEnv.IIS_HOST_5
            + "/HelloWorld.asmx", "urn:Example1", "sayHello", new String[] {
            "name", "Francis" });
    }

    protected void setupAxisAuth(Options options,
                                 String domain,
                                 String user,
                                 String password)
    {
        HttpTransportProperties.Authenticator auth = new HttpTransportProperties.Authenticator();

        // Use Axis authentication
        auth.setDomain(domain);
        auth.setUsername(user);
        auth.setPassword(password);
        auth.setHost("host");

        options.setProperty(HTTPConstants.AUTHENTICATE, auth);
    }

    public void testWsIISNtlmOkAxisAuth() throws Exception
    {
        HttpURLConnection.setDefaultUserAgent(null);

        initTransport();

        setupAxisAuth(_options,
                      HttpTestEnv.TEST_IIS_DOMAIN,
                      HttpTestEnv.TEST_IIS_USER,
                      HttpTestEnv.TEST_IIS_PASSWORD);

        _transportOut.getSender().init(_configContext, _transportOut);

        String result = invokeWindowsService();
        assertContains(result, "Hello Francis");
    }

    public void testWsIISNtlmOkAxisAuthChunked() throws Exception
    {
        HttpURLConnection.setDefaultUserAgent(null);

        com.oaklandsw.http.HttpURLConnection
                .setDefaultAuthenticationType(Credential.AUTH_NTLM);
        initTransport();

        setupAxisAuth(_options,
                      HttpTestEnv.TEST_IIS_DOMAIN,
                      HttpTestEnv.TEST_IIS_USER,
                      HttpTestEnv.TEST_IIS_PASSWORD);

        _transportOut
                .addParameter(new Parameter(HTTPConstants.HEADER_TRANSFER_ENCODING,
                                            HTTPConstants.HEADER_TRANSFER_ENCODING_CHUNKED));
        _transportOut.getSender().init(_configContext, _transportOut);

        String result = invokeWindowsService();
        assertContains(result, "Hello Francis");
    }

    public void testWsIISNtlmOkUserAgentAuth() throws Exception
    {
        TestUserAgent._type = TestUserAgent.GOOD;

        String result = invokeWindowsService();
        assertContains(result, "Hello Francis");
    }

    public void testSharepointIcewebGood() throws Exception
    {
        TestUserAgent._type = TestUserAgent.OFFICESHARE_ICEWEB;

        String output = invokeService(HttpTestEnv.TEST_ICEWEB_URL,
                                      "http://schemas.microsoft.com/sharepoint/soap/",
                                      "GetListCollection",
                                      new String[] {});

        // System.out.println(HexString.dump(output.getBytes()));
        assertContains(output, "GetListCollectionResponse");
    }

    public void testSharepointIcewebGoodStub() throws Exception
    {
        TestUserAgent._type = TestUserAgent.OFFICESHARE_ICEWEB;

        ServiceClient client = getStubServiceClient();
        Options opt = client.getOptions();
        AxisConfiguration ac = client.getAxisService().getAxisConfiguration();
        setupOaklandXport(opt, ac);

        validateStubResponse();
    }

    public void testSharepointIcewebGoodStub2x() throws Exception
    {
        HttpURLConnection.setMaxConnectionsPerHost(1);

        URL url = new URL(HttpTestEnv.TEST_URL_HOST_WEBAPP);
        testSharepointIcewebGoodStub();
        testSharepointIcewebGoodStub();
        checkNoActiveConns(url);
    }

    public void testSharepointGoodStubChunked(String url,
                                              int userAgentAuth,
                                              String domain,
                                              String user,
                                              String password) throws Exception
    {
        TestUserAgent._type = TestUserAgent.OFFICESHARE_ICEWEB;

        com.oaklandsw.http.HttpURLConnection
                .setDefaultAuthenticationType(Credential.AUTH_NTLM);

        ServiceClient client = getStubServiceClient();
        Options opt = client.getOptions();
        opt.setTimeOutInMilliSeconds(30000);

        if (_soap11)
        {
            opt
                    .setSoapVersionURI(org.apache.axiom.soap.SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI);
        }

        AxisConfiguration ac = client.getAxisService().getAxisConfiguration();
        TransportOutDescription to = setupOaklandXport(opt, ac);

        to
                .addParameter(new Parameter(HTTPConstants.HEADER_TRANSFER_ENCODING,
                                            HTTPConstants.HEADER_TRANSFER_ENCODING_CHUNKED));

        // FIXME - should not have to do this, there should be a way to init the
        // transport before the stub is created, figure this out
        to.getSender().init(client.getServiceContext()
                                    .getConfigurationContext(),
                            to);

        setupAxisAuth(opt,
                      HttpTestEnv.TEST_ICEWEB_DOMAIN,
                      HttpTestEnv.TEST_ICEWEB_USER,
                      HttpTestEnv.TEST_ICEWEB_PASSWORD);
        validateStubResponse();

        // System.out.println(resp);
        // System.out.println(HexString.dump(output.getBytes()));
        // assertContains(output, "GetListCollectionResponse");
    }

    public void testSharepointIcewebGoodStubChunked11() throws Exception
    {
        _soap11 = true;
        testSharepointGoodStubChunked(HttpTestEnv.TEST_ICEWEB_URL,
                                      TestUserAgent.OFFICESHARE_ICEWEB,
                                      HttpTestEnv.TEST_ICEWEB_DOMAIN,
                                      HttpTestEnv.TEST_ICEWEB_USER,
                                      HttpTestEnv.TEST_ICEWEB_PASSWORD);
    }

    public void testSharepointIcewebGoodStubChunked12() throws Exception
    {
        _soap11 = false;
        testSharepointGoodStubChunked(HttpTestEnv.TEST_ICEWEB_URL,
                                      TestUserAgent.OFFICESHARE_ICEWEB,
                                      HttpTestEnv.TEST_ICEWEB_DOMAIN,
                                      HttpTestEnv.TEST_ICEWEB_USER,
                                      HttpTestEnv.TEST_ICEWEB_PASSWORD);
    }

    public void testSharepointXsoGoodStubChunked() throws Exception
    {
        testSharepointGoodStubChunked(HttpTestEnv.TEST_XSOLIVE_URL,
                                      TestUserAgent.OFFICESHARE_XSO,
                                      HttpTestEnv.TEST_XSO_DOMAIN,
                                      HttpTestEnv.TEST_XSO_USER,
                                      HttpTestEnv.TEST_XSO_PASSWORD);
    }

    public void testSharepointIcewebGoodStubAxisAuth() throws Exception
    {
        // This forces it to use the credentials from Axis
        HttpURLConnection.setDefaultUserAgent(null);

        ServiceClient client = getStubServiceClient();
        Options opt = client.getOptions();
        AxisConfiguration ac = client.getAxisService().getAxisConfiguration();
        setupOaklandXport(opt, ac);

        setupAxisAuth(opt,
                      HttpTestEnv.TEST_ICEWEB_DOMAIN,
                      HttpTestEnv.TEST_ICEWEB_USER,
                      HttpTestEnv.TEST_ICEWEB_PASSWORD);

        validateStubResponse();

        // System.out.println(resp);
        // System.out.println(HexString.dump(output.getBytes()));
        // assertContains(output, "GetListCollectionResponse");
    }

    public void testSharepointIcewebBadNoAuth() throws Exception
    {
        HttpURLConnection.setDefaultUserAgent(null);

        try
        {
            invokeService(HttpTestEnv.TEST_ICEWEB_URL,
                          "http://schemas.microsoft.com/sharepoint/soap/",
                          "GetListCollection",
                          new String[] {});
            fail("Expected exception");
        }
        catch (AxisFault ex)
        {
            assertContains(ex.getMessage(), "Unauthorized");
        }
    }

    public void NOtestSharepointXsoLiveBad() throws Exception
    {
        TestUserAgent._type = TestUserAgent.GOOD;

        // xsolive.com
        String output = invokeService("http://74.218.125.36/_vti_bin/Lists.asmx",
                                      "http://schemas.microsoft.com/sharepoint/soap/",
                                      "GetListCollection",
                                      new String[] {});

        // System.out.println(output);
        assertContains(output, "user-agent:Axis2");

        String sessionCookie = (String)_serviceContext.getProperty("Cookie");
        // System.out.println("cookie: " + sessionCookie);
        assertNotNull(sessionCookie);
    }

    // This test is flaky, not sure why, but it's not our problem
    public void testWsGoogle() throws Exception
    {
        if (false)
        {
            // This gets a problem with the namespace specification on the key,
            // it looks like a google problem
            String result = invokeService(HttpTestEnv.HTTP_PROTOCOL
                                              + "//api.google.com/search/beta2",
                                          "urn:GoogleSearch",
                                          "doGoogleSearch",
                                          new String[] {
                                              "key",
                                              "Ywx8jV1QFHIbfnz/MxfxdPfNHsdxtvQN",
                                              "q", "oakland software", "start",
                                              "0", "maxResults", "10",
                                              "filter", "", "restrict", "",
                                              "safeSearch", "", "lr", "", "ie",
                                              "", "oe", "" });

            // Don't care about checking the result
        }
    }

    // Not working 25 Sep 09 FRU
    public void XXtestWsAmazon() throws Exception
    {
        String result = invokeService(HttpTestEnv.HTTP_PROTOCOL
                                          + "//soap.amazon.com/onca/soap?Service=AWSECommerceService",
                                      "http://webservices.amazon.com/AWSECommerceService/2007-05-14",
                                      "Help",
                                      new String[] {});
        assertContains(result, "HelpResponse");
    }

    public void testWsIISNtlmFail() throws Exception
    {
        try
        {
            _user = null;
            _password = null;
            com.oaklandsw.http.HttpURLConnection.setDefaultUserAgent(null);
            com.oaklandsw.http.HttpURLConnection.closeAllPooledConnections();
            invokeWindowsService();
            fail("This was supposed to fail due to auth problems");
        }
        catch (Exception ex)
        {
            assertTrue(ex.getMessage().indexOf("401") >= 0);
        }
    }
}
