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
package org.apache.ofbiz.order.request

import org.apache.ofbiz.webapp.WebAppUtil
import org.xml.sax.SAXException

// Link the notification to the request in the webapp of the web site the request came from:
// the web site of the notification, else the first web site of the request's product store.
String siteId = context.webSiteId
if (!siteId && custRequest?.productStoreId) {
    siteId = from('WebSite').where(productStoreId: custRequest.productStoreId).cache().queryFirst()?.webSiteId
}
if (siteId) {
    try {
        context.custRequestUrl = WebAppUtil.getWebappInfoFromWebsiteId(siteId).contextRoot +
                "/control/ViewRequest?custRequestId=${custRequest.custRequestId}"
    } catch (IllegalArgumentException | IOException | SAXException e) {
        logWarning("No webapp found for web site ${siteId}: ${e.message}")
    }
}
