<#if parameters.originUserLogin??>
    <a href="#impersonateContent" title="${uiLabelMap.CommonImpersonateTitle}" id="impersonateBtn"><img src="/images/img/impersonate-ico.png" alt="${uiLabelMap.CommonImpersonateTitle}"/></a>
    <div id="impersonateContent">
        <div class="impersonateModal">
            <a href="#" class="btn-close" title="${uiLabelMap.CommonClose}">×</a>
            <h3>${uiLabelMap.CommonImpersonateTitle}</h3>
            <p>${uiLabelMap.CommonImpersonateUserLogin} : <strong>${context.userLogin.userLoginId!}</strong></p>
            <a href="depersonateLogin" class="btn" title="${uiLabelMap.CommonImpersonateStop}">${uiLabelMap.CommonImpersonateStop}</a>
        </div>
    </div>
</#if>
