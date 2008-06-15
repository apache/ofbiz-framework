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

pplookupMap = [webSiteId : webSiteId, webSiteContentTypeId : 'PUBLISH_POINT'];
webSiteContents = delegator.findList("WebSiteContent", EntityCondition.makeCondition(pplookupMap), null, ['-fromDate'], null, false);
webSiteContents = EntityUtil.filterByDate(webSiteContents);
webSiteContent = EntityUtil.getFirst(webSiteContents);
if (webSiteContent) {
    content = webSiteContent.getRelatedOne("Content");
    contentRoot = content.contentId;
    context.content = content;
    context.contentRoot = contentRoot;

    // get all sub content for the publish point
    subsites = delegator.findList("ContentAssoc", EntityCondition.makeCondition([contentId : contentRoot]), null, null, null, false);
    context.subsites = subsites;
}

mnlookupMap = [webSiteId : webSiteId, webSiteContentTypeId : 'MENU_ROOT'];
webSiteMenus = delegator.findList("WebSiteContent", EntityCondition.makeCondition(mnlookupMap), null, ['-fromDate'], null, false);
webSiteMenu = EntityUtil.getFirst(webSiteMenus);
if (webSiteMenu) {
    menu = webSiteMenu.getRelatedOne("Content");
    menuRoot = menu.contentId;
    context.menu = menu;
    context.menuRoot = menuRoot;

    // get all sub content for the publish point
    menus = delegator.findList("ContentAssoc", EntityCondition.makeCondition([contentId : menuRoot]), null, null, null, false);
    context.menus = menus;
}