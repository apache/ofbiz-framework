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

<#if (requestAttributes.uiLabelMap)?exists><#assign uiLabelMap = requestAttributes.uiLabelMap></#if>
    
<br/>
<div class="tabletext">[${product.productId}] ${product.internalName}</div>
<br/>
<#if searchFeatures?has_content>
    <table border="1" cellpadding="2" cellspacing="0">
        <form method="post" action="<@ofbizUrl>LookupVariantProduct</@ofbizUrl>" name="selectAllForm">
        <input type="hidden" name="productId" value="${product.productId}">
        <#list searchFeatures as searchFeature>
            <tr>
                <td><div class="tabletext"><b>${searchFeature.featureType}</b></div></td>
                <td><div class="tabletext">
                    <select name="${searchFeature.featureType}" class="selectBox">
                    <#assign features = searchFeature.features>
                    <option value=""></option>
                    <#list features as feature>
                        <#if searchFeature.selectedFeatureId?has_content && searchFeature.selectedFeatureId == feature.productFeatureId>
                            <option value="${feature.productFeatureId}" selected>${feature.get("description",locale)}</option>
                        <#else>
                            <option value="${feature.productFeatureId}">${feature.get("description",locale)}</option>
                        </#if>
                    </#list>
                    </select>
                    </div>
                </td>
            </tr>
        </#list>
        <tr>
            <td><input type="submit" value="${uiLabelMap.CommonSearch}" class="smallSubmit"></td>
        </form>
        <form method="post" action="<@ofbizUrl>LookupProduct</@ofbizUrl>" name="">
            <td><input type="submit" value="${uiLabelMap.CommonBack}" class="smallSubmit"></td>
        </form>
    </tr>
    </table>
</#if>
<br/>
<#if variantProducts?exists>
    <table border="1" cellpadding="2" cellspacing="0">
        <tr>
            <th>&nbsp;</th>
            <th><div class="tabletext">${uiLabelMap.ProductBrandName}</div></th>
            <th><div class="tabletext">${uiLabelMap.ProductInternalName}</div></th>
        </tr>
        <#list variantProducts as variant>
            <tr>
                <td><a class="buttontext" href="javascript:set_value('${variant.productId}')">${variant.productId}</a></td>
                <td><div class="tabletext">${variant.brandName?if_exists}</div></td>
                <td><div class="tabletext">${variant.internalName?if_exists}</div></td>
            </tr>
        </#list>
    </table>
</#if>
<#if productFeatureIds?exists>
    <table border="1" cellpadding="2" cellspacing="0">
        <form method="post" action="<@ofbizUrl>LookupVariantProduct</@ofbizUrl>" name="createNewVariant">
        <input type="hidden" name="productId" value="${product.productId}">
        <input type="hidden" name="productFeatureIds" value="${productFeatureIds}">
        <input type="text" name="productVariantId" value="${productVariantId}" class="inputBox">
        <input type="submit" value="${uiLabelMap.ProductQuickAddVariants}" class="smallSubmit">
        </form>
    </table>
</#if>
