/*
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
*/

package org.apache.ofbiz.webtools.service


import org.apache.ofbiz.service.config.ServiceConfigUtil

savedSyncResult = null
if (session.getAttribute('_SAVED_SYNC_RESULT_') != null) {
    savedSyncResult = session.getAttribute('_SAVED_SYNC_RESULT_')
}

serviceName = parameters.SERVICE_NAME
context.POOL_NAME = ServiceConfigUtil.getServiceEngine().getThreadPool().getSendToPool()

scheduleOptions = []
serviceParameters = []
e = request.getParameterNames()
while (e.hasMoreElements()) {
    paramName = e.nextElement()
    paramValue = parameters[paramName]
    scheduleOptions.add([name: paramName, value: paramValue])
}

context.scheduleOptions = scheduleOptions

if (serviceName) {
    dctx = dispatcher.getDispatchContext()
    model = null
    try {
        model = dctx.getModelService(serviceName)
    } catch (Exception exc) {
        context.errorMessageList = [exc.getMessage()]
    }
    if (model != null) {
        model.getInParamNames().each { paramName ->
            par = model.getParam(paramName)
            if (par.internal) {
                return
            }
            serviceParam = null
            if (savedSyncResult?.get(par.name)) {
                serviceParam = [name: par.name, type: par.type, optional: par.optional ? 'Y' : 'N',
                                defaultValue: par.defaultValue, value: savedSyncResult.get(par.name)]
            } else {
                serviceParam = [name: par.name, type: par.type, optional: par.optional ? 'Y' : 'N', defaultValue: par.defaultValue]
            }
            serviceParameters.add(serviceParam)
        }
    }
}
context.serviceParameters = serviceParameters
