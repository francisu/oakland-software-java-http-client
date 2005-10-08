//
// Some portions copyright 2002-3003, oakland software, all rights reserved.
//
// May not be used or redistributed without specific written
// permission from oakland software.
//
/*
 * ====================================================================
 * 
 * The Apache Software License, Version 1.1
 * 
 * Copyright (c) 2002 the Apache Software Foundation. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any, must
 * include the following acknowlegement: "This product includes software
 * developed by the Apache Software Foundation (http://www.apache.org/)."
 * Alternately, this acknowlegement may appear in the software itself, if and
 * wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "The Jakarta Project", "HttpClient", and "Apache Software
 * Foundation" must not be used to endorse or promote products derived from this
 * software without prior written permission. For written permission, please
 * contact apache@apache.org.
 * 
 * 5. Products derived from this software may not be called "Apache" nor may
 * "Apache" appear in their names without prior written permission of the Apache
 * Group.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE APACHE
 * SOFTWARE FOUNDATION OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many individuals on
 * behalf of the Apache Software Foundation. For more information on the Apache
 * Software Foundation, please see <http://www.apache.org/>.
 * 
 * [Additional notices, if required by prior licensing conditions]
 * 
 */

package com.oaklandsw.http;

import java.io.IOException;

/**
 * The URI parsing and escape encoding exception.
 * <p>
 * 
 */
public class URIException extends IOException
{

    // ----------------------------------------------------------- constructors

    /**
     * Default constructor.
     */
    public URIException()
    {
    }

    /**
     * The constructor with a reason code argument.
     * 
     * @param rc
     *            the reason code
     */
    public URIException(int rc)
    {
        setReasonCode(rc);
    }

    /**
     * The constructor with a reason string and its code arguments.
     * 
     * @param rc
     *            the reason code
     * @param r
     *            the reason
     */
    public URIException(int rc, String r)
    {
        super(r); // for backward compatibility of Throwable
        this.reason = r;
        setReasonCode(rc);
    }

    /**
     * The constructor with a reason string argument.
     * 
     * @param r
     *            the reason
     */
    public URIException(String r)
    {
        super(r); // for backward compatibility of Throwable
        this.reason = r;
        setReasonCode(UNKNOWN);
    }

    // -------------------------------------------------------------- constants

    /**
     * No specified reason code.
     */
    public static final int UNKNOWN              = 0;

    /**
     * The URI parsing error.
     */
    public static final int PARSING              = 1;

    /**
     * The unsupported character encoding.
     */
    public static final int UNSUPPORTED_ENCODING = 2;

    /**
     * The URI escape or unescape error.
     */
    public static final int ESCAPING             = 3;

    // ------------------------------------------------------------- properties

    /**
     * The reason code.
     */
    protected int           reasonCode;

    /**
     * The reason message.
     */
    protected String        reason;

    // ---------------------------------------------------------------- methods

    /**
     * Get the reason code.
     * 
     * @return the reason code
     */
    public int getReasonCode()
    {
        return reasonCode;
    }

    /**
     * Set the reason code.
     * 
     * @param rc
     *            reason code
     */
    public void setReasonCode(int rc)
    {
        this.reasonCode = rc;
    }

    /**
     * Get the reason message.
     * 
     * @return the reason message
     */
    public String getReason()
    {
        return reason;
    }

    /**
     * Set the reason message.
     * 
     * @param r
     *            reason message
     */
    public void setReason(String r)
    {
        this.reason = r;
    }

}
