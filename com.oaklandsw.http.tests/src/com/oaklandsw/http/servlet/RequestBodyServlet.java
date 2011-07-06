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

import com.oaklandsw.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class RequestBodyServlet extends MultiMethodServlet {
    // The part of the URL where this servlet is installed
    public static final String NAME = "/body";

    protected void genericService(HttpServletRequest request,
        HttpServletResponse response) throws IOException, ServletException {
        response.setContentType("text/html");

        PrintWriter out = response.getWriter();
        String bodyStr = null;
        InputStream is = request.getInputStream();

        if (is != null) {
            bodyStr = Util.getStringFromInputStream(is);
        }

        out.println("<html>");
        out.println("<head><title>Request Body Servlet: " +
            request.getMethod() + "</title></head>");
        out.println("<body>");

        out.println("<p>This is a response to an HTTP " + request.getMethod() +
            " request.</p>");
        out.println("<p>Body:</p>");

        if (bodyStr != null) {
            out.println("<p><tt>" + bodyStr + "</tt></p>");
        } else {
            out.println("No body submitted.");
        }

        out.println("</body>");
        out.println("</html>");
    }
}
