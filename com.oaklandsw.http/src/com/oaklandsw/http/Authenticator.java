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

import java.security.MessageDigest;
import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.bouncycastle.util.encoders.Base64;

import com.oaklandsw.http.ntlm.Ntlm;

/**
 * Utility methods for HTTP authorization and authentication. This class
 * provides utility methods for generating responses to HTTP www and proxy
 * authentication challenges.
 * 
 * <p>
 * Preemptive authentication can be turned on by using the property value of
 * #PREEMPTIVE_PROPERTY. If left unspecified, it has the default value of
 * #PREEMPTIVE_DEFAULT. This configurable behaviour conforms to rcf2617:
 * <blockquote>A client SHOULD assume that all paths at or deeper than the depth
 * of the last symbolic element in the path field of the Request-URI also are
 * within the protection space specified by the Basic realm value of the current
 * challenge. A client MAY preemptively send the corresponding Authorization
 * header with requests for resources in that space without receipt of another
 * challenge from the server. Similarly, when a client sends a request to a
 * proxy, it may reuse a userid and password in the Proxy-Authorization header
 * field without receiving another challenge from the proxy server.
 * </blockquote>
 * </p>
 */
public class Authenticator
{
    // ~ Static variables/initializers
    // ������������������������������������������

    /** The www authenticate challange header */
    public static final String  WWW_AUTH        = "WWW-Authenticate";

    /** The www authenticate response header */
    public static final String  WWW_AUTH_RESP   = "Authorization";

    /** The proxy authenticate challange header */
    public static final String  PROXY_AUTH      = "Proxy-Authenticate";

    /** The proxy authenticate response header */
    public static final String  PROXY_AUTH_RESP = "Proxy-Authorization";

    public static final String  BASIC           = "basic";

    public static final String  DIGEST          = "digest";

    public static final String  NTLM            = "ntlm";

    /**
     * Hexa values used when creating 32 character long digest in HTTP Digest in
     * case of authentication.
     * 
     * @see #encode(byte[])
     */
    private static final char[] HEXADECIMAL     = { '0', '1', '2', '3', '4',
        '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    // Used only for the regression tests
    public static final boolean authenticate(HttpURLConnectInternal method,
                                             String authReq,
                                             String respHeader)
        throws HttpException
    {
        Headers h = new Headers();
        if (authReq != null)
            h.add(WWW_AUTH, authReq);
        else
            h = null;

        // parse the authenticate header
        Map challengeMap = parseAuthenticateHeader(h, WWW_AUTH, method);

        return authenticate(method, h, WWW_AUTH, challengeMap, respHeader);
    }

    public static final boolean authenticate(HttpURLConnectInternal method,
                                             Headers reqAuthenticators,
                                             String reqType,
                                             Map challengeMap,
                                             String respHeader)
        throws HttpException
    {
        boolean preemptive = HttpURLConnection.getPreemptiveAuthentication();
        Log log = method.getLog();

        // if there is no challenge, attempt to use preemptive authorization
        if (reqAuthenticators == null)
        {
            if (preemptive)
            {
                log.debug("Preemptively sending default basic credentials");

                try
                {
                    String requestHeader = Authenticator.basic(null,
                                                               method,
                                                               respHeader);
                    method.addRequestProperty(respHeader, requestHeader);

                    return true;
                }
                catch (HttpException httpe)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("No default credentials to preemptively"
                            + " send");
                    }

                    return false;
                }
            }
            // no challenge and no default creds so do nothing
            return false;
        }

        // determine the most secure request header to add
        String requestHeader = null;
        if (challengeMap.containsKey(NTLM))
        {
            String challenge = (String)challengeMap.get(NTLM);
            requestHeader = Authenticator.ntlm(challenge,
                                               method,
                                               reqType,
                                               respHeader);
        }
        else if (challengeMap.containsKey(DIGEST))
        {
            String challenge = (String)challengeMap.get(DIGEST);
            String realm = parseRealmFromChallenge(challenge, method);
            requestHeader = Authenticator.digest(realm,
                                                 challenge,
                                                 method,
                                                 respHeader);
        }
        else if (challengeMap.containsKey(BASIC))
        {
            String challenge = (String)challengeMap.get(BASIC);
            String realm = parseRealmFromChallenge(challenge, method);
            requestHeader = Authenticator.basic(realm, method, respHeader);
        }
        else if (challengeMap.size() == 0)
        {
            throw new HttpException("No authentication scheme found in "
                + reqType
                + " header(s)");
        }
        else
        {
            throw new HttpException("Requested authentication "
                + "scheme "
                + challengeMap.keySet()
                + " is unsupported");
        }

