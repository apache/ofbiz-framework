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
package org.ofbiz.service.mail;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import javax.mail.*;
import javax.mail.internet.MimeMessage;
import javax.mail.event.StoreEvent;
import javax.mail.event.StoreListener;

import org.ofbiz.base.container.Container;
import org.ofbiz.base.container.ContainerConfig;
import org.ofbiz.base.container.ContainerException;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.service.GenericDispatcher;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.GenericServiceException;

import org.apache.commons.collections.map.LinkedMap;

public class JavaMailContainer implements Container {

    public static final String module = JavaMailContainer.class.getName();
    public static final String INBOX = "INBOX";

    protected GenericDelegator delegator = null;
    protected LocalDispatcher dispatcher = null;
    protected GenericValue userLogin = null;
    protected long timerDelay = 300000;
    protected long maxSize = 1000000;
    protected Timer pollTimer = null;
    protected boolean deleteMail = false;    // whether to delete emails after fetching them.

    protected String configFile = null;
    protected Map stores = null;

    /**
     * Initialize the container
     *
     * @param args       args from calling class
     * @param configFile Location of master OFBiz configuration file
     * @throws org.ofbiz.base.container.ContainerException
     *
     */
    public void init(String[] args, String configFile) throws ContainerException {
        this.configFile = configFile;       
        this.stores = new LinkedMap();
        this.pollTimer = new Timer();
    }

    /**
     * Start the container
     *
     * @return true if server started
     * @throws org.ofbiz.base.container.ContainerException
     *
     */
    public boolean start() throws ContainerException {
        ContainerConfig.Container cfg = ContainerConfig.getContainer("javamail-container", configFile);
        String dispatcherName = ContainerConfig.getPropertyValue(cfg, "dispatcher-name", "JavaMailDispatcher");
        String delegatorName = ContainerConfig.getPropertyValue(cfg, "delegator-name", "default");
        this.deleteMail = "true".equals(ContainerConfig.getPropertyValue(cfg, "delete-mail", "false"));
        
        this.delegator = GenericDelegator.getGenericDelegator(delegatorName);
        this.dispatcher = GenericDispatcher.getLocalDispatcher(dispatcherName, delegator);
        this.timerDelay = (long) ContainerConfig.getPropertyValue(cfg, "poll-delay", 300000);
        this.maxSize = (long) ContainerConfig.getPropertyValue(cfg, "maxSize", 1000000); // maximum size in bytes

        // load the userLogin object
        String runAsUser = ContainerConfig.getPropertyValue(cfg, "run-as-user", "system");
        try {
            this.userLogin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", runAsUser));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to load run-as-user UserLogin; cannot start container", module);
            return false;
        }

        // load the MCA configuration
        ServiceMcaUtil.readConfig();

        // load the listeners
        List configs = cfg.getPropertiesWithValue("store-listener");
        Iterator i = configs.iterator();
        while (i.hasNext()) {
            ContainerConfig.Container.Property prop = (ContainerConfig.Container.Property) i.next();
            Session session = this.makeSession(prop);
            Store store = this.getStore(session);
            if (store != null) {
                stores.put(store, session);
                store.addStoreListener(new LoggingStoreListener());
            }
        }

        // start the polling timer
        if (stores != null && stores.size() > 0) {
            pollTimer.schedule(new PollerTask(dispatcher, userLogin), timerDelay, timerDelay);
        } else {
            Debug.logWarning("No JavaMail Store(s) configured; poller disabled.", module);
        }

