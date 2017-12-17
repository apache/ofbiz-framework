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
package org.apache.ofbiz.service.mail;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.mail.FetchProfile;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;
import javax.mail.event.StoreEvent;
import javax.mail.event.StoreListener;
import javax.mail.internet.MimeMessage;
import javax.mail.search.FlagTerm;

import org.apache.ofbiz.base.container.Container;
import org.apache.ofbiz.base.container.ContainerConfig;
import org.apache.ofbiz.base.container.ContainerException;
import org.apache.ofbiz.base.start.StartupCommand;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceContainer;

public class JavaMailContainer implements Container {

    public static final String module = JavaMailContainer.class.getName();
    public static final String INBOX = "INBOX";

    protected Delegator delegator = null;
    protected LocalDispatcher dispatcher = null;
    protected GenericValue userLogin = null;
    protected long timerDelay = 300000;
    protected long maxSize = 1000000;
    protected ScheduledExecutorService pollTimer = null;
    protected boolean deleteMail = false;    // whether to delete emails after fetching them.

    protected String configFile = null;
    protected Map<Store, Session> stores = null;
    private String name;
    /**
     * Initialize the container
     *
     * @param ofbizCommands command line arguments
     * @param configFile Location of master OFBiz configuration file
     * @throws org.apache.ofbiz.base.container.ContainerException
     *
     */
    @Override
    public void init(List<StartupCommand> ofbizCommands, String name, String configFile) throws ContainerException {
        this.name = name;
        this.configFile = configFile;
        this.stores = new LinkedHashMap<>();
        this.pollTimer = Executors.newScheduledThreadPool(1);
    }

    /**
     * Start the container
     *
     * @return true if server started
     * @throws org.apache.ofbiz.base.container.ContainerException
     *
     */
    @Override
    public boolean start() throws ContainerException {
        ContainerConfig.Configuration cfg = ContainerConfig.getConfiguration(name, configFile);
        String dispatcherName = ContainerConfig.getPropertyValue(cfg, "dispatcher-name", "JavaMailDispatcher");
        String delegatorName = ContainerConfig.getPropertyValue(cfg, "delegator-name", "default");
        this.deleteMail = "true".equals(ContainerConfig.getPropertyValue(cfg, "delete-mail", "false"));

        this.delegator = DelegatorFactory.getDelegator(delegatorName);
        this.dispatcher = ServiceContainer.getLocalDispatcher(dispatcherName, delegator);
        this.timerDelay = ContainerConfig.getPropertyValue(cfg, "poll-delay", 300000);
        this.maxSize = ContainerConfig.getPropertyValue(cfg, "maxSize", 1000000); // maximum size in bytes

        // load the userLogin object
        String runAsUser = ContainerConfig.getPropertyValue(cfg, "run-as-user", "system");
        try {
            this.userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", runAsUser).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to load run-as-user UserLogin; cannot start container", module);
            return false;
        }

        // load the MCA configuration
        ServiceMcaUtil.readConfig();

        // load the listeners
        List<ContainerConfig.Configuration.Property> configs = cfg.getPropertiesWithValue("store-listener");
        for (ContainerConfig.Configuration.Property prop: configs) {
            Session session = this.makeSession(prop);
            Store store = this.getStore(session);
            stores.put(store, session);
            store.addStoreListener(new LoggingStoreListener());
        }

        // start the polling timer
        if (stores != null) {
            pollTimer.scheduleAtFixedRate(new PollerTask(dispatcher, userLogin), timerDelay, timerDelay, TimeUnit.MILLISECONDS);
        } else {
            Debug.logWarning("No JavaMail Store(s) configured; poller disabled.", module);
        }

