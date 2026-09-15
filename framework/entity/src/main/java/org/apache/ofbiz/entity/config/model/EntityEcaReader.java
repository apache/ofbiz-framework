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
 * An object that models the <code>&lt;entity-eca-reader&gt;</code> element.
 *
 * @see <code>entity-config.xsd</code>
 */
@ThreadSafe
public final class EntityEcaReader extends AbstractConfigElement {

    private final EntityConfigGetter config = EntityConfigGetter.getInstance();
    public static final String ELEMENT_NAME = "entity-eca-reader";
    private final String xPath;

    private final String name;
    private final List<Resource> resourceList;

    EntityEcaReader(Element element, String xPathParent) throws GenericEntityConfException {
        String lineNumberText = EntityConfig.createConfigFileLineNumberText(element);
        String name = element.getAttribute("name").intern();
        if (name.isEmpty()) {
            throw new GenericEntityConfException("<entity-eca-reader> element name attribute is empty" + lineNumberText);
        }
        this.name = name;
        xPath = xPathParent.concat("/entity-eca-reader[@name='" + name + "']");
        List<Resource> resourceList = config.getSubElementsAsListEntries(xPath.concat("/resource"), element, Resource.class);
        this.resourceList = resourceList.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(resourceList);
    }

    EntityEcaReader(Map<String, Object> configObject, String xPath) throws GenericEntityConfException {
        this.xPath = xPath;
        String name = getNameFromXPath(xPath);
        if (name.isEmpty()) {
            throw new GenericEntityConfException("<entity-eca-reader> element name attribute is empty");
        }
        this.name = name;
        List<Resource> resourceList = config.getSubElementsAsListEntries(xPath.concat("/resource"), null, Resource.class);
        if (resourceList.isEmpty()) {
            this.resourceList = Collections.emptyList();
        } else {
            this.resourceList = Collections.unmodifiableList(resourceList);
        }
    }

    public static EntityEcaReader loadFromXml(Element element, String xPathParent) throws GenericEntityConfException {
        return new EntityEcaReader(element, xPathParent);
    }

    public static EntityEcaReader loadFromConfig(Map<String, Object> configMap, String xPath) throws GenericEntityConfException {
        return new EntityEcaReader(configMap, xPath);
    }

    public List<Resource> getResourceList() {
        return resourceList;
    }

    @Override
    public String getName() {
        return name;
    }

}
