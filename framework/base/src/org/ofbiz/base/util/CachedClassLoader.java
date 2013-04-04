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
package org.ofbiz.base.util;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Caching Class Loader
 *
 */
public class CachedClassLoader extends URLClassLoader {

    public interface Init {
        void loadClasses(ClassLoader loader) throws ClassNotFoundException;
    }

    public static final String module = CachedClassLoader.class.getName();
    public static final ConcurrentHashMap<String, Class<?>> globalClassNameClassMap = new ConcurrentHashMap<String, Class<?>>();
    private static final Set<String> globalBadClassNameSet = new CopyOnWriteArraySet<String>();
    private static final ConcurrentHashMap<String, URL> globalResourceMap = new ConcurrentHashMap<String, URL>();
    private static final Set<String> globalBadResourceNameSet = new CopyOnWriteArraySet<String>();

    private final String contextName;
    private final ConcurrentHashMap<String, Class<?>> localClassNameClassMap = new ConcurrentHashMap<String, Class<?>>();
    private final Set<String> localBadClassNameSet = new CopyOnWriteArraySet<String>();
    private final ConcurrentHashMap<String, URL> localResourceMap = new ConcurrentHashMap<String, URL>();
    private final Set<String> localBadResourceNameSet = new CopyOnWriteArraySet<String>();

    static {
        // setup some commonly used classes...
        registerClass(java.lang.Object.class);
        registerClass(java.lang.String.class);
        registerClass(java.lang.Boolean.class);
        registerClass(java.math.BigDecimal.class);
        registerClass(java.lang.Double.class);
        registerClass(java.lang.Float.class);
        registerClass(java.lang.Long.class);
        registerClass(java.lang.Integer.class);
        registerClass(java.lang.Short.class);
        registerClass(java.lang.Byte.class);
        registerClass(java.lang.Character.class);
        registerClass(java.sql.Timestamp.class);
        registerClass(java.sql.Time.class);
        registerClass(java.sql.Date.class);
        registerClass(java.util.Locale.class);
        registerClass(java.util.Date.class);
        registerClass(java.util.Collection.class);
        registerClass(java.util.List.class);
        registerClass(java.util.Set.class);
        registerClass(java.util.Map.class);
        registerClass(java.util.HashMap.class);
        registerClass(java.util.TimeZone.class);
        registerClass(java.util.Locale.class);

        // setup the primitive types
        registerClass(Boolean.TYPE);
        registerClass(Short.TYPE);
        registerClass(Integer.TYPE);
        registerClass(Long.TYPE);
        registerClass(Float.TYPE);
        registerClass(Double.TYPE);
        registerClass(Byte.TYPE);
        registerClass(Character.TYPE);

        // setup OFBiz classes
        registerClass(org.ofbiz.base.util.TimeDuration.class);

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        for(Init cachedClassLoader : ServiceLoader.load(Init.class, loader)) {
            try {
                cachedClassLoader.loadClasses(loader);
            } catch (Exception e) {
                Debug.logError(e, "Could not pre-initialize dynamically loaded class: ", module);
            }
        }
    }

    /** Registers a <code>Class</code> with the class loader. The class will be
     * added to the global class cache, and an alias name will be created.
     * <p>The alias name is the right-most portion of the binary name. Example:
     * the alias for <code>java.lang.Object</code> is <code>Object</code>.
     * If the alias already exists for another class, then no alias is created
     * (the previously aliased class takes precedence).</p>
     *
     * @param theClass The <code>Class</code> to register
     * @throws IllegalArgumentException If <code>theClass</code> is an array
     */
    public static void registerClass(Class<?> theClass) {
        if (theClass.isArray()) {
            throw new IllegalArgumentException("theClass cannot be an array");
        }
        Object obj = globalClassNameClassMap.get(theClass.getName());
        if (obj == null) {
            globalClassNameClassMap.putIfAbsent(theClass.getName(), theClass);
        }
        String alias = theClass.getName();
        int pos = alias.lastIndexOf(".");
        if (pos != -1) {
            alias = alias.substring(pos + 1);
        }
        obj = globalClassNameClassMap.get(alias);
        if (obj == null) {
            globalClassNameClassMap.putIfAbsent(alias, theClass);
        }
        if (Debug.verboseOn()) {
            Debug.logVerbose("Registered class " + theClass.getName() + ", alias " + alias, module);
        }
    }

