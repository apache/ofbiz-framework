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
    <img src="/rainbowstone/images/avatar.svg" alt="user">
</#if>
    <div id="user-details" style="display:none;">
        <div id="user-row1">
        <#if avatarDetail??>
            <img src="<@ofbizUrl>stream?contentId=${avatarDetail.contentId}</@ofbizUrl>" alt="user">
        <#else>
            <img src="/rainbowstone/images/avatar.svg" alt="user">
        </#if>
            <div id="user-name" <#if userLogin.partyId??>onclick="javascript:location.href='/partymgr/control/viewprofile?partyId=${userLogin.partyId}'"</#if>>
                <#if (person.firstName)?? && (person.lastName)??>
                    <span>${person.firstName}</span>
                    <span>${person.lastName?upper_case}</span>
                <#else>
                    <span>${userLogin.userLoginId}</span>
                </#if>
            </div>
            <a id="user-lang" href="<@ofbizUrl>ListLocales</@ofbizUrl>">
            <#assign userLang = locale.toString()>
            <#assign flagLang = locale.toString()?keep_after_last("_")>
            <#if "en" == userLang><#assign flagLang = "GB"></#if>
            <#if "fr" == userLang><#assign flagLang = "FR"></#if>
            <#if "zh" == userLang><#assign flagLang = "SG"></#if>
            <#if "th" == userLang><#assign flagLang = "TH"></#if>
                <span class="flag-icon flag-icon-<#if userLang?size <= 2>${userLang}<#else>${flagLang?lower_case}</#if>"><#if userLang?size <= 2>${userLang}<#else>${flagLang}</#if></span>
            </a>
        </div>
        <a id="visual-theme" class="user-pref-btn" href="<@ofbizUrl>ListVisualThemes</@ofbizUrl>">${uiLabelMap.CommonVisualThemes}</a>
        <a id="logout" class="user-pref-btn" href="<@ofbizUrl>logout</@ofbizUrl>">${uiLabelMap.CommonLogout}</a>
    </div>
</div>
