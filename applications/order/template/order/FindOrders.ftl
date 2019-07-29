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

<script type="application/javascript">
<!-- //
function lookupOrders(click) {
    orderIdValue = document.lookuporder.orderId.value;
    if (orderIdValue.length > 1) {
        document.lookuporder.action = "<@ofbizUrl>orderview</@ofbizUrl>";
        document.lookuporder.method = "get";
    } else {
        document.lookuporder.action = "<@ofbizUrl>searchorders</@ofbizUrl>";
    }

    if (click) {
        document.lookuporder.submit();
    }
    return true;
}
function toggleOrderId(master) {
    var form = document.massOrderChangeForm;
    var orders = form.elements.length;
    for (var i = 0; i < orders; i++) {
        var element = form.elements[i];
        if ("orderIdList" == element.name) {
            element.checked = master.checked;
        }
    }
}
function setServiceName(selection) {
    document.massOrderChangeForm.action = selection.value;
}
function runAction() {
    var form = document.massOrderChangeForm;
    form.submit();
}

function toggleOrderIdList() {
    var form = document.massOrderChangeForm;
    var orders = form.elements.length;
    var isAllSelected = true;
    for (var i = 0; i < orders; i++) {
        var element = form.elements[i];
        if ("orderIdList" == element.name && !element.checked)
            isAllSelected = false;
    }
    jQuery('#checkAllOrders').attr("checked", isAllSelected);
}

// -->

    function paginateOrderList(viewSize, viewIndex, hideFields) {
        document.paginationForm.viewSize.value = viewSize;
        document.paginationForm.viewIndex.value = viewIndex;
        document.paginationForm.hideFields.value = hideFields;
        document.paginationForm.submit();
    }

</script>

<#if security.hasEntityPermission("ORDERMGR", "_VIEW", session)>
<#if parameters.hideFields?has_content>
<form name='lookupandhidefields${requestParameters.hideFields?default("Y")}' method="post" action="<@ofbizUrl>searchorders</@ofbizUrl>">
  <#if parameters.hideFields?default("N")=='Y'>
    <input type="hidden" name="hideFields" value="N"/>
  <#else>
    <input type='hidden' name='hideFields' value='Y'/>
  </#if>
  <input type="hidden" name="viewSize" value="${viewSize}"/>
  <input type="hidden" name="viewIndex" value="${viewIndex}"/>
  <input type='hidden' name='correspondingPoId' value='${requestParameters.correspondingPoId!}'/>
  <input type='hidden' name='internalCode' value='${requestParameters.internalCode!}'/>
  <input type='hidden' name='productId' value='${requestParameters.productId!}'/>
  <input type='hidden' name='goodIdentificationTypeId' value='${requestParameters.goodIdentificationTypeId!}'/>
  <input type='hidden' name='goodIdentificationIdValue' value='${requestParameters.goodIdentificationIdValue!}'/>
  <input type='hidden' name='inventoryItemId' value='${requestParameters.inventoryItemId!}'/>
  <input type='hidden' name='serialNumber' value='${requestParameters.serialNumber!}'/>
  <input type='hidden' name='softIdentifier' value='${requestParameters.softIdentifier!}'/>
  <input type='hidden' name='partyId' value='${requestParameters.partyId!}'/>
  <input type='hidden' name='userLoginId' value='${requestParameters.userLoginId!}'/>
  <input type='hidden' name='billingAccountId' value='${requestParameters.billingAccountId!}'/>
  <input type='hidden' name='createdBy' value='${requestParameters.createdBy!}'/>
  <input type='hidden' name='minDate' value='${requestParameters.minDate!}'/>
  <input type='hidden' name='maxDate' value='${requestParameters.maxDate!}'/>
  <input type='hidden' name='roleTypeId' value="${requestParameters.roleTypeId!}"/>
  <input type='hidden' name='orderTypeId' value='${requestParameters.orderTypeId!}'/>
  <input type='hidden' name='salesChannelEnumId' value='${requestParameters.salesChannelEnumId!}'/>
  <input type='hidden' name='productStoreId' value='${requestParameters.productStoreId!}'/>
  <input type='hidden' name='orderWebSiteId' value='${requestParameters.orderWebSiteId!}'/>
  <input type='hidden' name='orderStatusId' value='${requestParameters.orderStatusId!}'/>
  <input type='hidden' name='hasBackOrders' value='${requestParameters.hasBackOrders!}'/>
  <input type='hidden' name='filterInventoryProblems' value='${requestParameters.filterInventoryProblems!}'/>
  <input type='hidden' name='filterPartiallyReceivedPOs' value='${requestParameters.filterPartiallyReceivedPOs!}'/>
  <input type='hidden' name='filterPOsOpenPastTheirETA' value='${requestParameters.filterPOsOpenPastTheirETA!}'/>
  <input type='hidden' name='filterPOsWithRejectedItems' value='${requestParameters.filterPOsWithRejectedItems!}'/>
  <input type='hidden' name='countryGeoId' value='${requestParameters.countryGeoId!}'/>
  <input type='hidden' name='includeCountry' value='${requestParameters.includeCountry!}'/>
  <input type='hidden' name='isViewed' value='${requestParameters.isViewed!}'/>
  <input type='hidden' name='shipmentMethod' value='${requestParameters.shipmentMethod!}'/>
  <input type='hidden' name='gatewayAvsResult' value='${requestParameters.gatewayAvsResult!}'/>
  <input type='hidden' name='gatewayScoreResult' value='${requestParameters.gatewayScoreResult!}'/>
