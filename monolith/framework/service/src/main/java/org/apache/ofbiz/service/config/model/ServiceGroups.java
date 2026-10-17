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
 * An object that models the <code>&lt;service-groups&gt;</code> element.
 */
@ThreadSafe
public final class ServiceGroups {

    private final String loader;
    private final String location;

    ServiceGroups(Element serviceGroupsElement) throws ServiceConfigException {
        String loader = serviceGroupsElement.getAttribute("loader").intern();
        if (loader.isEmpty()) {
            throw new ServiceConfigException("<service-groups> element loader attribute is empty");
        }
        this.loader = loader;
        String location = serviceGroupsElement.getAttribute("location").intern();
        if (location.isEmpty()) {
            throw new ServiceConfigException("<service-groups> element location attribute is empty");
        }
        this.location = location;
    }

    public String getLoader() {
        return loader;
    }

    public String getLocation() {
        return location;
    }
}
