/* Copyright 2004,2005 The Apache Software Foundation.
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

import java.lang.annotation.Annotation;

public class RequestWrapperAnnot implements javax.xml.ws.RequestWrapper {

    private String localName;
    private String targetNamespace;
    private String className;


    /** A RequestWrapperAnnot cannot be instantiated. */
    private RequestWrapperAnnot() {

    }

    private RequestWrapperAnnot(
            String localName,
            String targetNamespace,
            String className) {
        this.localName = localName;
        this.targetNamespace = targetNamespace;
        this.className = className;
    }

    public static RequestWrapperAnnot createRequestWrapperAnnotImpl() {
        return new RequestWrapperAnnot();
    }

    public static RequestWrapperAnnot createRequestWrapperAnnotImpl(
            String localName,
            String targetNamespace,
            String className
    ) {
        return new RequestWrapperAnnot(localName,
                                       targetNamespace,
                                       className);
    }


    /** @return Returns the name. */
    public String localName() {
        return this.localName;
    }

    /** @return Returns the targetNamespace. */
    public String targetNamespace() {
        return this.targetNamespace;
    }

    /** @return Returns the wsdlLocation. */
    public String className() {
        return this.className;
    }

    /** @param name The name to set. */
    public void setLocalName(String localName) {
        this.localName = localName;
    }

    /** @param targetNamespace The targetNamespace to set. */
    public void setTargetNamespace(String targetNamespace) {
        this.targetNamespace = targetNamespace;
    }

    /** @param wsdlLocation The wsdlLocation to set. */
    public void setClassName(String className) {
        this.className = className;
    }

    //hmm, should we really do this
    public Class<Annotation> annotationType() {
        return Annotation.class;
    }

    /**
     * Convenience method for unit testing. We will print all of the
     * data members here.
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        String newLine = "\n";
        sb.append(newLine);
        sb.append("@RequestWrapper.localName= " + localName);
        sb.append(newLine);
        sb.append("@RequestWrapper.className= " + className);
        sb.append(newLine);
        sb.append("@RequestWrapper.targetNamespace= " + targetNamespace);
        sb.append(newLine);
        return sb.toString();
	}


}
