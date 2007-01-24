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
    <#if productId?exists>
        <hr class="sepbar"/>
        <div class="head2">${uiLabelMap.ProductDuplicateProduct}</div>
        <form action="<@ofbizUrl>DuplicateProduct</@ofbizUrl>" method="post" style="margin: 0;">
            <input type="hidden" name="oldProductId" value="${productId}"/>
            <div>
                <span class="tabletext">${uiLabelMap.ProductDuplicateRemoveSelectedWithNewId}:</span>
                <input type="text" class="inputBox" size="20" maxlength="20" name="productId"/>&nbsp;<input type="submit" class="smallSubmit" value="${uiLabelMap.CommonDuplicate}!"/>
            </div>
            <div class="tabletext">
                <b>${uiLabelMap.CommonDuplicate}:</b>
                ${uiLabelMap.ProductPrices}&nbsp;<input type="checkbox" class="checkBox" name="duplicatePrices" value="Y" checked="checked"/>
                ${uiLabelMap.CommonId}&nbsp;<input type="checkbox" class="checkBox" name="duplicateIDs" value="Y" checked="checked"/>
                ${uiLabelMap.ProductContent}&nbsp;<input type="checkbox" class="checkBox" name="duplicateContent" value="Y" checked="checked"/>
                ${uiLabelMap.ProductCategoryMembers}&nbsp;<input type="checkbox" class="checkBox" name="duplicateCategoryMembers" value="Y" checked="checked"/>
                ${uiLabelMap.ProductAssocs}&nbsp;<input type="checkbox" class="checkBox" name="duplicateAssocs" value="Y" checked="checked"/>
                ${uiLabelMap.ProductAttributes}&nbsp;<input type="checkbox" class="checkBox" name="duplicateAttributes" value="Y" checked="checked"/>
                ${uiLabelMap.ProductFeatureAppls}&nbsp;<input type="checkbox" class="checkBox" name="duplicateFeatureAppls" value="Y" checked="checked"/>
                ${uiLabelMap.ProductInventoryItems}&nbsp;<input type="checkbox" class="checkBox" name="duplicateInventoryItems" value="Y"/>
            </div>
            <div class="tabletext">
                <b>${uiLabelMap.CommonRemove}:</b>
                ${uiLabelMap.ProductPrices}&nbsp;<input type="checkbox" class="checkBox" name="removePrices" value="Y"/>
                ${uiLabelMap.CommonId}&nbsp;<input type="checkbox" class="checkBox" name="removeIDs" value="Y"/>
                ${uiLabelMap.ProductContent}&nbsp;<input type="checkbox" class="checkBox" name="removeContent" value="Y"/>
                ${uiLabelMap.ProductCategoryMembers}&nbsp;<input type="checkbox" class="checkBox" name="removeCategoryMembers" value="Y"/>
                ${uiLabelMap.ProductAssocs}&nbsp;<input type="checkbox" class="checkBox" name="removeAssocs" value="Y"/>
                ${uiLabelMap.ProductAttributes}&nbsp;<input type="checkbox" class="checkBox" name="removeAttributes" value="Y"/>
                ${uiLabelMap.ProductFeatureAppls}&nbsp;<input type="checkbox" class="checkBox" name="removeFeatureAppls" value="Y"/>
                ${uiLabelMap.ProductInventoryItems}&nbsp;<input type="checkbox" class="checkBox" name="removeInventoryItems" value="Y"/>
            </div>
        </form>
        <#if product?exists && product.isVirtual?if_exists == "Y">
        <hr class="sepbar"/>
        <div class="head2">${uiLabelMap.ProductUpdateProductVariants}</div>
        <form action="<@ofbizUrl>UpdateProductVariants?productId=${productId}</@ofbizUrl>" method="post" style="margin: 0;">
            <input type="hidden" name="virtualProductId" value="${productId}"/>
            <div class="tabletext">
                <b>${uiLabelMap.ProductUpdateProductVariants}:</b>
                ${uiLabelMap.ProductRemoveBefore}&nbsp;<input type="checkbox" class="checkBox" name="removeBefore" value="Y"/>
                ${uiLabelMap.ProductPrices}&nbsp;<input type="checkbox" class="checkBox" name="duplicatePrices" value="Y" checked="checked"/>
                ${uiLabelMap.CommonId}&nbsp;<input type="checkbox" class="checkBox" name="duplicateIDs" value="Y" checked="checked"/>
                ${uiLabelMap.ProductContent}&nbsp;<input type="checkbox" class="checkBox" name="duplicateContent" value="Y" checked="checked"/>
                ${uiLabelMap.ProductCategoryMembers}&nbsp;<input type="checkbox" class="checkBox" name="duplicateCategoryMembers" value="Y" checked="checked"/>
                ${uiLabelMap.ProductAttributes}&nbsp;<input type="checkbox" class="checkBox" name="duplicateAttributes" value="Y" checked="checked"/>
                ${uiLabelMap.ProductFacilities}&nbsp;<input type="checkbox" class="checkBox" name="duplicateFacilities" value="Y" checked="checked"/>
                ${uiLabelMap.ProductLocations}&nbsp;<input type="checkbox" class="checkBox" name="duplicateLocations" value="Y" checked="checked"/>
            </div>
            <input type="submit" class="smallSubmit" value="${uiLabelMap.CommonGo}"/>
        </form>
        </#if>
        <br/>
    </#if>
