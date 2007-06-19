package com.oaklandsw.http.webapp;

import org.apache.commons.logging.Log;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.Credential;
import com.oaklandsw.http.HttpTestEnv;
import com.oaklandsw.http.HttpURLConnection;
import com.oaklandsw.http.TestUserAgent;
import com.oaklandsw.http.axis2.OaklandHTTPTransportSender2;
import com.oaklandsw.util.FileUtils;
import com.oaklandsw.util.LogUtils;

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

import sharepoint.ListsStub;

import javax.xml.stream.XMLOutputFactory;

import java.io.File;
import java.io.StringWriter;

public class TestAxis2 extends TestWebappBase
{
    private static final Log          _log = LogUtils.makeLogger();

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

    public TestAxis2(String testName)
    {
        super(testName);

        // Netproxy cannot deal with this as it closes the connection after
        // the POST, probably because of a non-standard header.
        _doAuthCloseProxyTest = false;
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestAxis2.class);
        return suite;
    }

    public static void main(String args[])
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

    public void testWsWebappList() throws Exception
    {
        String output = invokeService(HttpTestEnv.TEST_URL_HOST_WEBAPP
                                          + "axis/EchoHeaders.jws",
                                      "http://axis.apache.org",
                                      "list",
                                      new String[] {});

        // System.out.println(output);
        assertContains(output, "user-agent:Axis2");

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
        return FileUtils.fileToUriString(wFile);
    }

    protected String invokeWindowsService() throws Exception
    {
        return invokeService(HttpTestEnv.HTTP_PROTOCOL
            + "//"
            + HttpTestEnv.IIS_HOST
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

        ListsStub ls = new ListsStub(HttpTestEnv.TEST_ICEWEB_URL);

        ServiceClient client = ls._getServiceClient();
        Options opt = client.getOptions();
        AxisConfiguration ac = client.getAxisService().getAxisConfiguration();
        setupOaklandXport(opt, ac);

        ListsStub.GetListCollection req = new ListsStub.GetListCollection();
        ListsStub.GetListCollectionResponse resp = ls.GetListCollection(req);

        // System.out.println(resp);
        // System.out.println(HexString.dump(output.getBytes()));
        assertContains(resp.getGetListCollectionResult().getExtraElement()
                .toString(), "DefaultViewUrl");
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

        ListsStub ls = new ListsStub(HttpTestEnv.TEST_ICEWEB_URL);

        ServiceClient client = ls._getServiceClient();
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

        ListsStub.GetListCollection req = new ListsStub.GetListCollection();
        ListsStub.GetListCollectionResponse resp = ls.GetListCollection(req);

        assertContains(resp.getGetListCollectionResult().getExtraElement()
                .toString(), "DefaultViewUrl");
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
        HttpURLConnection.setDefaultUserAgent(null);

        ListsStub ls = new ListsStub(HttpTestEnv.TEST_ICEWEB_URL);

        ServiceClient client = ls._getServiceClient();
        Options opt = client.getOptions();
        AxisConfiguration ac = client.getAxisService().getAxisConfiguration();
        setupOaklandXport(opt, ac);

        setupAxisAuth(opt,
                      HttpTestEnv.TEST_ICEWEB_DOMAIN,
                      HttpTestEnv.TEST_ICEWEB_USER,
                      HttpTestEnv.TEST_ICEWEB_PASSWORD);

        ListsStub.GetListCollection req = new ListsStub.GetListCollection();
        ListsStub.GetListCollectionResponse resp = ls.GetListCollection(req);

        assertContains(resp.getGetListCollectionResult().getExtraElement()
                .toString(), "DefaultViewUrl");
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

    public void testSharepointXsoLive() throws Exception
    {
        TestUserAgent._type = TestUserAgent.OFFICESHARE_XSO;

        // System.out.println(output);
        // assertContains(output, "user-agent:Axis2");

        // String sessionCookie = (String)_serviceContext.getProperty("Cookie");
        // System.out.println("cookie: " + sessionCookie);
        // assertNotNull(sessionCookie);
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

    public void testWsAmazon() throws Exception
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
