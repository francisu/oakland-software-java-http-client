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
import java.io.PrintWriter;

import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class HeaderServlet extends MultiMethodServlet {
    // The part of the URL where this servlet is installed
    public static final String NAME = "/headers";
    public static final String TEXT_CONST = "abcdefghijklmnopqrstuvwxyz0123456789";

    protected void genericService(HttpServletRequest request,
        HttpServletResponse response) throws IOException, ServletException {
        response.setHeader("HeaderSetByServlet", "Yes");

        String respCode = null;

        try {
            respCode = request.getParameter("responseCode");
        } catch (Exception ex) {
            // Must not be present
        }

        if (respCode != null) {
            int respInt = 0;

            try {
                respInt = Integer.parseInt(respCode);
            } catch (Exception ex) {
            }

            response.setStatus(respInt);

            // Don't give a body if this is some kind of error
            if (request.getParameter("noBody") != null) {
                return;
            }
        }

        response.setContentType("text/html");

        PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("<head><title>Header Servlet: " + request.getMethod() +
            "</title></head>");
        out.println("<body>");

        out.println("<p>This is a response to an HTTP " + request.getMethod() +
            " request.</p>");
        out.println("<p>Request Headers:</p>");

        Enumeration names = request.getHeaderNames();
        int length = 0;

        while (names.hasMoreElements()) {
            String name = (String) (names.nextElement());
            Enumeration values = request.getHeaders(name);

            while (values.hasMoreElements()) {
                Object value = values.nextElement();

                // Allows and abitrary amount of text to be emitted
                if (name.equalsIgnoreCase("emit-text")) {
                    try {
                        length = Integer.parseInt((String) value);
                    } catch (Exception e) {
                        length = 0;
                    }
                }

                out.println("name=\"" + name + "\";value=\"" + value +
                    "\"<br>");
            }
        }

        if (length > 0) {
            for (int i = 0; i < length; i++)
                out.print(TEXT_CONST);
        }

        out.println("</body>");
        out.println("</html>");
    }
}
