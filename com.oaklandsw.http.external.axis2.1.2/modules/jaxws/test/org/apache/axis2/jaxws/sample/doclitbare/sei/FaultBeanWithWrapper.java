
package org.apache.axis2.jaxws.sample.doclitbare.sei;

import javax.xml.ws.WebFault;

import org.test.sample.doclitbare.BaseFault;


/**
 * This class was generated by the JAXWS SI.
 * JAX-WS RI 2.0_01-b15-fcs
 * Generated source version: 2.0
 * 
 */
@WebFault(name = "MyBaseFaultBean", faultBean="", targetNamespace = "http://doclitbare.sample.test.org")
public class FaultBeanWithWrapper
    extends Exception
{

    /**
     * Java type that goes as soapenv:Fault detail element.
     * 
     */
    private BaseFault faultInfo;

    /**
     * 
     * @param faultInfo
     * @param message
     */
    public FaultBeanWithWrapper(String message, BaseFault faultInfo) {
        super(message);
        this.faultInfo = faultInfo;
    }

    /**
     * 
     * @param faultInfo
     * @param message
     * @param cause
     */
    public FaultBeanWithWrapper(String message, BaseFault faultInfo, Throwable cause) {
        super(message, cause);
        this.faultInfo = faultInfo;
    }

    /**
     * 
     * @return
     *     returns fault bean: org.test.sample.doclitbare.BaseFault
     */
    public BaseFault getFaultInfo() {
        return faultInfo;
    }

}