        // Add the header if it has been created and return true
        if (requestHeader != null)
        {
            method.addRequestProperty(respHeader, requestHeader);
            return true;
        }
        return false;
    }

    /**
     * Creates an MD5 response digest.
     * 
     * @param uname
     *            Username
     * @param pwd
     *            Password
     * @param mapCreds
     *            map containing necessary header parameters to construct the
     *            digest. It must/can contain: uri, realm, nonce, cnonce, qop,
     *            nc.
     * 
     * @return The created digest as string. This will be the response tag's
     *         value in the Authentication HTTP header.
     * @throws HttpException
     *             when MD5 is an unsupported algorithm
     * 
     * @todo + Add createDigest() method
     */
    public static final String createDigest(String uname,
                                            String pwd,
                                            Map mapCreds) throws HttpException
    {
        final String digAlg = "MD5";

        // Collecting required tokens
        String uri = removeQuotes((String)mapCreds.get("uri"));
        String realm = removeQuotes((String)mapCreds.get("realm"));
        String nonce = removeQuotes((String)mapCreds.get("nonce"));
        String nc = removeQuotes((String)mapCreds.get("nc"));
        String cnonce = removeQuotes((String)mapCreds.get("cnonce"));
        String qop = removeQuotes((String)mapCreds.get("qop"));
        String method = (String)mapCreds.get("methodname");

        if (qop != null)
        {
            qop = "auth";
        }

        MessageDigest md5Helper;

        try
        {
            md5Helper = MessageDigest.getInstance(digAlg);
        }
        catch (Exception e)
        {
            throw new HttpException("Unsupported algorithm in HTTP Digest "
                + "authentication: "
                + digAlg);
        }

        // Calculating digest according to rfc 2617
        String a2 = method + ":" + uri;
        String md5a2 = encode(md5Helper.digest(a2.getBytes()));
        String digestValue = uname + ":" + realm + ":" + pwd;
        String md5a1 = encode(md5Helper.digest(digestValue.getBytes()));
        String serverDigestValue;

        if (qop == null)
        {
            serverDigestValue = md5a1 + ":" + nonce + ":" + md5a2;
        }
        else
        {
            serverDigestValue = md5a1
                + ":"
                + nonce
                + ":"
                + nc
                + ":"
                + cnonce
                + ":"
                + qop
                + ":"
                + md5a2;
        }

        String serverDigest = encode(md5Helper.digest(serverDigestValue
                .getBytes()));

        return serverDigest;
    }

    private static final String basic(String realm,
                                      HttpURLConnectInternal method,
                                      String respHeader) throws HttpException
    {
        boolean proxy = PROXY_AUTH_RESP.equals(respHeader);

        // We previously sent an response message and got back a 401/407,
        // this means we are not going to authenticate
        Credential sentCred = method.getCredentialSent(proxy);
        if (sentCred != null)
        {
            throw new HttpException("Basic Authentication failed; "
                + "with credential "
                + sentCred);
        }

        UserCredential cred = null;

        try
        {
            cred = (UserCredential)getCredentials(realm, proxy, BASIC, method);
        }
        catch (ClassCastException e)
        {
            throw new HttpException("UserCredential required for "
                + "Basic authentication.");
        }

        if (null == cred)
        {
            throw new HttpException("No credentials available for the Basic "
                + "authentication realm \'"
                + realm
                + "\'");
        }
        String authString = cred.getUser() + ":" + cred.getPassword();
        method.setCredentialSent(proxy, cred);
        return "Basic " + new String(Base64.encode(authString.getBytes()));
    }

    private static final String ntlm(String challenge,
                                     HttpURLConnectInternal method,
                                     String reqType,
                                     String respHeader) throws HttpException
    {
        boolean proxy = PROXY_AUTH_RESP.equalsIgnoreCase(respHeader);
        Log log = method.getLog();
        NtlmCredential cred = null;

        try
        {
            challenge = challenge.substring(challenge.toLowerCase()
                    .indexOf(NTLM)
                + NTLM.length()).trim();
        }
        catch (IndexOutOfBoundsException e)
        {
            throw new HttpException("Invalid NTLM challenge.");
        }

        // We previously sent an NTLM message and got back a 401/407,
        // this means we are not going to authenticate
        Credential sentCred = method.getCredentialSent(proxy);
        if (challenge.equals("") && sentCred != null)
        {
            throw new HttpException("NTLM Authentication failed; "
                + "with credential "
                + sentCred);
        }

        try
        {
            cred = (NtlmCredential)getCredentials(null, proxy, NTLM, method);
        }
        catch (ClassCastException e)
        {
            throw new HttpException("NtlmCredential required "
                + "for NTLM authentication.");
        }

        if (null == cred)
        {
            throw new HttpException("No credentials available for NTLM "
                + "authentication.");
        }
        try
        {
            String resp = "NTLM "
                + Ntlm.getResponseFor(challenge, cred.getUser(), cred
                        .getPassword(), cred.getHost(), cred.getDomain());
            if (log.isDebugEnabled())
            {
                log.debug("Replying to challenge with: " + resp);
            }
            method.setCredentialSent(proxy, cred);
            return resp;
        }
        catch (HttpException he)
        {
            log.warn("Exception processing NTLM message");
            return null;
        }
        catch (java.io.UnsupportedEncodingException e)
        {
            throw new HttpException("NTLM requires ASCII support.");
        }
    }

    private static final String digest(String realm,
                                       String challenge,
                                       HttpURLConnectInternal method,
                                       String respHeader) throws HttpException
    {
        boolean proxy = PROXY_AUTH_RESP.equalsIgnoreCase(respHeader);
        UserCredential cred = null;

        try
        {
            cred = (UserCredential)getCredentials(realm, proxy, DIGEST, method);
        }
        catch (ClassCastException e)
        {
            throw new HttpException("UserCredential required for "
                + "Digest authentication.");
        }

        if (null == cred)
        {
            throw new HttpException("No credentials available for the Digest "
                + "authentication realm \""
                + realm
                + "\"/");
        }
        Map headers = getHTTPDigestCredentials(challenge);
        headers.put("cnonce", "\"" + createCnonce() + "\"");
        headers.put("nc", "00000001");
        headers.put("uri", method.getPath());
        headers.put("methodname", method.getName());

        String digest = createDigest(cred.getUser(),
                                     cred.getPassword(),
                                     headers);

        return "Digest " + createDigestHeader(cred.getUser(), headers, digest);
    }

    private static final Map getHTTPDigestCredentials(String challenge)
    {
        // Get the authorization header value
        String authHeader = challenge.substring(7).trim();

        // map of digest tokens
        Map mapTokens = new Hashtable(17);

        // parse the authenticate header
        int i = 0;
        int j = authHeader.indexOf(",");

        while (j >= 0)
        {
            processDigestToken(authHeader.substring(i, j), mapTokens);
            i = j + 1;
            j = authHeader.indexOf(",", i);
        }

        if (i < authHeader.length())
        {
            processDigestToken(authHeader.substring(i), mapTokens);
        }

        return mapTokens;
    }

    static final Map parseAuthenticateHeader(Headers authHeaders,
                                             String authType,
                                             HttpURLConnectInternal method)
    {
        if (authHeaders == null)
            return new Hashtable(0);

        Log log = method.getLog();
        Map challengeMap = new Hashtable(7);

        String challenge = null;
        String scheme = null;

        // Look through all of the headers to find the ones
        // that match
        int len = authHeaders.length();
        for (int i = 0; i < len; i++)
        {
            String key = authHeaders.getKey(i);
            if (key == null)
                continue;
            if (key.equalsIgnoreCase(authType))
            {
                challenge = authHeaders.get(i);

                // find the blank and parse out the scheme
                int b = challenge.indexOf(' ');
                scheme = (b > 0) ? challenge.substring(0, b).trim() : challenge;

                // store the challenge keyed on the scheme
                challengeMap.put(scheme.toLowerCase(), challenge);
                if (log.isDebugEnabled())
                {
                    log.debug(scheme.toLowerCase() + "=>" + challenge);
                }
            }
        }

        return challengeMap;
    }

    private static final String createCnonce() throws HttpException
    {
        String cnonce;
        final String digAlg = "MD5";
        MessageDigest md5Helper;

        try
        {
            md5Helper = MessageDigest.getInstance(digAlg);
        }
        catch (Exception e)
        {
            throw new HttpException("Unsupported algorithm in HTTP Digest "
                + "authentication: "
                + digAlg);
        }

        cnonce = Long.toString(System.currentTimeMillis());
        cnonce = encode(md5Helper.digest(cnonce.getBytes()));

        return cnonce;
    }

    private static final String createDigestHeader(String uname,
                                                   Map mapCreds,
                                                   String digest)
    {
        StringBuffer sb = new StringBuffer();
        String uri = removeQuotes((String)mapCreds.get("uri"));
        String realm = removeQuotes((String)mapCreds.get("realm"));
        String nonce = removeQuotes((String)mapCreds.get("nonce"));
        String nc = removeQuotes((String)mapCreds.get("nc"));
        String cnonce = removeQuotes((String)mapCreds.get("cnonce"));
        String opaque = removeQuotes((String)mapCreds.get("opaque"));
        String response = digest;
        String qop = removeQuotes((String)mapCreds.get("qop"));

        if (qop != null)
        {
            qop = "auth"; // we only support auth
        }

        String algorithm = "MD5"; // we only support MD5

        sb.append("username=\"" + uname + "\"").append(", realm=\""
            + realm
            + "\"").append(", nonce=\"" + nonce + "\"").append(", uri=\""
            + uri
            + "\"").append(((qop == null) ? "" : ", qop=\"" + qop + "\""))
                .append(", algorithm=\"" + algorithm + "\"")
                .append(((qop == null) ? "" : ", nc=" + nc))
                .append(((qop == null) ? "" : ", cnonce=\"" + cnonce + "\""))
                .append(", response=\"" + response + "\"")
                .append((opaque == null) ? "" : ", opaque=\"" + opaque + "\"");

        return sb.toString();
    }

    /**
     * Encodes the 128 bit (16 bytes) MD5 digest into a 32 characters long
     * <CODE>String</CODE> according to RFC 2617.
     * 
     * @param binaryData
     *            array containing the digest
     * 
     * @return encoded MD5, or <CODE>null</CODE> if encoding failed
     * 
     * @todo + Add encode() method
     */
    private static final String encode(byte[] binaryData)
    {
        if (binaryData.length != 16)
        {
            return null;
        }

        char[] buffer = new char[32];

        for (int i = 0; i < 16; i++)
        {
            int low = (binaryData[i] & 0x0f);
            int high = ((binaryData[i] & 0xf0) >> 4);
            buffer[i * 2] = HEXADECIMAL[high];
            buffer[(i * 2) + 1] = HEXADECIMAL[low];
        }

        return new String(buffer);
    }

    private static final String parseRealmFromChallenge(String challenge,
                                                        HttpURLConnectInternal method)
        throws HttpException
    {
        Log log = method.getLog();

        // FIXME: Note that this won't work if there is more than one realm
        // within the challenge
        try
        {
            StringTokenizer strtok = new StringTokenizer(challenge, "=");
            strtok.nextToken().trim();
            String realm = strtok.nextToken().trim();
            int firstq = realm.indexOf('"');
            int lastq = realm.lastIndexOf('"');

            if ((firstq + 1) < lastq)
            {
                realm = realm.substring(firstq + 1, lastq);
            }

            if (log.isDebugEnabled())
            {
                log.debug("Parsed realm '"
                    + realm
                    + "' from challenge '"
                    + challenge
                    + "'");
            }

            return realm;
        }
        catch (Exception ex)
        {
            throw new HttpException("Failed to parse realm from challenge '"
                + challenge
                + "'");
        }
    }

    private static final void processDigestToken(String token, Map tokens)
    {
        int eqpos = token.indexOf("=");

        if ((eqpos > 0) && (eqpos < (token.length() - 1)))
        {
            tokens.put(token.substring(0, eqpos).trim(), token
                    .substring(eqpos + 1).trim());
        }
    }

    private static final String removeQuotes(String str)
    {
        if (str == null)
        {
            return null;
        }

        int fqpos = str.indexOf("\"") + 1;
        int lqpos = str.lastIndexOf("\"");

        if ((fqpos > 0) && (lqpos > fqpos))
        {
            return str.substring(fqpos, lqpos);
        }
        return str;
    }

    public static final int schemeToInt(String scheme)
    {
        if (scheme.equalsIgnoreCase(BASIC))
            return Credential.AUTH_BASIC;
        else if (scheme.equalsIgnoreCase(DIGEST))
            return Credential.AUTH_DIGEST;
        else if (scheme.equalsIgnoreCase(NTLM))
            return Credential.AUTH_NTLM;
        return -1;
    }

    private static Credential getCredentials(String realm,
                                             boolean proxy,
                                             String scheme,
                                             final HttpURLConnectInternal urlCon)
        throws HttpException
    {

        HttpUserAgent userAgent = urlCon.getUserAgent();

        // Pay attention the proxy user in the HttpURLConnection if there
        // is no user agent
        if (userAgent == null
            && proxy
            && urlCon.getConnectionProxyUser() != null)
        {
            userAgent = new HttpUserAgent()
            {

                public Credential getCredential(String realm1,
                                                String url,
                                                int scheme1)
                {
                    return null;
                }

                public Credential getProxyCredential(String realm1,
                                                     String url,
                                                     int scheme1)
                {
                    return UserCredential.createCredential(urlCon
                            .getConnectionProxyUser(), urlCon
                            .getConnectionProxyPassword());
                }
            };
        }

        if (userAgent == null)
        {
            throw new HttpException("No HttpUserAgent set - "
                + "can't get credential for "
                + scheme
                + ": "
                + realm);
        }

        int iScheme = schemeToInt(scheme);
        if (proxy)
        {
            return userAgent.getProxyCredential(realm, urlCon.getURL()
                    .toString(), iScheme);
        }

        return userAgent.getCredential(realm,
                                       urlCon.getURL().toString(),
                                       iScheme);

    }

}
