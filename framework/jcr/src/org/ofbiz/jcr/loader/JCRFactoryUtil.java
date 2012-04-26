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
package org.ofbiz.jcr.loader;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.ofbiz.base.util.Debug;
import org.ofbiz.security.SecurityFactory;

public class JCRFactoryUtil {

    public static final String module = JCRFactoryUtil.class.getName();

    private static JCRFactory jcrFactory;
    private static String jcrFactoryName;

    /**
     *
     * @return
     */
    public static JCRFactory getJCRFactory() {
        if (jcrFactory == null) {

            synchronized (SecurityFactory.class) {
                if (jcrFactory == null) {

                    ClassLoader loader = Thread.currentThread().getContextClassLoader();
                    Class<?> c;
                    try {
                        c = loader.loadClass(jcrFactoryName);
                        jcrFactory = (JCRFactory) c.newInstance();
                    } catch (ClassNotFoundException e) {
                        Debug.logError(e, "Cannot get instance of the jcr implementation", module);
                    } catch (InstantiationException e) {
                        Debug.logError(e, "Cannot get instance of the jcr implementation", module);
                    } catch (IllegalAccessException e) {
                        Debug.logError(e, "Cannot get instance of the jcr implementation", module);
                    }

                }
            }
        }

        return jcrFactory;
    }

    public static Session getSession() {
        Session session = null;
        try {
            session = getJCRFactory().createSession();
        } catch (RepositoryException e) {
            Debug.logError(e, module);
        }

        return session;
    }

    public static void setJcrFactoryClassName(String jcrFactoryClassName) {
        jcrFactoryName = jcrFactoryClassName;
    }
}
