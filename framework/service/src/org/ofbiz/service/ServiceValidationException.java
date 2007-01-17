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
package org.ofbiz.service;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * ServiceValidationException
 */
public class ServiceValidationException extends GenericServiceException {

    protected List messages = new ArrayList();
    protected List missingFields = new ArrayList();
    protected List extraFields = new ArrayList();
    protected String errorMode = null;
    protected ModelService service = null;
    
    public ServiceValidationException(ModelService service, List missingFields, List extraFields, String errorMode) {
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

    public ServiceValidationException(String str, ModelService service, List missingFields, List extraFields, String errorMode) {
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

    public ServiceValidationException(String str, Throwable nested, ModelService service, List missingFields, List extraFields, String errorMode) {
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

    public ServiceValidationException(List messages, ModelService service, List missingFields, List extraFields, String errorMode) {
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

    public ServiceValidationException(List messages, ModelService service, String errorMode) {
        this(messages, service, null, null, errorMode);
    }

    public List getExtraFields() {
        return extraFields;
    }

    public List getMissingFields() {
        return missingFields;
    }

    public List getMessageList() {
        if (this.messages == null || this.messages.size() == 0) {
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
        } else {
            return null;
        }
    }

    public String getMessage() {
        String msg = super.getMessage();
        if (this.messages != null && this.messages.size() > 0) {
            if (msg != null) {
                msg += "\n";
            } else {
                msg = "";
            }
            Iterator i = this.messages.iterator();
            while (i.hasNext()) {
                msg += i.next();
            }
        }
        return msg;
    }
}

