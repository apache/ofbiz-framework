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

<#if "Y" == requestParameters.lookupFlag?default("N")>
    <#if selectedFeatures?has_content>
<div class="screenlet">
  <div class="screenlet-title-bar">
    <ul>
      <li class="h3">${uiLabelMap.ManufacturingSelectedFeatures}</li>
    </ul>
    <br class="clear"/>
  </div>
  <div class="screenlet-body">
      <#list selectedFeatures as selectedFeature>
         <p>${selectedFeature.productFeatureTypeId} = ${selectedFeature.description!} [${selectedFeature.productFeatureId}]</p>
       </#list>
<#else>
<div class="screenlet">
  <div class="screenlet-title-bar">
    <ul>
      <li class="h3">${uiLabelMap.ManufacturingBomSimulation}</li>
    </ul>
    <br class="clear"/>
  </div>
  <div class="screenlet-body">
</#if>
      <table class="basic-table" cellspacing="0">
        <tr class="header-row">
          <td width="10%">${uiLabelMap.ManufacturingProductLevel}</td>
          <td width="20%">${uiLabelMap.ProductProductId}</td>
          <td width="10%">${uiLabelMap.ManufacturingProductVirtual}</td>
          <td width="40%">${uiLabelMap.ProductProductName}</td>
          <td width="10%" align="right">${uiLabelMap.CommonQuantity}</td>
          <td width="10%" align="right">&nbsp;</td>
        </tr>
        <#if tree?has_content>
          <#assign alt_row = false>
          <#list tree as node>
            <tr valign="middle"<#if alt_row> class="alternate-row"</#if>>
              <td>
              <table cellspacing="1">
              <tr>
              <td>${node.depth}</td>
              <#list 0..(node.depth) as level>
              <td bgcolor="red">&nbsp;&nbsp;</td>
              </#list>
              </tr>
              </table>
              </td>
              <td>
              <table cellspacing="1">
              <tr>
              <#list 0..(node.depth) as level>
              <td>&nbsp;&nbsp;</td>
              </#list>
              <td>
                ${node.product.productId}
              </td>
              </tr>
              </table>
              </td>
              <td>
                <#if "Y" == node.product.isVirtual?default("N")>
                    ${node.product.isVirtual}
                </#if>
                ${(node.ruleApplied.ruleId)!}
              </td>
              <td>${node.product.internalName?default("&nbsp;")}</td>
              <td align="right">${node.quantity}</td>
              <td align="right"><a href="<@ofbizUrl>EditProductBom?productId=${(node.product.productId)!}&amp;productAssocTypeId=${(node.bomTypeId)!}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonEdit}</a></td>
            </tr>
            <#-- toggle the row color -->
            <#assign alt_row = !alt_row>
          </#list>
        <#else>
            <tr>
              <td colspan="6">${uiLabelMap.CommonNoElementFound}.</td>
            </tr>
        </#if>
      </table>
      <br />
      <table class="basic-table" cellspacing="0">
        <tr class="header-row">
          <td width="20%">${uiLabelMap.ProductProductId}</td>
          <td width="50%">${uiLabelMap.ProductProductName}</td>
          <td width="6%" align="right">${uiLabelMap.CommonQuantity}</td>
          <td width="6%" align="right">${uiLabelMap.ProductQoh}</td>
          <td width="6%" align="right">${uiLabelMap.ProductWeight}</td>
          <td width="6%" align="right">${uiLabelMap.FormFieldTitle_cost}</td>
          <td width="6%" align="right">${uiLabelMap.CommonTotalCost}</td>
        </tr>
        <#if productsData?has_content>
          <#assign alt_row = false>
          <#list productsData as productData>
            <#assign node = productData.node>
            <tr valign="middle"<#if alt_row> class="alternate-row"</#if>>
              <td><a href="/catalog/control/EditProduct?productId=${node.product.productId}${StringUtil.wrapString(externalKeyParam)}" class="buttontext">${node.product.productId}</a></td>
              <td>${node.product.internalName?default("&nbsp;")}</td>
              <td align="right">${node.quantity}</td>
              <td align="right">${productData.qoh!}</td>
              <td align="right">${node.product.productWeight!}</td>
              <#if productData.unitCost?? && (productData.unitCost > 0)>
              <td align="right">${productData.unitCost!}</td>
              <#else>
              <td align="right"><a href="/catalog/control/EditProductCosts?productId=${node.product.productId}${StringUtil.wrapString(externalKeyParam)}" class="buttontext">NA</a></td>
              </#if>
              <td align="right">${productData.totalCost!}</td>
            </tr>
            <#-- toggle the row color -->
            <#assign alt_row = !alt_row>
          </#list>
        <#else>
          <tr>
            <td colspan="6">${uiLabelMap.CommonNoElementFound}.</td>
          </tr>
        </#if>
      </table>
  </div>
</div>
</#if>
