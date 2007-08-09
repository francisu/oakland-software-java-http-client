/*
 * $Header:
 * /home/cvspublic/jakarta-commons/httpclient/src/test/org/apache/commons/httpclient/SimpleHttpConnection.java,v
 * 1.1 2002/08/08 14:51:19 jsdever Exp $ $Revision: 1.1 $ $Date: 2002/08/08
 * 14:51:19 $
 * ====================================================================
 * 
 * The Apache Software License, Version 1.1
 * 
 * Copyright (c) 1999-2002 The Apache Software Foundation. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any, must
 * include the following acknowlegement: "This product includes software
 * developed by the Apache Software Foundation (http://www.apache.org/)."
 * Alternately, this acknowlegement may appear in the software itself, if and
 * wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
 * Foundation" must not be used to endorse or promote products derived from this
 * software without prior written permission. For written permission, please
 * contact apache@apache.org.
 * 
 * 5. Products derived from this software may not be called "Apache" nor may
 * "Apache" appear in their names without prior written permission of the Apache
 * Group.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE APACHE
 * SOFTWARE FOUNDATION OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many individuals on
 * behalf of the Apache Software Foundation. For more information on the Apache
 * Software Foundation, please see <http://www.apache.org/>.
 * 
 * [Additional notices, if required by prior licensing conditions]
 * 
 */

package com.oaklandsw.http;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Vector;

import org.apache.commons.logging.Log;

import com.oaklandsw.http.HttpConnection;
import com.oaklandsw.util.ExposedBufferInputStream;
import com.oaklandsw.util.LogUtils;

/**
 * For test-nohost testing purposes only.
 * 
 * @author <a href="mailto:jsdever@apache.org">Jeff Dever </a>
 */
public class SimpleHttpConnection extends HttpConnection
{
    private static final Log _log            = LogUtils.makeLogger();

    int                      hits            = 0;

    Vector                   bodies          = new Vector();

    ByteArrayInputStream     bodyInputStream = null;

    ByteArrayOutputStream    _bitBucket      = new ByteArrayOutputStream();

    static GlobalState       _globalState;

    static
    {
        _globalState = new GlobalState();
    }

    public void addResponse(String body)
    {
        _log.debug("addResponse: " + body);
        bodies.add(body);
    }

    protected void setup()
    {
        _connManager = new HttpConnectionManager(_globalState);
        _globalState._connManager = _connManager;
    }

    public SimpleHttpConnection()
    {
        super(null, -1, "localhost", 80, false, "none");
        setup();
        _connectionInfo = new ConnectionInfo(_connManager, "localhost");
        _output = new BufferedOutputStream(new ByteArrayOutputStream());
    }

    public SimpleHttpConnection(String host, int port, boolean isSecure)
    {
        super(null, -1, host, port, isSecure, "none");
        setup();
        _connectionInfo = new ConnectionInfo(_connManager, host);
    }

    public void open() throws IOException
    {
        _state = CS_OPEN;
        _log.trace("open");
        try
        {
            bodyInputStream = new ByteArrayInputStream(((String)bodies
                    .elementAt(hits)).getBytes());
            hits++;
        }
        catch (ArrayIndexOutOfBoundsException aiofbe)
        {
            throw new IOException("SimpleHttpConnection has been opened more times "
                + "than it has responses.  You might need to call addResponse().");
        }
    }

    public void close()
    {
        _log.trace("close");
        if (bodyInputStream != null)
        {
            try
            {
                bodyInputStream.close();
            }
            catch (IOException e)
            {
            }
            bodyInputStream = null;
        }
    }

    public void checkConnection()
    {
        // ignore this
    }

    public ExposedBufferInputStream getInputStream()
    {
        return new ExposedBufferInputStream(bodyInputStream, 1024);
    }

    public BufferedOutputStream getOutputStream()
    {
        return new BufferedOutputStream(_bitBucket);
    }

}
