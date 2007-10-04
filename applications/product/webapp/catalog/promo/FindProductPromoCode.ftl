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
<#if productPromoId?exists>
    <h3>${uiLabelMap.ProductPromotionUploadSetOfPromotionCodes}:</h3>
    <div class="tabletext">
        <form method="post" action="<@ofbizUrl>createBulkProductPromoCode</@ofbizUrl>" enctype="multipart/form-data" style="margin: 0;">
            <input type="hidden" name="productPromoId" value="${productPromoId}"/>            
            ${uiLabelMap.ProductPromoUserEntered}: <select name="userEntered" class="selectBox"><option>N</option><option>Y</option></select>
            ${uiLabelMap.ProductPromotionReqEmailOrParty}: <select name="requireEmailOrParty" class="selectBox"><option>N</option><option>Y</option></select>
            ${uiLabelMap.ProductPromotionUseLimits}:
            ${uiLabelMap.ProductPromotionPerCode}<input type="text" size="5" name="useLimitPerCode" class="inputBox">
            ${uiLabelMap.ProductPromotionPerCustomer}<input type="text" size="5" name="useLimitPerCustomer" class="inputBox">
            <div>
              <input type="file" size="40" name="uploadedFile" class="inputBox">
              <input type="submit" value="${uiLabelMap.CommonUpload}">
            </div>
        </form>
    </div>
    <br/>

    <h3>${uiLabelMap.ProductPromotionAddSetOfPromotionCodes}:</h3>
    <div class="tabletext">
        <form method="post" action="<@ofbizUrl>createProductPromoCodeSet</@ofbizUrl>" style="margin: 0;">            
            <input type="hidden" name="productPromoId" value="${productPromoId}"/>
            ${uiLabelMap.CommonQuantity}: <input type="text" size="5" name="quantity" class="inputBox">
            ${uiLabelMap.ProductPromoUserEntered}: <select name="userEntered" class="selectBox"><option>N</option><option>Y</option></select>
            ${uiLabelMap.ProductPromotionReqEmailOrParty}: <select name="requireEmailOrParty" class="selectBox"><option>N</option><option>Y</option></select>
            ${uiLabelMap.ProductPromotionUseLimits}:
            ${uiLabelMap.ProductPromotionPerCode}<input type="text" size="5" name="useLimitPerCode" class="inputBox">
            ${uiLabelMap.ProductPromotionPerCustomer}<input type="text" size="5" name="useLimitPerCustomer" class="inputBox">
            <input type="submit" value="${uiLabelMap.CommonAdd}">
        </form>
    </div>
</#if>
    
