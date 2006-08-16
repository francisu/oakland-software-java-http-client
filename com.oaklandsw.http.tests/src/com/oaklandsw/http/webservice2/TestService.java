/*
 * Copyright 2006 oakland software, incorporated.  All rights Reserved.
 */
package com.oaklandsw.http.webservice2;

import org.apache.axiom.om.OMElement;

public class TestService
{

    public void ping(OMElement element)
    {
        
    }

    public OMElement echo(OMElement element)
    {
        element.build();
        element.detach();
        return element;
    }
    
}
