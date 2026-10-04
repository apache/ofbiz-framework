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
 * An object that models the <code>&lt;group-map&gt;</code> element.
 *
 * @see <code>entity-config.xsd</code>
 */
@ThreadSafe
public final class GroupMap extends AbstractConfigElement {

    private final EntityConfigGetter config = EntityConfigGetter.getInstance();
    public static final String ELEMENT_NAME = "group-map";
    public static final String ELEMENT_FIELD_ID_NAME = "group-name";

    private final String xPath;
    private final String groupName;
    private final String datasourceName;

    GroupMap(Element element, String xPathParent) throws GenericEntityConfException {
        String lineNumberText = EntityConfig.createConfigFileLineNumberText(element);

        String groupName = element.getAttribute("group-name").intern();
        this.xPath = xPathParent.concat("/group-map[@group-name='" + groupName + "']");
        this.groupName = groupName;
        if (groupName.isEmpty()) {
            throw new GenericEntityConfException("<group-map> element group-name attribute is empty" + lineNumberText);
        }
        String datasourceName = config.getValue(this.xPath + "/@datasource-name");
        if (datasourceName.isEmpty()) {
            throw new GenericEntityConfException("<group-map> element datasource-name attribute is empty" + lineNumberText);
        }
        this.datasourceName = datasourceName;
    }

    GroupMap(Map<String, Object> configObject, String xPath) throws GenericEntityConfException {
        this.xPath = xPath;
        String groupName = config.getValue(configObject, "/@group-name");
        if (groupName.isEmpty()) {
            throw new GenericEntityConfException("<group-map> element group-name attribute is empty");
        }
        this.groupName = groupName;
        String datasourceName = config.getValue(configObject, "/@datasource-name");
        if (datasourceName.isEmpty()) {
            throw new GenericEntityConfException("<group-map> element datasource-name attribute is empty");
        }
        this.datasourceName = datasourceName;
    }

    public static GroupMap loadFromXml(Element element, String xPathParent) throws GenericEntityConfException {
        return new GroupMap(element, xPathParent);
    }

    public static GroupMap loadFromConfig(Map<String, Object> configMap, String xPath) throws GenericEntityConfException {
        return new GroupMap(configMap, xPath);
    }

    public String getGroupName() {
        return groupName;
    }

    public String getDatasourceName() {
        return datasourceName;
    }

    @Override
    public String getName() {
        return groupName;
    }

}
