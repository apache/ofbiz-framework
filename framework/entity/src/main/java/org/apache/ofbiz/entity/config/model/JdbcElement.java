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
import org.w3c.dom.Element;

import java.util.Map;

/**
 * An abstract class for <code>&lt;datasource&gt;</code> JDBC child elements.
 *
 * @see <code>entity-config.xsd</code>
 */
public abstract class JdbcElement extends AbstractConfigElement {

    private final EntityConfigGetter config = EntityConfigGetter.getInstance();
    private final String xPath;

    private final String isolationLevel;
    private final String lineNumber;


    protected JdbcElement(Element element, String xPathParent) {
        xPath = xPathParent;
        isolationLevel = config.getValue(xPath + "/@isolation-level");
        Object lineNumber = element.getUserData("startLine");
        this.lineNumber = lineNumber == null ? "unknown" : lineNumber.toString();
    }

    protected JdbcElement(Map<String, Object> configObject, String xPath) {
        this.xPath = xPath;
        isolationLevel = config.getValue(configObject, "/@isolation-level");
        lineNumber = "unknown";
    }

    /** Returns the value of the <code>isolation-level</code> attribute. */
    public String getIsolationLevel() {
        return isolationLevel;
    }

    /**
     * @return The configuration file line number for this element
     */
    public String getLineNumber() {
        return lineNumber;
    }

    /**
     * @return The current xpath of this element
     */
    public String getXPath() {
        return xPath;
    }
}
