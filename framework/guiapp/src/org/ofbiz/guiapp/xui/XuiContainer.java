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
package org.ofbiz.guiapp.xui;

import java.util.Locale;

import javax.swing.JFrame;
import javax.swing.UIManager;

import net.xoetrope.swing.XApplet;

import org.ofbiz.base.container.Container;
import org.ofbiz.base.container.ContainerConfig;
import org.ofbiz.base.container.ContainerException;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.service.GenericDispatcher;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.base.util.UtilProperties;

public abstract class XuiContainer implements Container {

    public static final String module = XuiContainer.class.getName();
    protected static XuiSession session = null;

    protected XuiScreen initialScreen = null;

    protected String startupDir = null;
    protected String startupFile = null;
    protected String configFile = null;

    public void init(String[] args, String configFile) throws ContainerException {
        this.configFile = configFile;        
    }

    public boolean start() throws ContainerException {
        // make sure the subclass sets the config name
        if (this.getContainerConfigName() == null) {
            throw new ContainerException("Unknown container config name");
        }
        // get the container config
        ContainerConfig.Container cc = ContainerConfig.getContainer(this.getContainerConfigName(), configFile);
        if (cc == null) {
            throw new ContainerException("No " + this.getContainerConfigName() + " configuration found in container config!");
        }

        // get the delegator
        String delegatorName = ContainerConfig.getPropertyValue(cc, "delegator-name", "default");
        GenericDelegator delegator = GenericDelegator.getGenericDelegator(delegatorName);

        // get the dispatcher
        String dispatcherName = ContainerConfig.getPropertyValue(cc, "dispatcher-name", "xui-dispatcher");
        LocalDispatcher dispatcher = GenericDispatcher.getLocalDispatcher(dispatcherName, delegator);

        // get the pre-defined session ID
        String xuiSessionId = ContainerConfig.getPropertyValue(cc, "xui-session-id", null);
        if (UtilValidate.isEmpty(xuiSessionId)) {
            throw new ContainerException("No xui-session-id value set in " + this.getContainerConfigName() + "!");
        }

        String laf = ContainerConfig.getPropertyValue(cc, "look-and-feel", null);
        if (UtilValidate.isNotEmpty(laf)) {
            try {
                UIManager.setLookAndFeel(laf);
            } catch (Exception e) {
                throw new ContainerException(e);
            } 
        }

        // create and cache the session
        session = new XuiSession(xuiSessionId, delegator, dispatcher, this);

        // configure the rest of the container
        this.configure(cc);

        // load the XUI and render the initial screen
        if (this.startupFile == null) {
            this.startupDir = ContainerConfig.getPropertyValue(cc, "startup-directory", "/specialpurpose/pos/config/");
            this.startupFile = ContainerConfig.getPropertyValue(cc, "startup-file", "xpos.properties");
        }
        this.initialScreen = new XuiScreen();
        this.initialScreen.setup(this.startupDir, this.startupFile);                

        return true;
    }

    public void stop() throws ContainerException {
    }

    public String getXuiPropertiesName() {
        return this.startupFile;
    }

    /**
     * @return String the name of the container name property
     */
    public abstract String getContainerConfigName();

    /**
     * Implementation specific configuration from the container config
     * This method is called after the initial XUI configuration, after
     * the session creation; before the initial screen is rendered.
     *
     * @param cc The container config object used to obtain the information
     * @throws ContainerException
     */
    public abstract void configure(ContainerConfig.Container cc) throws ContainerException;

    public static XuiSession getSession() {
        return session;
    }

    class XuiScreen extends XApplet {

        public void setup(String startupDir, String startupFile) {
            String xuiProps = System.getProperty("ofbiz.home") + startupDir + startupFile;
            String suffix = Locale.getDefault().getLanguage();
            if ("en".equals(suffix)) {
                suffix = "";
            } else {
                suffix = "_" + suffix;
            }
            UtilProperties.setPropertyValue(xuiProps, "Language", "XuiLabels" + suffix);            
            JFrame frame = new JFrame();
            frame.setUndecorated(true);
            frame.setVisible(false);
            frame.getContentPane().add(this);            
            super.setup(frame, new String[] { startupFile });
        }
    }
}
