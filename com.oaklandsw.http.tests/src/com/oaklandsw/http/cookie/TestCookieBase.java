/*
 * $Header: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//httpclient/src/test/org/apache/commons/httpclient/cookie/TestCookieBase.java,v 1.1 2004/04/24 19:39:24 olegk Exp $
 * $Revision: 155418 $
 * $Date: 2005-02-26 05:01:52 -0800 (Sat, 26 Feb 2005) $
 * ====================================================================
 *
 *  Copyright 1999-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package com.oaklandsw.http.cookie;

import com.oaklandsw.http.Cookie;
import com.oaklandsw.http.HttpTestBase;

/**
 * Test cases for Cookie
 *
 * @author <a href="mailto:oleg@ural.ru">Oleg Kalnichevski</a>
 * @version $Revision: 155418 $
 */
public class TestCookieBase extends HttpTestBase {

    // ------------------------------------------------------------ Constructor

    public TestCookieBase(String name) {
        super(name);
    }

    public static Cookie[] cookieParse(
        final CookieSpec parser,
        String host1,
        int port1,
        String path,
        boolean isSecure,
        String setHeader)
        throws MalformedCookieException {
        Cookie[] cookies = parser.parse(host1, port1, path, isSecure, setHeader);
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                parser.validate(host1, port1, path, isSecure, cookies[i]);
            }
        }
        return cookies;
    }
}
