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
import org.ofbiz.base.util.StringUtil
import org.ofbiz.base.util.UtilXml
import org.ofbiz.base.util.collections.MapStack
import org.ofbiz.base.util.template.FreeMarkerWorker
import org.ofbiz.widget.screen.ScreenFactory
import org.ofbiz.widget.screen.ScreenRenderer
import org.ofbiz.widget.html.HtmlFormRenderer
import org.ofbiz.widget.html.HtmlScreenRenderer

import org.webslinger.commons.vfs.VFSUtil
import org.webslinger.container.FileInfo

switch (webslinger.command) {
    case 'init':
        return new HtmlScreenRenderer()
    case 'get-creator':
        return [
            createValue: { fi, name ->
                def file = fi.file
                def singleScreenText = VFSUtil.getString(file)
                def screenName = fi.servletFile.name.path
                def fullDocumentText = '<screens xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="http://ofbiz.apache.org/dtds/widget-screen.xsd"><screen name="' + screenName + '">' + singleScreenText + '</screen></screens>'
                def doc = UtilXml.readXmlDocument(fullDocumentText)
                def screens = ScreenFactory.readScreenDocument(doc, "webslinger://$webslinger.webslingerServletContext.id")
                if (screens.size() != 1) throw new IllegalArgumentException('wrong size')
                return screens.values().iterator().next()
            }
        ] as FileInfo.Creator
}
def target = webslinger.payload
def modelScreen = target.pathContext.info.getCachedItem(null)
def screenRenderer = webslinger.initObject
//response.contentType = 'text/html'
def targetContext = MapStack.create(target.context)
def screens = new ScreenRenderer(response.writer, targetContext, screenRenderer);
screens.populateContextForRequest(request, response, webslinger.webslingerServletContext)
FreeMarkerWorker.getSiteParameters(request, targetContext)
targetContext.formStringRenderer = new HtmlFormRenderer(request, response)
targetContext.simpleEncoder = StringUtil.htmlEncoder
modelScreen.renderScreenString(response.writer, targetContext, screenRenderer)
return null
