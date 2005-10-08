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

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.AccessController;
import java.util.BitSet;
import java.util.Hashtable;
import java.util.Locale;

import sun.security.action.GetPropertyAction;

/**
 * The interface for the URI(Uniform Resource Identifiers) version of RFC 2396.
 * This class has the purpose of supportting of parsing a URI reference to
 * extend any specific protocols, the character encoding of the protocol to be
 * transported and the charset of the document.
 * <p>
 * A URI is always in an "escaped" form, since escaping or unescaping a
 * completed URI might change its semantics.
 * <p>
 * Implementers should be careful not to escape or unescape the same string more
 * than once, since unescaping an already unescaped string might lead to
 * misinterpreting a percent data character as another escaped character, or
 * vice versa in the case of escaping an already escaped string.
 * <p>
 * In order to avoid these problems, data types used as follows:
 * <p>
 * <blockquote>
 * 
 * <pre>
 * 
 *  
 *     URI character sequence: char
 *     octet sequence: byte
 *     original character sequence: String
 *   
 *  
 * </pre>
 * 
 * </blockquote>
 * <p>
 * 
 * So, a URI is a sequence of characters as an array of a char type, which is
 * not always represented as a sequence of octets as an array of byte.
 * <p>
 * 
 * URI Syntactic Components
 * <p>
 * <blockquote>
 * 
 * <pre>
 * 
 *  
 *   - In general, written as follows:
 *     Absolute URI = &lt;scheme&amp;gt:&lt;scheme-specific-part&gt;
 *     Generic URI = &lt;scheme&gt;://&lt;authority&gt;&lt;path&gt;?&lt;query&gt;
 *  
 *   - Syntax
 *     absoluteURI   = scheme &quot;:&quot; ( hier_part | opaque_part )
 *     hier_part     = ( net_path | abs_path ) [ &quot;?&quot; query ]
 *     net_path      = &quot;//&quot; authority [ abs_path ]
 *     abs_path      = &quot;/&quot;  path_segments
 *   
 *  
 * </pre>
 * 
 * </blockquote>
 * <p>
 * 
 * The following examples illustrate URI that are in common use.
 * 
 * <pre>
 * 
 *  
 *   ftp://ftp.is.co.za/rfc/rfc1808.txt
 *      -- ftp scheme for File Transfer Protocol services
 *   gopher://spinaltap.micro.umn.edu/00/Weather/California/Los%20Angeles
 *      -- gopher scheme for Gopher and Gopher+ Protocol services
 *   http://www.math.uio.no/faq/compression-faq/part1.html
 *      -- http scheme for Hypertext Transfer Protocol services
 *   mailto:mduerst@ifi.unizh.ch
 *      -- mailto scheme for electronic mail addresses
 *   news:comp.infosystems.www.servers.unix
 *      -- news scheme for USENET news groups and articles
 *   telnet://melvyl.ucop.edu/
 *      -- telnet scheme for interactive services via the TELNET Protocol
 *   
 *  
 * </pre>
 * 
 * Please, notice that there are many modifications from URL(RFC 1738) and
 * relative URL(RFC 1808).
 * <p>
 * <b>The expressions for a URI </b>
 * <p>
 * 
 * <pre>
 * 
 *  
 *   For escaped URI forms
 *    - URI(char[]) // constructor
 *    - char[] getRawXxx() // method
 *    - String getEscapedXxx() // method
 *    - String toString() // method
 *   
 *  
 * <p>
 * 
 *  
 *   For unescaped URI forms
 *    - URI(String) // constructor
 *    - String getXXX() // method
 *   
 *  
 * </pre>
 * 
 * <p>
 * 
 * @author <a href="mailto:jericho@apache.org">Sung-Gu </a>
 * @version $Revision: 1.5 $ $Date: 2002/03/14 15:14:01
 */
public class URI implements Cloneable, Comparable, Serializable
{

    // ----------------------------------------------------------- Constructors

    protected URI()
    {
    }

    /**
     * Construct a URI as an escaped form of a character array.
     * 
     * @param e
     *            the URI character sequence
     * @exception URIException
     * @throws NullPointerException
     *             if <code>escaped</code> is <code>null</code>
     */
    public URI(char[] e) throws URIException
    {
        parseUriReference(new String(e), true);
    }

    /**
     * Construct a URI from the given string.
     * <p>
     * <blockquote>
     * 
     * <pre>
     * 
     *  
     *     URI-reference = [ absoluteURI | relativeURI ] [ &quot;#&quot; fragment ]
     *   
     *  
     * </pre>
     * 
     * </blockquote>
     * <p>
     * 
     * @param original
     *            the string to be represented to URI character sequence It is
     *            one of absoluteURI and relativeURI.
     * @exception URIException
     */
    public URI(String original) throws URIException
    {
        parseUriReference(original, false);
    }

    /**
     * Construct a general URI from the given components.
     * <p>
     * <blockquote>
     * 
     * <pre>
     * 
     *  
     *     URI-reference = [ absoluteURI | relativeURI ] [ &quot;#&quot; fragment ]
     *     absoluteURI   = scheme &quot;:&quot; ( hier_part | opaque_part )
     *     opaque_part   = uric_no_slash *uric
     *   
     *  
     * </pre>
     * 
     * </blockquote>
     * <p>
     * It's for absolute URI = <scheme>: <scheme-specific-part># <fragment>
     * 
     * @param sch
     *            the scheme string
     * @param scheme_specific_part
     *            scheme_specific_part
     * @param frag
     *            the fragment string
     * @exception URIException
     */
    public URI(String sch, String scheme_specific_part, String frag)
        throws URIException
    {

        // validate and contruct the URI character sequence
        if (sch == null)
        {
            throw new URIException(URIException.PARSING, "scheme required");
        }
        char[] s = sch.toLowerCase().toCharArray();
        if (validate(s, URI.scheme))
        {
            _scheme = s; // is_absoluteURI
        }
        else
        {
            throw new URIException(URIException.PARSING, "incorrect scheme");
        }
        _opaque = encode(scheme_specific_part, allowed_opaque_part);
        // Set flag
        _is_opaque_part = true;
        setUriReference();
    }

    /**
     * Construct a general URI from the given components.
     * <p>
     * <blockquote>
     * 
     * <pre>
     * 
     *  
     *     URI-reference = [ absoluteURI | relativeURI ] [ &quot;#&quot; fragment ]
     *     absoluteURI   = scheme &quot;:&quot; ( hier_part | opaque_part )
     *     relativeURI   = ( net_path | abs_path | rel_path ) [ &quot;?&quot; query ]
     *     hier_part     = ( net_path | abs_path ) [ &quot;?&quot; query ]
     *   
     *  
     * </pre>
     * 
     * </blockquote>
     * <p>
     * It's for absolute URI = <scheme>: <path>? <query># <fragment>and relative
     * URI = <path>? <query># <fragment>
     * 
     * @param sch
     *            the scheme string
     * @param auth
     *            the authority string
     * @param p
     *            the path string
     * @param q
     *            the query string
     * @param f
     *            the fragment string
     * @exception URIException
     */
    public URI(String sch, String auth, String p, String q, String f)
        throws URIException
    {

        // validate and contruct the URI character sequence
        StringBuffer buff = new StringBuffer();
        if (sch != null)
        {
            buff.append(sch);
            buff.append(':');
        }
        if (auth != null)
        {
            buff.append("//");
            buff.append(auth);
        }
        if (p != null)
        { // accept empty path
            if ((sch != null || auth != null) && !p.startsWith("/"))
            {
                throw new URIException(URIException.PARSING,
                    "abs_path requested");
            }
            buff.append(p);
        }
        if (q != null)
        {
            buff.append('?');
            buff.append(q);
        }
        if (f != null)
        {
            buff.append('#');
            buff.append(f);
        }
        parseUriReference(buff.toString(), false);
    }

    /**
     * Construct a general URI from the given components.
     * 
     * @param sch
     *            the scheme string
     * @param ui
     *            the userinfo string
     * @param h
     *            the host string
     * @param port
     *            the port number
     * @exception URIException
     */
    public URI(String sch, String ui, String h, int p) throws URIException
    {

        this(sch, ui, h, p, null, null, null);
    }

    /**
     * Construct a general URI from the given components.
     * 
     * @param sch
     *            the scheme string
     * @param ui
     *            the userinfo string
     * @param h
     *            the host string
     * @param p
     *            the port number
     * @param pth
     *            the path string
     * @exception URIException
     */
    public URI(String sch, String ui, String h, int p, String pth)
        throws URIException
    {

        this(sch, ui, h, p, pth, null, null);
    }

    /**
     * Construct a general URI from the given components.
     * 
     * @param sch
     *            the scheme string
     * @param ui
     *            the userinfo string
     * @param h
     *            the host string
     * @param prt
     *            the port number
     * @param pth
     *            the path string
     * @param q
     *            the query string
     * @exception URIException
     */
    public URI(String sch, String ui, String h, int prt, String pth, String q)
        throws URIException
    {

        this(sch, ui, h, prt, pth, q, null);
    }

    /**
     * Construct a general URI from the given components.
     * 
     * @param sch
     *            the scheme string
     * @param ui
     *            the userinfo string
     * @param h
     *            the host string
     * @param p
     *            the port number
     * @param pth
     *            the path string
     * @param q
     *            the query string
     * @param f
     *            the fragment string
     * @exception URIException
     */
    public URI(String sch,
            String ui,
            String h,
            int p,
            String pth,
            String q,
            String f) throws URIException
    {

        this(sch, (h == null) ? null : ((ui != null) ? ui + '@' : "")
            + h
            + ((p != -1) ? ":" + p : ""), pth, q, f);
    }

    /**
     * Construct a general URI from the given components.
     * 
     * @param sch
     *            the scheme string
     * @param h
     *            the host string
     * @param p
     *            the path string
     * @param f
     *            the fragment string
     * @exception URIException
     */
    public URI(String sch, String h, String p, String f) throws URIException
    {

        this(sch, h, p, null, f);
    }

    /**
     * Construct a general URI with the given relative URI string.
     * 
     * @param base
     *            the base URI
     * @param relative
     *            the relative URI string
     * @exception URIException
     */
    public URI(URI base, String relative) throws URIException
    {
        this(base, new URI(relative));
    }

