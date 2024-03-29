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

package org.apache.axis2.jaxws.description.impl;

import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.java.security.AccessController;
import org.apache.axis2.jaxws.ClientConfigurationFactory;
import org.apache.axis2.jaxws.ExceptionFactory;
import org.apache.axis2.jaxws.catalog.JAXWSCatalogManager;
import org.apache.axis2.jaxws.catalog.impl.OASISCatalogManager;
import org.apache.axis2.jaxws.description.DescriptionFactory;
import org.apache.axis2.jaxws.description.EndpointDescription;
import org.apache.axis2.jaxws.description.EndpointInterfaceDescription;
import org.apache.axis2.jaxws.description.ServiceDescription;
import org.apache.axis2.jaxws.description.ServiceDescriptionJava;
import org.apache.axis2.jaxws.description.ServiceDescriptionWSDL;
import org.apache.axis2.jaxws.description.ServiceRuntimeDescription;
import org.apache.axis2.jaxws.description.builder.DescriptionBuilderComposite;
import org.apache.axis2.jaxws.description.builder.MDQConstants;
import static org.apache.axis2.jaxws.description.builder.MDQConstants.RETURN_TYPE_FUTURE;
import static org.apache.axis2.jaxws.description.builder.MDQConstants.RETURN_TYPE_RESPONSE;
import org.apache.axis2.jaxws.description.builder.MethodDescriptionComposite;
import org.apache.axis2.jaxws.description.builder.ParameterDescriptionComposite;
import org.apache.axis2.jaxws.description.xml.handler.HandlerChainsType;
import org.apache.axis2.jaxws.i18n.Messages;
import org.apache.axis2.jaxws.util.WSDL4JWrapper;
import org.apache.axis2.jaxws.util.WSDLWrapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.jws.HandlerChain;
import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.PortType;
import javax.wsdl.Service;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.handler.PortInfo;
import javax.xml.ws.soap.SOAPBinding;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.ConnectException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;


