<#if requestAttributes._ERROR_MESSAGE_?exists>
<br/><div class='errorMessage'>${requestAttributes._ERROR_MESSAGE_}</div><br/>
<#else>
${singleWrapper.renderFormString()}
</#if>