    /**
     * Construct a general URI with the given relative URI.
     * <p>
     * <blockquote>
     * 
     * <pre>
     * 
     *  
     *     URI-reference = [ absoluteURI | relativeURI ] [ &quot;#&quot; fragment ]
     *     relativeURI   = ( net_path | abs_path | rel_path ) [ &quot;?&quot; query ]
     *   
     *  
     * </pre>
     * 
     * </blockquote>
     * <p>
     * Resolving Relative References to Absolute Form.
     * 
     * <strong>Examples of Resolving Relative URI References </strong>
     * 
     * Within an object with a well-defined base URI of
     * <p>
     * <blockquote>
     * 
     * <pre>
     * 
     *  
     *     http://a/b/c/d;p?q
     *   
     *  
     * </pre>
     * 
     * </blockquote>
     * <p>
     * the relative URI would be resolved as follows:
     * 
     * Normal Examples
     * 
     * <p>
     * <blockquote>
     * 
     * <pre>
     * 
     *  
     *     g:h           =  g:h
     *     g             =  http://a/b/c/g
     *     ./g           =  http://a/b/c/g
     *     g/            =  http://a/b/c/g/
     *     /g            =  http://a/g
     *     //g           =  http://g
     *     ?y            =  http://a/b/c/?y
     *     g?y           =  http://a/b/c/g?y
     *     #s            =  (current document)#s
     *     g#s           =  http://a/b/c/g#s
     *     g?y#s         =  http://a/b/c/g?y#s
     *     ;x            =  http://a/b/c/;x
     *     g;x           =  http://a/b/c/g;x
     *     g;x?y#s       =  http://a/b/c/g;x?y#s
     *     .             =  http://a/b/c/
     *     ./            =  http://a/b/c/
     *     ..            =  http://a/b/
     *     ../           =  http://a/b/
     *     ../g          =  http://a/b/g
     *     ../..         =  http://a/
     *     ../../        =  http://a/ 
     *     ../../g       =  http://a/g
     *   
     *  
     * </pre>
     * 
     * </blockquote>
     * <p>
     * 
     * Some URI schemes do not allow a hierarchical syntax matching the
     * <hier_part>syntax, and thus cannot use relative references.
     * 
     * @param base
     *            the base URI
     * @param relative
     *            the relative URI
     * @exception URIException
     */
    public URI(URI base, URI relative) throws URIException
    {

        if (base._scheme == null)
        {
            throw new URIException(URIException.PARSING, "base URI required");
        }
        if (base._scheme != null)
        {
            this._scheme = base._scheme;
            this._authority = base._authority;
        }
        if (base._is_opaque_part || relative._is_opaque_part)
        {
            this._scheme = base._scheme;
            this._is_opaque_part = relative._is_opaque_part;
            this._opaque = relative._opaque;
            this._fragment = relative._fragment;
            this.setUriReference();
            return;
        }
        if (relative._scheme != null)
        {
            this._scheme = relative._scheme;
            this._is_net_path = relative._is_net_path;
            this._authority = relative._authority;
            if (relative._is_server)
            {
                this._userinfo = relative._userinfo;
                this._host = relative._host;
                this._port = relative._port;
            }
            else if (relative._is_reg_name)
            {
                this._is_reg_name = relative._is_reg_name;
            }
            this._is_abs_path = relative._is_abs_path;
            this._is_rel_path = relative._is_rel_path;
            this._path = relative._path;
        }
        else if (base._authority != null && relative._scheme == null)
        {
            this._is_net_path = base._is_net_path;
            this._authority = base._authority;
            if (base._is_server)
            {
                this._userinfo = base._userinfo;
                this._host = base._host;
                this._port = base._port;
            }
            else if (base._is_reg_name)
            {
                this._is_reg_name = base._is_reg_name;
            }
        }
        if (relative._authority != null)
        {
            this._is_net_path = relative._is_net_path;
            this._authority = relative._authority;
            if (relative._is_server)
            {
                this._is_server = relative._is_server;
                this._userinfo = relative._userinfo;
                this._host = relative._host;
                this._port = relative._port;
            }
            else if (relative._is_reg_name)
            {
                this._is_reg_name = relative._is_reg_name;
            }
            this._is_abs_path = relative._is_abs_path;
            this._is_rel_path = relative._is_rel_path;
            this._path = relative._path;
        }
        // resolve the path
        if (relative._scheme == null
            && relative._authority == null
            || equals(base._scheme, relative._scheme))
        {
            this._path = resolvePath(base._path, relative._path);
        }
        // base._query removed
        if (relative._query != null)
        {
            this._query = relative._query;
        }
        // base._fragment removed
        if (relative._fragment != null)
        {
            this._fragment = relative._fragment;
        }
        this.setUriReference();
    }

    // --------------------------------------------------- Instance Variables

    static final long             serialVersionUID       = 604752400577948726L;

    /**
     * This Uniform Resource Identifier (URI). The URI is always in an "escaped"
     * form, since escaping or unescaping a completed URI might change its
     * semantics.
     */
    protected char[]              _uri                   = null;

    /**
     * The default charset of the protocol. RFC 2277, 2396
     */
    protected static String       _protocolCharset       = "UTF-8";

    /**
     * The default charset of the document. RFC 2277, 2396 The platform's
     * charset is used for the document by default.
     */
    protected static String       _documentCharset       = null;
    // Static initializer for _documentCharset
    static
    {
        Locale locale = Locale.getDefault();
        if (locale != null)
        {
            // in order to support backward compatiblity
            _documentCharset = LocaleToCharsetMap.getCharset(locale);
        }
        else
        {
            _documentCharset = (String)AccessController
                    .doPrivileged(new GetPropertyAction("file.encoding"));
        }
    }

    /**
     * The scheme.
     */
    protected char[]              _scheme                = null;

    /**
     * The opaque.
     */
    protected char[]              _opaque                = null;

    /**
     * The authority.
     */
    protected char[]              _authority             = null;

    /**
     * The userinfo.
     */
    protected char[]              _userinfo              = null;

    /**
     * The host.
     */
    protected char[]              _host                  = null;

    /**
     * The port.
     */
    protected int                 _port                  = -1;

    /**
     * The path.
     */
    protected char[]              _path                  = null;

    /**
     * The query.
     */
    protected char[]              _query                 = null;

    /**
     * The fragment.
     */
    protected char[]              _fragment              = null;

    /**
     * The root path.
     */
    protected static char[]       rootPath               = { '/' };

    /**
     * The debug.
     */
    protected static int          debug                  = 0;

    // ---------------------- Generous characters for each component validation

    /**
     * The percent "%" character always has the reserved purpose of being the
     * escape indicator, it must be escaped as "%25" in order to be used as data
     * within a URI.
     */
    protected static final BitSet percent                = new BitSet(256);
    // Static initializer for percent
    static
    {
        percent.set('%');
    }

    /**
     * BitSet for digit.
     * <p>
     * <blockquote>
     * 
     * <pre>
     * digit = &quot;0&quot; | &quot;1&quot; | &quot;2&quot; | &quot;3&quot; | &quot;4&quot; | &quot;5&quot; | &quot;6&quot; | &quot;7&quot; | &quot;8&quot; | &quot;9&quot;
     * </pre>
     * 
     * </blockquote>
     * <p>
     */
    protected static final BitSet digit                  = new BitSet(256);
    // Static initializer for digit
    static
    {
        for (int i = '0'; i <= '9'; i++)
        {
            digit.set(i);
        }
    }

    /**
     * BitSet for alpha.
     * <p>
     * <blockquote>
     * 
     * <pre>
     * alpha = lowalpha | upalpha
     * </pre>
     * 
     * </blockquote>
     * <p>
     */
    protected static final BitSet alpha                  = new BitSet(256);
    // Static initializer for alpha
    static
    {
        for (int i = 'a'; i <= 'z'; i++)
        {
            alpha.set(i);
        }
        for (int i = 'A'; i <= 'Z'; i++)
        {
            alpha.set(i);
        }
    }

    /**
     * BitSet for alphanum (join of alpha &amp; digit).
     * <p>
     * <blockquote>
     * 
     * <pre>
     * alphanum = alpha | digit
     * </pre>
     * 
     * </blockquote>
     * <p>
     */
    protected static final BitSet alphanum               = new BitSet(256);
    // Static initializer for alphanum
    static
    {
        alphanum.or(alpha);
        alphanum.or(digit);
    }

    /**
     * BitSet for hex.
     * <p>
     * <blockquote>
     * 
     * <pre>
     * hex = digit
     *     | &quot;A&quot;
     *     | &quot;B&quot;
     *     | &quot;C&quot;
     *     | &quot;D&quot;
     *     | &quot;E&quot;
     *     | &quot;F&quot;
     *     | &quot;a&quot;
     *     | &quot;b&quot;
     *     | &quot;c&quot;
     *     | &quot;d&quot;
     *     | &quot;e&quot;
     *     | &quot;f&quot;
     * </pre>
     * 
     * </blockquote>
     * <p>
     */
    protected static final BitSet hex                    = new BitSet(256);
    // Static initializer for hex
    static
    {
        hex.or(digit);
        for (int i = 'a'; i <= 'f'; i++)
        {
            hex.set(i);
        }
        for (int i = 'A'; i <= 'F'; i++)
        {
            hex.set(i);
        }
    }

    /**
     * BitSet for escaped.
     * <p>
     * <blockquote>
     * 
     * <pre>
     * 
     *  
     *   escaped       = &quot;%&quot; hex hex
     *   
     *  
     * </pre>
     * 
     * </blockquote>
     * <p>
     */
    protected static final BitSet escaped                = new BitSet(256);
    // Static initializer for escaped
    static
    {
        escaped.or(percent);
        escaped.or(hex);
    }

    /**
     * BitSet for mark.
     * <p>
     * <blockquote>
     * 
     * <pre>
     * mark = &quot;-&quot; | &quot;_&quot; | &quot;.&quot; | &quot;!&quot; | &quot;&tilde;&quot; | &quot;*&quot; | &quot;'&quot; | &quot;(&quot; | &quot;)&quot;
     * </pre>
     * 
     * </blockquote>
     * <p>
     */
    protected static final BitSet mark                   = new BitSet(256);
    // Static initializer for mark
    static
    {
        mark.set('-');
        mark.set('_');
        mark.set('.');
        mark.set('!');
        mark.set('~');
        mark.set('*');
        mark.set('\'');
        mark.set('(');
        mark.set(')');
    }

    /**
     * Data characters that are allowed in a URI but do not have a reserved
     * purpose are called unreserved.
     * <p>
     * <blockquote>
     * 
     * <pre>
     * unreserved = alphanum | mark
     * </pre>
     * 
     * </blockquote>
     * <p>
     */
    protected static final BitSet unreserved             = new BitSet(256);
    // Static initializer for unreserved
    static
    {
        unreserved.or(alphanum);
        unreserved.or(mark);
    }

    /**
     * BitSet for reserved.
     * <p>
     * <blockquote>
     * 
     * <pre>
     * reserved = &quot;;&quot; | &quot;/&quot; | &quot;?&quot; | &quot;:&quot; | &quot;@&quot; | &quot;&amp;&quot; | &quot;=&quot; | &quot;+&quot; | &quot;$&quot; | &quot;,&quot;
     * </pre>
     * 
     * </blockquote>
     * <p>
     */
    protected static final BitSet reserved               = new BitSet(256);
    // Static initializer for reserved
    static
    {
        reserved.set(';');
        reserved.set('/');
        reserved.set('?');
        reserved.set(':');
        reserved.set('@');
        reserved.set('&');
        reserved.set('=');
        reserved.set('+');
        reserved.set('$');
        reserved.set(',');
    }

    /**
     * BitSet for uric.
     * <p>
     * <blockquote>
     * 
     * <pre>
     * uric = reserved | unreserved | escaped
     * </pre>
     * 
     * </blockquote>
     * <p>
     */
    protected static final BitSet uric                   = new BitSet(256);
    // Static initializer for uric
    static
    {
        uric.or(reserved);
        uric.or(unreserved);
        uric.or(escaped);
    }

    /**
     * BitSet for fragment (alias for uric).
     * <p>
     * <blockquote>
     * 
     * <pre>
     * 
     *  
     *   fragment      = *uric
     *   
     *  
     * </pre>
     * 
     * </blockquote>
     * <p>
     */
    protected static final BitSet fragment               = uric;

    /**
     * BitSet for query (alias for uric).
     * <p>
     * <blockquote>
     * 
     * <pre>
     * 
     *  
     *   query         = *uric
     *   
     *  
     * </pre>
     * 
     * </blockquote>
     * <p>
     */
    protected static final BitSet query                  = uric;

    /**
     * BitSet for pchar.
     * <p>
     * <blockquote>
     * 
     * <pre>
     * pchar = unreserved | escaped | &quot;:&quot; | &quot;@&quot; | &quot;&amp;&quot; | &quot;=&quot; | &quot;+&quot; | &quot;$&quot; | &quot;,&quot;
     * </pre>
     * 
     * </blockquote>
     * <p>
     */
    protected static final BitSet pchar                  = new BitSet(256);
    // Static initializer for pchar
    static
    {
        pchar.or(unreserved);
        pchar.or(escaped);
        pchar.set(':');
        pchar.set('@');
        pchar.set('&');
        pchar.set('=');
        pchar.set('+');
        pchar.set('$');
        pchar.set(',');
    }

    /**
     * BitSet for param (alias for pchar).
     * <p>
     * <blockquote>
     * 
     * <pre>
     * 
     *  
     *   param         = *pchar
     *   
     *  
     * </pre>
     * 
     * </blockquote>
     * <p>
     */
    protected static final BitSet param                  = pchar;

