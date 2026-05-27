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
import org.apache.ofbiz.base.lang.ThreadSafe;
import org.apache.ofbiz.entity.GenericEntityConfException;
import org.apache.ofbiz.service.config.ServiceConfigException;
import org.w3c.dom.Element;

/**
 * An object that models the <code>&lt;notification-group&gt;</code> element.
 */
@ThreadSafe
public final class NotificationGroup extends AbstractConfigElement {

    private final ServiceConfigGetter config = ServiceConfigGetter.getInstance();
    public static final String ELEMENT_NAME = "notification-group";
    private final String xPath;

    private final String name;
    private final Notification notification;
    private final List<Notify> notifyList;

    NotificationGroup(Element notificationGroupElement, String xPathParent) throws ServiceConfigException {
        String name = notificationGroupElement.getAttribute("name").intern();
        if (name.isEmpty()) {
            throw new ServiceConfigException("<notification-group> element name attribute is empty");
        }
        this.name = name;
        xPath = xPathParent.concat("/notification-group[@name='" + name + "']");
        Notification notification = config.getObjectSubElement(xPath, notificationGroupElement, Notification.class);
        if (notification == null) {
            throw new ServiceConfigException("<notification> element is missing");
        }
        this.notification = notification;
        List<Notify> notifyList = config.getSubElementsAsListEntries(xPath + "/notification",
                notificationGroupElement, Notify.class);
        if (notifyList.size() < 2) {
            throw new ServiceConfigException("<notify> element(s) missing");
        }
        this.notifyList = Collections.unmodifiableList(notifyList);
    }

    NotificationGroup(Map<String, Object> configObject, String xPath) throws ServiceConfigException {
        this.xPath = xPath;
        String name = getNameFromXPath(xPath);
        if (name.isEmpty()) {
            throw new ServiceConfigException("<notification-group> element name attribute is empty");
        }
        this.name = name;
        Notification notification = config.getObjectSubElement(xPath, null, Notification.class);
        if (notification == null) {
            throw new ServiceConfigException("<notification> element is missing");
        }
        this.notification = notification;
        List<Notify> notifyList = config.getSubElementsAsListEntries(xPath, null, Notify.class);
        if (notifyList.size() < 2) {
            throw new ServiceConfigException("<notify> element(s) missing");
        } else {
            this.notifyList = Collections.unmodifiableList(notifyList);
        }
    }

    public static NotificationGroup loadFromXml(Element element, String xPathParent)
            throws GenericEntityConfException, ServiceConfigException {
        return new NotificationGroup(element, xPathParent);
    }

    public static NotificationGroup loadFromConfig(Map<String, Object> configMap, String xPath)
            throws GenericEntityConfException, ServiceConfigException {
        return new NotificationGroup(configMap, xPath);
    }

    public Notification getNotification() {
        return notification;
    }

    public List<Notify> getNotifyList() {
        return notifyList;
    }

    @Override
    public String getName() {
        return name;
    }

}
