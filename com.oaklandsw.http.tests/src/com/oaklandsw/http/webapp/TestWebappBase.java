package com.oaklandsw.http.webapp;

import com.oaklandsw.http.HttpTestBase;

import com.oaklandsw.util.Log;
import com.oaklandsw.util.LogUtils;


public class TestWebappBase extends HttpTestBase {
    private static final Log _log = LogUtils.makeLogger();

    public TestWebappBase(String testName) {
        super(testName);
        _doAuthProxyTest = true;
        _doAuthCloseProxyTest = true;
        _doProxyTest = true;
        _do10ProxyTest = true;
        _doExplicitTest = true;
        _doAppletTest = true;
    }
}
