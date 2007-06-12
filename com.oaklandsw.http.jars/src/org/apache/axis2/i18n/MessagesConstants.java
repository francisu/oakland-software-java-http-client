/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.axis2.i18n;

import java.util.Locale;
import java.util.ResourceBundle;

public class MessagesConstants {
    public static final String projectName = "org.apache.axis2".intern();
    public static final String resourceName = "resource".intern();
    public static final Locale locale = null;

    public static final String rootPackageName = "org.apache.axis2.i18n".intern();

    public static final ResourceBundle rootBundle =
            ProjectResourceBundle.getBundle(projectName,
                                            rootPackageName,
                                            resourceName,
                                            locale,
                                            MessagesConstants.class.getClassLoader(),
                                            null);
}
