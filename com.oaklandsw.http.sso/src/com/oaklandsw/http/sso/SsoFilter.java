package com.oaklandsw.http.sso;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.bouncycastle.util.encoders.Base64;

import jcifs.Config;
import jcifs.UniAddress;
import jcifs.smb.NtStatus;
import jcifs.smb.NtlmChallenge;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbAuthException;
import jcifs.smb.SmbException;
import jcifs.smb.SmbSession;
import jcifs.util.LogStream;

import com.oaklandsw.http.Authenticator;
import com.oaklandsw.http.HttpException;
import com.oaklandsw.http.ntlm.AuthenticateMessage;
import com.oaklandsw.http.ntlm.ChallengeMessage;
import com.oaklandsw.http.ntlm.Message;
import com.oaklandsw.http.ntlm.NegotiateMessage;
import com.oaklandsw.http.ntlm.Ntlm;
import com.oaklandsw.util.LogUtils;

public class SsoFilter implements Filter
{

    // log4j logging
    private static final Log   _log                         = LogUtils
                                                                    .makeLogger();

    // Logging through JCIFS
    private static LogStream   _jcifsLog                    = LogStream
                                                                    .getInstance();

    public static final int    AUTH_NTLMv1                  = 0x01;
    public static final int    AUTH_NTLMv2                  = 0x02;
    public static final int    AUTH_BASIC                   = 0x04;
    public static final int    AUTH_KERBEROS                = 0x08;

    protected int              _supportedAuths              = AUTH_NTLMv1
                                                                | AUTH_NTLMv2;

    private String             domainController;
    private boolean            loadBalance;
    private String             realm;

    /**
     * The domain given to the client upon request as the default domain.
     * Specify this or jcifs.smb.client.server, but not both. This must be
     * specified if a domain is being used for authentication.
     */
    static String              TARGET_DOMAIN;                                         // jcifs.smb.client.domain

    /**
     * The server given to the client upon request as the default server. This
     * is specified if the server is not part of a domain. Specify this or
     * jcifs.smb.client.domain but not both.
     */
    static String              TARGET_SERVER;                                         // jcifs.smb.client.server

    /*
     * Session attributes.
     */

    /**
     * The domain provided by the client.
     */
    public static final String CLIENT_DOMAIN_ATTRIBUTE      = "NtlmClientDomain";

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

        if ((level = Config.getInt("jcifs.util.loglevel", -1)) != -1)
        {
            LogStream.setLevel(level);
        }

        TARGET_DOMAIN = Config.getProperty("jcifs.smb.client.domain");
        TARGET_SERVER = Config.getProperty("jcifs.smb.client.server");

        domainController = Config.getProperty("jcifs.http.domainController");
        if (domainController == null)
        {
            domainController = TARGET_DOMAIN;
            loadBalance = Config.getBoolean("jcifs.http.loadBalance", true);
        }

