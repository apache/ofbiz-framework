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
<h1>${title}</h1>
<#if facilityId?exists && locationSeqId?exists>
  <div class="button-bar">
    <a href="<@ofbizUrl>EditFacility</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductNewFacility}</a>
    <a href="<@ofbizUrl>EditFacilityLocation?facilityId=${facilityId?if_exists}</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductNewFacilityLocation}</a>
    <a href="<@ofbizUrl>EditInventoryItem?facilityId=${facilityId}&amp;locationSeqId=${locationSeqId}</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductNewInventoryItem}</a>
    <#assign latestGeoPoint= Static["org.ofbiz.common.geo.GeoWorker"].findLatestGeoPoint(delegator, "FacilityLocationAndGeoPoint", "facilityId", facilityId, "locationSeqId", locationSeqId)?if_exists/>
    <#if latestGeoPoint?has_content>
      <a href="<@ofbizUrl>FacilityLocationGeoLocation?facilityId=${facilityId}&amp;locationSeqId=${locationSeqId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonGeoLocation}</a>
    </#if>
  </div>
</#if>

<#if facilityId?exists && !(facilityLocation?exists)>
    <form action="<@ofbizUrl>CreateFacilityLocation</@ofbizUrl>" method="post">
    <input type="hidden" name="facilityId" value="${facilityId}" />
    <table class="basic-table" cellspacing="0">
<#elseif facilityLocation?exists>
    <form action="<@ofbizUrl>UpdateFacilityLocation</@ofbizUrl>" method="post">
    <input type="hidden" name="facilityId" value="${facilityId?if_exists}" />
    <input type="hidden" name="locationSeqId" value="${locationSeqId}" />
    <table class="basic-table" cellspacing="0">
    <tr>
        <td class="label">${uiLabelMap.ProductFacilityId}</td>
        <td>${facilityId?if_exists}</td>
    </tr>
    <tr>
        <td class="label">${uiLabelMap.ProductLocationSeqId}</td>
        <td>${locationSeqId}</td>
    </tr>
<#else>
    <h1>${uiLabelMap.ProductNotCreateLocationFacilityId}</h1>
</#if>

<#if facilityId?exists>
    <tr>
        <td class="label">${uiLabelMap.ProductType}</td>
        <td>
            <select name="locationTypeEnumId">
                <#if (facilityLocation.locationTypeEnumId)?has_content>
                    <#assign locationTypeEnum = facilityLocation.getRelatedOneCache("TypeEnumeration")?if_exists>
                    <option value="${facilityLocation.locationTypeEnumId}">${(locationTypeEnum.get("description",locale))?default(facilityLocation.locationTypeEnumId)}</option>
                    <option value="${facilityLocation.locationTypeEnumId}">----</option>
                </#if>
                <#list locationTypeEnums as locationTypeEnum>
                    <option value="${locationTypeEnum.enumId}">${locationTypeEnum.get("description",locale)}</option>
                </#list>
            </select>
        </td>
    </tr>
    <tr>
        <td class="label">${uiLabelMap.CommonArea}</td>
        <td><input type="text" name="areaId" value="${(facilityLocation.areaId)?if_exists}" size="19" maxlength="20" /></td>
    </tr>
    <tr>
        <td class="label">${uiLabelMap.ProductAisle}</td>
        <td><input type="text" name="aisleId" value="${(facilityLocation.aisleId)?if_exists}" size="19" maxlength="20" /></td>
    </tr>
    <tr>
        <td class="label">${uiLabelMap.ProductSection}</td>
        <td><input type="text" name="sectionId" value="${(facilityLocation.sectionId)?if_exists}" size="19" maxlength="20" /></td>
    </tr>
    <tr>
        <td class="label">${uiLabelMap.ProductLevel}</td>
        <td><input type="text" name="levelId" value="${(facilityLocation.levelId)?if_exists}" size="19" maxlength="20" /></td>
    </tr>
    <tr>
        <td class="label">${uiLabelMap.ProductPosition}</td>
        <td><input type="text" name="positionId" value="${(facilityLocation.positionId)?if_exists}" size="19" maxlength="20" /></td>
    </tr>
    <tr>
        <td>&nbsp;</td>
        <#if locationSeqId?exists>
          <td><input type="submit" value="${uiLabelMap.CommonUpdate}" /></td>
        <#else>
          <td><input type="submit" value="${uiLabelMap.CommonSave}" /></td>
        </#if>
    </tr>
  </table>
  </form>
  <#if locationSeqId?exists>
  <br />
  <div class="screenlet">
    <div class="screenlet-title-bar">
        <h3>${uiLabelMap.ProductLocationProduct}</h3>
    </div>
    <div class="screenlet-body">
        <#-- ProductFacilityLocation stuff -->
        <table class="basic-table hover-bar" cellspacing="0">
        <tr class="header-row">
            <td>${uiLabelMap.ProductProduct}</td>
            <td>${uiLabelMap.ProductMinimumStockAndMoveQuantity}</td>
        </tr>
        <#list productFacilityLocations?if_exists as productFacilityLocation>
            <#assign product = productFacilityLocation.getRelatedOne("Product")?if_exists>
            <tr>
                <td><#if product?exists>${(product.internalName)?if_exists}</#if>[${productFacilityLocation.productId}]</td>
                <td>
                    <form method="post" action="<@ofbizUrl>updateProductFacilityLocation</@ofbizUrl>" id="lineForm${productFacilityLocation_index}">
                        <input type="hidden" name="productId" value="${(productFacilityLocation.productId)?if_exists}"/>
                        <input type="hidden" name="facilityId" value="${(productFacilityLocation.facilityId)?if_exists}"/>
                        <input type="hidden" name="locationSeqId" value="${(productFacilityLocation.locationSeqId)?if_exists}"/>
                        <input type="text" size="10" name="minimumStock" value="${(productFacilityLocation.minimumStock)?if_exists}"/>
                        <input type="text" size="10" name="moveQuantity" value="${(productFacilityLocation.moveQuantity)?if_exists}"/>
                        <input type="submit" value="${uiLabelMap.CommonUpdate}"/>
                        <a href="javascript:document.getElementById('lineForm${productFacilityLocation_index}').action='<@ofbizUrl>deleteProductFacilityLocation</@ofbizUrl>';document.getElementById('lineForm${productFacilityLocation_index}').submit();" class="buttontext">${uiLabelMap.CommonDelete}</a>
                    </form>
                </td>
            </tr>
        </#list>
        </table>
    </div>
  </div>
  <div class="screenlet">
    <div class="screenlet-title-bar">
        <h3>${uiLabelMap.ProductAddProduct}</h3>
    </div>
    <div class="screenlet-body">
        <form method="post" action="<@ofbizUrl>createProductFacilityLocation</@ofbizUrl>" style="margin: 0;" name="createProductFacilityLocationForm">
            <input type="hidden" name="facilityId" value="${facilityId?if_exists}" />
            <input type="hidden" name="locationSeqId" value="${locationSeqId?if_exists}" />
            <input type="hidden" name="useValues" value="true" />
            <span class="label">${uiLabelMap.ProductProductId}</span><input type="text" size="10" name="productId" />
            <span class="label">${uiLabelMap.ProductMinimumStock}</span><input type="text" size="10" name="minimumStock" />
            <span class="label">${uiLabelMap.ProductMoveQuantity}</span><input type="text" size="10" name="moveQuantity" />
            <input type="submit" value="${uiLabelMap.CommonAdd}" />
        </form>
    </div>
  </div>
  </#if>
</#if>

