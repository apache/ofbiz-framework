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

import java.util.Map;

import org.apache.ofbiz.base.config.AbstractConfigElement;
import org.apache.ofbiz.base.lang.ThreadSafe;
import org.apache.ofbiz.entity.GenericEntityConfException;
import org.w3c.dom.Element;

/**
 * An object that models the <code>&lt;transaction-factory&gt;</code> element.
 *
 * @see <code>entity-config.xsd</code>
 */
@ThreadSafe
public final class TransactionFactory extends AbstractConfigElement {

    private final EntityConfigGetter config = EntityConfigGetter.getInstance();
    public static final String ELEMENT_NAME = "transaction-factory";
    private final String xPath;

    private final String className;
    private final UserTransactionJndi userTransactionJndi;
    private final TransactionManagerJndi transactionManagerJndi;

    TransactionFactory(Element element, String xPathParent) throws GenericEntityConfException {
        String lineNumberText = EntityConfig.createConfigFileLineNumberText(element);
        xPath = xPathParent.concat("/transaction-factory");
        String className = config.getValue(xPath + "/@class");
        if (className.isEmpty()) {
            throw new GenericEntityConfException("<transaction-factory> element class attribute is empty" + lineNumberText);
        }
        this.className = className;
        this.userTransactionJndi = config.getObjectSubElement(xPath.concat("/user-transaction-jndi"),
                element, UserTransactionJndi.class);
        this.transactionManagerJndi = config.getObjectSubElement(xPath.concat("/transaction-manager-jndi"),
                element, TransactionManagerJndi.class);
    }

    public static TransactionFactory loadFromXml(Element element, String xPathParent)
            throws GenericEntityConfException {
        return new TransactionFactory(element, xPathParent);
    }

    public static TransactionFactory loadFromConfig(Map<String, Object> configMap, String xPath)
            throws GenericEntityConfException {
        return null;
    }

    public String getClassName() {
        return className;
    }

    public UserTransactionJndi getUserTransactionJndi() {
        return userTransactionJndi;
    }

    public TransactionManagerJndi getTransactionManagerJndi() {
        return transactionManagerJndi;
    }


    @Override
    public String getName() {
        return "transaction-factory";
    }
}
