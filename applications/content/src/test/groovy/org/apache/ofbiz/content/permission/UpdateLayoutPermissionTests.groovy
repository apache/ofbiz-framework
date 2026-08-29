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
package org.apache.ofbiz.content.permission

import static org.junit.jupiter.api.Assertions.assertThrows

import java.sql.Timestamp

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceAuthException
import org.apache.ofbiz.testtools.JunitJupiterTest
import org.apache.ofbiz.testtools.JupiterTestHelper
import org.junit.jupiter.api.Test

/**
 * Regression coverage for the updateLayout permission bypass: the event used to authorize its
 * attacker-supplied target Content under the unrelated CONTENT_CREATE operation and then update
 * the real Content and DataResource records directly, without checking UPDATE permission on
 * either one. It now delegates both updates to the updateContent/updateDataResource services,
 * which check permission (via genericContentPermission/genericDataResourcePermission, main-action
 * UPDATE) against the real target records. These tests exercise exactly that permission boundary
 * for a low-privilege user - one whose only grants are OFBTOOLS_VIEW and CONTENTMGR_VIEW, with no
 * CONTENTMGR admin permission and no role or ownership standing on the target records - the same
 * profile the reported exploit used.
 */
@JunitJupiterTest
class UpdateLayoutPermissionTests implements JupiterTestHelper {

    // Content/DataResource/SecurityGroup ids are "id" fields (VARCHAR(20)); keep these <= 20 chars
    private static final String GROUP_ID = 'TEST_LAYOUT_VIEW'
    private static final String USER_LOGIN_ID = 'testLayoutViewOnlyUser'
    private static final String CONTENT_ID = 'TEST_LAYOUT_RESP_CT'
    private static final String DATA_RESOURCE_ID = 'TEST_LAYOUT_TGT_DR'
    private static final Timestamp GRANT_FROM_DATE = Timestamp.valueOf('2020-01-01 00:00:00.0')

    @Test
    void testViewOnlyUserCannotUpdateContentOrConvertDataResourceToTemplate() {
        GenericValue lowPrivUserLogin = createViewOnlyUserLogin()
        GenericValue content = createTestContent()
        GenericValue dataResource = createTestDataResource()

        // relinking/renaming the real Content record must be denied without UPDATE standing on it;
        // a failed permission-service check surfaces as ServiceAuthException out of runSync, not as
        // a returned error-result map
        assertThrows(ServiceAuthException) {
            dispatcher.runSync('updateContent', [contentId: content.contentId,
                    contentName: 'Hijacked by view-only user', userLogin: lowPrivUserLogin])
        }

        // converting the real DataResource into a template-bearing OFBIZ_FILE resource must be
        // denied without CONTENTMGR_SUPER, regardless of its current (non-template) type
        assertThrows(ServiceAuthException) {
            dispatcher.runSync('updateDataResource', [dataResourceId: dataResource.dataResourceId,
                    dataResourceTypeId: 'OFBIZ_FILE', dataTemplateTypeId: 'FTL',
                    objectInfo: 'runtime/logs/access_log.txt', userLogin: lowPrivUserLogin])
        }
    }

    private GenericValue createViewOnlyUserLogin() {
        GenericValue existing = from('UserLogin').where(userLoginId: USER_LOGIN_ID).queryOne()
        if (existing) {
            return existing
        }

        if (!from('SecurityGroup').where(groupId: GROUP_ID).queryOne()) {
            delegator.create('SecurityGroup', [groupId: GROUP_ID,
                    description: 'View-only group for the updateLayout permission regression test'])
            delegator.create('SecurityGroupPermission', [groupId: GROUP_ID, permissionId: 'OFBTOOLS_VIEW',
                    fromDate: GRANT_FROM_DATE])
            delegator.create('SecurityGroupPermission', [groupId: GROUP_ID, permissionId: 'CONTENTMGR_VIEW',
                    fromDate: GRANT_FROM_DATE])
        }

        GenericValue userLogin = delegator.create('UserLogin', [userLoginId: USER_LOGIN_ID, enabled: 'Y'])
        delegator.create('UserLoginSecurityGroup', [userLoginId: USER_LOGIN_ID, groupId: GROUP_ID,
                fromDate: GRANT_FROM_DATE])
        return userLogin
    }

    private GenericValue createTestContent() {
        GenericValue existing = from('Content').where(contentId: CONTENT_ID).queryOne()
        if (existing) {
            return existing
        }
        return delegator.create('Content', [contentId: CONTENT_ID, contentTypeId: 'DOCUMENT',
                contentName: 'Test Layout Response Content'])
    }

    private GenericValue createTestDataResource() {
        GenericValue existing = from('DataResource').where(dataResourceId: DATA_RESOURCE_ID).queryOne()
        if (existing) {
            return existing
        }
        return delegator.create('DataResource', [dataResourceId: DATA_RESOURCE_ID,
                dataResourceTypeId: 'ELECTRONIC_TEXT', mimeTypeId: 'text/plain'])
    }

}
