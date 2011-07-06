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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class PerfServlet extends MultiMethodServlet {
    static String ONEK = "";

    static {
        for (int i = 0; i < 64; i++)
            ONEK += "0123456789abcdef";
    }

    protected void genericService(HttpServletRequest request,
        HttpServletResponse response) throws IOException, ServletException {
        try {
            PrintWriter out = response.getWriter();

            String size = request.getParameter("size");

            if (size != null) {
                if (size.endsWith("k")) {
                    size = size.substring(0, size.length() - 1);
                }

                int sizeNum = Integer.parseInt(size);
                StringBuffer sb = new StringBuffer(sizeNum);

                for (int i = 0; i < sizeNum; i++) {
                    sb.append(ONEK);
                }

                //out.println("-" + Integer.toString(ONEK.length()) + "-");
                //out.println(Integer.toString(sb.length()) + "-");
                out.print(sb.toString());

                String chunked = request.getParameter("chunked");

                if (chunked == null) {
                    response.setContentLength(sizeNum * 1024);
                }
            }

            String delay = request.getParameter("delay");

            if (delay != null) {
                int delayNum = Integer.parseInt(delay);
                Thread.sleep(delayNum);
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
            System.out.println(ex);
        }
    }
}
