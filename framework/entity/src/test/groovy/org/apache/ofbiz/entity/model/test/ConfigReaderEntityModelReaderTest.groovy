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
import org.junit.Test

class ConfigReaderEntityModelReaderTest extends BaseEntityConfigReaderTest {

    @Test
    void testLoadEntityModelReaderFromXml() {
        EntityConfig config = mockXmlAndHoconCgf('<entity-model-reader name="test1"/><entity-model-reader name="test2"/>', '')

        assert config.getEntityModelReaderList().size() == 2
        assert config.getEntityModelReader('test1')
        assert config.getEntityModelReader('test2')
    }

    @Test
    void testLoadEntityModelReaderFromOverrideConfig() {
        EntityConfig config = mockXmlAndHoconCgf('<entity-model-reader name="test1"/><entity-model-reader name="test2"/>',
                '"entity-model-reader": [ {"name": "test3"} ]')

        assert config.getEntityModelReaderList().size() == 3
        assert config.getEntityModelReader('test1')
        assert config.getEntityModelReader('test2')
        assert config.getEntityModelReader('test3')
    }

}
