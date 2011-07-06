/*
 * ====================================================================
 * 
 * Copyright 2002-2004 The Apache Software Foundation
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.oaklandsw.utillog.Log;

import com.oaklandsw.http.cookie.CookieSpec;
import com.oaklandsw.http.cookie.CookieSpecBase;
import com.oaklandsw.http.cookie.NetscapeDraftSpec;
import com.oaklandsw.http.cookie.RFC2109Spec;
import com.oaklandsw.util.LogUtils;

/**
 * Cookie management policy class. The cookie policy provides corresponding
 * cookie management interfrace for a given type or version of cookie.
 * <p>
 * RFC 2109 specification is used per default. Other supported specification can
 * be chosen when appropriate or set default when desired
 * <p>
 * The following specifications are provided:
 * <ul>
 * <li><tt>BROWSER_COMPATIBILITY</tt>: compatible with the common cookie
 * management practices (even if they are not 100% standards compliant)
 * <li><tt>NETSCAPE</tt>: Netscape cookie draft compliant
 * <li><tt>RFC_2109</tt>: RFC2109 compliant (default)
 * <li><tt>IGNORE_COOKIES</tt>: do not automcatically process cookies
 * </ul>
 * 
 * @author <a href="mailto:oleg@ural.ru">Oleg Kalnichevski</a>
 * @author <a href="mailto:mbowler@GargoyleSoftware.com">Mike Bowler</a>
 * 
 * @since 2.0
 */
public abstract class CookiePolicy
{
    private static final Log                _log                  = LogUtils.makeLogger();

    private static Map         SPECS                 = Collections
                                                             .synchronizedMap(new HashMap());

    /**
     * The policy that provides high degree of compatibility with common cookie
     * management of popular HTTP agents.
     * 
     * @since 3.0
     */
    public static final String BROWSER_COMPATIBILITY = "compatibility";

    /**
     * The Netscape cookie draft compliant policy.
     * 
     * @since 3.0
     */
    public static final String NETSCAPE              = "netscape";

    /**
     * The RFC 2109 compliant policy.
     * 
     * @since 3.0
     */
    public static final String RFC_2109              = "rfc2109";

    /**
     * The policy that ignores cookies.
     * 
     * @since 3.0
     */
    public static final String IGNORE_COOKIES        = "ignoreCookies";

    /**
     * The default cookie policy.
     * 
     * @since 3.0
     */
    public static final String DEFAULT               = "default";

    static
    {
        CookiePolicy.registerCookieSpec(DEFAULT, RFC2109Spec.class);
        CookiePolicy.registerCookieSpec(RFC_2109, RFC2109Spec.class);
        CookiePolicy.registerCookieSpec(BROWSER_COMPATIBILITY,
                                        CookieSpecBase.class);
        CookiePolicy.registerCookieSpec(NETSCAPE, NetscapeDraftSpec.class);
    }

    /**
     * Registers a new {@link CookieSpec cookie specification} with the given
     * identifier. If a specification with the given ID already exists it will
     * be overridden. This ID is the same one used to retrieve the
     * {@link CookieSpec cookie specification} from
     * {@link #getCookieSpec(String)}.
     * 
     * @param id
     *            the identifier for this specification
     * @param clazz
     *            the {@link CookieSpec cookie specification} class to register
     * 
     * @see #getCookieSpec(String)
     * 
     * @since 3.0
     */
    public static void registerCookieSpec(final String id, final Class clazz)
    {
        if (id == null)
        {
            throw new IllegalArgumentException("Id may not be null");
        }
        if (clazz == null)
        {
            throw new IllegalArgumentException("Cookie spec class may not be null");
        }
        SPECS.put(id.toLowerCase(), clazz);
    }

    /**
     * Unregisters the {@link CookieSpec cookie specification} with the given
     * ID.
     * 
     * @param id
     *            the ID of the {@link CookieSpec cookie specification} to
     *            unregister
     * 
     * @since 3.0
     */
    public static void unregisterCookieSpec(final String id)
    {
        if (id == null)
        {
            throw new IllegalArgumentException("Id may not be null");
        }
        SPECS.remove(id.toLowerCase());
    }

    /**
     * Gets the {@link CookieSpec cookie specification} with the given ID.
     * 
     * @param id
     *            the {@link CookieSpec cookie specification} ID
     * 
     * @return {@link CookieSpec cookie specification}
     * 
     * @throws IllegalStateException
     *             if a policy with the ID cannot be found
     * 
     * @since 3.0
     */
    public static CookieSpec getCookieSpec(final String id)
        throws IllegalStateException
    {

        if (id == null)
            return getDefaultSpec();

        Class clazz = (Class)SPECS.get(id.toLowerCase());

        if (clazz != null)
        {
            try
            {
                CookieSpec cs = (CookieSpec)clazz.newInstance();
                cs.setPolicyName(id);
                return cs;
            }
            catch (Exception e)
            {
                _log.error("Error initializing cookie spec: " + id, e);
                throw new IllegalStateException(id
                    + " cookie spec implemented by "
                    + clazz.getName()
                    + " could not be initialized");
            }
        }
        throw new IllegalStateException("Unsupported cookie policy: " + id);
    }

    /**
     * Returns {@link CookieSpec cookie specification} registered as
     * {@link #DEFAULT}. If no default {@link CookieSpec cookie specification}
     * has been registered, {@link RFC2109Spec RFC2109 specification} is
     * returned.
     * 
     * @return default {@link CookieSpec cookie specification}
     * 
     * @see #DEFAULT
     */
    public static CookieSpec getDefaultSpec()
    {
        try
        {
            return getCookieSpec(DEFAULT);
        }
        catch (IllegalStateException e)
        {
            _log.warn("Default cookie policy is not registered");
            return new RFC2109Spec();
        }
    }

}
