//
// Copyright (c) 2001 CrossWeave, Inc.
// Portions copyright 2003-2006 Oakland Software.
// All rights reserved.
//

package com.oaklandsw.http.server;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import com.oaklandsw.http.TestEnv;
import com.oaklandsw.util.URIUtil;

//
// * Test class for simulating error conditions on remote sites
// * Use like so:
// *
// * http://localhost:PORT_NUMBER/? <args>where <args>is (standard query
// encoding)
// *
// * quit - to shutdown
// * close - add a connection close header
// * version - specifies protocol version, like "1.0"
// * status - specifies entire status line
// * error - specifies "none", "timeout", "disconnect", or an HTTP error code
// * when - one of: "before-read", "during-read", "before-headers",
// "during-headers",
// * "before-content", or "during-content"
// * lines - # of lines to output before
// * failing (used only for "during-content" case).
// * sec - timeout length in seconds (used only in "error=timeout" case)
// * noContentLength - the Content-Length header should be omitted
// * badContentLength - the Content-Length header should be incorrect
// * spaceContentLength - the Content-Length header should have trailing spaces
// * keepAlive - add keep-alive header
// *

public class ErrorServer extends Thread
{
    public static final int    PORT_NUMBER          = TestEnv.TEST_ERRORSVR_PORT;

    public static final int    CONTENT_LENGTH       = 8104;
    public static final int    CONTENT_LOOPS        = 100;
    public static final int    CONTENT_EOLS         = CONTENT_LOOPS + 2;

    public static final String ERROR_BEFORE_READ    = "before-read";
    public static final String ERROR_BEFORE_CONTENT = "before-content";
    public static final String ERROR_DURING_CONTENT = "during-content";
    public static final String ERROR_DURING_READ    = "during-read";
    public static final String ERROR_BEFORE_HEADERS = "before-headers";

    public static final String ERROR_IDLE_TIMEOUT   = "idle-timeout";

    public static final String ERROR_KEEP_ALIVE     = "keepAlive";

    // The POST request has no data
    public static final String POST_NO_DATA         = "postNoData";

    // Used if the server is launched inside of another process
    // (i.e. the main method is called from another process)
    static boolean             _running;

    String                     _version;
    String                     _statusLine;
    String                     _endOfLine;

    boolean                    _closeConnection;
    boolean                    _keepAlive;

    // The amount of time to keep this connection alive if it
    // has not been closed
    int                        _idleTimeout;

    // Include the Content-Length header
    boolean                    _contentLength       = true;
    boolean                    _badContentLength;
    boolean                    _spaceContentLength;

    Socket                     request;

    InputStream                is;
    OutputStream               os;
    PrintStream                ps;

    boolean                    post;

    boolean                    _postNoData;

    String                     errorType;
    String                     errorLoc;
    int                        errorLines;
    int                        errorSec;

    static boolean             debug;

    static String              defaultError         = "none";

    boolean                    unix                 = (File.separatorChar == '/');

    static int                 succeedAfter;

    static boolean             _quit;

    // Accepts HTTP requests on a socket, can accept multiple requests
    // on the same socket.
    public ErrorServer(Socket incoming)
    {
        this.request = incoming;

        // Normal end of line
        _endOfLine = "\r\n";
    }

    private void setupOutput() throws IOException
    {
        if (os != null)
            return;
        os = request.getOutputStream();
        ps = new PrintStream(os, true);
    }

    private void print(String s)
    {
        this.ps.print(s);
    }

    private void println()
    {
        this.ps.print(_endOfLine);
        this.ps.flush();
    }

    private void println(String s)
    {
        this.print(s);
        this.println();
    }

    private boolean simulateError() throws IOException
    {
        if (succeedAfter == 1)
        {
            // succeed despite error request for retry test
            succeedAfter = 0;
            return false;
        }
        else if (succeedAfter > 1)
        {
            succeedAfter--;
        }
        if (this.errorType.equalsIgnoreCase("none"))
        {
            return false;
        }
        else if (this.errorType.equalsIgnoreCase("timeout"))
        {
            try
            {
                if (errorSec == 0)
                    errorSec = 20;
                if (debug)
                    System.out.println("Sleeping for " + errorSec);
                Thread.sleep(this.errorSec * 1000);
            }
            catch (InterruptedException ie)
            {
                if (debug)
                {
                    System.out.println("ErrorServlet: InterruptedException "
                        + "attempting to timeout");
                }
            }
            return false;
        }
        else if (this.errorType.equalsIgnoreCase("disconnect"))
        {
            if (debug)
                System.out.println("Disconnected");
            request.close();
        }
        else
        {
            try
            {
                int respCode = Integer.parseInt(this.errorType);
                setupOutput();
                this.println("HTTP/" + _version + " " + respCode + " error");
                this.println();
                this.println("Here is some explanatory text about the "
                    + respCode
                    + " error.");
            }
            catch (NumberFormatException nfe)
            {
                if (debug)
                {
                    System.out.println("Unknown error type requested: '"
                        + this.errorType
                        + "'");
                }
            }
        }
        return true;
    }

