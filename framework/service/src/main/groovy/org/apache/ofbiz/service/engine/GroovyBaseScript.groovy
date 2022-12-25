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
package org.apache.ofbiz.service.engine

import org.apache.ofbiz.base.util.Debug
import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.util.EntityQuery
import org.apache.ofbiz.service.DispatchContext
import org.apache.ofbiz.service.ExecutionServiceException
import org.apache.ofbiz.service.LocalDispatcher
import org.apache.ofbiz.service.ModelService
import org.apache.ofbiz.service.ServiceUtil

abstract class GroovyBaseScript extends Script {
    public static final String module = GroovyBaseScript.class.getName()

    String getModule() {
        return this.class.getName()
    }

    Map runService(String serviceName, Map inputMap) throws ExecutionServiceException {
        LocalDispatcher dispatcher = binding.getVariable('dispatcher')
        DispatchContext dctx = dispatcher.getDispatchContext()
        if (!inputMap.userLogin) {
            inputMap.userLogin = this.binding.hasVariable('userLogin')
                    ? this.binding.getVariable('userLogin')
                    : this.binding.getVariable('parameters').userLogin
        }
        if (!inputMap.timeZone) {
            inputMap.timeZone = this.binding.hasVariable('timeZone')
                    ? this.binding.getVariable('timeZone')
                    : this.binding.getVariable('parameters').timeZone
        }
        if (!inputMap.locale) {
            inputMap.locale = this.binding.hasVariable('locale')
                    ? this.binding.getVariable('locale')
                    : this.binding.getVariable('parameters').locale
        }
        if (serviceName.equals("createAnonFile")) {
            String fileName = inputMap.get("dataResourceName")
            String fileNameAndPath = inputMap.get("objectInfo")
            File file = new File(fileNameAndPath)
            if (!fileName.isEmpty()) {
                // Check the file name
                if (!org.apache.ofbiz.security.SecuredUpload.isValidFileName(fileName, delegator)) {
                    String errorMessage = UtilProperties.getMessage("SecurityUiLabels", "SupportedFileFormatsIncludingSvg", inputMap.locale)
                    throw new ExecutionServiceException(errorMessage)
                }
                // TODO we could verify the file type (here "All") with dataResourceTypeId. Anyway it's done with isValidFile()
                // We would just have a better error message
                if (file.exists()) {
                    // Check if a webshell is not uploaded
                    if (!org.apache.ofbiz.security.SecuredUpload.isValidFile(fileNameAndPath, "All", delegator)) {
                        String errorMessage = UtilProperties.getMessage("SecurityUiLabels", "SupportedFileFormatsIncludingSvg", inputMap.locale)
                        throw new ExecutionServiceException(errorMessage)
                    }
                }
            }
        }
        Map serviceContext = dctx.makeValidContext(serviceName, ModelService.IN_PARAM, inputMap)
        Map result = dispatcher.runSync(serviceName, serviceContext)
        if (ServiceUtil.isError(result)) {
            throw new ExecutionServiceException(ServiceUtil.getErrorMessage(result))
        }
        return result
    }

    Map run(Map args) throws ExecutionServiceException {
        return runService((String)args.get('service'), (Map)args.get('with', new HashMap()))
    }

    Map makeValue(String entityName) throws ExecutionServiceException {
        return binding.getVariable('delegator').makeValue(entityName)
    }

    Map makeValue(String entityName, Map inputMap) throws ExecutionServiceException {
        return binding.getVariable('delegator').makeValidValue(entityName, inputMap)
    }

    EntityQuery from(def entity) {
        return EntityQuery.use(binding.getVariable('delegator')).from(entity)
    }

    EntityQuery select(String... fields) {
        return EntityQuery.use(binding.getVariable('delegator')).select(fields)
    }

    EntityQuery select(Set<String> fields) {
        return EntityQuery.use(binding.getVariable('delegator')).select(fields)
    }

    @Deprecated
    GenericValue findOne(String entityName, Map<String, ? extends Object> fields, boolean useCache) {
        return from(entityName).where(fields).cache(useCache).queryOne()
    }

    def success() {
        return success(null, null)
    }
    def success(String message) {
        return success(message, null)
    }
    def success(Map returnValues) {
        return success(null, returnValues)
    }
    def success(String message, Map returnValues) {
        // TODO: implement some clever i18n mechanism based on the userLogin and locale in the binding
        if (this.binding.hasVariable('request')) {
            // the script is invoked as an "event"
            if (message) {
                this.binding.getVariable('request').setAttribute("_EVENT_MESSAGE_", message)
            }
            if (returnValues) {
                returnValues.each {
                    this.binding.getVariable('request').setAttribute(it.getKey(), it.getValue())
                }
            }
            return 'success'
        } else {
            // the script is invoked as a "service"
            Map result = message ? ServiceUtil.returnSuccess(message) : ServiceUtil.returnSuccess()
            if (returnValues) {
                result.putAll(returnValues)
            }
            return result
        }
    }
    Map failure(String message) {
        // TODO: implement some clever i18n mechanism based on the userLogin and locale in the binding
        if (message) {
            return ServiceUtil.returnFailure(message)
        } else {
            return ServiceUtil.returnFailure()
        }
    }
    def error(String message) {
        // TODO: implement some clever i18n mechanism based on the userLogin and locale in the binding
        if (this.binding.hasVariable('request')) {
            // the script is invoked as an "event"
            if (message) {
                this.binding.getVariable('request').setAttribute("_ERROR_MESSAGE_", message)
            }
            return 'error'
        } else {
            if (message) {
                return ServiceUtil.returnError(message)
            } else {
                return ServiceUtil.returnError()
            }
        }
    }

    def logInfo(String message) {
        Debug.logInfo(message, getModule())
    }
    def logWarning(String message) {
        Debug.logWarning(message, getModule())
    }
    def logError(String message) {
        Debug.logError(message, getModule())
    }
    def logError(Throwable t, String message) {
        Debug.logError(t, message, getModule())
    }
    def logError(Throwable t) {
        Debug.logError(t, null, getModule())
    }
    def logVerbose(String message) {
        Debug.logVerbose(message, getModule())
    }

    def label(String ressource, String message) {
        return label(ressource, message, null)
    }
    def label(String ressource, String message, Map context) {
        Locale locale = this.binding.getVariable('locale')
        if (!locale) {
            locale = Locale.getDefault()
        }
        if (context) {
            return UtilProperties.getMessage(ressource, message, context, locale)
        }
        return UtilProperties.getMessage(ressource, message, locale)
    }
}
