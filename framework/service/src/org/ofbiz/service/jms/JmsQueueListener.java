/*
 * $Id: JmsQueueListener.java 5462 2005-08-05 18:35:48Z jonesde $
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
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ServiceDispatcher;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.JNDIContextFactory;

/**
 * JmsQueueListener - Queue (P2P) Message Listener.
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      2.0
 */
public class JmsQueueListener extends AbstractJmsListener {

    public static final String module = JmsQueueListener.class.getName();

    private QueueConnection con = null;
    private QueueSession session = null;
    private Queue queue = null;

    private String jndiServer, jndiName, queueName, userName, password;

    /**
     * Creates a new JmsQueueListener - Should only be called by the JmsListenerFactory.
     */
    public JmsQueueListener(ServiceDispatcher dispatcher, String jndiServer, String jndiName, String queueName, String userName, String password) {
        super(dispatcher);
        this.jndiServer = jndiServer;
        this.jndiName = jndiName;
        this.queueName = queueName;
        this.userName = userName;
        this.password = password;
    }

    public void close() throws GenericServiceException {
        try {
            session.close();
            con.close();
        } catch (JMSException e) {
            throw new GenericServiceException("Cannot close connection(s).", e);
        }
    }

    public synchronized void load() throws GenericServiceException {
        try {
            InitialContext jndi = JNDIContextFactory.getInitialContext(jndiServer);
            QueueConnectionFactory factory = (QueueConnectionFactory) jndi.lookup(jndiName);

            if (factory != null) {
                con = factory.createQueueConnection(userName, password);
                con.setExceptionListener(this);
                session = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
                queue = (Queue) jndi.lookup(queueName);
                if (queue != null) {
                    QueueReceiver receiver = session.createReceiver(queue);

                    receiver.setMessageListener(this);
                    con.start();
                    this.setConnected(true);
                    Debug.logInfo("Listening to queue [" + queueName + "]...", module);
                } else {
                    throw new GenericServiceException("Queue lookup failed.");
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
