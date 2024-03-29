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
package org.apache.axis2.jaxws.message.util.impl;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.util.StAXUtils;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.soap.impl.builder.StAXSOAPModelBuilder;
import org.apache.axis2.jaxws.ExceptionFactory;
import org.apache.axis2.jaxws.i18n.Messages;
import org.apache.axis2.jaxws.message.util.SAAJConverter;
import org.apache.axis2.jaxws.message.util.SOAPElementReader;
import org.apache.axis2.jaxws.utility.SAAJFactory;
import org.apache.axis2.util.XMLUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.namespace.QName;
import javax.xml.soap.Detail;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.ws.WebServiceException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import java.util.Iterator;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;

/** SAAJConverterImpl Provides an conversion methods between OM<->SAAJ */
public class SAAJConverterImpl implements SAAJConverter {

    private static final Log log = LogFactory.getLog(SAAJConverterImpl.class);

    /** Constructed via SAAJConverterFactory */
    SAAJConverterImpl() {
        super();
    }

    /* (non-Javadoc)
      * @see org.apache.axis2.jaxws.message.util.SAAJConverter#toSAAJ(org.apache.axiom.soap.SOAPEnvelope)
      */
    public SOAPEnvelope toSAAJ(org.apache.axiom.soap.SOAPEnvelope omEnvelope)
            throws WebServiceException {
        SOAPEnvelope soapEnvelope = null;
        try {
            // Build the default envelope
            OMNamespace ns = omEnvelope.getNamespace();
            MessageFactory mf = createMessageFactory(ns.getNamespaceURI());
            SOAPMessage sm = mf.createMessage();
            SOAPPart sp = sm.getSOAPPart();
            soapEnvelope = sp.getEnvelope();

            // The getSOAPEnvelope() call creates a default SOAPEnvelope with a SOAPHeader and SOAPBody.
            // The SOAPHeader and SOAPBody are removed (they will be added back in if they are present in the
            // OMEnvelope).
            SOAPBody soapBody = soapEnvelope.getBody();
            if (soapBody != null) {
                soapBody.detachNode();
            }
            SOAPHeader soapHeader = soapEnvelope.getHeader();
            if (soapHeader != null) {
                soapHeader.detachNode();
            }

            // We don't know if there is a real OM tree or just a backing XMLStreamReader.
            // The best way to walk the data is to get the XMLStreamReader and use this
            // to build the SOAPElements
            XMLStreamReader reader = omEnvelope.getXMLStreamReader();

            NameCreator nc = new NameCreator(soapEnvelope);
            buildSOAPTree(nc, soapEnvelope, null, reader, false);
        } catch (WebServiceException e) {
            throw e;
        } catch (SOAPException e) {
            throw ExceptionFactory.makeWebServiceException(e);
        }
        return soapEnvelope;
    }

    /* (non-Javadoc)
      * @see org.apache.axis2.jaxws.message.util.SAAJConverter#toOM(javax.xml.soap.SOAPEnvelope)
      */
    public org.apache.axiom.soap.SOAPEnvelope toOM(SOAPEnvelope saajEnvelope)
            throws WebServiceException {
        // Get a XMLStreamReader backed by a SOAPElement tree
        XMLStreamReader reader = new SOAPElementReader(saajEnvelope);
        // Get a SOAP OM Builder.  Passing null causes the version to be automatically triggered
        StAXSOAPModelBuilder builder = new StAXSOAPModelBuilder(reader, null);
        // Create and return the OM Envelope
        org.apache.axiom.soap.SOAPEnvelope omEnvelope = builder.getSOAPEnvelope();
        
        // TODO The following statement expands the OM tree.  This is 
        // a brute force workaround to get around an apparent bug in the om serialization
        // (the pull stream parsing was not pulling the final tag).
        // Four things need to occur:
        //   a) analyze fix the serialization/pull stream problem.
        //   b) add a method signature to allow the caller to request build or no build
        //   c) add a method signature to allow the caller to enable/disable caching
        //   d) possibly add an optimization to use OMSE for the body elements...to flatten the tree.
        try {
            omEnvelope.build();
        } catch (OMException ex){
            try {
                // Let's try to see if we can save the envelope as a string
                // and then make it into axiom SOAPEnvelope
                return toOM(toString(saajEnvelope));
            } catch (TransformerException e) {
                throw ExceptionFactory.makeWebServiceException(e);
            }
        }
        return omEnvelope;
    }

