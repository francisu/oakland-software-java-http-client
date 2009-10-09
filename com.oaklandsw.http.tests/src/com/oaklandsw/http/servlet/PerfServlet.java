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
