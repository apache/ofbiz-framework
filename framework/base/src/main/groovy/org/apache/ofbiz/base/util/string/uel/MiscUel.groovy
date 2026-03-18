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
                new UelMapping('sys:getenv', UelFunctions.getMethod('sysGetEnv', String),
                        'Gets the value of the specified environment variable.'),
                new UelMapping('sys:getProperty', UelFunctions.getMethod('sysGetProp', String),
                        'Gets the system property indicated by the specified key.'),
                new UelMapping('util:size', UelFunctions.getMethod('getSize', Object),
                        'Returns the default Locale.'),
                new UelMapping('util:defaultLocale', Locale.getMethod('getDefault'),
                        'Returns the default TimeZone.'),
                new UelMapping('util:defaultTimeZone', TimeZone.getMethod('getDefault'),
                        'Return the label present in ressource on the given locale.'),
                new UelMapping('util:label', UelFunctions.getMethod('label', String, String, Locale),
                        'Returns the size of Maps,'),
                new UelMapping('screen:id', UelFunctions.getMethod('resolveCurrentScreenId', ScreenRenderer.ScreenStack),
                        'Returns the id of the current screen, Collections, and Strings. Invalid Object types return -1.'),
                new UelMapping('dom:readHtmlDocument', UelFunctions.getMethod('readHtmlDocument', String),
                        'Reads an HTML file and returns a org.w3c.dom.Document instance.'),
                new UelMapping('dom:readXmlDocument', UelFunctions.getMethod('readXmlDocument', String),
                        'Reads an XML file and returns a org.w3c.dom.Document instance.'),
                new UelMapping('dom:toHtmlString', UelFunctions.getMethod('toHtmlString', Node, String, boolean, int),
                        'Returns a org.w3c.dom.Node as an HTML String.'),
                new UelMapping('dom:toXmlString', UelFunctions.getMethod('toXmlString', Node, String, boolean, boolean, int),
                        'Returns a org.w3c.dom.Node as an XML String.'),
                new UelMapping('dom:writeXmlDocument', UelFunctions.getMethod('writeXmlDocument', String, Node, String, boolean, boolean, int),
                        'Writes a org.w3c.dom.Node to an XML file and returns true if successful.')
        ]
    }

}