        return true;
    }

    /**
     * Stop the container
     *
     * @throws org.apache.ofbiz.base.container.ContainerException
     *
     */
    @Override
    public void stop() throws ContainerException {
        // stop the poller
        this.pollTimer.shutdown();
        Debug.logWarning("stop JavaMail poller", module);
    }

    @Override
    public String getName() {
        return name;
    }

    // java-mail methods
    protected Session makeSession(ContainerConfig.Configuration.Property client) {
        Properties props = new Properties();
        Map<String, ContainerConfig.Configuration.Property> clientProps = client.properties;
        if (clientProps != null) {
            for (ContainerConfig.Configuration.Property p: clientProps.values()) {
                props.setProperty(p.name.toLowerCase(Locale.getDefault()), p.value);
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
            if (Debug.verboseOn()) {
                Debug.logVerbose("URLName - " + urlName.toString(), module);
            }
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

        // check the port
        int portProps = 0;
        String portStr = props.getProperty("mail." + protocol + ".port");
        if (UtilValidate.isNotEmpty(portStr)) {
            try {
                portProps = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                Debug.logError("The port given in property mail." + protocol + ".port is wrong, please check", module);
            }
        }
        if (portProps == 0) {
            portStr = props.getProperty("mail.port");
            if (UtilValidate.isNotEmpty(portStr)) {
                try {
                    portProps = Integer.parseInt(props.getProperty("mail.port"));
                } catch (NumberFormatException e) {
                    Debug.logError("The port given in property mail.port is wrong, please check", module);
                }
            }
        }
        // override the port if have found one.
        if (portProps != 0) {
            port = portProps;
        }

        if (Debug.verboseOn()) {
            Debug.logVerbose("Update URL - " + protocol + "://" + userName + "@" + host + ":" + port + "!" + password + ";" + file, module);
        }
        return new URLName(protocol, host, port, file, userName, password);
    }

    static class LoggingStoreListener implements StoreListener {

        @Override
        public void notification(StoreEvent event) {
            String typeString = "";
            switch (event.getMessageType()) {
                case StoreEvent.ALERT:
                    typeString = "ALERT: ";
                    break;
                case StoreEvent.NOTICE:
                    typeString = "NOTICE: ";
                    break;
                default:
                    Debug.logWarning("There was a case error in LoggingStoreListener.notification", module);
            }

            if (Debug.verboseOn()) {
                Debug.logVerbose("JavaMail " + typeString + event.getMessage(), module);
            }
        }
    }

    class PollerTask implements Runnable {

        LocalDispatcher dispatcher;
        GenericValue userLogin;

        public PollerTask(LocalDispatcher dispatcher, GenericValue userLogin) {
            this.dispatcher = dispatcher;
            this.userLogin = userLogin;
        }

        @Override
        public void run() {
            if (stores != null) {
                for (Map.Entry<Store, Session> entry: stores.entrySet()) {
                    Store store = entry.getKey();
                    Session session = entry.getValue();
                    try {
                        checkMessages(store, session);
                    } catch (Exception e) {
                        // Catch all exceptions so the loop will continue running
                        Debug.logError("Mail service invocation error for mail store " + store + ": " + e, module);
                    }
                    if (store.isConnected()) {
                        try {
                            store.close();
                        } catch (Exception e) {}
                    }
                }
            }
        }

        protected void checkMessages(Store store, Session session) throws MessagingException {
            if (!store.isConnected()) {
                store.connect();
            }

            // open the default folder
            Folder folder = store.getDefaultFolder();
            if (!folder.exists()) {
                throw new MessagingException("No default (root) folder available");
            }

            // open the inbox
            folder = folder.getFolder(INBOX);
            if (!folder.exists()) {
                throw new MessagingException("No INBOX folder available");
            }

            // get the message count; stop if nothing to do
            folder.open(Folder.READ_WRITE);
            int totalMessages = folder.getMessageCount();
            if (totalMessages == 0) {
                folder.close(false);
                return;
            }

            // get all messages
            Message[] messages = folder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
            FetchProfile profile = new FetchProfile();
            profile.add(FetchProfile.Item.ENVELOPE);
            profile.add(FetchProfile.Item.FLAGS);
            profile.add("X-Mailer");
            folder.fetch(messages, profile);

            // process each message
            for (Message message: messages) {
                // process each un-read message
                if (!message.isSet(Flags.Flag.SEEN)) {
                    long messageSize = message.getSize();
                    if (message instanceof MimeMessage && messageSize >= maxSize) {
                        Debug.logWarning("Message from: " + message.getFrom()[0] + "not received, too big, size:" + messageSize + " cannot be more than " + maxSize + " bytes", module);

                        // set the message as read so it doesn't continue to try to process; but don't delete it
                        message.setFlag(Flags.Flag.SEEN, true);
                    } else {
                        this.processMessage(message, session);
                        if (Debug.verboseOn()) {
                            Debug.logVerbose("Message from " + UtilMisc.toListArray(message.getFrom()) + " with subject [" + message.getSubject() + "]  has been processed." , module);
                        }
                        message.setFlag(Flags.Flag.SEEN, true);
                        if (Debug.verboseOn()) {
                            Debug.logVerbose("Message [" + message.getSubject() + "] is marked seen", module);
                        }

                        // delete the message after processing
                        if (deleteMail) {
                            if (Debug.verboseOn()) {
                                Debug.logVerbose("Message [" + message.getSubject() + "] is being deleted", module);
                            }
                            message.setFlag(Flags.Flag.DELETED, true);
                        }
                    }
                }
            }

            // expunge and close the folder
            folder.close(true);
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
