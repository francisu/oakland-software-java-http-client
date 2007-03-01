package com.oaklandsw.http.webapp;

import org.apache.commons.logging.Log;

import com.oaklandsw.http.TestBase;
import com.oaklandsw.util.LogUtils;

public class TestWebappBase extends TestBase
{

    private static final Log   _log         = LogUtils.makeLogger();

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