    private org.apache.axiom.soap.SOAPEnvelope toOM(String xml)
            throws WebServiceException {
        XMLStreamReader reader;
        try {
            reader = StAXUtils.createXMLStreamReader(new ByteArrayInputStream(xml.getBytes()));
        } catch (XMLStreamException e) {
            throw ExceptionFactory.makeWebServiceException(e);
        }
        // Get a SOAP OM Builder.  Passing null causes the version to be automatically triggered
        StAXSOAPModelBuilder builder = new StAXSOAPModelBuilder(reader, null);
        // Create and return the OM Envelope
        return builder.getSOAPEnvelope();
    }

    private String toString(SOAPEnvelope saajEnvelope) throws TransformerException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Transformer tf;
            tf = TransformerFactory.newInstance().newTransformer();
            tf.transform(new DOMSource(saajEnvelope.getOwnerDocument()), new StreamResult(baos));
        return new String(baos.toByteArray());
    }

    /* (non-Javadoc)
      * @see org.apache.axis2.jaxws.message.util.SAAJConverter#toOM(javax.xml.soap.SOAPElement)
      */
    public OMElement toOM(SOAPElement soapElement) throws WebServiceException {
        // Get a XMLStreamReader backed by a SOAPElement tree
        XMLStreamReader reader = new SOAPElementReader(soapElement);
        // Get a OM Builder.
        StAXOMBuilder builder = new StAXOMBuilder(reader);
        // Create and return the Element
        OMElement om = builder.getDocumentElement();
        // TODO The following statement expands the OM tree.  This is 
        // a brute force workaround to get around an apparent bug in the om serialization
        // (the pull stream parsing was not pulling the final tag).
        // Three things need to occur:
        //   a) analyze fix the serialization/pull stream problem.
        //   b) add a method signature to allow the caller to request build or no build
        //   c) add a method signature to allow the caller to enable/disable caching
        om.build();
        return om;
    }

    /* (non-Javadoc)
      * @see org.apache.axis2.jaxws.message.util.SAAJConverter#toSAAJ(org.apache.axiom.om.OMElement, javax.xml.soap.SOAPElement)
      */
    public SOAPElement toSAAJ(OMElement omElement, SOAPElement parent) throws WebServiceException {
        XMLStreamReader reader = null;

        // If the OM element is not attached to a parser (builder), then the OM
        // is built and you cannot ask for XMLStreamReaderWithoutCaching.
        // This is probably a bug in OM.  You should be able to ask the OM whether
        // caching is supported.
        if (omElement.getBuilder() == null) {
            reader = omElement.getXMLStreamReader();
        } else {
            reader = omElement.getXMLStreamReaderWithoutCaching();
        }
        SOAPElement env = parent;
        while (env != null && !(env instanceof SOAPEnvelope)) {
            env = env.getParentElement();
        }
        if (env == null) {
            throw ExceptionFactory
                    .makeWebServiceException(Messages.getMessage("SAAJConverterErr1"));
        }
        NameCreator nc = new NameCreator((SOAPEnvelope)env);
        return buildSOAPTree(nc, null, parent, reader, false);
    }


    /* (non-Javadoc)
      * @see org.apache.axis2.jaxws.message.util.SAAJConverter#toSAAJ(org.apache.axiom.om.OMElement, javax.xml.soap.SOAPElement, javax.xml.soap.SOAPFactory)
      */
    public SOAPElement toSAAJ(OMElement omElement, SOAPElement parent, SOAPFactory sf)
            throws WebServiceException {
        XMLStreamReader reader = null;

        // If the OM element is not attached to a parser (builder), then the OM
        // is built and you cannot ask for XMLStreamReaderWithoutCaching.
        // This is probably a bug in OM.  You should be able to ask the OM whether
        // caching is supported.
        if (omElement.getBuilder() == null) {
            reader = omElement.getXMLStreamReader();
        } else {
            reader = omElement.getXMLStreamReaderWithoutCaching();
        }
        NameCreator nc = new NameCreator(sf);
        return buildSOAPTree(nc, null, parent, reader, false);
    }


    /**
     * Build SOAPTree Either the root or the parent is null. If the root is null, a new element is
     * created under the parent using information from the reader If the parent is null, the existing
     * root is updated with the information from the reader
     *
     * @param nc         NameCreator
     * @param root       SOAPElement (the element that represents the data in the reader)
     * @param parent     (the parent of the element represented by the reader)
     * @param reader     XMLStreamReader. the first START_ELEMENT matches the root
     * @param quitAtBody - true if quit reading after the body START_ELEMENT
     */
    protected SOAPElement buildSOAPTree(NameCreator nc,
                                        SOAPElement root,
                                        SOAPElement parent,
                                        XMLStreamReader reader,
                                        boolean quitAtBody)
            throws WebServiceException {
        try {
            while (reader.hasNext()) {
                int eventID = reader.next();
                switch (eventID) {
                    case XMLStreamReader.START_ELEMENT: {

                        // The first START_ELEMENT defines the prefix and attributes of the root
                        if (parent == null) {
                            updateTagData(nc, root, reader, false);
                            parent = root;
                        } else {
                            parent = createElementFromTag(nc, parent, reader);
                            if (root == null) {
                                root = parent;
                            }
                        }
                        if (quitAtBody && parent instanceof SOAPBody) {
                            return root;
                        }
                        break;
                    }
                    case XMLStreamReader.ATTRIBUTE: {
                        String eventName = "ATTRIBUTE";
                        this._unexpectedEvent(eventName);
                    }
                    case XMLStreamReader.NAMESPACE: {
                        String eventName = "NAMESPACE";
                        this._unexpectedEvent(eventName);
                    }
                    case XMLStreamReader.END_ELEMENT: {
                        if (parent instanceof SOAPEnvelope) {
                            parent = null;
                        } else {
                            parent = parent.getParentElement();
                        }
                        break;
                    }
                    case XMLStreamReader.CHARACTERS: {
                        parent.addTextNode(reader.getText());
                        break;
                    }
                    case XMLStreamReader.CDATA: {
                        parent.addTextNode(reader.getText());
                        break;
                    }
                    case XMLStreamReader.COMMENT: {
                        // SOAP really doesn't have an adequate representation for comments.
                        // The defacto standard is to add the whole element as a text node.
                        parent.addTextNode("<!--" + reader.getText() + "-->");
                        break;
                    }
                    case XMLStreamReader.SPACE: {
                        parent.addTextNode(reader.getText());
                        break;
                    }
                    case XMLStreamReader.START_DOCUMENT: {
                        // Ignore
                        break;
                    }
                    case XMLStreamReader.END_DOCUMENT: {
                        // Close reader and ignore
                        reader.close();
                        break;
                    }
                    case XMLStreamReader.PROCESSING_INSTRUCTION: {
                        // Ignore
                        break;
                    }
                    case XMLStreamReader.ENTITY_REFERENCE: {
                        // Ignore. this is unexpected in a web service message
                        break;
                    }
                    case XMLStreamReader.DTD: {
                        // Ignore. this is unexpected in a web service message
                        break;
                    }
                    default:
                        this._unexpectedEvent("EventID " + String.valueOf(eventID));
                }
            }
        } catch (WebServiceException e) {
            throw e;
        } catch (XMLStreamException e) {
            throw ExceptionFactory.makeWebServiceException(e);
        } catch (SOAPException e) {
            throw ExceptionFactory.makeWebServiceException(e);
        }
        return root;
    }

    /**
     * Create SOAPElement from the current tag data
     *
     * @param nc     NameCreator
     * @param parent SOAPElement for the new SOAPElement
     * @param reader XMLStreamReader whose cursor is at the START_ELEMENT
     * @return
     */
    protected SOAPElement createElementFromTag(NameCreator nc,
                                               SOAPElement parent,
                                               XMLStreamReader reader)
            throws SOAPException {
        // Unfortunately, the SAAJ object is a product of both the
        // QName of the element and the parent object.  For example,
        // All element children of a SOAPBody must be object's that are SOAPBodyElements.
        // createElement creates the proper child element.
        QName qName = reader.getName();
        SOAPElement child = createElement(parent, qName);

        // Update the tag data on the child
        updateTagData(nc, child, reader, true);
        return child;
    }

    /**
     * Create child SOAPElement
     *
     * @param parent SOAPElement
     * @param name   Name
     * @return
     */
    protected SOAPElement createElement(SOAPElement parent, QName qName)
            throws SOAPException {
        SOAPElement child;
        if (parent instanceof SOAPEnvelope) {
            if (qName.getNamespaceURI().equals(parent.getNamespaceURI())) {
                if (qName.getLocalPart().equals("Body")) {
                    child = ((SOAPEnvelope)parent).addBody();
                } else {
                    child = ((SOAPEnvelope)parent).addHeader();
                }
            } else {
                child = parent.addChildElement(qName);
            }
        } else if (parent instanceof SOAPBody) {
            if (qName.getNamespaceURI().equals(parent.getNamespaceURI()) &&
                    qName.getLocalPart().equals("Fault")) {
                child = ((SOAPBody)parent).addFault();
            } else {
                child = ((SOAPBody)parent).addBodyElement(qName);
            }
        } else if (parent instanceof SOAPHeader) {
            child = ((SOAPHeader)parent).addHeaderElement(qName);
        } else if (parent instanceof SOAPFault) {
            // This call assumes that the addChildElement implementation
            // is smart enough to add "Detail" or "SOAPFaultElement" objects.
            child = parent.addChildElement(qName);
        } else if (parent instanceof Detail) {
            child = ((Detail)parent).addDetailEntry(qName);
        } else {
            child = parent.addChildElement(qName);
        }

        return child;
    }

    /**
     * update the tag data of the SOAPElement
     *
     * @param NameCreator nc
     * @param element     SOAPElement
     * @param reader      XMLStreamReader whose cursor is at START_ELEMENT
     */
    protected void updateTagData(NameCreator nc,
                                 SOAPElement element,
                                 XMLStreamReader reader, 
                                 boolean newElement) throws SOAPException {
        String prefix = reader.getPrefix();
        prefix = (prefix == null) ? "" : prefix;

        // Make sure the prefix is correct
        if (prefix.length() > 0 && !element.getPrefix().equals(prefix)) {
            // Due to a bug in Axiom DOM or in the reader...not sure where yet,
            // there may be a non-null prefix and no namespace
            String ns = reader.getNamespaceURI();
            if (ns != null && ns.length() != 0) {
                element.setPrefix(prefix);
            }

        }
        
        if (!newElement) {    
            // Add the namespace declarations from the reader for the missing namespaces
            int size = reader.getNamespaceCount();
            for (int i=0; i<size; i++) {
                String pre = reader.getNamespacePrefix(i);
                String ns = reader.getNamespaceURI(i);
                String existingNS = element.getNamespaceURI(pre);
                if (!ns.equals(existingNS)) {
                    element.removeNamespaceDeclaration(pre);  // Is it necessary to remove the existing prefix/ns
                    element.addNamespaceDeclaration(pre, ns);
                }
            }
        } else {
            // Add the namespace declarations from the reader
            int size = reader.getNamespaceCount();
            for (int i=0; i<size; i++) {
                element.addNamespaceDeclaration(reader.getNamespacePrefix(i), reader.getNamespaceURI(i));
            }
        }

        addAttributes(nc, element, reader);

        return;
    }

    /**
     * add attributes
     *
     * @param NameCreator nc
     * @param element     SOAPElement which is the target of the new attributes
     * @param reader      XMLStreamReader whose cursor is at START_ELEMENT
     * @throws SOAPException
     */
    protected void addAttributes(NameCreator nc,
                                 SOAPElement element,
                                 XMLStreamReader reader) throws SOAPException {

        // Add the attributes from the reader
        int size = reader.getAttributeCount();
        for (int i = 0; i < size; i++) {
            QName qName = reader.getAttributeName(i);
            String prefix = reader.getAttributePrefix(i);
            String value = reader.getAttributeValue(i);
            Name name = nc.createName(qName.getLocalPart(), prefix, qName.getNamespaceURI());
            element.addAttribute(name, value);
        }
    }

    private void _unexpectedEvent(String event) throws WebServiceException {
        // Review We need NLS for this message, but this code will probably
        // be added to JAX-WS.  So for now we there is no NLS.
        // TODO NLS
        throw ExceptionFactory
                .makeWebServiceException(Messages.getMessage("SAAJConverterErr2", event));
    }

    /**
     * A Name can be created from either a SOAPEnvelope or SOAPFactory. Either one or the other is
     * available when the converter is called. NameCreator provides a level of abstraction which
     * simplifies the code.
     */
    protected class NameCreator {
        private SOAPEnvelope env = null;
        private SOAPFactory sf = null;

        public NameCreator(SOAPEnvelope env) {
            this.env = env;
        }

        public NameCreator(SOAPFactory sf) {
            this.sf = sf;
        }

        /**
         * Creates a Name
         *
         * @param localName
         * @param prefix
         * @param uri
         * @return Name
         */
        public Name createName(String localName, String prefix, String uri)
                throws SOAPException {
            if (sf != null) {
                return sf.createName(localName, prefix, uri);
            } else {
                return env.createName(localName, prefix, uri);
            }
        }

    }

    public MessageFactory createMessageFactory(String namespace)
            throws SOAPException, WebServiceException {
        return SAAJFactory.createMessageFactory(namespace);
    }
}
