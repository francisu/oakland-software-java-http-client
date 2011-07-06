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

import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class BasicAuthServlet extends MultiMethodServlet {
    // The part of the URL where this servlet is installed
    public static final String NAME = "/auth/basic";

    // rather then make this servlet depend upon a base64 impl,
    // we'll just hard code some base65 encodings
    private static final HashMap creds = new HashMap();

    static {
        creds.put("dW5hbWU6cGFzc3dk", "uname:passwd");
        creds.put("amFrYXJ0YTpjb21tb25z", "jakarta:commons");
        creds.put("amFrYXJ0YS5hcGFjaGUub3JnL2NvbW1vbnM6aHR0cGNsaWVudA==",
            "jakarta.apache.org/commons:httpclient");
    }

    protected void genericService(HttpServletRequest request,
        HttpServletResponse response) throws IOException, ServletException {
        PrintWriter out = response.getWriter();
        response.setContentType("text/html");

        String auth = request.getHeader("authorization");

        if (null == auth) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.addHeader("www-authenticate",
                "Basic realm=\"BasicAuthServlet\"");
            out.println("<html>");
            out.println("<head><title>BasicAuth Servlet: " +
                request.getMethod() + "</title></head>");
            out.println("<body>");
            out.println("<p>This is a response to an HTTP " +
                request.getMethod() + " request.</p>");
            out.println("<p>Not authorized.</p>");
            out.println("</body>");
            out.println("</html>");

            return;
        }

        String role = (String) (creds.get(auth.substring("basic ".length(),
                    auth.length())));

        if (null == role) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.addHeader("www-authenticate",
                "Basic realm=\"BasicAuthServlet\"");
            out.println("<html>");
            out.println("<head><title>BasicAuth Servlet: " +
                request.getMethod() + "</title></head>");
            out.println("<body>");
            out.println("<p>This is a response to an HTTP " +
                request.getMethod() + " request.</p>");
            out.println("<p>Not authorized. \"" + auth +
                "\" not recognized.</p>");
            out.println("</body>");
            out.println("</html>");

            return;
        }

        out.println("<html>");
        out.println("<head><title>BasicAuth Servlet: " + request.getMethod() +
            "</title></head>");
        out.println("<body>");
        out.println("<p>This is a response to an HTTP " + request.getMethod() +
            " request.</p>");
        out.println("<p>You have authenticated as \"" + role + "\"</p>");
        out.println("</body>");
        out.println("</html>");
    }
}
