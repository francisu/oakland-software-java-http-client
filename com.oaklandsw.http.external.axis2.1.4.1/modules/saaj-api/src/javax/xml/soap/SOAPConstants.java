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

package javax.xml.soap;

import javax.xml.namespace.QName;

/** The definition of constants pertaining to the SOAP 1.1 protocol. */
public interface SOAPConstants {

    /** The namespace identifier for the SOAP envelope. */
    public static final String URI_NS_SOAP_ENVELOPE =
            "http://schemas.xmlsoap.org/soap/envelope/";

    /**
     * The namespace identifier for the SOAP encoding (see section 5 of the SOAP 1.1
     * specification).
     */
    public static final String URI_NS_SOAP_ENCODING =
            "http://schemas.xmlsoap.org/soap/encoding/";

    /**
     * The URI identifying the first application processing a SOAP request as the intended actor for
     * a SOAP header entry (see section 4.2.2 of the SOAP 1.1 specification).
     */
    public static final String URI_SOAP_ACTOR_NEXT =
            "http://schemas.xmlsoap.org/soap/actor/next";

    public static final String DYNAMIC_SOAP_PROTOCOL
            = "Dynamic Protocol";

    public static final String SOAP_1_1_PROTOCOL
            = "SOAP 1.1 Protocol";

    public static final String SOAP_1_2_PROTOCOL
            = "SOAP 1.2 Protocol";

    public static final String DEFAULT_SOAP_PROTOCOL
            = "SOAP 1.1 Protocol";

    public static final String URI_NS_SOAP_1_1_ENVELOPE
            = "http://schemas.xmlsoap.org/soap/envelope/";

    public static final String URI_NS_SOAP_1_2_ENVELOPE
            = "http://www.w3.org/2003/05/soap-envelope";

    public static final String URI_NS_SOAP_1_2_ENCODING
            = "http://www.w3.org/2003/05/soap-encoding";

    public static final String SOAP_1_1_CONTENT_TYPE
            = "text/xml";

    public static final String SOAP_1_2_CONTENT_TYPE
            = "application/soap+xml";

    public static final String URI_SOAP_1_2_ROLE_NEXT
            = "http://www.w3.org/2003/05/soap-envelope/role/next";

    public static final String URI_SOAP_1_2_ROLE_NONE
            = "http://www.w3.org/2003/05/soap-envelope/role/none";

    public static final String URI_SOAP_1_2_ROLE_ULTIMATE_RECEIVER
            = "http://www.w3.org/2003/05/soap-envelope/role/ultimateReceiver";

    public static final String SOAP_ENV_PREFIX
            = "env";

    public static final QName SOAP_VERSIONMISMATCH_FAULT
            = new QName("http://www.w3.org/2003/05/soap-envelope", "VersionMismatch", "env");

    public static final QName SOAP_MUSTUNDERSTAND_FAULT
            = new QName("http://www.w3.org/2003/05/soap-envelope", "MustUnderstand", "env");

    public static final QName SOAP_DATAENCODINGUNKNOWN_FAULT
            = new QName("http://www.w3.org/2003/05/soap-envelope", "DataEncodingUnknown", "env");

    public static final QName SOAP_SENDER_FAULT
            = new QName("http://www.w3.org/2003/05/soap-envelope", "Sender", "env");

    public static final QName SOAP_RECEIVER_FAULT
            = new QName("http://www.w3.org/2003/05/soap-envelope", "Receiver", "env");
}
