/*
 * Copyright 2006 oakland software, incorporated. All rights Reserved.
 */

/*
 * Portions of this code:
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
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.oaklandsw.util.Log;

import com.oaklandsw.util.LogUtils;

/**
 * Holds a set of cookies.
 */
public class CookieContainer implements Collection
{
    private static final Log _log     = LogUtils.makeLogger();
    private List             _cookies = new ArrayList();

    public CookieContainer()
    {
        super();
    }

    // ------------------------------------------------------------- Properties

    /**
     * Adds an {@link Cookie HTTP cookie}, replacing any existing equivalent
     * cookies. If the given cookie has already expired it will not be added,
     * but existing values will still be removed.
     * 
     * @param cookie
     *            the {@link Cookie cookie} to be added
     * 
     * @see #addCookies(Cookie[])
     * 
     */
    public synchronized void addCookie(Cookie cookie)
    {
        if (_log.isDebugEnabled())
            _log.trace("addCookie " + cookie);

        if (cookie != null)
        {
            // first remove any old cookie that is equivalent
            for (Iterator it = _cookies.iterator(); it.hasNext();)
            {
                Cookie tmp = (Cookie)it.next();
                if (cookie.equals(tmp))
                {
                    it.remove();
                    break;
                }
            }
            if (!cookie.isExpired())
            {
                _cookies.add(cookie);
            }
            else
            {
                _log.debug("addCookie - not added - expired");
            }
        }
    }

    /**
     * Adds an array of {@link Cookie HTTP cookies}. Cookies are added
     * individually and in the given array order. If any of the given cookies
     * has already expired it will not be added, but existing values will still
     * be removed.
     * 
     * @param cookies
     *            the {@link Cookie cookies} to be added
     * 
     * @see #addCookie(Cookie)
     * 
     * 
     */
    public synchronized void addCookies(Cookie[] cookies)
    {
        if (cookies != null)
        {
            for (int i = 0; i < cookies.length; i++)
            {
                this.addCookie(cookies[i]);
            }
        }
    }

    /**
     * Returns an array of {@link Cookie cookies} that this HTTP state currently
     * contains.
     * 
     * @return an array of {@link Cookie cookies}.
     * 
     */
    public synchronized Cookie[] getCookies()
    {
        return (Cookie[])(_cookies.toArray(new Cookie[_cookies.size()]));
    }

    /**
     * Removes all of {@link Cookie cookies} in this HTTP state that have
     * expired according to the current system time.
     * 
     * @see #purgeExpiredCookies(java.util.Date)
     * 
     */
    public synchronized boolean purgeExpiredCookies()
    {
        return purgeExpiredCookies(new Date());
    }

    /**
     * Removes all of {@link Cookie cookies} in this HTTP state that have
     * expired by the specified {@link java.util.Date date}.
     * 
     * @param date
     *            The {@link java.util.Date date} to compare against.
     * 
     * @return true if any cookies were purged.
     * 
     * @see Cookie#isExpired(java.util.Date)
     * 
     * @see #purgeExpiredCookies()
     */
    public synchronized boolean purgeExpiredCookies(Date date)
    {
        boolean removed = false;
        Iterator it = _cookies.iterator();
        while (it.hasNext())
        {
            if (((Cookie)(it.next())).isExpired(date))
            {
                it.remove();
                removed = true;
            }
        }
        return removed;
    }

    /**
     * Returns a string representation of the cookies.
     * 
     * @param cookies
     *            The cookies
     * @return The string representation.
     */
    private static String getCookiesStringRepresentation(final List cookies)
    {
        StringBuffer sbResult = new StringBuffer();
        Iterator iter = cookies.iterator();
        while (iter.hasNext())
        {
            Cookie ck = (Cookie)iter.next();
            if (sbResult.length() > 0)
            {
                sbResult.append("#");
            }
            sbResult.append(ck.toExternalForm());
        }
        return sbResult.toString();
    }

    public synchronized int size()
    {
        return _cookies.size();
    }

    public synchronized void clear()
    {
        _cookies.clear();
    }

    public synchronized boolean isEmpty()
    {
        return _cookies.isEmpty();
    }

    public synchronized Object[] toArray()
    {
        return _cookies.toArray();
    }

    public synchronized boolean add(Object o)
    {
        addCookie((Cookie)o);
        return true;
    }

    public synchronized boolean contains(Object o)
    {
        return _cookies.contains(o);
    }

    public synchronized boolean remove(Object o)
    {
        return _cookies.remove(o);
    }

    public synchronized boolean addAll(Collection c)
    {
        Iterator it = c.iterator();
        while (it.hasNext())
        {
            Cookie cookie = (Cookie)it.next();
            addCookie(cookie);
        }
        return true;
    }

    public synchronized boolean containsAll(Collection c)
    {
        return _cookies.containsAll(c);
    }

    public synchronized boolean removeAll(Collection c)
    {
        return _cookies.removeAll(c);
    }

    public synchronized boolean retainAll(Collection c)
    {
        return _cookies.retainAll(c);
    }

    public synchronized Iterator iterator()
    {
        return _cookies.iterator();
    }

    public synchronized Object[] toArray(Object[] a)
    {
        return _cookies.toArray(a);
    }

    public String toString()
    {
        return _cookies.toString();
    }

}
