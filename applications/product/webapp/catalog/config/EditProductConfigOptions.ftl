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

  <table border="1" cellpadding='2' cellspacing='0'>
    <tr>
      <td><div class="tableheadtext">${uiLabelMap.CommonName}</div></td>
      <td><div class="tableheadtext">${uiLabelMap.CommonSequenceNum}</div></td>
      <td><div class="tableheadtext">${uiLabelMap.CommonDescription}</div></td>
      <td><div class="tableheadtext">&nbsp;</div></td>
      <td><div class="tableheadtext">&nbsp;</div></td>
      <td><div class="tableheadtext">&nbsp;</div></td>
    </tr>
    <#list configOptionList as question>
      <form method="post" action="<@ofbizUrl>updateProductConfigOption</@ofbizUrl>">
        <input type="hidden" name="configItemId" value="${question.configItemId}">
        <input type="hidden" name="configOptionId" value="${question.configOptionId}">
        <tr valign="middle">
          <td><div class="tabletext">${question.configOptionId} - ${question.configOptionName?if_exists}</div></td>
          <td><input type="text" name="sequenceNum" size="3" class="textBox" value="${question.sequenceNum?if_exists}">
          <td><div class="tabletext">${question.description?if_exists}</div></td>
          <td><input type="submit" value="${uiLabelMap.CommonUpdate}">
          <td><a href="<@ofbizUrl>EditProductConfigOptions?configItemId=${requestParameters.configItemId}&configOptionId=${question.configOptionId}#edit</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonEdit}]</a>
          <td><a href="<@ofbizUrl>deleteProductConfigOption?configItemId=${question.configItemId}&configOptionId=${question.configOptionId}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonRemove}]</a>
        </tr>
      </form>
    </#list>
  </table>
  <br/>

  <hr class="sepbar">
  <a name="edit"/>
  <#-- new question / category -->

    <#if configOptionId?has_content>
      <div class="head2">${uiLabelMap.CommonEdit} ${uiLabelMap.ProductConfigOptions}:</div>
      <a href="<@ofbizUrl>EditProductConfigOptions?configItemId=${requestParameters.configItemId}#edit</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonNew} ${uiLabelMap.ProductConfigOptions}]</a>
    <#else>
      <div class="head2">${uiLabelMap.CommonCreateNew} ${uiLabelMap.ProductConfigOptions}</div>
    </#if>
    ${sections.render("CreateConfigOptionForm")}

  <#if (configOption?has_content)>
    <br/>
    <hr class="sepbar">
    <div class="head1">${uiLabelMap.ProductComponents} - <span class="head2">${uiLabelMap.CommonId}: ${configOption.configOptionId?if_exists} - ${configOption.description?if_exists}</div>
    <table border="1" cellpadding='2' cellspacing='0'>
      <tr>
        <td><div class="tableheadtext">${uiLabelMap.CommonSequenceNum}</div></td>
        <td><div class="tableheadtext">${uiLabelMap.ProductProduct}</div></td>
        <td><div class="tableheadtext">${uiLabelMap.ProductQuantity}</div></td>
        <td><div class="tableheadtext">&nbsp;</div></td>
        <td><div class="tableheadtext">&nbsp;</div></td>
      </tr>

      <#list configProducts as component>
        <#assign product = component.getRelatedOne("ProductProduct")>
        <tr valign="middle">
          <td><div class="tabletext">${component.sequenceNum?if_exists}</div></td>
          <td><div class="tabletext">${component.productId?if_exists} - ${product.internalName?if_exists}</div></td>
          <td><div class="tabletext">${component.quantity?if_exists}</div></td>
          <td><a href="<@ofbizUrl>EditProductConfigOptions?configItemId=${requestParameters.configItemId}&configOptionId=${component.configOptionId}&productId=${component.productId}#edit</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonEdit}]</a>
          <td><a href="<@ofbizUrl>deleteProductConfigProduct?configItemId=${requestParameters.configItemId}&configOptionId=${component.configOptionId}&productId=${component.productId}#edit</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonRemove}]</a>
        </tr>
      </#list>
    </table>

    <#if !productConfigProduct?has_content>
      <div class="head2">${uiLabelMap.CommonAddA} ${uiLabelMap.ProductConfigs}:</div>
    <#else>
      <div class="head2">${uiLabelMap.CommonEdit} ${uiLabelMap.ProductConfigs}:</div>
      <a href="<@ofbizUrl>EditProductConfigOptions?configItemId=${requestParameters.configItemId}&configOptionId=${productConfigProduct.configOptionId}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonNew} ${uiLabelMap.ProductConfigs}]</a>
    </#if>
    ${sections.render("CreateConfigProductForm")}
  </#if>
