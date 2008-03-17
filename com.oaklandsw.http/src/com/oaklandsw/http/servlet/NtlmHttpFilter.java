/*
 * jcifs smb client library in Java Copyright (C) 2002 "Michael B. Allen" <jcifs
 * at samba dot org> "Jason Pugsley" <jcifs at samba dot org> "skeetz" <jcifs at
 * samba dot org> "Eric Glass" <jcifs at samba dot org> and Marcel, Thomas, ...
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
// This is substantially identical to it's corresponding
// class is JCIFS (version 1.2.13), with some warnings cleaned up and
// some additional tracing added.
package com.oaklandsw.http.servlet;

import java.io.*;
import java.util.Enumeration;
import javax.servlet.*;
import javax.servlet.http.*;

import com.oaklandsw.util.LogUtils;
import com.oaklandsw.util.Util;

import jcifs.*;
import jcifs.smb.NtStatus;
import jcifs.smb.SmbSession;
import jcifs.smb.NtlmChallenge;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbAuthException;
import jcifs.util.Base64;
import jcifs.util.LogStream;

/**
 * This servlet Filter can be used to negotiate password hashes with MSIE
 * clients using NTLM SSP. This is similar to <tt>Authentication:
 * BASIC</tt>
 * but weakly encrypted and without requiring the user to re-supply
 * authentication credentials.
 * <p>
 * Read <a href="../../../ntlmhttpauth.html">jCIFS NTLM HTTP Authentication and
 * the Network Explorer Servlet</a> for complete details.
 */

public class NtlmHttpFilter implements Filter
{

    private static LogStream log = LogStream.getInstance();

    private String           defaultDomain;
    private String           domainController;
    private boolean          loadBalance;
    private boolean          enableBasic;
    private boolean          insecureBasic;
    private String           realm;

    public void init(FilterConfig filterConfig) throws ServletException
    {
        String name;
        int level;

        /*
         * Set jcifs properties we know we want; soTimeout and cachePolicy to
         * 10min.
         */
        Config.setProperty("jcifs.smb.client.soTimeout", "300000");
        Config.setProperty("jcifs.netbios.cachePolicy", "1200");

        Enumeration e = filterConfig.getInitParameterNames();
        while (e.hasMoreElements())
        {
            name = (String)e.nextElement();
            if (name.startsWith("jcifs."))
            {
                Config.setProperty(name, filterConfig.getInitParameter(name));
            }
        }
        defaultDomain = Config.getProperty("jcifs.smb.client.domain");
        domainController = Config.getProperty("jcifs.http.domainController");
        if (domainController == null)
        {
            domainController = defaultDomain;
            loadBalance = Config.getBoolean("jcifs.http.loadBalance", true);
        }
        enableBasic = Boolean.valueOf(Config
                .getProperty("jcifs.http.enableBasic")).booleanValue();
        insecureBasic = Boolean.valueOf(Config
                .getProperty("jcifs.http.insecureBasic")).booleanValue();
        realm = Config.getProperty("jcifs.http.basicRealm");
        if (realm == null)
            realm = "jCIFS";

        if ((level = Config.getInt("jcifs.util.loglevel", -1)) != -1)
        {
            LogStream.setLevel(level);
        }

        if (LogStream.level > 2)
        {
            // This not not work unles log4j is setup in the app server
            LogUtils.logAll();

            log.println("oaklandsw - NtlmHttpFilter init: defaultDomain: "
                + defaultDomain
                + " domainController: "
                + domainController);
            try
            {
                Config.store(log, "JCIFS PROPERTIES");
            }
            catch (IOException ioe)
            {
            }
        }
    }

    public void destroy()
    {
    }

