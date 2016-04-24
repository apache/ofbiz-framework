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
<#assign displayApps = Static["org.ofbiz.base.component.ComponentConfig"].getAppBarWebInfos(ofbizServerName, "main")>
<#assign displaySecondaryApps = Static["org.ofbiz.base.component.ComponentConfig"].getAppBarWebInfos(ofbizServerName, "secondary")>
<#if person?has_content>
    <#assign avatarList = delegator.findByAnd("PartyContent", {"partyId" : person.partyId, "partyContentTypeId" : "LGOIMGURL"}, null, false)>
    <#if avatarList?has_content>
        <#assign avatar = Static["org.ofbiz.entity.util.EntityUtil"].getFirst(avatarList)>
        <#assign avatarDetail = Static["org.ofbiz.entity.util.EntityUtil"].getFirst(delegator.findByAnd("PartyContentDetail", {"partyId" : person.partyId, "contentId" : avatar.contentId}, null, false))>
    </#if>
</#if>
<body>
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
    <#assign appMax = 8>
    <#assign alreadySelected = false>
<div id="main-navigation-bar">
    <div id="main-nav-bar-left">
        <#--<a id="homeButton" href="<@ofbizUrl>HomeMenu</@ofbizUrl>"><img id="homeButtonImage" src="/rainbowstone/images/home.svg" alt="Home"></a>-->
        <ul id="app-bar-list">
            <#assign appCount = 0>
            <#assign firstApp = true>
            <#list displayApps as display>
                <#assign thisApp = display.getContextRoot()>
                <#assign permission = true>
                <#assign selected = false>
                <#assign permissions = display.getBasePermission()>
                <#list permissions as perm>
                    <#if (perm != "NONE" && !security.hasEntityPermission(perm, "_VIEW", session))>
                    <#-- User must have ALL permissions in the base-permission list -->
                        <#assign permission = false>
                    </#if>
                </#list>
                <#if permission == true>
                    <#if thisApp == contextPath || contextPath + "/" == thisApp>
                        <#assign selected = true>
                    </#if>
                    <#assign thisApp = StringUtil.wrapString(thisApp)>
                    <#assign thisURL = thisApp>
                    <#if thisApp != "/">
                        <#assign thisURL = thisURL + "/control/main">
                    </#if>
                    <#if layoutSettings.suppressTab?exists && display.name == layoutSettings.suppressTab>
                    <#-- do not display this component-->
                    <#else>
                        <#if appCount<=appMax>
                            <li class="app-btn<#if selected> selected</#if>">
                                <#if selected>
                                <div id="app-selected">
                                    <#assign alreadySelected = true>
                                </#if>
                                <a href="${thisURL}${StringUtil.wrapString(externalKeyParam)}"<#if uiLabelMap?exists> title="${uiLabelMap[display.description]}">${uiLabelMap[display.title]}<#else> title="${display.description}">${display.title}</#if></a>
                                <#if selected>
                                    <div id="color-add"></div>
                                </div>
                                </#if>
                            </li>
                        <#else>
                            <#break>
                        </#if>
                        <#assign appCount = appCount + 1>
                    </#if>
                </#if>
            </#list>
            <#list displaySecondaryApps as display>
                <#assign thisApp = display.getContextRoot()>
                <#assign permission = true>
                <#assign selected = false>
                <#assign permissions = display.getBasePermission()>
                <#list permissions as perm>
                    <#if (perm != "NONE" && !security.hasEntityPermission(perm, "_VIEW", session))>
                    <#-- User must have ALL permissions in the base-permission list -->
                        <#assign permission = false>
                    </#if>
                </#list>
                <#if permission == true>
                    <#if thisApp == contextPath || contextPath + "/" == thisApp>
                        <#assign selected = true>
                    </#if>
                    <#assign thisApp = StringUtil.wrapString(thisApp)>
                    <#assign thisURL = thisApp>
                    <#if thisApp != "/">
                        <#assign thisURL = thisURL + "/control/main">
                    </#if>
                    <#if appCount<=appMax>
                        <li class="app-btn<#if selected> selected</#if>">
                            <#if selected>
                            <div id="app-selected">
                                <#assign alreadySelected = true>
                            </#if>
                            <a href="${thisURL}${StringUtil.wrapString(externalKeyParam)}"<#if uiLabelMap?exists> title="${uiLabelMap[display.description]}">${uiLabelMap[display.title]}<#else> title="${display.description}">${display.title}</#if></a>
                            <#if selected>
                                <div id="color-add"></div>
                            </div>
                            </#if>
                        </li>
                    <#else>
                        <#break>
                    </#if>
                    <#assign appCount = appCount + 1>
                </#if>
            </#list>
        </ul>
        <!-- Si le nombre d'application est supérieur au nombre d'application max affichable, je met le restant
        dans un menu déroulant. J'ai volontairement doublé le code car sinon, la lecture du code lors d'une maintenance
        risquait d'être compliquée. A corriger si jamais les performances s'en font ressentir -->
        <#assign appCount = 0>
        <#assign moreApp = false>
        <#list displayApps as display>
            <#assign thisApp = display.getContextRoot()>
            <#assign permission = true>
            <#assign selected = false>
            <#assign permissions = display.getBasePermission()>
            <#list permissions as perm>
                <#if (perm != "NONE" && !security.hasEntityPermission(perm, "_VIEW", session))>
                <#-- User must have ALL permissions in the base-permission list -->
                    <#assign permission = false>
                </#if>
            </#list>
            <#if permission == true>
                <#if thisApp == contextPath || contextPath + "/" == thisApp>
                    <#assign selected = true>
                </#if>
                <#assign thisApp = StringUtil.wrapString(thisApp)>
                <#assign thisURL = thisApp>
                <#if thisApp != "/">
                    <#assign thisURL = thisURL + "/control/main">
                </#if>
                <#if layoutSettings.suppressTab?exists && display.name == layoutSettings.suppressTab>
                <#-- do not display this component-->
                <#else>
                    <#if appMax < appCount>
                        <#if !moreApp>
                        <div id="more-app" <#if !alreadySelected>class="selected"</#if>>
                            <span>+</span>
                        <ul id="more-app-list">
                            <#assign moreApp = true>
                        </#if>
                        <li class="app-btn-sup<#if selected> selected</#if>">
                            <a class="more-app-a" href="${thisURL}${StringUtil.wrapString(externalKeyParam)}"<#if uiLabelMap?exists> title="${uiLabelMap[display.description]}">${uiLabelMap[display.title]}<#else> title="${display.description}">${display.title}</#if></a>
                        </li>
                    </#if>
                    <#assign appCount = appCount + 1>
                </#if>
            </#if>
        </#list>
        <#list displaySecondaryApps as display>
            <#assign thisApp = display.getContextRoot()>
            <#assign permission = true>
            <#assign selected = false>
            <#assign permissions = display.getBasePermission()>
            <#list permissions as perm>
                <#if (perm != "NONE" && !security.hasEntityPermission(perm, "_VIEW", session))>
                <#-- User must have ALL permissions in the base-permission list -->
                    <#assign permission = false>
                </#if>
            </#list>
            <#if permission == true>
                <#if thisApp == contextPath || contextPath + "/" == thisApp>
                    <#assign selected = true>
                </#if>
                <#assign thisApp = StringUtil.wrapString(thisApp)>
                <#assign thisURL = thisApp>
                <#if thisApp != "/">
                    <#assign thisURL = thisURL + "/control/main">
                </#if>
                <#if appMax < appCount>
                    <#if !moreApp>
                    <div id="more-app">
                        <span>+</span>
                    <ul id="more-app-list">
                        <#assign moreApp = true>
                    </#if>
                    <li class="app-btn-sup<#if selected> selected</#if>">
                        <a class="more-app-a" href="${thisURL}${StringUtil.wrapString(externalKeyParam)}"<#if uiLabelMap?exists> title="${uiLabelMap[display.description]}">${uiLabelMap[display.title]}<#else> title="${display.description}">${display.title}</#if></a>
                    </li>
                </#if>
                <#assign appCount = appCount + 1>
            </#if>
        </#list>
        <#if moreApp>
        </ul> <!-- more-app-list -->
        </div> <!-- more-app -->
        </#if>
    </div>
        <div id="main-nav-bar-right">
            <div id="company-logo"></div>
            <#if parameters.componentName?exists && requestAttributes._CURRENT_VIEW_?exists && helpTopic?exists>
                <a class="dark-color" href="javascript:lookup_popup1('showHelp?helpTopic=${helpTopic}&amp;portalPageId=${parameters.portalPageId!}','help' ,500,500);" title="${uiLabelMap.CommonHelp}"></a>
            </#if>

            <#include "component://rainbowstone/template/includes/avatar.ftl"/>
        </div>
    </div> <!-- main navigation bar -->
    <div id="app-bar-line"></div>
</#if>
