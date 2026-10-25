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
 * An object that models the <code>&lt;field-type&gt;</code> element.
 *
 * @see <code>entity-config.xsd</code>
 */
@ThreadSafe
public final class FieldType extends AbstractConfigElement {

    private final EntityConfigGetter config = EntityConfigGetter.getInstance();
    public static final String ELEMENT_NAME = "field-type";
    private final String xPath;

    private final String name;
    private final String loader;
    private final String location;

    FieldType(Element element, String xPathParent) throws GenericEntityConfException {
        String lineNumberText = EntityConfig.createConfigFileLineNumberText(element);
        String name = element.getAttribute("name");
        if (name.isEmpty()) {
            throw new GenericEntityConfException("<field-type> element name attribute is empty" + lineNumberText);
        }
        this.name = name;
        xPath = xPathParent.concat("/field-type[@name='" + name + "']");
        String loader = config.getValue(this.xPath + "/@loader");
        if (loader.isEmpty()) {
            throw new GenericEntityConfException("<field-type> element loader attribute is empty" + lineNumberText);
        }
        this.loader = loader;
        String location = config.getValue(this.xPath + "/@location");
        if (location.isEmpty()) {
            throw new GenericEntityConfException("<field-type> element location attribute is empty" + lineNumberText);
        }
        this.location = location;
    }

    FieldType(Map<String, Object> configObject, String xPath) throws GenericEntityConfException {
        this.xPath = xPath;
        String name = getNameFromXPath(xPath);
        if (name.isEmpty()) {
            throw new GenericEntityConfException("<field-type> element name attribute is empty");
        }
        this.name = name;
        String loader = config.getValue(configObject, "/@loader");
        if (loader.isEmpty()) {
            throw new GenericEntityConfException("<field-type> element loader attribute is empty ");
        }
        this.loader = loader;
        String location = config.getValue(configObject, "/@location");
        if (location.isEmpty()) {
            throw new GenericEntityConfException("<field-type> element location attribute is empty");
        }
        this.location = location;
    }

    public static FieldType loadFromXml(Element element, String xPathParent)
            throws GenericEntityConfException {
        return new FieldType(element, xPathParent);
    }

    public static FieldType loadFromConfig(Map<String, Object> configMap, String xPath)
            throws GenericEntityConfException {
        return new FieldType(configMap, xPath);
    }

    public String getLoader() {
        return loader;
    }

    public String getLocation() {
        return location;
    }

    @Override
    public String getName() {
        return name;
    }

}