    /**
     * BitSet for segment.
     * <p>
     * <blockquote>
     * 
     * <pre>
     * 
     *  
     *   segment       = *pchar *( &quot;;&quot; param )
     *   
     *  
     * </pre>
     * 
     * </blockquote>
     * <p>
     */
    protected static final BitSet segment                = new BitSet(256);
    // Static initializer for segment
    static
    {
        segment.or(pchar);
        segment.set(';');
        segment.or(param);
    }

    /**
     * BitSet for path segments.
     * <p>
     * <blockquote>
     * 
     * <pre>
     * 
     *  
     *   path_segments = segment *( &quot;/&quot; segment )
     *   
     *  
     * </pre>
     * 
     * </blockquote>
     * <p>
     */
    protected static final BitSet path_segments          = new BitSet(256);
    // Static initializer for path_segments
    static
    {
        path_segments.set('/');
        path_segments.or(segment);
    }

    /**
     * URI absolute path.
     * <p>
     * <blockquote>
     * 
     * <pre>
     * 
     *  
     *   abs_path      = &quot;/&quot;  path_segments
     *   
     *  
     * </pre>
     * 
     * <blockquote>
     * <p>
     */
    protected static final BitSet abs_path               = new BitSet(256);
    // Static initializer for abs_path
    static
    {
        abs_path.set('/');
        abs_path.or(path_segments);
    }

    /**
     * URI bitset for encoding typical non-slash characters.
     * <p>
     * <blockquote>
     * 
     * <pre>
     * uric_no_slash = unreserved
     *     | escaped
     *     | &quot;;&quot;
     *     | &quot;?&quot;
     *     | &quot;:&quot;
     *     | &quot;@&quot;
     *     | &quot;&amp;&quot;
     *     | &quot;=&quot;
     *     | &quot;+&quot;
     *     | &quot;$&quot;
     *     | &quot;,&quot;
     * </pre>
     * 
     * </blockquote>
     * <p>
     */
    protected static final BitSet uric_no_slash          = new BitSet(256);
    // Static initializer for uric_no_slash
    static
    {
        uric_no_slash.or(unreserved);
        uric_no_slash.or(escaped);
        uric_no_slash.set(';');
        uric_no_slash.set('?');
        uric_no_slash.set(';');
        uric_no_slash.set('@');
        uric_no_slash.set('&');
        uric_no_slash.set('=');
        uric_no_slash.set('+');
        uric_no_slash.set('$');
        uric_no_slash.set(',');
    }

    /**
     * URI bitset that combines uric_no_slash and uric.
     * <p>
     * <blockquote>
     * 
     * <pre>
     * opaque_part = uric_no_slash * uric
     * </pre>
     * 
     * </blockquote>
     * <p>
     */
    protected static final BitSet opaque_part            = new BitSet(256);
    // Static initializer for opaque_part
    static
    {
        opaque_part.or(uric_no_slash);
        opaque_part.or(uric);
    }

    /**
     * URI bitset that combines absolute path and opaque part.
     * <p>
     * <blockquote>
     * 
     * <pre>
     * 
     *  
     *   path          = [ abs_path | opaque_part ]
     *   
     *  
     * </pre>
     * 
     * </blockquote>
     * <p>
     */
    protected static final BitSet path                   = new BitSet(256);
    // Static initializer for path
    static
    {
        path.or(abs_path);
        path.or(opaque_part);
    }

    /**
     * Port, a logical alias for digit.
     */
    protected static final BitSet port                   = digit;

    /**
     * Bitset that combines digit and dot fo IPv$address.
     * <p>
     * <blockquote>
     * 
     * <pre>
     * 
     *  
     *   IPv4address   = 1*digit &quot;.&quot; 1*digit &quot;.&quot; 1*digit &quot;.&quot; 1*digit
     *   
     *  
     * </pre>
     * 
     * </blockquote>
     * <p>
     */
    protected static final BitSet IPv4address            = new BitSet(256);
    // Static initializer for IPv4address
    static
    {
        IPv4address.or(digit);
        IPv4address.set('.');
    }

    /**
     * RFC 2373.
     * <p>
     * <blockquote>
     * 
     * <pre>
     * 
     *  
     *   IPv6address = hexpart [ &quot;:&quot; IPv4address ]
     *   
     *  
     * </pre>
     * 
     * </blockquote>
     * <p>
     */
    protected static final BitSet IPv6address            = new BitSet(256);
    // Static initializer for IPv6address reference
    static
    {
        IPv6address.or(hex); // hexpart
        IPv6address.set(':');
        IPv6address.or(IPv4address);
    }

    /**
     * RFC 2732, 2373.
     * <p>
     * <blockquote>
     * 
     * <pre>
     * 
     *  
     *   IPv6reference   = &quot;[&quot; IPv6address &quot;]&quot;
     *   
     *  
     * </pre>
     * 
     * </blockquote>
     * <p>
     */
    protected static final BitSet IPv6reference          = new BitSet(256);
    // Static initializer for IPv6reference
    static
    {
        IPv6reference.set('[');
        IPv6reference.or(IPv6address);
        IPv6reference.set(']');
    }

    /**
     * BitSet for toplabel.
     * <p>
     * <blockquote>
     * 
     * <pre>
     * 
     *  
     *   toplabel      = alpha | alpha *( alphanum | &quot;-&quot; ) alphanum
     *   
     *  
     * </pre>
     * 
     * </blockquote>
     * <p>
     */
    protected static final BitSet toplabel               = new BitSet(256);
    // Static initializer for toplabel
    static
    {
        toplabel.or(alphanum);
        toplabel.set('-');
    }

    /**
     * BitSet for domainlabel.
     * <p>
     * <blockquote>
     * 
     * <pre>
     * 
     *  
     *   domainlabel   = alphanum | alphanum *( alphanum | &quot;-&quot; ) alphanum
     *   
     *  
     * </pre>
     * 
     * </blockquote>
     * <p>
     */
    protected static final BitSet domainlabel            = toplabel;

    /**
     * BitSet for hostname.
     * <p>
     * <blockquote>
     * 
     * <pre>
     * 
     *  
     *   hostname      = *( domainlabel &quot;.&quot; ) toplabel [ &quot;.&quot; ]
     *   
     *  
     * </pre>
     * 
     * </blockquote>
     * <p>
     */
    protected static final BitSet hostname               = new BitSet(256);
    // Static initializer for hostname
    static
    {
        hostname.or(toplabel);
        // hostname.or(domainlabel);
        hostname.set('.');
    }

    /**
     * BitSet for host.
     * <p>
     * <blockquote>
     * 
     * <pre>
     * host = hostname | IPv4address | IPv6reference
     * </pre>
     * 
     * </blockquote>
     * <p>
     */
    protected static final BitSet host                   = new BitSet(256);
    // Static initializer for host
    static
    {
        host.or(hostname);
        // host.or(IPv4address);
        host.or(IPv6reference); // IPv4address
    }

    /**
     * BitSet for hostport.
     * <p>
     * <blockquote>
     * 
     * <pre>
     * 
     *  
     *   hostport      = host [ &quot;:&quot; port ]
     *   
     *  
     * </pre>
     * 
     * </blockquote>
     * <p>
     */
    protected static final BitSet hostport               = new BitSet(256);
    // Static initializer for hostport
    static
    {
        hostport.or(host);
        hostport.set(':');
        hostport.or(port);
    }

    /**
     * Bitset for userinfo.
     * <p>
     * <blockquote>
     * 
     * <pre>
     * 
     *  
     *   userinfo      = *( unreserved | escaped |
     *                      &quot;;&quot; | &quot;:&quot; | &quot;&amp;&quot; | &quot;=&quot; | &quot;+&quot; | &quot;$&quot; | &quot;,&quot; )
     *   
     *  
     * </pre>
     * 
     * </blockquote>
     * <p>
     */
    protected static final BitSet userinfo               = new BitSet(256);
    // Static initializer for userinfo
    static
    {
        userinfo.or(unreserved);
        userinfo.or(escaped);
        userinfo.set(';');
        userinfo.set(':');
        userinfo.set('&');
        userinfo.set('=');
        userinfo.set('+');
        userinfo.set('$');
        userinfo.set(',');
    }

    /**
     * Bitset for server.
     * <p>
     * <blockquote>
     * 
     * <pre>
     * 
     *  
     *   server        = [ [ userinfo &quot;@&quot; ] hostport ]
     *   
     *  
     * </pre>
     * 
     * </blockquote>
     * <p>
     */
    protected static final BitSet server                 = new BitSet(256);
    // Static initializer for server
    static
    {
        server.or(userinfo);
        server.set('@');
        server.or(hostport);
    }

    /**
     * BitSet for reg_name.
     * <p>
     * <blockquote>
     * 
     * <pre>
     * reg_name = 1 * (unreserved | escaped | &quot;$&quot; | &quot;,&quot; | &quot;;&quot; | &quot;:&quot; | &quot;@&quot; | &quot;&amp;&quot; | &quot;=&quot; | &quot;+&quot;)
     * </pre>
     * 
     * </blockquote>
     * <p>
     */
    protected static final BitSet reg_name               = new BitSet(256);
    // Static initializer for reg_name
    static
    {
        reg_name.or(unreserved);
        reg_name.or(escaped);
        reg_name.set('$');
        reg_name.set(',');
        reg_name.set(';');
        reg_name.set(':');
        reg_name.set('@');
        reg_name.set('&');
        reg_name.set('=');
        reg_name.set('+');
    }

    /**
     * BitSet for authority.
     * <p>
     * <blockquote>
     * 
     * <pre>
     * authority = server | reg_name
     * </pre>
     * 
     * </blockquote>
     * <p>
     */
    protected static final BitSet authority              = new BitSet(256);
    // Static initializer for authority
    static
    {
        authority.or(server);
        authority.or(reg_name);
    }

    /**
     * BitSet for scheme.
     * <p>
     * <blockquote>
     * 
     * <pre>
     * scheme = alpha * (alpha | digit | &quot;+&quot; | &quot;-&quot; | &quot;.&quot;)
     * </pre>
     * 
     * </blockquote>
     * <p>
     */
    protected static final BitSet scheme                 = new BitSet(256);
    // Static initializer for scheme
    static
    {
        scheme.or(alpha);
        scheme.or(digit);
        scheme.set('+');
        scheme.set('-');
        scheme.set('.');
    }

    /**
     * BitSet for rel_segment.
     * <p>
     * <blockquote>
     * 
     * <pre>
     * rel_segment = 1 * (unreserved | escaped | &quot;;&quot; | &quot;@&quot; | &quot;&amp;&quot; | &quot;=&quot; | &quot;+&quot; | &quot;$&quot; | &quot;,&quot;)
     * </pre>
     * 
     * </blockquote>
     * <p>
     */
    protected static final BitSet rel_segment            = new BitSet(256);
    // Static initializer for rel_segment
    static
    {
        rel_segment.or(unreserved);
        rel_segment.or(escaped);
        rel_segment.set(';');
        rel_segment.set('@');
        rel_segment.set('&');
        rel_segment.set('=');
        rel_segment.set('+');
        rel_segment.set('$');
        rel_segment.set(',');
    }

    /**
     * BitSet for rel_path.
     * <p>
     * <blockquote>
     * 
     * <pre>
     * rel_path = rel_segment[abs_path]
     * </pre>
     * 
     * </blockquote>
     * <p>
     */
    protected static final BitSet rel_path               = new BitSet(256);
    // Static initializer for rel_path
    static
    {
        rel_path.or(rel_segment);
        rel_path.or(abs_path);
    }

    /**
     * BitSet for net_path.
     * <p>
     * <blockquote>
     * 
     * <pre>
     * 
     *  
     *   net_path      = &quot;//&quot; authority [ abs_path ]
     *   
     *  
     * </pre>
     * 
     * </blockquote>
     * <p>
     */
    protected static final BitSet net_path               = new BitSet(256);
    // Static initializer for net_path
    static
    {
        net_path.set('/');
        net_path.or(authority);
        net_path.or(abs_path);
    }

