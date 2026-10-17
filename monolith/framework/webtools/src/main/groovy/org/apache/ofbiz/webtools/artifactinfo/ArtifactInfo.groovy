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
package org.apache.ofbiz.webtools.artifactinfo

import org.apache.ofbiz.base.util.Debug
import org.apache.ofbiz.base.util.UtilURL

String name = parameters.name
String location = parameters.location
if (location) {
    if (UtilURL.fromUrlString(location)) {
        Debug.logError('For security reason HTTP URLs are not accepted, see OFBIZ-12306', 'ArtifactInfo.groovy')
        return
    }
}
String type = parameters.type
String uniqueId = parameters.uniqueId
delegatorName = delegator.getDelegatorName()
if (delegatorName.contains('default#')) {
    delegatorName = 'default'
}
aif = ArtifactInfoFactory.getArtifactInfoFactory(delegatorName)
context.aif = aif
artifactInfo = null
if (parameters.findType == 'search') {
    artifactInfoSet = aif.getAllArtifactInfosByNamePartial(name, type)
    if (artifactInfoSet.size() == 1) {
        artifactInfo = artifactInfoSet.iterator().next()
        context.artifactInfo = artifactInfo
    } else {
        context.artifactInfoSet = new TreeSet(artifactInfoSet)
    }
} else {
    if (name) {
        artifactInfo = aif.getArtifactInfoByNameAndType(name, location, type)
        context.artifactInfo = artifactInfo
    } else if (uniqueId) {
        artifactInfo = aif.getArtifactInfoByUniqueIdAndType(uniqueId, type)
        context.artifactInfo = artifactInfo
    }
}

if (artifactInfo) {
    artifactInfoMap = [type: artifactInfo.getType(), uniqueId: artifactInfo.getUniqueId(), displayName: artifactInfo.getDisplayName()]
    // add to the recently viewed list
    recentArtifactInfoList = session.getAttribute('recentArtifactInfoList')
    if (!recentArtifactInfoList) {
        recentArtifactInfoList = []
        session.setAttribute('recentArtifactInfoList', recentArtifactInfoList)
    }
    if (!recentArtifactInfoList.contains(artifactInfoMap)) {
        recentArtifactInfoList.add(0, artifactInfoMap)
    }
}
