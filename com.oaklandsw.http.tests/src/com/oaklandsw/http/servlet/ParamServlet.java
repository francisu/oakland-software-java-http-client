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
import java.net.InetAddress;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ParamServlet extends MultiMethodServlet
{

    // The sequence number parameter that is sent back in a header
    // used for diagnostics
    public static final String SEQUENCE        = "Sequence";

    public static final String HOST_IP_ADDRESS = "HostIPAddress";
    public static final String HOST_CHECK      = "HostCheck";

    // The part of the URL where this servlet is installed
    public static final String NAME            = "/params";
    public static final String NAME_NTLM       = "/ntlmparams";

    protected void genericService(HttpServletRequest request,
                                  HttpServletResponse response)
        throws IOException,
            ServletException
    {

        try
        {
            int sequence = -1;

            String hostCheck = request.getHeader(HOST_CHECK);
            String foundHost = request.getHeader("Host");
            // Get rid of the port if it's there
            if (foundHost.indexOf(":") >= 0)
                foundHost = foundHost.substring(0, foundHost.indexOf(":"));

            // Check host header if requested
            if (null != hostCheck)
            {
                if (!hostCheck.equals(foundHost))
                {
                    response.setContentType("text/plain");
                    response.setStatus(500);
                    PrintWriter out = response.getWriter();
                    out.println("FAILED hostcheck - found: "
                        + foundHost
                        + " expected: "
                        + hostCheck);
                    return;
                }
            }

            response.setContentType("text/html");

            PrintWriter out = response.getWriter();
            out.println("hostcheck - found: "
                + foundHost
                + " expected: "
                + hostCheck);

            out.println("<html>");
            out.println("<head><title>Param Servlet: "
                + request.getMethod()
                + "</title></head>");
            out.println("<body>");

            out.println("<p>This is a response to an HTTP "
                + request.getMethod()
                + " request.</p>");

            out.print("<p>QueryString=");
            if (null == request.getQueryString())
            {
                out.print("null");
            }
            else
            {
                out.print("\"" + request.getQueryString() + "\"");
            }
            out.println("</p>");

            out.print("<p>RequestURI=");
            out.print("\"" + request.getRequestURI() + "\"");
            out.println("</p>");

            out.println("<p>Parameters</p>");
            Enumeration e = request.getParameterNames();
            while (e.hasMoreElements())
            {
                String name = (String)(e.nextElement());
                String[] values = request.getParameterValues(name);
                if (name.equals(SEQUENCE))
                    sequence = Integer.parseInt(values[0]);
                if (null == values || values.length < 1)
                {
                    out.println("name=\"" + name + "\";value=null<br>");
                }
                else
                {
                    for (int i = 0; i < values.length; i++)
                    {
                        if (null == values[i])
                        {
                            out.println("name=\"" + name + "\";value=null<br>");
                        }
                        else
                        {
                            out.println("name=\""
                                + name
                                + "\";value=\""
                                + values[i]
                                + "\"<br>");
                        }
                    }
                }

            }

            out.println("<p>Request Body</p>");
            // InputStream is = request.getInputStream();
            // byte[] buffer = new byte[1000];
            // int len;

            /*******************************************************************
             * // for tomcat 3.2 seems to die on this statement, // but no
             * exception is thrown while ((len = is.read(buffer)) > 0) {
             * out.print(new String(buffer, 0, len)); }
             ******************************************************************/

            out.println();

            out.println("</body>");
            out.println("</html>");

            // Return the IP address if requested
            if (request.getHeader(HOST_IP_ADDRESS) != null)
            {
                response.setHeader(HOST_IP_ADDRESS, InetAddress.getLocalHost()
                        .getHostAddress());
            }

            if (sequence != -1)
            {
                response.setHeader(SEQUENCE, Integer.toString(sequence));
            }

        }
        catch (Throwable ex)
        {
            ex.printStackTrace();
            System.out.println(ex);
        }
    }
}
