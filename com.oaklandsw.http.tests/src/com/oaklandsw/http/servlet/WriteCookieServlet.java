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

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class WriteCookieServlet extends MultiMethodServlet {
    // The part of the URL where this servlet is installed
    public static final String NAME = "/cookie/write";

    protected void genericService(HttpServletRequest request,
        HttpServletResponse response) throws IOException, ServletException {
        StringBuffer html = new StringBuffer();

        // Age for new cookies
        int setMaxAge = -1;

        String setMaxAgeStr = request.getParameter("expire");

        if (setMaxAgeStr != null) {
            setMaxAge = Integer.parseInt(setMaxAgeStr);
        }

        Cookie simple = new Cookie("simplecookie", "value");
        simple.setVersion(1);

        if ("set".equalsIgnoreCase(request.getParameter("simple"))) {
            simple.setMaxAge(setMaxAge);
            response.addCookie(simple);
            html.append("Wrote simplecookie.<br>");
        } else if ("unset".equalsIgnoreCase(request.getParameter("simple"))) {
            simple.setMaxAge(0);
            response.addCookie(simple);
            html.append("Deleted simplecookie.<br>");
        }

        Cookie domain = new Cookie("domaincookie", "value");
        domain.setDomain(request.getServerName());
        domain.setVersion(1);

        if ("set".equalsIgnoreCase(request.getParameter("domain"))) {
            domain.setMaxAge(setMaxAge);
            response.addCookie(domain);
            html.append("Wrote domaincookie.<br>");
        } else if ("unset".equalsIgnoreCase(request.getParameter("domain"))) {
            domain.setMaxAge(0);
            response.addCookie(domain);
            html.append("Deleted domaincookie.<br>");
        }

        Cookie path = new Cookie("pathcookie", "value");
        path.setPath(request.getParameter("path"));
        path.setVersion(1);

        if (null != request.getParameter("path")) {
            path.setPath(request.getParameter("path"));
            response.addCookie(path);
            html.append("Wrote pathcookie.<br>");
        }

        response.setContentType("text/html");

        PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("<head><title>WriteCookieServlet: " + request.getMethod() +
            "</title></head>");
        out.println("<body>");
        out.println("<p>This is a response to an HTTP " + request.getMethod() +
            " request.</p>");
        out.println(html.toString());
        out.println("</body>");
        out.println("</html>");
    }
}
