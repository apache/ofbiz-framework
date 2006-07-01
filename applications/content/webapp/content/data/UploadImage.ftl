<#if requestAttributes._ERROR_MESSAGE_?exists>
<br/><div class='errorMessage'>${requestAttributes._ERROR_MESSAGE_}</div><br/>
<#else>
    <#if dataResourceId?exists>
        <br/>
        <img src="<@ofbizUrl>img?imgId=${dataResourceId}</@ofbizUrl>" />
    </#if>
    <br/>
    ${singleWrapper.renderFormString()}
<br/>
</#if>

