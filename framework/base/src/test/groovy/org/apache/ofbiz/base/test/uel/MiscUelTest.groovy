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
package org.apache.ofbiz.base.test.uel

import org.apache.ofbiz.base.util.string.FlexibleStringExpander
import org.apache.ofbiz.base.util.string.UelFunctions

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

/**
 * ./gradlew test --tests "org.apache.ofbiz.base.test.uel.MiscUelTest"
 */
/* codenarc-disable GStringExpressionWithinString,ClosureAsLastMethodParameter */
class MiscUelTest {

    @BeforeAll
    static void loadUelFunctions() {
        UelTestSupport.ensureUelFunctionsLoaded()
    }

    @Test
    void systemEnvAndPropertyUelFunctionsWork() { // codenarc-disable JUnitTestMethodWithoutAssert
        doUelSystemTest('${sys:getenv("foo")}', 'foo', { String prop -> UelFunctions.sysGetEnv(prop) })
        doUelSystemTest('${sys:getProperty("bar")}', 'bar', { String prop -> UelFunctions.sysGetProp(prop) })
    }

    @Test
    void utilSizeUelFunctionReturnsListSize() {
        List foo = [1, 2, 3]
        FlexibleStringExpander fse = FlexibleStringExpander.getInstance('${util:size(foo)}')
        assert fse.expand([foo: foo]) == UelFunctions.getSize(foo)
    }

    @Test
    void defaultLocaleAndTimeZoneUelFunctionsReturnSystemDefaults() {
        FlexibleStringExpander fse = FlexibleStringExpander.getInstance('${util:defaultLocale()}')
        assert fse.expand([foo: 'bar']) == Locale.getDefault()
        fse = FlexibleStringExpander.getInstance('${util:defaultTimeZone()}')
        assert fse.expand([foo: 'bar']) == TimeZone.getDefault()
    }

    @Test
    void labelUelFunctionResolvesPropertyLabel() {
        String labelFile = 'CommonEntityLabels', labelKey = 'VisualTheme.description.RAINBOWSTONE_AMBER'
        FlexibleStringExpander fse = FlexibleStringExpander.getInstance(
                '${util:label(\'CommonEntityLabels\',\'VisualTheme.description.RAINBOWSTONE_AMBER\', locale)}')
        assert fse.expand([locale: Locale.getDefault()]) == UelFunctions.label(labelFile, labelKey, Locale.getDefault())
    }

    private void doUelSystemTest(String uelInput, String param, Closure uelFunction) {
        FlexibleStringExpander fse = FlexibleStringExpander.getInstance(uelInput)
        assert fse.expand([:]) == uelFunction(param)
    }

//     TODO: Some day
//    '${screen:id}', UelFunctions.class.getMethod("resolveCurrentScreenId", ScreenRenderer.ScreenStack.class))
//    '${dom:readHtmlDocument}', UelFunctions.class.getMethod("readHtmlDocument", String.class))
//    '${dom:readXmlDocument}', UelFunctions.class.getMethod("readXmlDocument", String.class))
//    '${dom:toHtmlString}', UelFunctions.class.getMethod("toHtmlString", Node.class, String.class, boolean.class,
//          int.class))
//    '${dom:toXmlString}', UelFunctions.class.getMethod("toXmlString", Node.class, String.class, boolean.class,
//          boolean.class, int.class))
//    '${dom:writeXmlDocument}', UelFunctions.class.getMethod("writeXmlDocument", String.class, Node.class,
//          String.class, boolean.class, boolean.class, int.class))

}
