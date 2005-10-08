//
// Some portions copyright 2002-3003, oakland software, all rights reserved.
//
// May not be used or redistributed without specific written
// permission from oakland software.
//
/*
 * The Apache Software License, Version 1.1
 * 
 * Copyright (c) 2002 the Apache Software Foundation. All rights reserved.
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

import java.io.UnsupportedEncodingException;
import java.util.BitSet;

import com.oaklandsw.util.StringUtils;

/**
 * The URI escape and character encoding and decoding utility. It's compatible
 * with {@link com.oaklandsw.http.HttpURL}rather than {@link com.oaklandsw.http.URI}.
 * 
 * @author <a href="mailto:jericho@apache.org">Sung-Gu </a>
 * @version $Revision: 1.5 $ $Date: 2002/03/14 15:14:01
 */

public class URIUtil
{

    protected static final BitSet empty = new BitSet(1);

    // ---------------------------------------------------------- URI utilities

    /**
     * Get the protocol of the URI.
     * 
     * @param uri
     *            a string regarded an URI
     * @return the host:port portion.
     */
    public static String getProtocol(String uri)
    {
        if (uri == null || uri.length() == 0)
            return uri;
        int end = uri.indexOf(":");
        // If not present, assume there is no protocol, and the host
        // is the first thing
        if (end == -1)
            return null;

        return uri.substring(0, end);
    }

    /**
     * Get the protocol/host/port of the URI.
     * 
     * @param uri
     *            a string regarded an URI
     * @return the protocol://host:port portion.
     */
    public static String getProtocolHostPort(String uri)
    {
        if (uri == null || uri.length() == 0)
            return uri;
        int start = uri.indexOf("://");
        // If not present, assume there is no protocol, and the host
        // is the first thing
        if (start == -1)
            return null;
        start += 3;
        return getHostPortCommon(uri, start, 0);
    }

    /**
     * Get the host/port of the URI.
     * 
     * @param uri
     *            a string regarded an URI
     * @return the host:port portion.
     */
    public static String getHostPort(String uri)
    {
        if (uri == null || uri.length() == 0)
            return uri;
        int start = uri.indexOf("://");
        // If not present, assume there is no protocol, and the host
        // is the first thing
        if (start == -1)
        {
            if (uri.charAt(0) == '/')
                return null;
            start = 0;
        }
        else
        {
            start += 3;
        }
        return getHostPortCommon(uri, start, start);
    }
    
    private static String getHostPortCommon(String uri, int start, int retStart)
    {
        int end = StringUtils.indexOfAny(uri.substring(start), new char[] {'?', '#', '/'});
        // Add start to end to compensate for starting from start (above)
        if (end != -1)
            return uri.substring(retStart, end + start);
        return uri.substring(retStart);
    }

    /**
     * Get the basename of an URI. It's possibly an empty string.
     * 
     * @param uri
     *            a string regarded an URI
     * @return the basename string; an empty string if the path ends with slash
     */
    public static String getName(String uri)
    {
        if (uri == null || uri.length() == 0)
            return uri;
        String path = URIUtil.getPath(uri);
        int at = path.lastIndexOf('/');
        int to = path.length();
        return (at >= 0) ? path.substring(at + 1, to) : path;
    }

    /**
     * Get the query of an URI.
     * 
     * @param uri
     *            a string regarded an URI
     * @return the query string; <code>null</code> if empty or undefined
     */
    public static String getQuery(String uri)
    {
        if (uri == null || uri.length() == 0)
            return null;
        // consider of net_path
        int at = uri.indexOf("//");
        int from = uri.indexOf('/', at >= 0
            ? (uri.lastIndexOf('/', at - 1) >= 0 ? 0 : at + 2)
            : 0);
        // the authority part of URI ignored
        int to = uri.length();
        // reuse the at and from variables to consider the query
        at = uri.indexOf('?', from);
        if (at >= 0)
        {
            from = at + 1;
        }
        else
            return null;
        // check the fragment
        if (uri.indexOf('#') > from)
            to = uri.indexOf('#');
        // get the path and query.
        return (from < 0 || from == to) ? null : uri.substring(from, to);
    }

