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

import org.apache.ofbiz.entity.GenericEntityConfException;
import org.w3c.dom.Element;

/**
 * An abstract class for <code>&lt;datasource&gt;</code> JDBC child elements.
 *
 * @see <code>entity-config.xsd</code>
 */
public abstract class JdbcElement {

    private final String isolationLevel;
    private final String lineNumber;

    protected JdbcElement(Element element) throws GenericEntityConfException {
        this.isolationLevel = element.getAttribute("isolation-level").intern();
        Object lineNumber = element.getUserData("startLine");
        this.lineNumber = lineNumber == null ? "unknown" : lineNumber.toString();
    }

    /** Returns the value of the <code>isolation-level</code> attribute. */
    public String getIsolationLevel() {
        return this.isolationLevel;
    }

    /**
     * Returns the configuration file line number for this element.
     * @return The configuration file line number for this element
     */
    public String getLineNumber() {
        return this.lineNumber;
    }
}