</form>
</#if>
<form class="basic-form" method="post" name="lookuporder" id="lookuporder" action="<@ofbizUrl>searchorders</@ofbizUrl>" onsubmit="javascript:lookupOrders();">
<input type="hidden" name="lookupFlag" value="Y"/>
<input type="hidden" name="hideFields" value="Y"/>
<input type="hidden" name="viewSize" value="${viewSize}"/>
<input type="hidden" name="viewIndex" value="${viewIndex}"/>

<div id="findOrders" class="screenlet">
  <div class="screenlet-title-bar">
    <ul>
      <li class="h3">${uiLabelMap.OrderFindOrder}</li>
      <div class="basic-nav">
        <ul>
      <#if "Y" == requestParameters.hideFields?default("N")>
        <li><a href="javascript:document.lookupandhidefields${requestParameters.hideFields}.submit()">${uiLabelMap.CommonShowLookupFields}</a></li>
      <#else>
        <#if orderList??><li><a href="javascript:document.lookupandhidefields${requestParameters.hideFields?default("Y")}.submit()">${uiLabelMap.CommonHideFields}</a></li></#if>
        <li><a href="/partymgr/control/findparty?externalLoginKey=${requestAttributes.externalLoginKey!}">${uiLabelMap.PartyLookupParty}</a></li>
        <li><a href="javascript:lookupOrders(true);">${uiLabelMap.OrderLookupOrder}</a></li>
      </#if>
      </ul>
    </div>
    </ul>
    <br class="clear"/>
  </div>
  <#if parameters.hideFields?default("N") != "Y">
    <div class="screenlet-body">
      <table class="basic-table" cellspacing='0'>
        <tr>
          <td align='center' width='100%'>
            <table class="basic-table" cellspacing='0'>
              <tr>
                <td class='label'>${uiLabelMap.OrderOrderId}</td>
                <td><input type='text' name='orderId'/></td>
              </tr>
              <tr>
                <td class='label'>${uiLabelMap.OrderOrderName}</td>
                <td ><input type='text' name='orderName'/></td>
              </tr>
             <tr>
                <td class='label'>${uiLabelMap.OrderExternalId}</td>
                <td ><input type='text' name='externalId'/></td>
              </tr>
              <tr>
                <td  class='label'>${uiLabelMap.OrderCustomerPo}</td>
                <td ><input type='text' name='correspondingPoId' value='${requestParameters.correspondingPoId!}'/></td>
              </tr>
              <tr>
                <td  class='label'>${uiLabelMap.OrderInternalCode}</td>
                <td ><input type='text' name='internalCode' value='${requestParameters.internalCode!}'/></td>
              </tr>
              <tr>
                <td class='label'>${uiLabelMap.ProductProductId}</td>
                <td >
                  <@htmlTemplate.lookupField value='${requestParameters.productId!}' formName="lookuporder" name="productId" id="productId" fieldFormName="LookupProduct"/>
                </td>
              </tr>
              <#if goodIdentificationTypes?has_content>
              <tr>
                  <td class='label'>${uiLabelMap.ProductGoodIdentificationType}</td>
                  <td>
                      <select name='goodIdentificationTypeId'>
                          <#if currentGoodIdentificationType?has_content>
                              <option value="${currentGoodIdentificationType.goodIdentificationTypeId}">${currentGoodIdentificationType.get("description", locale)}</option>
                              <option value="${currentGoodIdentificationType.goodIdentificationTypeId}">---</option>
                          </#if>
                          <option value="">${uiLabelMap.ProductAnyGoodIdentification}</option>
                          <#list goodIdentificationTypes as goodIdentificationType>
                              <option value="${goodIdentificationType.goodIdentificationTypeId}">${goodIdentificationType.get("description", locale)}</option>
                          </#list>
                      </select>
                  </td>
              </tr>
              <tr>
                  <td  class='label'>${uiLabelMap.ProductGoodIdentification}</td>
                  <td ><input type='text' name='goodIdentificationIdValue' value='${requestParameters.goodIdentificationIdValue!}'/></td>
              </tr>
              </#if>
              <tr>
                <td  class='label'>${uiLabelMap.ProductInventoryItemId}</td>
                <td ><input type='text' name='inventoryItemId' value='${requestParameters.inventoryItemId!}'/></td>
              </tr>
              <tr>
                <td class='label'>${uiLabelMap.ProductSerialNumber}</td>
                <td ><input type='text' name='serialNumber' value='${requestParameters.serialNumber!}'/></td>
              </tr>
              <tr>
                <td  class='label'>${uiLabelMap.ProductSoftIdentifier}</td>
                <td ><input type='text' name='softIdentifier' value='${requestParameters.softIdentifier!}'/></td>
              </tr>
              <tr>
                <td class='label'>${uiLabelMap.PartyRoleType}</td>
                <td >
                  <select name='roleTypeId' id='roleTypeId' multiple="multiple">
                    <#if currentRole?has_content>
                    <option value="${currentRole.roleTypeId}">${currentRole.get("description", locale)}</option>
                    </#if>
                    <option value="">${uiLabelMap.CommonAnyRoleType}</option>
                    <#list roleTypes as roleType>
                      <option value="${roleType.roleTypeId}">${roleType.get("description", locale)}</option>
                    </#list>
                  </select>
                </td>
              </tr>
              <tr>
                <td class='label'>${uiLabelMap.PartyPartyId}</td>
                <td>
                  <@htmlTemplate.lookupField value='${requestParameters.partyId!}' formName="lookuporder" name="partyId" id="partyId" fieldFormName="LookupPartyName"/>
                </td>
              </tr>
              <tr>
                <td class='label'>${uiLabelMap.CommonUserLoginId}</td>
                <td>
                  <@htmlTemplate.lookupField value='${requestParameters.userLoginId!}' formName="lookuporder" name="userLoginId" id="userLoginId" fieldFormName="LookupUserLoginAndPartyDetails"/>
                </td>
              </tr>
              <tr>
                <td class='label'>${uiLabelMap.OrderOrderType}</td>
                <td>
                  <select name='orderTypeId'>
                    <#if currentType?has_content>
                    <option value="${currentType.orderTypeId}">${currentType.get("description", locale)}</option>
                    <option value="${currentType.orderTypeId}">---</option>
                    </#if>
                    <option value="">${uiLabelMap.OrderAnyOrderType}</option>
                    <#list orderTypes as orderType>
                      <option value="${orderType.orderTypeId}">${orderType.get("description", locale)}</option>
                    </#list>
                  </select>
                </td>
              </tr>
              <tr>
                <td class='label'>${uiLabelMap.AccountingBillingAccount}</td>
                <td>
                  <@htmlTemplate.lookupField value='${requestParameters.billingAccountId!}' formName="lookuporder" name="billingAccountId" id="billingAccountId" fieldFormName="LookupBillingAccount"/>
                </td>
              </tr>
              <tr>
                <td class='label'>${uiLabelMap.CommonCreatedBy}</td>
                <td><input type='text' name='createdBy' value='${requestParameters.createdBy!}'/></td>
              </tr>
              <tr>
                <td class='label'>${uiLabelMap.OrderSalesChannel}</td>
                <td>
                  <select name='salesChannelEnumId'>
                    <#if currentSalesChannel?has_content>
                    <option value="${currentSalesChannel.enumId}">${currentSalesChannel.get("description", locale)}</option>
                    <option value="${currentSalesChannel.enumId}">---</option>
                    </#if>
                    <option value="">${uiLabelMap.CommonAnySalesChannel}</option>
                    <#list salesChannels as channel>
                      <option value="${channel.enumId}">${channel.get("description", locale)}</option>
                    </#list>
                  </select>
                </td>
              </tr>
              <tr>
                <td class='label'>${uiLabelMap.ProductProductStore}</td>
                <td>
                  <select name='productStoreId'>
                    <#if currentProductStore?has_content>
                    <option value="${currentProductStore.productStoreId}">${currentProductStore.storeName!}</option>
                    <option value="${currentProductStore.productStoreId}">---</option>
                    </#if>
                    <option value="">${uiLabelMap.CommonAnyStore}</option>
                    <#list productStores as store>
                      <option value="${store.productStoreId}">${store.storeName!}</option>
                    </#list>
                  </select>
                </td>
              </tr>
              <tr>
                <td class='label'>${uiLabelMap.ProductWebSite}</td>
                <td>
                  <select name='orderWebSiteId'>
                    <#if currentWebSite?has_content>
                    <option value="${currentWebSite.webSiteId}">${currentWebSite.siteName}</option>
                    <option value="${currentWebSite.webSiteId}">---</option>
                    </#if>
                    <option value="">${uiLabelMap.CommonAnyWebSite}</option>
                    <#list webSites as webSite>
                      <option value="${webSite.webSiteId}">${webSite.siteName!}</option>
                    </#list>
                  </select>
                </td>
              </tr>
              <tr>
                <td class='label'>${uiLabelMap.CommonStatus}</td>
                <td>
                  <#list orderStatuses as orderStatus>
                    <label>
                      <input type="checkbox" name="orderStatusId" value="${orderStatus.statusId}" <#if currentStatuses?has_content && currentStatuses.contains(orderStatus.statusId)>checked</#if>/>
                      ${orderStatus.get("description", locale)}
                    </label>
                  </#list>
                </td>
              </tr>
              <tr>
                <td class='label'>${uiLabelMap.OrderContainsBackOrders}</td>
                <td>
                  <select name='hasBackOrders'>
                    <#if requestParameters.hasBackOrders?has_content>
                    <option value="Y">${uiLabelMap.OrderBackOrders}</option>
                    <option value="Y">---</option>
                    </#if>
                    <option value="">${uiLabelMap.CommonShowAll}</option>
                    <option value="Y">${uiLabelMap.CommonOnly}</option>
                  </select>
                </td>
              </tr>
              <tr>
                <td class='label'>${uiLabelMap.OrderSelectShippingMethod}</td>
                <td>
                  <select name="shipmentMethod">
                    <#if currentCarrierShipmentMethod?has_content>
                      <#assign currentShipmentMethodType = currentCarrierShipmentMethod.getRelatedOne("ShipmentMethodType", false)>
                      <option value="${currentCarrierShipmentMethod.partyId}@${currentCarrierShipmentMethod.shipmentMethodTypeId}">${currentCarrierShipmentMethod.partyId!} ${currentShipmentMethodType.description!}</option>
                      <option value="${currentCarrierShipmentMethod.partyId}@${currentCarrierShipmentMethod.shipmentMethodTypeId}">---</option>
                    </#if>
                    <option value="">${uiLabelMap.OrderSelectShippingMethod}</option>
                    <#list carrierShipmentMethods as carrierShipmentMethod>
                      <#assign shipmentMethodType = carrierShipmentMethod.getRelatedOne("ShipmentMethodType", false)>
                      <option value="${carrierShipmentMethod.partyId}@${carrierShipmentMethod.shipmentMethodTypeId}">${carrierShipmentMethod.partyId!} ${shipmentMethodType.description!}</option>
                    </#list>
                  </select>
                </td>
              </tr>
              <tr>
                <td class='label'>${uiLabelMap.OrderViewed}</td>
                <td>
                  <select name="isViewed">
                    <#if requestParameters.isViewed?has_content>
                      <#assign isViewed = requestParameters.isViewed>
                      <option value="${isViewed}"><#if "Y" == isViewed>${uiLabelMap.CommonYes}<#elseif "N" == isViewed>${uiLabelMap.CommonNo}</#if></option>
                    </#if>
                    <option value=""></option>
                    <option value="Y">${uiLabelMap.CommonYes}</option>
                    <option value="N">${uiLabelMap.CommonNo}</option>
                  </select>
                </td>
              </tr>
              <tr>
                <td class='label'>${uiLabelMap.OrderAddressVerification}</td>
                <td><input type='text' name='gatewayAvsResult' value='${requestParameters.gatewayAvsResult!}'/></td>
              </tr>
              <tr>
                <td class='label'>${uiLabelMap.OrderScore}</td>
                <td><input type='text' name='gatewayScoreResult' value='${requestParameters.gatewayScoreResult!}'/></td>
              </tr>
              <tr>
                <td class='label'>${uiLabelMap.CommonDateFilter}</td>
                <td>
                  <@htmlTemplate.renderDateTimeField name="minDate" event="" action="" value="${requestParameters.minDate!}" className="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" size="25" maxlength="30" id="minDate1" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
                  <span class='label'>${uiLabelMap.CommonFrom}</span>
                </td>
              </tr>
              <tr>
                <td class="label"/>
                <td>
                    <@htmlTemplate.renderDateTimeField name="maxDate" event="" action="" value="${requestParameters.maxDate!}" className="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" size="25" maxlength="30" id="maxDate1" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
                    <span class='label'>${uiLabelMap.CommonThru}</span>
                </td>
              </tr>
              <tr>
                <td class='label'><label for="filterInventoryProblems">${uiLabelMap.OrderFilterOn} ${uiLabelMap.OrderFilterInventoryProblems}</label></td>
                <td>
                  <input type="checkbox" id="filterInventoryProblems" name="filterInventoryProblems" value="Y"
                  <#if "Y" == requestParameters.filterInventoryProblems?default("N")>checked="checked"</#if> />
                </td>
              </tr>
              <tr>
                <td class='label'><label for="filterPartiallyReceivedPOs">${uiLabelMap.OrderFilterOn} ${uiLabelMap.OrderFilterPOs} ${uiLabelMap.OrderFilterPartiallyReceivedPOs}</label></td>
                <td>
                    <input type="checkbox" id="filterPartiallyReceivedPOs" name="filterPartiallyReceivedPOs" value="Y"
                    <#if "Y" == requestParameters.filterPartiallyReceivedPOs?default("N")>checked="checked"</#if> />
                </td>
              </tr>
              <tr>
                <td class='label'><label for="filterPOsOpenPastTheirETA">${uiLabelMap.OrderFilterOn} ${uiLabelMap.OrderFilterPOs} ${uiLabelMap.OrderFilterPOsOpenPastTheirETA}</label></td>
                <td>
                    <input type="checkbox" id="filterPOsOpenPastTheirETA" name="filterPOsOpenPastTheirETA" value="Y"
                    <#if "Y" == requestParameters.filterPOsOpenPastTheirETA?default("N")>checked="checked"</#if> />
                </td>
              </tr>
              <tr>
                <td class='label'><label for="filterPOsWithRejectedItems">${uiLabelMap.OrderFilterOn} ${uiLabelMap.OrderFilterPOs} ${uiLabelMap.OrderFilterPOsWithRejectedItems}</label></td>
                <td>
                        <input type="checkbox" id="filterPOsWithRejectedItems" name="filterPOsWithRejectedItems" value="Y"
                            <#if "Y" == requestParameters.filterPOsWithRejectedItems?default("N")>checked="checked"</#if> />
                </td>
              </tr>
              <tr>
                <td class='label'>${uiLabelMap.OrderShipToCountry}</td>
                <td>
                  <select name="countryGeoId">
                    <#if requestParameters.countryGeoId?has_content>
                        <#assign countryGeoId = requestParameters.countryGeoId>
                        <#assign geo = delegator.findOne("Geo", Static["org.apache.ofbiz.base.util.UtilMisc"].toMap("geoId", countryGeoId), true)>
                        <option value="${countryGeoId}">${geo.geoName!}</option>
                        <option value="${countryGeoId}">---</option>
                    <#else>
                        <option value="">---</option>
                    </#if>
                    ${screens.render("component://common/widget/CommonScreens.xml#countries")}
                  </select>
                  <select name="includeCountry">
                    <#if requestParameters.includeCountry?has_content>
                       <#assign includeCountry = requestParameters.includeCountry>
                       <option value="${includeCountry}"><#if "Y" == includeCountry>${uiLabelMap.OrderOnlyInclude}<#elseif "N" == includeCountry>${uiLabelMap.OrderDoNotInclude}</#if></option>
                       <option value="${includeCountry}">---</option>
                    </#if>
                    <option value="Y">${uiLabelMap.OrderOnlyInclude}</option>
                    <option value="N">${uiLabelMap.OrderDoNotInclude}</option>
                  </select>
                </td>
              </tr>
              <tr>
                <td class='label'>${uiLabelMap.AccountingPaymentStatus}</td>
                <td>
                    <select name="paymentStatusId">
                        <option value="">${uiLabelMap.CommonAll}</option>
                        <#list paymentStatusList as paymentStatus>
                            <option value="${paymentStatus.statusId}">${paymentStatus.get("description", locale)}</option>
                        </#list>
                    </select>
                </td>
              </tr>
              <tr><td colspan="3"><hr /></td></tr>
              <tr>
                <td class="label"/>
                <td>
                    <input type="hidden" name="showAll" value="Y"/>
                    <input type='submit' value='${uiLabelMap.CommonFind}'/>
                </td>
              </tr>
            </table>
          </td>
        </tr>
      </table>
    </div>
      </#if>
