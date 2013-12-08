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
package org.ofbiz.base.container;

import java.net.URL;

import org.ofbiz.base.start.Classpath;
import org.ofbiz.base.util.CachedClassLoader;
import org.ofbiz.base.util.Debug;

/**
 * ClassLoader Container; Created a CachedClassLoader for use by all following containers
 *
 */
public class ClassLoaderContainer implements Container {

    public static final String module = ClassLoaderContainer.class.getName();
    protected static CachedClassLoader cl = null;
    public static Integer portOffset = 0;
    private String name;

    @Override
    public void init(String[] args, String name, String configFile) throws ContainerException {
        this.name = name;
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        if (parent == null) {
            parent = Classpath.class.getClassLoader();
        }
        if (parent == null) {
            parent = ClassLoader.getSystemClassLoader();
        }

        cl = new CachedClassLoader(new URL[0], parent);
        
        if (args != null) {
            for (String argument : args) {
                // arguments can prefix w/ a '-'. Just strip them off
                if (argument.startsWith("-")) {
                    int subIdx = 1;
                    if (argument.startsWith("--")) {
                        subIdx = 2;
                    }
                    argument = argument.substring(subIdx);
                }

                // parse the arguments
                if (argument.indexOf("=") != -1) {
                    String argumentName = argument.substring(0, argument.indexOf("="));
                    String argumentVal = argument.substring(argument.indexOf("=") + 1);

                    if ("portoffset".equalsIgnoreCase(argumentName) && !"${portoffset}".equals(argumentVal)) {
                        try {
                            ClassLoaderContainer.portOffset = Integer.valueOf(argumentVal);
                        } catch (NumberFormatException e) {
                            Debug.logError(e, module);
                        }
                    }
                }
            }
        }
        
        Thread.currentThread().setContextClassLoader(cl);
        Debug.logInfo("CachedClassLoader created", module);
    }

    /**
     * @see org.ofbiz.base.container.Container#start()
     */
    @Override
    public boolean start() throws ContainerException {
        return true;
    }

    /**
     * @see org.ofbiz.base.container.Container#stop()
     */
    @Override
    public void stop() throws ContainerException {
    }

    @Override
    public String getName() {
        return name;
    }

    public static ClassLoader getClassLoader() {
        if (cl != null) {
            return cl;
        } else {
            return ClassLoader.getSystemClassLoader();
        }
    }
}
