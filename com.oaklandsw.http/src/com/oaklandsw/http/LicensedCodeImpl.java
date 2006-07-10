/*
 * Copyright 2006 oakland software, incorporated.  All rights Reserved.
 */
package com.oaklandsw.http;

import java.util.Calendar;
import java.util.Date;

import com.oaklandsw.ConstantsOaklandsw;
import com.oaklandsw.license.LicenseImpl;
import com.oaklandsw.license.LicenseKeyProvider;
import com.oaklandsw.license.LicensedCode;

/**
 * Licensed code implementation for Java HTTP Client
 */
public class LicensedCodeImpl implements LicensedCode
{

    public static final int PRODUCT_ID = 1;
    
    public String getWiredLicenseKey()
    {
        return null;
    }

    public String getDefaultLicenseKey()
    {
        return null;
    }

    public void indicateLicenseFailure(int reason,
                                       Exception exception,
                                       LicenseImpl license)
    {
    }

    public LicenseImpl getEvalLicense()
    {
        return null;
    }

    public LicenseKeyProvider getLicenseKeyProvider()
    {
        return null;
    }

    public int getCodeBaseId()
    {
        return ConstantsOaklandsw.CODEBASE_JAVA_HTTP_CLIENT;
    }

    public String getCodeBaseVersion()
    {
        // We don't use the code base version for licenses
        // in this product
        return null;
        //return Version.VERSION;
    }

    public Date getCodeBaseDate()
    {
        return null;
    }

    public int getProductId()
    {
        return PRODUCT_ID;
    }

    public Date getExpirationDate()
    {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, 30);
        return c.getTime();
    }

    public String getHostName()
    {
        return null;
    }

}
