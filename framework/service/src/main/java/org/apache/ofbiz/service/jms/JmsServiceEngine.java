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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.jms.XAQueueConnection;
import javax.jms.XAQueueConnectionFactory;
import javax.jms.XAQueueSession;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.xa.XAResource;

import org.apache.ofbiz.base.config.GenericConfigException;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.JNDIContextFactory;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.entity.serialize.SerializeException;
import org.apache.ofbiz.entity.transaction.GenericTransactionException;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.service.GenericRequester;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.service.config.ServiceConfigUtil;
import org.apache.ofbiz.service.config.model.JmsService;
import org.apache.ofbiz.service.config.model.Server;
import org.apache.ofbiz.service.engine.AbstractEngine;
import org.w3c.dom.Element;

/**
 * AbstractJMSEngine
 */
public class JmsServiceEngine extends AbstractEngine {

    public static final String module = JmsServiceEngine.class.getName();

    public JmsServiceEngine(ServiceDispatcher dispatcher) {
        super(dispatcher);
    }

    protected JmsService getServiceElement(ModelService modelService) throws GenericServiceException {
        String location = this.getLocation(modelService);
        try {
            return ServiceConfigUtil.getServiceEngine().getJmsServiceByName(location);
        } catch (GenericConfigException e) {
            throw new GenericServiceException(e);
        }
    }

    protected Message makeMessage(Session session, ModelService modelService, Map<String, Object> context)
        throws GenericServiceException, JMSException {
        List<String> outParams = modelService.getParameterNames(ModelService.OUT_PARAM, false);

        if (UtilValidate.isNotEmpty(outParams))
            throw new GenericServiceException("JMS service cannot have required OUT parameters; no parameters will be returned.");
        String xmlContext = null;

        try {
            if (Debug.verboseOn()) Debug.logVerbose("Serializing Context --> " + context, module);
            xmlContext = JmsSerializer.serialize(context);
        } catch (SerializeException | IOException e) {
            throw new GenericServiceException("Cannot serialize context.", e);
        }
        MapMessage message = session.createMapMessage();

        message.setString("serviceName", modelService.invoke);
        message.setString("serviceContext", xmlContext);
        return message;
    }

    protected List<? extends Element> serverList(Element serviceElement) throws GenericServiceException {
        String sendMode = serviceElement.getAttribute("send-mode");
        List<? extends Element> serverList = UtilXml.childElementList(serviceElement, "server");

        if ("none".equals(sendMode)) {
            return new ArrayList<Element>();
        } else if ("all".equals(sendMode)) {
            return serverList;
        } else {
            throw new GenericServiceException("Requested send mode not supported.");
        }
    }

    protected Map<String, Object> runTopic(ModelService modelService, Map<String, Object> context, Server server) throws GenericServiceException {
        String serverName = server.getJndiServerName();
        String jndiName = server.getJndiName();
        String topicName = server.getTopicQueue();
        String userName = server.getUsername();
        String password = server.getPassword();
        String clientId = server.getClientId();

        InitialContext jndi = null;
        TopicConnectionFactory factory = null;
        TopicConnection con = null;

        try {
            jndi = JNDIContextFactory.getInitialContext(serverName);
            factory = (TopicConnectionFactory) jndi.lookup(jndiName);
        } catch (GeneralException ge) {
            throw new GenericServiceException("Problems getting JNDI InitialContext.", ge.getNested());
        } catch (NamingException ne) {
            JNDIContextFactory.clearInitialContext(serverName);
            try {
                jndi = JNDIContextFactory.getInitialContext(serverName);
                factory = (TopicConnectionFactory) jndi.lookup(jndiName);
            } catch (GeneralException ge2) {
                throw new GenericServiceException("Problems getting JNDI InitialContext.", ge2.getNested());
            } catch (NamingException ne2) {
                throw new GenericServiceException("JNDI lookup problems.", ne);
            }
        }

        try {
            con = factory.createTopicConnection(userName, password);

            if (clientId != null && clientId.length() > 1)
                con.setClientID(clientId);
            con.start();

            TopicSession session = con.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic topic = (Topic) jndi.lookup(topicName);
            TopicPublisher publisher = session.createPublisher(topic);

            // create/send the message
            Message message = makeMessage(session, modelService, context);

            publisher.publish(message);
            if (Debug.verboseOn()) Debug.logVerbose("Sent JMS Message to " + topicName, module);

            // close the connections
            publisher.close();
            session.close();
            con.close();
        } catch (NamingException ne) {
            throw new GenericServiceException("Problems with JNDI lookup.", ne);
        } catch (JMSException je) {
            throw new GenericServiceException("JMS Internal Error.", je);
        }
        return ServiceUtil.returnSuccess();

    }

    protected Map<String, Object> runQueue(ModelService modelService, Map<String, Object> context, Server server) throws GenericServiceException {
        String serverName = server.getJndiServerName();
        String jndiName = server.getJndiName();
        String queueName = server.getTopicQueue();
        String userName = server.getUsername();
        String password = server.getPassword();
        String clientId = server.getClientId();

        InitialContext jndi = null;
        QueueConnectionFactory factory = null;
        QueueConnection con = null;

        try {
            jndi = JNDIContextFactory.getInitialContext(serverName);
            factory = (QueueConnectionFactory) jndi.lookup(jndiName);
        } catch (GeneralException ge) {
            throw new GenericServiceException("Problems getting JNDI InitialContext.", ge.getNested());
        } catch (NamingException ne) {
            JNDIContextFactory.clearInitialContext(serverName);
            try {
                jndi = JNDIContextFactory.getInitialContext(serverName);
                factory = (QueueConnectionFactory) jndi.lookup(jndiName);
            } catch (GeneralException ge2) {
                throw new GenericServiceException("Problems getting JNDI InitialContext.", ge2.getNested());
            } catch (NamingException ne2) {
                throw new GenericServiceException("JNDI lookup problem.", ne2);
            }
        }

        try {
            con = factory.createQueueConnection(userName, password);

            if (clientId != null && clientId.length() > 1)
                con.setClientID(clientId);
            con.start();

            QueueSession session = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = (Queue) jndi.lookup(queueName);
            QueueSender sender = session.createSender(queue);

            // create/send the message
            Message message = makeMessage(session, modelService, context);

            sender.send(message);
            if (Debug.verboseOn()) Debug.logVerbose("Sent JMS Message to " + queueName, module);

            // close the connections
            sender.close();
            session.close();
            con.close();
        } catch (NamingException ne) {
            throw new GenericServiceException("Problems with JNDI lookup.", ne);
        } catch (JMSException je) {
            throw new GenericServiceException("JMS Internal Error.", je);
        }
        return ServiceUtil.returnSuccess();
    }

