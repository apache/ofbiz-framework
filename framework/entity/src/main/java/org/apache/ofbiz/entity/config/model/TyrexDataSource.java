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

import java.util.Map;

/**
 * An object that models the <code>&lt;tyrex-dataSource&gt;</code> element.
 *
 * @see <code>entity-config.xsd</code>
 */
@ThreadSafe
public final class TyrexDataSource extends JdbcElement {

    private final EntityConfigGetter config = EntityConfigGetter.getInstance();
    public static final String ELEMENT_NAME = "tyrex-dataSource";

    private final String dataSourceName;

    TyrexDataSource(Element element, String xPathParent) throws GenericEntityConfException {
        super(element, xPathParent.concat("tyrex-dataSource"));
        String lineNumberText = EntityConfig.createConfigFileLineNumberText(element);
        String dataSourceName = config.getValue(getXPath() + "dataSource-name");
        if (dataSourceName.isEmpty()) {
            throw new GenericEntityConfException("<tyrex-dataSource> element dataSource-name attribute is empty" + lineNumberText);
        }
        this.dataSourceName = dataSourceName;
    }

    TyrexDataSource(Map<String, Object> configObject, String xPath) throws GenericEntityConfException {
        super(configObject, xPath);
        String dataSourceName = config.getValue(configObject, "dataSource-name");
        if (dataSourceName.isEmpty()) {
            throw new GenericEntityConfException("<tyrex-dataSource> element dataSource-name attribute is empty");
        }
        this.dataSourceName = dataSourceName;
    }

    public static TyrexDataSource loadFromXml(Element element, String xPathParent) throws GenericEntityConfException {
        return new TyrexDataSource(element, xPathParent);
    }

    public static TyrexDataSource loadFromConfig(Map<String, Object> configMap, String xPath) throws GenericEntityConfException {
        return new TyrexDataSource(configMap, xPath);
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    @Override
    public String getName() {
        return "tyrex-dataSource";
    }
}
