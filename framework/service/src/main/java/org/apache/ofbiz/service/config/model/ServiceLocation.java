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

import org.apache.ofbiz.base.lang.ThreadSafe;
import org.apache.ofbiz.service.config.ServiceConfigException;
import org.w3c.dom.Element;

/**
 * An object that models the <code>&lt;service-location&gt;</code> element.
 */
@ThreadSafe
public final class ServiceLocation {

    private final String location;
    private final String name;

    ServiceLocation(Element serviceLocationElement, String location) throws ServiceConfigException {
        String name = serviceLocationElement.getAttribute("name").intern();
        if (name.isEmpty()) {
            throw new ServiceConfigException("<service-location> element name attribute is empty");
        }
        this.name = name;
        if (location.isEmpty()) {
            throw new ServiceConfigException("<service-location> element location attribute is empty");
        }
        this.location = location;
    }

    public String getLocation() {
        return location;
    }

    public String getName() {
        return name;
    }
}
