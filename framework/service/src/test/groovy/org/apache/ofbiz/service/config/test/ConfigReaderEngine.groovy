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
package org.apache.ofbiz.service.config.test

import org.apache.ofbiz.service.config.model.ServiceEngine
import org.junit.Test

class ConfigReaderEngine extends BaseServiceConfigReaderTest {

    @Test
    void testEngineFromXml() {
        ServiceEngine engine = getConfig('''
        <engine name="engine-test" class="org.apache.ofbiz.service.engine.EntityTest"/>''', '')
        assert engine.getEngines().size() == 1
        assert engine.getEngine('engine-test')?.getClassName() == 'org.apache.ofbiz.service.engine.EntityTest'
    }

    @Test
    void testEngineWithOverride() {
        ServiceEngine engine = getConfig('''
        <engine name="engine-test" class="org.apache.ofbiz.service.engine.EntityTest"/>''',
                '''"engine": {
            "engine-test" : { "class": "org.apache.ofbiz.service.engine.EntityTestOver" }
        }''')
        assert engine.getEngines().size() == 1
        assert engine.getEngine('engine-test')?.getClassName() == 'org.apache.ofbiz.service.engine.EntityTestOver'
    }

    @Test
    void testEngineWithAddByOverride() {
        ServiceEngine engine = getConfig('''
        <engine name="engine-test" class="org.apache.ofbiz.service.engine.EntityTest"/>''',
                '''"engine": {
            "engine-test-add" : { "class": "org.apache.ofbiz.service.engine.EntityTestOver" }
        }''')
        assert engine.getEngines().size() == 2
        assert engine.getEngine('engine-test')?.getClassName() == 'org.apache.ofbiz.service.engine.EntityTest'
        assert engine.getEngine('engine-test-add')?.getClassName() == 'org.apache.ofbiz.service.engine.EntityTestOver'
    }

}