    public void handleRequest() throws Exception
    {
        boolean closeConnection = false;

        while (!closeConnection)
        {
            // Will happen if the connection timed out
            if (request == null)
                return;

            // Assume we want to close the connection
            closeConnection = true;

            char[] buf = new char[10000];
            if (is == null)
                is = request.getInputStream();

            char term[] = new char[] { '\r', '\n', '\r', '\n' };
            int termInd = 0;
            int i = 0;
            // Read only the headers, no other data
            for (;; i++)
            {
                int c = is.read();
                if (c == -1)
                    break;

                buf[i] = (char)c;

                if (c == term[termInd])
                {
                    termInd++;
                    // We matched on all of them, we are at the end
                    if (termInd >= 4)
                        break;
                }
                else
                {
                    // No match - reset count
                    termInd = 0;
                }
            }

            if (i == 0)
            {
                System.out.println("Stream EOF - thread returning");
                return;
            }

            String strBuf = new String(buf, 0, i);
            System.out.println("got message: " + strBuf);

            StringTokenizer tok = new StringTokenizer(strBuf);
            String method = tok.nextToken();
            String query = tok.nextToken();

            if (query != null)
            {
                if (query.startsWith("/"))
                    query = query.substring(1);
                if (query.startsWith("?"))
                    query = query.substring(1);
            }

            System.out.println("Query: " + query);
            Map args = new HashMap();
            String name = null;
            String val = "";
            StringTokenizer st = new StringTokenizer(URIUtil.decode(query),
                                                     "&=",
                                                     true);
            while (st.hasMoreTokens())
            {
                String token = st.nextToken();
                System.out.println("Token: " + token);
                if (token.equals("&"))
                {
                    val = val.replace('_', ' ');
                    args.put(name, val);
                    name = null;
                    val = "";
                }
                else if (token.equals("="))
                {
                    if (name == null)
                        throw new IllegalArgumentException("no arg name!");
                }
                else
                {
                    if (name == null)
                        name = token;
                    else
                        val = token;
                }
            }

            // Hack to allow spaces in the values
            val = val.replace('_', ' ');
            if (name != null)
                args.put(name, val);

            if (args.get("debug") != null)
                debug = true;

            _statusLine = (String)args.get("status");
            System.out.println("status: " + _statusLine);

            if ((String)args.get("endOfLine") != null)
            {
                _endOfLine = (String)args.get("endOfLine");
                System.out.println("endOfLine: " + _endOfLine);
                _endOfLine = _endOfLine.replace('N', '\n');
                _endOfLine = _endOfLine.replace('R', '\r');
            }

            _version = (String)args.get("version");
            if (_version == null)
                _version = "1.1";

            if (args.get("close") != null)
                _closeConnection = true;

            if (args.get(ERROR_KEEP_ALIVE) != null)
                _keepAlive = true;

            if (args.get(POST_NO_DATA) != null)
                _postNoData = true;

            if (args.get("noContentLength") != null)
                _contentLength = false;

            if (args.get("badContentLength") != null)
                _badContentLength = true;

            if (args.get("spaceContentLength") != null)
                _spaceContentLength = true;

            // First check for shutdown message
            if (args.get("quit") != null)
            {
                this.setupOutput();
                this.println("HTTP/" + _version + " 200 OK");
                this.println();
                this.println("goodbye!");
                request.close();
                _quit = true;
            }

            if (method.equalsIgnoreCase("post"))
                this.post = true;

            if (debug)
                System.out.println("ErrorServer received request: " + query);

            errorType = (String)args.get("error");
            if (errorType == null)
                errorType = defaultError;
            errorLoc = (String)args.get("when");
            if (errorLoc == null)
                errorLoc = "before-headers";

            String linesStr = (String)args.get("lines");
            String secStr = (String)args.get("sec");

            if (linesStr != null)
                errorLines = Integer.parseInt(linesStr);
            else
                errorLines = 0;

            if (secStr != null)
                errorSec = Integer.parseInt(secStr);
            else
                errorSec = 0;

            String idleTimeout = (String)args.get(ERROR_IDLE_TIMEOUT);
            if (idleTimeout != null)
                _idleTimeout = Integer.parseInt(idleTimeout);
            else
                _idleTimeout = 0;

            Thread timeoutThread = new Thread()
            {
                public void run()
                {
                    try
                    {
                        System.out.println("Starting timeout wait: "
                            + _idleTimeout);
                        Thread.sleep(_idleTimeout);
                        System.out.println("Connection timeout");
                        System.out.println("time: "
                            + System.currentTimeMillis());
                        if (is != null)
                            is.close();
                        if (request != null)
                            request.close();
                        request = null;
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            };

            if (_idleTimeout != 0)
            {
                timeoutThread.start();
            }

            // If we aren't already in a retry loop, set number of
            // times to fail before success now.
            if (succeedAfter == 0)
            {
                String successStr = (String)args.get("success");
                if (successStr != null)
                    succeedAfter = Integer.parseInt(successStr);
            }

            if (debug)
            {
                System.out.println("errorType = " + errorType);
                System.out.println("errorLoc = " + errorLoc);
                System.out.println("errorLines = " + errorLines);
                System.out.println("errorSec = " + errorSec);
                System.out.println("succeedAfter = " + succeedAfter);
                System.out.println("statusLine = " + _statusLine);
            }

            if (post)
            {
                if (errorLoc.equalsIgnoreCase(ERROR_BEFORE_READ)
                    && this.simulateError())
                    return;
                if (!_postNoData)
                {
                    while (true)
                    {
                        int c = is.read();
                        if (c == -1)
                            break;
                        if (errorLoc.equalsIgnoreCase(ERROR_DURING_READ)
                            && this.simulateError())
                            return;
                    }
                }
            }
            if (errorLoc.equalsIgnoreCase(ERROR_BEFORE_HEADERS)
                && this.simulateError())
                return;

            setupOutput();

            if (_statusLine != null)
                this.println(_statusLine);
            else
                this.println("HTTP/" + _version + " 200 OK");

            if (_closeConnection)
            {
                closeConnection = true;
                this.println("Connection: close");
            }

            if (_keepAlive)
            {
                closeConnection = false;
                this.println("Connection: keep-alive");
            }

            this.println("Cache-Control: no-cache");

            if (errorLoc.equalsIgnoreCase("during-headers")
                && this.simulateError())
                return;

            this.println("Content-Type: text/html");
            if (_contentLength)
            {
                String cl;
                // Adjust the content length depending on the EOL style
                // we are using
                if (_endOfLine.length() == 2)
                    cl = Integer.toString(CONTENT_LENGTH);
                else
                    cl = Integer.toString(CONTENT_LENGTH - CONTENT_EOLS);

                if (_badContentLength)
                    cl = "badcl";
                if (_spaceContentLength)
                    cl += "  ";
                this.println("Content-Length: " + cl);
            }

            this.println();

            if (errorLoc.equalsIgnoreCase(ERROR_BEFORE_CONTENT)
                && this.simulateError())
                return;

            NumberFormat fmt = NumberFormat.getInstance();
            fmt.setMinimumIntegerDigits(3);
            this.print("<html><body><table border=\"1\">");
            this.println("<!-- xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx -->");

            for (int ln = 1; ln <= CONTENT_LOOPS; ln++)
            {
                this.print("<tr><td>Line " + fmt.format(ln) + "</td>");
                this.println("<td>xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
                    + "</td></tr>");
                if (errorLines == ln
                    && errorLoc.equalsIgnoreCase(ERROR_DURING_CONTENT))
                {
                    if (this.simulateError())
                        return;
                }
            }
            this.println("</table></body></html>");

            if (closeConnection)
            {
                request.close();
                request = null;
            }
        }

    }

    public void run()
    {
        try
        {
            handleRequest();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                if (request != null)
                    request.close();
            }
            catch (IOException ie)
            {
            }
        }
    }

    // This was an attempt to make URLConnection.connect() timeout.
    // It didn't work.
    /*
     * 
     * static class BusyThread extends Thread { public void run() { ServerSocket
     * busyListener; Socket s; try { // Setup listening port busyListener = new
     * ServerSocket(BUSY_PORT_NUMBER); // Don't accept so it will just timeout
     * //s = busyListener.accept(); //Thread.sleep(10000); //s.close(); }
     * catch(Exception e) { e.printStackTrace(); System.exit(1); return; } } }
     */

    public static void main(String argv[])
    {
        ServerSocket listener;
        ErrorServer handler = null;
        int port = PORT_NUMBER;

        if (_running)
            return;
        _running = true;

        try
        {
            // Setup listening port
            if (argv.length > 0)
                port = Integer.parseInt(argv[0]);
            if (argv.length > 1)
                defaultError = argv[1];
            listener = new ServerSocket(port);
            // Setup "busy" listener to test connect timeout
            // new BusyThread().start();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.exit(1);
            return;
        }

        // Handle incoming requests until handleRequests detects a "quit"
        // message
        do
        {
            try
            {
                handler = new ErrorServer(listener.accept());
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            handler.start();
        }
        while (!_quit);
    }
}
