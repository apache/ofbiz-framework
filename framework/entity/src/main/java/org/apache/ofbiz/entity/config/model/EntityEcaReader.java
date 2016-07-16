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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.ofbiz.base.lang.ThreadSafe;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.entity.GenericEntityConfException;
import org.w3c.dom.Element;

/**
 * An object that models the <code>&lt;entity-eca-reader&gt;</code> element.
 *
 * @see <code>entity-config.xsd</code>
 */
@ThreadSafe
public final class EntityEcaReader {

    private final String name; // type = xs:string
    private final List<Resource> resourceList; // <resource>

    EntityEcaReader(Element element) throws GenericEntityConfException {
        String lineNumberText = EntityConfig.createConfigFileLineNumberText(element);
        String name = element.getAttribute("name").intern();
        if (name.isEmpty()) {
            throw new GenericEntityConfException("<entity-eca-reader> element name attribute is empty" + lineNumberText);
        }
        this.name = name;
        List<? extends Element> resourceElementList = UtilXml.childElementList(element, "resource");
        if (resourceElementList.isEmpty()) {
            this.resourceList = Collections.emptyList();
        } else {
            List<Resource> resourceList = new ArrayList<Resource>(resourceElementList.size());
            for (Element resourceElement : resourceElementList) {
                resourceList.add(new Resource(resourceElement));
            }
            this.resourceList = Collections.unmodifiableList(resourceList);
        }
    }

    /** Returns the value of the <code>name</code> attribute. */
    public String getName() {
        return this.name;
    }

    /** Returns the <code>&lt;resource&gt;</code> child elements. */
    public List<Resource> getResourceList() {
        return this.resourceList;
    }
}
