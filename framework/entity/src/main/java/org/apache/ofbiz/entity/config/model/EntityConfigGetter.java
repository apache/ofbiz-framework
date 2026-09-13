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
package org.apache.ofbiz.entity.config.model;

import org.apache.ofbiz.base.config.AbstractXmlConfigGetter;
import org.apache.ofbiz.base.lang.ThreadSafe;

/**
 * A singleton class that models the <code>&lt;entity-config&gt;</code> element.
 *
 * @see <code>entity-config.xsd</code>
 */
@ThreadSafe
public final class EntityConfigGetter extends AbstractXmlConfigGetter {
    public static final String ENTITY_ENGINE_XML_FILENAME = "entityengine.xml";
    private static final EntityConfigGetter INSTANCE = initInstance();


    public EntityConfigGetter() {
        super(ENTITY_ENGINE_XML_FILENAME, "/entity-config");
    }

    public static EntityConfigGetter getInstance() {
        return INSTANCE;
    }

    public static EntityConfigGetter initInstance() {
        synchronized (EntityConfigGetter.class) {
            return new EntityConfigGetter();
        }
    }

}
