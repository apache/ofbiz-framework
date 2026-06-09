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

public class ModelOperation {

    private String service;
    private String verb;
    private String produces;
    private String consumes;
    private String path;
    private String description;
    private boolean auth;

    /**
     * @return the auth
     */
    public boolean isAuth() {
        return auth;
    }

    /**
     * @param auth the auth to set
     */
    public void setAuth(boolean auth) {
        this.auth = auth;
    }

    /**
     * @param auth
     * @return ModelOperation
     */
    public ModelOperation auth(boolean auth) {
        this.auth = auth;
        return this;
    }

    /**
     * Gets the value of the service property.
     *
     * @return possible object is {@link String }
     *
     */
    public String getService() {
        return service;
    }

    /**
     * Sets the value of the service property.
     *
     * @param value allowed object is {@link String }
     *
     */
    public void setService(String value) {
        this.service = value;
    }

    /**
     * @param value
     * @return ModelOperation
     */
    public ModelOperation service(String value) {
        this.service = value;
        return this;
    }

    /**
     * Gets the value of the verb property.
     *
     * @return possible object is {@link String }
     *
     */
    public String getVerb() {
        return verb;
    }

    /**
     * Sets the value of the verb property.
     *
     * @param value allowed object is {@link String }
     *
     */
    public void setVerb(String value) {
        this.verb = value;
    }

    /**
     * @param value
     * @return ModelOperation
     */
    public ModelOperation verb(String value) {
        this.verb = value;
        return this;
    }

    /**
     * Gets the value of the produces property.
     *
     * @return possible object is {@link String }
     *
     */
    public String getProduces() {
        return produces;
    }

    /**
     * Sets the value of the produces property.
     *
     * @param value allowed object is {@link String }
     *
     */
    public void setProduces(String value) {
        this.produces = value;
    }

    /**
     * @param value
     * @return ModelOperation
     */
    public ModelOperation produces(String value) {
        this.produces = value;
        return this;
    }

    /**
     * Gets the value of the consumes property.
     *
     * @return possible object is {@link String }
     *
     */
    public String getConsumes() {
        return consumes;
    }

    /**
     * Sets the value of the consumes property.
     *
     * @param value allowed object is {@link String }
     *
     */
    public void setConsumes(String value) {
        this.consumes = value;
    }

    /**
     * @param value
     * @return ModelOperation
     */
    public ModelOperation consumes(String value) {
        this.consumes = value;
        return this;
    }

    /**
     * Gets the value of the path property.
     *
     * @return possible object is {@link String }
     *
     */
    public String getPath() {
        return path;
    }

    /**
     * Sets the value of the path property.
     *
     * @param value allowed object is {@link String }
     *
     */
    public void setPath(String value) {
        this.path = value;
    }

    /**
     * @param value
     * @return ModelOperation
     */
    public ModelOperation path(String value) {
        this.path = value;
        return this;
    }

    /**
     * Gets the value of the description property.
     *
     * @return possible object is {@link String }
     *
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the value of the description property.
     *
     * @param value allowed object is {@link String }
     *
     */
    public void setDescription(String value) {
        this.description = value;
    }

    /**
     * @param value
     * @return ModelOperation
     */
    public ModelOperation description(String value) {
        this.description = value;
        return this;
    }

    /**
     * @return String
     */
    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return "service: " + service + ", path: " + path + ", verb: " + verb + ", description: " + description
                + ", produces: " + produces;
    }

}
