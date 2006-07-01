/*
 * $Id: XuiContainer.java 7215 2006-04-06 14:42:02Z les7arts $
 *
 * Copyright (c) 2004 The Open For Business Project - www.ofbiz.org
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

/**
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      3.1
 */
public abstract class XuiContainer implements Container {

    public static final String module = XuiContainer.class.getName();
    protected static XuiSession session = null;

    protected XuiScreen initialScreen = null;

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
        LocalDispatcher dispatcher = null;
        try {
            dispatcher = GenericDispatcher.getLocalDispatcher(dispatcherName, delegator);
        } catch (GenericServiceException e) {
            throw new ContainerException(e);
        }

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
            this.startupFile = ContainerConfig.getPropertyValue(cc, "startup-file", "xui.properties");
        }
        this.initialScreen = new XuiScreen();
        this.initialScreen.setup(this.startupFile);                

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

        public void setup(String startupFile) {
            String xuiProps = System.getProperty("ofbiz.home") + "/applications/pos/config/" + startupFile;
            String suffix = Locale.getDefault().getLanguage();
            if ("en" == suffix ) 
                suffix = "";
            else
                suffix = "_" + suffix;            
            UtilProperties.setPropertyValue(xuiProps, "Language", "XuiLabels" + suffix);            
            JFrame frame = new JFrame();
            frame.setUndecorated(true);
            frame.setVisible(false);
            frame.getContentPane().add(this);            
            super.setup(frame, new String[] { startupFile });
        }
    }
}
