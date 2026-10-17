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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRootName;

@JsonRootName(value = "error")
@JsonPropertyOrder({ "statusCode", "statusDescription", "errorType", "errorMessage", "errorDescription",
        "additionalErrors" })
public class Error {

    private int statusCode;
    private String statusDescription;
    private String errorMessage;
    @JsonProperty("errorType")
    private String type;
    private String errorCode;
    private String errorDescription;
    private List<String> additionalErrors;

    /**
     * Creates an empty error response.
     */
    public Error() {
    }

    /**
     * Creates an error response with basic status and message information.
     *
     * @param statusCode HTTP status code
     * @param statusDescription HTTP status reason phrase or description
     * @param errorMessage human-readable error message
     */
    public Error(int statusCode, String statusDescription, String errorMessage) {
        this.statusCode = statusCode;
        this.statusDescription = statusDescription;
        this.errorMessage = errorMessage;
    }

    /**
     * Creates an error response with additional error details.
     *
     * @param statusCode HTTP status code
     * @param statusDescription HTTP status reason phrase or description
     * @param errorMessage human-readable error message
     * @param additionalErrors list of additional validation or processing errors
     */
    public Error(int statusCode, String statusDescription, String errorMessage, List<String> additionalErrors) {
        this.statusCode = statusCode;
        this.statusDescription = statusDescription;
        this.errorMessage = errorMessage;
        this.additionalErrors = additionalErrors;
    }

    /**
     * Fluent builder methods for constructing an {@code Error} response.
     *
     * <p>These methods allow chaining to populate error metadata in a concise way.
     * Each method sets a field and returns the same {@code Error} instance.</p>
     */
    public Error description(String statusDescription) {
        this.statusDescription = statusDescription;
        return this;
    }

    /**
     * Fluent builder methods for constructing an {@code Error} response.
     *
     * <p>These methods allow chaining to populate error metadata in a concise way.
     * Each method sets a field and returns the same {@code Error} instance.</p>
     */
    public Error message(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    /**
     * Fluent builder methods for constructing an {@code Error} response.
     *
     * <p>These methods allow chaining to populate error metadata in a concise way.
     * Each method sets a field and returns the same {@code Error} instance.</p>
     */
    public Error type(String type) {
        this.type = type;
        return this;
    }

    /**
     * Fluent builder methods for constructing an {@code Error} response.
     *
     * <p>These methods allow chaining to populate error metadata in a concise way.
     * Each method sets a field and returns the same {@code Error} instance.</p>
     */
    public Error statusCode(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    /**
     * Fluent builder methods for constructing an {@code Error} response.
     *
     * <p>These methods allow chaining to populate error metadata in a concise way.
     * Each method sets a field and returns the same {@code Error} instance.</p>
     */
    public Error additionalErrors(List<String> additionalErrors) {
        this.additionalErrors = additionalErrors;
        return this;
    }

    /**
     * Fluent builder methods for constructing an {@code Error} response.
     *
     * <p>These methods allow chaining to populate error metadata in a concise way.
     * Each method sets a field and returns the same {@code Error} instance.</p>
     */
    public Error errorDescription(String errorDescription) {
        this.setErrorDescription(errorDescription);
        return this;
    }

    /**
     * Fluent builder methods for constructing an {@code Error} response.
     *
     * <p>These methods allow chaining to populate error metadata in a concise way.
     * Each method sets a field and returns the same {@code Error} instance.</p>
     */
    public Error errorCode(String errorCode) {
        this.setErrorCode(errorCode);
        return this;
    }

    /**
     * Returns the status code.
     *
     * @return the HTTP or application status code
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Returns the type.
     *
     * @return the type value
     */
    public String getType() {
        return type;
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
     * Returns the errorCode
     *
     * @return errorCode
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Sets the errorCode
     *
     * @param errorCode the errorCode to set
     */
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * Returns the error message.
     *
     * @return the error message
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets the error message.
     *
     * @param errorMessage the error message to set
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Returns the list of additional errors.
     *
     * @return list of additional error messages
     */
    public List<String> getAdditionalErrors() {
        return additionalErrors;
    }

    /**
     * Sets the list of additional errors.
     *
     * @param additionalErrors the additional errors to set
     */
    public void setAdditionalErrors(List<String> additionalErrors) {
        this.additionalErrors = additionalErrors;
    }

    /**
     * Returns the error description.
     *
     * @return the error description
     */
    public String getErrorDescription() {
        return errorDescription;
    }

    /**
     * Sets the error description.
     *
     * @param errorDescription the error description to set
     */
    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

}
