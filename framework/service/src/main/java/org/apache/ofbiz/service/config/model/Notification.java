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
 * An object that models the <code>&lt;notification&gt;</code> element.
 */
@ThreadSafe
public final class Notification extends AbstractConfigElement {

    private final ServiceConfigGetter config = ServiceConfigGetter.getInstance();
    public static final String ELEMENT_NAME = "notification";
    private final String xPath;

    private final String screen;
    private final String service;
    private final String subject;

    Notification(Element notificationElement, String xPathParent) throws ServiceConfigException {
        xPath = xPathParent.concat("/notification");
        String subject = config.getValue(xPath.concat("/@subject"));
        if (subject.isEmpty()) {
            throw new ServiceConfigException("<notification> element subject attribute is empty");
        }
        this.subject = subject;
        String screen = config.getValue(xPath.concat("/@screen"));
        if (screen.isEmpty()) {
            throw new ServiceConfigException("<notification> element screen attribute is empty");
        }
        this.screen = screen;
        String service = config.getValue(xPath.concat("/@service"));
        if (service.isEmpty()) {
            service = "sendMailFromScreen";
        }
        this.service = service;
    }

    Notification(Map<String, Object> configObject, String xPath) throws ServiceConfigException {
        this.xPath = xPath;
        String subject = config.getValue(configObject, "/@subject");
        if (subject.isEmpty()) {
            throw new ServiceConfigException("<notification> element subject attribute is empty");
        }
        this.subject = subject;
        String screen = config.getValue(configObject, "/@screen");
        if (screen.isEmpty()) {
            throw new ServiceConfigException("<notification> element screen attribute is empty");
        }
        this.screen = screen;
        String service = config.getValue(configObject, "/@service");
        if (service.isEmpty()) {
            service = "sendMailFromScreen";
        }
        this.service = service;
    }

    public static Notification loadFromXml(Element element, String xPathParent)
            throws GenericEntityConfException, ServiceConfigException {
        return new Notification(element, xPathParent);
    }

    public static Notification loadFromConfig(Map<String, Object> configMap, String xPath)
            throws GenericEntityConfException, ServiceConfigException {
        return new Notification(configMap, xPath);
    }

    public String getScreen() {
        return screen;
    }

    public String getService() {
        return service;
    }

    public String getSubject() {
        return subject;
    }

    @Override
    public String getName() {
        return "notification";
    }
}
