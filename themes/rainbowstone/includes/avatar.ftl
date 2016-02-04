<div id="user-avatar" onclick="showHideUserPref()">
<#if avatarDetail??>
    <img src="/content/control/stream?contentId=${avatarDetail.contentId}" alt="user">
<#else>
    <img src="/rainbowstone/images/avatar.svg" alt="user">
</#if>
    <div id="user-details" style="display:none;">
        <div id="user-row1">
        <#if avatarDetail??>
            <img src="/content/control/stream?contentId=${avatarDetail.contentId}" alt="user">
        <#else>
            <img src="/rainbowstone/images/avatar.svg" alt="user">
        </#if>
            <div id="user-name" <#if userLogin.partyId??>onclick="javascript:location.href='/partymgr/control/viewprofile?partyId=${userLogin.partyId}'"</#if>>
                <#if person?exists>
                    <#if person.firstName??>
                <span>${person.firstName}</span>
                <span>${person.lastName?upper_case}</span>
                    <#else>
                <span>${userLogin.userLoginId}</span>
                    </#if>
                <#else>
                    <span>${userLogin.userLoginId}</span>
                </#if>
            </div>
            <a id="user-lang" href="<@ofbizUrl>ListLocales</@ofbizUrl>">
            <#assign userLang = locale.toString()>
            <#assign flagLang = locale.toString()?keep_after_last("_")>
            <#if userLang == "en"><#assign flagLang = "GB"></#if>
            <#if userLang == "fr"><#assign flagLang = "FR"></#if>
            <#if userLang == "zh"><#assign flagLang = "SG"></#if>
            <#if userLang == "th"><#assign flagLang = "TH"></#if>
                <span class="flag-icon flag-icon-<#if userLang?size <= 2>${userLang}<#else>${flagLang?lower_case}</#if>"><#if userLang?size <= 2>${userLang}<#else>${flagLang}</#if></span>
            </a>
        </div>
        <a id="visual-theme" class="user-pref-btn" href="<@ofbizUrl>ListVisualThemes</@ofbizUrl>">${uiLabelMap.CommonVisualThemes}</a>
        <a id="logout" class="user-pref-btn" href="<@ofbizUrl>logout</@ofbizUrl>">${uiLabelMap.CommonLogout}</a>
    </div>
</div>
