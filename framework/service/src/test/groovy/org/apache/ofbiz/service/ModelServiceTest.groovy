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
package org.apache.ofbiz.service

import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.eq

import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.base.util.UtilURL;
import org.apache.ofbiz.base.util.UtilXml
import org.apache.ofbiz.base.util.cache.UtilCache
import org.junit.Assert
import org.junit.Test
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.w3c.dom.Element

class ModelServiceTest {

    private static final String SERVICE_CACHE_NAME = 'service.ModelServiceMapByModel'
    private static final UtilCache<String, Map<String, ModelService>> MODEL_SERVICE_MAP_BY_MODEL =
            UtilCache.createUtilCache(SERVICE_CACHE_NAME, 0, 0, false)
    private MockedStatic<UtilProperties> utilities

    @BeforeEach
    void initMock() {
        utilities = Mockito.mockStatic(UtilProperties)
        utilities.when(UtilProperties.getMessage(eq(ModelService.RESOURCE), any(), any())).thenReturn('Failed')
        utilities.when(UtilProperties.createProperties(eq('debug.properties'))).thenReturn(new Properties())
    }

    @AfterEach
    void closeMock() {
        utilities.close()
        UtilCache.clearAllCaches()
    }

    @Test
    void callValidateServiceWithOneSingleRequiredParam() {
        String serviceXml = '''<service name="testParam" engine="java"
               location="org.apache.ofbiz.common.CommonServices" invoke="ping">
               <attribute name="message" type="String" mode="IN"/>
           </service>'''
        try {
            createModelService(serviceXml)
                    .validate([message: 'ok'],
                            'IN', Locale.default)
        } catch (ServiceValidationException ignored) {
            Assert.fail('Required parameters not validated')
        }
    }

    @Test
    void callValidateServiceWithOneSingleOptionalParam() {
        String serviceXml = '''<service name="testParam" engine="java"
               location="org.apache.ofbiz.common.CommonServices" invoke="ping">
               <attribute name="message" type="String" mode="IN" optional="true"/>
           </service>'''
        try {
            createModelService(serviceXml)
                    .validate([message: 'ok'],
                            'IN', Locale.default)
        } catch (ServiceValidationException ignored) {
            Assert.fail('Optional parameter not validated')
        }
    }

    @Test
    void callValidateServiceWithTowParamWithSameName() {
        String serviceXml = '''<service name="testParam" engine="java"
               location="org.apache.ofbiz.common.CommonServices" invoke="ping">
               <attribute name="message" type="String" mode="IN"/>
               <attribute name="message" type="String" mode="IN"/>
           </service>'''
        try {
            createModelService(serviceXml)
                    .validate([message: 'ok'],
                            'IN', Locale.default)
        } catch (ServiceValidationException ignored) {
            Assert.fail('Optional parameter not validated')
        }
    }

    @Test(expected = ServiceValidationException)
    void callValidateServiceWithNullRequiredParam() {
        String serviceXml = '''<service name="testParam" engine="java"
               location="org.apache.ofbiz.common.CommonServices" invoke="ping">
               <attribute name="message" type="String" mode="IN"/>
           </service>'''
        createModelService(serviceXml)
                .validate([message: null],
                        'IN', Locale.default)
    }

    @Test
    void callValidateServiceWithNullOptionalParam() {
        String serviceXml = '''<service name="testParam" engine="java"
               location="org.apache.ofbiz.common.CommonServices" invoke="ping">
               <attribute name="message" type="String" mode="IN" optional="true"/>
           </service>'''
        try {
            createModelService(serviceXml)
                    .validate([message: null],
                            'IN', Locale.default)
        } catch (ServiceValidationException ignored) {
            Assert.fail('Optional parameter not validated')
        }
    }

    @Test(expected = ServiceValidationException)
    void callValidateServiceWithOneSingleRequiredParamMissing() {
        String serviceXml = '''<service name="testParam" engine="java"
               location="org.apache.ofbiz.common.CommonServices" invoke="ping">
               <attribute name="message" type="String" mode="IN"/>
           </service>'''
        createModelService(serviceXml)
                .validate([missing: 'ok'],
                        'IN', Locale.default)
    }

