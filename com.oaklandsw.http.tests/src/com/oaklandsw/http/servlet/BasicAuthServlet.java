/*
 * ====================================================================
 * 
 * The Apache Software License, Version 1.1
 * 
 * Copyright (c) 1999 The Apache Software Foundation. All rights reserved.
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
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
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

package com.oaklandsw.http.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class BasicAuthServlet extends MultiMethodServlet
{
    // The part of the URL where this servlet is installed
    public static final String   NAME  = "/auth/basic";

    // rather then make this servlet depend upon a base64 impl,
    // we'll just hard code some base65 encodings
    private static final HashMap creds = new HashMap();
    static
    {
        creds.put("dW5hbWU6cGFzc3dk", "uname:passwd");
        creds.put("amFrYXJ0YTpjb21tb25z", "jakarta:commons");
        creds.put("amFrYXJ0YS5hcGFjaGUub3JnL2NvbW1vbnM6aHR0cGNsaWVudA==",
                  "jakarta.apache.org/commons:httpclient");
    }

    protected void genericService(HttpServletRequest request,
                                  HttpServletResponse response)
        throws IOException,
            ServletException
    {
        PrintWriter out = response.getWriter();
        response.setContentType("text/html");

        String auth = request.getHeader("authorization");
        if (null == auth)
        {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.addHeader("www-authenticate",
                               "Basic realm=\"BasicAuthServlet\"");
            out.println("<html>");
            out.println("<head><title>BasicAuth Servlet: "
                + request.getMethod()
                + "</title></head>");
            out.println("<body>");
            out.println("<p>This is a response to an HTTP "
                + request.getMethod()
                + " request.</p>");
            out.println("<p>Not authorized.</p>");
            out.println("</body>");
            out.println("</html>");
            return;
        }

        String role = (String)(creds.get(auth.substring("basic ".length(), auth
                .length())));
        if (null == role)
        {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.addHeader("www-authenticate",
                               "Basic realm=\"BasicAuthServlet\"");
            out.println("<html>");
            out.println("<head><title>BasicAuth Servlet: "
                + request.getMethod()
                + "</title></head>");
            out.println("<body>");
            out.println("<p>This is a response to an HTTP "
                + request.getMethod()
                + " request.</p>");
            out.println("<p>Not authorized. \""
                + auth
                + "\" not recognized.</p>");
            out.println("</body>");
            out.println("</html>");
            return;
        }

        out.println("<html>");
        out.println("<head><title>BasicAuth Servlet: "
            + request.getMethod()
            + "</title></head>");
        out.println("<body>");
        out.println("<p>This is a response to an HTTP "
            + request.getMethod()
            + " request.</p>");
        out.println("<p>You have authenticated as \"" + role + "\"</p>");
        out.println("</body>");
        out.println("</html>");

    }
}