    /**
     * BitSet for hier_part.
     * <p>
     * <blockquote>
     * 
     * <pre>
     * 
     *  
     *   hier_part     = ( net_path | abs_path ) [ &quot;?&quot; query ]
     *   
     *  
     * </pre>
     * 
     * </blockquote>
     * <p>
     */
    protected static final BitSet hier_part              = new BitSet(256);
    // Static initializer for hier_part
    static
    {
        hier_part.or(net_path);
        hier_part.or(abs_path);
        // hier_part.set('?'); aleady included
        hier_part.or(query);
    }

    /**
     * BitSet for relativeURI.
     * <p>
     * <blockquote>
     * 
     * <pre>
     * 
     *  
     *   relativeURI   = ( net_path | abs_path | rel_path ) [ &quot;?&quot; query ]
     *   
     *  
     * </pre>
     * 
     * </blockquote>
     * <p>
     */
    protected static final BitSet relativeURI            = new BitSet(256);
    // Static initializer for relativeURI
    static
    {
        relativeURI.or(net_path);
        relativeURI.or(abs_path);
        relativeURI.or(rel_path);
        // relativeURI.set('?'); aleady included
        relativeURI.or(query);
    }

    /**
     * BitSet for absoluteURI.
     * <p>
     * <blockquote>
     * 
     * <pre>
     * 
     *  
     *   absoluteURI   = scheme &quot;:&quot; ( hier_part | opaque_part )
     *   
     *  
     * </pre>
     * 
     * </blockquote>
     * <p>
     */
    protected static final BitSet absoluteURI            = new BitSet(256);
    // Static initializer for absoluteURI
    static
    {
        absoluteURI.or(scheme);
        absoluteURI.set(':');
        absoluteURI.or(hier_part);
        absoluteURI.or(opaque_part);
    }

    /**
     * BitSet for URI-reference.
     * <p>
     * <blockquote>
     * 
     * <pre>
     * 
     *  
     *   URI-reference = [ absoluteURI | relativeURI ] [ &quot;#&quot; fragment ]
     *   
     *  
     * </pre>
     * 
     * </blockquote>
     * <p>
     */
    protected static final BitSet URI_reference          = new BitSet(256);
    // Static initializer for URI_reference
    static
    {
        URI_reference.or(absoluteURI);
        URI_reference.or(relativeURI);
        URI_reference.set('#');
        URI_reference.or(fragment);
    }

    // ---------------------------- Characters disallowed within the URI syntax
    // Excluded US-ASCII Characters are like control, space, delims and unwise

    /**
     * BitSet for control.
     */
    public static final BitSet    control                = new BitSet(256);
    // Static initializer for control
    static
    {
        for (int i = 0; i <= 0x1F; i++)
        {
            control.set(i);
        }
        control.set(0x7F);
    }

    /**
     * BitSet for space.
     */
    public static final BitSet    space                  = new BitSet(256);
    // Static initializer for space
    static
    {
        space.set(0x20);
    }

    /**
     * BitSet for delims.
     */
    public static final BitSet    delims                 = new BitSet(256);
    // Static initializer for delims
    static
    {
        delims.set('<');
        delims.set('>');
        delims.set('#');
        delims.set('%');
        delims.set('"');
    }

    /**
     * BitSet for unwise.
     */
    public static final BitSet    unwise                 = new BitSet(256);
    // Static initializer for unwise
    static
    {
        unwise.set('{');
        unwise.set('}');
        unwise.set('|');
        unwise.set('\\');
        unwise.set('^');
        unwise.set('[');
        unwise.set(']');
        unwise.set('`');
    }

    /**
     * Disallowed rel_path before escaping.
     */
    public static final BitSet    disallowed_rel_path    = new BitSet(256);
    // Static initializer for disallowed_rel_path
    static
    {
        disallowed_rel_path.or(uric);
        disallowed_rel_path.andNot(rel_path);
    }

    /**
     * Disallowed opaque_part before escaping.
     */
    public static final BitSet    disallowed_opaque_part = new BitSet(256);
    // Static initializer for disallowed_opaque_part
    static
    {
        disallowed_opaque_part.or(uric);
        disallowed_opaque_part.andNot(opaque_part);
    }

    // ------------------------------- Characters allowed within each component

    /**
     * Those characters that are allowed within the authority component.
     */
    public static final BitSet    allowed_authority      = new BitSet(256);
    // Static initializer for allowed_authority
    static
    {
        allowed_authority.or(authority);
        allowed_authority.clear('%');
    }

    /**
     * Those characters that are allowed within the opaque_part.
     */
    public static final BitSet    allowed_opaque_part    = new BitSet(256);
    // Static initializer for allowed_opaque_part
    static
    {
        allowed_opaque_part.or(opaque_part);
        allowed_opaque_part.clear('%');
    }

    /**
     * Those characters that are allowed within the reg_name.
     */
    public static final BitSet    allowed_reg_name       = new BitSet(256);
    // Static initializer for allowed_reg_name
    static
    {
        allowed_reg_name.or(reg_name);
        // allowed_reg_name.andNot(percent);
        allowed_reg_name.clear('%');
    }

    /**
     * Those characters that are allowed within the userinfo component.
     */
    public static final BitSet    allowed_userinfo       = new BitSet(256);
    // Static initializer for allowed_userinfo
    static
    {
        allowed_userinfo.or(userinfo);
        // allowed_userinfo.andNot(percent);
        allowed_userinfo.clear('%');
    }

    /**
     * Those characters that are allowed within the IPv6reference component. The
     * characters '[', ']' in IPv6reference should be excluded.
     */
    public static final BitSet    allowed_IPv6reference  = new BitSet(256);
    // Static initializer for allowed_IPv6reference
    static
    {
        allowed_IPv6reference.or(IPv6reference);
        // allowed_IPv6reference.andNot(unwise);
        allowed_IPv6reference.clear('[');
        allowed_IPv6reference.clear(']');
    }

    /**
     * Those characters that are allowed within the host component. The
     * characters '[', ']' in IPv6reference should be excluded.
     */
    public static final BitSet    allowed_host           = new BitSet(256);
    // Static initializer for allowed_host
    static
    {
        allowed_host.or(hostname);
        allowed_host.or(allowed_IPv6reference);
    }

    /**
     * Those characters that are allowed within the abs_path.
     */
    public static final BitSet    allowed_abs_path       = new BitSet(256);
    // Static initializer for allowed_abs_path
    static
    {
        allowed_abs_path.or(abs_path);
        // allowed_abs_path.set('/'); // aleady included
        allowed_abs_path.andNot(percent);
    }

    /**
     * Those characters that are allowed within the rel_path.
     */
    public static final BitSet    allowed_rel_path       = new BitSet(256);
    // Static initializer for allowed_rel_path
    static
    {
        allowed_rel_path.or(rel_path);
        allowed_rel_path.clear('%');
    }

    /**
     * Those characters that are allowed within the query component.
     */
    public static final BitSet    allowed_query          = new BitSet(256);
    // Static initializer for allowed_query
    static
    {
        allowed_query.or(uric);
        allowed_query.clear('%');
        allowed_query.clear('=');
        allowed_query.clear('&');
    }

    /**
     * Those characters that are allowed within the fragment component.
     */
    public static final BitSet    allowed_fragment       = new BitSet(256);
    // Static initializer for allowed_fragment
    static
    {
        allowed_fragment.or(uric);
        allowed_fragment.clear('%');
    }

    // ------------------------------------------- Flags for this URI-reference

    // URI-reference = [ absoluteURI | relativeURI ] [ "#" fragment ]
    // absoluteURI = scheme ":" ( hier_part | opaque_part )
    protected boolean             _is_hier_part;

    protected boolean             _is_opaque_part;

    // relativeURI = ( net_path | abs_path | rel_path ) [ "?" query ]
    // hier_part = ( net_path | abs_path ) [ "?" query ]
    protected boolean             _is_net_path;

    protected boolean             _is_abs_path;

    protected boolean             _is_rel_path;

    // net_path = "//" authority [ abs_path ]
    // authority = server | reg_name
    protected boolean             _is_reg_name;

    protected boolean             _is_server;                                  // =
    // _has_server

    // server = [ [ userinfo "@" ] hostport ]
    // host = hostname | IPv4address | IPv6reference
    protected boolean             _is_hostname;

    protected boolean             _is_IPv4address;

    protected boolean             _is_IPv6reference;

    // ------------------------------------------ Character and escape encoding

    /**
     * Encode with the default protocol charset.
     * 
     * @param original
     *            the original character sequence
     * @param allowed
     *            those characters that are allowed within a component
     * @return URI character sequence
     * @exception URIException
     *                null component or unsupported character encoding
     */
    protected static char[] encode(String original, BitSet allowed)
        throws URIException
    {

        return encode(original, allowed, _protocolCharset);
    }

    /**
     * Encodes URI string.
     * 
     * This is a two mapping, one from original characters to octets, and
     * subsequently a second from octets to URI characters:
     * <p>
     * <blockquote>
     * 
     * <pre>
     * 
     *  
     *     original character sequence-&gt;octet sequence-&gt;URI character sequence
     *   
     *  
     * </pre>
     * 
     * </blockquote>
     * <p>
     * 
     * An escaped octet is encoded as a character triplet, consisting of the
     * percent character "%" followed by the two hexadecimal digits representing
     * the octet code. For example, "%20" is the escaped encoding for the
     * US-ASCII space character.
     * <p>
     * Conversion from the local filesystem character set to UTF-8 will normally
     * involve a two step process. First convert the local character set to the
     * UCS; then convert the UCS to UTF-8. The first step in the process can be
     * performed by maintaining a mapping table that includes the local
     * character set code and the corresponding UCS code. The next step is to
     * convert the UCS character code to the UTF-8 encoding.
     * <p>
     * Mapping between vendor codepages can be done in a very similar manner as
     * described above.
     * <p>
     * The only time escape encodings can allowedly be made is when a URI is
     * being created from its component parts. The escape and validate methods
     * are internally performed within this method.
     * 
     * @param original
     *            the original character sequence
     * @param allowed
     *            those characters that are allowed within a component
     * @param charset
     *            the protocol charset
     * @return URI character sequence
     * @exception URIException
     *                null component or unsupported character encoding
     */
    protected static char[] encode(String original,
                                   BitSet allowed,
                                   String charset) throws URIException
    {

        // encode original to uri characters.
        if (original == null)
        {
            throw new URIException(URIException.PARSING, "null");
        }
        // escape octet to uri characters.
        if (allowed == null)
        {
            throw new URIException(URIException.PARSING,
                "null allowed characters");
        }
        byte[] octets;
        try
        {
            octets = original.getBytes(charset);
        }
        catch (UnsupportedEncodingException error)
        {
            throw new URIException(URIException.UNSUPPORTED_ENCODING, charset);
        }
        StringBuffer buf = new StringBuffer(octets.length);
        for (int i = 0; i < octets.length; i++)
        {
            char c = (char)octets[i];
            if (allowed.get(c))
            {
                buf.append(c);
            }
            else
            {
                buf.append('%');
                byte b = octets[i]; // use the original byte value
                char hexadecimal = Character.forDigit((b >> 4) & 0xF, 16);
                buf.append(Character.toUpperCase(hexadecimal)); // high
                hexadecimal = Character.forDigit(b & 0xF, 16);
                buf.append(Character.toUpperCase(hexadecimal)); // low
            }
        }

        return buf.toString().toCharArray();
    }

    /**
     * Decode with the default protocol charset.
     * 
     * @param component
     *            the URI character sequence
     * @return original character sequence
     * @exception URIException
     *                incomplete trailing escape pattern or unsupported
     *                character encoding
     */
    protected static String decode(char[] component) throws URIException
    {
        return decode(component, _protocolCharset);
    }