    @Test
    void callValidateServiceWithOneComplexParameterAllRequired() {
        String serviceXml = '''<service name="testParam" engine="java"
               location="org.apache.ofbiz.common.CommonServices" invoke="ping">
               <attribute name="header" type="java.util.Map" mode="IN" optional="false">
                   <attribute name="headerParam" type="String" mode="IN" optional="false"/>
               </attribute>
           </service>'''
        try {
            createModelService(serviceXml)
                    .validate([header: [headerParam: 'foo']],
                            'IN', Locale.default)
        } catch (ServiceValidationException ignored) {
            Assert.fail('Paramètre complexe non identifié')
        }
    }

    @Test(expected = ServiceValidationException)
    void callValidateServiceWithOneComplexParameterAllRequiredEmbeddedMissing() {
        String serviceXml = '''<service name="testParam" engine="java"
               location="org.apache.ofbiz.common.CommonServices" invoke="ping">
               <attribute name="header" type="java.util.Map" mode="IN" optional="false">
                   <attribute name="headerParam" type="String" mode="IN" optional="false"/>
                   <attribute name="otherParam" type="String" mode="IN" optional="false"/>
               </attribute>
           </service>'''
        createModelService(serviceXml)
                .validate([header: [headerParam: 'foo']],
                        'IN', Locale.default)
    }

    @Test
    void callValidateServiceWithOneComplexParameterOnlyOneRequiredEmbeddedMissing() {
        String serviceXml = '''<service name="testParam" engine="java"
               location="org.apache.ofbiz.common.CommonServices" invoke="ping">
               <attribute name="header" type="java.util.Map" mode="IN" optional="false">
                   <attribute name="headerParam" type="String" mode="IN" optional="false"/>
                   <attribute name="otherParam" type="String" mode="IN" optional="true"/>
               </attribute>
           </service>'''
        try {
            createModelService(serviceXml)
                    .validate([header: [headerParam: 'foo']],
                            'IN', Locale.default)
        } catch (ServiceValidationException ignored) {
            Assert.fail('Missing optional should not throw exception')
        }
    }

    @Test
    void callValidateServiceWithOneComplexParameterOnlyOneRequiredAndOneOptionalEmbedded() {
        String serviceXml = '''<service name="testParam" engine="java"
               location="org.apache.ofbiz.common.CommonServices" invoke="ping">
               <attribute name="header" type="java.util.Map" mode="IN" optional="false">
                   <attribute name="headerParam" type="String" mode="IN" optional="false"/>
                   <attribute name="otherParam" type="String" mode="IN" optional="true"/>
               </attribute>
           </service>'''
        try {
            createModelService(serviceXml)
                    .validate([header: [headerParam: 'foo', otherParam: 'Good']],
                            'IN', Locale.default)
        } catch (ServiceValidationException ignored) {
            Assert.fail('Complex parameter control error')
        }
    }

    @Test(expected = ServiceValidationException)
    void callValidateServiceWithOneComplexParameterAndUnexpectedEmbeededParam() {
        String serviceXml = '''<service name="testParam" engine="java"
               location="org.apache.ofbiz.common.CommonServices" invoke="ping">
               <attribute name="header" type="java.util.Map" mode="IN" optional="false">
                   <attribute name="headerParam" type="String" mode="IN" optional="false"/>
                   <attribute name="otherParam" type="String" mode="IN" optional="true"/>
               </attribute>
           </service>'''
        createModelService(serviceXml)
                .validate([header: [headerParam: 'foo', otherParam: 'Good', unexpectedParam: 'Bad']],
                        'IN', Locale.default)
    }

    @Test(expected = ServiceValidationException)
    void callValidateServiceWithOneComplexParameterAndBadListValue() {
        String serviceXml = '''<service name="testParam" engine="java"
               location="org.apache.ofbiz.common.CommonServices" invoke="ping">
               <attribute name="header" type="java.util.Map" mode="IN" optional="false">
                   <attribute name="headerParam" type="String" mode="IN" optional="false"/>
                   <attribute name="otherParam" type="String" mode="IN" optional="true"/>
               </attribute>
           </service>'''
        createModelService(serviceXml)
                .validate([header: ['headerParam', 'otherParam']],
                        'IN', Locale.default)
    }

