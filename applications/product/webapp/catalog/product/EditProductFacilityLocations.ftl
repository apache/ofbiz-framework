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

<#if productId?exists && product?exists>
    <table border="1" cellpadding="2" cellspacing="0">
    <tr>
        <td><div class="tabletext"><b>${uiLabelMap.ProductFacility}</b></div></td>
        <td><div class="tabletext"><b>${uiLabelMap.ProductLocation}</b></div></td>
        <td align="center"><div class="tabletext"><b>${uiLabelMap.ProductMinimumStockAndMoveQuantity}</b></div></td>
        <td><div class="tabletext"><b>&nbsp;</b></div></td>
    </tr>
    <#list productFacilityLocations as productFacilityLocation>
        <#assign facility = productFacilityLocation.getRelatedOneCache("Facility")>
        <#assign facilityLocation = productFacilityLocation.getRelatedOne("FacilityLocation")?if_exists>
        <#assign facilityLocationTypeEnum = (facilityLocation.getRelatedOneCache("TypeEnumeration"))?if_exists>
        <tr valign="middle">
            <td><div class="tabletext"><#if facility?exists>${facility.facilityName}<#else>[${productFacilityLocation.facilityId}]</#if></div></td>
            <td><div class="tabletext"><#if facilityLocation?exists>${facilityLocation.areaId?if_exists}:${facilityLocation.aisleId?if_exists}:${facilityLocation.sectionId?if_exists}:${facilityLocation.levelId?if_exists}:${facilityLocation.positionId?if_exists}</#if><#if facilityLocationTypeEnum?has_content>(${facilityLocationTypeEnum.description})</#if>[${productFacilityLocation.locationSeqId}]</div></td>
            <td align="center">
                <form method="post" action="<@ofbizUrl>updateProductFacilityLocation</@ofbizUrl>" name="lineForm${productFacilityLocation_index}">
                    <input type="hidden" name="productId" value="${(productFacilityLocation.productId)?if_exists}"/>
                    <input type="hidden" name="facilityId" value="${(productFacilityLocation.facilityId)?if_exists}"/>
                    <input type="hidden" name="locationSeqId" value="${(productFacilityLocation.locationSeqId)?if_exists}"/>
                    <input type="text" size="10" name="minimumStock" value="${(productFacilityLocation.minimumStock)?if_exists}" class="inputBox"/>
                    <input type="text" size="10" name="moveQuantity" value="${(productFacilityLocation.moveQuantity)?if_exists}" class="inputBox"/>
                    <input type="submit" value="${uiLabelMap.CommonUpdate}" style="font-size: x-small;"/>
                </form>
            </td>
            <td align="center">
            <a href="<@ofbizUrl>deleteProductFacilityLocation?productId=${(productFacilityLocation.productId)?if_exists}&facilityId=${(productFacilityLocation.facilityId)?if_exists}&locationSeqId=${(productFacilityLocation.locationSeqId)?if_exists}</@ofbizUrl>" class="buttontext">
            [${uiLabelMap.CommonDelete}]</a>
            </td>
        </tr>
    </#list>
    </table>
    <br/>
    <form method="post" action="<@ofbizUrl>createProductFacilityLocation</@ofbizUrl>" style="margin: 0;" name="createProductFacilityLocationForm">
        <input type="hidden" name="productId" value="${productId?if_exists}">
        <input type="hidden" name="useValues" value="true">
        <div class="head2">${uiLabelMap.CommonAdd} ${uiLabelMap.ProductFacilityLocation}:</div>
        <div class="tabletext">
            ${uiLabelMap.ProductFacility}:
            <select name="facilityId" class="selectBox">
                <#list facilities as facility>
                    <option value="${(facility.facilityId)?if_exists}">${(facility.facilityName)?if_exists}</option>
                </#list>
            </select>
            ${uiLabelMap.ProductLocationSeqId}:&nbsp;<input type="text" size="10" name="locationSeqId" class="inputBox"/>
            <span class="tabletext">
              <a href="javascript:call_fieldlookup2(document.createProductFacilityLocationForm.locationSeqId,'LookupFacilityLocation');">
                <img src="/images/fieldlookup.gif" width="15" height="14" border="0" alt="Click here For Field Lookup"/>
              </a>
            </span>
            ${uiLabelMap.ProductMinimumStock}:&nbsp;<input type="text" size="10" name="minimumStock" class="inputBox"/>
            ${uiLabelMap.ProductMoveQuantity}:&nbsp;<input type="text" size="10" name="moveQuantity" class="inputBox"/>
            <input type="submit" value="${uiLabelMap.CommonAdd}" style="font-size: x-small;"/>
        </div>
    </form>
</#if>    
