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

import static org.mockito.Mockito.any

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.apache.ofbiz.base.config.ConfigHelper
import org.apache.ofbiz.base.config.ConfigurationInterface
import org.apache.ofbiz.base.config.ConfigurationFactory
import org.apache.ofbiz.base.config.XmlFileReader
import org.apache.ofbiz.base.util.UtilXml
import org.apache.ofbiz.service.config.model.ServiceConfigFactory
import org.apache.ofbiz.service.config.model.ServiceConfigGetter
import org.apache.ofbiz.service.config.model.ServiceEngine
import org.junit.After
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.w3c.dom.Document

class BaseServiceConfigReaderTest {

    private static final String XML_HEADER = '''<?xml version="1.0" encoding="UTF-8" ?>
            <service-config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:noNamespaceSchemaLocation="https://ofbiz.apache.org/dtds/service-config.xsd">
            <service-engine name="test">'''
    private static final String XML_FOOTER = '</service-engine></service-config>'
    private MockedStatic<XmlFileReader> mockXmlReader
    private MockedStatic<ConfigHelper> mockHelper
    private MockedStatic<ConfigFactory> mockConfig
    private MockedStatic<ServiceConfigGetter> mockConfigGetter

    @After
    void closeMock() {
        mockXmlReader?.close()
        mockConfigGetter?.close()
        mockHelper?.close()
        mockConfig?.close()
    }

    private static String prepareXmlContent(String xmlContent) {
        return XML_HEADER + xmlContent + XML_FOOTER
    }

    private static Document readXmlContent(String xmlContent) {
        return UtilXml.readXmlDocument(prepareXmlContent(xmlContent), false)
    }

    private void initHoconConfig(String hoconContent) {
        Config mockedConfig = hoconContent
                ? ConfigFactory.parseString(
                """{"serviceengine": {"service-config": {"service-engine": {"test": {${hoconContent}}}}}}""")
                : ConfigFactory.empty()
        mockConfig = Mockito.mockStatic(ConfigFactory)
        mockConfig.when(ConfigFactory.load((String) any()))
                .thenReturn(mockedConfig)
        mockConfig.when(ConfigFactory::load)
                .thenReturn(mockedConfig)
        mockConfig.when(ConfigFactory::empty)
                .thenReturn(mockedConfig)
    }

    private void initXmlConfig(String xmlContent) {
        mockHelper = Mockito.mockStatic(ConfigHelper)
        mockHelper.when(ConfigHelper::checkStrictXmlStructure)
                .thenReturn(Boolean.FALSE)
        Document xmlDoc = readXmlContent(xmlContent)
        mockXmlReader = Mockito.mockStatic(XmlFileReader)
        mockXmlReader.when(XmlFileReader.read(any()))
                .thenReturn(xmlDoc.getDocumentElement())
    }

    protected ServiceEngine getConfig(String xmlContent, String hoconContent, boolean override = true) {
        initXmlConfig(xmlContent)
        initHoconConfig(hoconContent)
        ConfigurationInterface configuration = ConfigurationFactory.resetAndGet() // creation of configuration is enough
        configuration.clearCache()
        configuration.setUseOverrideValue(override)

        mockConfigGetter = Mockito.mockStatic(ServiceConfigGetter)
        mockConfigGetter.when(ServiceConfigGetter::getInstance)
                .thenReturn(new ServiceConfigGetter())
        return ServiceConfigFactory.createInstance().getServiceEngine('test')
    }

}
