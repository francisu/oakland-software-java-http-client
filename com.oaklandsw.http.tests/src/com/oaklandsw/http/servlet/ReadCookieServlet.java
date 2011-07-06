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


public class ReadCookieServlet extends MultiMethodServlet {
    // The part of the URL where this servlet is installed
    public static final String NAME = "/cookie/read";

    protected void genericService(HttpServletRequest request,
        HttpServletResponse response) throws IOException, ServletException {
        response.setContentType("text/html");

        PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("<head><title>ReadCookieServlet: " + request.getMethod() +
            "</title></head>");
        out.println("<body>");
        out.println("<p>This is a response to an HTTP " + request.getMethod() +
            " request.</p>");
        out.println("<p><tt>Cookie: " + request.getHeader("Cookie") +
            "</tt></p>");

        Cookie[] cookies = request.getCookies();

        if (null != cookies) {
            for (int i = 0; i < cookies.length; i++) {
                out.println("<tt>" + cookies[i].getName() + "=" +
                    cookies[i].getValue() + "</tt><br>");
            }
        }

        out.println("</body>");
        out.println("</html>");
    }
}
