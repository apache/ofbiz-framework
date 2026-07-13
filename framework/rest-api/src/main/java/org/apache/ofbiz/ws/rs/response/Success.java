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
package org.apache.ofbiz.ws.rs.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonRootName;

@JsonRootName(value = "success")
@JsonInclude(Include.NON_NULL)
public class Success {
    private int statusCode;
    private String statusDescription;
    private String successMessage;
    private Object data;

    /**
     * Creates a success response with basic status and message information.
     *
     * @param statusCode HTTP status code
     * @param statusDescription HTTP status reason phrase or description
     * @param successMessage human-readable success message
     * @param data
     */
    public Success(int statusCode, String statusDescription, String successMessage, Object data) {
        this.statusCode = statusCode;
        this.statusDescription = statusDescription;
        this.successMessage = successMessage;
        this.data = data;
    }

    /**
     * Returns the status code.
     *
     * @return the status code representing the result
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Sets the status code.
     *
     * @param statusCode the status code to set
     */
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * Returns the status description.
     *
     * @return the status description
     */
    public String getStatusDescription() {
        return statusDescription;
    }

    /**
     * Sets the status description.
     *
     * @param statusDescription the status description to set
     */
    public void setStatusDescription(String statusDescription) {
        this.statusDescription = statusDescription;
    }

    /**
     * Returns the success message.
     *
     * @return the success message
     */
    public String getSuccessMessage() {
        return successMessage;
    }

    /**
     * Sets the success message.
     *
     * @param successMessage the success message to set
     */
    public void setSuccessMessage(String successMessage) {
        this.successMessage = successMessage;
    }

    /**
     * Returns the response data.
     *
     * @return the data object
     */
    public Object getData() {
        return data;
    }

    /**
     * Sets the response data.
     *
     * @param data the data object to set
     */
    public void setData(Object data) {
        this.data = data;
    }

}
