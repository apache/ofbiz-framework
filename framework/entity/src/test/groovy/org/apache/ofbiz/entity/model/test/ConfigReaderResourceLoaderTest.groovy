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
import org.apache.ofbiz.entity.config.model.ResourceLoader
import org.junit.Test

class ConfigReaderResourceLoaderTest extends BaseEntityConfigReaderTest {

    @Test
    void testLoadResourceLoaderFromXml() {
        EntityConfig config = mockXmlAndHoconCgf('''<resource-loader name="name-test" class="class-test"
            prepend-env="prepend-env-test" prefix="prefix-test"/>'''
            , '')
        ResourceLoader resourceLoader = config.getResourceLoader('name-test')

        assert resourceLoader
        resourceLoader.with {
            assert getName() == 'name-test'
            assert getClassName() == 'class-test'
            assert getPrependEnv() == 'prepend-env-test'
            assert getPrefix() == 'prefix-test'
        }
    }

    @Test
    void testLoadResourceLoaderFromOverrideConfig() {
        EntityConfig config = mockXmlAndHoconCgf('''<resource-loader name="name-test" class="class-test"
            prepend-env="prepend-env-test" prefix="prefix-test"/>''',
                '''"resource-loader": {
                                 "name-over" : {
                                     "class": "class-over",
                                     "prepend-env": "prepend-env-over",
                                     "prefix": "prefix-over"
                                 }
                             } ''')
        ResourceLoader resourceLoader = config.getResourceLoader('name-over')

        assert resourceLoader
        resourceLoader.with {
            assert getName() == 'name-over'
            assert getClassName() == 'class-over'
            assert getPrependEnv() == 'prepend-env-over'
            assert getPrefix() == 'prefix-over'
        }
    }

}
