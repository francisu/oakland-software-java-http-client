/*
 * Copyright 2006 oakland software, incorporated. All rights Reserved.
 */
package com.oaklandsw.http.axis;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.handlers.AbstractHandler;
import org.apache.axis2.transport.TransportSender;

import com.oaklandsw.util.Util;

public class OaklandHTTPSender2 extends AbstractHandler
    implements
        TransportSender
{

    private OaklandHTTPSender _delegate;

    public OaklandHTTPSender2()
    {
        Util.impossible("not yet implemented");
    }

    public void cleanup(MessageContext msgContext) throws AxisFault
    {

    }

    public void init(ConfigurationContext confContext,
                     TransportOutDescription transportOut) throws AxisFault
    {

    }

    public void stop()
    {

    }

    public void invoke(MessageContext msgContext2) throws AxisFault
    {

    }

}
