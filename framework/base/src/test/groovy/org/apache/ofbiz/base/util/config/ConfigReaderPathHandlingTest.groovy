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
package org.apache.ofbiz.base.util.config

import static org.apache.ofbiz.base.config.TypesafeConfigImplReader.convertToConfigPath

import org.apache.ofbiz.base.config.TypesafeConfigImplReader
import org.junit.jupiter.api.Test

class ConfigReaderPathHandlingTest extends ConfigReaderTest {

    private TypesafeConfigImplReader confReader

    @Test
    void testXPathConversionToHoconConfigWithSimpleCase() {
        assert convertToConfigPath('/test-config/path-test/@value') == 'test-config.path-test.value'
        assert convertToConfigPath('/test-config/path-test/value') == 'test-config.path-test.value'
    }

    @Test
    void testXPathConversionToHoconConfigWithAttributeName() {
        assert convertToConfigPath('/test-config/path-test[@name=node-test]/path-node-test/@value')
                == 'test-config.path-test."node-test".path-node-test.value'
        assert convertToConfigPath('/test-config/path-test[@name=node-test]/path-node-test/value')
                == 'test-config.path-test."node-test".path-node-test.value'
        assert convertToConfigPath('/test-config/path-test[@name=\'node-test\']/path-node-test/@value')
                == 'test-config.path-test."node-test".path-node-test.value'
        assert convertToConfigPath('/test-config/path-test[@name=\"node-test\"]/path-node-test/@value')
                == 'test-config.path-test."node-test".path-node-test.value'
    }

    @Test
    void testXPathConversionToHoconConfigWithAttributeNameAndDot() {
        assert convertToConfigPath('/test-config/path-test[@name=node.test]/path-node-test/@value')
                == 'test-config.path-test."node.test".path-node-test.value'
    }

    @Test
    void testXPathConversionToHoconConfigWithBadAttributeName() {
        assert convertToConfigPath('/test-config/path-test[@badKey=\"node-test\"]/path-node-test/@value')
                == null
    }

    @Test
    void testXPathConversionToHoconConfigWithListElement() {
        assert convertToConfigPath('/test-config/path-test[1]/@name') == 'test-config.path-test.0.name'
    }

    @Test
    void testXPathConversionToHoconConfigWithTwoListElement() {
        assert convertToConfigPath('/test-config/path-test[3]/node[4]/@name') == 'test-config.path-test.2.node.3.name'
    }

    @Test
    void testPropertiesToHoconConfig() {
        assert convertToConfigPath('test.value') == 'test.value'
    }

    @Test
    void testGetSimpleValueFromHocon() {
        confReader = initReader('''{"test": {"simple-test": {"name": "value-test"}}}''')
        assert confReader.getValue('test',
                '/simple-test/@name') == 'value-test'
    }

    @Test
    void testGetNullValueFromHocon() {
        confReader = initReader('''{"test": {"null-test": {"name": "value-test"}}}''')
        assert confReader.getValue('test',
                '/null-test/@description') == null
    }

    /* FIXME
    @Test
    void testGetIntegerValueFromHocon() {
        confReader = initReader('''{"test": {"integer-test": {"name": "1"}}}''')
        assert confReader.getValue('test',
                '/integer-test/@name', Integer.class) == 1
    }*/

    @Test
    void testGetValueFromMapListFromHocon() {
        confReader = initReader('''{"test": {"list-test": {"test1": {"description": "desc1"},
                                                                       "test2": {"description": "desc2"}}}}''')
        assert confReader.getValue('test',
                '/list-test[@name=test1]/description') == 'desc1'
    /* FIXME
        assert confReader.getValue('test',
                '/list-test[1]/description') == 'desc1'
                */
    }

    /* FIXME
    @Test
    void testGetValueFromListMap() {
        confReader = initReader('''{"test": {"list-test": [{"name": "test1", "description": "desc1"},
                                                                       {"name": "test2", "description": "desc2"}]}}''')
        assert confReader.getValue('test',
                '/list-test[@name=test1]/description') == 'desc1'
    }*/

    @Test
    void testGetIncorrectKeyFromListFromHocon() {
        confReader = initReader('''{"test": {"list-test": {"test1": {"description": "desc1"},
                                                                       "test2": {"description": "desc2"}}}}''')
        assert confReader.getValue('test',
                '/list-test[@name=test3]/description') == null
        assert confReader.getValue('test',
                '/list-test[@name=test1]/missing') == null
    }

}
