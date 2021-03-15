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
    @JsonProperty("errorDescription")
    private String errorDesc;
    private List<String> additionalErrors;

    public Error() {

    }

    public Error(int statusCode, String statusDescription, String errorMessage) {
        this.statusCode = statusCode;
        this.statusDescription = statusDescription;
        this.errorMessage = errorMessage;
    }

    public Error(int statusCode, String statusDescription, String errorMessage, List<String> additionalErrors) {
        this.statusCode = statusCode;
        this.statusDescription = statusDescription;
        this.errorMessage = errorMessage;
        this.additionalErrors = additionalErrors;
    }

    /**
     * @param statusCode
     * @return
     */
    public Error code(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    /**
     * @param statusDescription
     * @return
     */
    public Error description(String statusDescription) {
        this.statusDescription = statusDescription;
        return this;
    }

    /**
     * @param errorMessage
     * @return
     */
    public Error message(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    /**
     * @param type
     * @return
     */
    public Error type(String type) {
        this.type = type;
        return this;
    }

    /**
     * @param statusCode
     * @return
     */
    public Error statusCode(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    /**
     * @param additionalErrors
     * @return
     */
    public Error additionalErrors(List<String> additionalErrors) {
        this.additionalErrors = additionalErrors;
        return this;
    }

    /**
     * @param errorDesc
     * @return
     */
    public Error errorDesc(String errorDesc) {
        this.setErrorDesc(errorDesc);
        return this;
    }

    /**
     * @return the statusCode
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param statusCode the statusCode to set
     */
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * @return the statusDescription
     */
    public String getStatusDescription() {
        return statusDescription;
    }

    /**
     * @param statusDescription the statusDescription to set
     */
    public void setStatusDescription(String statusDescription) {
        this.statusDescription = statusDescription;
    }

    /**
     * @return the errorMessage
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * @param errorMessage the errorMessage to set
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * @return the additionalErrors
     */
    public List<String> getAdditionalErrors() {
        return additionalErrors;
    }

    /**
     * @param additionalErrors the additionalErrors to set
     */
    public void setAdditionalErrors(List<String> additionalErrors) {
        this.additionalErrors = additionalErrors;
    }

    /**
     * @return the errorDesc
     */
    public String getErrorDesc() {
        return errorDesc;
    }

    /**
     * @param errorDesc the errorDesc to set
     */
    public void setErrorDesc(String errorDesc) {
        this.errorDesc = errorDesc;
    }

}
