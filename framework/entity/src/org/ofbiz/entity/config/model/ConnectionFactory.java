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
package org.ofbiz.entity.config.model;

import org.ofbiz.base.lang.ThreadSafe;
import org.ofbiz.entity.GenericEntityConfException;
import org.ofbiz.entity.config.EntityConfigUtil;
import org.w3c.dom.Element;

/**
 * An object that models the <code>&lt;connection-factory&gt;</code> element.
 *
 * @see <code>entity-config.xsd</code>
 */
@ThreadSafe
public final class ConnectionFactory {

    private final String className; // type = xs:string

    ConnectionFactory(Element element) throws GenericEntityConfException {
        String lineNumberText = EntityConfigUtil.createConfigFileLineNumberText(element);
        String className = element.getAttribute("class").intern();
        if (className.isEmpty()) {
            throw new GenericEntityConfException("<connection-factory> element class attribute is empty" + lineNumberText);
        }
        this.className = className;
    }

    /** Returns the value of the <code>class</code> attribute. */
    public String getClassName() {
        return this.className;
    }
}
