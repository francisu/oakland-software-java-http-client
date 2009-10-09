/*
 * $Header:
 * /home/cvspublic/jakarta-commons/httpclient/src/test/org/apache/commons/httpclient/TestHttpStatus.java,v
 * 1.1 2002/03/04 03:23:37 marcsaeg Exp $ $Revision: 1.1 $ $Date: 2002/03/04
 * 03:23:37 $
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation. All rights reserved.
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
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
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
package com.oaklandsw.http.local;

import com.oaklandsw.http.HttpStatus;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;


/**
 *
 * Unit tests for {@link HttpStatus}
 *
 * @author Sean C. Sullivan
 * @version $Id: TestHttpStatus.java,v 1.1 2002/03/04 03:23:37 marcsaeg Exp $
 */
public class TestHttpStatus extends TestCase {
    // ------------------------------------------------------------ Constructor
    public TestHttpStatus(String testName) {
        super(testName);
    }

    // ------------------------------------------------------------------- Main
    public static void main(String[] args) {
        String[] testCaseName = { TestHttpStatus.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }

    // ------------------------------------------------------- TestCase Methods
    public static Test suite() {
        return new TestSuite(TestHttpStatus.class);
    }

    // ----------------------------------------------------------- Test Methods
    public void testStatusText() throws IllegalAccessException {
        Field[] publicFields = HttpStatus.class.getFields();

        assertTrue(publicFields != null);

        assertTrue(publicFields.length > 0);

        for (int i = 0; i < publicFields.length; i++) {
            final Field f = publicFields[i];

            final int modifiers = f.getModifiers();

            if ((f.getType() == int.class) && Modifier.isPublic(modifiers) &&
                    Modifier.isFinal(modifiers) &&
                    Modifier.isStatic(modifiers)) {
                final int iValue = f.getInt(null);
                final String text = HttpStatus.getStatusText(iValue);

                assertTrue("text is null for HttpStatus." + f.getName(),
                    (text != null));

                assertTrue(text.length() > 0);
            }
        }
    }
}
