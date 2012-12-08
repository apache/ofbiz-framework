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
package org.ofbiz.catalina.container;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import org.apache.catalina.Container;
import org.apache.catalina.Loader;
import org.apache.catalina.Session;
import org.apache.catalina.session.StandardSession;
import org.apache.catalina.session.StoreBase;
import org.apache.catalina.util.CustomObjectInputStream;
import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;

public class OfbizStore extends StoreBase {

    public static final String module = OfbizStore.class.getName();
    public static final String entityName = "CatalinaSession";

    protected static String storeName = "OfbizStore";

    protected Delegator delegator = null;

    public OfbizStore(Delegator delegator) {
        this.delegator = delegator;
    }

    @Override
    public String getStoreName() {
        return storeName;
    }

    public int getSize() throws IOException {
        long count = 0;
        try {
            count = delegator.findCountByCondition(entityName, null, null, null);
        } catch (GenericEntityException e) {
            throw new IOException(e.getMessage());
        }

        return (int) count;
    }

    public String[] keys() throws IOException {
        List<GenericValue> sessions = null;
        try {
            sessions = delegator.findList(entityName, null, null, null, null, false);
        } catch (GenericEntityException e) {
            throw new IOException(e.getMessage());
        }

        if (sessions == null) {
            return new String[0];
        } else {
            String[] ids = new String[sessions.size()];
            int loc = 0;
            for (GenericValue value: sessions) {
                ids[loc++] = value.getString("sessionId");
            }

            return ids;
        }
    }

    public Session load(String id) throws ClassNotFoundException, IOException {
        StandardSession _session = null;
        GenericValue sessionValue = null;
        try {
            sessionValue = delegator.findOne(entityName, false, "sessionId", id);
        } catch (GenericEntityException e) {
            throw new IOException(e.getMessage());
        }

        if (sessionValue != null) {
            byte[] bytes = sessionValue.getBytes("sessionInfo");
            if (bytes != null) {
                BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(bytes));

                Container container = manager.getContainer();
                ClassLoader classLoader = null;
                Loader loader = null;

                if (container != null) {
                    loader = container.getLoader();
                }
                if (loader != null) {
                    classLoader = loader.getClassLoader();
                }

                ObjectInputStream ois = null;
                if (classLoader != null) {
                    ois = new CustomObjectInputStream(bis, classLoader);
                } else {
                    ois = new ObjectInputStream(bis);
                }

                //Debug.logInfo("Loading Session Store [" + id + "]", module);
                _session = (StandardSession) manager.createEmptySession();
                _session.readObjectData(ois);
                _session.setManager(manager);
            }
        }

        return _session;
    }

    public void remove(String id) throws IOException {
        try {
            delegator.removeByAnd(entityName, "sessionId", id);
        } catch (GenericEntityException e) {
            throw new IOException(e.getMessage());
        }
    }

    public void clear() throws IOException {
        try {
            delegator.removeAll(entityName);
        } catch (GenericEntityException e) {
            throw new IOException(e.getMessage());
        }
    }

    public void save(Session session) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(bos));

        ((StandardSession) session).writeObjectData(oos);
        oos.close();
        oos = null;

        byte[] obs = bos.toByteArray();
        int size = obs.length;

        GenericValue sessionValue = delegator.makeValue(entityName);
        sessionValue.setBytes("sessionInfo", obs);
        sessionValue.set("sessionId", session.getId());
        sessionValue.set("sessionSize", size);
        sessionValue.set("isValid", session.isValid() ? "Y" : "N");
        sessionValue.set("maxIdle", session.getMaxInactiveInterval());
        sessionValue.set("lastAccessed", session.getLastAccessedTime());

        try {
            delegator.createOrStore(sessionValue);
        } catch (GenericEntityException e) {
            throw new IOException(e.getMessage());
        }

        Debug.logInfo("Persisted session [" + session.getId() + "]", module);
    }
}
