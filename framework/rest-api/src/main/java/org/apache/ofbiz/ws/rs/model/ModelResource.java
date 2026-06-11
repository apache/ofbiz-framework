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

public class ModelResource {

    private List<ModelOperation> operations;
    private List<ModelResource> subResources = new ArrayList<>();
    private String name;
    private String path;
    private String displayName;
    private String description;
    private boolean publish;
    private boolean auth;

    /**
     * Returns whether the user is authenticated.
     *
     * @return true if the user is authenticated, false otherwise
     */
    public boolean isAuth() {
        return auth;
    }

    /**
     * Sets the authentication status of the user.
     *
     * @param auth true to mark the user as authenticated, false otherwise
     */
    public void setAuth(boolean auth) {
        this.auth = auth;
    }

    /**
     * Sets whether this resource requires JWT authentication and returns
     * this instance to support method chaining.
     *
     * @param auth {@code true} if a valid JWT is required; {@code false} otherwise
     * @return this {@link ModelResource} instance
     */
    public ModelResource auth(boolean auth) {
        this.auth = auth;
        return this;
    }

    /**
     * Sets whether the item is published.
     *
     * @param publish true to mark as published, false otherwise
     */
    protected void setPublish(boolean publish) {
        this.publish = publish;
    }

    /**
     * Returns the operations defined for this resource, or an empty list if
     * no operations have been added yet.
     *
     * @return the list of {@link ModelOperation} instances; never {@code null}
     */
    public List<ModelOperation> getOperations() {
        return operations == null ? new ArrayList<ModelOperation>() : operations;
    }

    /**
     * Adds an operation to this resource and returns this instance
     * to support method chaining.
     *
     * @param operation the {@link ModelOperation} to add
     * @return this {@link ModelResource} instance
     */
    public ModelResource addOperation(ModelOperation operation) {
        if (this.operations == null) {
            this.operations = new ArrayList<>();
        }
        this.operations.add(operation);
        return this;
    }

    /**
     * Returns the name.
     *
     * @return the current name value
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name.
     *
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the name of this resource and returns this instance
     * to support method chaining.
     *
     * @param name the resource name
     * @return this {@link ModelResource} instance
     */
    public ModelResource name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets the path of this resource and returns this instance
     * to support method chaining.
     *
     * @param path the URL path segment for this resource
     * @return this {@link ModelResource} instance
     */
    public ModelResource path(String path) {
        this.path = path;
        return this;
    }

    /**
     * Returns the path.
     *
     * @return the current path value
     */
    public String getPath() {
        return path;
    }

    /**
     * Sets the path.
     *
     * @param path the path to set
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Returns the display name.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Sets the display name of this resource and returns this instance
     * to support method chaining.
     *
     * @param displayName the human-readable display name
     * @return this {@link ModelResource} instance
     */
    public ModelResource displayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    /**
     * Sets the display name.
     *
     * @param displayName the display name to set
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
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
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Adds a nested sub-resource to this resource.
     *
     * @param resource the child {@link ModelResource} to add
     */
    public void addSubResource(ModelResource resource) {
        this.subResources.add(resource);
    }

    /**
     * Returns the nested sub-resources of this resource.
     *
     * @return the list of child {@link ModelResource} instances; never {@code null}
     */
    public List<ModelResource> getSubResources() {
        return subResources;
    }

    /**
     * Sets the description of this resource and returns this instance
     * to support method chaining.
     *
     * @param description the resource description
     * @return this {@link ModelResource} instance
     */
    public ModelResource description(String description) {
        this.description = description;
        return this;
    }

    /**
     * Returns whether the item is published.
     *
     * @return true if the item is published, false otherwise
     */
    public boolean isPublish() {
        return publish;
    }

    /**
     * Sets whether this resource is published and returns this instance
     * to support method chaining.
     *
     * @param publish {@code true} if this resource should be published; {@code false} otherwise
     * @return this {@link ModelResource} instance
     */
    public ModelResource publish(boolean publish) {
        this.publish = publish;
        return this;
    }

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return "name: " + name + ", path: " + path + ", displayName: " + displayName + ", description: " + description
                + ", publish: " + publish;
    }

}
