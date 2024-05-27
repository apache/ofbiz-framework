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
package org.apache.ofbiz.common

import org.apache.ofbiz.entity.GenericValue

/** Duplicate content of portalPage, portalPageColumn, portalPagePortlet, portletAttribute,
 *        this method should be call with portalpageId and portalPage in context-->
 */
String duplicatePortalPage() {
    GenericValue portalPage = from('PortalPage').where(parameters).queryOne()
    Map serviceResult = run service: 'createPortalPage', with: [*: portalPage.getAllFields(),
                                                                portalPageId: null,
                                                                originalPortalPageId: parameters.portalPageId]
    run service: 'duplicatePortalPageDetails', with: [fromPortalPageId: parameters.portalPageId,
                                                      toPortalPageId: serviceResult.portalPageId]
    request.setAttribute('portalPageId', serviceResult.portalPageId)
    return success()
}

/**
 * Sets a PortalPortlet attributes
 */
String setPortalPortletAttributes() {
    if (parameters) {
        delegator.removeByAnd('PortletAttribute', [portalPageId: parameters.portalPageId,
                                                   portalPortletId: parameters.portalPortletId,
                                                   portletSeqId: parameters.portletSeqId])
        List skipField = ['portalPageId', 'portalPortletId', 'portletSeqId']
        parameters.each {
            if (skipField.contains(it.key)) {
                return
            }
            GenericValue porletAttr = from('PortletAttribute')
                    .where(portalPageId: parameters.portalPageId,
                            portalPortletId: parameters.portalPortletId,
                            portletSeqId: parameters.portletSeqId,
                            attrName: it.key)
                    .queryOne()
            if (porletAttr) {
                porletAttr.remove()
            }
            run service: 'createPortletAttribute', [*: parameters,
                                                    attrName: it.key,
                                                    attrValue: it.value]
        }
    }
    return success()
}
