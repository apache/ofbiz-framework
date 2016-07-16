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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.ofbiz.base.lang.ThreadSafe;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.service.config.ServiceConfigException;
import org.w3c.dom.Element;

/**
 * An object that models the <code>&lt;notification-group&gt;</code> element.
 */
@ThreadSafe
public final class NotificationGroup {

    private final String name;
    private final Notification notification;
    private final List<Notify> notifyList;

    NotificationGroup(Element notificationGroupElement) throws ServiceConfigException {
        String name = notificationGroupElement.getAttribute("name").intern();
        if (name.isEmpty()) {
            throw new ServiceConfigException("<notification-group> element name attribute is empty");
        }
        this.name = name;
        Element notification = UtilXml.firstChildElement(notificationGroupElement, "notification");
        if (notification == null) {
            throw new ServiceConfigException("<notification> element is missing");
        }
        this.notification = new Notification(notification);
        List<? extends Element> notifyElementList = UtilXml.childElementList(notificationGroupElement, "notify");
        if (notifyElementList.size() < 2) {
            throw new ServiceConfigException("<notify> element(s) missing");
        } else {
            List<Notify> notifyList = new ArrayList<Notify>(notifyElementList.size());
            for (Element notifyElement : notifyElementList) {
                notifyList.add(new Notify(notifyElement));
            }
            this.notifyList = Collections.unmodifiableList(notifyList);
        }
    }

    public String getName() {
        return name;
    }

    public Notification getNotification() {
        return notification;
    }

    public List<Notify> getNotifyList() {
        return this.notifyList;
    }
}
