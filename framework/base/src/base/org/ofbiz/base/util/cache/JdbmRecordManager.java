/*
 * Copyright 2001-2006 The Apache Software Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.ofbiz.base.util.cache;

import java.io.IOException;

/**
 * Customer JDBM Record Manager
 * 
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      3.3
 */
public class JdbmRecordManager extends jdbm.recman.BaseRecordManager {

    protected jdbm.helper.Serializer serial = null;

    public JdbmRecordManager(String name) throws IOException {
        super(name);
        serial = new JdbmSerializer();
    }

    public long insert(Object o) throws IOException {
        return this.insert(o, serial);
    }

    public void update(long l, Object o) throws IOException {
        this.update(l, o, serial);
    }

    public Object fetch(long l) throws IOException {
        return this.fetch(l, serial);
    }
}
