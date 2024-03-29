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

import java.lang.annotation.Annotation;

public class WebServiceRefAnnot implements javax.xml.ws.WebServiceRef {

    private String name = "";
    private String wsdlLocation = "";
    private Class type;
    private Class value;
    private String mappedName = "";

    private String typeString = "";
    private String valueString = "";

    /** A WebServiceRefAnnot cannot be instantiated. */
    private WebServiceRefAnnot() {

    }

    private WebServiceRefAnnot(
            String name,
            String wsdlLocation,
            Class type,
            Class value,
            String mappedName,
            String typeString,
            String valueString) {
        this.name = name;
        this.wsdlLocation = wsdlLocation;
        this.type = type;
        this.value = value;
        this.mappedName = mappedName;
        this.typeString = typeString;
        this.valueString = valueString;
    }

    public static WebServiceRefAnnot createWebServiceRefAnnotImpl() {
        return new WebServiceRefAnnot();
    }

    public static WebServiceRefAnnot createWebServiceRefAnnotImpl(
            String name,
            String wsdlLocation,
            Class type,
            Class value,
            String mappedName,
            String typeString,
            String valueString
    ) {
        return new WebServiceRefAnnot(name,
                                      wsdlLocation,
                                      type,
                                      value,
                                      mappedName,
                                      typeString,
                                      valueString);
    }


    /** @return Returns the mappedName. */
    public String mappedName() {
        return mappedName;
    }

    /** @return Returns the name. */
    public String name() {
        return name;
    }

    /** @return Returns the type. */
    public Class type() {
        return type;
    }

    /** @return Returns the value. */
    public Class value() {
        return value;
    }

    /** @return Returns the wsdlLocation. */
    public String wsdlLocation() {
        return wsdlLocation;
    }

    /** @return Returns the typeString. */
    public String getTypeString() {
        return typeString;
    }

    /** @return Returns the valueString. */
    public String getValueString() {
        return valueString;
    }

    /** @param mappedName The mappedName to set. */
    public void setMappedName(String mappedName) {
        this.mappedName = mappedName;
    }

    /** @param name The name to set. */
    public void setName(String name) {
        this.name = name;
    }

    /** @param type The type to set. */
    public void setType(Class type) {
        this.type = type;
    }

    /** @param value The value to set. */
    public void setValue(Class value) {
        this.value = value;
    }

    /** @param wsdlLocation The wsdlLocation to set. */
    public void setWsdlLocation(String wsdlLocation) {
        this.wsdlLocation = wsdlLocation;
    }

    /** @return Returns the wsdlLocation. */
    public String getWsdlLocation() {
        return wsdlLocation;
    }

    /** @param typeString The typeString to set. */
    public void setTypeString(String typeString) {
        this.typeString = typeString;
    }

    /** @param valueString The valueString to set. */
    public void setValueString(String valueString) {
        this.valueString = valueString;
    }

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
        sb.append("@WebServiceRef.name= " + name);
        sb.append(newLine);
        sb.append("@WebServiceRef.wsdlLocation= " + wsdlLocation);
        sb.append(newLine);
        sb.append("@WebServiceRef.mappedName= " + mappedName);
        sb.append(newLine);
        sb.append("@WebServiceRef.type= " + typeString);
        sb.append(newLine);
        sb.append("@WebServiceRef.value= " + valueString);
        sb.append(newLine);
		return sb.toString();
	}
}
