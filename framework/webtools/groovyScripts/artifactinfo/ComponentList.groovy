/*
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
 */

import java.util.Collection
import java.util.List

import org.apache.ofbiz.base.component.ComponentConfig
import org.apache.ofbiz.base.component.ComponentConfig.WebappInfo

import org.apache.ofbiz.base.util.*

Collection <ComponentConfig> components = ComponentConfig.getAllComponents()
List componentList = []

components.each { component ->
     List<WebappInfo> webApps = component.getWebappInfos()
     webApps.each { webApp ->
         componentMap = [:]
         componentMap.compName = component.getComponentName()
         componentMap.rootLocation =  component.getRootLocation()
         componentMap.enabled = (component.enabled() == true? "Y" : "N")
         componentMap.webAppName = webApp.getName()
         componentMap.contextRoot = webApp.getContextRoot()
         componentMap.location = webApp.getLocation()
         componentMap.webAppName = webApp.getName()
         componentMap.contextRoot = webApp.getContextRoot()
         componentMap.location = webApp.getLocation()
         componentList.add(componentMap)
     }
     if (!webApps) {
         componentMap = [:]
         componentMap.compName = component.getComponentName()
         componentMap.rootLocation =  component.getRootLocation()
         componentMap.enabled = (component.enabled() == true? "Y" : "N")
         componentList.add(componentMap)
         componentMap.webAppName = ""
         componentMap.contextRoot = ""
         componentMap.location = ""
         componentMap.webAppName = ""
         componentMap.contextRoot = ""
         componentMap.location = ""
     }
}

// sort the entries
componentList = UtilMisc.sortMaps(componentList, UtilMisc.toList("+compName"))

// make the list more readable
lastComp = null
for (int entry = 0; entry < componentList.size(); entry++) {
    compSave = componentList[entry].compName
    if (lastComp != null && compSave.equals(lastComp)) {
        componentList[entry].compName = ""
        componentList[entry].rootLocation = ""
        componentList[entry].enabled = ""
    }    
    lastComp = compSave
}
context.componentList = componentList
