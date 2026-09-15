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

class ConfigReaderFieldTypeTest extends BaseEntityConfigReaderTest {

    @Test
    void testLoadFieldTypeFromXml() {
        EntityConfig config = mockXmlAndHoconCgf('''
             <field-type name="test1"
                loader="loader-test1"
                location="location-test1"/>
             <field-type name="test2"
                loader="loader-test2"
                location="location-test2"/>''', '')

        assert config.getFieldTypeList().size() == 2
        config.getFieldType('test1')?.with {
            assert getLocation() == 'location-test1'
            assert getLoader() == 'loader-test1'
        }
        config.getFieldType('test2')?.with {
            assert getLocation() == 'location-test2'
            assert getLoader() == 'loader-test2'
        }
    }

    @Test
    void testLoadFieldTypeFromOverrideConfig() {
        EntityConfig config = mockXmlAndHoconCgf('''
             <field-type name="test1"
                loader="loader-test1"
                location="location-test1"/>
             <field-type name="test2"
                loader="loader-test2"
                location="location-test2"/>''',
                '''"field-type": {
                "test1": {"loader": "loader-over1",
                           "location": "location-over1"},
                "test3": {"loader": "loader-over3",
                           "location": "location-over3"}}''')

        assert config.getFieldTypeList().size() == 3
        assert config.getFieldType('test1')
        assert config.getFieldType('test2')
        assert config.getFieldType('test3')
        config.getFieldType('test1')?.with {
            assert getLocation() == 'location-over1'
            assert getLoader() == 'loader-over1'
        }
        config.getFieldType('test2')?.with {
            assert getLocation() == 'location-test2'
            assert getLoader() == 'loader-test2'
        }
        config.getFieldType('test3')?.with {
            assert getLocation() == 'location-over3'
            assert getLoader() == 'loader-over3'
        }
    }

}
