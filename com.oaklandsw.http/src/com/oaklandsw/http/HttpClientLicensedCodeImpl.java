/*
 * Copyright 2006 oakland software, incorporated.  All rights Reserved.
 */
package com.oaklandsw.http;

import java.io.File;
import java.util.Calendar;
import java.util.Date;

import com.oaklandsw.ConstantsOaklandsw;
import com.oaklandsw.license.License;
import com.oaklandsw.license.LicensedCode;

/**
 * Licensed code implementation for Java HTTP Client
 */
public class HttpClientLicensedCodeImpl implements LicensedCode
{

    // Even though this is not really used since we don't have
    // per-copy licenses, this is the product id, and if we ever
    // want to use per-copy licenses, this is what we need to use
    public static final int PRODUCT_ID = 16;
    
    public int actionToTake(int status,
                            int reason,
                            License license,
                            Exception exception)
    {
        return ACTION_NOTHING; 
    }

    public String askForKey(License license)
    {
        // Not used
        return null;
    }

    public int getLicenseType()
    {
        return License.LIC_EVALUATION;
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
        // Not used
        return null;
    }

    public String getEvalLicenseFileName()
    {
        // Make this name kind of obscure in hopes that the user
        // does not delete it
        return ".xmlfuoshc";
    }

    public File getLicenseFile()
    {
        // This is not used
        return null;
    }

}
