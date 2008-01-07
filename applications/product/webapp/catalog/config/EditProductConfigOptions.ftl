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
<table cellspacing="0" class="basic-table">
    <tr class="header-row">
      <td><b>${uiLabelMap.CommonName}</b></td>
      <td><b>${uiLabelMap.CommonSequenceNum}</b></td>
      <td><b>${uiLabelMap.CommonDescription}</b></td>
      <td><b>&nbsp;</b></td>
      <td><b>&nbsp;</b></td>
      <td><b>&nbsp;</b></td>
    </tr>
    <#assign rowClass = "2">
    <#list configOptionList as question>
      <form method="post" action="<@ofbizUrl>updateProductConfigOption</@ofbizUrl>">
        <input type="hidden" name="configItemId" value="${question.configItemId}">
        <input type="hidden" name="configOptionId" value="${question.configOptionId}">
        <tr valign="middle"<#if rowClass == "1"> class="alternate-row"</#if>>
          <td>${question.configOptionId} - ${question.configOptionName?if_exists}</td>
          <td><input type="text" name="sequenceNum" size="3" value="${question.sequenceNum?if_exists}"></td>
          <td>${question.description?if_exists}</td>
          <td><input type="submit" value="${uiLabelMap.CommonUpdate}"></td>
          <td><a href="<@ofbizUrl>EditProductConfigOptions?configItemId=${requestParameters.configItemId}&configOptionId=${question.configOptionId}#edit</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonEdit}</a></td>
          <td><a href="<@ofbizUrl>deleteProductConfigOption?configItemId=${question.configItemId}&configOptionId=${question.configOptionId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonRemove}</a></td>
        </tr>
      </form>
    </#list>
  </table>
  <br/>
  <a name="edit"/>
  <#-- new question / category -->
    <#if configOptionId?has_content>
      <h2>${uiLabelMap.CommonEdit} ${uiLabelMap.ProductConfigOptions}:</h2>
      <a href="<@ofbizUrl>EditProductConfigOptions?configItemId=${requestParameters.configItemId}#edit</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonNew} ${uiLabelMap.ProductConfigOptions}</a>
    <#else>
      <h2>${uiLabelMap.CommonCreateNew} ${uiLabelMap.ProductConfigOptions}</h2>
    </#if>
    ${sections.render("CreateConfigOptionForm")}

  <#if (configOption?has_content)>
    <br/>
    <h1>${uiLabelMap.ProductComponents} - ${uiLabelMap.CommonId}: ${configOption.configOptionId?if_exists} - ${configOption.description?if_exists}</h1>
    <table cellspacing="0" class="basic-table">
      <tr class="header-row">
        <td>${uiLabelMap.CommonSequenceNum}</td>
        <td>${uiLabelMap.ProductProduct}</td>
        <td>${uiLabelMap.ProductQuantity}</td>
        <td>&nbsp;</div></td>
        <td>&nbsp;</div></td>
      </tr>
      <#assign rowClass = "2">
      <#list configProducts as component>
        <#assign product = component.getRelatedOne("ProductProduct")>
        <tr valign="middle"<#if rowClass == "1"> class="alternate-row"</#if>>
          <td>${component.sequenceNum?if_exists}</td>
          <td>${component.productId?if_exists} - ${product.internalName?if_exists}</td>
          <td>${component.quantity?if_exists}</td>
          <td><a href="<@ofbizUrl>EditProductConfigOptions?configItemId=${requestParameters.configItemId}&configOptionId=${component.configOptionId}&productId=${component.productId}#edit</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonEdit}</a>
          <td><a href="<@ofbizUrl>deleteProductConfigProduct?configItemId=${requestParameters.configItemId}&configOptionId=${component.configOptionId}&productId=${component.productId}#edit</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonRemove}</a>
        </tr>
        <#-- toggle the row color -->
        <#if rowClass == "2">
            <#assign rowClass = "1">
        <#else>
            <#assign rowClass = "2">
        </#if>
      </#list>
    </table>

    <#if !productConfigProduct?has_content>
      <h2>${uiLabelMap.CommonAddA} ${uiLabelMap.ProductConfigs}:</h2>
    <#else>
      <h2>${uiLabelMap.CommonEdit} ${uiLabelMap.ProductConfigs}:</h2>
      <a href="<@ofbizUrl>EditProductConfigOptions?configItemId=${requestParameters.configItemId}&configOptionId=${productConfigProduct.configOptionId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonNew} ${uiLabelMap.ProductConfigs}</a>
    </#if>
    ${sections.render("CreateConfigProductForm")}
  </#if>