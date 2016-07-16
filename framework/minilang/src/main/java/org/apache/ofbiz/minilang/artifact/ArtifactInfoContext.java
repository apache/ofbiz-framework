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
package org.apache.ofbiz.minilang.artifact;

import java.util.HashSet;
import java.util.Set;

import org.apache.ofbiz.minilang.SimpleMethod;

/**
 * An object used for gathering artifact information.
 */
public final class ArtifactInfoContext {

    private final Set<String> entityNameSet = new HashSet<String>();
    private final Set<String> serviceNameSet = new HashSet<String>();
    private final Set<String> simpleMethodNameSet = new HashSet<String>();

    /**
     * Adds an entity name to this context.
     * @param name The entity name to add to this context
     */
    public void addEntityName(String name) {
        if (name != null) {
            this.entityNameSet.add(name);
        }
    }

    /**
     * Adds a service name to this context.
     * @param name The service name to add to this context
     */
    public void addServiceName(String name) {
        if (name != null) {
            this.serviceNameSet.add(name);
        }
    }

    /**
     * Adds a visited <code>SimpleMethod</code> to this context.
     * @param method the <code>SimpleMethod</code> to add to this context
     */
    public void addSimpleMethod(SimpleMethod method) {
        this.simpleMethodNameSet.add(method.getLocationAndName());
    }

    /**
     * Returns the entity names in this context.
     * @return The entity names in this context
     */
    public Set<String> getEntityNames() {
        return this.entityNameSet;
    }

    /**
     * Returns the service names in this context.
     * @return The service names in this context
     */
    public Set<String> getServiceNames() {
        return this.serviceNameSet;
    }

    /**
     * Returns <code>true</code> if this context has visited <code>method</code>.
     * @param method The <code>SimpleMethod</code> to test
     * @return <code>true</code> if this context has visited <code>method</code>
     */
    public boolean hasVisited(SimpleMethod method) {
        return simpleMethodNameSet.contains(method.getLocationAndName());
    }
}
