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

<#assign productFeaturesByTypeMap = Static["org.ofbiz.product.feature.ParametricSearch"].makeCategoryFeatureLists(productCategoryId, delegator)>

<#if productCategoryId?has_content>
    <a href="<@ofbizUrl>EditCategory?productCategoryId=${productCategoryId}</@ofbizUrl>" class="buttontext">[${uiLabelMap.ProductBackToEditCategory}]</a>
</#if>

<form name="createProductInCategoryCheckExistingForm" method="post" action="<@ofbizUrl>createProductInCategoryCheckExisting</@ofbizUrl>" style="margin: 0;">
    <input type="hidden" name="productCategoryId" value="${productCategoryId}">
    <table cellpadding="1" cellspacing="0" border="1">
        <#list productFeaturesByTypeMap.keySet() as productFeatureTypeId>
            <#assign findPftMap = Static["org.ofbiz.base.util.UtilMisc"].toMap("productFeatureTypeId", productFeatureTypeId)>
            <#assign productFeatureType = delegator.findByPrimaryKeyCache("ProductFeatureType", findPftMap)>
            <#assign productFeatures = productFeaturesByTypeMap[productFeatureTypeId]>
            <tr>
                <td width="15%">
                    <div class="tabletext">${productFeatureType.description}:</div>
                </td>
                <td>
                    <div class="tabletext">
                        <select class="selectBox" name="pft_${productFeatureTypeId}">
                            <option value="">- ${uiLabelMap.CommonNone} -</option>
                            <#list productFeatures as productFeature>
                                <option value="${productFeature.productFeatureId}">${productFeature.description}</option>
                            </#list>
                        </select>
                        <input type="checkbox" name="pftsel_${productFeatureTypeId}"/>${uiLabelMap.ProductSelectable}
                    </div>
                </td>
            </tr>
        </#list>
        <tr>
            <td width="15%"><div class="tabletext">${uiLabelMap.ProductInternalName}:</div></td>
            <td><input type="text" name="internalName" size="30" maxlength="60" class="inputBox"/></td>
        </tr>
        <tr>
            <td width="15%"><div class="tabletext">${uiLabelMap.ProductProductName}:</div></td>
            <td><input type="text" name="productName" size="30" maxlength="60" class="inputBox"/></td>
        </tr>
        <tr>
            <td width="15%"><div class="tabletext">${uiLabelMap.ProductShortDescription}:</div></td>
            <td><input type="text" name="description" size="60" maxlength="250" class="inputBox"/></td>
        </tr>
        <tr>
            <td width="15%"><div class="tabletext">${uiLabelMap.ProductDefaultPrice}:</div></td>
            <td><input type="text" name="defaultPrice" size="8" class="inputBox"/></td>
        </tr>
        <tr>
            <td width="15%"><div class="tabletext">${uiLabelMap.ProductAverageCost}:</div></td>
            <td><input type="text" name="averageCost" size="8" class="inputBox"/></td>
        </tr>
        <tr>
            <td colspan="3">
                <a href="javascript:document.createProductInCategoryCheckExistingForm.submit()" class="buttontext">${uiLabelMap.ProductCheckExisting}</a>
            </td>
        </tr>
    </table>
</form>
