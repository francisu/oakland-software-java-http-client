/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.axis2.jaxws.description;

import javax.jws.WebService;
import javax.xml.ws.BindingType;
import javax.xml.ws.Service;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceProvider;

/**
 * 
 */
public interface EndpointDescriptionJava {

    public WebService getAnnoWebService();

    public WebServiceProvider getAnnoWebServiceProvider();

    public String getAnnoWebServiceEndpointInterface();

    public String getAnnoWebServiceName();

    public String getAnnoWebServicePortName();

    public String getAnnoWebServiceServiceName();

    public String getAnnoWebServiceTargetNamespace();

    public String getAnnoWebServiceWSDLLocation();

    public BindingType getAnnoBindingType();

    public String getAnnoBindingTypeValue();

    public ServiceMode getAnnoServiceMode();

    public Service.Mode getAnnoServiceModeValue();
}