    protected Map<String, Object> runXaQueue(ModelService modelService, Map<String, Object> context, Element server) throws GenericServiceException {
        String serverName = server.getAttribute("jndi-server-name");
        String jndiName = server.getAttribute("jndi-name");
        String queueName = server.getAttribute("topic-queue");
        String userName = server.getAttribute("username");
        String password = server.getAttribute("password");
        String clientId = server.getAttribute("client-id");

        InitialContext jndi = null;
        XAQueueConnectionFactory factory = null;
        XAQueueConnection con = null;

        try {
            jndi = JNDIContextFactory.getInitialContext(serverName);
            factory = (XAQueueConnectionFactory) jndi.lookup(jndiName);
        } catch (GeneralException ge) {
            throw new GenericServiceException("Problems getting JNDI InitialContext.", ge.getNested());
        } catch (NamingException ne) {
            JNDIContextFactory.clearInitialContext(serverName);
            try {
                jndi = JNDIContextFactory.getInitialContext(serverName);
                factory = (XAQueueConnectionFactory) jndi.lookup(jndiName);
            } catch (GeneralException ge2) {
                throw new GenericServiceException("Problems getting JNDI InitialContext.", ge2.getNested());
            } catch (NamingException ne2) {
                throw new GenericServiceException("JNDI lookup problems.", ne2);
            }
        }

        try {
            con = factory.createXAQueueConnection(userName, password);

            if (clientId.length() > 1)
                con.setClientID(userName);
            con.start();

            // enlist the XAResource
            XAQueueSession session = con.createXAQueueSession();
            XAResource resource = session.getXAResource();

            if (TransactionUtil.getStatus() == TransactionUtil.STATUS_ACTIVE)
                TransactionUtil.enlistResource(resource);

            Queue queue = (Queue) jndi.lookup(queueName);
            QueueSession qSession = session.getQueueSession();
            QueueSender sender = qSession.createSender(queue);

            // create/send the message
            Message message = makeMessage(session, modelService, context);

            sender.send(message);

            if (TransactionUtil.getStatus() != TransactionUtil.STATUS_ACTIVE)
                session.commit();

            Debug.logInfo("Message sent.", module);

            // close the connections
            sender.close();
            session.close();
            con.close();
        } catch (GenericTransactionException gte) {
            throw new GenericServiceException("Problems enlisting resource w/ transaction manager.", gte.getNested());
        } catch (NamingException ne) {
            throw new GenericServiceException("Problems with JNDI lookup.", ne);
        } catch (JMSException je) {
            throw new GenericServiceException("JMS Internal Error.", je);
        }
        return ServiceUtil.returnSuccess();
    }

    protected Map<String, Object> run(ModelService modelService, Map<String, Object> context) throws GenericServiceException {
        JmsService serviceElement = getServiceElement(modelService);
        List<Server> serverList = serviceElement.getServers();

        Map<String, Object> result = new HashMap<String, Object>();
        for (Server server: serverList) {
            String serverType = server.getType();
            if ("topic".equals(serverType))
                result.putAll(runTopic(modelService, context, server));
            else if ("queue".equals(serverType))
                result.putAll(runQueue(modelService, context, server));
            else
                throw new GenericServiceException("Illegal server messaging type.");
        }
        return result;
    }

    /**
     * @see org.apache.ofbiz.service.engine.GenericEngine#runSync(java.lang.String, org.apache.ofbiz.service.ModelService, java.util.Map)
     */
    public Map<String, Object> runSync(String localName, ModelService modelService, Map<String, Object> context) throws GenericServiceException {
        return run(modelService, context);
    }

    /**
     * @see org.apache.ofbiz.service.engine.GenericEngine#runSyncIgnore(java.lang.String, org.apache.ofbiz.service.ModelService, java.util.Map)
     */
    public void runSyncIgnore(String localName, ModelService modelService, Map<String, Object> context) throws GenericServiceException {
        run(modelService, context);
    }

    /**
     * @see org.apache.ofbiz.service.engine.GenericEngine#runAsync(java.lang.String, org.apache.ofbiz.service.ModelService, java.util.Map, org.apache.ofbiz.service.GenericRequester, boolean)
     */
    public void runAsync(String localName, ModelService modelService, Map<String, Object> context, GenericRequester requester, boolean persist) throws GenericServiceException {
        Map<String, Object> result = run(modelService, context);

        requester.receiveResult(result);
    }

    /**
     * @see org.apache.ofbiz.service.engine.GenericEngine#runAsync(java.lang.String, org.apache.ofbiz.service.ModelService, java.util.Map, boolean)
     */
    public void runAsync(String localName, ModelService modelService, Map<String, Object> context, boolean persist) throws GenericServiceException {
        run(modelService, context);
    }

}
