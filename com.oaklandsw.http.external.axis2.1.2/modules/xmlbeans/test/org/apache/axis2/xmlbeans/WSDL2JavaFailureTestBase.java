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

package org.apache.axis2.xmlbeans;

import org.apache.axis2.wsdl.codegen.CodeGenerationException;

public abstract class WSDL2JavaFailureTestBase extends WSDL2JavaSuccessTestBase {

    public void testWSDLFile() {
        try {
            generateAndCompile(wsdlFileName,
                               OUTPUT_LOCATION_BASE + OUTPUT_LOCATION_PREFIX + folderCount++);
            fail("Undesired behavior while code generating!");
        } catch (CodeGenerationException e) {
            //we are ok here
        }
    }


}
