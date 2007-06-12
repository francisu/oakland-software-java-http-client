/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//httpclient/src/java/org/apache/commons/httpclient/HeaderElement.java,v
 * 1.23 2004/05/13 04:03:25 mbecke Exp $ $Revision: 155418 $ $Date: 2005-02-26
 * 05:01:52 -0800 (Sat, 26 Feb 2005) $
 * 
 * ====================================================================
 * 
 * Copyright 1999-2004 The Apache Software Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many individuals on
 * behalf of the Apache Software Foundation. For more information on the Apache
 * Software Foundation, please see <http://www.apache.org/>.
 * 
 */

package com.oaklandsw.http;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * One element of an HTTP header's value.
 * </p>
 * <p>
 * Some HTTP headers (such as the set-cookie header) have values that can be
 * decomposed into multiple elements. Such headers must be in the following
 * form:
 * </p>
 * 
 * <pre>
 *  header  = [ element ] *( &quot;,&quot; [ element ] )
 *  element = name [ &quot;=&quot; [ value ] ] *( &quot;;&quot; [ param ] )
 *  param   = name [ &quot;=&quot; [ value ] ]
 * 
 *  name    = token
 *  value   = ( token | quoted-string )
 * 
 *  token         = 1*&lt;any char except &quot;=&quot;, &quot;,&quot;, &quot;;&quot;, &lt;&quot;&gt; and
 *                        white space&gt;
 *  quoted-string = &lt;&quot;&gt; *( text | quoted-char ) &lt;&quot;&gt;
 *  text          = any char except &lt;&quot;&gt;
 *  quoted-char   = &quot;\&quot; char
 * </pre>
 * 
 * <p>
 * Any amount of white space is allowed between any part of the header, element
 * or param and is ignored. A missing value in any element or param will be
 * stored as the empty {@link String}; if the "=" is also missing <var>null</var>
 * will be stored instead.
 * </p>
 * <p>
 * This class represents an individual header element, containing both a
 * name/value pair (value may be <tt>null</tt>) and optionally a set of
 * additional parameters.
 * </p>
 * <p>
 * This class also exposes a {@link #parse} method for parsing a {@link Header}
 * value into an array of elements.
 * </p>
 * 
 * @see Header
 * 
 * @author <a href="mailto:bcholmes@interlog.com">B.C. Holmes</a>
 * @author <a href="mailto:jericho@thinkfree.com">Park, Sung-Gu</a>
 * @author <a href="mailto:mbowler@GargoyleSoftware.com">Mike Bowler</a>
 * @author <a href="mailto:oleg@ural.com">Oleg Kalnichevski</a>
 * 
 * @since 1.0
 * @version $Revision: 155418 $ $Date: 2005-02-26 05:01:52 -0800 (Sat, 26 Feb
 *          2005) $
 */
public class HeaderElement extends NameValuePair
{

    // ----------------------------------------------------------- Constructors

    /**
     * Default constructor.
     */
    public HeaderElement()
    {
        this(null, null, null);
    }

    /**
     * Constructor.
     * 
     * @param name
     *            my name
     * @param value
     *            my (possibly <tt>null</tt>) value
     */
    public HeaderElement(String name, String value)
    {
        this(name, value, null);
    }

    /**
     * Constructor with name, value and parameters.
     * 
     * @param name
     *            my name
     * @param value
     *            my (possibly <tt>null</tt>) value
     * @param parameters1
     *            my (possibly <tt>null</tt>) parameters
     */
    public HeaderElement(String name, String value, NameValuePair[] parameters1)
    {
        super(name, value);
        this.parameters = parameters1;
    }

    /**
     * Constructor with array of characters.
     * 
     * @param chars
     *            the array of characters
     * @param offset -
     *            the initial offset.
     * @param length -
     *            the length.
     * 
     * @since 3.0
     */
    public HeaderElement(char[] chars, int offset, int length)
    {
        this();
        if (chars == null)
        {
            return;
        }
        ParameterParser parser = new ParameterParser();
        List params = parser.parse(chars, offset, length, ';');
        if (params.size() > 0)
        {
            NameValuePair element = (NameValuePair)params.remove(0);
            setName(element.getName());
            setValue(element.getValue());
            if (params.size() > 0)
            {
                this.parameters = (NameValuePair[])params
                        .toArray(new NameValuePair[params.size()]);
            }
        }
    }

    /**
     * Constructor with array of characters.
     * 
     * @param chars
     *            the array of characters
     * 
     * @since 3.0
     */
    public HeaderElement(char[] chars)
    {
        this(chars, 0, chars.length);
    }

    // -------------------------------------------------------- Constants

    // ----------------------------------------------------- Instance Variables

    /** My parameters, if any. */
    private NameValuePair[]  parameters = null;

    // ------------------------------------------------------------- Properties

    /**
     * Get parameters, if any.
     * 
     * @since 2.0
     * @return parameters as an array of {@link NameValuePair}s
     */
    public NameValuePair[] getParameters()
    {
        return this.parameters;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * This parses the value part of a header. The result is an array of
     * HeaderElement objects.
     * 
     * @param headerValue
     *            the array of char representation of the header value (as
     *            received from the web server).
     * @return array of {@link HeaderElement}s.
     * 
     * @since 3.0
     */
    public static final HeaderElement[] parseElements(char[] headerValue)
    {

        if (headerValue == null)
        {
            return new HeaderElement[] {};
        }
        List elements = new ArrayList();

        int i = 0;
        int from = 0;
        int len = headerValue.length;
        boolean qouted = false;
        while (i < len)
        {
            char ch = headerValue[i];
            if (ch == '"')
            {
                qouted = !qouted;
            }
            HeaderElement element = null;
            if ((!qouted) && (ch == ','))
            {
                element = new HeaderElement(headerValue, from, i);
                from = i + 1;
            }
            else if (i == len - 1)
            {
                element = new HeaderElement(headerValue, from, len);
            }
            if ((element != null) && (element.getName() != null))
            {
                elements.add(element);
            }
            i++;
        }
        return (HeaderElement[])elements.toArray(new HeaderElement[elements
                .size()]);
    }

    /**
     * This parses the value part of a header. The result is an array of
     * HeaderElement objects.
     * 
     * @param headerValue
     *            the string representation of the header value (as received
     *            from the web server).
     * @return array of {@link HeaderElement}s.
     * 
     * @since 3.0
     */
    public static final HeaderElement[] parseElements(String headerValue)
    {

        if (headerValue == null)
        {
            return new HeaderElement[] {};
        }
        return parseElements(headerValue.toCharArray());
    }

    /**
     * Returns parameter with the given name, if found. Otherwise null is
     * returned
     * 
     * @param name
     *            The name to search by.
     * @return NameValuePair parameter with the given name
     */

    public NameValuePair getParameterByName(String name)
    {
        if (name == null)
        {
            throw new IllegalArgumentException("Name may not be null");
        }
        NameValuePair found = null;
        NameValuePair parameters1[] = getParameters();
        if (parameters1 != null)
        {
            for (int i = 0; i < parameters1.length; i++)
            {
                NameValuePair current = parameters1[i];
                if (current.getName().equalsIgnoreCase(name))
                {
                    found = current;
                    break;
                }
            }
        }
        return found;
    }

}
