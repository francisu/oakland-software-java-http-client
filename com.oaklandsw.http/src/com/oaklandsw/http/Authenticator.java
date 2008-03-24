//
// Some portions copyright 2002-2007, oakland software, all rights reserved.
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

import java.io.InterruptedIOException;
import java.security.MessageDigest;
import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.bouncycastle.util.encoders.Base64;

import com.oaklandsw.http.ntlm.Ntlm;
import com.oaklandsw.util.LogUtils;

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
    private static final Log     _log            = LogUtils.makeLogger();

    public static final String   BASIC           = "basic";

    public static final String   DIGEST          = "digest";

    public static final String   NTLM            = "ntlm";

    public static final String[] RESP_HEADERS    = new String[] {
        "Authorization", "Proxy-Authorization"  };
    public static final byte[][] RESP_HEADERS_LC = new byte[][] {
        "Authorization".getBytes(), "Proxy-Authorization".getBytes() };

    public static final String[] REQ_HEADERS     = new String[] {
        "WWW-Authenticate", "Proxy-Authenticate" };
    public static final byte[][] REQ_HEADERS_LC  = new byte[][] {
        "www-authenticate".getBytes(), "proxy-authenticate".getBytes() };

    private static final char[]  HEXADECIMAL     = { '0', '1', '2', '3', '4',
        '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    // Returns false if authentication not attempted (because we don't need it)
    // throws if fails
    public static boolean authenticate(HttpURLConnectInternal urlCon,
                                       Headers reqAuthenticators,
                                       String reqType,
                                       Map challengeMap,
                                       int normalOrProxy)
        throws HttpException,
            InterruptedIOException
    {
        String respHeader = RESP_HEADERS[normalOrProxy];

        int authenticationType = urlCon.getAuthenticationType(normalOrProxy);
        boolean preemptive = authenticationType == Credential.AUTH_BASIC;

        // If there is no challenge, attempt to use preemptive authorization
        if (reqAuthenticators == null)
        {
            HttpUserAgent userAgent = urlCon.getUserAgent();

            if (preemptive && userAgent != null)
            {
                if (authenticationType != 0
                    && authenticationType != Credential.AUTH_BASIC)
                {
                    throw new IllegalStateException("Preemptive authentication is "
                        + "supported only for BASIC authentication.  "
                        + "See HttpURLConnection.setAuthenticationType()");
                }
                try
                {
                    _log.debug("Preemptively sending basic credentials");
                    Authenticator
                            .basic(null, urlCon, respHeader, normalOrProxy);
                    return true;
                }
                catch (HttpException httpe)
                {
                    if (_log.isDebugEnabled())
                    {
                        _log.debug("No default credentials to preemptively"
                            + " send");
                    }
                    return false;
                }
            }
            // no challenge and no default creds so do nothing
            return false;
        }

        if (urlCon.isPipelining())
        {
            if (authenticationType == 0)
            {
                String authType;
                if (normalOrProxy == HttpURLConnection.AUTH_PROXY)
                    authType = "proxyAuthenticationType";
                else
                    authType = "authenticationType";

                throw new IllegalStateException("Pipelining is only supported for "
                    + "connections requiring "
                    + HttpURLConnection.normalOrProxyToString(normalOrProxy)
                    + "authentication only if the "
                    + authType
                    + " "
                    + "is set or if preemptive authentication (BASIC) is used.  "
                    + "See HttpURLConnection.set"
                    + authType.substring(0, 1).toUpperCase()
                    + authType.substring(1)
                    + "()");
            }
        }

        // We previously sent an response message and got back a 401/407,
        // this means we are not going to authenticate
        if (urlCon._authState[normalOrProxy] == HttpURLConnectInternal.AS_FINAL_AUTH_SENT)
        {
            Credential sentCred = urlCon.getCredentialSent(normalOrProxy);
            throw new HttpException("Authentication failed; "
                + "with credential "
                + sentCred);
        }

        // determine the most secure request header to add
        if (challengeMap.containsKey(NTLM))
        {
            String challenge = (String)challengeMap.get(NTLM);
            Authenticator.ntlm(challenge,
                               urlCon,
                               respHeader,
                               reqType,
                               normalOrProxy);
        }
        else if (challengeMap.containsKey(DIGEST))
        {
            String challenge = (String)challengeMap.get(DIGEST);
            String realm = parseRealmFromChallenge(challenge, urlCon);
            Authenticator.digest(realm,
                                 challenge,
                                 urlCon,
                                 respHeader,
                                 normalOrProxy);
        }
        else if (challengeMap.containsKey(BASIC))
        {
            String challenge = (String)challengeMap.get(BASIC);
            String realm = parseRealmFromChallenge(challenge, urlCon);
            Authenticator.basic(realm, urlCon, respHeader, normalOrProxy);
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

        return true;
    }

    static boolean isNtlm(Map challengeMap)
    {
        if (challengeMap == null)
            return false;
        return challengeMap.containsKey(NTLM);
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

    private static void checkCredential(UserCredential cred)
    {
        if (null == cred || cred._user == null || cred._password == null)
        {
            throw new IllegalArgumentException("Either no credential available or the "
                + "credential supplied is missing a username or password for "
                + "authentication.");
        }
    }

    private static final void basic(String realm,
                                    HttpURLConnectInternal urlCon,
                                    String property,
                                    int normalOrProxy) throws HttpException
    {
        UserCredential cred = null;

        try
        {
            cred = (UserCredential)urlCon.getCredentialSent(normalOrProxy);
            ;
            if (cred == null)
            {
                cred = (UserCredential)getCredentials(realm,
                                                      normalOrProxy == HttpURLConnection.AUTH_PROXY,
                                                      BASIC,
                                                      urlCon);
            }
        }
        catch (ClassCastException e)
        {
            throw new IllegalArgumentException("UserCredential required for "
                + "Basic authentication.");
        }

        checkCredential(cred);

        String authString = cred.getUser() + ":" + cred.getPassword();
        urlCon.setCredentialSent(Credential.AUTH_BASIC,
                                 normalOrProxy,
                                 cred,
                                 HttpURLConnectInternal.AS_FINAL_AUTH_SENT);
        String requestHeader = "Basic "
            + new String(Base64.encode(authString.getBytes()));
        urlCon.addRequestProperty(property, requestHeader);
    }

    private static final void ntlm(String challenge,
                                   HttpURLConnectInternal urlCon,
                                   String property,
                                   String reqType,
                                   int normalOrProxy) throws HttpException
    {
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

        try
        {
            cred = (NtlmCredential)urlCon.getCredentialSent(normalOrProxy);
            if (cred == null)
            {
                cred = (NtlmCredential)getCredentials(null,
                                                      normalOrProxy == HttpURLConnection.AUTH_PROXY,
                                                      NTLM,
                                                      urlCon);
            }
        }
        catch (ClassCastException e)
        {
            throw new IllegalArgumentException("NtlmCredential required "
                + "for NTLM authentication.");
        }

        checkCredential(cred);

        int newAuthState;

        // If there is nothing in the challenge, then this is the start
        // even if we already sent the negotiate message
        if (challenge.equals(""))
            newAuthState = HttpURLConnectInternal.AS_INITIAL_AUTH_SENT;
        else
            newAuthState = HttpURLConnectInternal.AS_FINAL_AUTH_SENT;

        String header = "NTLM "
            + Ntlm.getResponseFor(challenge,
                                  cred.getUser(),
                                  cred.getPassword(),
                                  cred.getHost(),
                                  cred.getDomain());
        if (_log.isDebugEnabled())
        {
            _log.debug("Replying to challenge with: " + header);
        }

        urlCon.setCredentialSent(Credential.AUTH_NTLM,
                                 normalOrProxy,
                                 cred,
                                 newAuthState);

        urlCon.addRequestProperty(property, header);
    }

    private static final void digest(String realm,
                                     String challenge,
                                     HttpURLConnectInternal urlCon,
                                     String property,
                                     int normalOrProxy) throws HttpException
    {
        UserCredential cred = null;

        try
        {
            cred = (UserCredential)urlCon.getCredentialSent(normalOrProxy);
            ;
            if (cred == null)
            {
                cred = (UserCredential)getCredentials(realm,
                                                      normalOrProxy == HttpURLConnection.AUTH_PROXY,
                                                      DIGEST,
                                                      urlCon);
            }
        }
        catch (ClassCastException e)
        {
            throw new HttpException("UserCredential required for "
                + "Digest authentication.");
        }

        checkCredential(cred);

        Map headers = getHTTPDigestCredentials(challenge);
        headers.put("cnonce", "\"" + createCnonce() + "\"");
        headers.put("nc", "00000001");
        headers.put("uri", urlCon.getPath());
        headers.put("methodname", urlCon.getRequestMethod());

        String digest = createDigest(cred.getUser(),
                                     cred.getPassword(),
                                     headers);

        urlCon.setCredentialSent(Credential.AUTH_DIGEST,
                                 normalOrProxy,
                                 cred,
                                 HttpURLConnectInternal.AS_FINAL_AUTH_SENT);
        String header = "Digest "
            + createDigestHeader(cred.getUser(), headers, digest);
        urlCon.addRequestProperty(property, header);
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
                                             HttpURLConnectInternal urlCon)
    {
        if (authHeaders == null)
            return new Hashtable(0);

        Map challengeMap = new Hashtable(7);

        String challenge = null;
        String scheme = null;

        // Look through all of the headers to find the ones
        // that match
        int len = authHeaders.length();
        for (int i = 0; i < len; i++)
        {
            String key = authHeaders.getKeyAsString(i);
            if (key == null)
                continue;
            if (key.equalsIgnoreCase(authType))
            {
                challenge = authHeaders.getAsString(i);

                // find the blank and parse out the scheme
                int b = challenge.indexOf(' ');
                scheme = (b > 0) ? challenge.substring(0, b).trim() : challenge;

                // store the challenge keyed on the scheme
                challengeMap.put(scheme.toLowerCase(), challenge);
                if (_log.isDebugEnabled())
                {
                    _log.debug("authenticator: adding to ChallengeMap: "
                        + scheme.toLowerCase()
                        + "=>"
                        + challenge);
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
                                                        HttpURLConnectInternal urlCon)
        throws HttpException
    {
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

            if (_log.isDebugEnabled())
            {
                _log.debug("Parsed realm '"
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
        if (scheme == null)
            return -1;
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
        Credential cred = getCredentialInternal(realm, proxy, scheme, urlCon);
        if (cred == null)
        {
            throw new HttpException("No HttpUserAgent set or null Credential returned - "
                + "can't get credential for "
                + scheme
                + ": "
                + realm
                + " userAgent: "
                + urlCon.getUserAgent());
        }
        return cred;
    }

    private static Credential getCredentialInternal(String realm,
                                                    boolean proxy,
                                                    String scheme,
                                                    final HttpURLConnectInternal urlCon)
    {

        HttpUserAgent userAgent = urlCon.getUserAgent();

        // Pay attention the proxy user in the HttpURLConnection if there
        // is no user agent
        if (userAgent == null
            && proxy
            && urlCon.getConnectionProxyUser() != null)
        {
            _log.debug("getCredentialInternal - "
                + "fabricating user agent for proxy credential");
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
            _log.debug("getCredentialInternal - no user agent");
            return null;
        }

        int iScheme = schemeToInt(scheme);
        Credential cred;

        if (_log.isDebugEnabled())
        {
            _log.debug("getCredentialInternal: calling "
                + userAgent
                + (proxy ? " PROXY " : "")
                + " realm: "
                + realm
                + " url: "
                + urlCon.getURL().toString()
                + " scheme: "
                + scheme
                + " ("
                + iScheme
                + ")");
        }

        if (proxy)
        {
            cred = userAgent.getProxyCredential(realm, urlCon.getURL()
                    .toString(), iScheme);
        }
        else
        {

            cred = userAgent.getCredential(realm,
                                           urlCon.getURL().toString(),
                                           iScheme);
        }

        if (_log.isDebugEnabled())
        {
            _log.debug("getCredentialInternal: result " + cred);
        }

        return cred;

    }
}
