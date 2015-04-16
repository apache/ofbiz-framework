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
package org.ofbiz.base.util.cache;

import java.io.IOException;

import jdbm.RecordManager;
import jdbm.helper.ISerializationHandler;
import jdbm.helper.Serializer;
import jdbm.recman.BaseRecordManager;

/**
 * Customer JDBM Record Manager
 *
 */
public class JdbmRecordManager implements RecordManager {

    protected BaseRecordManager manager = null;
    protected JdbmSerializer serial = null;

    public JdbmRecordManager(String name) throws IOException {
        manager = new BaseRecordManager(name);
        serial = new JdbmSerializer();
    }

    public ISerializationHandler getSerializationHandler() {
        return serial;
    }

    public RecordManager getBaseRecordManager() {
        return manager;
    }

    public RecordManager getRecordManager() {
        return this;
    }

    public long insert(Object o) throws IOException {
        return insert(o, serial);
    }

    public void update(long l, Object o) throws IOException {
        update(l, o, serial);
    }

    public Object fetch(long l) throws IOException {
        return fetch(l, serial);
    }

    public void close() throws IOException {
        manager.close();
    }

    public void commit() throws IOException {
        manager.commit();
    }

    public void delete(long l) throws IOException {
        manager.delete(l);
    }

    public Object fetch(long l, Serializer s) throws IOException {
        return manager.fetch(l, s);
    }

    public long getNamedObject(String name) throws IOException {
        return manager.getNamedObject(name);
    }

    public long getRoot(int i) throws IOException {
        return manager.getRoot(i);
    }

    public int getRootCount() {
        return manager.getRootCount();
    }

    public long insert(Object o, Serializer s) throws IOException {
        return manager.insert(o, s);
    }

    public void rollback() throws IOException {
        manager.rollback();
    }

    public void setNamedObject(String s, long l) throws IOException {
        manager.setNamedObject(s, l);
    }

    public void setRoot(int i, long l) throws IOException {
        manager.setRoot(i, l);
    }

    public void update(long l, Object o, Serializer s) throws IOException {
        manager.update(l, o, s);
    }
}
