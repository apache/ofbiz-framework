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
 * An object that models the <code>&lt;sql-load-path&gt;</code> element.
 *
 * @see <code>entity-config.xsd</code>
 */
@ThreadSafe
public final class SqlLoadPath extends AbstractConfigElement {

    private final EntityConfigGetter config = EntityConfigGetter.getInstance();
    public static final String ELEMENT_NAME = "sql-load-path";
    private final String xPath;

    private final String path;
    private final String prependEnv;

    SqlLoadPath(Element element, String xPathParent) throws GenericEntityConfException {
        String lineNumberText = EntityConfig.createConfigFileLineNumberText(element);
        String path = element.getAttribute("path").intern();
        xPath = xPathParent.concat("/sql-load-path[@path='" + path + "']");
        if (path.isEmpty()) {
            throw new GenericEntityConfException("<sql-load-path> element path attribute is empty" + lineNumberText);
        }
        this.path = path;
        prependEnv = config.getValue(xPath + "/@prepend-env");
    }

    SqlLoadPath(Map<String, Object> configObject, String xPath) throws GenericEntityConfException {
        this.xPath = xPath;
        String path = config.getValue(configObject, "path");
        if (path.isEmpty()) {
            throw new GenericEntityConfException("<sql-load-path> element path attribute is empty");
        }
        this.path = path;
        prependEnv = config.getValue(configObject, "prepend-env");
    }

    public static SqlLoadPath loadFromXml(Element element, String xPathParent) throws GenericEntityConfException {
        return new SqlLoadPath(element, xPathParent);
    }

    public static SqlLoadPath loadFromConfig(Map<String, Object> configMap, String xPath) throws GenericEntityConfException {
        return new SqlLoadPath(configMap, xPath);
    }

    public String getPath() {
        return path;
    }

    public String getPrependEnv() {
        return prependEnv;
    }

    @Override
    public String getName() {
        return path;
    }

}
