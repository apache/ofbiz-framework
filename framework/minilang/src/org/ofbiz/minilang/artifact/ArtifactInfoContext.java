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
package org.ofbiz.minilang.artifact;

import java.util.Set;

import javolution.util.FastSet;
import org.ofbiz.minilang.SimpleMethod;

/**
 * An object used for gathering artifact information.
 */
public final class ArtifactInfoContext {

    private final Set<String> entityNameSet = FastSet.newInstance();
    private final Set<String> serviceNameSet = FastSet.newInstance();
    private final Set<String> simpleMethodNameSet = FastSet.newInstance();

    public void addEntityName(String name) {
        if (name != null) {
            this.entityNameSet.add(name);
        }
    }

    public void addServiceName(String name) {
        if (name != null) {
            this.serviceNameSet.add(name);
        }
    }

    public void addSimpleMethod(SimpleMethod method) {
        this.simpleMethodNameSet.add(method.getLocationAndName());
    }

    public Set<String> getEntityNames() {
        return this.entityNameSet;
    }

    public Set<String> getServiceNames() {
        return this.serviceNameSet;
    }

    public boolean hasVisited(SimpleMethod method) {
        return simpleMethodNameSet.contains(method.getLocationAndName());
    }
}
