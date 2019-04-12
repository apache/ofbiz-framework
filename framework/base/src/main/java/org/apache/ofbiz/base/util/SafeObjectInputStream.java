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
package org.apache.ofbiz.base.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.regex.Pattern;

/**
 * ObjectInputStream
 *
 */
public class SafeObjectInputStream extends java.io.ObjectInputStream {

    private ClassLoader classloader;
    private Pattern WHITELIST_PATTERN = null;

    public SafeObjectInputStream(InputStream in, ClassLoader loader) throws IOException {
        super(in);
        this.classloader = loader;
    }

    public SafeObjectInputStream(InputStream in, ClassLoader loader, List<String> whitelist) throws IOException {
        super(in);
        this.classloader = loader;
        StringBuilder bld = new StringBuilder("(");
        for (int i = 0; i < whitelist.size(); i++) {
            bld.append(whitelist.get(i));
            if (i != whitelist.size() - 1) {
                bld.append("|");
            }
        }
        bld.append(")");
        WHITELIST_PATTERN = Pattern.compile(bld.toString());
    }


    /**
     * @see java.io.ObjectInputStream#resolveClass(java.io.ObjectStreamClass)
     */
    @Override
    protected Class<?> resolveClass(ObjectStreamClass classDesc) throws IOException, ClassNotFoundException {
        if (!WHITELIST_PATTERN.matcher(classDesc.getName()).find()) {
            Debug.logWarning("***Incompatible class***: " + classDesc.getName() + 
                    ". Please see OFBIZ-10837.  Report to dev ML if you use OFBiz without changes. "
                    + "Else add your class into UtilObject::getObjectException", "SafeObjectInputStream");
            throw new ClassCastException("Incompatible class: " + classDesc.getName());
        }
        
        return ObjectType.loadClass(classDesc.getName(), classloader);
    }

    /**
     * @see java.io.ObjectInputStream#resolveProxyClass(java.lang.String[])
     */
    @Override
    protected Class<?> resolveProxyClass(String[] interfaces) throws IOException, ClassNotFoundException {
        Class<?>[] cinterfaces = new Class<?>[interfaces.length];
        for (int i = 0; i < interfaces.length; i++) {
            cinterfaces[i] = classloader.loadClass(interfaces[i]);
        }
        //Proxy.getInvocationHandler(proxy)
        
        try {
            return Proxy.getProxyClass(classloader, cinterfaces);
        } catch (IllegalArgumentException e) {
            throw new ClassNotFoundException(null, e);
        }

    }
}
