/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.apache.ofbiz.service.jms;

import java.util.Map;

import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.ObjectType;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.serialize.XmlSerializer;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceContainer;

/**
 * AbstractJmsListener
 */
public abstract class AbstractJmsListener implements GenericMessageListener, ExceptionListener {

    public static final String module = AbstractJmsListener.class.getName();

    protected LocalDispatcher dispatcher;
    protected boolean isConnected = false;

    /**
     * Initializes the LocalDispatcher for this service listener.
     * @param delegator the delegator associated to the dispatcher
     */
    protected AbstractJmsListener(Delegator delegator) {
        this.dispatcher = ServiceContainer.getLocalDispatcher("JMSDispatcher", delegator);
    }

    /**
     * Runs the service defined in the MapMessage
     * @param message
     * @return Map
     */
    protected Map<String, Object> runService(MapMessage message) {
        Map<String, ? extends Object> context = null;
        String serviceName = null;
        String xmlContext = null;

        try {
            serviceName = message.getString("serviceName");
            xmlContext = message.getString("serviceContext");
            if (serviceName == null || xmlContext == null) {
                Debug.logError("Message received is not an OFB service message. Ignored!", module);
                return null;
            }

            Object o = XmlSerializer.deserialize(xmlContext, dispatcher.getDelegator());

            if (Debug.verboseOn()) Debug.logVerbose("De-Serialized Context --> " + o, module);
            if (ObjectType.instanceOf(o, "java.util.Map"))
                context = UtilGenerics.checkMap(o);
        } catch (JMSException je) {
            Debug.logError(je, "Problems reading message.", module);
        } catch (Exception e) {
            Debug.logError(e, "Problems deserializing the service context.", module);
        }

        try {
            ModelService model = dispatcher.getDispatchContext().getModelService(serviceName);
            if (!model.export) {
                Debug.logWarning("Attempt to invoke a non-exported service: " + serviceName, module);
                return null;
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, "Unable to get ModelService for service : " + serviceName, module);
        }

        if (Debug.verboseOn()) Debug.logVerbose("Running service: " + serviceName, module);

        Map<String, Object> result = null;
        if (context != null) {
            try {
                result = dispatcher.runSync(serviceName, context);
            } catch (GenericServiceException gse) {
                Debug.logError(gse, "Problems with service invocation.", module);
            }
        }
        return result;
    }

    /**
     * Receives the MapMessage and processes the service.
     * @see javax.jms.MessageListener#onMessage(Message)
     */
    @Override
    public void onMessage(Message message) {
        MapMessage mapMessage = null;

        if (Debug.verboseOn()) Debug.logVerbose("JMS Message Received --> " + message, module);

        if (message instanceof MapMessage) {
            mapMessage = (MapMessage) message;
        } else {
            Debug.logError("Received message is not a MapMessage!", module);
            return;
        }
        runService(mapMessage);
    }

    /**
     * On exception try to re-establish connection to the JMS server.
     * @see javax.jms.ExceptionListener#onException(JMSException)
     */
    @Override
    public void onException(JMSException je) {
        this.setConnected(false);
        Debug.logError(je, "JMS connection exception", module);
        while (!isConnected()) {
            try {
                this.refresh();
            } catch (GenericServiceException e) {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ie) {}
                continue;
            }
        }
    }

    @Override
    public void refresh() throws GenericServiceException {
        this.close();
        this.load();
    }

    @Override
    public boolean isConnected() {
        return this.isConnected;
    }

    /**
     * Setter method for the connected field.
     * @param connected
     */
    protected void setConnected(boolean connected) {
        this.isConnected = connected;
    }

}
