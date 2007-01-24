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
<table border="1" cellpadding="2" cellspacing="0">
    <tr>
      <td nowrap><div class="tableheadtext">${uiLabelMap.ProductCountry}</td>
      <td nowrap><div class="tableheadtext">${uiLabelMap.ProductState}</div></td>
      <td nowrap><div class="tableheadtext">${uiLabelMap.ProductTaxCategory}</div></td>
      <td nowrap><div class="tableheadtext">${uiLabelMap.ProductMinItemPrice}</div></td>
      <td nowrap><div class="tableheadtext">${uiLabelMap.ProductMinPurchase}</div></td>
      <td nowrap><div class="tableheadtext">${uiLabelMap.ProductTaxRate}</div></td>
      <td nowrap><div class="tableheadtext">${uiLabelMap.CommonFromDate}</div></td>             
      <td nowrap><div class="tabletext">&nbsp;</div></td>
    </tr>
    <#list taxItems as taxItem>      
      <tr>                  
        <td><div class="tabletext">${taxItem.countryGeoId}</div></td>
        <td><div class="tabletext">${taxItem.stateProvinceGeoId}</div></td>
        <td><div class="tabletext">${taxItem.taxCategory}</div></td>
        <td><div class="tabletext">${taxItem.minItemPrice?string("##0.00")}</div></td>
        <td><div class="tabletext">${taxItem.minPurchase?string("##0.00")}</div></td>
        <td><div class="tabletext">${taxItem.salesTaxPercentage?if_exists}</div></td>
        <td><div class="tabletext">${taxItem.fromDate?string}</div></td>
        <#if security.hasEntityPermission("TAXRATE", "_DELETE", session)>
          <td align="center"><div class="tabletext"><a href="<@ofbizUrl>storeRemoveTaxRate?productStoreId=${productStoreId}&countryGeoId=${taxItem.countryGeoId}&stateProvinceGeoId=${taxItem.stateProvinceGeoId}&taxCategory=${taxItem.taxCategory}&minItemPrice=${taxItem.minItemPrice?string.number}&minPurchase=${taxItem.minPurchase?string.number}&fromDate=${taxItem.fromDate}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonDelete}]</a></div></td>
        <#else>
          <td>&nbsp;</td>
        </#if>
      </tr>
    </#list>
</table>
  
<br/>
<table>
    <#if security.hasEntityPermission("TAXRATE", "_CREATE", session)>
      <form name="addrate" action="<@ofbizUrl>storeCreateTaxRate</@ofbizUrl>">
        <input type="hidden" name="productStoreId" value="${productStoreId}">
        <tr>
          <td><span class="tableheadtext">${uiLabelMap.ProductCountry}</span></td>
          <td>
            <select name="countryGeoId" class="selectBox">
              <option value="_NA_">${uiLabelMap.CommonAll}</option>
              ${screens.render("component://common/widget/CommonScreens.xml#countries")}
            </select>
          </td>
        </tr>
        <tr>
          <td><span class="tableheadtext">${uiLabelMap.PartyState}</span></td>
          <td>
            <select name="stateProvinceGeoId" class="selectBox">
              <option value="_NA_">${uiLabelMap.CommonAll}</option>
              ${screens.render("component://common/widget/CommonScreens.xml#states")}
            </select>
          </td>
        </tr>
        <tr>
          <td><span class="tableheadtext">${uiLabelMap.ProductTaxCategory}</span></td>      
          <td><input type="text" size="20" name="taxCategory" class="inputBox"></td>      
        </tr>
        <tr>
          <td><span class="tableheadtext">${uiLabelMap.ProductMinimumItemPrice}</span></td>
          <td><input type="text" size="10" name="minItemPrice" class="inputBox" value="0.00"></td>
        </tr>
        <tr>
          <td><span class="tableheadtext">${uiLabelMap.ProductMinimumPurchase}</span></td>
          <td><input type="text" size="10" name="minPurchase" class="inputBox" value="0.00"></td>      
        </tr>
        <tr>
          <td><span class="tableheadtext">${uiLabelMap.ProductTaxShipping}?</span></td>
          <td>
            <select name="taxShipping" class="selectBox">
              <option value="N">${uiLabelMap.CommonNo}</option>
              <option value="Y">${uiLabelMap.CommonYes}</option>
            </select>
          </td>
        </tr>
        <tr>
          <td><span class="tableheadtext">${uiLabelMap.CommonDescription}</span></td>
          <td><input type="text" size="20" name="description" class="inputBox"></td>
        </tr>
        <tr>
          <td><span class="tableheadtext">${uiLabelMap.ProductTaxRate}</span></td>
          <td><input type="text" size="10" name="salesTaxPercentage" class="inputBox"></td>
        </tr>
        <tr>
          <td><span class="tableheadtext">${uiLabelMap.CommonFromDate}</span></td>
          <td><input type="text" name="fromDate" class="inputBox"><a href="javascript:call_cal(document.addrate.fromDate, null);"><img src='/images/cal.gif' width='16' height='16' border='0' alt='Calendar'></a></td>
        </tr>
       <tr>
          <td><span class="tableheadtext">${uiLabelMap.CommonThruDate}</span></td>
          <td><input type="text" name="thruDate" class="inputBox"><a href="javascript:call_cal(document.addrate.thruDate, null);"><img src='/images/cal.gif' width='16' height='16' border='0' alt='Calendar'></a></td>
        </tr>        
        <tr>
          <td><input type="submit" class="smallSubmit" value="${uiLabelMap.CommonAdd}"/></td>
        </tr>
      </form>
    </#if>
</table>  
