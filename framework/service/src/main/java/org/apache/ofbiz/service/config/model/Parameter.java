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
package org.apache.ofbiz.service.config.model;

import java.util.Map;

import org.apache.ofbiz.base.config.AbstractConfigElement;
import org.apache.ofbiz.base.lang.ThreadSafe;
import org.apache.ofbiz.entity.GenericEntityConfException;
import org.apache.ofbiz.service.config.ServiceConfigException;
import org.w3c.dom.Element;

/**
 * An object that models the <code>&lt;parameter&gt;</code> element.
 */
@ThreadSafe
public final class Parameter extends AbstractConfigElement {

    private final ServiceConfigGetter config = ServiceConfigGetter.getInstance();
    public static final String ELEMENT_NAME = "parameter";
    private final String xPath;

    private final String name;
    private final String value;

    Parameter(Element parameterElement, String xPathParent) throws ServiceConfigException {
        String name = parameterElement.getAttribute("name").intern();
        if (name.isEmpty()) {
            throw new ServiceConfigException("<parameter> element name attribute is empty");
        }
        this.name = name;
        xPath = xPathParent.concat("/engine[@name='" + name + "']");
        String value = config.getValue(xPath.concat("/@value"));
        if (value.isEmpty()) {
            throw new ServiceConfigException("<parameter> element value attribute is empty");
        }
        this.value = value;
    }

    Parameter(Map<String, Object> configObject, String xPath) throws ServiceConfigException {
        this.xPath = xPath;
        String name = getNameFromXPath(xPath);
        if (name.isEmpty()) {
            throw new ServiceConfigException("<parameter> element name attribute is empty");
        }
        this.name = name;
        String value = config.getValue(configObject, "/@value");
        if (value.isEmpty()) {
            throw new ServiceConfigException("<parameter> element value attribute is empty");
        }
        this.value = value;
    }

    public static Parameter loadFromXml(Element element, String xPathParent)
            throws GenericEntityConfException, ServiceConfigException {
        return new Parameter(element, xPathParent);
    }

    public static Parameter loadFromConfig(Map<String, Object> configMap, String xPath)
            throws GenericEntityConfException, ServiceConfigException {
        return new Parameter(configMap, xPath);
    }

    public String getValue() {
        return value;
    }

    @Override
    public String getName() {
        return name;
    }

}
