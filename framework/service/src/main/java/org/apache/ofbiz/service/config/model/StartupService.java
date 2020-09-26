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
 * An object that models the <code>&lt;startup-service&gt;</code> element.
 */
@ThreadSafe
public final class StartupService {

    private final String name;
    private final String runInPool;
    private final String runtimeDataId;
    private final int runtimeDelay;

    StartupService(Element startupServiceElement) throws ServiceConfigException {
        String name = startupServiceElement.getAttribute("name").intern();
        if (name.isEmpty()) {
            throw new ServiceConfigException("<startup-service> element name attribute is empty");
        }
        this.name = name;
        String runtimeDataId = startupServiceElement.getAttribute("runtime-data-id").intern();
        this.runtimeDataId = runtimeDataId.isEmpty() ? null : runtimeDataId;
        String runtimeDelay = startupServiceElement.getAttribute("runtime-delay").intern();
        if (runtimeDelay.isEmpty()) {
            this.runtimeDelay = 0;
        } else {
            try {
                this.runtimeDelay = Integer.parseInt(runtimeDelay);
            } catch (Exception e) {
                throw new ServiceConfigException("<startup-service> element runtime-delay attribute value is invalid");
            }
        }
        this.runInPool = startupServiceElement.getAttribute("run-in-pool").intern();
    }

    public String getName() {
        return name;
    }

    public String getRunInPool() {
        return runInPool;
    }

    public String getRuntimeDataId() {
        return runtimeDataId;
    }

    public int getRuntimeDelay() {
        return runtimeDelay;
    }
}
