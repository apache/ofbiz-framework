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
package org.apache.ofbiz.content.content

import org.apache.ofbiz.base.util.GeneralException

// A <content content-id="${contentId}"/> widget does not necessarily render contentId's own
// DataResource: ContentWorker.findContentForRendering() can redirect a WEB_SITE_PUB_PT publish
// point to its PUBLISH_LINK-ed target, or substitute an ALTERNATE_LOCALE sibling. Resolve the
// same record here so a caller can check isPublic against what will actually be rendered.
renderedDataResource = null
if (contentId) {
    try {
        resolvedContent = ContentWorker.findContentForRendering(delegator, contentId, locale, null, null, true)
        if (resolvedContent.dataResourceId) {
            renderedDataResource = from('DataResource').where('dataResourceId', resolvedContent.dataResourceId).cache().queryOne()
        }
    } catch (GeneralException | IOException e) {
        // contentId does not exist, or e.g. a publish point has no currently published target --
        // leave renderedDataResource null so callers deny access by default
        logVerbose("Could not resolve content for rendering [${contentId}]: ${e.message}")
    }
}
context.renderedDataResource = renderedDataResource
