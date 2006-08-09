/*
 * Copyright 2006 oakland software, incorporated. All rights Reserved.
 */
package com.oaklandsw.http;

import com.oaklandsw.license.License;
import com.oaklandsw.license.LicenseManager;
import com.oaklandsw.license.LicensedCode;

/**
 * This does the HTTP cliense license check. It is dynamically loaded by
 * HttpURLConnection. This is done so that if this class does not exist, the
 * license check is bypassed. This is to support the source distribution so that
 * it does not need to contain any of the license code, as well as to not have
 * the source version depend on a license.
 */
public class HttpLicenseCheck
{
    // Used by the tests to make sure we have the correct license type
    public static int           _licenseType;

    private static final String EVAL_MESSAGE = "******\n******\n******\n******\n"
                                                 + "******  This is an evaluation version.  To purchase go to www.oaklandsoftware.com.\n"
                                                 + "******\n******\n******\n******\n";

    public void checkLicense()
    {
        LicensedCode lc = new HttpLicensedCodeImpl();
        LicenseManager lm = new LicenseManager(lc);
        License lic = lm.licenseCheck();
        if (lic == null || lic.validate(lc) != License.VALID)
            throw new RuntimeException("License check failed");

        _licenseType = lic.getLicenseType();
        if (lic.getLicenseType() == License.LIC_EVALUATION)
        {
            System.out.println(EVAL_MESSAGE
                + "\nExpires: "
                + lic.getExpirationDate()
                + "\n\n");
        }

    }

}
