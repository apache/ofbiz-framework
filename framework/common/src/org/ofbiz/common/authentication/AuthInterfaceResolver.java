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

package org.ofbiz.common.authentication;

import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.ofbiz.base.util.Debug;
import org.ofbiz.common.authentication.api.Authenticator;

/**
 * AuthInterfaceResolver
 *
 * Discovers implementations of Authenticator on the class path (implementations must be in org.ofbiz.* package)
 */
public class AuthInterfaceResolver {

    private static final String module = AuthInterfaceResolver.class.getName();
    protected List<Class> authenticators = new ArrayList<Class>();
    protected ClassLoader loader;

    public AuthInterfaceResolver() {
        loader = getContextClassLoader();
    }

    public List<Class> getImplementations() {
        find("org.ofbiz");
        return authenticators;
    }

    protected void find(String packageName) {
        packageName = packageName.replace('.', '/');
        Enumeration<URL> urls;

        try {
            urls = loader.getResources(packageName);
        }
        catch (IOException io) {
            Debug.logWarning(io, "Could not read package: " + packageName, module);
            return;
        }

        while (urls.hasMoreElements()) {
            try {
                String urlPath = urls.nextElement().getFile();
                urlPath = URLDecoder.decode(urlPath, "UTF-8");
                if (Debug.verboseOn())
                    Debug.logVerbose("Found library file [" + urlPath + "]", module);

                if (urlPath.startsWith("file:")) {
                    urlPath = urlPath.substring(5);
                }

                if (urlPath.indexOf('!') > 0) {
                    urlPath = urlPath.substring(0, urlPath.indexOf('!'));
                }

                if (Debug.verboseOn())
                    Debug.logVerbose("Scanning for classes in [" + urlPath + "]", module);

                File file = new File(urlPath);
                if (file.isDirectory()) {
                    readDirectory(packageName, file);
                } else {
                    readJar(packageName, file);
                }
            }
            catch (IOException io) {
                Debug.logError(io, "Could not read resource entries", module);
            }
        }
    }

    protected void readDirectory(String parent, File location) {
        File[] files = location.listFiles();
        StringBuffer buf;

        if (files == null) {
            Debug.logWarning("Could not list directory " + location.getAbsolutePath() + " when looking for component classes", module);
            return;
        }

        for (File file : files) {
            buf = new StringBuffer();
            buf.append(parent);
            if (buf.length() > 0)
                buf.append("/");
            buf.append(file.getName());
            String packageOrClass = (parent == null ? file.getName() : buf.toString());

            if (file.isDirectory()) {
                readDirectory(packageOrClass, file);
            } else if (file.getName().endsWith(".class")) {
                checkFile(packageOrClass);
            }
        }
    }

    protected void readJar(String parent, File jarfile) {
        try {
            JarEntry entry;
            JarInputStream jarStream = new JarInputStream(new FileInputStream(jarfile));

            while ((entry = jarStream.getNextJarEntry()) != null) {
                String name = entry.getName();
                if (!entry.isDirectory() && name.startsWith(parent) && name.endsWith(".class")) {
                    checkFile(name);
                }
            }
        }
        catch (IOException io) {
            Debug.logError(io, "Could not search jar file [" + jarfile + "]", module);
        }
    }

    protected void checkFile(String name) {
        String externalName = name.substring(0, name.indexOf('.')).replace('/', '.');
        if (Debug.verboseOn())
            Debug.logVerbose("Converted file value [" + name + "] to class [" + externalName + "]", module);
        try {
            resolveClass(loader.loadClass(externalName));
        } catch (ClassNotFoundException e) {
            Debug.logWarning("No class found - " + externalName, module);
        }
    }

    protected ClassLoader getContextClassLoader() {
        return AccessController.doPrivileged(
                new PrivilegedAction<ClassLoader>() {
                    public ClassLoader run() {
                        ClassLoader cl = null;
                        try {
                            cl = Thread.currentThread().getContextClassLoader();

                        } catch (SecurityException e) {
                            Debug.logError(e, e.getMessage(), module);
                        }
                        return cl;
                    }
                });
    }


    public void resolveClass(Class clazz) {
        Class[] ifaces = clazz.getInterfaces();
        for (Class iface : ifaces) {
            if (Authenticator.class.equals(iface)) {
                authenticators.add(clazz);
            }
        }
    }
}

