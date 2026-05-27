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
package org.apache.ofbiz.entity.model.test

import org.apache.ofbiz.entity.config.model.DelegatorElement
import org.apache.ofbiz.entity.config.model.EntityConfig
import org.apache.ofbiz.entity.config.model.GroupMap
import org.junit.Test

class ConfigReaderDelegatorElementTest extends BaseEntityConfigReaderTest {

    @Test
    void testLoadDelegatorElementFromXml() {
        EntityConfig config = mockXmlAndHoconCgf('''<delegator
        name="delegator-test"
        default-group-name="default-group-name-test"
        distributed-cache-clear-class-name="distributed-cache-clear-class-name-test"
        distributed-cache-clear-enabled="true"
        distributed-cache-clear-user-login-id="distributed-cache-clear-user-login-id-test"
        entity-eca-enabled="true"
        entity-eca-handler-class-name="entity-eca-handler-class-name-test"
        entity-eca-reader="entity-eca-reader-test"
        entity-group-reader="entity-group-reader-test"
        entity-model-reader="entity-model-reader-test"
        key-encrypting-key="key-encrypting-key-test"
        sequenced-id-prefix="sequenced-id-prefix-test"
                 />''', '')
        assert config.getDelegator('delegator-test')
        config.getDelegator('delegator-test').with {
            assert getName() == 'delegator-test'
            assert getDefaultGroupName() == 'default-group-name-test'
            assert getDistributedCacheClearClassName() == 'distributed-cache-clear-class-name-test'
            assert getDistributedCacheClearEnabled()
            assert getDistributedCacheClearUserLoginId() == 'distributed-cache-clear-user-login-id-test'
            assert getEntityEcaEnabled()
            assert getEntityEcaHandlerClassName() == 'entity-eca-handler-class-name-test'
            assert getEntityEcaReader() == 'entity-eca-reader-test'
            assert getKeyEncryptingKey() == 'key-encrypting-key-test'
            assert getSequencedIdPrefix() == 'sequenced-id-prefix-test'
        }
    }

    @Test
    void testLoadDelegatorElementFromOverrideConfig() {
        EntityConfig config = mockXmlAndHoconCgf('''<delegator
        name="empty"
        default-group-name="empty"
        distributed-cache-clear-class-name="empty"
        distributed-cache-clear-enabled="false"
        distributed-cache-clear-user-login-id="empty"
        entity-eca-enabled="false"
        entity-eca-handler-class-name="empty"
        entity-eca-reader="empty"
        entity-group-reader="empty"
        entity-model-reader="empty"
        key-encrypting-key="empty"
        sequenced-id-prefix="empty"
                 />''', '''"delegator": {
            "delegator-over": {
            "default-group-name": "default-group-name-over"
            "distributed-cache-clear-class-name": "distributed-cache-clear-class-name-over"
            "distributed-cache-clear-enabled": "true"
            "distributed-cache-clear-user-login-id": "distributed-cache-clear-user-login-id-over"
            "entity-eca-enabled": "true"
            "entity-eca-handler-class-name": "entity-eca-handler-class-name-over"
            "entity-eca-reader": "entity-eca-reader-over"
            "entity-group-reader": "entity-group-reader-over"
            "entity-model-reader": "entity-model-reader-over"
            "key-encrypting-key": "key-encrypting-key-over"
            "sequenced-id-prefix": "sequenced-id-prefix-over"
    }}''')
        assert config.getDelegator('delegator-over')
        config.getDelegator('delegator-over').with {
            assert getName() == 'delegator-over'
            assert getDefaultGroupName() == 'default-group-name-over'
            assert getDistributedCacheClearClassName() == 'distributed-cache-clear-class-name-over'
            assert getDistributedCacheClearEnabled()
            assert getDistributedCacheClearUserLoginId() == 'distributed-cache-clear-user-login-id-over'
            assert getEntityEcaEnabled()
            assert getEntityEcaHandlerClassName() == 'entity-eca-handler-class-name-over'
            assert getEntityEcaReader() == 'entity-eca-reader-over'
            assert getKeyEncryptingKey() == 'key-encrypting-key-over'
            assert getSequencedIdPrefix() == 'sequenced-id-prefix-over'
        }
    }

    @Test
    void testLoadDelegatorGroupMapFromXmlConfig() {
        EntityConfig config = mockXmlAndHoconCgf('''<delegator name="delegator-test-group-map-xml">
            <group-map group-name="group-name-test1" datasource-name="datasource-name-test"/>
            <group-map group-name="group-name-test2" datasource-name="datasource-name-test"/>
        </delegator>''', '')
        assert config.getDelegator('delegator-test-group-map-xml')
        List<GroupMap> groupMapList = config.getDelegator('delegator-test-group-map-xml').getGroupMapList()
        assert groupMapList.size() == 2
        groupMapList[0].with {
            assert getGroupName() == 'group-name-test1'
            assert getDatasourceName() == 'datasource-name-test'
        }
        groupMapList[1].with {
            assert getGroupName() == 'group-name-test2'
            assert getDatasourceName() == 'datasource-name-test'
        }
    }

    @Test
    void testLoadDelegatorGroupMapFromOverrideConfig() {
        EntityConfig config = mockXmlAndHoconCgf('''<delegator name="delegator-test-group-map-over">
            <group-map group-name="group-name-test1" datasource-name="datasource-name-test"/>
            <group-map group-name="group-name-test2" datasource-name="datasource-name-test"/>
        </delegator>''', '''"delegator": {
            "delegator-test-group-map-over": {
                "group-map": {
                    "group-name-test1": {
                        "datasource-name": "datasource-name-over"
                    },
                    "group-name-test3": {
                        "datasource-name": "datasource-name-over"
                    }
                }
            }}
            ''')
        DelegatorElement delegatorElement = config.getDelegator('delegator-test-group-map-over')
        assert delegatorElement
        List<GroupMap> groupMapList = delegatorElement.getGroupMapList()
        assert groupMapList.size() == 3
        assert groupMapList.find { it.getGroupName() == 'group-name-test1' }
                ?.getDatasourceName() == 'datasource-name-over'
        assert groupMapList.find { it.getGroupName() == 'group-name-test2' }
                ?.getDatasourceName() == 'datasource-name-test'
        assert groupMapList.find { it.getGroupName() == 'group-name-test3' }
                ?.getDatasourceName() == 'datasource-name-over'

        assert delegatorElement.getGroupDataSource('group-name-test1')?.getDatasourceName()
                == 'datasource-name-over'
        assert delegatorElement.getGroupDataSource('group-name-test2')?.getDatasourceName()
                == 'datasource-name-test'
        assert delegatorElement.getGroupDataSource('group-name-test3')?.getDatasourceName()
                == 'datasource-name-over'
    }

}
