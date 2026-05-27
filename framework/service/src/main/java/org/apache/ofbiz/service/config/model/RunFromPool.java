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
 * An object that models the <code>&lt;run-from-pool&gt;</code> element.
 */
@ThreadSafe
public final class RunFromPool extends AbstractConfigElement {

    public static final String ELEMENT_NAME = "run-from-pool";
    private final String xPath;
    private final String name;

    RunFromPool(Element runFromPoolElement, String xPathParent) throws ServiceConfigException {
        String name = runFromPoolElement.getAttribute("name").intern();
        if (name.isEmpty()) {
            throw new ServiceConfigException("<run-from-pool> element name attribute is empty");
        }
        this.name = name;
        xPath = xPathParent.concat("/run-from-pool[@name='" + name + "']");
    }

    RunFromPool(Map<String, Object> configObject, String xPath) throws ServiceConfigException {
        String name = getNameFromXPath(xPath);
        if (name.isEmpty()) {
            throw new ServiceConfigException("<run-from-pool> element name attribute is empty");
        }
        this.name = name;
        this.xPath = xPath;
    }

    public static RunFromPool loadFromXml(Element element, String xPathParent)
            throws GenericEntityConfException, ServiceConfigException {
        return new RunFromPool(element, xPathParent);
    }

    public static RunFromPool loadFromConfig(Map<String, Object> configMap, String xPath)
            throws GenericEntityConfException, ServiceConfigException {
        return new RunFromPool(configMap, xPath);
    }

    @Override
    public boolean allowMultipleSources() {
        return false;
    }

    @Override
    public String getName() {
        return name;
    }

}
