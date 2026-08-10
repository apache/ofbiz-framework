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

import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilValidate;

public class ModelOperation {

    private String service;
    private String verb;
    private String produces;
    private String consumes;
    private String path;
    private String description;
    private boolean auth;
    private String addApiResponses;

    /**
     * Returns whether the user is authenticated.
     *
     * @return true if authenticated, false otherwise
     */
    public boolean isAuth() {
        return auth;
    }

    /**
     * Sets the authentication state.
     *
     * @param auth true to mark as authenticated, false otherwise
     */
    public void setAuth(boolean auth) {
        this.auth = auth;
    }

    /**
     * @return the addApiResponses
     */
    public String getAddApiResponses() {
        return addApiResponses;
    }

    /**
     * @param addApiResponses the addApiResponses to set
     */
    public void setAddApiResponses(String addApiResponses) {
        this.addApiResponses = addApiResponses;
    }

    /**
     * @param addApiResponses the addApiResponses to set
     */
    public ModelOperation addApiResponses(String addApiResponses) {
        this.addApiResponses = addApiResponses;
        return this;
    }

    /**
     * @return the addApiResponses as list
     */
    public List<String> getAddApiResponsesList() {
        if (UtilValidate.isEmpty(addApiResponses)) {
            return new ArrayList<>();
        }
        return StringUtil.split(addApiResponses, ",");
    }


    /**
     * Sets whether this operation requires JWT authentication and returns
     * this instance.
     *
     * @param auth {@code true} if a valid JWT is required; {@code false} otherwise
     * @return this {@link ModelOperation} instance
     */
    public ModelOperation auth(boolean auth) {
        this.auth = auth;
        return this;
    }

    /**
     * Returns the service name.
     *
     * @return the service value
     */
    public String getService() {
        return service;
    }

    /**
     * Sets the service name.
     *
     * @param value the service value to set
     */
    public void setService(String value) {
        this.service = value;
    }

    /**
     * Sets the OFBiz service name for this operation and returns this instance.
     *
     * @param value the service name
     * @return this {@link ModelOperation} instance
     */
    public ModelOperation service(String value) {
        this.service = value;
        return this;
    }

    /**
     * Returns the HTTP verb.
     *
     * @return the verb value (e.g., GET, POST, PUT, DELETE)
     */
    public String getVerb() {
        return verb;
    }

    /**
     * Sets the HTTP verb.
     *
     * @param value the verb to set (e.g., GET, POST, PUT, DELETE)
     */
    public void setVerb(String value) {
        this.verb = value;
    }

    /**
     * Sets the HTTP verb for this operation and returns this instance.
     *
     * @param value the HTTP verb (e.g. {@code GET}, {@code POST})
     * @return this {@link ModelOperation} instance
     */
    public ModelOperation verb(String value) {
        this.verb = value;
        return this;
    }

    /**
     * Returns the response media type produced by the service.
     *
     * @return the produces value (e.g., application/json)
     */
    public String getProduces() {
        return produces;
    }

    /**
     * Sets the response media type produced by the service.
     *
     * @param value the produces value to set (e.g., application/json)
     */
    public void setProduces(String value) {
        this.produces = value;
    }

    /**
     * Sets the media type this operation produces and returns this instance.
     *
     * @param value the media type (e.g. {@code application/json})
     * @return this {@link ModelOperation} instance
     */
    public ModelOperation produces(String value) {
        this.produces = value;
        return this;
    }

    /**
     * Returns the request media type consumed by the service.
     *
     * @return the consumes value (e.g., application/json)
     */
    public String getConsumes() {
        return consumes;
    }

    /**
     * Sets the request media type consumed by the service.
     *
     * @param value the consumes value to set (e.g., application/json)
     */
    public void setConsumes(String value) {
        this.consumes = value;
    }

    /**
     * Sets the media type this operation consumes and returns this instance.
     *
     * @param value the media type (e.g. {@code application/json})
     * @return this {@link ModelOperation} instance
     */
    public ModelOperation consumes(String value) {
        this.consumes = value;
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
     * @param value the path to set
     */
    public void setPath(String value) {
        this.path = value;
    }

    /**
     * Sets the sub-path for this operation and returns this instance.
     *
     * @param value the sub-path relative to the parent resource path
     * @return this {@link ModelOperation} instance
     */
    public ModelOperation path(String value) {
        this.path = value;
        return this;
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
     * @param value the description to set
     */
    public void setDescription(String value) {
        this.description = value;
    }

    /**
     * Sets the description for this operation and returns this instance.
     *
     * @param value the operation description
     * @return this {@link ModelOperation} instance
     */
    public ModelOperation description(String value) {
        this.description = value;
        return this;
    }

    @Override
    public String toString() {
        return "service: " + service + ", path: " + path + ", verb: " + verb + ", description: " + description
                + ", produces: " + produces + ", addApiResponses:" + addApiResponses;
    }

}
