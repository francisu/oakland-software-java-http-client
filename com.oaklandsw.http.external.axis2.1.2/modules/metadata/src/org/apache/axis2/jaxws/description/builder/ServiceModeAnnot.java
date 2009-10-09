/*
 * Copyright 2004,2005 The Apache Software Foundation.
 * Copyright 2006 International Business Machines Corp.
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

package org.apache.axis2.jaxws.description.builder;

import javax.xml.ws.Service;
import java.lang.annotation.Annotation;

public class ServiceModeAnnot implements javax.xml.ws.ServiceMode {

    private Service.Mode value = Service.Mode.PAYLOAD;

    /** A ServiceModeAnnot cannot be instantiated. */
    private ServiceModeAnnot() {

    }

    private ServiceModeAnnot(Service.Mode value) {
        this.value = value;
    }

    public static ServiceModeAnnot createWebServiceAnnotImpl() {
        return new ServiceModeAnnot();
    }

    public static ServiceModeAnnot createWebServiceAnnotImpl(Service.Mode value) {
        return new ServiceModeAnnot(value);
    }

    public Service.Mode value() {
        return this.value;
    }

    //hmm, should we really do this
    public Class<Annotation> annotationType() {
        return Annotation.class;
    }

    //Setters
    public void setValue(Service.Mode value) {
        this.value = value;
    }

    /** Convenience method for unit testing. We will print all of the data members here. */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        String newLine = "\n";
        sb.append(newLine);
        sb.append("@ServiceMode.value= " + value.toString());
        sb.append(newLine);
        return sb.toString();
    }
	
}
