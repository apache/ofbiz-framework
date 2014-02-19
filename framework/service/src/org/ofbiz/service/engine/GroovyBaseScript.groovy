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
package org.ofbiz.service.engine

import org.ofbiz.base.util.Debug;
import org.ofbiz.service.ServiceUtil
import org.ofbiz.service.ExecutionServiceException

abstract class GroovyBaseScript extends Script {
    public static final String module = GroovyBaseScript.class.getName();

    Map runService(String serviceName, Map inputMap) throws ExecutionServiceException {
        if (!inputMap.userLogin) {
            inputMap.userLogin = this.binding.getVariable('parameters').userLogin;
        }
        Map result = binding.getVariable('dispatcher').runSync(serviceName, inputMap);
        if (ServiceUtil.isError(result)) {
            throw new ExecutionServiceException(ServiceUtil.getErrorMessage(result))
        }
        return result;
    }

    Map makeValue(String entityName) throws ExecutionServiceException {
        return result = binding.getVariable('delegator').makeValue(entityName);
    }

    Map findOne(String entityName, Map inputMap) {
        Map genericValue = binding.getVariable('delegator').findOne(entityName, inputMap, true);
        // TODO: get the list of pk fields from the map and use them only
        return genericValue;
    }

    List findList(String entityName, Map inputMap) {
        List genericValues = binding.getVariable('delegator').findByAnd(entityName, inputMap, null, false);
        // TODO: get the list of entity fields from the map and use them only
        return genericValues;
    }

    def success(String message) {
        // TODO: implement some clever i18n mechanism based on the userLogin and locale in the binding
        if (this.binding.hasVariable('request')) {
            // the script is invoked as an "event"
            if (message) {
                this.binding.getVariable('request').setAttribute("_EVENT_MESSAGE_", message)
            }
            return 'success';
        } else {
            // the script is invoked as a "service"
            if (message) {
                return ServiceUtil.returnSuccess(message);
            } else {
                return ServiceUtil.returnSuccess();
            }
        }
    }
    Map failure(String message) {
        // TODO: implement some clever i18n mechanism based on the userLogin and locale in the binding
        if (message) {
            return ServiceUtil.returnFailure(message);
        } else {
            return ServiceUtil.returnFailure();
        }
    }
    def error(String message) {
        // TODO: implement some clever i18n mechanism based on the userLogin and locale in the binding
        if (this.binding.hasVariable('request')) {
            // the script is invoked as an "event"
            if (message) {
                this.binding.getVariable('request').setAttribute("_ERROR_MESSAGE_", message)
            }
            return 'error';
        } else {
            if (message) {
                return ServiceUtil.returnError(message);
            } else {
                return ServiceUtil.returnError();
            }
        }
    }
    def logInfo(String message) {
        Debug.logInfo(message, module);
    }
    def logWarning(String message) {
        Debug.logWarning(message, module);
    }
    def logError(String message) {
        Debug.logError(message, module);
    }
}
