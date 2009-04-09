<#if requestAttributes.errorMessageList?has_content><#assign errorMessageList=requestAttributes.errorMessageList></#if>
<#if requestAttributes.eventMessageList?has_content><#assign eventMessageList=requestAttributes.eventMessageList></#if>
<#if requestAttributes.serviceValidationException?exists><#assign serviceValidationException = requestAttributes.serviceValidationException></#if>
<#if requestAttributes.uiLabelMap?has_content><#assign uiLabelMap = requestAttributes.uiLabelMap></#if>

<#if !errorMessage?has_content>
  <#assign errorMessage = requestAttributes._ERROR_MESSAGE_?if_exists>
</#if>
<#if !errorMessageList?has_content>
  <#assign errorMessageList = requestAttributes._ERROR_MESSAGE_LIST_?if_exists>
</#if>
<#if !eventMessage?has_content>
  <#assign eventMessage = requestAttributes._EVENT_MESSAGE_?if_exists>
</#if>
<#if !eventMessageList?has_content>
  <#assign eventMessageList = requestAttributes._EVENT_MESSAGE_LIST_?if_exists>
</#if>

<#-- display the error messages -->
<#if (errorMessage?has_content || errorMessageList?has_content)>
<script type="text/javascript">
    Event.observe(window, 'load', function() {
        humanMsg.displayMsg('<div class="errorMessage"><#if errorMessage?has_content><p>${errorMessage}</p></#if><#if errorMessageList?has_content><#list errorMessageList as errorMsg><p>${errorMsg}</p></#list></#if></p></div>');
        return false;
    });
</script>
</#if>

<#-- display the event messages -->
<#if (eventMessage?has_content || eventMessageList?has_content)>
<script type="text/javascript">
    Event.observe(window, 'load', function() {
        humanMsg.displayMsg('<div class="eventMessage"><#if eventMessage?has_content><p>${eventMessage}</p></#if><#if eventMessageList?has_content><#list eventMessageList as eventMsg><p>${eventMsg}</p></#list></#if></div>');
        return false;
    });
</script>
</#if>
