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
 * An object that models the <code>&lt;resource-loader&gt;</code> element.
 */
@ThreadSafe
public final class ResourceLoader {

    private final String className;
    private final String name;
    private final String prefix;
    private final String prependEnv;

    ResourceLoader(Element resourceLoaderElement) throws ServiceConfigException {
        String name = resourceLoaderElement.getAttribute("name").intern();
        if (name.isEmpty()) {
            throw new ServiceConfigException("<resource-loader> element name attribute is empty");
        }
        this.name = name;
        String className = resourceLoaderElement.getAttribute("class").intern();
        if (className.isEmpty()) {
            throw new ServiceConfigException("<resource-loader> element class attribute is empty");
        }
        this.className = className;
        this.prependEnv = resourceLoaderElement.getAttribute("prepend-env").intern();
        this.prefix = resourceLoaderElement.getAttribute("prefix").intern();
    }

    public String getClassName() {
        return className;
    }

    public String getName() {
        return name;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getPrependEnv() {
        return prependEnv;
    }
}
