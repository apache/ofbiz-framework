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

import org.apache.ofbiz.entity.config.model.ConnectionFactory
import org.apache.ofbiz.entity.config.model.EntityConfig
import org.junit.Test

class ConfigReaderConnectionFactoryTest extends BaseEntityConfigReaderTest {

    @Test
    void testLoadConnectionFactoryClassNameFromXml() {
        EntityConfig config = mockXmlAndHoconCgf('<connection-factory class="org.apache.ofbiz.test.test1"/>', '')
        ConnectionFactory connectionFactory = config.getConnectionFactory()

        assert connectionFactory.getClassName() == 'org.apache.ofbiz.test.test1'
    }

    @Test
    void testLoadConnectionFactoryClassNameFromOverrideConfig() {
        EntityConfig config = mockXmlAndHoconCgf('<connection-factory class="org.apache.ofbiz.test.test2"/>',
                '''"connection-factory": {"class": "org.apache.ofbiz.test.override"}''')
        ConnectionFactory connectionFactory = config.getConnectionFactory()

        assert connectionFactory.getClassName() == 'org.apache.ofbiz.test.override'
    }

    @Test
    void testLoadConnectionFactoryClassNameWithOverrideConfigNoneUsed() {
        EntityConfig config = mockXmlAndHoconCgf('<connection-factory class="org.apache.ofbiz.test.test3"/>',
                '''"connection-factory": {"class": "org.apache.ofbiz.test.overridenoneread"}'''
                , false)
        ConnectionFactory connectionFactory = config.getConnectionFactory()

        assert connectionFactory.getClassName() == 'org.apache.ofbiz.test.test3'
    }

}
