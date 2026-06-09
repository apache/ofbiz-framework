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

class ConfigReaderEntityGroupReaderTest extends BaseEntityConfigReaderTest {

    @Test
    void testLoadEntityGroupReaderFromXml() {
        EntityConfig config = mockXmlAndHoconCgf('''
             <entity-group-reader name="test1"
                loader="loader-test1"
                location="location-test1"/>
             <entity-group-reader name="test2"
                loader="loader-test2"
                location="location-test2"/>''', '')

        assert config.getEntityGroupReaderList().size() == 2
        config.getEntityGroupReader('test1')?.with {
            assert getLocation() == 'location-test1'
            assert getLoader() == 'loader-test1'
        }
        config.getEntityGroupReader('test2')?.with {
            assert getLocation() == 'location-test2'
            assert getLoader() == 'loader-test2'
        }
    }

    @Test
    void testLoadEntityGroupReaderFromOverrideConfig() {
        EntityConfig config = mockXmlAndHoconCgf('''
             <entity-group-reader name="test1"
                loader="loader-test1"
                location="location-test1"/>
             <entity-group-reader name="test2"
                loader="loader-test2"
                location="location-test2"/>''',
                '''"entity-group-reader": {
                "test1": {"loader": "loader-over1",
                           "location": "location-over1"},
                "test3": {"loader": "loader-over3",
                           "location": "location-over3"}}''')

        assert config.getEntityGroupReaderList().size() == 3
        assert config.getEntityGroupReader('test1')
        assert config.getEntityGroupReader('test2')
        assert config.getEntityGroupReader('test3')
        config.getEntityGroupReader('test1')?.with {
            assert getLocation() == 'location-over1'
            assert getLoader() == 'loader-over1'
        }
        config.getEntityGroupReader('test2')?.with {
            assert getLocation() == 'location-test2'
            assert getLoader() == 'loader-test2'
        }
        config.getEntityGroupReader('test3')?.with {
            assert getLocation() == 'location-over3'
            assert getLoader() == 'loader-over3'
        }
    }

}
