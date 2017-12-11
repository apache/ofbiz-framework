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
package org.apache.ofbiz.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.ofbiz.base.util.UtilValidate;

/**
 * ServiceValidationException
 */
@SuppressWarnings("serial")
public class ServiceValidationException extends GenericServiceException {

    protected List<String> messages = new ArrayList<>();
    protected List<String> missingFields = new ArrayList<>();
    protected List<String> extraFields = new ArrayList<>();
    protected String errorMode = null;
    protected ModelService service = null;

    public ServiceValidationException(ModelService service, List<String> missingFields, List<String> extraFields, String errorMode) {
        super();
        this.service = service;
        this.errorMode = errorMode;
        if (missingFields != null) {
            this.missingFields = missingFields;
        }
        if (extraFields != null) {
            this.extraFields = extraFields;
        }
    }

    public ServiceValidationException(String str, ModelService service) {
        super(str);
        this.service = service;
    }

    public ServiceValidationException(String str, ModelService service, List<String> missingFields, List<String> extraFields, String errorMode) {
        super(str);
        this.service = service;
        this.errorMode = errorMode;
        if (missingFields != null) {
            this.missingFields = missingFields;
        }
        if (extraFields != null) {
            this.extraFields = extraFields;
        }
    }

    public ServiceValidationException(String str, Throwable nested, ModelService service) {
        super(str, nested);
        this.service = service;
    }

    public ServiceValidationException(String str, Throwable nested, ModelService service, List<String> missingFields, List<String> extraFields, String errorMode) {
        super(str, nested);
        this.service = service;
        this.errorMode = errorMode;
        if (missingFields != null) {
            this.missingFields = missingFields;
        }
        if (extraFields != null) {
            this.extraFields = extraFields;
        }
    }

    public ServiceValidationException(List<String> messages, ModelService service, List<String> missingFields, List<String> extraFields, String errorMode) {
        super();
        this.messages = messages;
        this.service = service;
        this.errorMode = errorMode;
        if (missingFields != null) {
            this.missingFields = missingFields;
        }
        if (extraFields != null) {
            this.extraFields = extraFields;
        }
    }

    public ServiceValidationException(List<String> messages, ModelService service, String errorMode) {
        this(messages, service, null, null, errorMode);
    }

    public List<String> getExtraFields() {
        return extraFields;
    }

    public List<String> getMissingFields() {
        return missingFields;
    }

    @Override
    public List<String> getMessageList() {
        if (UtilValidate.isEmpty(this.messages)) {
            return null;
        }
        return this.messages;
    }

    public ModelService getModelService() {
        return service;
    }

    public String getMode() {
        return errorMode;
    }

    public String getServiceName() {
        if (service != null) {
            return service.name;
        }
        return null;
    }

    @Override
    public String getMessage() {
        String msg = super.getMessage();
        if (UtilValidate.isNotEmpty(this.messages)) {
            StringBuilder sb = new StringBuilder();
            if (msg != null) {
                sb.append(msg).append('\n');
            }
            for (String m: this.messages) {
                sb.append(m);
            }
            msg = sb.toString();
        }
        return msg;
    }
}

