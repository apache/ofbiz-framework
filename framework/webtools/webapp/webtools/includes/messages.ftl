
<#if requestAttributes.errorMessageList?has_content><#assign errorMessageList=requestAttributes.errorMessageList></#if>
<#if requestAttributes.eventMessageList?has_content><#assign eventMessageList=requestAttributes.eventMessageList></#if>
<#if requestAttributes.serviceValidationException?exists><#assign serviceValidationException = requestAttributes.serviceValidationException></#if>

<#-- special error message override -->
<#if serviceValidationException?exists>
  <#assign serviceName = serviceValidationException.getServiceName()?if_exists>
  <#assign missingList = serviceValidationException.getMissingFields()?if_exists>
  <#assign extraList = serviceValidationException.getExtraFields()?if_exists>

  <#--  need if statement for EACH service (see the controller.xml file for service names) -->
  <#if serviceName?has_content && serviceName == "createPartyContactMechPurpose">
    <#-- create the inital message prefix -->
    <#assign message = "The following required fields where found empty:">

    <#-- loop through all the missing fields -->
    <#list missingList as missing>
      <#--
           check for EACH required field (see the service definition)
           then append a message for the missing field; some fields may be
           and not needed; this example show ALL fields for the service.
           ** The value inside quotes must match 100% case included.
       -->

      <#if missing == "partyId">
        <#assign message = message + "<li>Party ID</li>">
      </#if>
      <#if missing == "contactMechId">
        <#assign message = message + "<li>ContactMech ID</li>">
      </#if>
      <#if missing == "contactMechPurposeTypeId">
        <#assign message = message + "<li>Contact Purpose</li>">
      </#if>
    </#list>

    <#-- this will replace the current error message with the new one -->
    <#assign errorMsgReq = message>
  </#if>
</#if>

<#-- display the error messages -->
<#if errorMessageList?has_content>
<div class="errorMessage">The following errors occurred:</div><br/>
<ul>
  <#list errorMessageList as errorMsg>
    <li class="errorMessage">${errorMsg}</li>
  </#list>
</ul>
<br/>
</#if>
<#if eventMessageList?has_content>
<div class="eventMessage">The following occurred:</div><br/>
<ul>
  <#list eventMessageList as eventMsg>
    <li class="eventMessage">${eventMsg}</li>
  </#list>
</ul>
<br/>
</#if>
