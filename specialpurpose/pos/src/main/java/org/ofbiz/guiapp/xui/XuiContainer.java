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
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.DelegatorFactory;
import org.ofbiz.entity.util.EntityUtilProperties;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.service.ServiceContainer;

public abstract class XuiContainer implements Container {

    public static final String module = XuiContainer.class.getName();
    protected static XuiSession xuiSession = null;

    protected String startupDir = null;
    protected String startupFile = null;
    protected String configFile = null;
    protected String name;

    public void init(String[] args, String name, String configFile) throws ContainerException {
        this.name = name;
        this.configFile = configFile;
    }

    public boolean start() throws ContainerException {
        // make sure the subclass sets the config name
        if (name == null) {
            throw new ContainerException("Unknown container config name");
        }
        // get the container config
        ContainerConfig.Container cc = ContainerConfig.getContainer(name, configFile);
        if (cc == null) {
            throw new ContainerException("No " + name + " configuration found in container config!");
        }

        // get the delegator
        String delegatorName = ContainerConfig.getPropertyValue(cc, "delegator-name", "default");
        Delegator delegator = null;
        delegator = DelegatorFactory.getDelegator(delegatorName);

        // get the dispatcher
        String dispatcherName = ContainerConfig.getPropertyValue(cc, "dispatcher-name", "xui-dispatcher");
        LocalDispatcher dispatcher = ServiceContainer.getLocalDispatcher(dispatcherName, delegator);

        // get the pre-defined session ID
        String xuiSessionId = ContainerConfig.getPropertyValue(cc, "xui-session-id", null);
        if (UtilValidate.isEmpty(xuiSessionId)) {
            throw new ContainerException("No xui-session-id value set in " + name + "!");
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
        xuiSession = new XuiSession(xuiSessionId, delegator, dispatcher, this);

        // configure the rest of the container
        this.configure(cc);

        // load the XUI and render the initial screen
        if (this.startupFile == null) {
            this.startupDir = ContainerConfig.getPropertyValue(cc, "startup-directory", "specialpurpose/pos/config/");
            this.startupFile = ContainerConfig.getPropertyValue(cc, "startup-file", "xpos.properties");
        }

        String classPackageName = ContainerConfig.getPropertyValue(cc, "class-package-name", "net.xoetrope.swing");

        JFrame jframe = new JFrame();
        jframe.setUndecorated(true);
        new XuiScreen(
                new String[] { this.startupDir + this.startupFile,
                classPackageName}, jframe, delegator);
        return true;
    }

    public void stop() throws ContainerException {
    }

    public String getName() {
        return name;
    }

    public String getXuiPropertiesName() {
        return this.startupFile;
    }

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
        return xuiSession;
    }

    @SuppressWarnings("serial")
    class XuiScreen extends XApplet {
        protected String startupProperties = "";

        public XuiScreen(String[] args, JFrame frame, Delegator delegator) {
            super(args, frame);
            if (args.length > 0) {
                startupProperties = args[0];
            }
            String languageSuffix = EntityUtilProperties.getPropertyValue("xui", "languageSuffix", "", delegator);
            String suffix = null;
            if(UtilValidate.isEmpty(languageSuffix)) {
                suffix = Locale.getDefault().getLanguage();
            } else {
                suffix = languageSuffix;
            }
            if ("en".equals(suffix)) {
                suffix = "";
            } else {
                suffix = "_" + suffix;
            }
            String language = EntityUtilProperties.getPropertyValue(startupProperties, "Language", delegator);
            if (language.compareTo("XuiLabels" + suffix) != 0) {
                UtilProperties.setPropertyValue(startupProperties, "Language", "XuiLabels" + suffix);
            }
            if (suffix.equals("_zh")) { // TODO maybe needed for other languages using non Latin alphabet http://en.wikipedia.org/wiki/Alphabet#Types
                UtilProperties.setPropertyValue(startupProperties, "StyleFile", "posstyles" + suffix + ".xml"); // For the moment only a Chinese StyleFile is provided
            } else {
                UtilProperties.setPropertyValue(startupProperties, "StyleFile", "posstyles.xml"); // Languages using Latin alphabet
            }
            frame.setVisible(true);
            frame.getContentPane().add(this);
            frame.validate();
        }
    }
}