    @Test
    void callValidateServiceWithTwoComplexLevelParameter() {
        String serviceXml = '''<service name="testParam" engine="java"
               location="org.apache.ofbiz.common.CommonServices" invoke="ping">
               <attribute name="header" type="java.util.Map" mode="IN" optional="false">
                   <attribute name="headerParam" type="java.util.Map" mode="IN" optional="false">
                        <attribute name="subHeaderParam" type="String" mode="IN" optional="false"/>
                   </attribute>
                   <attribute name="otherParam" type="String" mode="IN" optional="true"/>
               </attribute>
           </service>'''
        try {
            createModelService(serviceXml)
                    .validate([header: [headerParam: [subHeaderParam: 'true'],
                                        otherParam: 'true']],
                            'IN', Locale.default)
        } catch (ServiceValidationException ignored) {
            Assert.fail('Paramètre complexe non identifié')
        }
    }

    @Test(expected = ServiceValidationException)
    void callValidateServiceWithTwoComplexLevelParameterUnwantedParameter() {
        String serviceXml = '''<service name="testParam" engine="java"
               location="org.apache.ofbiz.common.CommonServices" invoke="ping">
               <attribute name="header" type="java.util.Map" mode="IN" optional="false">
                   <attribute name="headerParam" type="java.util.Map" mode="IN" optional="false">
                        <attribute name="subHeaderParam" type="String" mode="IN" optional="false"/>
                   </attribute>
                   <attribute name="otherParam" type="String" mode="IN" optional="true"/>
               </attribute>
           </service>'''
        createModelService(serviceXml)
                .validate([header: [headerParam: [subHeaderParam: 'true', otherParam: 'false'],
                                    otherParam: 'true']],
                        'IN', Locale.default)
    }

    @Test
    void callValidateServiceWithoutAnalyzeEntryMap() {
        String serviceXml = '''<service name="testParam" engine="java"
               location="org.apache.ofbiz.common.CommonServices" invoke="ping">
               <attribute name="header" type="java.util.Map" mode="IN" optional="false"/>
           </service>'''
        try {
            createModelService(serviceXml)
                    .validate([header: [headerParam: [subHeaderParam: 'true', otherParam: 'false'],
                                        otherParam: 'true']],
                            'IN', Locale.default)
        } catch (ServiceValidationException ignored) {
            Assert.fail('Map should not have been analyzed')
        }
    }

    @Test
    void callValidateServiceWithOneComplexParameterAsList() {
        String serviceXml = '''<service name="testParam" engine="java"
               location="org.apache.ofbiz.common.CommonServices" invoke="ping">
               <attribute name="header" type="java.util.List" mode="IN" optional="false">
                   <attribute name="headerParam" type="String" mode="IN" optional="false"/>
                   <attribute name="otherParam" type="String" mode="IN" optional="true"/>
               </attribute>
           </service>'''
        try {
            createModelService(serviceXml)
                    .validate([header: [[headerParam: 'line1', otherParam: 'Good'],
                                        [headerParam: 'line2', otherParam: 'Good']]],
                            'IN', Locale.default)
        } catch (ServiceValidationException ignored) {
            Assert.fail('Complex List Parameter Error')
        }
    }

    @Test(expected = ServiceValidationException)
    void callValidateServiceWithOneComplexParameterAsListAndUnwantedParameter() {
        String serviceXml = '''<service name="testParam" engine="java"
               location="org.apache.ofbiz.common.CommonServices" invoke="ping">
               <attribute name="header" type="java.util.List" mode="IN" optional="false">
                   <attribute name="headerParam" type="String" mode="IN" optional="false"/>
                   <attribute name="otherParam" type="String" mode="IN" optional="true"/>
               </attribute>
           </service>'''
        createModelService(serviceXml)
                .validate([header: [[headerParam: 'line1', otherParam: 'Good'],
                                    [headerParam: 'line2', otherParam: 'Good',
                                     unwanted: 'Bad']]],
                        'IN', Locale.default)
    }