    /**
     * Get the path of an URI.
     * 
     * @param uri
     *            a string regarded an URI
     * @return the path string
     */
    public static String getPath(String uri)
    {
        if (uri == null)
            return null;
        // consider of net_path
        int at = uri.indexOf("//");
        int from = uri.indexOf('/', at >= 0
            ? (uri.lastIndexOf('/', at - 1) >= 0 ? 0 : at + 2)
            : 0);
        // the authority part of URI ignored
        int to = uri.length();
        // check the query
        if (uri.indexOf('?') > from)
            to = uri.indexOf('?');
        // check the fragment
        if (uri.indexOf('#') > from && uri.indexOf('#') < to)
            to = uri.indexOf('#');
        // get only the path.
        return (from < 0) ? (at >= 0 ? "/" : uri) : uri.substring(from, to);
    }

    /**
     * Get the path and query of an URI.
     * 
     * @param uri
     *            a string regarded an URI
     * @return the path and query string
     */
    public static String getPathQuery(String uri)
    {
        if (uri == null)
            return null;
        // consider of net_path
        int at = uri.indexOf("//");
        int from = uri.indexOf('/', at >= 0
            ? (uri.lastIndexOf('/', at - 1) >= 0 ? 0 : at + 2)
            : 0);
        // the authority part of URI ignored
        int to = uri.length();
        // Ignore the '?' mark so to ignore the query.
        // check the fragment
        if (uri.indexOf('#') > from)
            to = uri.indexOf('#');
        // get the path and query.
        return (from < 0) ? (at >= 0 ? "/" : uri) : uri.substring(from, to);
    }

    /**
     * Get the path of an URI and its rest part.
     * 
     * @param uri
     *            a string regarded an URI
     * @return the string from the path part
     */
    public static String getFromPath(String uri)
    {
        if (uri == null)
            return null;
        // consider of net_path
        int at = uri.indexOf("//");
        int from = uri.indexOf('/', at >= 0
            ? (uri.lastIndexOf('/', at - 1) >= 0 ? 0 : at + 2)
            : 0);
        // get the path and its rest.
        return (from < 0) ? (at >= 0 ? "/" : uri) : uri.substring(from);
    }

    // ----------------------------------------------------- Encoding utilities

    /**
     * Get the all escaped and encoded string with the default protocl charset.
     * 
     * @param unescaped
     *            an unescaped string
     * @param charset
     *            the charset
     * @return the escaped string
     * @exception URIException
     * @see URI#getProtocolCharset
     * @see #encode
     */
    public static String encodeAll(String unescaped) throws URIException
    {
        return encodeAll(unescaped, URI.getProtocolCharset());
    }

    /**
     * Get the all escaped and encoded string with a given charset.
     * 
     * @param unescaped
     *            an unescaped string
     * @param charset
     *            the charset
     * @return the escaped string
     * @exception URIException
     * @see #encode
     */
    public static String encodeAll(String unescaped, String charset)
        throws URIException
    {

        return encode(unescaped, empty, charset);
    }

    /**
     * Escape and encode a string regarded as the path and query components of
     * an URI with the default protocol charset.
     * 
     * @param unescaped
     *            an unescaped string
     * @param charset
     *            the charset
     * @return the escaped string
     * @exception URIException
     * @see URI#getProtocolCharset
     * @see #encode
     */
    public static String encodePathQuery(String unescaped) throws URIException
    {
        return encodePathQuery(unescaped, URI.getProtocolCharset());
    }

