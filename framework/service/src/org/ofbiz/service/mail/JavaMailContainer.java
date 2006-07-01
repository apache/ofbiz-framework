/*
 * $Id: JavaMailContainer.java 7788 2006-06-13 18:26:15Z jonesde $
 *
 * Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
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

/**
 * 
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      3.3
 */
public class JavaMailContainer implements Container {

    public static final String module = JavaMailContainer.class.getName();
    public static final String INBOX = "INBOX";

    protected GenericDelegator delegator = null;
    protected LocalDispatcher dispatcher = null;
    protected GenericValue userLogin = null;
    protected long timerDelay = 300000;
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
        if ("true".equals(ContainerConfig.getPropertyValue(cfg, "delete-mail", "false"))) {
            this.deleteMail = true;
        } else {
            this.deleteMail = false;
        }
        
        this.delegator = GenericDelegator.getGenericDelegator(delegatorName);
        this.dispatcher = new GenericDispatcher(dispatcherName, delegator);
        this.timerDelay = (long) ContainerConfig.getPropertyValue(cfg, "poll-delay", 300000);

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
        Debug.logInfo("stop JavaMail poller", module);
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
                props.setProperty(p.name.toLowerCase(), p.value.toLowerCase());
            }
        }
        return Session.getInstance(props);
    }

    protected Store getStore(Session session) throws ContainerException {
        // create the store object
        Store store = null;
        try {
            store = session.getStore();
        } catch (NoSuchProviderException e) {
            throw new ContainerException(e);
        }

        // re-write the URLName including the password for this store
        if (store != null && store.getURLName() != null) {
            URLName urlName = this.updateUrlName(store.getURLName(), session.getProperties());
            Debug.log("URLName - " + urlName.toString(), module);
            try {
                store = session.getStore(urlName);
            } catch (NoSuchProviderException e) {
                throw new ContainerException(e);
            }
        }

        // test the store
        try {
            store.connect();
            store.close();
        } catch (MessagingException e) {
            Debug.logError("Unable to connect to mail store : " + store.getURLName().toString(), module);
            return null;
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

        Debug.logInfo("Update URL - " + protocol + "://" + userName + "@" + host + ":" + port + "!" + password + ";" + file, module);
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

            Debug.log("JavaMail " + typeString + event.getMessage(), module);
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
            store.addStoreListener(new LoggingStoreListener());
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
                    this.processMessage(messages[i], session);
                    Debug.logVerbose("Message from " + UtilMisc.toListArray(messages[i].getFrom()) + " with subject [" + messages[i].getSubject() + "]  has been processed." , module);
                    messages[i].setFlag(Flags.Flag.SEEN, true);
                    Debug.logVerbose("Message [" + messages[i].getSubject() + "] is marked seen", module);
                }
                if (deleteMail) {
                    Debug.logVerbose("Message [" + messages[i].getSubject() + "] is being deleted", module);
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
