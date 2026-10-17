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
<#escape x as x?html>
  <#if requestAttributes.errorMessageList?has_content><#assign errorMessageList=requestAttributes.errorMessageList></#if>
  <#if requestAttributes.eventMessageList?has_content><#assign eventMessageList=requestAttributes.eventMessageList></#if>
  <#if requestAttributes.warningMessageList?has_content><#assign warningMessageList=requestAttributes.warningMessageList></#if>
  <#if requestAttributes.serviceValidationException??><#assign serviceValidationException = requestAttributes.serviceValidationException></#if>
  <#if requestAttributes.uiLabelMap?has_content><#assign uiLabelMap = requestAttributes.uiLabelMap></#if>

  <#if !errorMessage?has_content>
    <#assign errorMessage = requestAttributes._ERROR_MESSAGE_!>
  </#if>
  <#if !errorMessageList?has_content>
    <#assign errorMessageList = requestAttributes._ERROR_MESSAGE_LIST_!>
  </#if>
  <#if !eventMessage?has_content>
    <#assign eventMessage = requestAttributes._EVENT_MESSAGE_!>
  </#if>
  <#if !eventMessageList?has_content>
    <#assign eventMessageList = requestAttributes._EVENT_MESSAGE_LIST_!>
  </#if>
  <#if !warningMessage?has_content>
    <#assign warningMessage = requestAttributes._WARNING_MESSAGE_?if_exists>
  </#if>
  <#if !warningMessageList?has_content>
    <#assign warningMessageList = requestAttributes._WARNING_MESSAGE_LIST_?if_exists>
  </#if>
  <#assign unsafeEventMessage = requestAttributes._UNSAFE_EVENT_MESSAGE_!>

  <#-- display the error messages -->
  <#if (errorMessage?has_content || errorMessageList?has_content)>
    <div id="content-messages" class="content-messages errorMessage"
        onclick="document.getElementById('content-messages').parentNode.removeChild(this)">
      <#noescape><p>${uiLabelMap.CommonFollowingErrorsOccurred}:</p></#noescape>
      <#if errorMessage?has_content>
        <p>${StringUtil.wrapString(errorMessage)}</p>
      </#if>
      <#if errorMessageList?has_content>
        <#list errorMessageList as errorMsg>
          <p>${StringUtil.wrapString(errorMsg)}</p>
        </#list>
      </#if>
    </div>
  </#if>

  <#-- display the event messages -->
  <#if (eventMessage?has_content || eventMessageList?has_content || unsafeEventMessage?has_content)>
    <div id="content-messages" class="content-messages eventMessage hidden"
      onclick="document.getElementById('content-messages').parentNode.removeChild(this)">
    <#noescape><p>${uiLabelMap.CommonFollowingOccurred}:</p></#noescape>
    <#if eventMessage?has_content>
      <p>${StringUtil.wrapString(eventMessage)}</p>
    </#if>
    <#if eventMessageList?has_content>
      <#list eventMessageList as eventMsg>
        <p>${StringUtil.wrapString(eventMsg)}</p>
      </#list>
    </#if>
    <#if unsafeEventMessage?has_content>
      <#noescape><p>${StringUtil.wrapString(unsafeEventMessage)}</p></#noescape>
    </#if>
    </div>
  </#if>

  <#-- display the warning messages -->
  <#if (warningMessage?has_content || warningMessageList?has_content)>
    <div id="content-messages" class="content-messages errorMessage"
        onclick="document.getElementById('content-messages').parentNode.removeChild(this)">
    <#noescape><p>${uiLabelMap.CommonFollowingErrorsOccurred}:</p></#noescape>
    <#if warningMessage?has_content>
      <p>${StringUtil.wrapString(warningMessage)}</p>
    </#if>
      <#if warningMessageList?has_content>
        <#list warningMessageList as warningMsg>
          <p>${StringUtil.wrapString(warningMsg)}</p>
        </#list>
      </#if>
    </div>
  </#if>

  <#if (errorMessage?has_content || errorMessageList?has_content
     || eventMessage?has_content || eventMessageList?has_content || unsafeEventMessage?has_content
     || warningMessage?has_content || warningMessageList?has_content)>
    <#assign jGrowlPosition = modelTheme.getProperty("jgrowlPosition")>
    <#assign jGrowlWidth = modelTheme.getProperty("jgrowlWidth")>
    <#assign jGrowlHeight = modelTheme.getProperty("jgrowlHeight")>
    <#assign jGrowlSpeed = modelTheme.getProperty("jgrowlSpeed")>
    <script>
    <#if unsafeEventMessage?has_content>
setTimeout(function(){
  showjGrowl(
          "${uiLabelMap.CommonShowAll}", "${uiLabelMap.CommonCollapse}", "${uiLabelMap.CommonHideAllNotifications}",
          "${jGrowlPosition}", "${jGrowlWidth}", "${jGrowlHeight}", "${jGrowlSpeed}");
      }, 100);
    <#else>
showjGrowl(
        "${uiLabelMap.CommonShowAll}", "${uiLabelMap.CommonCollapse}", "${uiLabelMap.CommonHideAllNotifications}",
        "${jGrowlPosition}", "${jGrowlWidth}", "${jGrowlHeight}", "${jGrowlSpeed}");
    </#if>
    </script>
  </#if>
</#escape>
