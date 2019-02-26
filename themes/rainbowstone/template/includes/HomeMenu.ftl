<#--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

<#if (requestAttributes.externalLoginKey)?exists><#assign externalKeyParam = "?externalLoginKey=" + requestAttributes.externalLoginKey?if_exists></#if>
<#if (externalLoginKey)?exists><#assign externalKeyParam = "?externalLoginKey=" + requestAttributes.externalLoginKey?if_exists></#if>
<#assign ofbizServerName = application.getAttribute("_serverId")?default("default-server")>
<#assign contextPath = request.getContextPath()>
<#assign displayApps = Static["org.apache.ofbiz.webapp.WebAppCache"].getShared().getAppBarWebInfos(ofbizServerName, "main")>
<#assign displaySecondaryApps = Static["org.apache.ofbiz.webapp.WebAppCache"].getShared().getAppBarWebInfos(ofbizServerName, "secondary")>
<#assign avatarList = EntityQuery.use(delegator).from("PartyContent").where("partyId", person.partyId!, "partyContentTypeId", "LGOIMGURL").queryList()!>
<#if avatarList?has_content>
    <#assign avatar = Static["org.apache.ofbiz.entity.util.EntityUtil"].getFirst(avatarList)>
    <#assign avatarDetail = EntityQuery.use(delegator).from("PartyContentDetail").where("partyId", person.partyId!, "contentId", avatar.contentId!).queryFirst()!>
</#if>
<body onpageshow="showHideFavorites()">
<script type="application/javascript">
    function showHideFavorites() {
        var showHideStatus = document.getElementById("showHideBtn");
        var normalItems = document.getElementsByClassName("normalItem");
        for (var i=0;i<normalItems.length;i++) {
            if (showHideStatus.checked) {
                normalItems[i].style.display = 'none';
            }else{
                normalItems[i].style.display = 'flex';
            }
        }
        displayFavorite();
    }

    function addToFavorite(linkId, itemMenu) {
        var tile = document.getElementById(linkId);
        var imgTile = document.getElementById('img'+linkId);
        var isFavorite = 'false' ;
        if(tile.getAttribute('class').indexOf('normalItem')==-1) {
            isFavorite = 'false' ;
            tile.setAttribute('class','hp-menu-item normalItem') ;
            imgTile.setAttribute('src','/rainbowstone/images/star-white.svg')
        } else {
            isFavorite = 'true' ;
            tile.setAttribute('class','hp-menu-item') ;
            imgTile.setAttribute('src','/rainbowstone/images/star-yellow.svg')
        }
        setUserLayoutPreferences('HOME_MENU_FAVORITES', itemMenu, isFavorite);
    }

    function displayFavorite() {
        var showHideStatus = document.getElementById("showHideBtn");
        var displayFavorite = 'false';
        if (showHideStatus.checked) {
            displayFavorite = 'true';
        }else{
            displayFavorite = 'false';
        }
        setUserLayoutPreferences('HOME_MENU_FAVORITES', 'displayFavorites', displayFavorite);
    }
</script>
<div id="wait-spinner" style="display:none">
    <div id="wait-spinner-image"></div>
</div>
<div class="page-container">
    <div class="hidden">
        <a href="#column-container" title="${uiLabelMap.CommonSkipNavigation}" accesskey="2">
        ${uiLabelMap.CommonSkipNavigation}
        </a>
    </div>
<#if userLogin?has_content>
    <div id="main-navigation-bar" class="fixed-nav-bar">
        <div id="main-nav-bar-left">
            <img id="homeGlyph" src="/rainbowstone/images/home.svg" alt="Home">
            <span id="homePageTitle">Home Page</span>
            <label class="main-bar-label">${uiLabelMap.EmbiHomeMenuDisplayAllMenu}</label>
            <input id="showHideBtn" type="checkbox" class="nrd-chkbox" <#if "true" == displayFavorites>checked</#if> onchange="showHideFavorites()">
            <label for="showHideBtn"></label>
            <label class="main-bar-label">${uiLabelMap.EmbiHomeMenuDisplayFavorites}</label>
        </div> <!-- main-nav-bar-left -->
        <div id="main-nav-bar-right">
            <div id="company-logo"></div>
            <#include "component://rainbowstone/template/includes/Avatar.ftl"/>
        </div>  <!-- main-nav-bar-right -->
    </div> <!-- main navigation bar -->
    <div id="nav-bar-offset"></div>
    <div id="home-menu-list">
        <#assign isComponentEmpty=true>
        <#assign tileNumber=0>
        <#list componentList as mainMenu>
            <#list mainMenu as itemMenu>
                <#if itemMenu.type=="Component">
                <ul class="hp-applist">
                    <div class="app-title"><span><a href="${itemMenu.mountPoint}control/main"<#if uiLabelMap?exists> title="${uiLabelMap[itemMenu.description]}">${uiLabelMap[itemMenu.title]}<#else> title="${itemMenu.description}">${itemMenu.title}</#if></a></span></div>
                <#else>
                    <#assign tileNumber = tileNumber+1>
                    <#assign isComponentEmpty=false>
                    <#assign menuTitle = Static["org.apache.ofbiz.base.util.string.FlexibleStringExpander"].expandString(itemMenu.menuTitle, context)/>
                    <li  id="Tile${tileNumber}" class="hp-menu-item <#if "white" == itemMenu.favorite>normalItem<#else>favoriteItem</#if>">
                        <a href="${itemMenu.urlLink}" class="menu-link" title="${itemMenu.menuTitle}">${menuTitle}</a>
                        <a href="javascript:addToFavorite('Tile${tileNumber}', '${itemMenu.urlLink}')" title="${uiLabelMap.ClicToAddInFavorite}">
                            <img id='imgTile${tileNumber}' class="star-link" src="/rainbowstone/images/star-${itemMenu.favorite}.svg">
                        </a>
                    </li>
                </#if>
            </#list>
            <#if !isComponentEmpty>
            </ul>
                <#assign isComponentEmpty=true>
            </#if>
        </#list>
    </div>
</#if>