</div>
</form>
<#if requestParameters.hideFields?default("N") != "Y">
<script type="application/javascript">
<!--//
document.lookuporder.orderId.focus();
//-->
</script>
</#if>

<br />

<div id="findOrdersList" class="screenlet">
  <div class="screenlet-title-bar">
    <ul>
      <li class="h3">${uiLabelMap.OrderOrderFound}</li>
      <#if (orderList?has_content && 0 < orderList?size)>
        <#if (orderListSize > highIndex)>
          <li><a href="javascript:paginateOrderList('${viewSize}', '${viewIndex+1}', '${requestParameters.hideFields?default("N")}')">${uiLabelMap.CommonNext}</a></li>
        <#else>
          <li><span class="disabled">${uiLabelMap.CommonNext}</span></li>
        </#if>
        <#if (orderListSize > 0)>
          <li><span>${lowIndex} - ${highIndex} ${uiLabelMap.CommonOf} ${orderListSize}</span></li>
        </#if>
        <#if (viewIndex > 1)>
          <li><a href="javascript:paginateOrderList('${viewSize}', '${viewIndex-1}', '${requestParameters.hideFields?default("N")}')">${uiLabelMap.CommonPrevious}</a></li>
        <#else>
          <li><span class="disabled">${uiLabelMap.CommonPrevious}</span></li>
        </#if>
      </#if>
    </ul>
    <br class="clear" />
  </div>
  <div class="screenlet-body">
    <form name="paginationForm" method="post" action="<@ofbizUrl>searchorders</@ofbizUrl>">
      <input type="hidden" name="viewSize"/>
      <input type="hidden" name="viewIndex"/>
      <input type="hidden" name="hideFields"/>
      <#if paramIdList?? && paramIdList?has_content>
        <#list paramIdList as paramIds>
          <#assign paramId = paramIds.split("=")/>
          <input type="hidden" name="${paramId[0]}" value="${paramId[1]}"/>
        </#list>
      </#if>
    </form>
    <form class="basic-form" name="massOrderChangeForm" method="post" action="javascript:void(0);">
      <div>&nbsp;</div>
      <div align="right">
        <input type="hidden" name="screenLocation" value="component://order/widget/ordermgr/OrderPrintScreens.xml#OrderPDF"/>
        <#if paramList??>
            <#assign ampersand = "&amp;">
        <#else>
            <#assign ampersand = "">
        </#if>
        <select name="serviceName" onchange="javascript:setServiceName(this);">
           <option value="javascript:void(0);">&nbsp;</option>
           <option value="<@ofbizUrl>massApproveOrders?hideFields=${requestParameters.hideFields?default("N")}${ampersand}${paramList}</@ofbizUrl>">${uiLabelMap.OrderApproveOrder}</option>
           <option value="<@ofbizUrl>massHoldOrders?hideFields=${requestParameters.hideFields?default("N")}${ampersand}${paramList}</@ofbizUrl>">${uiLabelMap.OrderHold}</option>
           <option value="<@ofbizUrl>massProcessOrders?hideFields=${requestParameters.hideFields?default("N")}${ampersand}${paramList}</@ofbizUrl>">${uiLabelMap.OrderProcessOrder}</option>
           <option value="<@ofbizUrl>massCancelOrders?hideFields=${requestParameters.hideFields?default("N")}${ampersand}${paramList}</@ofbizUrl>">${uiLabelMap.OrderCancelOrder}</option>
           <option value="<@ofbizUrl>massCancelRemainingPurchaseOrderItems?hideFields=${requestParameters.hideFields?default("N")}${ampersand}${paramList}</@ofbizUrl>">${uiLabelMap.OrderCancelRemainingPOItems}</option>
           <option value="<@ofbizUrl>massRejectOrders?hideFields=${requestParameters.hideFields?default("N")}${ampersand}${paramList}</@ofbizUrl>">${uiLabelMap.OrderRejectOrder}</option>
           <option value="<@ofbizUrl>massPickOrders?hideFields=${requestParameters.hideFields?default("N")}${ampersand}${paramList}</@ofbizUrl>">${uiLabelMap.OrderPickOrders}</option>
           <option value="<@ofbizUrl>massQuickShipOrders?hideFields=${requestParameters.hideFields?default("N")}${ampersand}${paramList}</@ofbizUrl>">${uiLabelMap.OrderQuickShipEntireOrder}</option>
           <option value="<@ofbizUrl>massPrintOrders?hideFields=${requestParameters.hideFields?default('N')}${ampersand}${paramList}</@ofbizUrl>">${uiLabelMap.CommonPrint}</option>
           <option value="<@ofbizUrl>massCreateFileForOrders?hideFields=${requestParameters.hideFields?default('N')}${ampersand}${paramList}</@ofbizUrl>">${uiLabelMap.ContentCreateFile}</option>
        </select>
        <select name="printerName">
           <option value="javascript:void(0);">&nbsp;</option>
           <#list printers as printer>
           <option value="${printer}">${printer}</option>
           </#list>
        </select>
        <a href="javascript:runAction();" class="buttontext">${uiLabelMap.OrderRunAction}</a>
        <br class="clear" />
      </div>

      <table class="basic-table hover-bar" cellspacing='0'>
        <tr class="header-row">
          <td >
            <input type="checkbox" id="checkAllOrders" name="checkAllOrders" value="1" onchange="javascript:toggleOrderId(this);"/>
          </td>
          <td >${uiLabelMap.OrderOrderType}</td>
          <td >${uiLabelMap.OrderOrderId}</td>
          <td >${uiLabelMap.OrderOrderName}</td>
          <td >${uiLabelMap.PartyName}</td>
          <td align="right">${uiLabelMap.OrderSurvey}</td>
          <td align="right">${uiLabelMap.OrderItemsOrdered}</td>
          <td align="right">${uiLabelMap.OrderItemsBackOrdered}</td>
          <td align="right">${uiLabelMap.OrderItemsReturned}</td>
          <td align="right">${uiLabelMap.OrderRemainingSubTotal}</td>
          <td align="right" >${uiLabelMap.OrderOrderTotal}

            <#if ("Y" == requestParameters.filterInventoryProblems?default("N")) || ("Y" == requestParameters.filterPOsOpenPastTheirETA?default("N")) || ("Y" == requestParameters.filterPOsWithRejectedItems?default("N")) || ("Y" == requestParameters.filterPartiallyReceivedPOs?default("N"))>
              <td>${uiLabelMap.CommonStatus}</td>
              <td>${uiLabelMap.CommonFilter}</td>
            <#else>
              <td></td>
              <td>${uiLabelMap.CommonStatus}</td>
            </#if>
          <td>${uiLabelMap.OrderDate}</td>
          <td>${uiLabelMap.PartyPartyId}</td>
        </tr>
        <#if orderList?has_content>
          <#assign alt_row = false>
          <#list orderList as orderHeader>
            <#assign orh = Static["org.apache.ofbiz.order.order.OrderReadHelper"].getHelper(orderHeader)>
            <#assign statusItem = orderHeader.getRelatedOne("StatusItem", true)>
            <#assign orderType = orderHeader.getRelatedOne("OrderType", true)>
            <#if "PURCHASE_ORDER" == orderType.orderTypeId>
              <#assign displayParty = orh.getSupplierAgent()!>
            <#else>
              <#assign displayParty = orh.getPlacingParty()!>
            </#if>
            <#assign partyId = displayParty.partyId?default("_NA_")>
            <tr valign="middle"<#if alt_row> class="alternate-row"</#if>>
              <td>
                 <input type="checkbox" name="orderIdList" value="${orderHeader.orderId}" onchange="javascript:toggleOrderIdList();"/>
              </td>
              <td>${orderType.get("description",locale)?default(orderType.orderTypeId?default(""))}</td>
              <td><a href="<@ofbizUrl>orderview?orderId=${orderHeader.orderId}</@ofbizUrl>" class='buttontext'>${orderHeader.orderId}</a></td>
              <#if orderHeader.orderName?has_content>
                <td><a href="<@ofbizUrl>orderview?orderId=${orderHeader.orderId}</@ofbizUrl>" class='buttontext'>${orderHeader.orderName}</a></td>
              <#else>
                <td></td>
              </#if>
              <td>
                <div>
                  <#if displayParty?has_content>
                      <#assign displayPartyNameResult = dispatcher.runSync("getPartyNameForDate", Static["org.apache.ofbiz.base.util.UtilMisc"].toMap("partyId", displayParty.partyId, "compareDate", orderHeader.orderDate, "userLogin", userLogin))/>
                      ${displayPartyNameResult.fullName?default("[${uiLabelMap.OrderPartyNameNotFound}]")}
                  <#else>
                    ${uiLabelMap.CommonNA}
                  </#if>
                </div>
              </td>
              <td align="right">${orh.hasSurvey()?string.number}</td>
              <td align="right">${orh.getTotalOrderItemsQuantity()?string.number}</td>
              <td align="right">${orh.getOrderBackorderQuantity()?string.number}</td>
              <td align="right">${orh.getOrderReturnedQuantity()?string.number}</td>
              <td align="right"><@ofbizCurrency amount=orderHeader.remainingSubTotal isoCode=orh.getCurrency()/></td>
              <td align="right"><@ofbizCurrency amount=orderHeader.grandTotal isoCode=orh.getCurrency()/></td>

              <td>&nbsp;</td>
              <td>${statusItem.get("description",locale)?default(statusItem.statusId?default("N/A"))}</td>
              </td>
              <#if ("Y" == requestParameters.filterInventoryProblems?default("N")) || ("Y" == requestParameters.filterPOsOpenPastTheirETA?default("N")) || ("Y" == requestParameters.filterPOsWithRejectedItems?default("N")) || ("Y" == requestParameters.filterPartiallyReceivedPOs?default("N"))>
                  <td>
                      <#if filterInventoryProblems.contains(orderHeader.orderId)>
                        Inv&nbsp;
                      </#if>
                      <#if filterPOsOpenPastTheirETA.contains(orderHeader.orderId)>
                        ETA&nbsp;
                      </#if>
                      <#if filterPOsWithRejectedItems.contains(orderHeader.orderId)>
                        Rej&nbsp;
                      </#if>
                      <#if filterPartiallyReceivedPOs.contains(orderHeader.orderId)>
                        Part&nbsp;
                      </#if>
                  </td>
              </#if>
              <td>${orderHeader.getString("orderDate")}</td>
              <td>
                <#if partyId != "_NA_">
                  <a href="${customerDetailLink}${partyId}" class="buttontext">${partyId}</a>
                <#else>
                  ${uiLabelMap.CommonNA}
                </#if>
              </td>
              <td align='right'>
                <a href="<@ofbizUrl>orderview?orderId=${orderHeader.orderId}</@ofbizUrl>" class='buttontext'>${uiLabelMap.CommonView}</a>
              </td>
            </tr>
            <#-- toggle the row color -->
            <#assign alt_row = !alt_row>
          </#list>
        <#else>
          <tr>
            <td>
            <h3>${uiLabelMap.OrderNoOrderFound}</h3>
            </td>
          </tr>
        </#if>
        <#if lookupErrorMessage??>
          <tr>
            </td>
            <h3>${lookupErrorMessage}</h3>
            </td>
          </tr>
        </#if>
      </table>
    </form>
  </div>
</div>
<#else>
  <h3>${uiLabelMap.OrderViewPermissionError}</h3>
</#if>
