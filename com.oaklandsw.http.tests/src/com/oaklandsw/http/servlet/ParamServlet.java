/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oaklandsw.http.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import java.net.InetAddress;

import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class ParamServlet extends MultiMethodServlet {
    // The sequence number parameter that is sent back in a header
    // used for diagnostics
    public static final String SEQUENCE = "Sequence";
    public static final String HOST_IP_ADDRESS = "HostIPAddress";
    public static final String HOST_CHECK = "HostCheck";

    // The part of the URL where this servlet is installed
    public static final String NAME = "/params";

    // Unmodified JCIFS filter
    public static final String NAME_NTLM = "/ntlmparams";

    // Oakland Software NTLMv2 filter
    public static final String NAME_NTLM2 = "/ntlm2params";

    // Various levels
    public static final String NAME_NTLM2_0 = NAME_NTLM2 + "0";
    public static final String NAME_NTLM2_5 = NAME_NTLM2 + "5";

    protected void genericService(HttpServletRequest request,
        HttpServletResponse response) throws IOException, ServletException {
        try {
            int sequence = -1;

            String hostCheck = request.getHeader(HOST_CHECK);
            String foundHost = request.getHeader("Host");

            // Get rid of the port if it's there
            if (foundHost.indexOf(":") >= 0) {
                foundHost = foundHost.substring(0, foundHost.indexOf(":"));
            }

            // Check host header if requested
            if (null != hostCheck) {
                if (!hostCheck.equals(foundHost)) {
                    response.setContentType("text/plain");
                    response.setStatus(500);

                    PrintWriter out = response.getWriter();
                    out.println("FAILED hostcheck - found: " + foundHost +
                        " expected: " + hostCheck);

                    return;
                }
            }

            response.setContentType("text/html");

            PrintWriter out = response.getWriter();
            out.println("hostcheck - found: " + foundHost + " expected: " +
                hostCheck);

            out.println("<html>");
            out.println("<head><title>Param Servlet: " + request.getMethod() +
                "</title></head>");
            out.println("<body>");

            out.println("<p>This is a response to an HTTP " +
                request.getMethod() + " request.</p>");

            out.print("<p>QueryString=");

            if (null == request.getQueryString()) {
                out.print("null");
            } else {
                out.print("\"" + request.getQueryString() + "\"");
            }

            out.println("</p>");

            out.print("<p>RequestURI=");
            out.print("\"" + request.getRequestURI() + "\"");
            out.println("</p>");

            out.println("<p>Parameters</p>");

            Enumeration e = request.getParameterNames();

            while (e.hasMoreElements()) {
                String name = (String) (e.nextElement());
                String[] values = request.getParameterValues(name);

                if (name.equals(SEQUENCE)) {
                    sequence = Integer.parseInt(values[0]);
                }

                if ((null == values) || (values.length < 1)) {
                    out.println("name=\"" + name + "\";value=null<br>");
                } else {
                    for (int i = 0; i < values.length; i++) {
                        if (null == values[i]) {
                            out.println("name=\"" + name + "\";value=null<br>");
                        } else {
                            out.println("name=\"" + name + "\";value=\"" +
                                values[i] + "\"<br>");
                        }
                    }
                }
            }

            out.println("<p>Request Body</p>");

            InputStream is = request.getInputStream();
            byte[] buffer = new byte[1000];
            int len;

            while ((len = is.read(buffer)) > 0) {
                out.print(new String(buffer, 0, len));
            }

            out.println();

            out.println("");
            out.println(
                "<FORM action=\"http://berlioz:8080/oaklandsw-http/ntlm2params\" " +
                "method=\"post\">");
            out.println("    <P>");
            out.println("    <LABEL for=\"firstname\">First name: </LABEL>");
            out.println("<INPUT type=\"text\" id=\"firstname\"><BR>");
            out.println("     <INPUT type=\"submit\" value=\"Send\"> " +
                "<INPUT type=\"reset\">");
            out.println(" </P> ");
            out.println("  </FORM>");

            out.println("</body>");
            out.println("</html>");

            // Return the IP address if requested
            if (request.getHeader(HOST_IP_ADDRESS) != null) {
                response.setHeader(HOST_IP_ADDRESS,
                    InetAddress.getLocalHost().getHostAddress());
            }

            if (sequence != -1) {
                response.setHeader(SEQUENCE, Integer.toString(sequence));
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
            System.out.println(ex);
        }
    }
}