    @Test
    void callValidateServiceWitImplementParameter() {
        ModelServiceReader reader = new ModelServiceReader(true, UtilURL.fromUrlString('http://ofbiz.apache.org'), null, null)
        String serviceXml1 = '''
           <service name="toImplement" engine="java"
               location="org.apache.ofbiz.common.CommonServices" invoke="ping">
               <attribute name="header" type="java.util.Map" mode="IN" optional="false">
                   <attribute name="headerParam" type="String" mode="IN" optional="false"/>
                   <attribute name="otherParam" type="String" mode="IN" optional="true"/>
               </attribute>
           </service>'''
        Element servicesElement2 = UtilXml.readXmlDocument(serviceXml1, false).getDocumentElement()
        String serviceXml2 = '''
           <service name="testParam" engine="java"
               location="org.apache.ofbiz.common.CommonServices" invoke="ping">
               <implements service="toImplement"/>
           </service>'''
        Element servicesElement1 = UtilXml.readXmlDocument(serviceXml2, false).getDocumentElement()
        ModelService modelService = reader.createModelService(servicesElement2, 'TEST')
        MODEL_SERVICE_MAP_BY_MODEL.put('', ['toImplement': reader.createModelService(servicesElement1, 'TEST'),
                                            'testParam': modelService])

        try {
            modelService.validate([header: [headerParam: 'line1', otherParam: 'Good']], 'IN', Locale.default)
        } catch (ServiceValidationException ignored) {
            Assert.fail('Complex implement not valid')
        }
    }

    @Test
    void callMakeValidContextWithIntegerInsteadOfBigDecimal() {
        String serviceXml = '''<service name="testParam" engine="java"
               location="org.apache.ofbiz.common.CommonServices" invoke="ping">
               <attribute name="quantity" type="BigDecimal" mode="IN"/>
           </service>'''
        ModelService fo = createModelService(serviceXml)
        Map sanitizedContext = [:]
        try {
            sanitizedContext = DispatchContext.makeValidContext(fo, 'IN', [quantity: 20])
        } catch (GeneralServiceException ignored) {
            Assert.fail('Error calling with integer for BigDecimal')
        }
        assert sanitizedContext.quantity instanceof BigDecimal
    }

    @Test
    void callMakeValidContextWithIntegerInsteadOfBigDecimalEmbeddedInMap() {
        String serviceXml = '''<service name="testParam" engine="java"
               location="org.apache.ofbiz.common.CommonServices" invoke="ping">
               <attribute name="someMap" type="Map" mode="IN">
                   <attribute name="quantity" type="BigDecimal" mode="IN"/>
               </attribute>
           </service>'''
        ModelService fo = createModelService(serviceXml)
        Map sanitizedContext = [:]
        try {
            sanitizedContext = DispatchContext.makeValidContext(fo, 'IN', [someMap: [quantity: 20]])
        } catch (GeneralServiceException ignored) {
            Assert.fail('Error calling with integer for BigDecimal in Map')
        }
        assert sanitizedContext.someMap.quantity instanceof BigDecimal
    }

    @Test
    void callMakeValidContextWithIntegerInsteadOfBigDecimalEmbeddedInList() {
        String serviceXml = '''<service name="testParam" engine="java"
               location="org.apache.ofbiz.common.CommonServices" invoke="ping">
               <attribute name="someList" type="List" mode="IN">
                   <attribute name="quantity" type="BigDecimal" mode="IN"/>
               </attribute>
           </service>'''
        ModelService fo = createModelService(serviceXml)
        Map sanitizedContext = [:]
        try {
            sanitizedContext = DispatchContext.makeValidContext(fo, 'IN', [someList: [[quantity: 20]]])
        } catch (GeneralServiceException ignored) {
            Assert.fail('Error calling with integer for BigDecimal in List')
        }
        assert sanitizedContext.someList[0].quantity instanceof BigDecimal
    }

    private static ModelService createModelService(String serviceXml) {
        Element serviceElement = UtilXml.readXmlDocument(serviceXml, false).getDocumentElement()
        return new ModelServiceReader(true, UtilURL.fromUrlString('http://ofbiz.apache.org'), null, null)
                .createModelService(serviceElement, 'TEST')
    }

}
