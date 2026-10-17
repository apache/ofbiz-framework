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
package org.apache.ofbiz.common.test

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.testtools.JunitJupiterTest
import org.apache.ofbiz.testtools.JupiterTestHelper
import org.junit.jupiter.api.Test

@JunitJupiterTest
class UserLoginTests implements JupiterTestHelper {

    @Test
    void testCreateUserLogin() {
        String userLoginId = 'demo.person'

        Map serviceCtx = [
                userLoginId: userLoginId,
                enabled: 'Y',
                currentPassword: 'ofbiz',
                currentPasswordVerify: 'ofbiz'
        ]
        Map serviceResult = dispatcher.runSync('createUserLogin', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue createdUserLogin = from('UserLogin')
                .where('userLoginId', userLoginId)
                .queryOne()
        assert createdUserLogin
        assert createdUserLogin.enabled == 'Y'
    }

    /*
     * createUserLogin's duplicate-userLoginId check compares userLoginId ignoring case. A
     * userLoginId containing quotes and parentheses should be compared literally, just like any
     * other value, and match only an existing UserLogin with that exact id - so creating a new,
     * not-yet-registered userLoginId built from such characters should still succeed.
     */
    @Test
    void testCreateUserLoginIgnoreCaseDuplicateCheckComparesValueLiterally() {
        String userLoginId = "\\') OR (1=1) OR ('"

        Map serviceCtx = [
                userLoginId: userLoginId,
                enabled: 'Y',
                currentPassword: 'ofbiz',
                currentPasswordVerify: 'ofbiz'
        ]
        Map serviceResult = dispatcher.runSync('createUserLogin', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult): serviceResult.errorMessage

        GenericValue createdUserLogin = from('UserLogin')
                .where('userLoginId', userLoginId)
                .queryOne()
        assert createdUserLogin
    }

}
