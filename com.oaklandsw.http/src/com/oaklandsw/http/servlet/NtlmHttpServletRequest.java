/* jcifs smb client library in Java
 * Copyright (C) 2002  "Michael B. Allen" <jcifs at samba dot org>
 *                   "Eric Glass" <jcifs at samba dot org>
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

// Oakland Software modifications:
// 
// This is substantially identical to it's corresponding
// class is JCIFS (version 1.2.13), with some warnings cleaned up.


package com.oaklandsw.http.servlet;

import java.security.Principal;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

class NtlmHttpServletRequest extends HttpServletRequestWrapper {

    Principal principal;

    NtlmHttpServletRequest( HttpServletRequest req, Principal principal1 ) {
        super( req );
        this.principal = principal1;
    }
    public String getRemoteUser() {
        return principal.getName();
    }
    public Principal getUserPrincipal() {
        return principal;
    }
    public String getAuthType() {
        return "NTLM";
    }
}

