/*
 * $Header:
 * /home/cvspublic/jakarta-commons/httpclient/src/test-webapp/src/org/apache/commons/httpclient/WriteCookieServlet.java,v
 * 1.3 2002/03/15 23:32:40 marcsaeg Exp $ $Revision: 1.3 $ $Date: 2002/03/15
 * 23:32:40 $
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

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class WriteCookieServlet extends MultiMethodServlet
{
    // The part of the URL where this servlet is installed
    public static final String NAME = "/cookie/write";

    protected void genericService(HttpServletRequest request,
                                  HttpServletResponse response)
        throws IOException,
            ServletException
    {
        StringBuffer html = new StringBuffer();

        Cookie simple = new Cookie("simplecookie", "value");
        simple.setVersion(1);
        if ("set".equalsIgnoreCase(request.getParameter("simple")))
        {
            response.addCookie(simple);
            html.append("Wrote simplecookie.<br>");
        }
        else if ("unset".equalsIgnoreCase(request.getParameter("simple")))
        {
            simple.setMaxAge(0);
            response.addCookie(simple);
            html.append("Deleted simplecookie.<br>");
        }

        Cookie domain = new Cookie("domaincookie", "value");
        domain.setDomain(request.getServerName());
        domain.setVersion(1);
        if ("set".equalsIgnoreCase(request.getParameter("domain")))
        {
            response.addCookie(domain);
            html.append("Wrote domaincookie.<br>");
        }
        else if ("unset".equalsIgnoreCase(request.getParameter("domain")))
        {
            domain.setMaxAge(0);
            response.addCookie(domain);
            html.append("Deleted domaincookie.<br>");
        }

        Cookie path = new Cookie("pathcookie", "value");
        path.setPath(request.getParameter("path"));
        path.setVersion(1);
        if (null != request.getParameter("path"))
        {
            path.setPath(request.getParameter("path"));
            response.addCookie(path);
            html.append("Wrote pathcookie.<br>");
        }

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("<head><title>WriteCookieServlet: "
            + request.getMethod()
            + "</title></head>");
        out.println("<body>");
        out.println("<p>This is a response to an HTTP "
            + request.getMethod()
            + " request.</p>");
        out.println(html.toString());
        out.println("</body>");
        out.println("</html>");
    }
}
