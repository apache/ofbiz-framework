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

import org.apache.ofbiz.base.util.string.UelFunctions
import org.apache.ofbiz.base.util.string.UelMappingInterface
import org.apache.ofbiz.widget.renderer.ScreenRenderer
import org.w3c.dom.Node

import java.lang.reflect.Method

/**
 * Class for importing the various system, util, and dom Uel
 */
class MiscUel implements UelMappingInterface {

    @Override
    Map<String, Method> getMapping() {
        return [
                'sys:getenv': UelFunctions.getMethod('sysGetEnv', String),
                'sys:getProperty': UelFunctions.getMethod('sysGetProp', String),
                'util:size': UelFunctions.getMethod('getSize', Object),
                'util:defaultLocale': Locale.getMethod('getDefault'),
                'util:defaultTimeZone': TimeZone.getMethod('getDefault'),
                'util:label': UelFunctions.getMethod('label', String, String, Locale),
                'screen:id': UelFunctions.getMethod('resolveCurrentScreenId', ScreenRenderer.ScreenStack),
                'dom:readHtmlDocument': UelFunctions.getMethod('readHtmlDocument', String),
                'dom:readXmlDocument': UelFunctions.getMethod('readXmlDocument', String),
                'dom:toHtmlString': UelFunctions.getMethod('toHtmlString', Node, String, boolean, int),
                'dom:toXmlString': UelFunctions.getMethod('toXmlString', Node, String, boolean, boolean, int),
                'dom:writeXmlDocument': UelFunctions.getMethod('writeXmlDocument', String, Node, String, boolean, boolean, int),
        ]
    }

}
