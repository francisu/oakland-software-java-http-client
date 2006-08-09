/*
 * Copyright 2006 oakland software, incorporated. All rights Reserved.
 */
package com.oaklandsw.http;

import java.io.File;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;

import com.oaklandsw.ConstantsOaklandsw;
import com.oaklandsw.license.License;
import com.oaklandsw.license.LicenseManager;
import com.oaklandsw.license.LicensedCode;

/**
 * Licensed code implementation for Java HTTP Client
 */
public class HttpLicensedCodeImpl implements LicensedCode
{
    private static final String LIC_FILE   = "http.lic";

    // Even though this is not really used since we don't have
    // per-copy licenses, this is the product id, and if we ever
    // want to use per-copy licenses, this is what we need to use
    public static final int     PRODUCT_ID = 16;

    public boolean isAllowRegister()
    {
        return false;
    }

    public boolean isAllowEval()
    {
        return true;
    }

    public String askForKey(LicenseManager lm,
                            License license,
                            int status,
                            int reason,
                            String message,
                            String prevKey)
    {
        // Not used
        return null;
    }

    public void showLicense(LicenseManager lm,
                            License lic,
                            String message,
                            boolean showRegister)
    {
    }

    public int getLicenseType()
    {
        return License.LIC_EVALUATION;
    }

    public String getLicenseName()
    {
        return "Java HTTP Client";
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
        // return Version.VERSION;
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

    public URL getLicenseUrl()
    {
        return HttpURLConnection.class.getResource(LIC_FILE);
    }

    public File getLicenseFile()
    {
        // This is not used since we don't write a license file
        return null;
    }

    public String getProxyHost()
    {
        return null;
    }

    public int getProxyPort()
    {
        return 0;
    }

}
