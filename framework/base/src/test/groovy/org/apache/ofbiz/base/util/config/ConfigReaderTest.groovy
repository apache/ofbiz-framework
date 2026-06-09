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

import static org.mockito.Mockito.any

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigParseOptions
import org.apache.ofbiz.base.config.ConfigurationInterface
import org.apache.ofbiz.base.config.ConfigurationFactory
import org.apache.ofbiz.base.config.TypesafeConfigImplReader
import org.apache.ofbiz.base.util.UtilProperties
import org.junit.jupiter.api.AfterEach
import org.mockito.MockedStatic
import org.mockito.Mockito

class ConfigReaderTest {

    private MockedStatic<ConfigFactory> mockConfig
    private MockedStatic<UtilProperties> mockProperties

    @AfterEach
    void closeMock() {
        mockConfig?.close()
        mockProperties?.close()
    }

    private void initHoconConfig(String hoconContent) {
        Config mockedConfig = hoconContent
                ? ConfigFactory.parseString(hoconContent)
                : ConfigFactory.empty()
        mockConfig = Mockito.mockStatic(ConfigFactory)
        mockConfig.when(ConfigFactory.load((String) any()))
                .thenReturn(mockedConfig)
        mockConfig.when(ConfigFactory.parseFile((File) any()))
                .thenReturn(mockedConfig)
        mockConfig.when(ConfigFactory.parseFile((File) any(), (ConfigParseOptions) any()))
                .thenReturn(mockedConfig)
        mockConfig.when(ConfigFactory::load)
                .thenReturn(mockedConfig)
        mockConfig.when(ConfigFactory::empty())
                .thenReturn(mockedConfig)
    }

    protected ConfigurationInterface setConfig(String hoconContent) {
        initHoconConfig(hoconContent)
        ConfigurationInterface configuration = ConfigurationFactory.resetAndGet()
        configuration.clearCache()
        return configuration
    }

    protected TypesafeConfigImplReader initReader(String hoconContent) {
        setConfig(hoconContent)
        return new TypesafeConfigImplReader()
    }

    protected TypesafeConfigImplReader initReaderAndProperties(String hoconContent, String propFileName = null,
                                                               String propFileContent = null) {
        mockProperties = Mockito.mockStatic(UtilProperties, Mockito.CALLS_REAL_METHODS)
        if (propFileName && propFileContent) {
            Properties props = new Properties()
            props.load(new ByteArrayInputStream(propFileContent.getBytes()))
            mockProperties.when(() -> UtilProperties.getProperties(propFileName)).thenReturn(props)
        }
        return initReader(hoconContent)
    }

}
