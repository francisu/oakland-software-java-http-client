package com.oaklandsw.http.sso;

import java.util.Map;

import jcifs.Config;
import jcifs.dcerpc.DcerpcBinding;
import jcifs.smb.NtlmPasswordAuthentication;

import org.apache.commons.logging.Log;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.oaklandsw.http.HttpTestBase;
import com.oaklandsw.http.HttpTestEnv;
import com.oaklandsw.http.ntlm.NtlmProviderImpl;
import com.oaklandsw.util.LogUtils;

/**
 * Initial local test case for SSO to test the netlogon.
 */

public class TestLocal extends HttpTestBase
{
    private static final Log _log = LogUtils.makeLogger();

    public TestLocal(String testName)
    {
        super(testName);
    }

    public static void main(String args[])
    {
        String[] testCaseName = { TestLocal.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }

    public static Test suite()
    {
        return new TestSuite(TestLocal.class);
    }

    public void testOpenSession() throws Exception
    {
        logAll();
        
        Config.setProperty("jcifs.smb.lmCompatibility", "5");

        NtlmPasswordAuthentication.setNtlmProvider(new NtlmProviderImpl());
        
        Map interfaceMap = DcerpcBinding.getInterfaceMap();
        interfaceMap.put("netlogon", netlogon.getSyntax());

        NtlmPasswordAuthentication auth = null;

        if (!false)
        {
            // With .26
            auth = new NtlmPasswordAuthentication(HttpTestEnv.TEST_OAKLANDSW_TEST_DOMAIN,
                                                  HttpTestEnv.TEST_OAKLANDSW_TEST_USER,
                                                  HttpTestEnv.TEST_OAKLANDSW_TEST_PASSWORD);
        }
        else
        {
            // with .27
            auth = new NtlmPasswordAuthentication("workgroup",
                                                  "administrator",
                                                  "admin");
            
        }
        NetrSession session = new NetrSession("192.168.1.26", auth);

    }

}
