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

import org.ofbiz.entity.condition.*
import org.ofbiz.entity.util.*

webSiteContent = from("WebSiteContent").where("webSiteId", webSiteId, "webSiteContentTypeId", "PUBLISH_POINT").orderBy("-fromDate").filterByDate().queryFirst();
if (webSiteContent) {
    content = webSiteContent.getRelatedOne("Content", false);
    contentRoot = content.contentId;
    context.content = content;
    context.contentRoot = contentRoot;

    // get all sub content for the publish point
    subsites = from("ContentAssoc").where("contentId", contentRoot).queryList();
    context.subsites = subsites;
}

webSiteMenu = from("WebSiteContent").where("webSiteId", webSiteId, "webSiteContentTypeId", "MENU_ROOT").orderBy("-fromDate").queryFirst();
if (webSiteMenu) {
    menu = webSiteMenu.getRelatedOne("Content", false);
    menuRoot = menu.contentId;
    context.menu = menu;
    context.menuRoot = menuRoot;

    // get all sub content for the menu root
    menus = from("ContentAssoc").where("contentId", menuRoot).queryList();
    context.menus = menus;
}

webSiteError = from("WebSiteContent").where("webSiteId", webSiteId, "webSiteContentTypeId", "ERROR_ROOT").orderBy("-fromDate").queryFirst();
if (webSiteError) {
    error = webSiteError.getRelatedOne("Content", false);
    errorRoot = error.contentId;
    context.error = error;
    context.errorRoot = errorRoot;

    // get all sub content for the error root
    errors = from("ContentAssoc").where("contentId", errorRoot).queryList();
    context.errors = errors;
}
