package com.oaklandsw.http.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TimeoutServlet extends MultiMethodServlet
{
    // The part of the URL where this servlet is installed
    public static final String NAME = "/timeout";

    protected void genericService(HttpServletRequest request,
                                  HttpServletResponse response)
        throws IOException,
            ServletException
    {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("<head><title>Timeout Servlet: "
            + request.getMethod()
            + "</title></head>");
        out.println("<body>");

        int timeout = 0;

        String timeoutStr = request.getHeader("timeout");
        if (timeoutStr != null)
        {
            try
            {
                timeout = Integer.parseInt(timeoutStr);
            }
            catch (Exception e)
            {
            }
        }

        try
        {
            Thread.sleep(timeout);
        }
        catch (InterruptedException ex)
        {
            // Ignore
        }

        out.println("</body>");
        out.println("</html>");
    }
}