    /**
     * Escape and encode a string regarded as the path and query components of
     * an URI with a given charset.
     * 
     * @param unescaped
     *            an unescaped string
     * @param charset
     *            the charset
     * @return the escaped string
     * @exception URIException
     * @see #encode
     */
    public static String encodePathQuery(String unescaped, String charset)
        throws URIException
    {

        int at = unescaped.indexOf('?');
        if (at < 0)
        {
            return encode(unescaped, URI.allowed_abs_path, charset);
        }
        // else
        return encode(unescaped.substring(0, at), URI.allowed_abs_path, charset)
            + '?'
            + encode(unescaped.substring(at + 1), URI.allowed_query, charset);
    }

    /**
     * Escape and encode a string regarded as the path component of an URI with
     * the default protocol charset.
     * 
     * @param unescaped
     *            an unescaped string
     * @param charset
     *            the charset
     * @return the escaped string
     * @exception URIException
     * @see URI#getProtocolCharset
     * @see #encode
     */
    public static String encodePath(String unescaped) throws URIException
    {
        return encodePath(unescaped, URI.getProtocolCharset());
    }

    /**
     * Escape and encode a string regarded as the path component of an URI with
     * a given charset.
     * 
     * @param unescaped
     *            an unescaped string
     * @param charset
     *            the charset
     * @return the escaped string
     * @exception URIException
     * @see #encode
     */
    public static String encodePath(String unescaped, String charset)
        throws URIException
    {

        return encode(unescaped, URI.allowed_abs_path, charset);
    }

    /**
     * Escape and encode a string regarded as the query component of an URI with
     * the default protocol charset.
     * 
     * @param unescaped
     *            an unescaped string
     * @return the escaped string
     * @exception URIException
     * @see URI#getProtocolCharset
     * @see #encode
     */
    public static String encodeQuery(String unescaped) throws URIException
    {
        return encodeQuery(unescaped, URI.getProtocolCharset());
    }

    /**
     * Escape and encode a string regarded as the query component of an URI with
     * a given charset.
     * 
     * @param unescaped
     *            an unescaped string
     * @param charset
     *            the charset
     * @return the escaped string
     * @exception URIException
     * @see #encode
     */
    public static String encodeQuery(String unescaped, String charset)
        throws URIException
    {

        return encode(unescaped, URI.allowed_query, charset);
    }

    /**
     * Escape and encode a given string and the default protocol charset.
     * 
     * @param unescaped
     *            a string
     * @return the escaped string
     * @exception URIException
     * @see URI#getProtocolCharset
     * @see Coder#encode
     */
    public static String encode(String unescaped) throws URIException
    {

        return encode(unescaped, empty, URI.getProtocolCharset());
    }

    /**
     * Escape and encode a given string with allowed characters not to be
     * escaped and the default protocol charset.
     * 
     * @param unescaped
     *            a string
     * @param allowed
     *            allowed characters not to be escaped
     * @return the escaped string
     * @exception URIException
     * @see URI#getProtocolCharset
     * @see Coder#encode
     */
    public static String encode(String unescaped, BitSet allowed)
        throws URIException
    {

        return encode(unescaped, allowed, URI.getProtocolCharset());
    }

    /**
     * Escape and encode a given string with allowed characters not to be
     * escaped and a given charset.
     * 
     * @param unescaped
     *            a string
     * @param allowed
     *            allowed characters not to be escaped
     * @param charset
     *            the charset
     * @return the escaped string
     * @exception URIException
     * @see Coder#encode
     */
    public static String encode(String unescaped, BitSet allowed, String charset)
        throws URIException
    {

        return new String(Coder.encode(unescaped, allowed, charset));
    }

    /**
     * Unescape and decode a given string regarded as an escaped string with the
     * default protocol charset.
     * 
     * @param escaped
     *            a string
     * @return the unescaped string
     * @exception URIException
     * @see URI#getProtocolCharset
     * @see Coder#decode
     */
    public static String decode(String escaped) throws URIException
    {
        return Coder.decode(escaped.toCharArray(), URI.getProtocolCharset());
    }