    /**
     * This method simply calls <tt>negotiate( req, resp, false )</tt> and
     * then <tt>chain.doFilter</tt>. You can override and call negotiate
     * manually to achive a variety of different behavior.
     */
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain)
        throws IOException,
            ServletException
    {
        HttpServletRequest req = (HttpServletRequest)request;
        HttpServletResponse resp = (HttpServletResponse)response;
        NtlmPasswordAuthentication ntlm;

        if (LogStream.level > 3)
        {
            log.println("oaklandsw - Start Authentication " + req);
        }

        if ((ntlm = negotiate(req, resp, false)) == null)
        {
            if (LogStream.level > 1)
            {
                log
                        .println("oaklandsw - Authentication failed or not complete "
                            + "- not calling downstream filters");
            }
            return;
        }

        if (LogStream.level > 2)
        {
            log.println("oaklandsw - Authentication OK: " + ntlm);
        }
        chain.doFilter(new NtlmHttpServletRequest(req, ntlm), response);
    }

    /**
     * Negotiate password hashes with MSIE clients using NTLM SSP
     * 
     * @param req
     *            The servlet request
     * @param resp
     *            The servlet response
     * @param skipAuthentication
     *            If true the negotiation is only done if it is initiated by the
     *            client (MSIE post requests after successful NTLM SSP
     *            authentication). If false and the user has not been
     *            authenticated yet the client will be forced to send an
     *            authentication (server sends
     *            HttpServletResponse.SC_UNAUTHORIZED).
     * @return True if the negotiation is complete, otherwise false
     */
    protected NtlmPasswordAuthentication negotiate(HttpServletRequest req,
                                                   HttpServletResponse resp,
                                                   boolean skipAuthentication)
        throws IOException,
            ServletException
    {
        UniAddress dc;
        String msg;
        NtlmPasswordAuthentication ntlm = null;
        msg = req.getHeader("Authorization");
        boolean offerBasic = enableBasic && (insecureBasic || req.isSecure());

        if (msg != null
            && (msg.startsWith("NTLM ") || (offerBasic && msg
                    .startsWith("Basic "))))
        {
            if (msg.startsWith("NTLM "))
            {
                HttpSession ssn = req.getSession();
                byte[] challenge;

                if (loadBalance)
                {
                    NtlmChallenge chal = (NtlmChallenge)ssn
                            .getAttribute("NtlmHttpChal");
                    if (chal == null)
                    {
                        chal = SmbSession.getChallengeForDomain();
                        ssn.setAttribute("NtlmHttpChal", chal);
                    }
                    dc = chal.dc;
                    challenge = chal.challenge;
                }
                else
                {
                    dc = UniAddress.getByName(domainController, true);
                    challenge = SmbSession.getChallenge(dc);
                }

                if (LogStream.level > 3)
                {
                    log.println("oaklandsw - NtlmHttpFilter: "
                        + msg
                        + " Got Challenge: "
                        + Util.bytesToString(challenge));
                }

                if ((ntlm = NtlmSsp.authenticate(req, resp, challenge)) == null)
                {
                    if (LogStream.level > 1)
                    {
                        log.println("oaklandsw - NtlmHttpFilter: "
                            + msg
                            + " Failed authenticate step");
                    }
                    return null;
                }

                /* negotiation complete, remove the challenge object */
                ssn.removeAttribute("NtlmHttpChal");
            }
            else
            {
                String auth = new String(Base64.decode(msg.substring(6)),
                                         "US-ASCII");
                int index = auth.indexOf(':');
                String user = (index != -1) ? auth.substring(0, index) : auth;
                String password = (index != -1)
                    ? auth.substring(index + 1)
                    : "";
                index = user.indexOf('\\');
                if (index == -1)
                    index = user.indexOf('/');
                String domain = (index != -1)
                    ? user.substring(0, index)
                    : defaultDomain;
                user = (index != -1) ? user.substring(index + 1) : user;
                ntlm = new NtlmPasswordAuthentication(domain, user, password);
                dc = UniAddress.getByName(domainController, true);
            }
            try
            {

                if (LogStream.level > 3)
                {
                    log.println("oaklandsw - NtlmHttpFilter: "
                        + ntlm
                        + " before logon "
                        + dc);
                }

                SmbSession.logon(dc, ntlm);

                if (LogStream.level > 3)
                {
                    log.println("oaklandsw - NtlmHttpFilter: "
                        + ntlm
                        + " successfully authenticated against "
                        + dc);
                }
            }
            catch (SmbAuthException sae)
            {
                if (LogStream.level > 1)
                {
                    log.println("oaklandsw - NtlmHttpFilter: "
                        + ntlm.getName()
                        + ": 0x"
                        + jcifs.util.Hexdump.toHexString(sae.getNtStatus(), 8)
                        + ": "
                        + sae);
                }
                if (sae.getNtStatus() == NtStatus.NT_STATUS_ACCESS_VIOLATION)
                {
                    /*
                     * Server challenge no longer valid for externally supplied
                     * password hashes.
                     */
                    HttpSession ssn = req.getSession(false);
                    if (ssn != null)
                    {
                        ssn.removeAttribute("NtlmHttpAuth");
                    }
                }
                resp.setHeader("WWW-Authenticate", "NTLM");
                if (offerBasic)
                {
                    resp.addHeader("WWW-Authenticate", "Basic realm=\""
                        + realm
                        + "\"");
                }
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.setContentLength(0); /* Marcel Feb-15-2005 */
                resp.flushBuffer();
                return null;
            }
            req.getSession().setAttribute("NtlmHttpAuth", ntlm);
        }
        else
        {
            if (!skipAuthentication)
            {
                HttpSession ssn = req.getSession(false);
                if (ssn == null
                    || (ntlm = (NtlmPasswordAuthentication)ssn
                            .getAttribute("NtlmHttpAuth")) == null)
                {
                    if (LogStream.level > 1)
                    {
                        log.println("oaklandsw - NtlmHttpFilter: "
                            + msg
                            + " Failed could not get session");
                    }

                    resp.setHeader("WWW-Authenticate", "NTLM");
                    if (offerBasic)
                    {
                        resp.addHeader("WWW-Authenticate", "Basic realm=\""
                            + realm
                            + "\"");
                    }
                    resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    resp.setContentLength(0);
                    resp.flushBuffer();
                    return null;
                }
            }
        }

        return ntlm;
    }

    // Added by cgross to work with weblogic 6.1.
    public void setFilterConfig(FilterConfig f)
    {
        try
        {
            init(f);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public FilterConfig getFilterConfig()
    {
        return null;
    }
}