/** @see ../ServiceDescription */
class ServiceDescriptionImpl
        implements ServiceDescription, ServiceDescriptionWSDL, ServiceDescriptionJava {
    private ClientConfigurationFactory clientConfigFactory;
    private ConfigurationContext configContext;

    private String wsdlURL;
    private QName serviceQName;

    // TODO: Possibly remove Definition and delegate to the Defn on the AxisSerivce set as a paramater by WSDLtoAxisServicBuilder?
    private WSDLWrapper wsdlWrapper;
    private WSDLWrapper generatedWsdlWrapper;
    
    //ANNOTATION: @HandlerChain
    private HandlerChain handlerChainAnnotation;
    private HandlerChainsType handlerChainsType;

    // EndpointDescriptions from annotations and wsdl
    private Map<QName, EndpointDescription> definedEndpointDescriptions =
                new HashMap<QName, EndpointDescription>();

    // Endpoints for dynamic ports
    private Map<Object, Map<QName, EndpointDescriptionImpl>> dynamicEndpointDescriptions =
                new WeakHashMap<Object, Map<QName, EndpointDescriptionImpl>>();

    // Cache classes for the handler chain
    Map<PortInfo, List<Class>> handlerClassesMap =
            new WeakHashMap<PortInfo, List<Class>>();
    
    private static final Log log = LogFactory.getLog(ServiceDescriptionImpl.class);

    private HashMap<String, DescriptionBuilderComposite> dbcMap = null;

    private DescriptionBuilderComposite composite = null;
    private boolean isServerSide = false;

    // Allow a unique XML CatalogManager per service description.
    private JAXWSCatalogManager catalogManager = null;
    
    // RUNTIME INFORMATION
    Map<String, ServiceRuntimeDescription> runtimeDescMap =
            new ConcurrentHashMap<String, ServiceRuntimeDescription>();
            //Collections.synchronizedMap(new HashMap<String, ServiceRuntimeDescription>());

    
    

    /**
     * Create a service-requester side (aka client-side) service description.
     * Construct a service description hierachy
     * based on WSDL (may be null), the Service class, and a service QName.
     *
     * @param wsdlURL      The WSDL file (this may be null).
     * @param serviceQName The name of the service in the WSDL.  This can not be null since a
     *                     javax.xml.ws.Service can not be created with a null service QName.
     * @param serviceClass The JAX-WS service class.  This could be an instance of
     *                     javax.xml.ws.Service or a generated service subclass thereof.  This will
     *                     not be null.
     */
    ServiceDescriptionImpl(URL wsdlURL, QName serviceQName, Class serviceClass) {
        this(wsdlURL, serviceQName, serviceClass, null, null);
    }
    
    /**
     * Create a service-requester side service description.  Same as constructor above with an 
     * additonal composite paramater.  Note that the composite is "sparse" in that any values it
     * contains overrides any values on the corresponding class annotation HOWEVER if the composite
     * doesn't specify a value, it is gotten from the class annotation. 
     * @param wsdlURL
     * @param serviceQName
     * @param serviceClass
     * @param sparseComposite a composite with any annotation overrides such as from a client deployment
     *     descriptor.  CAN NOT BE NULL, but it can be an object created with a default constructor
     */
    ServiceDescriptionImpl(URL wsdlURL, QName serviceQName, Class serviceClass, 
                           DescriptionBuilderComposite sparseComposite,
                           Object sparseCompositeKey) {
        if (log.isDebugEnabled()) {
            log.debug("ServiceDescriptionImpl(URL,QName,Class,DescriptionBuilderComposite,Object)");
        }
    	
    	if (sparseComposite != null)
    	    catalogManager = sparseComposite.getCatalogManager();
    	else
    		catalogManager = new OASISCatalogManager();
    	
        if (serviceQName == null) {
            throw ExceptionFactory.makeWebServiceException(Messages.getMessage("serviceDescErr0"));
        }
        if (serviceClass == null) {
            throw ExceptionFactory
                    .makeWebServiceException(Messages.getMessage("serviceDescErr1", "null"));
        }
        if (!javax.xml.ws.Service.class.isAssignableFrom(serviceClass)) {
            throw ExceptionFactory.makeWebServiceException(
                    Messages.getMessage("serviceDescErr1", serviceClass.getName()));
        }

        composite = new DescriptionBuilderComposite();
        composite.setIsServiceProvider(false);
        composite.setCorrespondingClass(serviceClass);
        // The classloader was originally gotten off this class, but it seems more logical to 
        // get it off the application service class.
//        composite.setClassLoader(this.getClass().getClassLoader());
        composite.setClassLoader(getClassLoader(serviceClass));
        composite.setSparseComposite(sparseCompositeKey, sparseComposite);
        
        // If there's a WSDL URL specified in the sparse composite, that is a override, for example
        // from a JSR-109 deployment descriptor, and that's the one to use.
        URL sparseCompositeWsdlURL = getSparseCompositeWsdlURL(sparseComposite);
        if (sparseCompositeWsdlURL != null) {
            if (log.isDebugEnabled()) {
                log.debug("Wsdl location overriden by sparse composite; overriden value: " + this.wsdlURL);
            }
            this.wsdlURL = sparseCompositeWsdlURL.toString();
        } else {
            this.wsdlURL = wsdlURL == null ? null : wsdlURL.toString();
        }
        if (log.isDebugEnabled()) {
            log.debug("Wsdl Location value used: " + this.wsdlURL);
        }
        // TODO: On the client side, we should not support partial WSDL; i.e. if the WSDL is specified it must be
        //       complete and must contain the ServiceQName.  This is how the Sun RI behaves on the client.
        //       When this is fixed, the check in ServiceDelegate(URL, QName, Class) should be removed
        
        // TODO: The serviceQName needs to be verified between the argument/WSDL/Annotation
        this.serviceQName = serviceQName;

        setupWsdlDefinition();
    }
    
    URL getSparseCompositeWsdlURL(DescriptionBuilderComposite sparseComposite) {
        // Use the WSDL file if it is specified in the composite
        URL url = null;
        if (sparseComposite != null) {
            WebServiceClient wsc = (WebServiceClient) sparseComposite.getWebServiceClientAnnot();
            if (wsc != null && wsc.wsdlLocation() != null) {
                String wsdlLocation = wsc.wsdlLocation();
                URL wsdlUrl = getWSDLURL(wsdlLocation);
                
                if (wsdlUrl == null) {
                    throw ExceptionFactory.makeWebServiceException(Messages.getMessage("serviceDescErr4", wsdlLocation));
                } else {
                    url = wsdlUrl;
                }
            }
        }
        return url;
    }

    private static URL createWsdlURL(String wsdlLocation) {
        URL theUrl = null;
        try {
            theUrl = new URL(wsdlLocation);
        } catch (Exception ex) {
            // Just return a null to indicate we couldn't create a URL from the string
            if (log.isDebugEnabled()) {
                log.debug("Unable to obtain URL for WSDL file: " + wsdlLocation
                        + " by using File reference");
            }
        }
        return theUrl;
    }
    


    /**
     * Create a service-provider side ServiceDesciption.  Create a service Description
     * based on a service implementation class.  Note this is for test-only code; it should not be 
     * used in production code.  And it is being removed from the test code as well.
     *
     * @deprecated
     * @param serviceImplClass
     */
    ServiceDescriptionImpl(Class serviceImplClass, AxisService axisService) {
        if (log.isDebugEnabled()) {
            log.debug("ServiceDescriptionImpl(Class,AxisService)");
        }
        composite = new DescriptionBuilderComposite();
        composite.setIsDeprecatedServiceProviderConstruction(true);
        composite.setIsServiceProvider(true);
        isServerSide = true;

        // Create the EndpointDescription hierachy from the service impl annotations; Since the PortQName is null, 
        // it will be set to the annotation value.
        EndpointDescriptionImpl endpointDescription =
                new EndpointDescriptionImpl(serviceImplClass, null, axisService, this);
        addEndpointDescription(endpointDescription);

        // TODO: The ServiceQName instance variable should be set based on annotation or default
    }

    ServiceDescriptionImpl(
                           HashMap<String, DescriptionBuilderComposite> dbcMap,
                           DescriptionBuilderComposite composite) {
        this(dbcMap, composite, null);
    }
    
    /**
     * Create a service-provider side Service description hierachy.  The hierachy is created entirely
     * from composite.  All relevant classes and interfaces referenced from the class represented by
     * composite must be included in the map.
     * @param dbcMap
     * @param composite
     */
    ServiceDescriptionImpl(
            HashMap<String, DescriptionBuilderComposite> dbcMap,
            DescriptionBuilderComposite composite, 
            ConfigurationContext configContext) {
        this.composite = composite;
        
        this.configContext = configContext;
        if (log.isDebugEnabled()) {
            log.debug("ServiceDescriptionImpl(HashMap<String,DescriptionBuilderComposite>,ConfigurationContext)");
        }

        String serviceImplName = this.composite.getClassName();

        this.dbcMap = dbcMap;
        //TODO: How to we get this when called from server side, create here for now
        //REVIEW: The value being set here is used later in validation checking to
        //        validation that should occur separately on server and client. If
        //        at some point this constructor is ever called by the client side,
        //        then we'll have to get smarter about how we determine server/client
        //        validation
        this.isServerSide = true;

        //capture the WSDL, if there is any...to be used for later processing
        setupWsdlDefinition();

        // Do a first pass validation for this DescriptionBuilderComposite.
        // This is not intended to be a full integrity check, but rather a fail-fast mechanism
        // TODO: Refactor this to a seperate validator class?
        validateDBCLIntegrity();

        // The ServiceQName instance variable is set based on annotation or default
        // It will be set by the EndpointDescriptionImpl since it is the one that knows
        // how to process the annotations and the defaults.
        //TODO: When we get this, need to consider verifying service name between WSDL
        //      and annotations, so

        // Create the EndpointDescription hierachy from the service impl annotations; Since the PortQName is null, 
        // it will be set to the annotation value.
        //EndpointDescription endpointDescription = new EndpointDescription(null, this, serviceImplName);
        EndpointDescriptionImpl endpointDescription =
                new EndpointDescriptionImpl(this, serviceImplName, composite.getProperties());
        addEndpointDescription(endpointDescription);
    }

    /*=======================================================================*/
    /*=======================================================================*/
    // START of public accessor methods

    /**
     * Update or create an EndpointDescription. Updates to existing EndpointDescriptons will be
     * based on the SEI class and its annotations.  Both declared ports and dynamic ports can be
     * updated.  A declared port is one that is defined (e.g. in WSDL or via annotations); a dyamic
     * port is one that is not defined (e.g. not via WSDL or annotations) and has been added via
     * Serivce.addPort.
     * <p/>
     * For predefined ports, a composite of sparse metadata, such as from a deployment descriptor
     * may be supplied.  This can be used to modify the endpoint interfaceannotations such as
     * adding a handler chain.  The sparse composite is NOT supported for dynamic (i.e. ADD_PORT)
     * or dispatch clients. 
     * <p/>
     * Notes on how an EndpointDescription can be updated or created: 1) Service.createDispatch can
     * create a Dispatch client for either a declared or dynamic port 2) Note that creating a
     * Dispatch does not associate an SEI with an endpoint 3) Service.getPort will associate an SEI
     * with a port 4) A getPort on an endpoint which was originally created for a Distpatch will
     * update that EndpointDescription with the SEI provided on the getPort 5) Service.getPort can
     * not be called on a dynamic port (per the JAX-WS spec) 6) Service.addPort can not be called
     * for a declared port
     *
     * @param sei        This will be non-null if the update is of type GET_PORT; it will be null if
     *                   the update is ADD_PORT or CREATE_DISPATCH
     * @param portQName
     * @param updateType Indicates what is causing the update GET_PORT is an attempt to get a
     *                   declared SEI-based port ADD_PORT is an attempt to add a previously
     *                   non-existent dynamic port CREATE_DISPATCH is an attempt to create a
     *                   Dispatch-based client to either a declared port or a pre-existing dynamic
     *                   port.
     * @param composite  May contain sparse metadata, for example from a deployment descriptor, that
     *                   should be used in conjunction with the class annotations to update the
     *                   description hierarchy.  For example, it may contain a HandlerChain annotation
     *                   based on information in a JSR-109 deployment descriptor.                    
     */

    EndpointDescription updateEndpointDescription(Class sei, 
                                                  QName portQName,
                                                  DescriptionFactory.UpdateType updateType,
                                                  DescriptionBuilderComposite composite,
                                                  Object serviceDelegateKey) {

	
    	EndpointDescriptionImpl endpointDescription = getEndpointDescriptionImpl(portQName);
    	boolean isPortDeclared = isPortDeclared(portQName);

    	// If a defined endpointDescription is not available, try and locate a dynamic endpoint.
    	// Note that a dynamic port will only be found for the client that created it, per the
    	// serviceDelegateKey

    	if (endpointDescription == null && serviceDelegateKey != null) {
    		endpointDescription = getDynamicEndpointDescriptionImpl(portQName, serviceDelegateKey);
    	}

        // If no QName was specified in the arguments, one may have been specified in the sparse
        // composite metadata when the service was created.
        if (DescriptionUtils.isEmpty(portQName)) {
            QName preferredPortQN = getPreferredPort(serviceDelegateKey);
            if (!DescriptionUtils.isEmpty(preferredPortQN)) {
                portQName = preferredPortQN;
            }
        }

        switch (updateType) {

            case ADD_PORT:
                if (composite != null) {
                    throw ExceptionFactory.makeWebServiceException(Messages.getMessage("serviceDescErr5", portQName.toString()));
                }
                // Port must NOT be declared (e.g. can not already exist in WSDL)
                // If an EndpointDesc doesn't exist; create it as long as it doesn't exist in the WSDL
                if (DescriptionUtils.isEmpty(portQName)) {
                    throw ExceptionFactory
                            .makeWebServiceException(Messages.getMessage("addPortErr2"));
                }
                if (getWSDLWrapper() != null && isPortDeclared) {
                    throw ExceptionFactory.makeWebServiceException(
                            Messages.getMessage("addPortDup", portQName.toString()));
                } else if (endpointDescription == null) {
                    // Use the SEI Class and its annotations to finish creating the Description hierarchy.  Note that EndpointInterface, Operations, Parameters, etc.
                    // are not created for dynamic ports.  It would be an error to later do a getPort against a dynamic port (per the JAX-WS spec)
                    // If we can't add the dynamic port under a specific service delegate, that is an error

                    if (serviceDelegateKey == null) {
                        throw ExceptionFactory.makeWebServiceException(
                                 Messages.getMessage("serviceDescriptionImplAddPortErr"));
                    }

                    endpointDescription = new EndpointDescriptionImpl(sei, portQName, true, this);

                    addDynamicEndpointDescriptionImpl(endpointDescription, serviceDelegateKey);

                } else {
                    // All error chJeck above passed, the EndpointDescription already exists and needs no updating
                }
                break;

            case GET_PORT:
                
                // try to find existing endpointDesc by SEI class if portQName was not specified 
                if (endpointDescription == null && portQName == null && sei != null) {
                    endpointDescription = getEndpointDescriptionImpl(sei);
                }
                
                // If an endpointDesc doesn't exist, and the port exists in the WSDL, create it
                // If an endpointDesc already exists and has an associated SEI already, make sure they match
                // If an endpointDesc already exists and was created for Dispatch (no SEI), update that with the SEI provided on the getPort

                // Port must be declared (e.g. in WSDL or via annotations)
                // TODO: Once isPortDeclared understands annotations and not just WSDL, the 2nd part of this check can possibly be removed.
                //       Although consider the check below that updates an existing EndpointDescritpion with an SEI.
                if (!isPortDeclared ||
                        (endpointDescription != null && endpointDescription.isDynamicPort())) {
                    // This guards against the case where an addPort was done previously and now a getPort is done on it.
                    throw ExceptionFactory.makeWebServiceException(
                    		Messages.getMessage("updateEPDescrErr1",(portQName != null ? portQName.toString() : "not specified")));
                } else if (sei == null) {
                    throw ExceptionFactory.makeWebServiceException(
                    		Messages.getMessage("updateEPDescrErr2",(portQName != null ? portQName.toString() : "not specified")));
                } else if (endpointDescription == null) {
                    // Use the SEI Class and its annotations to finish creating the Description hierachy: Endpoint, EndpointInterface, Operations, Parameters, etc.
                    endpointDescription = new EndpointDescriptionImpl(sei, portQName, this, composite, serviceDelegateKey);
                    addEndpointDescription(endpointDescription);
                    /*
                     * We must reset the service runtime description after adding a new endpoint
                     * since the runtime description information could have references to old
                     * information. See MarshalServiceRuntimeDescriptionFactory and 
                     * MarshalServiceRuntimeDescription.
                     */
                    resetServiceRuntimeDescription();
                } else
                if (getEndpointSEI(portQName) == null && !endpointDescription.isDynamicPort()) {
                    // Existing endpointDesc from a declared port needs to be updated with an SEI.
                    // This could happen if the client first did a CREATE_DISPATCH on a declared
                    // port, which would cause it to be created, and then did a GET_PORT on the 
                    // same port later, providing an SEI.
                    // Notes 
                    // 1) An EndpointDescritption created from an addPort (i.e. a dynamic port) can 
                    //    not do this.
                    // 2) A sparse composite may be specified.  We don't allow mixing JAX-WS unmanaged
                    //    apis (CREATE_DISPATCH) with JSR-109 managed apis (GET_PORT with sparse
                    //    composite metadata from a DD).  Since the sparse composite is stored by
                    //    a key AND CREATE_DISPATCH and ADD_PORT will thrown an exception of a composite
                    //    is specified, having a composite and key on the GET_PORTs shouldn't be
                    //    a problem.
                    endpointDescription.updateWithSEI(sei, composite, serviceDelegateKey);
                } else if (getEndpointSEI(portQName) != sei) {
                    throw ExceptionFactory.makeWebServiceException(
                    		Messages.getMessage("updateEPDescrErr3",portQName.toString(),
                    				sei.getName(),getEndpointSEI(portQName).getName()));
                } else {
                    // All error check above passed, the EndpointDescription already exists and needs no updating
                    // Just add the sparse composite if one was specified.
                    endpointDescription.getDescriptionBuilderComposite().setSparseComposite(serviceDelegateKey, composite);
                }
                break;

            case CREATE_DISPATCH:
                if (composite != null) {
                    throw ExceptionFactory.makeWebServiceException(Messages.getMessage("serviceDescErr6"));
                }
                
                // Port may or may not exist in WSDL.
                // If an endpointDesc doesn't exist and it is in the WSDL, it can be created
                // Otherwise, it is an error.
                if (DescriptionUtils.isEmpty(portQName)) {
                    throw ExceptionFactory
                            .makeWebServiceException(Messages.getMessage("createDispatchFail0"));
                } else if (endpointDescription != null) {
                    // The EndpointDescription already exists; nothing needs to be done
                } else if (sei != null) {
                    // The Dispatch should not have an SEI associated with it on the update call.
                    // REVIEW: Is this a valid check?
                    throw ExceptionFactory.makeWebServiceException(
                    		Messages.getMessage("createDispatchFail3",portQName.toString()));
                } else if (getWSDLWrapper() != null && isPortDeclared) {
                    // EndpointDescription doesn't exist and this is a declared Port, so create one
                    // Use the SEI Class and its annotations to finish creating the Description hierarchy.  Note that EndpointInterface, Operations, Parameters, etc.
                    // are not created for Dispatch-based ports, but might be updated later if a getPort is done against the same declared port.
                    // TODO: Need to create the Axis Description objects after we have all the config info (i.e. from this SEI)
                    endpointDescription = new EndpointDescriptionImpl(sei, portQName, this);
                    addEndpointDescription(endpointDescription);
                } else {
                    // The port is not a declared port and it does not have an EndpointDescription, 
                	// meaning an addPort has not been done for it
                    // This is an error.
                    throw ExceptionFactory.makeWebServiceException(
                            Messages.getMessage("createDispatchFail1", portQName.toString()));
                }
                break;
        }
        return endpointDescription;
    }

    private Class getEndpointSEI(QName portQName) {
        Class endpointSEI = null;
        EndpointDescription endpointDesc = getEndpointDescription(portQName);
        if (endpointDesc != null) {
            EndpointInterfaceDescription endpointInterfaceDesc = 
                endpointDesc.getEndpointInterfaceDescription();
            if (endpointInterfaceDesc != null ) {
                endpointSEI = endpointInterfaceDesc.getSEIClass();
            }
        }
        return endpointSEI;
    }

    private boolean isPortDeclared(QName portQName) {
        // TODO: This needs to account for declaration of the port via annotations in addition to just WSDL
        // TODO: Add logic to check the portQN namespace against the WSDL Definition NS
        boolean portIsDeclared = false;
        if (!DescriptionUtils.isEmpty(portQName)) {
            if (getWSDLWrapper() != null) {
                Definition wsdlDefn = getWSDLWrapper().getDefinition();
                Service wsdlService = wsdlDefn.getService(serviceQName);
                Port wsdlPort = wsdlService.getPort(portQName.getLocalPart());
                portIsDeclared = (wsdlPort != null);
            } else {
                // TODO: Add logic to determine if port is declared via annotations when no WSDL is present.  For now, we have to assume it is declared 
                // so getPort(...) and createDispatch(...) calls work when there is no WSDL.
                portIsDeclared = true;
            }
        } else {
            // PortQName is null, so the runtime gets to choose which one to use.  Since there's no WSDL
            // we'll use annotations, so it is implicitly declared
            portIsDeclared = true;
        }
        return portIsDeclared;
    }

    /* (non-Javadoc)
    * @see org.apache.axis2.jaxws.description.ServiceDescription#getEndpointDescriptions()
    */
    public EndpointDescription[] getEndpointDescriptions() {
        return definedEndpointDescriptions.values().toArray(new EndpointDescriptionImpl[0]);
    }

    public Collection<EndpointDescriptionImpl> getDynamicEndpointDescriptions_AsCollection(Object serviceDelegateKey) {
        Collection <EndpointDescriptionImpl> dynamicEndpoints = null;
    	if (serviceDelegateKey != null ) {
    		if (dynamicEndpointDescriptions.get(serviceDelegateKey) != null)
    			dynamicEndpoints = dynamicEndpointDescriptions.get(serviceDelegateKey).values();
        }
    	return dynamicEndpoints;
    }

    public Collection<EndpointDescription> getEndpointDescriptions_AsCollection() {
    	return definedEndpointDescriptions.values();
    }

    /* (non-Javadoc)
    * @see org.apache.axis2.jaxws.description.ServiceDescription#getEndpointDescription(javax.xml.namespace.QName)
    */
    public EndpointDescription getEndpointDescription(QName portQName) {

    	return getEndpointDescription(portQName, null);
    }

    public EndpointDescription getEndpointDescription(QName portQName, Object serviceDelegateKey) {
        EndpointDescription returnDesc = null;
        if (!DescriptionUtils.isEmpty(portQName)) {
    		returnDesc = definedEndpointDescriptions.get(portQName);

    		if (returnDesc == null && serviceDelegateKey != null) {
		           returnDesc = getDynamicEndpointDescriptionImpl(portQName, serviceDelegateKey);
    		}    		        	
        }
        return returnDesc;
    }

    EndpointDescriptionImpl getEndpointDescriptionImpl(QName portQName) {
        return (EndpointDescriptionImpl)getEndpointDescription(portQName, null);
    }
    
    EndpointDescriptionImpl getEndpointDescriptionImpl(QName portQName, Object serviceDelegateKey) {
        return (EndpointDescriptionImpl)getEndpointDescription(portQName);
    }
    
    EndpointDescriptionImpl getEndpointDescriptionImpl(Class seiClass) {
        for (EndpointDescription endpointDescription : definedEndpointDescriptions.values()) {
            EndpointInterfaceDescription endpointInterfaceDesc =
                    endpointDescription.getEndpointInterfaceDescription();
            // Note that Dispatch endpoints will not have an endpointInterface because the do not have an associated SEI
            if (endpointInterfaceDesc != null) {
                Class endpointSEIClass = endpointInterfaceDesc.getSEIClass();
                if (endpointSEIClass != null && endpointSEIClass.equals(seiClass)) {
                    return (EndpointDescriptionImpl)endpointDescription;
                }
            }
        }
        return null;
    }

    public DescriptionBuilderComposite getDescriptionBuilderComposite() {
        return composite;
    }

    /* (non-Javadoc)
     * @see org.apache.axis2.jaxws.description.ServiceDescription#getEndpointDescription(java.lang.Class)
     */
    public EndpointDescription[] getEndpointDescription(Class seiClass) {
        EndpointDescription[] returnEndpointDesc = null;
        ArrayList<EndpointDescriptionImpl> matchingEndpoints =
                new ArrayList<EndpointDescriptionImpl>();
        for (EndpointDescription endpointDescription : definedEndpointDescriptions.values()) {
            EndpointInterfaceDescription endpointInterfaceDesc =
                    endpointDescription.getEndpointInterfaceDescription();
            // Note that Dispatch endpoints will not have an endpointInterface because the do not have an associated SEI
            if (endpointInterfaceDesc != null) {
                Class endpointSEIClass = endpointInterfaceDesc.getSEIClass();
                if (endpointSEIClass != null && endpointSEIClass.equals(seiClass)) {
                    matchingEndpoints.add((EndpointDescriptionImpl)endpointDescription);
                }
            }
        }
        if (matchingEndpoints.size() > 0) {
            returnEndpointDesc = matchingEndpoints.toArray(new EndpointDescriptionImpl[0]);
        }
        return returnEndpointDesc;
    }

    // END of public accessor methods
    /*=======================================================================*/
    /*=======================================================================*/
    private void addEndpointDescription(EndpointDescriptionImpl endpoint) {
    	definedEndpointDescriptions.put(endpoint.getPortQName(), endpoint);
    }

    private void setupWsdlDefinition() {
        // Note that there may be no WSDL provided, for example when called from 
        // Service.create(QName serviceName).

        if (log.isDebugEnabled()) {
            log.debug("setupWsdlDefinition()");
        }
        
        if (composite.isServiceProvider()) {

            //  Currently, there is a bug which allows the wsdlDefinition to be placed
            //  on either the impl class composite or the sei composite, or both. We need to
            //  look in both places and find the correct one, if it exists.

            if (((composite.getWebServiceAnnot() != null) &&
                    DescriptionUtils.isEmpty(composite.getWebServiceAnnot().endpointInterface()))
                    ||
                    (!(composite.getWebServiceProviderAnnot() == null))) {
                //This is either an implicit SEI, or a WebService Provider
                if (composite.getWsdlDefinition() != null) {
                    URL url = composite.getWsdlURL();
                    this.wsdlURL = url == null ? null : url.toString();

                    try {
                        if (log.isDebugEnabled() ) {
                            if (configContext != null) {
                                log.debug("new WSDL4JWrapper-ConfigContext not null1"); 
                            } else {
                                log.debug("new WSDL4JWrapper-ConfigContext null1"); 
                            }
                        }

                        this.wsdlWrapper = new WSDL4JWrapper(url,
                                                             composite.getWsdlDefinition(), 
                                                             configContext,
                                                             this.catalogManager);
                    } catch (WSDLException e) {
                        throw ExceptionFactory.makeWebServiceException(
                                Messages.getMessage("wsdlException", e.getMessage()), e);
                    }
                } else {
                	String wsdlLocation = null;
                	wsdlLocation = composite.getWebServiceAnnot() != null ? composite.getWebServiceAnnot().wsdlLocation() :
                		composite.getWebServiceProviderAnnot().wsdlLocation();
                	if(wsdlLocation != null
                			&&
                			!"".equals(wsdlLocation)) {
                		setWSDLDefinitionOnDBC(wsdlLocation);
                	}
                }

            } else if (composite.getWebServiceAnnot() != null) {
                //This impl class specifies an SEI...this is a special case. There is a bug
                //in the tooling that allows for the wsdllocation to be specifed on either the
                //impl. class, or the SEI, or both. So, we need to look for the wsdl as follows:
                //          1. If the Wsdl exists on the SEI, then check for it on the impl.
                //          2. If it is not found in either location, in that order, then generate

                DescriptionBuilderComposite seic =
                        getDBCMap().get(composite.getWebServiceAnnot().endpointInterface());

                try {
                    if (seic == null) {
                        if (log.isDebugEnabled()) {
                            log.debug("The SEI class " + composite.getWebServiceAnnot().endpointInterface() + " was not found.");
                        }
                    }
                    if (seic != null && seic.getWsdlDefinition() != null) {
                        // set the wsdl def from the SEI composite
                        if (log.isDebugEnabled()) {
                            log.debug("Get the wsdl definition from the SEI composite.");
                        }
                        URL url = seic.getWsdlURL(); 
                        this.wsdlURL = url.toString();
                        if (log.isDebugEnabled() ) {
                            if (configContext != null) {
                                log.debug("new WSDL4JWrapper-ConfigContext not null2"); 
                            } else {
                                log.debug("new WSDL4JWrapper-ConfigContext null2"); 
                            }
                        }
                        this.wsdlWrapper =
                                new WSDL4JWrapper(seic.getWsdlURL(), 
                                                  seic.getWsdlDefinition(), 
                                                  configContext,
                                                  this.catalogManager);
                            
                    } else if (composite.getWsdlDefinition() != null) {
                        //set the wsdl def from the impl. class composite
                        if (log.isDebugEnabled()) {
                            log.debug("Get the wsdl definition from the impl class composite.");
                        }
                        if (log.isDebugEnabled() ) {
                            if (configContext != null) {
                                log.debug("new WSDL4JWrapper-ConfigContext not null3"); 
                            } else {
                                log.debug("new WSDL4JWrapper-ConfigContext null3"); 
                            }
                        }
                        URL url = composite.getWsdlURL();
                        this.wsdlURL = url == null ? null : url.toString();
                        this.wsdlWrapper = new WSDL4JWrapper(composite.getWsdlURL(),
                                                             composite.getWsdlDefinition(), 
                                                             configContext,
                                                             this.catalogManager);
                                                            
                    } else {
                    	String wsdlLocation = null;
                    	// first check to see if the wsdlLocation is on the SEI
                    	if(seic != null
                    			&&
                    			seic.getWebServiceAnnot() != null) {
                    	    if (log.isDebugEnabled()) {
                    	        log.debug("Get the wsdl location from the SEI composite.");
                    	    }
                    	    wsdlLocation = seic.getWebServiceAnnot().wsdlLocation();
                    	}
                    	
                    	// now check the impl
                    	if(wsdlLocation == null
                    	        ||
                    	        "".equals(wsdlLocation)) {
                    	    if (log.isDebugEnabled()) {
                    	        log.debug("Get the wsdl location from the impl class composite.");
                            }
                    	    wsdlLocation = composite.getWebServiceAnnot().wsdlLocation();
                    	}
                    	
                    	if(wsdlLocation != null
                    	        &&
                    	        !"".equals(wsdlLocation)) {
                    	    if (log.isDebugEnabled()) {
                    	        log.debug("wsdl location =" + wsdlLocation);
                    	    }
                            this.wsdlURL = wsdlLocation;
                            setWSDLDefinitionOnDBC(wsdlLocation);
                    	}
                    }
                } catch (WSDLException e) {
                    throw ExceptionFactory.makeWebServiceException(
                            Messages.getMessage("wsdlException", e.getMessage()), e);
                }
            }

            //Deprecate this code block when MDQ is fully integrated
        } else if (wsdlURL != null) {
            try {
                if (log.isDebugEnabled() ) {
                    if (configContext != null) {
                        log.debug("new WSDL4JWrapper-ConfigContext not null4"); 
                    } else {
                        log.debug("new WSDL4JWrapper-ConfigContext null4"); 
                    }
                }
                this.wsdlWrapper = new WSDL4JWrapper(new URL(this.wsdlURL),configContext,
                		                             this.catalogManager);
             
            }
            catch (FileNotFoundException e) {
                throw ExceptionFactory.makeWebServiceException(
                        Messages.getMessage("wsdlNotFoundErr", e.getMessage()), e);
            }
            catch (UnknownHostException e) {
                throw ExceptionFactory.makeWebServiceException(
                        Messages.getMessage("unknownHost", e.getMessage()), e);
            }
            catch (ConnectException e) {
                throw ExceptionFactory.makeWebServiceException(
                        Messages.getMessage("connectionRefused", e.getMessage()), e);
            }
            catch(IOException e) {
                throw ExceptionFactory.makeWebServiceException(Messages.getMessage("urlStream", e.getMessage()), e);
            }
            catch (WSDLException e) {
                throw ExceptionFactory.makeWebServiceException(
                        Messages.getMessage("wsdlException", e.getMessage()), e);
            }
        }
    }
    
    /**
     * This method accepts a location of a WSDL document and attempts to
     * load and set the WSDL definition on the DBC object.
     * @param wsdlLocation
     */
    private void setWSDLDefinitionOnDBC(String wsdlLocation) {
    	if(log.isDebugEnabled()) {
			log.debug("Attempting to load WSDL file from location specified in annotation: " +
					wsdlLocation);
		}
		if(composite.getClassLoader() == null) {
			if(log.isDebugEnabled()) {
				log.debug("A classloader could not be found for class: " + composite.
						getClassName() + ". The WSDL file: " + wsdlLocation + " will not be " +
								"processed, and the ServiceDescription will be built from " +
								"annotations");
			}
		}
		try {
		    if(log.isDebugEnabled()) {
		        log.debug("Attempting to read WSDL: " + wsdlLocation + " for web " +
		        		"service endpoint: " + composite.getClassName());
		    }
                    if (log.isDebugEnabled() ) {
                        if (configContext != null) {
                            log.debug("new WSDL4JWrapper-ConfigContext not null5");
                        } else {
                            log.debug("new WSDL4JWrapper-ConfigContext null5");
                        }
                    }

                    URL url = getWSDLURL(wsdlLocation);
                    ConfigurationContext cc = composite.getConfigurationContext();
                    if (cc != null) {
                        this.wsdlWrapper = new WSDL4JWrapper(url, cc, this.catalogManager);
                    } else {
                        // Probably shouldn't get here.  But if we do, use a memory sensitive
                        // wsdl wrapper
                        this.wsdlWrapper = new WSDL4JWrapper(url, this.catalogManager, true, 2);
                    }
                    composite.setWsdlDefinition(wsdlWrapper.getDefinition());
		}
		catch(Exception e) {
			if(log.isDebugEnabled()) {
				log.debug("The WSDL file: " + wsdlLocation + " for class: " + composite.getClassName() + 
						"could not be processed. The ServiceDescription will be built from " +
								"annotations");
			}
		}
    }
    
    /**
     * This method will handle obtaining a URL for the given WSDL location.  The WSDL will be
     * looked for in the following places in this order:
     * 1) As a resource on the classpath
     * 2) As a fully specified URL
     * 3) As a file on the filesystem.  This is analagous to what the generated
     *    Service client does.  Is prepends "file:/" to whatever is specified in the
     *    @WebServiceClient.wsdlLocation element.
     * 
     * @param wsdlLocation The WSDL for which a URL is wanted
     * @return A URL if the WSDL can be located, or null
     */
    private URL getWSDLURL(String wsdlLocation) {
        // Look for the WSDL file as follows:
        // 1) As a resource on the classpath

        ClassLoader loader = composite.getClassLoader();
        URL url = null;
        if (loader != null) {
            url = getResource(wsdlLocation, loader); 
        }
        
        // Try the context class loader
        if(url == null){
            ClassLoader classLoader = getContextClassLoader(null);
            if(classLoader != loader){
                url = getResource(wsdlLocation, classLoader);
            }
        }

        // 2) As a fully specified URL
        if (url == null) {
            if (log.isDebugEnabled()) {
                log.debug("URL for wsdl file: " + wsdlLocation + " could not be "
                        + "determined by classloader... looking for file reference");
            }
            url = createWsdlURL(wsdlLocation);
        }
        // 3) As a file on the filesystem.  This is analagous to what the generated
        //    Service client does.  Is prepends "file:/" to whatever is specified in the
        //    @WebServiceClient.wsdlLocation element.
        if (url == null) {
            if (log.isDebugEnabled()) {
                log.debug("URL for wsdl file: " + wsdlLocation + " could not be "
                        + "found as local file reference... prepending file: protocol");
            }
            // This check is necessary because Unix/Linux file paths begin
            // with a '/'. When adding the prefix 'jar:file:/' we may end
            // up with '//' after the 'file:' part. This causes the URL 
            // object to treat this like a remote resource
            if(wsdlLocation.indexOf("/") == 0) {
                wsdlLocation = wsdlLocation.substring(1, wsdlLocation.length());
            }
            url = createWsdlURL("file:/" + wsdlLocation);

        }
        if (url == null) {
            if (log.isDebugEnabled()) {
                log.debug("Unable to obtain URL for WSDL file: " + wsdlLocation
                        + " by using prepended file: protocol");
            }
        }
        return url;
    }

    private URL getResource(final String wsdlLocation, final ClassLoader loader) {
        return (URL) AccessController.doPrivileged(
                new PrivilegedAction() {
                    public Object run() {
                        return loader.getResource(wsdlLocation);
                    }
                }
        );
    }

    // TODO: Remove these and replace with appropraite get* methods for WSDL information
    /* (non-Javadoc)
     * @see org.apache.axis2.jaxws.description.ServiceDescriptionWSDL#getWSDLWrapper()
     */
    public WSDLWrapper getWSDLWrapper() {
        return wsdlWrapper;
    }

    /* (non-Javadoc)
    * @see org.apache.axis2.jaxws.description.ServiceDescriptionWSDL#getWSDLLocation()
    */
    public String getWSDLLocation() {
        return wsdlURL;
    }

    /**
     * TODO: This method should be replaced with specific methods for getWSDLGenerated... similar to
     * how getWsdlWrapper should be replaced.
     */
    public WSDLWrapper getGeneratedWsdlWrapper() {
        return this.generatedWsdlWrapper;
    }

    void setAxisConfigContext(ConfigurationContext config) {
        this.configContext = config;
    }
    
    /* (non-Javadoc)
    * @see org.apache.axis2.jaxws.description.ServiceDescription#getAxisConfigContext()
    */
    public ConfigurationContext getAxisConfigContext() {
        if (configContext == null) {
            configContext = getClientConfigurationFactory().getClientConfigurationContext();
        }
        return configContext;

    }

    ClientConfigurationFactory getClientConfigurationFactory() {

        if (clientConfigFactory == null) {
            clientConfigFactory = DescriptionFactory.createClientConfigurationFactory();
        }
        return clientConfigFactory;
    }

    /* (non-Javadoc)
    * @see org.apache.axis2.jaxws.description.ServiceDescription#getServiceClient(javax.xml.namespace.QName)
    */
    public ServiceClient getServiceClient(QName portQName, Object serviceDelegateKey) {
        ServiceClient returnServiceClient = null;
        if (!DescriptionUtils.isEmpty(portQName)) {
            EndpointDescription endpointDesc = getEndpointDescription(portQName, serviceDelegateKey);
            
            if (endpointDesc != null) {
                returnServiceClient = endpointDesc.getServiceClient();
            }
            else {
                // Couldn't find Endpoint Description for port QName
                if (log.isDebugEnabled()) {
                    log.debug("Could not find portQName: " + portQName 
                            + " under ServiceDescription: " + toString());
                }
            }
        }
        else {
            // PortQName is empty
            if (log.isDebugEnabled()) {
                log.debug("PortQName agrument is invalid; it can not be null or an empty string: " + portQName);
            }
        }
        
        return returnServiceClient;
    }

    /* (non-Javadoc)
    * @see org.apache.axis2.jaxws.description.ServiceDescription#getServiceQName()
    */
    public QName getServiceQName() {
        //It is assumed that this will always be set in the constructor rather than
        //built up from the class or DBC
        return serviceQName;
    }

    void setServiceQName(QName theName) {
        serviceQName = theName;
    }
    
    public JAXWSCatalogManager getCatalogManager() {
    	return catalogManager;
    }

    /* (non-Javadoc)
     * @see org.apache.axis2.jaxws.description.ServiceDescription#isMTOMEnabled(java.lang.Object)
     */
    public boolean isMTOMEnabled(Object key) {
        return getDescriptionBuilderComposite().isMTOMEnabled(key);
    }
    
    /* (non-Javadoc)
     * @see org.apache.axis2.jaxws.description.ServiceDescription#getPreferredPort(java.lang.Object)
     */
    public QName getPreferredPort(Object key) {
        return getDescriptionBuilderComposite().getPreferredPort(key);
    }
    
    /* (non-Javadoc)
     * @see org.apache.axis2.jaxws.description.ServiceDescription#isServerSide()
     */
    public boolean isServerSide() {
        return isServerSide;
    }

    HashMap<String, DescriptionBuilderComposite> getDBCMap() {
        return dbcMap;
    }

    void setGeneratedWsdlWrapper(WSDL4JWrapper wrapper) {
        this.generatedWsdlWrapper = wrapper;
    }

    void setWsdlWrapper(WSDL4JWrapper wrapper) {
        this.wsdlWrapper = wrapper;
    }

    private void validateDBCLIntegrity() {

        //First, check the integrity of this input composite
        //and retrieve
        //the composite that represents this impl

//TODO: Currently, we are calling this method on the DBC. However, the DBC
//will eventually need access to to the whole DBC map to do proper validation.
//We don't want to pass the map of DBC's back into a single DBC.
//So, for starters, this method and all the privates that it calls should be 
// moved to here. At some point, we should consider using a new class that we
//can implement scenarios of, like validateServiceImpl implements validator

        try {
            validateIntegrity();
        }
        catch (Exception ex) {
            throw ExceptionFactory.makeWebServiceException(
            		Messages.getMessage("dbclIntegrityErr",ex.toString(),composite.toString()), ex);
        }
    }

    /*
      * Validates the integrity of an impl. class. This should not be called directly for an SEI composite
      */
    void validateIntegrity() {
        //TODO: Consider moving this to a utils area, do we really want a public
        //      method that checks integrity...possibly

        //In General, this integrity checker should do gross level checking
        //It should not be setting spec-defined default values, but can look
        //at things like empty strings or null values

        //TODO: This method will validate the integrity of this object. Basically, if
        //consumer set this up improperly, then we should fail fast, should consider placing
        //this method in a utils class within the 'description' package

        //Verify that, if this implements a strongly typed provider interface, that it
        // also contain a WebServiceProvider annotation per JAXWS Sec. 5.1
        Iterator<String> iter =
                composite.getInterfacesList().iterator();

        // Remember if we've validated the Provider interface.  Later we'll make sure that if we have an 
        // WebServiceProvider annotation, we found a valid interface here.
        boolean providerInterfaceValid = false;
        while (iter.hasNext()) {
            String interfaceString = iter.next();
            if (interfaceString.equals(MDQConstants.PROVIDER_SOURCE)
                    || interfaceString.equals(MDQConstants.PROVIDER_SOAP)
                    || interfaceString.equals(MDQConstants.PROVIDER_DATASOURCE)
                    || interfaceString.equals(MDQConstants.PROVIDER_STRING)) {
                providerInterfaceValid = true;
                //This is a provider based endpoint, make sure the annotation exists
                if (composite.getWebServiceProviderAnnot() == null) {
                    throw ExceptionFactory.makeWebServiceException(
                    		Messages.getMessage("validateIntegrityErr1",composite.getClassName()));
                }
            }
        }

        //Verify that WebService and WebServiceProvider are not both specified
        //per JAXWS - Sec. 7.7
        if (composite.getWebServiceAnnot() != null &&
                composite.getWebServiceProviderAnnot() != null) {
            throw ExceptionFactory.makeWebServiceException(
            		Messages.getMessage("validateIntegrityErr2",composite.getClassName()));
        }

        if (composite.getWebServiceProviderAnnot() != null) {
            if (!providerInterfaceValid) {
                throw ExceptionFactory.makeWebServiceException(
                		Messages.getMessage("validateIntegrityErr3",composite.getClassName()));
            }
            // There must be a public default constructor per JAXWS - Sec 5.1
            if (!validateDefaultConstructor()) {
                throw ExceptionFactory.makeWebServiceException(
                		Messages.getMessage("validateIntegrityErr4",composite.getClassName()));
            }
            // There must be an invoke method per JAXWS - Sec 5.1.1
            if (!validateInvokeMethod()) {
                throw ExceptionFactory.makeWebServiceException(
                		Messages.getMessage("validateIntegrityErr5",composite.getClassName()));
            }

            //If ServiceMode annotation specifies 'payload', then make sure that it is not typed with
            // SOAPMessage or DataSource
            validateProviderInterfaces();

        } else if (composite.getWebServiceAnnot() != null) {

            if (composite.getServiceModeAnnot() != null) {
                throw ExceptionFactory.makeWebServiceException(
                		Messages.getMessage("validateIntegrityErr6",composite.getClassName()));
            }

            //TODO: hmmm, will we ever actually validate an interface directly...don't think so
            if (!composite.isInterface()) {
                // TODO: Validate on the class that this.classModifiers Array does not contain the strings
                //        FINAL or ABSTRACT, but does contain PUBLIC
                // TODO: Validate on the class that a public constructor exists
                // TODO: Validate on the class that a finalize() method does not exist
                if (!DescriptionUtils.isEmpty(composite.getWebServiceAnnot().wsdlLocation())) {
                    if (composite.getWsdlDefinition() == null && composite.getWsdlURL() == null) {
                        throw ExceptionFactory.makeWebServiceException(
                        		Messages.getMessage("validateIntegrityErr7",composite.getClassName(),
                        				composite.getWebServiceAnnot().wsdlLocation()));
                    }
                }

                //		setWebServiceAnnotDefaults(true=impl); Must happen before we start checking annot
                if (!DescriptionUtils.isEmpty(composite.getWebServiceAnnot().endpointInterface())) {

                    DescriptionBuilderComposite seic =
                            dbcMap.get(composite.getWebServiceAnnot().endpointInterface());

                    //Verify that we can find the SEI in the composite list
                    if (seic == null) {
                        throw ExceptionFactory.makeWebServiceException(
                        		Messages.getMessage("validateIntegrityErr8",composite.getClassName(),
                        				composite.getWebServiceAnnot().endpointInterface()));
                    }

                    // Verify that the only class annotations are WebService and HandlerChain
                    // (per JSR181 Sec. 3.1).  Note that this applies to JSR-181 annotations; the restriction
                    // does not apply to JSR-224 annotations such as BindingType
                    if (composite.getSoapBindingAnnot() != null
                            || composite.getWebFaultAnnot() != null
                            || composite.getWebServiceClientAnnot() != null
                            ) {
                        throw ExceptionFactory.makeWebServiceException(
                        		Messages.getMessage("validateIntegrityErr9",composite.getClassName()));
                    }

                    //Verify that WebService annotation does not contain a name attribute
                    //(per JSR181 Sec. 3.1)
                    if (!DescriptionUtils.isEmpty(composite.getWebServiceAnnot().name())) {
                        throw ExceptionFactory.makeWebServiceException(
                        		Messages.getMessage("validateIntegrityErr10",composite.getClassName(),
                        				composite.getWebServiceAnnot().name()));
                    }

                    validateSEI(seic);
                    //Verify that that this implementation class implements all methods in the interface
                    validateImplementation(seic);

                    //Verify that this impl. class does not contain any @WebMethod annotations
                    if (webMethodAnnotationsExist()) {
                        throw ExceptionFactory.makeWebServiceException(
                        		Messages.getMessage("validateIntegrityErr11",composite.getClassName()));
                    }


                } else { //this is an implicit SEI (i.e. impl w/out endpointInterface

                    
                    checkImplicitSEIAgainstWSDL();
                    //	TODO:	Call ValidateWebMethodAnnots()
                    //			- this method will check that all methods are public - ???
                    //
                }
            } else { //this is an interface...we should not be processing interfaces here
                throw ExceptionFactory.makeWebServiceException(
                		Messages.getMessage("validateIntegrityErr12",composite.getClassName()));
            }

            // Verify that the SOAPBinding annotations are supported.
            //REVIEW: The assumption here is that SOAPBinding annotation can be place on an impl. class
            //        (implicit SEI) or a WebServiceProvider
            if (composite.getSoapBindingAnnot() != null) {
                if (composite.getSoapBindingAnnot().use() == javax.jws.soap.SOAPBinding.Use.ENCODED) {
                    throw ExceptionFactory.makeWebServiceException(
                    		Messages.getMessage("validateIntegrityErr13",composite.getClassName()));  
                }
            }
            
            //TODO: don't think this is necessary
            checkMethodsAgainstWSDL();
        }
    }

    /**
     * Validate there is an invoke method on the composite.
     *
     * @return
     */
    private boolean validateInvokeMethod() {
        boolean validInvokeMethod = false;
        List<MethodDescriptionComposite> invokeMethodList =
                composite.getMethodDescriptionComposite("invoke");
        if (invokeMethodList != null && !invokeMethodList.isEmpty()) {
            validInvokeMethod = true;
        }
        return validInvokeMethod;
    }

    /**
     * Validate that, if using PAYLOAD mode, then interfaces list cannot contain SOAPMessage or
     * DataSource
     *
     * @return
     */
    private void validateProviderInterfaces() {

        // Default for ServiceMode is 'PAYLOAD'. So, if it is specified  (explicitly or
        // implicitly) then verify that we are not implementing improper interfaces)
        if ((composite.getServiceModeAnnot() == null)
                || composite.getServiceModeAnnot().value() == javax.xml.ws.Service.Mode.PAYLOAD) {

            Iterator<String> iter = composite.getInterfacesList().iterator();

            while (iter.hasNext()) {
                String interfaceString = iter.next();
                if (interfaceString.equals(MDQConstants.PROVIDER_SOAP)
                        || interfaceString.equals(MDQConstants.PROVIDER_DATASOURCE)) {
                	
                    throw ExceptionFactory.makeWebServiceException(
                    		Messages.getMessage("validatePIsErr1",composite.getClassName()));
                }
            }

        } else {
            // We are in MESSAGE mode
            // Conformance: JAXWS Spec.- Sec. 4.3 (javax.activation.DataSource)

            // REVIEW: Should the provider interface validation be moved to post-construction validation, 
            // since it seems that the logic to understand the default values for binding type 
            // (see comment below) should be left to the creation of the Description objects.
            String bindingType = null;
            if (composite.getBindingTypeAnnot() != null) {
                bindingType = composite.getBindingTypeAnnot().value();
            }

            Iterator<String> iter = composite.getInterfacesList().iterator();

            while (iter.hasNext()) {
                String interfaceString = iter.next();

                if (interfaceString.equals(MDQConstants.PROVIDER_SOAP)) {

                    // Make sure BindingType is SOAP/HTTP with SOAPMessage
					// object, Default for Binding Type is SOAP/HTTP
					if (!DescriptionUtils.isEmpty(bindingType)
							&& !bindingType
									.equals(SOAPBinding.SOAP11HTTP_BINDING)
							&& !bindingType
									.equals(SOAPBinding.SOAP11HTTP_MTOM_BINDING)
							&& !bindingType
									.equals(SOAPBinding.SOAP12HTTP_BINDING)
							&& !bindingType
									.equals(SOAPBinding.SOAP12HTTP_MTOM_BINDING)
							&& !bindingType
									.equals(MDQConstants.SOAP11JMS_BINDING)
							&& !bindingType
									.equals(MDQConstants.SOAP11JMS_MTOM_BINDING)
							&& !bindingType
									.equals(MDQConstants.SOAP12JMS_BINDING)
							&& !bindingType
									.equals(MDQConstants.SOAP12JMS_MTOM_BINDING))

						throw ExceptionFactory.makeWebServiceException(Messages
								.getMessage("validatePIsErr2", composite
										.getClassName()));
                
                
                
                } else if (interfaceString
                        .equals(MDQConstants.PROVIDER_DATASOURCE)) {

                    // Make sure BindingType is XML/HTTP with DataSource object
                    if (DescriptionUtils.isEmpty(bindingType)
                            || !bindingType
                            .equals(javax.xml.ws.http.HTTPBinding.HTTP_BINDING))
                    	
                        throw ExceptionFactory.makeWebServiceException(
                        		Messages.getMessage("validatePIsErr3",composite.getClassName()));
                }
            }
        }
    }


    /**
     * Validate there is a default no-argument constructor on the composite.
     *
     * @return
     */
    private boolean validateDefaultConstructor() {
        boolean validDefaultCtor = false;
        List<MethodDescriptionComposite> constructorList =
                composite.getMethodDescriptionComposite("<init>");
        if (constructorList != null && !constructorList.isEmpty()) {
            // There are public constructors; make sure there is one that takes no arguments.
            for (MethodDescriptionComposite checkCtor : constructorList) {
                List<ParameterDescriptionComposite> paramList =
                        checkCtor.getParameterDescriptionCompositeList();
                if (paramList == null || paramList.isEmpty()) {
                    validDefaultCtor = true;
                    break;
                }
            }
        }

        return validDefaultCtor;
    }

    private void validateImplementation(DescriptionBuilderComposite seic) {
        /*
         *  Verify that an impl class implements all the methods of the SEI. We have to verify this 
         *  because an impl class is not required to actually use the 'implements' clause. So, if 
         *  it doesn't, the Java compiler won't catch it. Don't need to worry about chaining 
         *  because only one EndpointInterface can be specified, and the SEI cannot specify an 
         *  EndpointInterface, so the Java compiler will take care of everything else.
         *  
         *  Note, however, that we do need to take overloaded methods into a consideration.  The
         *  same method name can be specified for multiple methods, but they can have different
         *  parameters.  Note that methods which differ only in the return type or the exceptions
         *  thrown are not overloaded (and therefore would cause a compile error).
         */

        List<MethodDescriptionComposite> implMethods = composite.getMethodDescriptionsList();
        // Add methods declared in the implementation's superclass
        addSuperClassMethods(implMethods, composite);

        List<MethodDescriptionComposite> seiMethods = seic.getMethodDescriptionsList();
        // Add any methods declared in superinterfaces of the SEI
        addSuperClassMethods(seiMethods, seic);

        // Make sure all the methods in the SEI (including any inherited from superinterfaces) are
        // implemented by the bean (including inherited methods on the bean), taking into
        // account overloaded methods.
        Iterator<MethodDescriptionComposite> verifySEIIterator = seiMethods.iterator();
        while (verifySEIIterator.hasNext()) {
            MethodDescriptionComposite seiMDC = verifySEIIterator.next();
            
            // Make sure the implementation implements this SEI method.  Since we have to account
            // for method overloading, we look for ALL methods with the same name in the 
            // implementation, then from that collection of methods, we look for one that has the 
            // same parameters.  If we find one with the same parameters, then we check the return
            // and exceptions.  Note that in Java, overloaded methods are ones that have the same
            // name but different parameters; a difference in the return type or thrown exceptions
            // does not constitute overloading and is a compile error.
            Iterator<MethodDescriptionComposite> implMDCIterator = implMethods.iterator();
            boolean methodImplFound = false;
            while (implMDCIterator.hasNext()) {
                MethodDescriptionComposite implMDC = implMDCIterator.next();
                
                if (seiMDC.getMethodName().equals(implMDC.getMethodName())) {
                    // The method names match, so now check the parameters
                    try {
                        validateMethodParameters(seiMDC, implMDC, seic.getClassName());
                        methodImplFound = true;
                    }
                    catch (Exception ex) {
                        // The parameters didn't match, so we'll check the next 
                        // implemntation method on the next iteration of the inner loop.
                    }
                    
                    // If the name and the parameters matched, then we've found the method
                    // implementation, even if it was overloaded. Now check the return value and
                    // thrown exceptions.  Note these will methods throw exceptions if validation fails.
                    // If all the validation passes, we can break out of the inner loop since we 
                    // found the implementation for this sei method.
                    if (methodImplFound) {
                        validateMethodExceptions(seiMDC, implMDC, seic.getClassName());
                        validateMethodReturnValue(seiMDC, implMDC, seic.getClassName());
                        break;
                    }
                }
            }
            
            if (!methodImplFound) {
                // We didn't find the implementation for this SEI method, so throw a validation
                // exception.
                throw ExceptionFactory.makeWebServiceException(
                		Messages.getMessage("validateImplErr",composite.getClassName(),
                				seiMDC.getMethodName(),seic.getClassName()));
            }
        }
    }

    private void validateMethodParameters(MethodDescriptionComposite seiMDC,
        MethodDescriptionComposite implMDC, String className) {
        List<ParameterDescriptionComposite> seiPDCList = seiMDC
            .getParameterDescriptionCompositeList();
        List<ParameterDescriptionComposite> implPDCList = implMDC
            .getParameterDescriptionCompositeList();
        if ((seiPDCList == null || seiPDCList.isEmpty())
            && (implPDCList == null || implPDCList.isEmpty())) {
            // There are no parameters on the SEI or the impl; all is well
        } else if ((seiPDCList == null || seiPDCList.isEmpty())
            && !(implPDCList == null || implPDCList.isEmpty())) {
            throw ExceptionFactory.makeWebServiceException(
            		Messages.getMessage("validateMethodParamErr1",implPDCList.toString(),
            				composite.getClassName(),seiMDC.getMethodName(),className));
        } else if ((seiPDCList != null && !seiPDCList.isEmpty())
            && !(implPDCList != null && !implPDCList.isEmpty())) {
            throw ExceptionFactory.makeWebServiceException(
            		Messages.getMessage("validateMethodParamErr2",seiPDCList.toString(),
            				composite.getClassName(),seiMDC.getMethodName(),className));
        } else if (seiPDCList.size() != implPDCList.size()) {
            throw ExceptionFactory.makeWebServiceException(
            		Messages.getMessage("validateMethodParamErr3",
            				new Integer(seiPDCList.size()).toString(),
            				new Integer(implPDCList.size()).toString(),composite.getClassName(),
            				seiMDC.getMethodName(),className));
        } else {
            // Make sure the order and type of parameters match
            // REVIEW: This checks for strict equality of the fully qualified
            // type. It does not
            // take into consideration object hierachy. For example foo(Animal)
            // will not equal bar(Zebra)
            boolean parametersMatch = true;
            String failingMessage = null;
            for (int paramNumber = 0; paramNumber < seiPDCList.size(); paramNumber++) {
                String seiParamType = seiPDCList.get(paramNumber).getParameterType();
                String implParamType = implPDCList.get(paramNumber).getParameterType();
                if (!seiParamType.equals(implParamType)) {
                    parametersMatch = false;
                    String[] inserts = new String[] {
                            String.valueOf(paramNumber), 
                            seiParamType,
                            implParamType,
                            composite.getClassName(),
                            seiMDC.getMethodName(),
                            className
                    };
                    
                    failingMessage = Messages.getMessage("serviceDescriptionImplValidationErr",
                                                         inserts);
                                                         
                    break;
                }
            }
            if (!parametersMatch) {
                throw ExceptionFactory.makeWebServiceException(failingMessage);
            }
        }
    }

    private void validateMethodReturnValue(MethodDescriptionComposite seiMDC,
        MethodDescriptionComposite implMDC, String className) {
        String seiReturnValue = seiMDC.getReturnType();
        String implReturnValue = implMDC.getReturnType();

        if (seiReturnValue == null && implReturnValue == null) {
            // Neither specify a return value; all is well
        } else if (seiReturnValue == null && implReturnValue != null) {
            throw ExceptionFactory.makeWebServiceException(
            		Messages.getMessage("validateMethodRVErr1",implReturnValue,
            				composite.getClassName(),seiMDC.getMethodName(),className));
        } else if (seiReturnValue != null && implReturnValue == null) {
            throw ExceptionFactory.makeWebServiceException(
            		Messages.getMessage("validateMethodRVErr2",seiReturnValue,
            				composite.getClassName(),seiMDC.getMethodName(),className));
        } else if (!seiReturnValue.equals(implReturnValue)) {
            throw ExceptionFactory.makeWebServiceException(
            		Messages.getMessage("validateMethodRVErr3",seiReturnValue,implReturnValue,
            				composite.getClassName(),seiMDC.getMethodName(),className));
        }

    }

    private void validateMethodExceptions ( MethodDescriptionComposite seiMDC, 
        MethodDescriptionComposite implMDC,
        String className) {

        String[] seiExceptions = seiMDC.getExceptions();
        String[] implExceptions = implMDC.getExceptions();

        // An impl can choose to throw fewer checked exceptions than declared on the SEI, but not more.
        // This is analagous to the Java rules for interfaces.
        if (seiExceptions == null) {
            if (implExceptions == null) {
                return;
            } else {
                // SEI delcares no checked exceptions, but the implementation has checked exceptions, which is an error
                throw ExceptionFactory.makeWebServiceException(
                		Messages.getMessage("validateMethodExceptionErr1",
                				composite.getClassName(),seiMDC.getMethodName(),className));
            }
        } else if (implExceptions == null) {
            // Implementation throws fewer checked exceptions than SEI, which is OK.
            return;
        }
        
        // Check the list length; An implementation can not declare more exceptions than the SEI
        if (seiExceptions.length < implExceptions.length) {
            throw ExceptionFactory.makeWebServiceException(
            		Messages.getMessage("validateMethodExceptionErr2",
            				new Integer(implExceptions.length).toString(),
            				new Integer(seiExceptions.length).toString(),
            				composite.getClassName(),seiMDC.getMethodName(),className));
        }
        
        // Make sure that each checked exception declared by the 
        // implementation is on the SEI also
        if (implExceptions.length > 0) {                
            for (String implException : implExceptions) {
                boolean foundIt = false;
                if (seiExceptions.length > 0) {         
                    for (String seiException : seiExceptions) {
                        if (seiException.equals(implException)) {
                            foundIt = true;
                            break;
                        }
                    }
                }
                
                if (!foundIt) {    	
                    throw ExceptionFactory.makeWebServiceException(
                    		Messages.getMessage("validateMethodExceptionErr3",implException,
                    				composite.getClassName(),seiMDC.getMethodName(),className));
                }
            }
        }

    }

    /**
     * Adds any methods declared in superclasses to the List.  The hierachy starting with the DBC
     * will be walked up recursively, adding methods from each parent DBC encountered.
     * <p/>
     * Note that this can be used for either classes or interfaces.
     *
     * @param methodList The current collection of methods, including overloaded ones
     * @param dbc The composite to be checked for methods to be added to the collection
     */
    private void addSuperClassMethods(List<MethodDescriptionComposite> methodList, DescriptionBuilderComposite dbc) {
        DescriptionBuilderComposite superDBC = dbcMap.get(dbc.getSuperClassName());
        if (superDBC != null) {
            Iterator<MethodDescriptionComposite> mIter = superDBC.getMethodDescriptionsList().iterator();
            while (mIter.hasNext()) {
                MethodDescriptionComposite mdc = mIter.next();
                methodList.add(mdc);
            }
            addSuperClassMethods(methodList, superDBC);
        }
    }


    /*
      * This method verifies that, if there are any WebMethod with exclude == false, then
      * make sure that we find all of those methods represented in the wsdl. However, if
      * there are no exclusions == false, or there are no WebMethod annotations, then verify
      * that all the public methods are in the wsdl
      */
    private void checkMethodsAgainstWSDL() {
//Verify that, for ImplicitSEI, that all methods that should exist(if one false found, then
//only look for WebMethods w/ False, else take all public methods but ignore those with
//exclude == true
        if (webMethodAnnotationsExist()) {
            if (DescriptionUtils.falseExclusionsExist(composite))
                verifyFalseExclusionsWithWSDL();
            else
                verifyPublicMethodsWithWSDL();
        } else {
            verifyPublicMethodsWithWSDL();
        }
    }

    private void checkImplicitSEIAgainstWSDL() {

        //TODO: If there is a WSDL, then verify that all WebMethods on this class and in the
        //		superclasses chain are represented in the WSDL...Look at logic below to make
        //		sure this really happening


        if (webMethodAnnotationsExist()) {
            if (DescriptionUtils.falseExclusionsExist(composite))
                verifyFalseExclusionsWithWSDL();
            else
                verifyPublicMethodsWithWSDL();
        } else {
            verifyPublicMethodsWithWSDL();
        }

    }

    private void checkSEIAgainstWSDL() {
        //TODO: Place logic here to verify that each publicMethod with WebMethod annot
        //      is contained in the WSDL (If there is a WSDL) If we find
        //	    a WebMethod annotation, use its values for looking in the WSDL

    }

    private void validateSEI(DescriptionBuilderComposite seic) {

        //TODO: Validate SEI superclasses -- hmmm, may be doing this below
        //
        if (seic.getWebServiceAnnot() == null) {
            throw ExceptionFactory.makeWebServiceException(
            		Messages.getMessage("validateSEIErr1",composite.getClassName(),seic.getClassName()));
        }
        if (!seic.getWebServiceAnnot().endpointInterface().equals("")) {
            throw ExceptionFactory.makeWebServiceException(
            		Messages.getMessage("validateSEIErr2",composite.getClassName(),
            				seic.getClassName(),seic.getWebServiceAnnot().endpointInterface()));
        }
        // Verify that the SOAPBinding annotations are supported.
        if (seic.getSoapBindingAnnot() != null &&
        		seic.getSoapBindingAnnot().use() == javax.jws.soap.SOAPBinding.Use.ENCODED) {
        	throw ExceptionFactory.makeWebServiceException(Messages.getMessage("validateSEIErr3",seic.getClassName()));  
        }

        checkSEIAgainstWSDL();

        //TODO: More validation here

        //TODO: Make sure we don't find any WebMethod annotations with exclude == true
        //		anywhere in the superclasses chain

        //TODO: Check that all WebMethod annotations in the superclass chain are represented in
        //		WSDL, assuming there is WSDL

        //TODO:	Validate that the interface is public

        //		Call ValidateWebMethodAnnots()
        //

        //This will perform validation for all methods, regardless of WebMethod annotations
        //It is called for the SEI, and an impl. class that does not specify an endpointInterface
        validateMethods(seic.getMethodDescriptionsList());
    }

    /** @return Returns TRUE if we find just one WebMethod Annotation */
    private boolean webMethodAnnotationsExist() {
        MethodDescriptionComposite mdc = null;
        Iterator<MethodDescriptionComposite> iter =
                composite.getMethodDescriptionsList().iterator();

        while (iter.hasNext()) {
            mdc = iter.next();

            if (mdc.getWebMethodAnnot() != null)
                return true;
        }

        return false;
    }

    private void verifyFalseExclusionsWithWSDL() {
        //TODO: Place logic here to verify that each exclude==false WebMethod annot we find
        //      is contained in the WSDL
    }

    private void verifyPublicMethodsWithWSDL() {
        //TODO: Place logic here to verify that each publicMethod with no WebMethod annot
        //      is contained in the WSDL

    }


    private void validateMethods(List<MethodDescriptionComposite> mdcList) {
        if (mdcList != null && !mdcList.isEmpty()) {
            for (MethodDescriptionComposite mdc : mdcList) {
                String returnType = mdc.getReturnType();
                if (returnType != null
                                && (returnType.equals(RETURN_TYPE_FUTURE) || returnType
                                                .equals(RETURN_TYPE_RESPONSE))) {
                    throw ExceptionFactory.makeWebServiceException(Messages
                                    .getMessage("serverSideAsync", mdc.getDeclaringClass(), mdc
                                                    .getMethodName()));
                }
                // Verify that the SOAPBinding annotation values are supported.
                if (mdc.getSoapBindingAnnot() != null) {

                    // For this JAXWS engine, SOAPBinding.Use = ENCODED is unsupported
                    if (mdc.getSoapBindingAnnot().use() == javax.jws.soap.SOAPBinding.Use.ENCODED) {
                        throw ExceptionFactory.
                          makeWebServiceException(Messages.getMessage("soapBindingUseEncoded",
                                                                      mdc.getDeclaringClass(),
                                                                      mdc.getMethodName()));

                    }

                    // Verify that, if a SOAPBinding annotation exists, that its style be set to
                    // only DOCUMENT JSR181-Sec 4.7.1
                    if (mdc.getSoapBindingAnnot().style() == javax.jws.soap.SOAPBinding.Style.RPC) {
                        throw ExceptionFactory.
                          makeWebServiceException(Messages.getMessage("soapBindingStyle",
                                                                      mdc.getDeclaringClass(),
                                                                      mdc.getMethodName()));
                    }

                } 
            }
        }
        // TODO: Fill this out to validate all MethodDescriptionComposite (and
        // their inclusive
        //      annotations on this SEI (SEI is assumed here)
        //check oneway
        //

        //This could be an SEI, or an impl. class that doesn' specify an EndpointInterface (so, it
        //is implicitly an SEI...need to consider this
        //

        //TODO: Verify that, if this is an interface...that there are no Methods with WebMethod
        //      annotations that contain exclude == true

        //TODO: Verify that, if a SOAPBinding annotation exists, that its style be set to
        //      only DOCUMENT JSR181-Sec 4.7.1

    }

    private void validateWSDLOperations() {
        //Verifies that all operations on the wsdl are found in the impl/sei class
    }

    public boolean isWSDLSpecified() {
        boolean wsdlSpecified = false;
        if (getWSDLWrapper() != null) {
            wsdlSpecified = (getWSDLWrapper().getDefinition() != null);
        }
        return wsdlSpecified;
    }

    
    // ===========================================
    // ANNOTATION: HandlerChain
    // ===========================================

    /* (non-Javadoc)
     * @see org.apache.axis2.jaxws.description.ServiceDescription#getHandlerChain()
     */
    public HandlerChainsType getHandlerChain() {
        return getHandlerChain(null);
    }
    
    /* (non-Javadoc)
     * @see org.apache.axis2.jaxws.description.ServiceDescription#getHandlerChain(java.lang.Object)
     */
    public HandlerChainsType getHandlerChain(Object sparseCompositeKey) {
        // If there is a HandlerChainsType in the sparse composite for this ServiceDelegate
        // (i.e. this sparseCompositeKey), then return that.
        if (sparseCompositeKey != null) {
            DescriptionBuilderComposite sparseComposite = composite.getSparseComposite(sparseCompositeKey);
            if (sparseComposite != null && sparseComposite.getHandlerChainsType() != null) {
                return sparseComposite.getHandlerChainsType();
            }
        }
        
        // If there is no HandlerChainsType in the composite, then read in the file specified
        // on the HandlerChain annotation if it is present.
        if (handlerChainsType == null) {

            getAnnoHandlerChainAnnotation();
            if (handlerChainAnnotation != null) {

                String handlerFileName = handlerChainAnnotation.file();

                if (log.isDebugEnabled()) {
                    log.debug("EndpointDescriptionImpl.getHandlerChain: fileName: "
                              + handlerFileName + " className: " + composite.getClassName());
                }

                String className = composite.getClassName();

                ClassLoader classLoader = composite.getClassLoader();

                InputStream is =
                        DescriptionUtils.openHandlerConfigStream(handlerFileName,
                                                                 className,
                                                                 classLoader);

                if(is == null) {
                    log.warn("Unable to load handlers from file: " + handlerFileName);
                }
                else {
                    handlerChainsType =
                        DescriptionUtils.loadHandlerChains(is, 
                              getClassLoader(this.getClass()));
                }
            }
        }
        return handlerChainsType;
    }

    
    /*
     * This is a client side only method. The generated service class may contain 
     * handler chain annotations
     */
    public HandlerChain getAnnoHandlerChainAnnotation() {
        if (this.handlerChainAnnotation == null) {
            Class serviceClass = composite.getCorrespondingClass();
                if (serviceClass != null) {
                    handlerChainAnnotation =
                            (HandlerChain) getAnnotation(serviceClass, HandlerChain.class);
            }
        }

        return handlerChainAnnotation;
    }
    
    /* Returns the WSDL definiton as specified in the metadata.  Note that this WSDL may not be
     * complete.
     */
    public Definition getWSDLDefinition() {
        Definition defn = null;
        if (getWSDLWrapper() != null) {
            defn = getWSDLWrapper().getDefinition();
        }
        return defn;
    }

    /**
     * Returns the WSDL definiton as created by calling the WSDL generator.  This will be null
     * unless the WSDL definition provided by the metadata is incomplete
     */
    public Definition getWSDLGeneratedDefinition() {
        Definition defn = null;
        if (getGeneratedWsdlWrapper() != null) {
            defn = getGeneratedWsdlWrapper().getDefinition();
        }
        return defn;
    }

    public Service getWSDLService() {
        Service returnWSDLService = null;
        Definition defn = getWSDLDefinition();
        if (defn != null) {
            returnWSDLService = defn.getService(getServiceQName());
        }
        return returnWSDLService;
    }

    public Map getWSDLPorts() {
        Service wsdlService = getWSDLService();
        if (wsdlService != null) {
            return wsdlService.getPorts();
        } else {
            return null;
        }
    }

    public List<QName> getPorts(Object serviceDelegateKey) {
        ArrayList<QName> portList = new ArrayList<QName>();
        // Note that we don't cache these results because the list of ports can be added
        // to via getPort(...) and addPort(...).

        // If the WSDL is specified, get the list of ports under this service
        Map wsdlPortsMap = getWSDLPorts();
        if (wsdlPortsMap != null) {
            Iterator wsdlPortsIterator = wsdlPortsMap.values().iterator();
            // Note that the WSDL Ports do not have a target namespace associated with them.
            // JAXWS says to use the TNS from the Service.
            String serviceTNS = getServiceQName().getNamespaceURI();
            for (Port wsdlPort = null; wsdlPortsIterator.hasNext();) {
                wsdlPort = (Port)wsdlPortsIterator.next();
                String wsdlPortLocalPart = wsdlPort.getName();
                portList.add(new QName(serviceTNS, wsdlPortLocalPart));
            }
        }

        // Go through the list of Endpoints that have been created and add any
        // not already in the list.  This will include ports added to the Service
        // via getPort(...) and addPort(...)
        Collection<EndpointDescription> endpointDescs = getEndpointDescriptions_AsCollection();
        for (EndpointDescription endpointDesc : endpointDescs) {
            QName endpointPortQName = endpointDesc.getPortQName();
            if (!portList.contains(endpointPortQName)) {
                portList.add(endpointPortQName);
            }
        }
        
        //Retrieve all the dynamic ports for this client
        if (serviceDelegateKey != null) {
			Collection<EndpointDescriptionImpl> dynamicEndpointDescs = getDynamicEndpointDescriptions_AsCollection(serviceDelegateKey);
			if (dynamicEndpointDescs != null) {
				for (EndpointDescription dynamicEndpointDesc : dynamicEndpointDescs) {
					QName endpointPortQName = dynamicEndpointDesc
							.getPortQName();
					if (!portList.contains(endpointPortQName)) {
						portList.add(endpointPortQName);
					}
				}
			}
		}
        return portList;
    }

    public List<Port> getWSDLPortsUsingPortType(QName portTypeQN) {
        ArrayList<Port> portList = new ArrayList<Port>();
        if (!DescriptionUtils.isEmpty(portTypeQN)) {
            Map wsdlPortMap = getWSDLPorts();
            if (wsdlPortMap != null && !wsdlPortMap.isEmpty()) {
                for (Object mapElement : wsdlPortMap.values()) {
                    Port wsdlPort = (Port)mapElement;
                    PortType wsdlPortType = wsdlPort.getBinding().getPortType();
                    QName wsdlPortTypeQN = wsdlPortType.getQName();
                    if (portTypeQN.equals(wsdlPortTypeQN)) {
                        portList.add(wsdlPort);
                    }
                }
            }
        }
        return portList;
    }

    public List<Port> getWSDLPortsUsingSOAPAddress(List<Port> wsdlPorts) {
        ArrayList<Port> portsUsingAddress = new ArrayList<Port>();
        if (wsdlPorts != null && !wsdlPorts.isEmpty()) {
            for (Port checkPort : wsdlPorts) {
                List extensibilityElementList = checkPort.getExtensibilityElements();
                for (Object checkElement : extensibilityElementList) {
                    if (EndpointDescriptionImpl
                            .isSOAPAddressElement((ExtensibilityElement)checkElement)) {
                        portsUsingAddress.add(checkPort);
                    }
                }
            }
        }
        return portsUsingAddress;
    }

    public ServiceRuntimeDescription getServiceRuntimeDesc(String name) {
        return runtimeDescMap.get(name);
    }

    public void setServiceRuntimeDesc(ServiceRuntimeDescription srd) {
        runtimeDescMap.put(srd.getKey(), srd);
    }
    
    private void resetServiceRuntimeDescription() {
        runtimeDescMap.clear();
    }
    
    /**
     * Return the name of the client-side service class if it exists.
     */
    protected String getServiceClassName() {
        return composite.getClassName();
    }

    private EndpointDescriptionImpl getDynamicEndpointDescriptionImpl(QName portQName, Object key) {
        Map<QName, EndpointDescriptionImpl> innerMap = null;
        synchronized(dynamicEndpointDescriptions) {
        	innerMap = dynamicEndpointDescriptions.get(key);
            if (innerMap != null) {
            	return innerMap.get(portQName);
            }
        }
        return null;
    }

    private void addDynamicEndpointDescriptionImpl(EndpointDescriptionImpl endpointDescriptionImpl, 
    												Object key) {
        Map<QName, EndpointDescriptionImpl> innerMap = null;
        synchronized(dynamicEndpointDescriptions) {
            innerMap = dynamicEndpointDescriptions.get(key);
            if (innerMap == null) {
               innerMap = new HashMap<QName, EndpointDescriptionImpl>();
               dynamicEndpointDescriptions.put(key, innerMap);
            }
            innerMap.put(endpointDescriptionImpl.getPortQName(), endpointDescriptionImpl);
        }
    }
    
    /** Return a string representing this Description object and all the objects it contains. */
    public String toString() {
        final String newline = "\n";
        final String sameline = "; ";
        // This produces a TREMENDOUS amount of output if we have the WSDL Definition objects 
        // do a toString on themselves.
        boolean dumpWSDLContents = false;
        StringBuffer string = new StringBuffer();
        try {
            // Basic information
            string.append(super.toString());
            string.append(newline);
            string.append("ServiceQName: " + getServiceQName());
            // WSDL information
            string.append(newline);
            string.append("isWSDLSpecified: " + isWSDLSpecified());
            string.append(sameline);
            string.append("WSDL Location: " + getWSDLLocation());
            string.append(newline);
            if (dumpWSDLContents) {
                string.append("WSDL Definition: " + getWSDLDefinition());
                string.append(newline);
                string.append("Generated WSDL Definition: " + getWSDLGeneratedDefinition());
            } else {
                string.append("WSDL Definition available: " + (getWSDLDefinition() != null));
                string.append(sameline);
                string.append("Generated WSDL Definition available: " +
                        (getWSDLGeneratedDefinition() != null));
            }
            // Ports
            string.append(newline);
            List<QName> ports = getPorts(null);
            string.append("Number of defined ports: " + ports.size());
            //TODO: Show the map that contains the dynamic ports
            string.append(newline);
            string.append("Port QNames: ");
            for (QName port : ports) {
                string.append(port + sameline);
            }
            // Axis Config information
            // We don't print out the config context because it will force one to be created
            // if it doesn't already exist.
            // string.append(newline);
            // string.append("ConfigurationContext: " + getAxisConfigContext());
            // EndpointDescriptions
            string.append(newline);
            Collection<EndpointDescription> endpointDescs = getEndpointDescriptions_AsCollection();
            if (endpointDescs == null) {
                string.append("EndpointDescription array is null");
            }
            else {
                string.append("Number of EndpointDescrptions: " + endpointDescs.size());
                string.append(newline);
                for (EndpointDescription endpointDesc : endpointDescs) {
                    string.append(endpointDesc.toString());
                    string.append(newline);
                }
            }
            string.append("RuntimeDescriptions:" + this.runtimeDescMap.size());
            string.append(newline);
            for (ServiceRuntimeDescription runtimeDesc : runtimeDescMap.values()) {
                string.append(runtimeDesc.toString());
                string.append(newline);
            }
        }
        catch (Throwable t) {
            string.append(newline);
            string.append("Complete debug information not currently available for " +
                    "ServiceDescription");
            return string.toString();
        }
        return string.toString();

    }
    /**
     * Get an annotation.  This is wrappered to avoid a Java2Security violation.
     * @param cls Class that contains annotation 
     * @param annotation Class of requrested Annotation
     * @return annotation or null
     */
    private static Annotation getAnnotation(final Class cls, final Class annotation) {
        return (Annotation) AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                return cls.getAnnotation(annotation);
            }
        });
    }

    private static ClassLoader getContextClassLoader(final ClassLoader classLoader) {
        ClassLoader cl;
        try {
            cl = (ClassLoader) AccessController.doPrivileged(
                    new PrivilegedExceptionAction() {
                        public Object run() throws ClassNotFoundException {
                            return classLoader != null ? classLoader : Thread.currentThread().getContextClassLoader();
                        }
                    }
            );
        } catch (PrivilegedActionException e) {
            if (log.isDebugEnabled()) {
                log.debug("Exception thrown from AccessController: " + e.getMessage(), e);
            }
            throw ExceptionFactory.makeWebServiceException(e.getException());
        }

        return cl;
    }
    
    private static ClassLoader getClassLoader(final Class cls) {
        ClassLoader cl = null;
        try {
            cl = (ClassLoader) AccessController.doPrivileged(
                    new PrivilegedExceptionAction() {
                        public Object run() throws ClassNotFoundException {
                            return cls.getClassLoader();
                        }
                    }
            );
        } catch (PrivilegedActionException e) {
            if (log.isDebugEnabled()) {
                log.debug("Exception thrown from AccessController: " + e);
            }
            throw ExceptionFactory.makeWebServiceException(e.getException());
        }

        return cl;
    }

    public List<Class> getHandlerChainClasses(PortInfo portinfo) {
        return handlerClassesMap.get(portinfo);
    }

    public void setHandlerChainClasses(PortInfo portinfo, List<Class> handlerClasses) {
        handlerClassesMap.put(portinfo, handlerClasses);
    }

}
