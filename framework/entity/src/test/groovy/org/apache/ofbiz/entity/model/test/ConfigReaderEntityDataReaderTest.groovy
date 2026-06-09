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

import org.apache.ofbiz.entity.config.model.EntityConfig
import org.junit.jupiter.api.Test

class ConfigReaderEntityDataReaderTest extends BaseEntityConfigReaderTest {

    @Test
    void testLoadEntityDataReaderFromXml() {
        EntityConfig config = mockXmlAndHoconCgf('''
             <entity-data-reader name="test1"/>
             <entity-data-reader name="test2"/>''', '')

        assert config.getEntityDataReaderList().size() == 2
        config.getEntityDataReader('test1')
        config.getEntityDataReader('test2')
    }

    @Test
    void testLoadEntityDataReaderFromOverrideConfig() {
        EntityConfig config = mockXmlAndHoconCgf('''
             <entity-data-reader name="test1"/>
             <entity-data-reader name="test2"/>''',
                '''entity-data-reader: [{"name": "test1"}, {"name": "test3"}]''')

        assert config.getEntityDataReaderList().size() == 3
        config.getEntityDataReader('test1')
        config.getEntityDataReader('test2')
        config.getEntityDataReader('test3')
    }

}

