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

import org.apache.ofbiz.base.lang.ThreadSafe;
import org.apache.ofbiz.entity.GenericEntityConfException;
import org.w3c.dom.Element;

/**
 * An object that models the <code>&lt;resource-loader&gt;</code> element.
 *
 * @see <code>entity-config.xsd</code>
 */
@ThreadSafe
public final class ResourceLoader {

    private final String name; // type = xs:string
    private final String className; // type = xs:string
    private final String prependEnv; // type = xs:string
    private final String prefix; // type = xs:string

    ResourceLoader(Element element) throws GenericEntityConfException {
        String lineNumberText = EntityConfig.createConfigFileLineNumberText(element);
        String name = element.getAttribute("name").intern();
        if (name.isEmpty()) {
            throw new GenericEntityConfException("<resource-loader> element name attribute is empty" + lineNumberText);
        }
        this.name = name;
        String className = element.getAttribute("class").intern();
        if (className.isEmpty()) {
            throw new GenericEntityConfException("<resource-loader> element class attribute is empty" + lineNumberText);
        }
        this.className = className;
        this.prependEnv = element.getAttribute("prepend-env").intern();
        this.prefix = element.getAttribute("prefix").intern();
    }

    /** Returns the value of the <code>name</code> attribute. */
    public String getName() {
        return this.name;
    }

    /** Returns the value of the <code>class</code> attribute. */
    public String getClassName() {
        return this.className;
    }

    /** Returns the value of the <code>prepend-env</code> attribute. */
    public String getPrependEnv() {
        return this.prependEnv;
    }

    /** Returns the value of the <code>prefix</code> attribute. */
    public String getPrefix() {
        return this.prefix;
    }
}