    /**
     * Decodes URI encoded string.
     * 
     * This is a two mapping, one from URI characters to octets, and
     * subsequently a second from octets to original characters:
     * <p>
     * <blockquote>
     * 
     * <pre>
     * 
     *  
     *     URI character sequence-&gt;octet sequence-&gt;original character sequence
     *   
     *  
     * </pre>
     * 
     * </blockquote>
     * <p>
     * 
     * A URI must be separated into its components before the escaped characters
     * within those components can be allowedly decoded.
     * <p>
     * Notice that there is a chance that URI characters that are non UTF-8 may
     * be parsed as valid UTF-8. A recent non-scientific analysis found that EUC
     * encoded Japanese words had a 2.7% false reading; SJIS had a 0.0005% false
     * reading; other encoding such as ASCII or KOI-8 have a 0% false reading.
     * <p>
     * The percent "%" character always has the reserved purpose of being the
     * escape indicator, it must be escaped as "%25" in order to be used as data
     * within a URI.
     * <p>
     * The unescape method is internally performed within this method.
     * 
     * @param component
     *            the URI character sequence
     * @param charset
     *            the protocol charset
     * @return original character sequence
     * @exception URIException
     *                incomplete trailing escape pattern or unsupported
     *                character encoding
     */
    protected static String decode(char[] component, String charset)
        throws URIException
    {

        // unescape uri characters to octets
        if (component == null)
            return null;

        byte[] octets;
        try
        {
            octets = new String(component).getBytes(charset);
        }
        catch (UnsupportedEncodingException error)
        {
            throw new URIException(URIException.UNSUPPORTED_ENCODING,
                "not supported " + charset + " encoding");
        }
        int length = octets.length;
        int oi = 0; // output index
        for (int ii = 0; ii < length; oi++)
        {
            byte aByte = octets[ii++];
            if (aByte == '%' && ii + 2 <= length)
            {
                byte high = (byte)Character.digit((char)octets[ii++], 16);
                byte low = (byte)Character.digit((char)octets[ii++], 16);
                if (high == -1 || low == -1)
                {
                    throw new URIException(URIException.ESCAPING,
                        "incomplete trailing escape pattern");

                }
                aByte = (byte)((high << 4) + low);
            }
            octets[oi] = aByte;
        }

        String result;
        try
        {
            result = new String(octets, 0, oi, charset);
        }
        catch (UnsupportedEncodingException error)
        {
            throw new URIException(URIException.UNSUPPORTED_ENCODING,
                "not supported " + charset + " encoding");
        }

        return result;
    }

