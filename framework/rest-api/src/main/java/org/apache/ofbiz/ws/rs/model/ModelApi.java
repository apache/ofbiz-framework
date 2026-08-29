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
package org.apache.ofbiz.ws.rs.model;

import java.util.ArrayList;
import java.util.List;

public class ModelApi {

    private List<ModelResource> resources;
    private List<ModelMapping> mappings;
    private String name;
    private String path;
    private String displayName;
    private String description;
    private boolean publish;

    /**
     * Returns the List of ModelResources. Creates an empty
     * List if no resources set.
     *
     * @return the resources value
     */
    public List<ModelResource> getResources() {
        if (resources == null) {
            resources = new ArrayList<>();
        }
        return this.resources;
    }

    /**
     * Adds a resource to this API definition and returns the current instance
     * to support method chaining.
     *
     * @param resource the {@link ModelResource} to add
     * @return this {@link ModelApi} instance
     */
    public ModelApi addResource(ModelResource resource) {
        if (this.resources == null) {
            this.resources = new ArrayList<>();
        }
        this.resources.add(resource);
        return this;
    }

    /**
     * Returns the mappings
     *
     * @return mappings List
     */
    public List<ModelMapping> getMappings() {
        if (mappings == null) {
            mappings = new ArrayList<>();
        }
        return this.mappings;
    }

    /**
     * Adds a mapping List
     *
     * @param mapping the {@link ModelMapping} to add
     */
    public ModelApi addMapping(ModelMapping mapping) {
        if (this.mappings == null) {
            this.mappings = new ArrayList<>();
        }
        this.mappings.add(mapping);
        return this;
    }
    /**
     * Returns the name.
     *
     * @return the name value
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the path.
     *
     * @return the path value
     */
    public String getPath() {
        return path;
    }

    /**
     * Sets the name.
     *
     * @param value the name value to set
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Sets the path.
     *
     * @param path the path value to set
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Returns the display name.
     *
     * @return the display name value
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Sets the display name.
     *
     * @param value the display name value to set
     */
    public void setDisplayName(String value) {
        this.displayName = value;
    }

    /**
     * Returns the description.
     *
     * @return the description value
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description.
     *
     * @param value the description value to set
     */
    public void setDescription(String value) {
        this.description = value;
    }

    /**
     * Returns whether publish is enabled.
     *
     * @return true if publish is enabled, otherwise false
     */
    public boolean isPublish() {
        return publish;
    }

    /**
     * Sets whether publish is enabled.
     *
     * @param publish true to enable publish, false to disable it
     */
    public void setPublish(boolean publish) {
        this.publish = publish;
    }

}
