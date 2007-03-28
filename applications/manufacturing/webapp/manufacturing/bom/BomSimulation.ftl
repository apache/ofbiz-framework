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

<#if requestParameters.lookupFlag?default("N") == "Y">

<#if selectedFeatures?has_content>
<hr/>
<h2>${uiLabelMap.ManufacturingSelectedFeatures}</h2>
<#list selectedFeatures as selectedFeature>
    <p>${selectedFeature.productFeatureTypeId} = ${selectedFeature.description?if_exists} [${selectedFeature.productFeatureId}]</p>
</#list>
</#if>
<hr/>
      <table border='0' cellspacing='0' cellpadding='2' class='boxbottom'>
        <tr>
          <th width="10%" align="left">${uiLabelMap.ManufacturingProductLevel}</th>
          <th width="20%" align="left">${uiLabelMap.ProductProductId}</th>
          <th width="10%" align="left">&nbsp;</th>
          <th width="40%" align="left">${uiLabelMap.ProductProductName}</th>
          <th width="10%" align="right">${uiLabelMap.CommonQuantity}</th>
          <th width="10%" align="right">&nbsp;</th>
        </tr>
        <tr>
          <td colspan="6"><hr/></td>
        </tr>
        <#if tree?has_content>
          <#assign rowClass = "viewManyTR2">
          <#list tree as node>
            <tr class="${rowClass}">
              <td>
              <table cellspacing="1"><tr>
              <td>${node.depth}</td>
              <#list 0..(node.depth) as level>
              <td bgcolor="red">&nbsp;&nbsp;</td>
              </#list>
              </tr>
              </table>
              </td>
              <td>${node.product.productId}</td>
              <td>
                <#if node.product.isVirtual?default("N") == "Y">
                    Virtual
                </#if>
                ${(node.ruleApplied.ruleId)?if_exists}
              </td>
              <td>${node.product.internalName?default("&nbsp;")}</td>
              <td align="right">${node.quantity}</td>
              <td align="right"><a href="<@ofbizUrl>EditProductBom?productId=${(node.product.productId)?if_exists}&productAssocTypeId=${(node.bomTypeId)?if_exists}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonEdit}</a></td>
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
            <th colspan="4">${uiLabelMap.CommonNoElementFound}.</th>
          </tr>        
        </#if>
      </table>
<hr/>
<hr/>
      <table border='0' cellspacing='0' cellpadding='2' class='boxbottom'>
        <tr>
          <th width="18%" align="left">${uiLabelMap.ProductProductId}</th>
          <th width="50%" align="left">${uiLabelMap.ProductProductName}</th>
          <th width="8%" align="right">${uiLabelMap.CommonQuantity}</th>
          <th width="8%" align="right">${uiLabelMap.ProductQoh}</th>
          <th width="8%" align="right">${uiLabelMap.FormFieldTitle_cost}</th>
          <th width="8%" align="right">${uiLabelMap.CommonTotalCost}</th>
        </tr>
        <tr>
          <td colspan="6"><hr/></td>
        </tr>
        <#if productsData?has_content>
          <#assign rowClass = "viewManyTR2">
          <#list productsData as productData>
            <#assign node = productData.node>
            <tr class='${rowClass}'>
              <td><a href="/catalog/control/EditProduct?productId=${node.product.productId}" class="buttontext">${node.product.productId}</a></td>
              <td>${node.product.internalName?default("&nbsp;")}</td>
              <td align="right">${node.quantity}</td>
              <td align="right">${productData.qoh?if_exists}</td>
              <#if productData.unitCost?exists && (productData.unitCost > 0)>
              <td align="right">${productData.unitCost?if_exists}</td>
              <#else>
              <td align="center"><a href="/catalog/control/EditProductCosts?productId=${node.product.productId}" class="buttontext">NA</a></td>
              </#if>
              <td align="right">${productData.totalCost?if_exists}</td>
            </tr>
            <#-- toggle the row color -->
            <#if rowClass == "viewManyTR2">
              <#assign rowClass = "viewManyTR1">
            <#else>
              <#assign rowClass = "viewManyTR2">
            </#if>
          </#list>
          <#--
          <#if grandTotalCost?exists>
          <tr>
            <th colspan="6" align="right">${grandTotalCost}</th>
          </tr>
          </#if>
          -->
        <#else>
          <tr>
            <td colspan="6"><h3>${uiLabelMap.CommonNoElementFound}.</h3></td>
          </tr>
        </#if>
      </table>
</#if>