    /**
     * Pre-validate the unescaped URI string within a specific component.
     * 
     * @param component
     *            the component string within the component
     * @param disallowed
     *            those characters disallowed within the component
     * @return if true, it doesn't have the disallowed characters if false, the
     *         component is undefined or an incorrect one
     */
    protected boolean prevalidate(String component, BitSet disallowed)
    {
        // prevalidate the given component by disallowed characters
        if (component == null)
        {
            return false; // undefined
        }
        char[] target = component.toCharArray();
        for (int i = 0; i < target.length; i++)
        {
            if (disallowed.get(target[i]))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Validate the URI characters within a specific component. The component
     * must be performed after escape encoding. Or it doesn't include escaped
     * characters.
     * 
     * @param component
     *            the characters sequence within the component
     * @param generous
     *            those characters that are allowed within a component
     * @return if true, it's the correct URI character sequence
     */
    protected boolean validate(char[] component, BitSet generous)
    {
        // validate each component by generous characters
        return validate(component, 0, -1, generous);
    }

    /**
     * Validate the URI characters within a specific component. The component
     * must be performed after escape encoding. Or it doesn't include escaped
     * characters.
     * <p>
     * It's not that much strict, generous. The strict validation might be
     * performed before being called this method.
     * 
     * @param component
     *            the characters sequence within the component
     * @param soffset
     *            the starting offset of the given component
     * @param eoffset
     *            the ending offset of the given component if -1, it means the
     *            length of the component
     * @param generous
     *            those characters that are allowed within a component
     * @return if true, it's the correct URI character sequence
     */
    protected boolean validate(char[] component,
                               int soffset,
                               int eoffset,
                               BitSet generous)
    {
        // validate each component by generous characters
        if (eoffset == -1)
        {
            eoffset = component.length - 1;
        }
        for (int i = soffset; i < eoffset; i++)
        {
            if (!generous.get(component[i]))
                return false;
        }
        return true;
    }

    /**
     * In order to avoid any possilbity of conflict with non-ASCII characters,
     * Parse a URI reference as a <code>String</code> with the character
     * encoding of the local system or the document.
     * <p>
     * The following line is the regular expression for breaking-down a URI
     * reference into its components.
     * <p>
     * <blockquote>
     * 
     * <pre>
     * 
     *  
     *     &circ;(([&circ;:/?#]+):)?(//([&circ;/?#]*))?([&circ;?#]*)(\?([&circ;#]*))?(#(.*))?
     *      12            3  4          5       6  7        8 9
     *   
     *  
     * </pre>
     * 
     * </blockquote>
     * <p>
     * For example, matching the above expression to
     * http://jakarta.apache.org/ietf/uri/#Related results in the following
     * subexpression matches:
     * <p>
     * <blockquote>
     * 
     * <pre>
     * 
     *  
     *                 $1 = http:
     *    scheme    =  $2 = http
     *                 $3 = //jakarta.apache.org
     *    authority =  $4 = jakarta.apache.org
     *    path      =  $5 = /ietf/uri/
     *                 $6 = &lt;undefined&gt;
     *    query     =  $7 = &lt;undefined&gt;
     *                 $8 = #Related
     *    fragment  =  $9 = Related
     *   
     *  
     * </pre>
     * 
     * </blockquote>
     * <p>
     * 
     * @param original
     *            the original character sequence
     * @param e
     *            <code>true</code> if <code>original</code> is escaped
     * @return the original character sequence
     * @exception URIException
     */
    protected void parseUriReference(String original, boolean e)
        throws URIException
    {

        // validate and contruct the URI character sequence
        if (original == null)
        {
            throw new URIException("URI-Reference required");
        }

        /**
         * @ ^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\?([^#]*))?(#(.*))?
         */
        String tmp = original.trim();

        /**
         * The starting index
         */
        int from = 0;

        /**
         * The test flag whether the URI is started from the path component.
         */
        boolean isStartedFromPath = false;
        int atColon = tmp.indexOf(':');
        int atSlash = tmp.indexOf('/');
        if (atColon < 0 || (atSlash >= 0 && atSlash < atColon))
        {
            isStartedFromPath = true;
        }

        /**
         * Find @ symbol.
         *      <p>
         *      <blockquote>
         * 
         * <pre>
         * 
         *  
         *       @@@@@@@@
         *    &circ;(([&circ;:/?#]+):)?(//([&circ;/?#]*))?([&circ;?#]*)(\?([&circ;#]*))?(#(.*))?
         *   
         *  
         * </pre>
         * 
         * </blockquote>
         * <p>
         */
        int at = indexFirstOf(tmp, isStartedFromPath ? "/?#" : ":/?#", from);
        if (at == -1)
            at = 0;

        /**
         * The length of the sequence of characters. It may not be equal to the
         * length of the byte array.
         */
        int length = tmp.length();

        /**
         * Find scheme.
         * <p>
         * <blockquote>
         * 
         * <pre>
         * 
         *  
         *    scheme    =  $2 = http
         *                @
         *    &circ;(([&circ;:/?#]+):)?(//([&circ;/?#]*))?([&circ;?#]*)(\?([&circ;#]*))?(#(.*))?
         *   
         *  
         * </pre>
         * 
         * </blockquote>
         * <p>
         */
        if (at < length && tmp.charAt(at) == ':')
        {
            char[] target = tmp.substring(0, at).toLowerCase().toCharArray();
            if (validate(target, scheme))
            {
                _scheme = target;
            }
            else
            {
                throw new URIException("incorrect scheme");
            }
            from = ++at;
        }

        /**
         * Find authority.
         * <p>
         * <blockquote>
         * 
         * <pre>
         * 
         *  
         *    authority =  $4 = jakarta.apache.org
         *                    @@
         *    &circ;(([&circ;:/?#]+):)?(//([&circ;/?#]*))?([&circ;?#]*)(\?([&circ;#]*))?(#(.*))?
         *   
         *  
         * </pre>
         * 
         * </blockquote>
         * <p>
         */
        // Reset flags
        _is_net_path = _is_abs_path = _is_rel_path = _is_hier_part = false;
        if (0 <= at && at < length && tmp.charAt(at) == '/')
        {
            // Set flag
            _is_hier_part = true;
            if (at + 2 < length && tmp.charAt(at + 1) == '/')
            {
                // the temporary index to start the search from
                int next = indexFirstOf(tmp, "/?#", at + 2);
                if (next == -1)
                {
                    next = (tmp.substring(at + 2).length() == 0) ? at + 2 : tmp
                            .length();
                }
                parseAuthority(tmp.substring(at + 2, next), e);
                from = at = next;
                // Set flag
                _is_net_path = true;
            }
            if (from == at)
            {
                // Set flag
                _is_abs_path = true;
            }
        }

        /**
         * Find path.
         * <p>
         * <blockquote>
         * 
         * <pre>
         * 
         *  
         *    path      =  $5 = /ietf/uri/
         *                                  @@@@@@
         *    &circ;(([&circ;:/?#]+):)?(//([&circ;/?#]*))?([&circ;?#]*)(\?([&circ;#]*))?(#(.*))?
         *   
         *  
         * </pre>
         * 
         * </blockquote>
         * <p>
         */
        if (from < length)
        {
            // rel_path = rel_segment [ abs_path ]
            int next = indexFirstOf(tmp, "?#", from);
            if (next == -1)
            {
                next = tmp.length();
            }
            if (!_is_abs_path)
            {
                if (!e
                    && prevalidate(tmp.substring(from, next),
                                   disallowed_rel_path)
                    || e
                    && validate(tmp.substring(from, next).toCharArray(),
                                rel_path))
                {
                    // Set flag
                    _is_rel_path = true;
                }
                else if (!e
                    && prevalidate(tmp.substring(from, next),
                                   disallowed_opaque_part)
                    || e
                    && validate(tmp.substring(from, next).toCharArray(),
                                opaque_part))
                {
                    // Set flag
                    _is_opaque_part = true;
                }
                else
                {
                    // the path component may be empty
                    _path = null;
                }
            }
            setPath(tmp.substring(from, next));
            at = next;
        }

        /**
         * Find query.
         * <p>
         * <blockquote>
         * 
         * <pre>
         * 
         *  
         *    query     =  $7 = &lt;undefined&gt;
         *                                          @@@@@@@@@
         *    &circ;(([&circ;:/?#]+):)?(//([&circ;/?#]*))?([&circ;?#]*)(\?([&circ;#]*))?(#(.*))?
         *   
         *  
         * </pre>
         * 
         * </blockquote>
         * <p>
         */
        if (0 <= at && at + 1 < length && tmp.charAt(at) == '?')
        {
            int next = tmp.indexOf('#', at + 1);
            if (next == -1)
            {
                next = tmp.length();
            }
            _query = (e)
                ? tmp.substring(at + 1, next).toCharArray()
                : encode(tmp.substring(at + 1, next), allowed_query);
            at = next;
        }

        /**
         * Find fragment.
         * <p>
         * <blockquote>
         * 
         * <pre>
         * 
         *  
         *    fragment  =  $9 = Related
         *                                                     @@@@@@@@
         *    &circ;(([&circ;:/?#]+):)?(//([&circ;/?#]*))?([&circ;?#]*)(\?([&circ;#]*))?(#(.*))?
         *   
         *  
         * </pre>
         * 
         * </blockquote>
         * <p>
         */
        if (0 <= at && at + 1 < length && tmp.charAt(at) == '#')
        {
            _fragment = (e) ? tmp.substring(at + 1).toCharArray() : encode(tmp
                    .substring(at + 1), allowed_fragment);
        }

        // set this URI.
        setUriReference();
    }

    /**
     * Get the earlier index that to be searched for the first occurrance in one
     * of any of the given string.
     * 
     * @param s
     *            the string to be indexed
     * @param d
     *            the delimiters used to index
     * @return the earlier index if there are delimiters
     */
    protected int indexFirstOf(String s, String d)
    {
        return indexFirstOf(s, d, -1);
    }

    /**
     * Get the earlier index that to be searched for the first occurrance in one
     * of any of the given string.
     * 
     * @param s
     *            the string to be indexed
     * @param d
     *            the delimiters used to index
     * @param offset
     *            the from index
     * @return the earlier index if there are delimiters
     */
    protected int indexFirstOf(String s, String d, int offset)
    {
        if (s == null || s.length() == 0)
        {
            return -1;
        }
        if (d == null || d.length() == 0)
        {
            return -1;
        }
        // check boundaries
        if (offset < 0)
        {
            offset = 0;
        }
        else if (offset > s.length())
        {
            return -1;
        }
        // s is never null
        int min = s.length();
        char[] delim = d.toCharArray();
        for (int i = 0; i < delim.length; i++)
        {
            int at = s.indexOf(delim[i], offset);
            if (at >= 0 && at < min)
            {
                min = at;
            }
        }
        return (min == s.length()) ? -1 : min;
    }

    /**
     * Get the earlier index that to be searched for the first occurrance in one
     * of any of the given array.
     * 
     * @param s
     *            the character array to be indexed
     * @param delim
     *            the delimiter used to index
     * @return the ealier index if there are a delimiter
     */
    protected int indexFirstOf(char[] s, char delim)
    {
        return indexFirstOf(s, delim, 0);
    }

    /**
     * Get the earlier index that to be searched for the first occurrance in one
     * of any of the given array.
     * 
     * @param s
     *            the character array to be indexed
     * @param delim
     *            the delimiter used to index
     * @return the ealier index if there is a delimiter
     */
    protected int indexFirstOf(char[] s, char delim, int offset)
    {
        if (s == null || s.length == 0)
        {
            return -1;
        }
        // check boundaries
        if (offset < 0)
        {
            offset = 0;
        }
        else if (offset > s.length)
        {
            return -1;
        }
        for (int i = offset; i < s.length; i++)
        {
            if (s[i] == delim)
            {
                return i;
            }
        }
        return -1;
    }

    /**
     * Parse the authority component.
     * 
     * @param original
     *            the original character sequence of authority component
     * @param e
     *            <code>true</code> if <code>original</code> is escaped
     * @exception URIException
     */
    protected void parseAuthority(String original, boolean e)
        throws URIException
    {

        // Reset flags
        _is_reg_name = _is_server = _is_hostname = _is_IPv4address = _is_IPv6reference = false;

        boolean has_port = true;
        int from = 0;
        int next = original.indexOf('@');
        if (next != -1)
        { // neither -1 and 0
            // each protocol extented from URI supports the specific userinfo
            _userinfo = (e)
                ? original.substring(0, next).toCharArray()
                : encode(original.substring(0, next), allowed_userinfo);
            from = next + 1;
        }
        next = original.indexOf('[', from);
        if (next >= from)
        {
            next = original.indexOf(']', from);
            if (next == -1)
            {
                throw new URIException(URIException.PARSING, "IPv6reference");
            }
            next++;
            // In IPv6reference, '[', ']' should be excluded
            _host = (e)
                ? original.substring(from, next).toCharArray()
                : encode(original.substring(from, next), allowed_IPv6reference);
            // Set flag
            _is_IPv6reference = true;
        }
        else
        { // only for !_is_IPv6reference
            next = original.indexOf(':', from);
            if (next == -1)
            {
                next = original.length();
                has_port = false;
            }
            // REMINDME: it doesn't need the pre-validation
            _host = original.substring(from, next).toCharArray();
            if (validate(_host, IPv4address))
            {
                // Set flag
                _is_IPv4address = true;
            }
            else if (validate(_host, hostname))
            {
                // Set flag
                _is_hostname = true;
            }
            else
            {
                // Set flag
                _is_reg_name = true;
            }
        }
        if (_is_reg_name)
        {
            // Reset flags for a server-based naming authority
            _is_server = _is_hostname = _is_IPv4address = _is_IPv6reference = false;
            // set a registry-based naming authority
            _authority = (e)
                ? original.toString().toCharArray()
                : encode(original.toString(), allowed_reg_name);
        }
        else
        {
            if (original.length() - 1 > next
                && has_port
                && original.charAt(next) == ':')
            { // not empty
                from = next + 1;
                try
                {
                    _port = Integer.parseInt(original.substring(from));
                }
                catch (NumberFormatException error)
                {
                    throw new URIException(URIException.PARSING,
                        "invalid port number");
                }
            }
            // set a server-based naming authority
            StringBuffer buf = new StringBuffer();
            if (_userinfo != null)
            { // has_userinfo
                buf.append(_userinfo);
                buf.append('@');
            }
            if (_host != null)
            {
                buf.append(_host);
                if (_port != -1)
                {
                    buf.append(':');
                    buf.append(_port);
                }
            }
            _authority = buf.toString().toCharArray();
            // Set flag
            _is_server = true;
        }
    }

    /**
     * Once it's parsed successfully, set this URI.
     * 
     * @see #getRawURI
     */
    protected void setUriReference()
    {
        // set _uri
        StringBuffer buf = new StringBuffer();
        // ^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\?([^#]*))?(#(.*))?
        if (_scheme != null)
        {
            buf.append(_scheme);
            buf.append(':');
        }
        if (_is_net_path)
        {
            buf.append("//");
            if (_authority != null)
            { // has_authority
                if (_userinfo != null)
                { // by default, remove userinfo part
                    if (_host != null)
                    {
                        buf.append(_host);
                        if (_port != -1)
                        {
                            buf.append(':');
                            buf.append(_port);
                        }
                    }
                }
                else
                {
                    buf.append(_authority);
                }
            }
        }
        if (_opaque != null && _is_opaque_part)
        {
            buf.append(_opaque);
        }
        else if (_path != null)
        {
            // _is_hier_part or _is_relativeURI
            if (_path.length != 0)
            {
                buf.append(_path);
            }
            if (_query != null)
            { // has_query
                buf.append('?');
                buf.append(_query);
            }
        }
        if (_fragment != null)
        { // has_fragment
            buf.append('#');
            buf.append(_fragment);
        }

        _uri = buf.toString().toCharArray();
    }

    // ----------------------------------------------------------- Test methods

    /**
     * Tell whether or not this URI is absolute.
     * 
     * @return true iif this URI is absoluteURI
     */
    public boolean isAbsoluteURI()
    {
        return (_scheme != null);
    }

    /**
     * Tell whether or not this URI is relative.
     * 
     * @return true iif this URI is relativeURI
     */
    public boolean isRelativeURI()
    {
        return (_scheme == null);
    }

    /**
     * Tell whether or not the absoluteURI of this URI is hier_part.
     * 
     * @return true iif the absoluteURI is hier_part
     */
    public boolean isHierPart()
    {
        return _is_hier_part;
    }

    /**
     * Tell whether or not the absoluteURI of this URI is opaque_part.
     * 
     * @return true iif the absoluteURI is opaque_part
     */
    public boolean isOpaquePart()
    {
        return _is_opaque_part;
    }

    /**
     * Tell whether or not the relativeURI or heir_part of this URI is net_path.
     * It's the same function as the has_authority() method.
     * 
     * @return true iif the relativeURI or heir_part is net_path
     * @see #hasAuthority
     */
    public boolean isNetPath()
    {
        return _is_net_path || (_authority != null);
    }

    /**
     * Tell whether or not the relativeURI or hier_part of this URI is abs_path.
     * 
     * @return true iif the relativeURI or hier_part is abs_path
     */
    public boolean isAbsPath()
    {
        return _is_abs_path;
    }

    /**
     * Tell whether or not the relativeURI of this URI is rel_path.
     * 
     * @return true iif the relativeURI is rel_path
     */
    public boolean isRelPath()
    {
        return _is_rel_path;
    }

    /**
     * Tell whether or not this URI has authority. It's the same function as the
     * is_net_path() method.
     * 
     * @return true iif this URI has authority
     * @see #isNetPath
     */
    public boolean hasAuthority()
    {
        return (_authority != null) || _is_net_path;
    }

    /**
     * Tell whether or not the authority component of this URI is reg_name.
     * 
     * @return true iif the authority component is reg_name
     */
    public boolean isRegName()
    {
        return _is_reg_name;
    }

    /**
     * Tell whether or not the authority component of this URI is server.
     * 
     * @return true iif the authority component is server
     */
    public boolean isServer()
    {
        return _is_server;
    }

    /**
     * Tell whether or not this URI has userinfo.
     * 
     * @return true iif this URI has userinfo
     */
    public boolean hasUserinfo()
    {
        return (_userinfo != null);
    }

    /**
     * Tell whether or not the host part of this URI is hostname.
     * 
     * @return true iif the host part is hostname
     */
    public boolean isHostname()
    {
        return _is_hostname;
    }

    /**
     * Tell whether or not the host part of this URI is IPv4address.
     * 
     * @return true iif the host part is IPv4address
     */
    public boolean isIPv4address()
    {
        return _is_IPv4address;
    }

    /**
     * Tell whether or not the host part of this URI is IPv6reference.
     * 
     * @return true iif the host part is IPv6reference
     */
    public boolean isIPv6reference()
    {
        return _is_IPv6reference;
    }

    /**
     * Tell whether or not this URI has query.
     * 
     * @return true iif this URI has query
     */
    public boolean hasQuery()
    {
        return (_query != null);
    }

    /**
     * Tell whether or not this URI has fragment.
     * 
     * @return true iif this URI has fragment
     */
    public boolean hasFragment()
    {
        return (_fragment != null);
    }

    // ---------------------------------------------------------------- Charset

    /**
     * Set the default charset of the protocol.
     * <p>
     * The character set used to store files SHALL remain a local decision and
     * MAY depend on the capability of local operating systems. Prior to the
     * exchange of URIs they SHOULD be converted into a ISO/IEC 10646 format and
     * UTF-8 encoded. This approach, while allowing international exchange of
     * URIs, will still allow backward compatibility with older systems because
     * the code set positions for ASCII characters are identical to the one byte
     * sequence in UTF-8.
     * <p>
     * An individual URI scheme may require a single charset, define a default
     * charset, or provide a way to indicate the charset used.
     * 
     * @param charset
     *            the default charset for each protocol
     */
    public static void setProtocolCharset(String charset)
    {
        _protocolCharset = charset;
    }

    /**
     * Get the default charset of the protocol.
     * <p>
     * An individual URI scheme may require a single charset, define a default
     * charset, or provide a way to indicate the charset used.
     * <p>
     * To work globally either requires support of a number of character sets
     * and to be able to convert between them, or the use of a single preferred
     * character set. For support of global compatibility it is STRONGLY
     * RECOMMENDED that clients and servers use UTF-8 encoding when exchanging
     * URIs.
     * 
     * @return the charset string
     */
    public static String getProtocolCharset()
    {
        return _protocolCharset;
    }

    /**
     * Set the default charset of the document.
     * <p>
     * Notice that it will be possible to contain mixed characters (e.g.
     * ftp://host/KoreanNamespace/ChineseResource). To handle the Bi-directional
     * display of these character sets, the protocol charset could be simply
     * used again. Because it's not yet implemented that the insertion of BIDI
     * control characters at different points during composition is extracted.
     * 
     * @param charset
     *            the default charset for the document
     */
    public static void setDocumentCharset(String charset)
    {
        _documentCharset = charset;
    }

    /**
     * Get the default charset of the document.
     * 
     * @return the charset string
     */
    public static String getDocumentCharset()
    {
        return _documentCharset;
    }

    // ------------------------------------------------------------- The scheme

    /**
     * Get the scheme.
     * 
     * @return the scheme
     */
    public char[] getRawScheme()
    {
        return _scheme;
    }

    /**
     * Get the scheme.
     * 
     * @return the scheme null if undefined scheme
     */
    public String getScheme()
    {
        return (_scheme == null) ? null : new String(_scheme);
    }

    // ---------------------------------------------------------- The authority

    /**
     * Set the authority. It can be one type of server, hostport, hostname,
     * IPv4address, IPv6reference and reg_name.
     * <p>
     * <blockquote>
     * 
     * <pre>
     * authority = server | reg_name
     * </pre>
     * 
     * </blockquote>
     * <p>
     * 
     * @param the
     *            authority
     * @exception URIException
     */
    public void setAuthority(String a) throws URIException
    {
        parseAuthority(a, false);
        setUriReference();
    }

    /**
     * Get the raw-escaped authority.
     * 
     * @return the raw-escaped authority
     */
    public char[] getRawAuthority()
    {
        return _authority;
    }

    /**
     * Get the escaped authority.
     * 
     * @return the escaped authority
     */
    public String getEscapedAuthority()
    {
        return (_authority == null) ? null : new String(_authority);
    }

    /**
     * Get the authority.
     * 
     * @return the authority
     * @exception URIException
     * @see #decode
     */
    public String getAuthority() throws URIException
    {
        return (_authority == null) ? null : decode(_authority);
    }

    // ----------------------------------------------------------- The userinfo

    /**
     * Get the raw-escaped userinfo.
     * 
     * @return the raw-escaped userinfo
     * @see #getAuthority
     */
    public char[] getRawUserinfo()
    {
        return _userinfo;
    }

    /**
     * Get the escaped userinfo.
     * 
     * @return the escaped userinfo
     * @see #getAuthority
     */
    public String getEscapedUserinfo()
    {
        return (_userinfo == null) ? null : new String(_userinfo);
    }

    /**
     * Get the userinfo.
     * 
     * @return the userinfo
     * @exception URIException
     * @see #decode
     * @see #getAuthority
     */
    public String getUserinfo() throws URIException
    {
        return (_userinfo == null) ? null : decode(_userinfo);
    }

    // --------------------------------------------------------------- The host

    /**
     * Get the host.
     * <p>
     * <blockquote>
     * 
     * <pre>
     * host = hostname | IPv4address | IPv6reference
     * </pre>
     * 
     * </blockquote>
     * <p>
     * 
     * @return the host
     * @see #getAuthority
     */
    public char[] getRawHost()
    {
        return _host;
    }

    /**
     * Get the host.
     * <p>
     * <blockquote>
     * 
     * <pre>
     * host = hostname | IPv4address | IPv6reference
     * </pre>
     * 
     * </blockquote>
     * <p>
     * 
     * @return the host
     * @exception URIException
     * @see #decode
     * @see #getAuthority
     */
    public String getHost() throws URIException
    {
        return decode(_host);
    }

    // --------------------------------------------------------------- The port

    /**
     * Get the port. In order to get the specfic default port, the specific
     * protocol-supported class extended from the URI class should be used. It
     * has the server-based naming authority.
     * 
     * @return the port if -1, it has the default port for the scheme or the
     *         server-based naming authority is not supported in the specific
     *         URI.
     */
    public int getPort()
    {
        return _port;
    }

    // --------------------------------------------------------------- The path

    /**
     * Set the path. The method couldn't be used by API programmers.
     * 
     * @param p
     *            the path string
     * @exception URIException
     *                set incorrectly or fragment only
     * @see #encode
     */
    protected void setPath(String p) throws URIException
    {

        // set path
        if (_is_net_path || _is_abs_path)
        {
            _path = encode(p, allowed_abs_path);
        }
        else if (_is_rel_path)
        {
            StringBuffer buff = new StringBuffer(p.length());
            int at = p.indexOf('/');
            if (at > 0)
            { // never 0
                buff.append(encode(p.substring(0, at), allowed_rel_path));
                buff.append(encode(p.substring(at), allowed_abs_path));
            }
            else
            {
                buff.append(encode(p, allowed_rel_path));
            }
            _path = buff.toString().toCharArray();
        }
        else if (_is_opaque_part)
        {
            _opaque = encode(p, allowed_opaque_part);
        }
        else
        {
            throw new URIException(URIException.PARSING, "incorrect path");
        }
    }

    /**
     * Resolve the base and relative path.
     * 
     * @param base_path
     *            a character array of the base_path
     * @param rel_p
     *            a character array of the rel_path
     * @return the resolved path
     */
    protected char[] resolvePath(char[] base_path, char[] rel_p)
    {

        // REMINDME: paths are never null
        String base = (base_path == null) ? "" : new String(base_path);
        int at = base.lastIndexOf('/');
        if (at != -1)
        {
            base_path = base.substring(0, at + 1).toCharArray();
        }
        // _path could be empty
        if (rel_p == null || rel_p.length == 0)
        {
            return normalize(base_path);
        }
        else if (rel_p[0] == '/')
        {
            return rel_p;
        }
        else
        {
            StringBuffer buff = new StringBuffer(base.length() + rel_p.length);
            if (at != -1)
            {
                buff.append(base.substring(0, at + 1));
                buff.append(rel_p);
            }
            return normalize(buff.toString().toCharArray());
        }
    }

    /**
     * Get the raw-escaped current hierarchy level in the given path. If the
     * last namespace is a collection, the slash mark ('/') should be ended with
     * at the last character of the path string.
     * 
     * @param p
     *            the path
     * @return the current hierarchy level
     * @exception URIException
     *                no hierarchy level
     */
    protected char[] getRawCurrentHierPath(char[] p) throws URIException
    {

        if (_is_opaque_part)
        {
            throw new URIException(URIException.PARSING, "no hierarchy level");
        }
        if (p == null)
        {
            throw new URIException(URIException.PARSING, "emtpy path");
        }
        String buff = new String(p);
        int first = buff.indexOf('/');
        int last = buff.lastIndexOf('/');
        if (last == 0)
        {
            return rootPath;
        }
        else if (first != last && last != -1)
        {
            return buff.substring(0, last).toCharArray();
        }
        // FIXME: it could be a document on the server side
        return p;
    }

    /**
     * Get the raw-escaped current hierarchy level.
     * 
     * @return the raw-escaped current hierarchy level
     * @exception URIException
     *                no hierarchy level
     */
    public char[] getRawCurrentHierPath() throws URIException
    {
        return (_path == null) ? null : getRawCurrentHierPath(_path);
    }

    /**
     * Get the escaped current hierarchy level.
     * 
     * @return the escaped current hierarchy level
     * @exception URIException
     *                no hierarchy level
     */
    public String getEscapedCurrentHierPath() throws URIException
    {
        char[] p = getRawCurrentHierPath();
        return (p == null) ? null : new String(p);
    }

    /**
     * Get the current hierarchy level.
     * 
     * @return the current hierarchy level
     * @exception URIException
     * @see #decode
     */
    public String getCurrentHierPath() throws URIException
    {
        char[] p = getRawCurrentHierPath();
        return (p == null) ? null : decode(p);
    }

    /**
     * Get the level above the this hierarchy level.
     * 
     * @return the raw above hierarchy level
     * @exception URIException
     */
    public char[] getRawAboveHierPath() throws URIException
    {
        char[] p = getRawCurrentHierPath();
        return (p == null) ? null : getRawCurrentHierPath(p);
    }

    /**
     * Get the level above the this hierarchy level.
     * 
     * @return the raw above hierarchy level
     * @exception URIException
     */
    public String getEscapedAboveHierPath() throws URIException
    {
        char[] p = getRawAboveHierPath();
        return (p == null) ? null : new String(p);
    }

    /**
     * Get the level above the this hierarchy level.
     * 
     * @return the above hierarchy level
     * @exception URIException
     * @see #decode
     */
    public String getAboveHierPath() throws URIException
    {
        char[] o = getRawAboveHierPath();
        return (o == null) ? null : decode(o);
    }

    /**
     * Get the raw-escaped path.
     * <p>
     * <blockquote>
     * 
     * <pre>
     * 
     *  
     *     path          = [ abs_path | opaque_part ]
     *   
     *  
     * </pre>
     * 
     * </blockquote>
     * <p>
     * 
     * @return the raw-escaped path
     */
    public char[] getRawPath()
    {
        return _is_opaque_part ? _opaque : _path;
    }

    /**
     * Get the escaped path.
     * <p>
     * <blockquote>
     * 
     * <pre>
     * 
     *  
     *     path          = [ abs_path | opaque_part ]
     *     abs_path      = &quot;/&quot;  path_segments 
     *     opaque_part   = uric_no_slash *uric
     *   
     *  
     * </pre>
     * 
     * </blockquote>
     * <p>
     * 
     * @return the escaped path string
     */
    public String getEscapedPath()
    {
        char[] p = getRawPath();
        return (p == null) ? null : new String(p);
    }

    /**
     * Get the path.
     * <p>
     * <blockquote>
     * 
     * <pre>
     * 
     *  
     *     path          = [ abs_path | opaque_part ]
     *   
     *  
     * </pre>
     * 
     * </blockquote>
     * <p>
     * 
     * @return the path string
     * @exception URIException
     * @see #decode
     */
    public String getPath() throws URIException
    {
        char[] p = getRawPath();
        return (p == null) ? null : decode(p);
    }

    /**
     * Get the raw-escaped basename of the path.
     * 
     * @return the raw-escaped basename
     */
    public char[] getRawName()
    {
        if (_path == null)
            return null;

        int at = 0;
        for (int i = _path.length - 1; i >= 0; i--)
        {
            if (_path[i] == '/')
            {
                at = i + 1;
                break;
            }
        }
        int len = _path.length - at;
        char[] basename = new char[len];
        System.arraycopy(_path, at, basename, 0, len);
        return basename;
    }

    /**
     * Get the escaped basename of the path.
     * 
     * @return the escaped basename string
     */
    public String getEscapedName()
    {
        char[] basename = getRawName();
        return (basename == null) ? null : new String(basename);
    }

    /**
     * Get the basename of the path.
     * 
     * @return the basename string
     * @exception URIException
     *                incomplete trailing escape pattern Or unsupported
     *                character encoding
     * @see #decode
     */
    public String getName() throws URIException
    {
        char[] basename = getRawName();
        return (basename == null) ? null : decode(getRawName());
    }

    // ----------------------------------------------------- The path and query

    /**
     * Get the raw-escaped path and query.
     * 
     * @return the raw-escaped path and query
     */
    public char[] getRawPathQuery()
    {

        if (_path == null && _query == null)
        {
            return null;
        }
        StringBuffer buff = new StringBuffer();
        if (_path != null)
        {
            buff.append(_path);
        }
        if (_query != null)
        {
            buff.append('?');
            buff.append(_query);
        }
        return buff.toString().toCharArray();
    }

    /**
     * Get the escaped query.
     * 
     * @return the escaped path and query string
     */
    public String getEscapedPathQuery()
    {
        char[] rawPathQuery = getRawPathQuery();
        return (rawPathQuery == null) ? null : new String(rawPathQuery);
    }

    /**
     * Get the path and query.
     * 
     * @return the path and query string.
     * @exception URIException
     *                incomplete trailing escape pattern Or unsupported
     *                character encoding
     * @see #decode
     */
    public String getPathQuery() throws URIException
    {
        char[] rawPathQuery = getRawPathQuery();
        return (rawPathQuery == null) ? null : decode(rawPathQuery);
    }

    // -------------------------------------------------------------- The query

    /**
     * Set the query.
     * 
     * @param q
     *            the query string.
     * @exception URIException
     *                incomplete trailing escape pattern Or unsupported
     *                character encoding
     * @throws NullPointerException
     *             null query
     * @see #encode
     */
    public void setQuery(String q) throws URIException
    {
        _query = encode(q, allowed_query);
        setUriReference();
    }

    /**
     * Get the raw-escaped query.
     * 
     * @return the raw-escaped query
     */
    public char[] getRawQuery()
    {
        return _query;
    }

    /**
     * Get the escaped query.
     * 
     * @return the escaped query string
     */
    public String getEscapedQuery()
    {
        return (_query == null) ? null : new String(_query);
    }

    /**
     * Get the query.
     * 
     * @return the query string.
     * @exception URIException
     *                incomplete trailing escape pattern Or unsupported
     *                character encoding
     * @see #decode
     */
    public String getQuery() throws URIException
    {
        return (_query == null) ? null : decode(_query);
    }

    // ----------------------------------------------------------- The fragment

    /**
     * Set the fragment.
     * <p>
     * An empty URI reference represents the base URI of the current document
     * and should be replaced by that URI when transformed into a request.
     * 
     * @param the
     *            fragment string.
     * @exception URIException
     *                Or unsupported character encoding
     * @throws NullPointerException
     *             null fragment
     */
    public void setFragment(String f) throws URIException
    {
        _fragment = encode(f, allowed_fragment);
        setUriReference();
    }

    /**
     * Get the raw-escaped fragment.
     * <p>
     * The optional fragment identifier is not part of a URI, but is often used
     * in conjunction with a URI.
     * <p>
     * The format and interpretation of fragment identifiers is dependent on the
     * media type [RFC2046] of the retrieval result.
     * <p>
     * A fragment identifier is only meaningful when a URI reference is intended
     * for retrieval and the result of that retrieval is a document for which
     * the identified fragment is consistently defined.
     * 
     * @return the raw-escaped fragment
     */
    public char[] getRawFragment()
    {
        return _fragment;
    }

    /**
     * Get the escaped fragment.
     * 
     * @return the escaped fragment string
     */
    public String getEscapedFragment()
    {
        return (_fragment == null) ? null : new String(_fragment);
    }

    /**
     * Get the fragment.
     * 
     * @return the fragment string
     * @exception URIException
     *                incomplete trailing escape pattern Or unsupported
     *                character encoding
     * @see #decode
     */
    public String getFragment() throws URIException
    {
        return (_fragment == null) ? null : decode(_fragment);
    }

    // ------------------------------------------------------------- Utilities

    /**
     * Normalize the given hier path part.
     * 
     * @param p
     *            the path to normalize
     * @return the normalized path
     */
    protected char[] normalize(char[] p)
    {

        if (p == null)
            return null;

        String normalized = new String(p);
        boolean endsWithSlash = true;
        // precondition
        if (!normalized.endsWith("/"))
        {
            normalized += '/';
            endsWithSlash = false;
        }
        if (normalized.endsWith("/./") || normalized.endsWith("/../"))
        {
            endsWithSlash = true;
        }
        // Resolve occurrences of "/./" in the normalized path
        while (true)
        {
            int at = normalized.indexOf("/./");
            if (at == -1)
            {
                break;
            }
            normalized = normalized.substring(0, at)
                + normalized.substring(at + 2);
        }
        // Resolve occurrences of "/../" in the normalized path
        while (true)
        {
            int at = normalized.indexOf("/../");
            if (at == -1)
            {
                break;
            }
            if (at == 0)
            {
                normalized = "/";
                break;
            }
            int backward = normalized.lastIndexOf('/', at - 1);
            if (backward == -1)
            {
                // consider the rel_path
                normalized = normalized.substring(at + 4);
            }
            else
            {
                normalized = normalized.substring(0, backward)
                    + normalized.substring(at + 3);
            }
        }
        // Resolve occurrences of "//" in the normalized path
        while (true)
        {
            int at = normalized.indexOf("//");
            if (at == -1)
            {
                break;
            }
            normalized = normalized.substring(0, at)
                + normalized.substring(at + 1);
        }
        if (!endsWithSlash && normalized.endsWith("/"))
        {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        else if (endsWithSlash && !normalized.endsWith("/"))
        {
            normalized = normalized + "/";
        }
        // Set the normalized path that we have completed
        return normalized.toCharArray();
    }

    /**
     * Normalize the path part of this URI.
     */
    public void normalize()
    {
        _path = normalize(_path);
    }

    /**
     * Set debug mode
     * 
     * @param level
     *            the level of debug mode
     */
    public void setDebug(int level)
    {
        debug = level;
    }

    /**
     * Test if the first array is equal to the second array.
     * 
     * @param first
     *            the first character array
     * @param second
     *            the second character array
     * @return true if they're equal
     */
    protected boolean equals(char[] first, char[] second)
    {

        if (first == null && second == null)
        {
            return true;
        }
        if (first == null || second == null)
        {
            return false;
        }
        if (first.length != second.length)
        {
            return false;
        }
        for (int i = 0; i < first.length; i++)
        {
            if (first[i] != second[i])
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Test an object if this URI is equal to another.
     * 
     * @param obj
     *            an object to compare
     * @return true if two URI objects are equal
     */
    public boolean equals(Object obj)
    {

        // normalize and test each components
        if (obj == this)
        {
            return true;
        }
        if (!(obj instanceof URI))
        {
            return false;
        }
        URI another = (URI)obj;
        // scheme
        if (!equals(_scheme, another._scheme))
        {
            return false;
        }
        // is_opaque_part or is_hier_part? and opaque
        if (!equals(_opaque, another._opaque))
        {
            return false;
        }
        // is_hier_part
        // has_authority
        if (!equals(_authority, another._authority))
        {
            return false;
        }
        // path
        if (!equals(_path, another._path))
        {
            return false;
        }
        // has_query
        if (!equals(_query, another._query))
        {
            return false;
        }
        // has_fragment? should be careful of the only fragment case.
        if (!equals(_fragment, another._fragment))
        {
            return false;
        }
        return true;
    }

    // ---------------------------------------------------------- Serialization

    /**
     * Write the content of this URI.
     * 
     * @param oos
     *            the object-output stream
     */
    protected void writeObject(java.io.ObjectOutputStream oos)
        throws IOException
    {

        oos.defaultWriteObject();
    }

    /**
     * Read a URI.
     * 
     * @param ois
     *            the object-input stream
     */
    protected void readObject(java.io.ObjectInputStream ois)
        throws ClassNotFoundException,
            IOException
    {

        ois.defaultReadObject();
    }

    // ------------------------------------------------------------- Comparison

    /**
     * Compare this URI to another object.
     * 
     * @param obj
     *            the object to be compared.
     * @return 0, if it's same, -1, if failed, first being compared with in the
     *         authority component
     * @exception ClassCastException
     *                not URI argument
     * @throws NullPointerException
     *             null object
     */
    public int compareTo(Object obj)
    {

        URI another = (URI)obj;
        if (!equals(_authority, another.getRawAuthority()))
            return -1;
        return toString().compareTo(another.toString());
    }

    // ------------------------------------------------------------------ Clone

    /**
     * Create and return a copy of this object, the URI-reference containing the
     * userinfo component. Notice that the whole URI-reference including the
     * userinfo component counld not be gotten as a <code>String</code>.
     * <p>
     * To copy the identical <code>URI</code> object including the userinfo
     * component, it should be used.
     * 
     * @return a clone of this instance
     */
    public synchronized Object clone()
    {

        URI instance = new URI();

        instance._uri = _uri;
        instance._scheme = _scheme;
        instance._opaque = _opaque;
        instance._authority = _authority;
        instance._userinfo = _userinfo;
        instance._host = _host;
        instance._port = _port;
        instance._path = _path;
        instance._query = _query;
        instance._fragment = _fragment;
        // flags
        instance._is_hier_part = _is_hier_part;
        instance._is_opaque_part = _is_opaque_part;
        instance._is_net_path = _is_net_path;
        instance._is_abs_path = _is_abs_path;
        instance._is_rel_path = _is_rel_path;
        instance._is_reg_name = _is_reg_name;
        instance._is_server = _is_server;
        instance._is_hostname = _is_hostname;
        instance._is_IPv4address = _is_IPv4address;
        instance._is_IPv6reference = _is_IPv6reference;

        return instance;
    }

    // ------------------------------------------------------------ Get the URI

    /**
     * It can be gotten the URI character sequence. It's raw-escaped. For the
     * purpose of the protocol to be transported, it will be useful.
     * <p>
     * It is clearly unwise to use a URL that contains a password which is
     * intended to be secret. In particular, the use of a password within the
     * 'userinfo' component of a URL is strongly disrecommended except in those
     * rare cases where the 'password' parameter is intended to be public.
     * <p>
     * When you want to get each part of the userinfo, you need to use the
     * specific methods in the specific URL. It depends on the specific URL.
     * 
     * @return URI character sequence
     */
    public char[] getRawURI()
    {
        return _uri;
    }

    /**
     * It can be gotten the URI character sequence. It's escaped. For the
     * purpose of the protocol to be transported, it will be useful.
     * 
     * @return the URI string
     */
    public String getEscapedURI()
    {
        return (_uri == null) ? null : new String(_uri);
    }

    /**
     * It can be gotten the URI character sequence.
     * 
     * @return the URI string
     * @exception URIException
     *                incomplete trailing escape pattern Or unsupported
     *                character encoding
     * @see #decode
     */
    public String getURI() throws URIException
    {
        return (_uri == null) ? null : decode(_uri);
    }

    /**
     * Get the escaped URI string.
     * <p>
     * On the document, the URI-reference form is only used without the userinfo
     * component like http://jakarta.apache.org/ by the security reason. But the
     * URI-reference form with the userinfo component could be parsed.
     * <p>
     * In other words, this URI and any its subclasses must not expose the
     * URI-reference expression with the userinfo component like
     * http://user:password@hostport/restricted_zone. <br>
     * It means that the API client programmer should extract each user and
     * password to access manually. Probably it will be supported in the each
     * subclass, however, not a whole URI-reference expression.
     * 
     * @return the URI string
     * @see #clone()
     */
    public String toString()
    {
        return getEscapedURI();
    }

    // ------------------------------------------------------------ Inner class

    /**
     * A mapping to determine the (somewhat arbitrarily) preferred charset for a
     * given locale. Supports all locales recognized in JDK 1.1.
     * <p>
     * The distribution of this class is Servlets.com. It was originally written
     * by Jason Hunter [jhunter@acm.org] and used by the Jakarta commons
     * HttpClient and Slide project with permission.
     */
    public static class LocaleToCharsetMap
    {

        private static Hashtable map;
        static
        {
            map = new Hashtable();
            map.put("ar", "ISO-8859-6");
            map.put("be", "ISO-8859-5");
            map.put("bg", "ISO-8859-5");
            map.put("ca", "ISO-8859-1");
            map.put("cs", "ISO-8859-2");
            map.put("da", "ISO-8859-1");
            map.put("de", "ISO-8859-1");
            map.put("el", "ISO-8859-7");
            map.put("en", "ISO-8859-1");
            map.put("es", "ISO-8859-1");
            map.put("et", "ISO-8859-1");
            map.put("fi", "ISO-8859-1");
            map.put("fr", "ISO-8859-1");
            map.put("hr", "ISO-8859-2");
            map.put("hu", "ISO-8859-2");
            map.put("is", "ISO-8859-1");
            map.put("it", "ISO-8859-1");
            map.put("iw", "ISO-8859-8");
            map.put("ja", "Shift_JIS");
            map.put("ko", "EUC-KR");
            map.put("lt", "ISO-8859-2");
            map.put("lv", "ISO-8859-2");
            map.put("mk", "ISO-8859-5");
            map.put("nl", "ISO-8859-1");
            map.put("no", "ISO-8859-1");
            map.put("pl", "ISO-8859-2");
            map.put("pt", "ISO-8859-1");
            map.put("ro", "ISO-8859-2");
            map.put("ru", "ISO-8859-5");
            map.put("sh", "ISO-8859-5");
            map.put("sk", "ISO-8859-2");
            map.put("sl", "ISO-8859-2");
            map.put("sq", "ISO-8859-2");
            map.put("sr", "ISO-8859-5");
            map.put("sv", "ISO-8859-1");
            map.put("tr", "ISO-8859-9");
            map.put("uk", "ISO-8859-5");
            map.put("zh", "GB2312");
            map.put("zh_TW", "Big5");
        }

        /**
         * Get the preferred charset for the given locale.
         * 
         * @param locale
         *            the locale
         * @return the preferred charset or null if the locale is not recognized
         */
        public static String getCharset(Locale locale)
        {
            // try for an full name match (may include country)
            String charset = (String)map.get(locale.toString());
            if (charset != null)
                return charset;

            // if a full name didn't match, try just the language
            charset = (String)map.get(locale.getLanguage());
            return charset; // may be null
        }

    }

}