        return true;
    }

    /**
     * Stop the container
     *
     * @throws org.ofbiz.base.container.ContainerException
     *
     */
    public void stop() throws ContainerException {
        // stop the poller
        this.pollTimer.cancel();
        Debug.logVerbose("stop JavaMail poller", module);
    }

    // java-mail methods
    protected Session makeSession(ContainerConfig.Container.Property client) {
        Properties props = new Properties();
        Map clientProps = client.properties;
        if (clientProps != null) {
            Iterator i = clientProps.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry e = (Map.Entry) i.next();
                ContainerConfig.Container.Property p = (ContainerConfig.Container.Property) e.getValue();
                props.setProperty(p.name.toLowerCase(), p.value);
            }
        }
        return Session.getInstance(props);
    }

    protected Store getStore(Session session) throws ContainerException {
        // create the store object
        Store store;
        try {
            store = session.getStore();
        } catch (NoSuchProviderException e) {
            throw new ContainerException(e);
        }

        // re-write the URLName including the password for this store
        if (store != null && store.getURLName() != null) {
            URLName urlName = this.updateUrlName(store.getURLName(), session.getProperties());
            if (Debug.verboseOn()) Debug.logVerbose("URLName - " + urlName.toString(), module);
            try {
                store = session.getStore(urlName);
            } catch (NoSuchProviderException e) {
                throw new ContainerException(e);
            }
        }

        if (store == null) {
            throw new ContainerException("No store configured!");
        }

        // test the store
        try {
            store.connect();
            store.close();
        } catch (MessagingException e) {
            Debug.logError("Unable to connect to mail store : " + store.getURLName().toString() + " : " + e.getMessage(), module);
        }

        return store;
    }

    protected URLName updateUrlName(URLName urlName, Properties props) {
        String protocol = urlName.getProtocol();
        String userName = urlName.getUsername();
        String password = urlName.getPassword();
        String host = urlName.getHost();
        String file = urlName.getFile();
        int port = urlName.getPort();

        // check the username
        if (UtilValidate.isEmpty(userName)) {
            userName = props.getProperty("mail." + protocol + ".user");
            if (UtilValidate.isEmpty(userName)) {
                userName = props.getProperty("mail.user");
            }
        }

        // check the password; update with the non-standard property
        if (UtilValidate.isEmpty(password)) {
            password = props.getProperty("mail." + protocol + ".pass");
            if (UtilValidate.isEmpty(password)) {
                password = props.getProperty("mail.pass");
            }
        }

        // check the host
        if (UtilValidate.isEmpty(host)) {
            host = props.getProperty("mail." + protocol + ".host");
            if (UtilValidate.isEmpty(host)) {
                host = props.getProperty("mail.host");
            }
        }

        if (Debug.verboseOn()) Debug.logVerbose("Update URL - " + protocol + "://" + userName + "@" + host + ":" + port + "!" + password + ";" + file, module);
        return new URLName(protocol, host, port, file, userName, password);
    }

    class LoggingStoreListener implements StoreListener {

        public void notification(StoreEvent event) {
            String typeString = "";
            switch(event.getMessageType()) {
                case StoreEvent.ALERT:
                    typeString = "ALERT: ";
                    break;
                case StoreEvent.NOTICE:
                    typeString = "NOTICE: ";
                    break;
            }

            if (Debug.verboseOn()) Debug.logVerbose("JavaMail " + typeString + event.getMessage(), module);
        }
    }

    class PollerTask extends TimerTask {

        LocalDispatcher dispatcher;
        GenericValue userLogin;

        public PollerTask(LocalDispatcher dispatcher, GenericValue userLogin) {
            this.dispatcher = dispatcher;
            this.userLogin = userLogin;
        }

        public void run() {
            if (stores != null && stores.size() > 0) {
                Iterator i = stores.keySet().iterator();
                while (i.hasNext()) {
                    Store store = (Store) i.next();
                    Session session = (Session) stores.get(store);
                    try {
                        checkMessages(store, session);
                    } catch (GeneralException e) {
                        Debug.logError(e, "Mail service invocation error", module);
                    } catch (MessagingException e) {
                        Debug.logError(e, "Mail message error", module);
                    }
                }
            }
        }

        protected void checkMessages(Store store, Session session) throws MessagingException, GeneralException {
            store.connect();

            // open the default folder
            Folder folder = store.getDefaultFolder();
            if (folder == null) {
                throw new MessagingException("No default folder available");
            }

            // open the inbox
            folder = folder.getFolder(INBOX);
            if (folder == null) {
                throw new MessagingException("No INBOX folder available");
            }

            // get the message count; stop if nothing to do
            folder.open(Folder.READ_WRITE);
            int totalMessages = folder.getMessageCount();
            if (totalMessages == 0) {
                folder.close(false);
                store.close();
                return;
            }

            // get all messages
            Message[] messages = folder.getMessages();
            FetchProfile profile = new FetchProfile();
            profile.add(FetchProfile.Item.ENVELOPE);
            profile.add(FetchProfile.Item.FLAGS);
            profile.add("X-Mailer");
            folder.fetch(messages, profile);

            // process each message
            for (int i = 0; i < messages.length; i++) {
                // process each un-read message
            	if (!messages[i].isSet(Flags.Flag.SEEN)) {
            		long messageSize = messages[i].getSize();
            		if (messages[i] instanceof MimeMessage && messageSize >= maxSize) {
            			Debug.logWarning("Message from: " + messages[i].getFrom()[0] + "not received, to big, size:" + messageSize + " cannot be more than " + maxSize + " bytes", module);
            		} else {
            			this.processMessage(messages[i], session);
            			if (Debug.verboseOn()) Debug.logVerbose("Message from " + UtilMisc.toListArray(messages[i].getFrom()) + " with subject [" + messages[i].getSubject() + "]  has been processed." , module);
            			messages[i].setFlag(Flags.Flag.SEEN, true);
            			if (Debug.verboseOn()) Debug.logVerbose("Message [" + messages[i].getSubject() + "] is marked seen", module);
            		}
            	}
            	if (deleteMail) {
            		if (Debug.verboseOn()) Debug.logVerbose("Message [" + messages[i].getSubject() + "] is being deleted", module);
            		messages[i].setFlag(Flags.Flag.DELETED, true);
            	}
            }

            // expunge and close the folder
            folder.close(true);
            store.close();
        }

        protected void processMessage(Message message, Session session) {
            if (message instanceof MimeMessage) {
                MimeMessageWrapper wrapper = new MimeMessageWrapper(session, (MimeMessage) message);
                try {
                    ServiceMcaUtil.evalRules(dispatcher, wrapper, userLogin);
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Problem processing message", module);
                }
            }
        }
    }
}
