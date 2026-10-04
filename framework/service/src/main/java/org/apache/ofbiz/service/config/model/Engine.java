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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.ofbiz.base.config.AbstractConfigElement;
import org.apache.ofbiz.base.config.ConfigHelper;
import org.apache.ofbiz.base.lang.ThreadSafe;
import org.apache.ofbiz.entity.GenericEntityConfException;
import org.apache.ofbiz.service.config.ServiceConfigException;
import org.w3c.dom.Element;

/**
 * An object that models the <code>&lt;engine&gt;</code> element.
 */
@ThreadSafe
public final class Engine extends AbstractConfigElement {

    private final ServiceConfigGetter config = ServiceConfigGetter.getInstance();
    public static final String ELEMENT_NAME = "engine";
    private final String xPath;

    private final String className;
    private final String name;
    private final List<Parameter> parameters;
    private final Map<String, Parameter> parameterMap;

    Engine(Element engineElement, String xPathParent) throws ServiceConfigException {
        boolean checkStructure = ConfigHelper.checkStrictXmlStructure();
        String name = engineElement.getAttribute("name").intern();
        if (name.isEmpty() && checkStructure) {
            throw new ServiceConfigException("<engine> element name attribute is empty");
        }
        this.name = name;
        xPath = xPathParent.concat("/engine[@name='" + name + "']");
        String className = config.getValue(xPath.concat("/@class"));
        if (className.isEmpty() && checkStructure) {
            throw new ServiceConfigException("<engine> element class attribute is empty");
        }
        this.className = className;
        List<Parameter> parameterList = config.getSubElementsAsListEntries(xPath, engineElement, Parameter.class);
        parameters = Collections.unmodifiableList(parameterList);
        if (parameterList.isEmpty()) {
            parameterMap = Collections.emptyMap();
        } else {
            this.parameterMap = Collections.unmodifiableMap(config.getSubElementsAsMapValues(xPath,
                    engineElement, Parameter.class));
        }
    }

    Engine(Map<String, Object> configObject, String xPath) throws ServiceConfigException {
        this.xPath = xPath;
        String name = getNameFromXPath(xPath);
        if (name.isEmpty()) {
            throw new ServiceConfigException("<engine> element name attribute is empty");
        }
        this.name = name;
        String className = config.getValue(configObject, "/@class");
        if (className.isEmpty()) {
            throw new ServiceConfigException("<engine> element class attribute is empty");
        }
        this.className = className;
        List<Parameter> parameterList = config.getSubElementsAsListEntries(xPath, null, Parameter.class);
        parameters = Collections.unmodifiableList(parameterList);
        if (parameterList.isEmpty()) {
            parameterMap = Collections.emptyMap();
        } else {
            this.parameterMap = Collections.unmodifiableMap(config.getSubElementsAsMapValues(xPath, null, Parameter.class));
        }
    }

    public static Engine loadFromXml(Element element, String xPathParent)
            throws GenericEntityConfException, ServiceConfigException {
        return new Engine(element, xPathParent);
    }

    public static Engine loadFromConfig(Map<String, Object> configMap, String xPath)
            throws GenericEntityConfException, ServiceConfigException {
        return new Engine(configMap, xPath);
    }

    public String getClassName() {
        return className;
    }

    public Parameter getParameter(String parameterName) {
        return parameterMap.get(parameterName);
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public String getParameterValue(String parameterName) {
        Parameter parameter = parameterMap.get(parameterName);
        return parameter != null
                ? parameter.getValue()
                : null;
    }

    @Override
    public String getName() {
        return name;
    }

}
