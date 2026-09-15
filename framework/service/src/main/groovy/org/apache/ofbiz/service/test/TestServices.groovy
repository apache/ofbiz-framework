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
*/
package org.apache.ofbiz.service.test

// Despite it's name, this class is not for test, it's only services

Map testPingSuccess() {
    Map returnMap = success('Service result success')
    if (parameters.ping) {
        returnMap.pong = parameters.ping
    }
    return returnMap
}

Map testPingError() {
    Map returnMap = error('Service result error')
    if (parameters.ping) {
        returnMap.pong = parameters.ping
    }
    return returnMap
}

Map testPingSuccessWithDSLCall() {
    run service: 'testGroovyPingSuccess', with: parameters
}

Map testPingErrorWithDSLCall() {
    run service: 'testGroovyPingError', with: parameters
}

Map testPingSuccessWithAsyncDSLCall() {
    runServiceAsync('testGroovyPingSuccess', parameters)
    return success()
}

Map testPingSuccessWithI18n() {
    return success('ServiceErrorUiLabels', 'ServiceValueNotFound')
}

Map testPingSuccessWithI18nContext() {
    return success('ServiceErrorUiLabels', 'ServiceParameterValueNotValid',
            [parameterName: parameters.parameterName, errorDetails: parameters.errorDetails], [:])
}

Map testPingErrorWithI18n() {
    return error('ServiceErrorUiLabels', 'ServiceValueNotFound')
}

Map testPingErrorWithI18nContext() {
    return error('ServiceErrorUiLabels', 'ServiceParameterValueNotValid',
            [parameterName: parameters.parameterName, errorDetails: parameters.errorDetails])
}

Map testPingFailureWithI18n() {
    return failure('ServiceErrorUiLabels', 'ServiceValueNotFound')
}

Map testFail() {
    fail('Direct failure message')
    return success()
}

Map testRequire() {
    require(parameters.conditionMet, 'Required condition was not met')
    return success()
}

Map testFailFromNestedClosure() {
    // Proves the throw unwinds past the closure boundary, unlike `return error(...)` which would
    // only exit this closure and let execution fall through to the success() below.
    Closure innerCheck = {
        fail('Nested failure message')
    }
    innerCheck()
    return success()
}

Map testFailWithI18n() {
    fail('ServiceErrorUiLabels', 'ServiceValueNotFound')
    return success()
}

Map testRequireWithI18nContext() {
    require(false, 'ServiceErrorUiLabels', 'ServiceParameterValueNotValid',
            [parameterName: parameters.parameterName, errorDetails: parameters.errorDetails])
    return success()
}

Map testEntityDslCreate() {
    create('Testing', [testingId: parameters.testingId, testingName: parameters.testingName])
    return success()
}

Map testEntityDslUpdate() {
    update('Testing').where([testingId: parameters.testingId]).set([testingName: parameters.testingName])
    return success()
}

Map testEntityDslDelete() {
    int rowsRemoved = delete('Testing').where([testingId: parameters.testingId])
    return success([rowsRemoved: rowsRemoved])
}
