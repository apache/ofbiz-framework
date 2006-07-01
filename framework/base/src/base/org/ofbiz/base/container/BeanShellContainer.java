/*
 * $Id: BeanShellContainer.java 5462 2005-08-05 18:35:48Z jonesde $
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
package org.ofbiz.base.container;

import bsh.Interpreter;
import bsh.EvalError;

import org.ofbiz.base.util.Debug;

/**
 * BeanShellContainer - Container implementation for BeanShell
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
  *@version    $Rev$
 * @since      3.0
 */
public class BeanShellContainer implements Container {

    public static final String module = BeanShellContainer.class.getName();

    protected String configFileLocation = null;
    protected Interpreter bsh = null;
    protected String name;
    protected int port;

    /**
     * @see org.ofbiz.base.container.Container#init(java.lang.String[], java.lang.String)
     */
    public void init(String[] args, String configFile) {
        this.configFileLocation = configFile;
    }
    
    /**
     * @see org.ofbiz.base.container.Container#init(java.lang.String[], java.lang.String)
     */
    public boolean start() throws ContainerException {
        // get the container config
        ContainerConfig.Container cfg = ContainerConfig.getContainer("beanshell-container", configFileLocation);

        // get the app-name
        ContainerConfig.Container.Property appName = cfg.getProperty("app-name");
        if (appName == null || appName.value == null || appName.value.length() == 0) {
            throw new ContainerException("Invalid app-name defined in container configuration");
        } else {
            this.name = appName.value;
        }

        // get the telnet-port
        ContainerConfig.Container.Property telnetPort = cfg.getProperty("telnet-port");
        if (telnetPort == null || telnetPort.value == null || telnetPort.value.length() == 0) {
            throw new ContainerException("Invalid telnet-port defined in container configuration");
        } else {
            try {
                this.port = Integer.parseInt(telnetPort.value);
            } catch (Exception e) {
                throw new ContainerException("Invalid telnet-port defined in container configuration; not a valid int");
            }
        }

        // create the interpreter
        bsh = new Interpreter();

        // configure the interpreter
        if (bsh != null) {
            try {
                bsh.set(name, this);
            } catch (EvalError evalError) {
                throw new ContainerException(evalError);
            }
            try {
                bsh.set("portnum", (port - 1));
            } catch (EvalError evalError) {
                throw new ContainerException(evalError);
            }
            try {
                bsh.eval("setAccessibility(true)");
            } catch (EvalError evalError) {
                throw new ContainerException(evalError);
            }

            try {
                bsh.eval("server(portnum)");
            } catch (EvalError evalError) {
                throw new ContainerException(evalError);
            }

            Debug.logInfo("Started BeanShell telnet service on " + (port - 1) + ", " + port, module);
            Debug.logInfo("NOTICE: BeanShell service ports are not secure. Please protect the ports", module);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Stop the container
     *
     * @throws ContainerException
     *
     */
    public void stop() throws ContainerException {
        bsh = null;
    }
}
