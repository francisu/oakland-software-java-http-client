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

package samples.ping.service;

import javax.xml.stream.XMLStreamException;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Pingable;

import org.apache.axis2.AxisFault;

import java.util.HashMap;

public class PingService implements Pingable {

    private HashMap map = new HashMap();

    public OMElement getPrice(OMElement element) throws XMLStreamException {
        element.build();
        element.detach();

        OMElement symbolElement = element.getFirstElement();
        String symbol = symbolElement.getText();

        String returnText = "42";
        Double price = (Double) map.get(symbol);
        if (price != null) {
            returnText = "" + price.doubleValue();
        }
        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMNamespace omNs =
                fac.createOMNamespace("http://quickstart.samples/xsd", "ns");
        OMElement method = fac.createOMElement("getPriceResponse", omNs);
        OMElement value = fac.createOMElement("return", omNs);
        value.addChild(fac.createOMText(value, returnText));
        method.addChild(value);
        return method;
    }

    public void update(OMElement element) throws XMLStreamException {
        element.build();
        element.detach();

        OMElement symbolElement = element.getFirstElement();
        String symbol = symbolElement.getText();

        OMElement priceElement = (OMElement) symbolElement.getNextOMSibling();
        String price = priceElement.getText();

        map.put(symbol, new Double(price));
    }

    public int ping() throws AxisFault {
        MessageContext msgContext = MessageContext.getCurrentMessageContext();
        //operation name to can be accessed in this manner
        String operationName = (String)msgContext.getProperty(OPERATION_TO_PING);

        //Implementation of this method depends on the particular service
        if(operationName != null && operationName.equals("getPrice")){
            //For this sample
            return PING_SUCCESSFUL;
        }else if(operationName != null && operationName.equals("update")){
            return PING_FAILD;
        }
        return PING_FAILD;
    }
}