    public CachedClassLoader(URL[] url, ClassLoader parent, String contextName) {
        super(url, parent);
        this.contextName = contextName;
        if (Debug.verboseOn()) {
            Package[] paks = this.getPackages();
            StringBuilder pakList = new StringBuilder();
            for (int i = 0; i < paks.length; i++) {
                pakList.append(paks[i].getName());
                if (i < (paks.length - 1)) {
                    pakList.append(":");
                }
            }
            Debug.logVerbose("Cached ClassLoader Packages : " + pakList.toString(), module);
        }
    }

    public CachedClassLoader(ClassLoader parent, String contextName) {
        this(new URL[0], parent, contextName);
    }

    public CachedClassLoader(URL[] url, ClassLoader parent) {
        this(url, parent, "__globalContext");
    }

    @Override
    public String toString() {
        return "org.ofbiz.base.util.CachedClassLoader(" + contextName + ") / " + getParent().toString();
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return loadClass(name, false);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // check global common classes, ie for all instances
        Class<?> theClass = globalClassNameClassMap.get(name);
        if (theClass != null) {
            return theClass;
        }
        // check local classes, ie for this instance
        theClass = this.localClassNameClassMap.get(name);
        if (theClass != null) {
            return theClass;
        }
        // make sure it is not a known bad class name
        if (this.localBadClassNameSet.contains(name) || globalBadClassNameSet.contains(name)) {
            if (Debug.verboseOn()) {
                Debug.logVerbose("Cached loader got a known bad class name: [" + name + "]", module);
            }
            throw new ClassNotFoundException("Cached loader got a known bad class name: " + name);
        }
        if (Debug.verboseOn()) {
            Debug.logVerbose("Cached loader cache miss for class name: [" + name + "]", module);
        }
        try {
            theClass = super.loadClass(name, resolve);
            if (isGlobalPath(name)) {
                globalClassNameClassMap.putIfAbsent(name, theClass);
                theClass = globalClassNameClassMap.get(name);
            } else {
                this.localClassNameClassMap.putIfAbsent(name, theClass);
                theClass = this.localClassNameClassMap.get(name);
            }
        } catch (ClassNotFoundException e) {
            // Debug.logInfo(e, module);
            if (Debug.verboseOn()) {
                Debug.logVerbose("Remembering invalid class name: [" + name + "]", module);
            }
            if (isGlobalPath(name)) {
                globalBadClassNameSet.add(name);
            } else {
                this.localBadClassNameSet.add(name);
            }
            throw e;
        }
        return theClass;
    }

    @Override
    public URL getResource(String name) {
        // check global common resources, ie for all instances
        URL theResource = globalResourceMap.get(name);
        if (theResource != null) {
            return theResource;
        }
        // check local resources, ie for this instance
        theResource = this.localResourceMap.get(name);
        if (theResource != null) {
            return theResource;
        }
        // make sure it is not a known bad resource name
        if (localBadResourceNameSet.contains(name) || globalBadResourceNameSet.contains(name)) {
            if (Debug.verboseOn()) {
                Debug.logVerbose("Cached loader got a known bad resource name: [" + name + "]", module);
            }
            return null;
        }
        if (Debug.verboseOn()) {
            Debug.logVerbose("Cached loader cache miss for resource name: [" + name + "]", module);
        }
        // Debug.logInfo("Cached loader cache miss for resource name: [" + name + "]", module);
        theResource = super.getResource(name);
        if (theResource == null) {
            if (Debug.verboseOn())
                Debug.logVerbose("Remembering invalid resource name: [" + name + "]", module);
            // Debug.logInfo("Remembering invalid resource name: [" + name + "]", module);
            if (isGlobalPath(name)) {
                globalBadResourceNameSet.add(name);
            } else {
                this.localBadResourceNameSet.add(name);
            }
        } else {
            if (isGlobalPath(name)) {
                globalResourceMap.putIfAbsent(name, theResource);
                theResource = globalResourceMap.get(name);
            } else {
                this.localResourceMap.putIfAbsent(name, theResource);
                theResource = this.localResourceMap.get(name);
            }
        }
        return theResource;
    }

    protected boolean isGlobalPath(String name) {
        if (name.startsWith("java.") || name.startsWith("java/") || name.startsWith("/java/")) return true;
        if (name.startsWith("javax.") || name.startsWith("javax/") || name.startsWith("/javax/")) return true;
        if (name.startsWith("sun.") || name.startsWith("sun/") || name.startsWith("/sun/")) return true;
        if (name.startsWith("org.ofbiz.") || name.startsWith("org/ofbiz/")) return true;
        return false;
    }
}
