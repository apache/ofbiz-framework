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

<div id="user-avatar" onclick="showHideUserPref()">
<#if avatarDetail??>
    <img src="<@ofbizUrl>stream?contentId=${avatarDetail.contentId}</@ofbizUrl>" alt="user">
<#else>
    <svg class="appbar-btn-img" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M5.121 17.804A13.937 13.937 0 0112 16c2.5 0 4.847.655 6.879 1.804M15 10a3 3 0 11-6 0 3 3 0 016 0zm6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
    </svg>
</#if>
    <div id="user-details" style="display:none;">
        <p id="user-name" <#if userLogin.partyId??>onclick="javascript:location.href='/partymgr/control/viewprofile?partyId=${userLogin.partyId}'"</#if>>
            <#if (person.firstName)?? && (person.lastName)??>
                ${person.firstName}&nbsp;<strong> ${person.lastName?upper_case}</strong>
            <#else>
                <strong>${userLogin.userLoginId}</strong>
            </#if>
        </p>

        <a id="user-lang" href="<@ofbizUrl>ListLocales</@ofbizUrl>">
            <#assign userLang = locale.toString()>
            <#assign flagLang = locale.toString()?keep_after_last("_")>
            <#if "en" == userLang || "fr" == userLang || "zh" == userLang || "th" == userLang>
                <#if "en" == userLang><#assign flagLang = "GB"></#if>
                <#if "fr" == userLang><#assign flagLang = "FR"></#if>
                <#if "zh" == userLang><#assign flagLang = "SG"></#if>
                <#if "th" == userLang><#assign flagLang = "TH"></#if>
            <#elseif 2 == userLang?length><#assign flagLang = "UN"></#if>
                <span class="flag-icon flag-icon-<#if userLang?size <= 2>${userLang}<#else>${flagLang?lower_case}</#if>"><#if userLang?size <= 2>${userLang}<#else>${flagLang}</#if></span>
        </a>
        <a class="dark-color" title="${uiLabelMap.CommonHelp}" href="${userDocUri!Static["org.apache.ofbiz.entity.util.EntityUtilProperties"].getPropertyValue("general", "userDocUri", delegator)}<#if helpAnchor??>#${helpAnchor}</#if>" target="help">${uiLabelMap.CommonHelp}</a>
        <a id="visual-theme" class="user-pref-btn" href="<@ofbizUrl>ListVisualThemes</@ofbizUrl>">${uiLabelMap.CommonVisualThemes}</a>
        <a id="logout" class="user-pref-btn" href="<@ofbizUrl>logout</@ofbizUrl>">${uiLabelMap.CommonLogout}</a>
    </div>
</div>
