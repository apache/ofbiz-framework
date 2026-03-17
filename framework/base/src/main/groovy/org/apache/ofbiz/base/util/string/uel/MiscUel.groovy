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
package org.apache.ofbiz.base.util.string.uel

import org.apache.ofbiz.base.util.string.IUelMappingLibrary
import org.apache.ofbiz.base.util.string.UelFunctions
import org.apache.ofbiz.base.util.string.UelMapping
import org.apache.ofbiz.widget.renderer.ScreenRenderer
import org.w3c.dom.Node

/**
 * Class for importing the various system, util, and dom Uel
 */
class MiscUel implements IUelMappingLibrary {

    @Override
    List<UelMapping> getUelMappingList() {
        return [
                new UelMapping('sys:getenv', UelFunctions.getMethod('sysGetEnv', String)),
                new UelMapping('sys:getProperty', UelFunctions.getMethod('sysGetProp', String)),
                new UelMapping('util:size', UelFunctions.getMethod('getSize', Object)),
                new UelMapping('util:defaultLocale', Locale.getMethod('getDefault')),
                new UelMapping('util:defaultTimeZone', TimeZone.getMethod('getDefault')),
                new UelMapping('util:label', UelFunctions.getMethod('label', String, String, Locale)),
                new UelMapping('screen:id', UelFunctions.getMethod('resolveCurrentScreenId', ScreenRenderer.ScreenStack)),
                new UelMapping('dom:readHtmlDocument', UelFunctions.getMethod('readHtmlDocument', String)),
                new UelMapping('dom:readXmlDocument', UelFunctions.getMethod('readXmlDocument', String)),
                new UelMapping('dom:toHtmlString', UelFunctions.getMethod('toHtmlString', Node, String, boolean, int)),
                new UelMapping('dom:toXmlString', UelFunctions.getMethod('toXmlString', Node, String, boolean, boolean, int)),
                new UelMapping('dom:writeXmlDocument', UelFunctions.getMethod('writeXmlDocument', String, Node, String, boolean, boolean, int)),
        ]
    }

}
