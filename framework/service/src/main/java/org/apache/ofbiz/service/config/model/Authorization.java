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
 * An object that models the <code>&lt;authorization&gt;</code> element.
 */
@ThreadSafe
public final class Authorization extends AbstractConfigElement {

    private final ServiceConfigGetter config = ServiceConfigGetter.getInstance();
    public static final String ELEMENT_NAME = "authorization";
    private final String xPath;

    private final String serviceName;

    Authorization(Element authElement, String xPathParent) throws ServiceConfigException {
        xPath = xPathParent.concat("/authorization");
        String serviceName = config.getValue(xPath + "/@service-name");
        if (serviceName.isEmpty()) {
            throw new ServiceConfigException("<authorization> element service-name attribute is empty");
        }
        this.serviceName = serviceName;
    }

    Authorization(Map<String, Object> configObject, String xPath) throws ServiceConfigException {
        this.xPath = xPath;
        String serviceName = getNameFromXPath(xPath + "/service-name");
        if (serviceName.isEmpty()) {
            throw new ServiceConfigException("<authorization> element service-name attribute is empty");
        }
        this.serviceName = serviceName;
    }

    public static Authorization loadFromXml(Element element, String xPathParent)
            throws GenericEntityConfException, ServiceConfigException {
        return new Authorization(element, xPathParent);
    }

    public static Authorization loadFromConfig(Map<String, Object> configMap, String xPath)
            throws GenericEntityConfException, ServiceConfigException {
        return new Authorization(configMap, xPath);
    }

    public String getServiceName() {
        return serviceName;
    }

    @Override
    public String getName() {
        return "authorization";
    }
}
