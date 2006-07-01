<#-- display the error messages -->
<#if requestAttributes.errorMessageList?has_content>
<div class="errorMessage">The following errors occurred:</div><br/>
<ul>
  <#list requestAttributes.errorMessageList as errorMsg>
    <li class="errorMessage">${errorMsg}</li>
  </#list>
</ul>
</#if>
<#if requestAttributes.eventMessageList?has_content>
<div class="eventMessage">The following occurred:</div><br/>
<ul>
  <#list requestAttributes.eventMessageList as eventMsg>
    <li class="eventMessage">${eventMsg}</li>
  </#list>
</ul>
</#if>
