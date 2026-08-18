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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilValidate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ModelOperation {

    private String service;
    private String verb;
    private String produces;
    private String consumes;
    private String path;
    private String description;
    private boolean auth;
    private String primaryPermission;
    private String mainAction;
    private String addApiResponses;
    private String customHeaders;
    private List<ModelQueryParam> queryParams;
    private List<ModelExample> examples;

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
     * Gets the primary permission
     *
     * @return the primaryPermission
     */
    public String getPrimaryPermission() {
        return primaryPermission;
    }

    /**
     * Sets the primary permission
     *
     * @param primaryPermission the primaryPermission to set
     */
    public void setPrimaryPermission(String primaryPermission) {
        this.primaryPermission = primaryPermission;
    }

    /**
     * Sets the primaryPermission and returns this ModelOperation
     *
     * @param primaryPermission
     * @return this ModelOperation
     */
    public ModelOperation primaryPermission(String primaryPermission) {
        this.primaryPermission = primaryPermission;
        return this;
    }

    /**
     * Sets the mainAction (VIEW, CREATE, UPDATE, DELETE, ADMIN)
     *
     * @return the mainAction
     */
    public String getMainAction() {
        return mainAction;
    }

    /**
     * Gets the mainAction (VIEW, CREATE, UPDATE, DELETE, ADMIN)
     *
     * @param mainAction the mainAction to set
     */
    public void setMainAction(String mainAction) {
        this.mainAction = mainAction;
    }

    /**
     * Sets the mainAction (VIEW, CREATE, UPDATE, DELETE, ADMIN)
     * and returns this instance
     *
     * @param mainAction
     * @return this ModelOperation
     */
    public ModelOperation mainAction(String mainAction) {
        this.mainAction = mainAction;
        return this;
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

    /**
     * Gets the raw, comma-separated custom headers string configured for this resource.
     *
     * @return the customHeaders as a raw comma-separated string, or {@code null} if unset
     */
    public String getCustomHeaders() {
        return customHeaders;
    }

    /**
     * Sets the raw custom headers for this resource, as a comma-separated string.
     *
     * @param customHeaders the customHeaders to set, expected as a comma-separated list
     */
    public void setCustomHeaders(String customHeaders) {
        this.customHeaders = customHeaders;
    }

    /**
     * Fluent-style setter for customHeaders, allowing method chaining when building
     * a {@link ModelOperation}.
     *
     * @param customHeaders the comma-separated custom headers to set
     * @return this {@link ModelOperation} instance, for chaining
     */
    public ModelOperation customHeaders(String customHeaders) {
        this.customHeaders = customHeaders;
        return this;
    }

    /**
     * Parses the raw {@link #customHeaders} string into a list of individual header names,
     * splitting on commas.
     *
     * @return a {@link List} of individual custom header names; an empty list if
     * {@link #customHeaders} is unset or empty
     */
    public List<String> getCustomHeadersList() {
        if (UtilValidate.isEmpty(customHeaders)) {
            return new ArrayList<>();
        }
        return StringUtil.split(customHeaders, ",");
    }

    /**
     * Retrieves queryParams if they are set or an empty ArrayList if not
     *
     * @return {@link #queryParams}
     */
    public List<ModelQueryParam> getQueryParams() {
        return queryParams == null ? new ArrayList<ModelQueryParam>() : queryParams;
    }

    /**
     * Fluent-style setter for adding a queryParam, allowing method chaining when building
     * a {@link ModelOperation}.
     * @param queryParam
     * @return this {@link ModelOperation} instance
     */
    public ModelOperation addQueryParam(ModelQueryParam queryParam) {
        if (this.queryParams == null) {
            this.queryParams = new ArrayList<>();
        }
        this.queryParams.add(queryParam);
        return this;
    }

    /**
     * Returns the list of examples
     * @return {@link List<ModelExample>}
     */
    public List<ModelExample> getExamples() {
        return examples == null ? new ArrayList<ModelExample>() : examples;
    }

    /**
     * Fluent-style setter for adding a {@link ModelExample}, allowing method chaining when building
     * a {@link ModelOperation}.
     * @param example {@link ModelExample}
     * @return {@link ModelOperation} instance
     */
    public ModelOperation addExample(ModelExample example) {
        if (this.examples == null) {
            this.examples = new ArrayList<>();
        }
        this.examples.add(example);
        return this;
    }

    /**
     * Retrieves an {@link ModelExample} based on type and code.
     * Returns null if nothings found
     * @param type
     * @param code
     * @return {@link ModelExample}
     */
    public ModelExample getExample(String type, String code) {
        if (UtilValidate.isEmpty(type) || UtilValidate.isEmpty(code)) {
            return null;
        }
        return getExamples().stream().filter(param -> (type.equals(param.getType()) && code.equals(param.getCode()))).findFirst().orElse(null);

    }

    /**
     * Tries to get a HashMap out of a JSON example text, preserving the order of the entries.
     * Returns a simple @String object if this fails.
     * @param type
     * @param code
     * @return
     */
    public Object getExampleObject(String type, String code) {
        ModelExample example = getExample(type, code);

        if (example == null) {
            return null;
        }

        String text = example.getExampleText();
        if (text == null) {
            return null;
        }

        Map<String, Object> obj = null;
        try {
            obj = UtilGenerics.cast(new ObjectMapper().readValue(text, LinkedHashMap.class));
        } catch (JsonProcessingException e) {
        }

        if (obj != null) {
            return obj;
        }
        return new String(text);
    }

    @Override
    public String toString() {
        return "service: " + service + ", path: " + path + ", verb: " + verb + ", description: " + description
                + ", produces: " + produces + ", primaryPermission: " + primaryPermission + ", mainAction:  "
                + mainAction + ", addApiResponses:" + addApiResponses + ", customHeaders: " + customHeaders
                + ", queryParams: " + queryParams;
    }

}
