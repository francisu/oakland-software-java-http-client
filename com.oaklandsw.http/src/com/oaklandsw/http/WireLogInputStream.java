//
// Some portions copyright 2002-3003, oakland software, all rights reserved.
//
// May not be used or redistributed without specific written
// permission from oakland software.
//
/*
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
 * 4. The names "The Jakarta Project", "HttpClient", and "Apache Software
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

import java.io.FilterInputStream;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Logs all data read to the wire log.
 * 
 * @author Ortwin Gl�ck
 * 
 * @since 2.0
 */

class WireLogInputStream extends FilterInputStream
{

    private static final Log _wireLog = LogFactory
                                              .getLog(HttpConnection.WIRE_LOG);

    private StringBuffer     _traceBuff;

    public WireLogInputStream(InputStream inStr)
    {
        super(inStr);
        _traceBuff = new StringBuffer();
    }

    private void traceChar(char b)
    {
        WireLogOutputStream.traceChar(_traceBuff, b);
        if (b == '\n')
            dumpBuff();
    }

    public int read() throws java.io.IOException
    {
        int b = super.read();
        traceChar((char)b);
        return b;
    }

    public int read(byte[] b, int off, int len) throws java.io.IOException
    {
        int l = super.read(b, off, len);
        if (l > 0)
        {
            for (int i = 0; i < l; i++)
                traceChar((char)b[off + i]);
        }
        return l;
    }

    private final void dumpBuff()
    {
        if (_traceBuff.length() != 0)
        {
            // Don't write the trailing newline
            String tbString = _traceBuff.toString();
            if (tbString.charAt(_traceBuff.length() - 1) == '\n')
                tbString = tbString.substring(0, _traceBuff.length() - 1);
            _wireLog.debug("<< " + tbString);
            _traceBuff = new StringBuffer();
        }
    }

    public void close() throws java.io.IOException
    {
        dumpBuff();
        super.close();
    }

}