        if (LogStream.level > 2)
        {
            // This will not work unless log4j is setup in the app server
            LogUtils.logAll();
            try
            {
                Config.store(_jcifsLog, "JCIFS PROPERTIES");
            }
            catch (IOException ioe)
            {
            }
        }

    }

    public void destroy()
    {
    }

    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain)
        throws IOException,
            ServletException
    {
        HttpServletRequest req = (HttpServletRequest)request;
        HttpServletResponse resp = (HttpServletResponse)response;
        NtlmPasswordAuthentication principal;

        if (_log.isDebugEnabled())
            _log.debug("doFilter " + req);

        if ((principal = processAuthentication(req, resp)) == null)
        {
            return;
        }

        if (_log.isDebugEnabled())
            _log.debug("doFilter - success: " + principal);

        chain.doFilter(new SsoRequestWrapper(req, principal), response);
    }

    protected static final String NTLM            = "NTLM";
    protected static final String BASIC           = "Basic";
    protected static final String KERBEROS        = "Kerberos";

    protected static final String AUTH_HEADER     = Authenticator.RESP_HEADERS[0];
    protected static final String WWW_AUTH_HEADER = Authenticator.REQ_HEADERS[0];

    protected NtlmPasswordAuthentication processAuthentication(HttpServletRequest req,
                                                               HttpServletResponse resp)
    {
        UniAddress dc;
        String msg;
        NtlmPasswordAuthentication principal = null;
        msg = req.getHeader(AUTH_HEADER);
        try
        {
            if (msg != null)
            {
                // Start from the most desirable
                if (msg.startsWith(KERBEROS))
                    principal = processKerberosAuthentication(req, resp, msg);
                else if (msg.startsWith(NTLM))
                    principal = processNtlmAuthentication(req, resp, msg);
                else if (msg.startsWith(BASIC))
                    principal = processBasicAuthentication(req, resp, msg);
            }

            // The authentication process got us a principal, see if
            // it's good
            if (principal != null)
            {
                SmbSession.logon(dc, principal);
                return principal;
            }
        }
        catch (Exception ex)
        {
            _log.error(ex);
            // Fall through to try again
        }

        // Go around again

        // The header may have already been set above
        if (!resp.containsHeader(WWW_AUTH_HEADER))
        {
            if ((_supportedAuths & (AUTH_NTLMv1 | AUTH_NTLMv2)) != 0)
            {
                resp.setHeader(WWW_AUTH_HEADER, NTLM);
            }

            if ((_supportedAuths & AUTH_BASIC) != 0)
            {
                resp.addHeader(WWW_AUTH_HEADER, BASIC
                    + " realm=\""
                    + realm
                    + "\"");
            }
        }
        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        resp.setContentLength(0);
        try
        {
            resp.flushBuffer();
        }
        catch (IOException e)
        {
            // We don't care
        }
        return null;
    }

    protected NtlmPasswordAuthentication processNtlmAuthentication(HttpServletRequest req,
                                                                   HttpServletResponse resp,
                                                                   String challenge)
        throws HttpException,
            SmbException,
            UnknownHostException,
            UnsupportedEncodingException
    {
        HttpSession ssn = req.getSession();
        UniAddress dc;
        byte[] nonce;

        try
        {
            challenge = challenge.substring(challenge.toLowerCase()
                    .indexOf(NTLM)
                + NTLM.length()).trim();
        }
        catch (IndexOutOfBoundsException e)
        {
            throw new HttpException("Invalid NTLM challenge: " + challenge);
        }

        Message msg = new Message();
        msg.setBytes(Base64.decode(challenge.getBytes(Ntlm.ENCODING)));
        int index = msg.decode();

        switch (msg.getType())
        {
            case Message.MSG_NEGOTIATE:

                // Received negotiate message
                NegotiateMessage nmsg = new NegotiateMessage();
                nmsg.setBytes(msg.getBytes());
                nmsg.decodeAfterHeader(index);

                // Save this in case we don't get a domain from the auth message
                if ((nmsg.getFlags() & Message.NEGOTIATE_OEM_DOMAIN_SUPPLIED) != 0
                    && nmsg.getDomain() != null)
                {
                    ssn.setAttribute(CLIENT_DOMAIN_ATTRIBUTE, nmsg.getDomain());
                }

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
                    nonce = chal.challenge;
                }
                else
                {
                    dc = UniAddress.getByName(domainController, true);
                    nonce = SmbSession.getChallenge(dc);
                }

                Ntlm._challengeMessageFlags = Message.NEGOTIATE_NTLM
                    | Message.NEGOTIATE_ALWAYS_SIGN
                    | Message.REQUEST_TARGET;

                ChallengeMessage challengeMsg = new ChallengeMessage();

                challengeMsg.setupOutgoing(nonce,
                                           nmsg,
                                           TARGET_DOMAIN,
                                           TARGET_SERVER);
                challengeMsg.encode();
                String responseMsg = new String(Base64.encode(challengeMsg
                        .getBytes()), Ntlm.ENCODING);
                resp.setHeader("WWW-Authenticate", "NTLM " + responseMsg);
                break;
            case Message.MSG_AUTHENTICATE:

                // Received authenticate message
                AuthenticateMessage amsg = new AuthenticateMessage();
                amsg.setBytes(msg.getBytes());
                amsg.decodeAfterHeader(index);

                if (amsg._user == null)
                {
                    throw new IllegalArgumentException("Username must not be null");
                }

                if (amsg._domain == null)
                {
                    // Get the domain from the negotiate message
                    amsg._domain = (String)ssn
                            .getAttribute(CLIENT_DOMAIN_ATTRIBUTE);
                    if (amsg._domain == null)
                    {
                        amsg._domain = TARGET_DOMAIN;
                    }
                }

                byte[] lmResponse = amsg._lmResponse;
                if (lmResponse == null)
                    lmResponse = new byte[0];
                byte[] ntResponse = amsg._ntResponse;
                if (ntResponse == null)
                    ntResponse = new byte[0];
                return new NtlmPasswordAuthentication(amsg._domain,
                                                      amsg._user,
                                                      nonce,
                                                      lmResponse,
                                                      ntResponse);
            default:
                throw new HttpException("Invalid message type: "
                    + msg.getType());
        }
        return null;
    }

    protected NtlmPasswordAuthentication processBasicAuthentication(HttpServletRequest req,
                                                                    HttpServletResponse resp,
                                                                    String msg)
    {
        return null;
    }

    protected NtlmPasswordAuthentication processKerberosAuthentication(HttpServletRequest req,
                                                                       HttpServletResponse resp,
                                                                       String msg)
    {
        return null;
    }

}
