/*
 * jcifs smb client library in Java Copyright (C) 2002 "Michael B. Allen" <jcifs
 * at samba dot org> "Eric Glass" <jcifs at samba dot org> "Jason Pugsley"
 * <jcifs at samba dot org> "skeetz" <jcifs at samba dot org>
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

// Oakland Software modifications:
// 
// This is very similar to it's corresponding
// class is JCIFS (version 1.2.13), the only change being to call the
// Oakland Software NTLM authentication methods.
//

package com.oaklandsw.http.servlet;

import java.io.IOException;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.oaklandsw.http.ntlm.AuthenticateMessage;
import com.oaklandsw.http.ntlm.ChallengeMessage;
import com.oaklandsw.http.ntlm.NegotiateMessage;

import jcifs.smb.NtlmPasswordAuthentication;

import jcifs.util.Base64;

import jcifs.ntlmssp.NtlmFlags;
import jcifs.ntlmssp.Type2Message;

/**
 * This class is used internally by <tt>NtlmHttpFilter</tt>,
 * <tt>NtlmServlet</tt>, and <tt>NetworkExplorer</tt> to negiotiate
 * password hashes via NTLM SSP with MSIE. It might also be used directly by
 * servlet containers to incorporate similar functionality.
 * <p>
 * How NTLMSSP is used in conjunction with HTTP and MSIE clients is described in
 * an <A HREF="http://www.innovation.ch/java/ntlm.html">NTLM Authentication
 * Scheme for HTTP</A>.
 * <p>
 * Also, read <a href="../../../ntlmhttpauth.html">jCIFS NTLM HTTP
 * Authentication and the Network Explorer Servlet</a> related information.
 */

public class NtlmSsp implements NtlmFlags
{

    /**
     * Calls the static {@link #authenticate(HttpServletRequest,
     * HttpServletResponse, byte[])} method to perform NTLM authentication for
     * the specified servlet request.
     * 
     * @param req
     *            The request being serviced.
     * @param resp
     *            The response.
     * @param challenge
     *            The domain controller challenge.
     * @throws IOException
     *             If an IO error occurs.
     * @throws ServletException
     *             If an error occurs.
     */
    public NtlmPasswordAuthentication doAuthentication(HttpServletRequest req,
                                                       HttpServletResponse resp,
                                                       byte[] challenge)
        throws IOException,
            ServletException
    {
        return authenticate(req, resp, challenge);
    }

    /**
     * Performs NTLM authentication for the servlet request.
     * 
     * @param req
     *            The request being serviced.
     * @param resp
     *            The response.
     * @param challenge
     *            The domain controller challenge.
     * @throws IOException
     *             If an IO error occurs.
     * @throws ServletException
     *             If an error occurs.
     */
    public static NtlmPasswordAuthentication authenticate(HttpServletRequest req,
                                                          HttpServletResponse resp,
                                                          byte[] challenge)
        throws IOException,
            ServletException
    {
        String msg = req.getHeader("Authorization");
        if (msg != null && msg.startsWith("NTLM "))
        {
            byte[] src = Base64.decode(msg.substring(5));
            if (src[8] == 1)
            {
                // Received negotiate message
                NegotiateMessage nmsg = new NegotiateMessage();
                nmsg.setBytes(src);
                nmsg.decode();

                ChallengeMessage challengeMsg = new ChallengeMessage();
                challengeMsg.setupOutgoing(challenge, nmsg, Type2Message
                        .getDefaultTargetInformation(), Type2Message
                        .getDefaultDomain());
                challengeMsg.encode();
                msg = Base64.encode(challengeMsg.getBytes());
                resp.setHeader("WWW-Authenticate", "NTLM " + msg);
            }
            else if (src[8] == 3)
            {
                // Received authenticate message
                AuthenticateMessage amsg = new AuthenticateMessage();
                amsg.setBytes(src);
                amsg.decode();

                byte[] lmResponse = amsg._lmResponse;
                if (lmResponse == null)
                    lmResponse = new byte[0];
                byte[] ntResponse = amsg._ntResponse;
                if (ntResponse == null)
                    ntResponse = new byte[0];
                return new NtlmPasswordAuthentication(amsg._domain,
                                                      amsg._user,
                                                      challenge,
                                                      lmResponse,
                                                      ntResponse);
            }
        }
        else
        {
            resp.setHeader("WWW-Authenticate", "NTLM");
        }
        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        resp.setContentLength(0);
        resp.flushBuffer();
        return null;
    }

}