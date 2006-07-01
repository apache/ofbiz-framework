/*
 * $Id: JmsTopicListener.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 *  Copyright (c) 2001, 2002 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.ofbiz.service.jms;

import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ServiceDispatcher;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.JNDIContextFactory;

/**
 * JmsTopicListener - Topic (Pub/Sub) Message Listener.
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      2.0
 */
public class JmsTopicListener extends AbstractJmsListener {

    public static final String module = JmsTopicListener.class.getName();

    private TopicConnection con = null;
    private TopicSession session = null;
    private Topic topic = null;

    private String jndiServer, jndiName, topicName, userName, password;

    /**
     * Creates a new JmsTopicListener - Should only be called by the JmsListenerFactory.
     */
    public JmsTopicListener(ServiceDispatcher dispatcher, String jndiServer, String jndiName, String topicName, String userName, String password) {
        super(dispatcher);
        this.jndiServer = jndiServer;
        this.jndiName = jndiName;
        this.topicName = topicName;
        this.userName = userName;
        this.password = password;
    }

    public void close() throws GenericServiceException {
        try {
            if (session != null)
                session.close();
            if (con != null)
                con.close();
        } catch (JMSException e) {
            throw new GenericServiceException("Cannot close connection(s).", e);
        }
    }

    public synchronized void load() throws GenericServiceException {
        try {
            InitialContext jndi = JNDIContextFactory.getInitialContext(jndiServer);
            TopicConnectionFactory factory = (TopicConnectionFactory) jndi.lookup(jndiName);

            if (factory != null) {
                con = factory.createTopicConnection(userName, password);
                con.setExceptionListener(this);
                session = con.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
                topic = (Topic) jndi.lookup(topicName);
                if (topic != null) {
                    TopicSubscriber subscriber = session.createSubscriber(topic);
                    subscriber.setMessageListener(this);
                    con.start();
                    this.setConnected(true);                    
                    if (Debug.infoOn()) Debug.logInfo("Listening to topic [" + topicName + "] on [" + jndiServer + "]...", module);
                } else {
                    throw new GenericServiceException("Topic lookup failed.");
                }
            } else {
                throw new GenericServiceException("Factory (broker) lookup failed.");
            }
        } catch (NamingException ne) {
            throw new GenericServiceException("JNDI lookup problems; listener not running.", ne);
        } catch (JMSException je) {
            throw new GenericServiceException("JMS internal error; listener not running.", je);
        } catch (GeneralException ge) {
            throw new GenericServiceException("Problems with InitialContext; listener not running.", ge);
        }
    }
}
