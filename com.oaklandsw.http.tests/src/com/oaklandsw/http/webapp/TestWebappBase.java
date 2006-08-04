package com.oaklandsw.http.webapp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.oaklandsw.http.TestBase;

public class TestWebappBase extends TestBase
{

    private static final Log _log = LogFactory.getLog(TestWebappBase.class);

    public TestWebappBase(String testName)
    {
        super(testName);
        _doAuthProxyTest = true;
        // _doAuthCloseProxyTest = true;
        _doProxyTest = true;
        _do10ProxyTest = true;
        _doExplicitTest = true;
        _doAppletTest = true;
    }



    
}
