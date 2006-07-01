<#--

Copyright 2001-2006 The Apache Software Foundation

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.
-->

<#if requestParameters.lookupFlag?default("N") == "Y">

<#if selectedFeatures?has_content>
<hr>
<div class="tableheadtext">${uiLabelMap.ManufacturingSelectedFeatures}</div>
<#list selectedFeatures as selectedFeature>
    <div class="tabletext">${selectedFeature.productFeatureTypeId} = ${selectedFeature.description?if_exists} [${selectedFeature.productFeatureId}]</div>
</#list>
</#if>
<hr>
      <table border='0' cellspacing='0' cellpadding='2' class='boxbottom'>
        <tr>
          <td width="10%" align="left"><div class="tableheadtext">${uiLabelMap.ManufacturingProductLevel}</div></td>
          <td width="20%" align="left"><div class="tableheadtext">${uiLabelMap.ProductProductId}</div></td>
          <td width="10%" align="left"><div class="tableheadtext">---</div></td>
          <td width="40%" align="left"><div class="tableheadtext">${uiLabelMap.ProductProductName}</div></td>
          <td width="20%" align="right"><div class="tableheadtext">${uiLabelMap.CommonQuantity}</div></td>
        </tr>
        <tr>
          <td colspan='5'><hr class='sepbar'></td>
        </tr>
        <#if tree?has_content>
          <#assign rowClass = "viewManyTR2">
          <#list tree as node>            
            <tr class='${rowClass}'>
              <td><img src='/manufacturing/images/depth${node.depth}.gif' height='16' border='0' alt='Depth'></td>
              <td><a href="<@ofbizUrl>EditProductBom?productId=${(node.product.productId)?if_exists}&productAssocTypeId=${(node.bomTypeId)?if_exists}</@ofbizUrl>" class="buttontext">${node.product.productId}</a></td>
              <td>
                <#if node.product.isVirtual?default("N") == "Y">
                    Virtual
                </#if>
                ${(node.ruleApplied.ruleId)?if_exists}
              </td>
              <td>${node.product.internalName?default("&nbsp;")}</td>
              <td align="right">${node.quantity}</td>
            </tr>
            <#-- toggle the row color -->
            <#if rowClass == "viewManyTR2">
              <#assign rowClass = "viewManyTR1">
            <#else>
              <#assign rowClass = "viewManyTR2">
            </#if>
          </#list>          
        <#else>
          <tr>
            <td colspan='4'><div class='head3'>${uiLabelMap.CommonNoElementFound}.</div></td>
          </tr>        
        </#if>
      </table>
<hr>
<hr>
      <table border='0' cellspacing='0' cellpadding='2' class='boxbottom'>
        <tr>
          <td width="20%" align="left"><div class="tableheadtext">${uiLabelMap.ProductProductId}</div></td>
          <td width="40%" align="left"><div class="tableheadtext">${uiLabelMap.ProductProductName}</div></td>
          <td width="40%" align="right"><div class="tableheadtext">${uiLabelMap.CommonQuantity}</div></td>
        </tr>
        <tr>
          <td colspan='3'><hr class='sepbar'></td>
        </tr>
        <#if treeQty?has_content>
          <#assign rowClass = "viewManyTR2">
          <#list treeQty as nodeQty>            
            <tr class='${rowClass}'>
              <td>${nodeQty.product.productId}</td>
              <td>${nodeQty.product.internalName?default("&nbsp;")}</td>
              <td align="right">${nodeQty.quantity}</td>
            </tr>
            <#-- toggle the row color -->
            <#if rowClass == "viewManyTR2">
              <#assign rowClass = "viewManyTR1">
            <#else>
              <#assign rowClass = "viewManyTR2">
            </#if>
          </#list>          
        <#else>
          <tr>
            <td colspan='4'><div class='head3'>${uiLabelMap.CommonNoElementFound}.</div></td>
          </tr>        
        </#if>
      </table>
</#if>
