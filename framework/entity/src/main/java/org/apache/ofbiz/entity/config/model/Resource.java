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
 * An object that models the <code>&lt;resource&gt;</code> element.
 *
 * @see <code>entity-config.xsd</code>
 */
@ThreadSafe
public final class Resource extends AbstractConfigElement {

    private final EntityConfigGetter config = EntityConfigGetter.getInstance();
    public static final String ELEMENT_NAME = "resource";
    private final String xPath;

    private final String loader;
    private final String location;

    Resource(Element element, String xPathParent) throws GenericEntityConfException {
        String lineNumberText = EntityConfig.createConfigFileLineNumberText(element);
        String location = element.getAttribute("location");
        xPath = xPathParent.concat("/resource[@location='" + location + "']");
        String loader = config.getValue(xPath.concat("/@loader"));
        if (loader.isEmpty()) {
            throw new GenericEntityConfException("<resource> element loader attribute is empty" + lineNumberText);
        }
        this.loader = loader;
        if (location.isEmpty()) {
            throw new GenericEntityConfException("<resource> element location attribute is empty" + lineNumberText);
        }
        this.location = location;
    }

    Resource(Map<String, Object> configObject, String xPath) throws GenericEntityConfException {
        this.xPath = xPath;
        String loader = config.getValue(configObject, "loader");
        if (loader.isEmpty()) {
            throw new GenericEntityConfException("<resource> element loader attribute is empty");
        }
        this.loader = loader;
        String location = config.getValue(configObject, "location");
        if (location.isEmpty()) {
            throw new GenericEntityConfException("<resource> element location attribute is empty");
        }
        this.location = location;
    }

    public String getLoader() {
        return loader;
    }

    public String getLocation() {
        return location;
    }

    public static Resource loadFromXml(Element element, String xPathParent) throws GenericEntityConfException {
        return new Resource(element, xPathParent);
    }

    public static Resource loadFromConfig(Map<String, Object> configMap, String xPath) throws GenericEntityConfException {
        return new Resource(configMap, xPath);
    }

    @Override
    public String getName() {
        return location;
    }
}
