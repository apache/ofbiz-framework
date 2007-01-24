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
        <td align="center"><div class="tabletext"><b>${uiLabelMap.ProductMinimumStockReorderQuantityDaysToShip}</b></div></td>
        <td><div class="tabletext"><b>&nbsp;</b></div></td>
    </tr>
    <#list productFacilities as productFacility>
        <#assign facility = productFacility.getRelatedOneCache("Facility")>
        <tr valign="middle">
            <td><div class="tabletext"><#if facility?exists>${facility.facilityName}<#else>[${productFacility.facilityId}]</#if></div></td>
            <td align="center">
                <form method="post" action="<@ofbizUrl>updateProductFacility</@ofbizUrl>" name="lineForm${productFacility_index}">
                    <input type="hidden" name="productId" value="${(productFacility.productId)?if_exists}"/>
                    <input type="hidden" name="facilityId" value="${(productFacility.facilityId)?if_exists}"/>
                    <input type="text" size="10" name="minimumStock" value="${(productFacility.minimumStock)?if_exists}" class="inputBox"/>
                    <input type="text" size="10" name="reorderQuantity" value="${(productFacility.reorderQuantity)?if_exists}" class="inputBox"/>
                    <input type="text" size="10" name="daysToShip" value="${(productFacility.daysToShip)?if_exists}" class="inputBox"/>
                    <input type="submit" value="${uiLabelMap.CommonUpdate}" style="font-size: x-small;"/>
                </form>
            </td>
            <td align="center">
                <a href="<@ofbizUrl>deleteProductFacility?productId=${(productFacility.productId)?if_exists}&facilityId=${(productFacility.facilityId)?if_exists}</@ofbizUrl>" class="buttontext">
                [${uiLabelMap.CommonDelete}]</a>
            </td>
        </tr>
    </#list>
    </table>
    <br/>
    <form method="post" action="<@ofbizUrl>createProductFacility</@ofbizUrl>" style="margin: 0;" name="createProductFacilityForm">
        <input type="hidden" name="productId" value="${productId?if_exists}"/>
        <input type="hidden" name="useValues" value="true"/>
    
        <div class="head2">${uiLabelMap.ProductAddFacility}:</div>
        <div class="tabletext">
            ${uiLabelMap.ProductFacility}:
            <select name="facilityId" class="selectBox">
                <#list facilities as facility>
                    <option value="${(facility.facilityId)?if_exists}">${(facility.facilityName)?if_exists}</option>
                </#list>
            </select>
            ${uiLabelMap.ProductMinimumStock}:&nbsp;<input type="text" size="10" name="minimumStock" class="inputBox"/>
            ${uiLabelMap.ProductReorderQuantity}:&nbsp;<input type="text" size="10" name="reorderQuantity" class="inputBox"/>
            ${uiLabelMap.ProductDaysToShip}:&nbsp;<input type="text" size="10" name="daysToShip" class="inputBox"/>
            <input type="submit" value="${uiLabelMap.CommonAdd}" style="font-size: x-small;"/>
        </div>
    </form>
</#if>    
