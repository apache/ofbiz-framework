/*
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
 */
package org.apache.ofbiz.entity.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.apache.ofbiz.base.util.Assert;
import org.apache.ofbiz.base.util.cache.UtilCache;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;

/**
 * A class loader that retrieves Java resources from the
 * <b>JavaResource</b> entity. The entity is searched for the
 * resource(s), and if it is not found, searching is delegated
 * to the parent class loader.
 *
 */
public final class EntityClassLoader extends ClassLoader {

    private static final ThreadLocal<Boolean> inFind = new ThreadLocal<Boolean>(); // Guards against infinite recursion
    private static final URLStreamHandler streamHandler = new EntityURLStreamHandler();
    private static final UtilCache<String, String> misses = UtilCache.createUtilCache("entity.classloader.misses", 500, 0, true);

    public static ClassLoader getInstance(String delegatorName, ClassLoader parent) {
        Assert.notNull("delegatorName", delegatorName, "parent", parent);
        if (parent instanceof EntityClassLoader) {
            EntityClassLoader ecl = (EntityClassLoader) parent;
            if (delegatorName.equals(ecl.delegatorName)) {
                return ecl;
            }
            return new EntityClassLoader(delegatorName, ecl.getParent());
        }
        return new EntityClassLoader(delegatorName, parent);
    }

    private final String delegatorName;

    private EntityClassLoader(String delegatorName, ClassLoader parent) {
        super(parent);
        this.delegatorName = delegatorName;
    }

    @Override
    protected URL findResource(String name) {
        URL url = null;
        if (!isInFind()) {
            String key = delegatorName.concat(":").concat(name);
            if (misses.containsKey(key)) {
                return null;
            }
            try {
                inFind.set(Boolean.TRUE);
                Delegator delegator = DelegatorFactory.getDelegator(delegatorName);
                GenericValue resourceValue = delegator.findOne("JavaResource", true, "resourceName", name);
                if (resourceValue != null) {
                    url = newUrl(resourceValue);
                } else {
                    misses.put(key, key);
                }
            } catch (Exception e) {
                throw new EntityClassLoaderException(e);
            } finally {
                inFind.set(Boolean.FALSE);
            }
        }
        return url;
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        Enumeration<URL> urlEnum = null;
        if (!isInFind()) {
            String key = delegatorName.concat(":").concat(name);
            if (misses.containsKey(key)) {
                return null;
            }
            try {
                inFind.set(Boolean.TRUE);
                Delegator delegator = DelegatorFactory.getDelegator(delegatorName);
                EntityCondition condition = EntityCondition.makeCondition("resourceName", EntityOperator.LIKE, name);
                List<GenericValue> resourceValues = delegator.findList("JavaResource", condition, null, null, null, true);
                if (!resourceValues.isEmpty()) {
                    List<URL> urls = new ArrayList<URL>(resourceValues.size());
                    for (GenericValue resourceValue : resourceValues) {
                        urls.add(newUrl(resourceValue));
                    }
                    urlEnum = Collections.enumeration(urls);
                } else {
                    misses.put(key, key);
                }
            } catch (Exception e) {
                throw new EntityClassLoaderException(e);
            } finally {
                inFind.set(Boolean.FALSE);
            }
        }
        return urlEnum;
    }

    public String getDelegatorName() {
        return delegatorName;
    }

    @Override
    public URL getResource(String name) {
        Assert.notEmpty("name", name);
        URL url = findResource(name);
        if (url == null) {
            url = getParent().getResource(name);
        }
        return url;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        Assert.notEmpty("name", name);
        Enumeration<URL> urlEnum = findResources(name);
        if (urlEnum == null) {
            urlEnum = getParent().getResources(name);
        }
        return urlEnum;
    }

    private boolean isInFind() {
        Boolean inFindValue = inFind.get();
        if (inFindValue == null) {
            inFindValue = Boolean.FALSE;
            inFind.set(inFindValue);
        }
        return inFindValue;
    }

    private URL newUrl(GenericValue resourceValue) throws MalformedURLException {
        return new URL("entity", resourceValue.getDelegator().getDelegatorName(), -1, "/".concat(resourceValue
                .getString("resourceName")), streamHandler);
    }

    private static class EntityURLStreamHandler extends URLStreamHandler {

        @Override
        protected URLConnection openConnection(URL url) throws IOException {
            Assert.notNull("url", url);
            try {
                Delegator delegator = DelegatorFactory.getDelegator(url.getHost());
                String resourceName = url.getFile();
                if (resourceName.startsWith("/")) {
                    resourceName = resourceName.substring(1);
                }
                GenericValue resourceValue = delegator.findOne("JavaResource", true, "resourceName", resourceName);
                return new EntityURLConnection(url, resourceValue.getBytes("resourceValue"));
            } catch (Exception e) {
                throw new EntityClassLoaderException(e);
            }
        }
    }

    private static class EntityURLConnection extends URLConnection {

        private final byte[] data;

        private EntityURLConnection(URL url, byte[] data) {
            super(url);
            this.data = data;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(data);
        }

        @Override
        public void connect() throws IOException {
        }
    }

    @SuppressWarnings("serial")
    public static class EntityClassLoaderException extends RuntimeException {
        public EntityClassLoaderException(Throwable cause) {
            super(cause);
        }
    }
}
