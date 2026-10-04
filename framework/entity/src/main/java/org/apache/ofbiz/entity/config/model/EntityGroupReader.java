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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.config.AbstractConfigElement;
import org.apache.ofbiz.base.lang.ThreadSafe;
import org.apache.ofbiz.entity.GenericEntityConfException;
import org.w3c.dom.Element;

/**
 * An object that models the <code>&lt;entity-group-reader&gt;</code> element.
 *
 * @see <code>entity-config.xsd</code>
 */
@ThreadSafe
public final class EntityGroupReader extends AbstractConfigElement {

    private final EntityConfigGetter config = EntityConfigGetter.getInstance();
    public static final String ELEMENT_NAME = "entity-group-reader";
    private final String xPath;

    private final String name;
    private final String loader;
    private final String location;
    private final List<Resource> resourceList;

    EntityGroupReader(Element element, String xPathParent) throws GenericEntityConfException {
        String lineNumberText = EntityConfig.createConfigFileLineNumberText(element);
        String name = element.getAttribute("name").intern();
        if (name.isEmpty()) {
            throw new GenericEntityConfException("<entity-group-reader> element name attribute is empty" + lineNumberText);
        }
        this.name = name;
        this.xPath = xPathParent.concat("/entity-group-reader[@name='" + name + "']");
        loader = config.getValue(xPath + "/@loader");
        location = config.getValue(xPath + "/@location");
        List<Resource> resourceList = config.getSubElementsAsListEntries(xPath.concat("/resource"), element, Resource.class);
        if (resourceList.isEmpty()) {
            this.resourceList = Collections.emptyList();
        } else {
            this.resourceList = Collections.unmodifiableList(resourceList);
        }
    }

    EntityGroupReader(Map<String, Object> configObject, String xPath) throws GenericEntityConfException {
        this.xPath = xPath;
        String name = getNameFromXPath(xPath);
        if (name.isEmpty()) {
            throw new GenericEntityConfException("<entity-group-reader> element name attribute is empty");
        }
        this.name = name;
        loader = config.getValue(configObject, "/@loader");
        location = config.getValue(configObject, "/@location");
        List<Resource> resourceList = config.getSubElementsAsListEntries(xPath.concat("/resource"), null, Resource.class);
        this.resourceList = resourceList.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(resourceList);
    }

    public static EntityGroupReader loadFromXml(Element element, String xPathParent)
            throws GenericEntityConfException {
        return new EntityGroupReader(element, xPathParent);
    }

    public static EntityGroupReader loadFromConfig(Map<String, Object> configMap, String xPath)
            throws GenericEntityConfException {
        return new EntityGroupReader(configMap, xPath);
    }

    public String getLoader() {
        return loader;
    }

    public String getLocation() {
        return location;
    }

    public List<Resource> getResourceList() {
        return resourceList;
    }

    @Override
    public String getName() {
        return name;
    }

}
