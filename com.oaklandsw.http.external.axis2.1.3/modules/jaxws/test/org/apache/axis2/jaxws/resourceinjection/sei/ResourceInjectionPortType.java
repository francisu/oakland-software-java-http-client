
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.axis2.jaxws.resourceinjection.sei;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

@WebService(name = "ResourceInjectionPortType", targetNamespace = "http://resourceinjection.sample.test.org")
public interface ResourceInjectionPortType {


    /**
     * 
     * @param arg
     * @return
     *     returns java.lang.String
     */
    @WebMethod(action = "http://resourceinjection.sample.test.org/NewOperation")
    @WebResult(name = "response", targetNamespace = "")
    @RequestWrapper(localName = "echo", targetNamespace = "http://resourceinjection.sample.test.org", className = "org.test.sample.resourceinjection.Echo")
    @ResponseWrapper(localName = "echoResponse", targetNamespace = "http://resourceinjection.sample.test.org", className = "org.test.sample.resourceinjection.EchoResponse")
    public String echo(
        @WebParam(name = "arg", targetNamespace = "")
        String arg);

}
