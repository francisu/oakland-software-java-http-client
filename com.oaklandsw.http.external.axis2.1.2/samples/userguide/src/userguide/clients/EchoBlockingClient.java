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


package userguide.clients;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;

/**
 * Sample for synchronous single channel blocking service invocation.
 * Message Exchage Pattern IN-OUT
 */
public class EchoBlockingClient {
    private static EndpointReference targetEPR = new EndpointReference("http://localhost:8080/axis2/services/MyService");

    public static void main(String[] args) {
        try {
            OMElement payload = ClientUtil.getEchoOMElement();
            Options options = new Options();
            options.setTo(targetEPR);
            options.setAction("urn:echo");

            //Blocking invocation
            ServiceClient sender = new ServiceClient();
            sender.setOptions(options);
            OMElement result = sender.sendReceive(payload);

            System.out.println(result);

        } catch (AxisFault axisFault) {
            axisFault.printStackTrace();
        }
    }
}
