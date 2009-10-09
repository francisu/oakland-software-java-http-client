/*
 * Copyright 2006,2007 The Apache Software Foundation.
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

package org.apache.axis2.builder;

import org.apache.axiom.attachments.Attachments;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.MTOMConstants;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.context.MessageContext;

import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;

public class MIMEBuilder implements Builder {

    public OMElement processDocument(InputStream inputStream, String contentType,
                                     MessageContext msgContext)
            throws AxisFault {
        XMLStreamReader streamReader;
        Attachments attachments =
                BuilderUtil.createAttachmentsMap(msgContext, inputStream, contentType);
        String charSetEncoding =
                BuilderUtil.getCharSetEncoding(attachments.getSOAPPartContentType());

        if ((charSetEncoding == null)
                || "null".equalsIgnoreCase(charSetEncoding)) {
            charSetEncoding = MessageContext.UTF_8;
        }
        msgContext.setProperty(Constants.Configuration.CHARACTER_SET_ENCODING,
                               charSetEncoding);

        //  Put a reference to Attachments Map in to the message context For
        // backword compatibility with Axis2 1.0 
        msgContext.setProperty(MTOMConstants.ATTACHMENTS, attachments);

        // Setting the Attachments map to new SwA API
        msgContext.setAttachmentMap(attachments);

//      if (isSOAP) {
        Builder builder =
                BuilderUtil.getBuilderFromSelector(attachments.getAttachmentSpecType(), msgContext);
        OMElement element = builder.processDocument(attachments.getSOAPPartInputStream(),
                                                    contentType, msgContext);

//      }
//      // To handle REST XOP case
//      else {
//      if (attachments.getAttachmentSpecType().equals(
//      MTOMConstants.MTOM_TYPE)) {
//      XOPAwareStAXOMBuilder stAXOMBuilder = new XOPAwareStAXOMBuilder(
//      streamReader, attachments);
//      builder = stAXOMBuilder;
//      
//      } else if (attachments.getAttachmentSpecType().equals(
//      MTOMConstants.SWA_TYPE)) {
//      builder = new StAXOMBuilder(streamReader);
//      } else if (attachments.getAttachmentSpecType().equals(
//      MTOMConstants.SWA_TYPE_12) ) {
//      builder = new StAXOMBuilder(streamReader);
//      }
//      }

        return element;
    }

}
