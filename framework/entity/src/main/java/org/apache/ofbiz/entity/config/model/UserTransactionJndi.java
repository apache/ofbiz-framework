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

import org.apache.ofbiz.base.config.AbstractConfigElement;
import org.apache.ofbiz.base.lang.ThreadSafe;
import org.apache.ofbiz.entity.GenericEntityConfException;
import org.w3c.dom.Element;

import java.util.Map;

/**
 * An object that models the <code>&lt;user-transaction-jndi&gt;</code> element.
 *
 * @see <code>entity-config.xsd</code>
 */
@ThreadSafe
public final class UserTransactionJndi extends AbstractConfigElement {

    private final EntityConfigGetter config = EntityConfigGetter.getInstance();
    public static final String ELEMENT_NAME = "user-transaction-jndi";
    private final String xPath;

    private final String jndiServerName;
    private final String jndiName;

    UserTransactionJndi(Element element, String xPathParent) throws GenericEntityConfException {
        String lineNumberText = EntityConfig.createConfigFileLineNumberText(element);
        xPath = xPathParent.concat("/user-transaction-jndi");
        String jndiServerName = config.getValue(xPath + "/@jndi-server-name");
        if (jndiServerName.isEmpty()) {
            throw new GenericEntityConfException("<user-transaction-jndi> element jndi-server-name attribute is empty" + lineNumberText);
        }
        this.jndiServerName = jndiServerName;
        String jndiName = config.getValue("/@jndi-name");
        if (jndiName.isEmpty()) {
            throw new GenericEntityConfException("<user-transaction-jndi> element jndi-name attribute is empty" + lineNumberText);
        }
        this.jndiName = jndiName;
    }

    public static TransactionManagerJndi loadFromXml(Element element, String xPathParent) throws GenericEntityConfException {
        return new TransactionManagerJndi(element, xPathParent);
    }

    public static TransactionManagerJndi loadFromConfig(Map<String, Object> configMap, String xPath) throws GenericEntityConfException {
        return null;
    }

    public String getJndiServerName() {
        return jndiServerName;
    }

    public String getJndiName() {
        return jndiName;
    }

    @Override
    public String getName() {
        return "user-transaction-jndi";
    }
}
