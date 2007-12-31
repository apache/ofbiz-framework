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
<div class="screenlet">
    <div class="screenlet-title-bar">
        <h3>${uiLabelMap.PageTitleEditProductStoreTaxSetup}</h3>
    </div>
    <div class="screenlet-body"> 
        <table cellspacing="0" class="basic-table">
            <tr class="header-row">
              <td nowrap>${uiLabelMap.ProductCountry}</td>
              <td nowrap>${uiLabelMap.ProductState}</td>
              <td nowrap>${uiLabelMap.ProductTaxCategory}</td>
              <td nowrap>${uiLabelMap.ProductMinItemPrice}</td>
              <td nowrap>${uiLabelMap.ProductMinPurchase}</td>
              <td nowrap>${uiLabelMap.ProductTaxRate}</td>
              <td nowrap>${uiLabelMap.CommonFromDate}</td>             
              <td nowrap>&nbsp;</td>
            </tr>
            <#list taxItems as taxItem>      
              <tr>                  
                <td>${taxItem.countryGeoId}</td>
                <td>${taxItem.stateProvinceGeoId}</td>
                <td>${taxItem.taxCategory}</td>
                <td>${taxItem.minItemPrice?string("##0.00")}</td>
                <td>${taxItem.minPurchase?string("##0.00")}</td>
                <td>${taxItem.salesTaxPercentage?if_exists}</td>
                <td>${taxItem.fromDate?string}</td>
                <#if security.hasEntityPermission("TAXRATE", "_DELETE", session)>
                  <td align="center"><a href="<@ofbizUrl>storeRemoveTaxRate?productStoreId=${productStoreId}&countryGeoId=${taxItem.countryGeoId}&stateProvinceGeoId=${taxItem.stateProvinceGeoId}&taxCategory=${taxItem.taxCategory}&minItemPrice=${taxItem.minItemPrice?string.number}&minPurchase=${taxItem.minPurchase?string.number}&fromDate=${taxItem.fromDate}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonDelete}</a></td>
                <#else>
                  <td>&nbsp;</td>
                </#if>
              </tr>
            </#list>
        </table>
    </div>
</div>      
<div class="screenlet">
    <div class="screenlet-title-bar">
        <h3>${uiLabelMap.PageTitleEditProductStoreTaxSetup}</h3>
    </div>
    <div class="screenlet-body"> 
        <table cellspacing="0" class="basic-table">
            <#if security.hasEntityPermission("TAXRATE", "_CREATE", session)>
              <form name="addrate" action="<@ofbizUrl>storeCreateTaxRate</@ofbizUrl>">
                <input type="hidden" name="productStoreId" value="${productStoreId}">
                <tr>
                  <td class="label">${uiLabelMap.ProductCountry}</td>
                  <td>
                    <select name="countryGeoId">
                      <option value="_NA_">${uiLabelMap.CommonAll}</option>
                      ${screens.render("component://common/widget/CommonScreens.xml#countries")}
                    </select>
                  </td>
                </tr>
                <tr>
                  <td class="label">${uiLabelMap.PartyState}</td>
                  <td>
                    <select name="stateProvinceGeoId">
                      <option value="_NA_">${uiLabelMap.CommonAll}</option>
                      ${screens.render("component://common/widget/CommonScreens.xml#states")}
                    </select>
                  </td>
                </tr>
                <tr>
                  <td class="label">${uiLabelMap.ProductTaxCategory}</td>      
                  <td><input type="text" size="20" name="taxCategory"></td>      
                </tr>
                <tr>
                  <td class="label">${uiLabelMap.ProductMinimumItemPrice}</td>
                  <td><input type="text" size="10" name="minItemPrice" value="0.00"></td>
                </tr>
                <tr>
                  <td class="label">${uiLabelMap.ProductMinimumPurchase}</td>
                  <td><input type="text" size="10" name="minPurchase" value="0.00"></td>      
                </tr>
                <tr>
                  <td class="label">${uiLabelMap.ProductTaxShipping}?</td>
                  <td>
                    <select name="taxShipping">
                      <option value="N">${uiLabelMap.CommonNo}</option>
                      <option value="Y">${uiLabelMap.CommonYes}</option>
                    </select>
                  </td>
                </tr>
                <tr>
                  <td class="label">${uiLabelMap.CommonDescription}</td>
                  <td><input type="text" size="20" name="description"></td>
                </tr>
                <tr>
                  <td class="label">${uiLabelMap.ProductTaxRate}</td>
                  <td><input type="text" size="10" name="salesTaxPercentage"></td>
                </tr>
                <tr>
                  <td class="label">${uiLabelMap.CommonFromDate}</td>
                  <td><input type="text" name="fromDate"><a href="javascript:call_cal(document.addrate.fromDate, null);"><img src='/images/cal.gif' width='16' height='16' border='0' alt='Calendar'></a></td>
                </tr>
               <tr>
                  <td class="label">${uiLabelMap.CommonThruDate}</td>
                  <td><input type="text" name="thruDate"><a href="javascript:call_cal(document.addrate.thruDate, null);"><img src='/images/cal.gif' width='16' height='16' border='0' alt='Calendar'></a></td>
                </tr>        
                <tr>
                  <td><input type="submit" class="smallSubmit" value="${uiLabelMap.CommonAdd}"/></td>
                </tr>
              </form>
            </#if>
        </table>  
    </div>
</div>