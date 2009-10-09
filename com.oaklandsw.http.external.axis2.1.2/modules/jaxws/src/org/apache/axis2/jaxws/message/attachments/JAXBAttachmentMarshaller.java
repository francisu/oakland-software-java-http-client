/*
 * Copyright 2006 The Apache Software Foundation.
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
package org.apache.axis2.jaxws.message.attachments;

import org.apache.axiom.om.OMText;
import org.apache.axiom.om.impl.MTOMXMLStreamWriter;
import org.apache.axiom.om.impl.llom.OMTextImpl;
import org.apache.axiom.om.util.UUIDGenerator;
import org.apache.axis2.jaxws.ExceptionFactory;
import org.apache.axis2.jaxws.message.Message;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.activation.DataHandler;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimePartDataSource;
import javax.xml.bind.attachment.AttachmentMarshaller;
import javax.xml.stream.XMLStreamWriter;

/**
 * An implementation of the JAXB AttachmentMarshaller that is used to handle binary data from JAXB
 * and create populate the appropriate constructs within the JAX-WS Message Model.
 */
public class JAXBAttachmentMarshaller extends AttachmentMarshaller {

    private static final Log log = LogFactory.getLog(JAXBAttachmentMarshaller.class);

    private Message message;
    private XMLStreamWriter writer;
    private final String APPLICATION_OCTET = "application/octet-stream";
    
    public JAXBAttachmentMarshaller(Message message, XMLStreamWriter writer) {
        this.message = message;
        this.writer = writer;
    }

    @Override
    public boolean isXOPPackage() {
        boolean value = false;
        
        // For outbound messages, only trigger MTOM if
        // the message is mtom enabled (which indicates that
        // the api dispatch/provider/proxy/impl has an MTOM binding)
        if (message != null) {
          value = message.isMTOMEnabled();
        }
    
        if (log.isDebugEnabled()){ 
            log.debug("isXOPPackage returns " + value);
        }
        return value;

    }

    @Override
    public String addMtomAttachment(byte[] data, int offset, int length,
                                    String mimeType, String namespace, String localPart) {

        if (mimeType == null || mimeType.length() == 0) {
            mimeType = APPLICATION_OCTET;
        }
        
        if (log.isDebugEnabled()){ 
            log.debug("Adding MTOM/XOP byte array attachment for element: " + "{" + namespace + "}" + localPart);
        }
        
        String cid;
        try {
            // Create MIME Body Part
            InternetHeaders ih = new InternetHeaders();
            ih.setHeader(HTTPConstants.HEADER_CONTENT_TYPE, mimeType);
            MimeBodyPart mbp = new MimeBodyPart(ih, data);
            
            //Create a data source for the MIME Body Part
            MimePartDataSource mpds = new MimePartDataSource(mbp);
            
            DataHandler dataHandler = new DataHandler(mpds);
            cid = addDataHandler(dataHandler);
            
            // Add the content id to the mime body part
            mbp.setHeader(HTTPConstants.HEADER_CONTENT_ID, cid);
        } catch (Throwable t) {
            throw ExceptionFactory.makeWebServiceException(t);
        }

        return "cid:" + cid;

    }
    
    @Override
    public String addMtomAttachment(DataHandler data, String namespace, String localPart) {
        if (log.isDebugEnabled()){ 
            log.debug("Adding MTOM/XOP datahandler attachment for element: " + "{" + namespace + "}" + localPart);
        }
        String cid = addDataHandler(data);
        return "cid:" + cid;
    }
    
    @Override
    public String addSwaRefAttachment(DataHandler data) {
        if (log.isDebugEnabled()){ 
            log.debug("Adding SWAREF attachment");
        }
        
        String cid = addDataHandler(data);
        return "cid:" + cid;
    }
    
    private String addDataHandler(DataHandler dh) {
        String cid = null;
        OMText textNode = null;
        
        // If this is an MTOMXMLStreamWriter then inform the writer 
        // that it must write out this attchment (I guess we should do this
        // even if the attachment is SWAREF ?)
        if (writer instanceof MTOMXMLStreamWriter) {
            textNode = new OMTextImpl(dh, null);
            cid = textNode.getContentID();
            ((MTOMXMLStreamWriter) writer).writeOptimized(textNode);
        } else {
            cid = UUIDGenerator.getUUID();
        }
        
        if (log.isDebugEnabled()){ 
            log.debug("   content id=" + cid);
            log.debug("   dataHandler  =" + dh);
        }
        // Remember the attachment on the message.
        message.addDataHandler(dh, cid);
        return cid;
    }

}
