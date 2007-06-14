package com.oaklandsw.http.webapp;

import org.apache.commons.logging.Log;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.HttpTestEnv;
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

import javax.xml.stream.XMLOutputFactory;

import java.io.File;
import java.io.StringWriter;

public class TestAxis2 extends TestWebappBase
{
    private static final Log          _log = LogUtils.makeLogger();

    protected String                  _user;
    protected String                  _password;

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
            _transportOut = new TransportOutDescription(Constants.TRANSPORT_HTTP);
            TransportSender transportSender = (TransportSender)OaklandHTTPTransportSender2.class
                    .newInstance();
            _transportOut.setSender(transportSender);
            _axisConfig.addTransportOut(_transportOut);
            // This is called below, and by the test case if necessary
            // transportSender.init(_configContext, _transportOut);
            _options.setSenderTransport(Constants.TRANSPORT_HTTP, _axisConfig);
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

    public void testWsIISNtlmOk() throws Exception
    {
        // Turn this off because this overrides Axis2 authentication
        com.oaklandsw.http.HttpURLConnection.setDefaultUserAgent(null);

        initTransport();

        HttpTransportProperties.Authenticator auth = new HttpTransportProperties.Authenticator();

        // Use Axis authentication
        auth.setDomain(HttpTestEnv.TEST_IIS_DOMAIN);
        auth.setUsername(HttpTestEnv.TEST_IIS_USER);
        auth.setPassword(HttpTestEnv.TEST_IIS_PASSWORD);

        _options.setProperty(HTTPConstants.AUTHENTICATE, auth);

        _transportOut.getSender().init(_configContext, _transportOut);

        String result = invokeWindowsService();
        assertContains(result, "Hello Francis");
    }

    public void testWsIISNtlmOkUserAgent() throws Exception
    {
        // User the HttpUserAgent authentication
        com.oaklandsw.http.HttpURLConnection
                .setDefaultUserAgent(new com.oaklandsw.http.TestUserAgent());
        TestUserAgent._type = TestUserAgent.GOOD;
        String result = invokeWindowsService();
        assertContains(result, "Hello Francis");
    }

    public void testSharepointIcewebGood() throws Exception
    {
        logAll();
        com.oaklandsw.http.HttpURLConnection
                .setDefaultUserAgent(new com.oaklandsw.http.TestUserAgent());
        TestUserAgent._type = TestUserAgent.OFFICESHARE_ICEWEB;
        String output = invokeService("http://sharepoint.iceweb.com/sites/demo/_vti_bin/Lists.asmx",
                                      "http://schemas.microsoft.com/sharepoint/soap/",
                                      "GetListCollection",
                                      new String[] {});

        // System.out.println(HexString.dump(output.getBytes()));
        assertContains(output, "GetListCollectionResponse");
    }

    // FIXME - this gets an NPE when run by itself, but when run with other
    // tests it passes (clearly something is wrong)
    public void testSharepointIcewebBadNoAuth() throws Exception
    {
        logAll();
        // com.oaklandsw.http.HttpURLConnection
        // .setDefaultUserAgent(new com.oaklandsw.http.TestUserAgent());
        // TestUserAgent._type = TestUserAgent.OFFICESHARE_ICEWEB;
        String output = invokeService("http://sharepoint.iceweb.com/sites/demo/_vti_bin/Lists.asmx",
                                      "http://schemas.microsoft.com/sharepoint/soap/",
                                      "GetListCollection",
                                      new String[] {});

        // System.out.println(HexString.dump(output.getBytes()));
        assertContains(output, "GetListCollectionResponse");
    }

    public void NOtestSharepointXsoLive() throws Exception
    {
        logAll();
        com.oaklandsw.http.HttpURLConnection
                .setDefaultUserAgent(new com.oaklandsw.http.TestUserAgent());
        TestUserAgent._type = TestUserAgent.OFFICESHARE_XSO;
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

    public void NOtestSharepointXsoLiveBad() throws Exception
    {
        logAll();
        com.oaklandsw.http.HttpURLConnection
                .setDefaultUserAgent(new com.oaklandsw.http.TestUserAgent());
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
