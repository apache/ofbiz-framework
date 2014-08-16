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
<#if productPromoId??>
    <div class="screenlet">
        <div class="screenlet-title-bar">
            <h3>${uiLabelMap.ProductPromotionUploadSetOfPromotionCodes}</h3>
        </div>
        <div class="screenlet-body">
            <form method="post" action="<@ofbizUrl>createBulkProductPromoCode</@ofbizUrl>" enctype="multipart/form-data">
                <input type="hidden" name="productPromoId" value="${productPromoId}"/>
                <span class="label">${uiLabelMap.ProductPromoUserEntered}:</span>
                    <select name="userEntered">
                        <option value="Y">${uiLabelMap.CommonY}</option>
                        <option value="N">${uiLabelMap.CommonN}</option>
                    </select>
                <span class="label">${uiLabelMap.ProductPromotionReqEmailOrParty}:</span>
                    <select name="requireEmailOrParty">
                        <option value="N">${uiLabelMap.CommonN}</option>
                        <option value="Y">${uiLabelMap.CommonY}</option>
                    </select>
                <span class="label">${uiLabelMap.ProductPromotionUseLimits}:
                ${uiLabelMap.ProductPromotionPerCode}</span><input type="text" size="5" name="useLimitPerCode" />
                <span class="label">${uiLabelMap.ProductPromotionPerCustomer}</span><input type="text" size="5" name="useLimitPerCustomer" />
                <div>
                  <input type="file" size="40" name="uploadedFile" />
                  <input type="submit" value="${uiLabelMap.CommonUpload}" />
                </div>
            </form>
        </div>
    </div>
    <br />
    <div class="screenlet">
        <div class="screenlet-title-bar">
            <h3>${uiLabelMap.ProductPromotionAddSetOfPromotionCodes}</h3>
        </div>
        <div class="screenlet-body">
            <form method="post" action="<@ofbizUrl>createProductPromoCodeSet</@ofbizUrl>">
                <input type="hidden" name="productPromoId" value="${productPromoId}"/>
                <span class="label">${uiLabelMap.CommonQuantity}:</span><input type="text" size="5" name="quantity" />
                <span class="label">${uiLabelMap.ProductPromoCodeLength}:</span><input type="text" size="12" name="codeLength" />
                <span class="label">${uiLabelMap.ProductPromoCodeLayout}:</span>
                    <select name="promoCodeLayout">
                        <option value="smart">${uiLabelMap.ProductPromoLayoutSmart}</option>
                        <option value="normal">${uiLabelMap.ProductPromoLayoutNormal}</option>
                        <option value="sequence">${uiLabelMap.ProductPromoLayoutSeqNum}</option>
                    </select>
                <span class="tooltip">${uiLabelMap.ProductPromoCodeLayoutTooltip}</span>
                <br />
                <span class="label">${uiLabelMap.ProductPromoUserEntered}:</span>
                    <select name="userEntered">
                        <option value="Y">${uiLabelMap.CommonY}</option>
                        <option value="N">${uiLabelMap.CommonN}</option>
                    </select>
                <span class="label">${uiLabelMap.ProductPromotionReqEmailOrParty}:</span>
                    <select name="requireEmailOrParty">
                        <option value="N">${uiLabelMap.CommonN}</option>
                        <option value="Y">${uiLabelMap.CommonY}</option>
                    </select>
                <span class="label">${uiLabelMap.ProductPromotionUseLimits}:
                ${uiLabelMap.ProductPromotionPerCode}</span><input type="text" size="5" name="useLimitPerCode" />
                <span class="label">${uiLabelMap.ProductPromotionPerCustomer}</span><input type="text" size="5" name="useLimitPerCustomer" />
                <input type="submit" value="${uiLabelMap.CommonAdd}" />
            </form>
        </div>
    </div>
</#if>