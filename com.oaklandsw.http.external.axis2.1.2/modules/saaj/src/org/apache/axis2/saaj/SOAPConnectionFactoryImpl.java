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
package org.apache.axis2.saaj;

import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPException;

/**
 * 
 */
public class SOAPConnectionFactoryImpl extends SOAPConnectionFactory {
    /**
     * Create a new <CODE>SOAPConnection</CODE>.
     *
     * @return the new <CODE>SOAPConnection</CODE> object.
     * @throws javax.xml.soap.SOAPException if there was an exception creating the
     *                                      <CODE>SOAPConnection</CODE> object.
     */
    public SOAPConnection createConnection() throws SOAPException {
        return new SOAPConnectionImpl();
    }
}
