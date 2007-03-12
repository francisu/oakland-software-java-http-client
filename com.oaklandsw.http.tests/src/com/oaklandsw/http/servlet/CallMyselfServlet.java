package com.oaklandsw.http.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.oaklandsw.http.HttpTestBase;
import com.oaklandsw.http.HttpTestEnv;

public class CallMyselfServlet extends MultiMethodServlet
{

    // The part of the URL where this servlet is installed
    public static final String NAME = "/callmyself";

    protected void genericService(HttpServletRequest request,
                                  HttpServletResponse response)
        throws IOException,
            ServletException
    {

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("<head><title>CallMyself Servlet: "
            + request.getMethod()
            + "</title></head>");
        out.println("<body>");

        out.println("<p>This is a response to an HTTP "
            + request.getMethod()
            + " request.</p>");

        out.println("<p>Request Body</p>");
        InputStream is = request.getInputStream();
        byte[] buffer = new byte[10000];
        int len;
        while ((len = is.read(buffer)) > 0)
        {
            out.print(new String(buffer, 0, len));
        }
        out.println();
        out.println("<p>End of Request Body</p>");

        String servletToCall = request.getParameter("call");
        if (servletToCall == null)
            servletToCall = ParamServlet.NAME;

        String methodToCall = request.getParameter("method");
        if (methodToCall == null)
            methodToCall = "GET";

        URL url = new URL(HttpTestEnv.TEST_URL_WEBAPP
            + servletToCall
            + "?method="
            + methodToCall);

        HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
        urlCon.setRequestMethod(methodToCall);

        if (methodToCall.equals("POST"))
        {
            urlCon.setDoOutput(true);
            OutputStream os = urlCon.getOutputStream();
            os.write("servlet-param-one=servlet-param-value".getBytes("ASCII"));
        }

        urlCon.connect();

        out.println("<p>Response code: " + urlCon.getResponseCode() + "</p>");
        out.println("<p>Reply from call</p>");
        out.println(HttpTestBase.getReply(urlCon));
        out.println("<p>End of Reply from call</p>");

        out.println("</body>");
        out.println("</html>");
    }
}