    /**
     * Unescape and decode a given string regarded as an escaped string.
     * 
     * @param escaped
     *            a string
     * @param charset
     *            the charset
     * @return the unescaped string
     * @exception URIException
     * @see Coder#decode
     */
    public static String decode(String escaped, String charset)
        throws URIException
    {

        return Coder.decode(escaped.toCharArray(), charset);
    }

    /**
     * Convert a target string to the specified character encoded string with
     * the default document charset.
     * 
     * @param target
     *            a target string
     * @return the document character encoded string
     * @exception URIException
     * @see URI#getDocumentCharset
     */
    public static String toDocumentCharset(String target) throws URIException
    {
        return toDocumentCharset(target, URI.getDocumentCharset());
    }

    /**
     * Convert a target string to the specified character encoded string with a
     * given document charset.
     * 
     * @param target
     *            a target string
     * @param charset
     *            the charset
     * @return the document character encoded string
     * @exception URIException
     */
    public static String toDocumentCharset(String target, String charset)
        throws URIException
    {

        try
        {
            return new String(target.getBytes(), charset);
        }
        catch (UnsupportedEncodingException error)
        {
            throw new URIException(URIException.UNSUPPORTED_ENCODING, error
                    .getMessage());
        }
    }

    // ---------------------------------------------------------- Inner classes

    /**
     * The basic and internal utility for URI escape and character encoding and
     * decoding.
     */
    protected static class Coder extends URI
    {

        /**
         * Escape and encode a given string with allowed characters not to be
         * escaped.
         * 
         * @param unescapedComponent
         *            an unescaped component
         * @param allowed
         *            allowed characters not to be escaped
         * @param charset
         *            the charset to encode
         * @exception URIException
         * @return the escaped and encoded string
         */
        public static char[] encode(String unescapedComponent,
                                    BitSet allowed,
                                    String charset) throws URIException
        {

            return URI.encode(unescapedComponent, allowed, charset);
        }

        /**
         * Unescape and decode a given string.
         * 
         * @param unescapedComponent
         *            an unescaped component
         * @param allowed
         *            allowed characters not to be escaped
         * @param charset
         *            the charset to decode
         * @exception URIException
         * @return the escaped and encoded string
         */
        public static String decode(char[] escapedComponent, String charset)
            throws URIException
        {

            return URI.decode(escapedComponent, charset);
        }

        /**
         * Verify whether a given string is escaped or not
         * 
         * @param original
         *            given characters
         * @return true if the given character array is 7 bit ASCII-compatible.
         */
        public static boolean verifyEscaped(char[] original)
        {
            for (int i = 0; i < original.length; i++)
            {
                int c = original[i];
                if (c > 128)
                {
                    return false;
                }
                else if (c == '%')
                {
                    if (Character.digit(original[++i], 16) == -1
                        || Character.digit(original[++i], 16) == -1)
                        return false;
                }
            }
            return true;
        }

        /**
         * Replace from a given character to given character in an array order
         * for a given string.
         * 
         * @param original
         *            a given string
         * @param from
         *            a replacing character array
         * @param to
         *            a replaced character array
         * @return the replaced string
         */
        public static String replace(String original, char[] from, char[] to)
        {
            for (int i = from.length; i > 0; --i)
            {
                original = replace(original, from[i], to[i]);
            }
            return original.toString();
        }

        /**
         * Replace from a given character to given character for a given string.
         * 
         * @param original
         *            a given string
         * @param from
         *            a replacing character array
         * @param to
         *            a replaced character array
         * @return the replaced string
         */
        public static String replace(String original, char from, char to)
        {
            StringBuffer result = new StringBuffer(original.length());
            int at, saved = 0;
            do
            {
                at = original.indexOf(from);
                if (at >= 0)
                {
                    result.append(original.substring(0, at));
                    result.append(to);
                }
                else
                {
                    result.append(original.substring(saved));
                }
                saved = at;
            }
            while (at >= 0);
            return result.toString();
        }
    }

}
