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

<script>
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
    toggleOrderIdList();
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
    var isSingle = true;
    for (var i = 0; i < orders; i++) {
        var element = form.elements[i];
        if ("orderIdList" == element.name) {
            if (element.checked) {
                isSingle = false;
            } else {
                isAllSelected = false;
            }
        }
    }
    if (isAllSelected) {
        jQuery('#checkAllOrders').attr('checked', true);
    } else {
        jQuery('#checkAllOrders').attr('checked', false);
    }
    jQuery('#checkAllOrders').attr("checked", isAllSelected);
    if (!isSingle && jQuery('#serviceName').val() != "") {
        jQuery('#submitButton').removeAttr("disabled");
    } else {
        jQuery('#submitButton').attr('disabled', true);
    }
}

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
        <li><a href="<@ofbizUrl controlPath="/partymgr/control">findparty?externalLoginKey=${requestAttributes.externalLoginKey!}</@ofbizUrl>">${uiLabelMap.PartyLookupParty}</a></li>
        <li><a href="javascript:lookupOrders(true);">${uiLabelMap.OrderLookupOrder}</a></li>
      </#if>
      </ul>
    </div>
    </ul>
    <br class="clear"/>
  </div>
  <#if parameters.hideFields?default("N") != "Y">
      <div class="ofbiz-form with-left-right-columns">

          <label for="orderId">${uiLabelMap.OrderOrderId}</label>
          <input id="orderId" type='text' name='orderId'/>

          <label for="orderName">${uiLabelMap.OrderOrderName}</label>
          <input id="orderName" type='text' name='orderName'/>

          <label for="orderTypeId">${uiLabelMap.OrderOrderType}</label>
          <select id='orderTypeId' name='orderTypeId'>
              <#if currentType?has_content>
                  <option value="${currentType.orderTypeId}">${currentType.get("description", locale)}</option>
                  <option value="${currentType.orderTypeId}">---</option>
              </#if>
              <option value="">${uiLabelMap.OrderAnyOrderType}</option>
              <#list orderTypes as orderType>
                  <option value="${orderType.orderTypeId}">${orderType.get("description", locale)}</option>
              </#list>
          </select>

          <label for="correspondingPoId">${uiLabelMap.OrderCustomerPo}</label>
          <input id='correspondingPoId' type='text' name='correspondingPoId' value='${requestParameters.correspondingPoId!}'/>

          <label for="correspondingPoId" class="left-column">${uiLabelMap.CommonDateFilter}</label>
          <div>
              <@htmlTemplate.renderDateTimeField name="minDate" event="" action="" value="${requestParameters.minDate!}" className="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" size="25" maxlength="30" id="minDate1" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
              <span class='label'>${uiLabelMap.CommonFrom}</span>
          </div>

          <label for="externalId" class="right-column">${uiLabelMap.OrderExternalId}</label>
          <input id='externalId' type='text' name='externalId'/>

          <label class="left-column"></label>
          <div>
              <@htmlTemplate.renderDateTimeField name="maxDate" event="" action="" value="${requestParameters.maxDate!}" className="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" size="25" maxlength="30" id="maxDate1" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
              <span class='label'>${uiLabelMap.CommonThru}</span>
          </div>

          <label for="partyId" class="left-column">${uiLabelMap.PartyPartyId}</label>
          <@htmlTemplate.lookupField value='${requestParameters.partyId!}' formName="lookuporder" name="partyId" id="partyId" fieldFormName="LookupPartyName"/>

          <label for="userLoginId" class="right-column">${uiLabelMap.CommonUserLoginId}</label>
          <@htmlTemplate.lookupField value='${requestParameters.userLoginId!}' formName="lookuporder" name="userLoginId" id="userLoginId" fieldFormName="LookupUserLoginAndPartyDetails"/>

          <label for="roleTypeId" class="left-column">${uiLabelMap.PartyRoleType}</label>
          <select name='roleTypeId' id='roleTypeId' multiple="multiple">
              <#if currentRole?has_content>
                  <option value="${currentRole.roleTypeId}">${currentRole.get("description", locale)}</option>
              </#if>
              <option value="">${uiLabelMap.CommonAnyRoleType}</option>
              <#list roleTypes as roleType>
                  <option value="${roleType.roleTypeId}">${roleType.get("description", locale)}</option>
              </#list>
          </select>

          <label for="createdBy" class="right-column">${uiLabelMap.CommonCreatedBy}</label>
          <input type='text' id='createdBy' name='createdBy' value='${requestParameters.createdBy!}'/>

          <#if goodIdentificationTypes?has_content>
              <label for="goodIdentificationTypeId" class="left-column">${uiLabelMap.ProductGoodIdentificationType}</label>
              <select id="goodIdentificationTypeId" name='goodIdentificationTypeId'>
                  <#if currentGoodIdentificationType?has_content>
                      <option value="${currentGoodIdentificationType.goodIdentificationTypeId}">${currentGoodIdentificationType.get("description", locale)}</option>
                      <option value="${currentGoodIdentificationType.goodIdentificationTypeId}">---</option>
                  </#if>
                  <option value="">${uiLabelMap.ProductAnyGoodIdentification}</option>
                  <#list goodIdentificationTypes as goodIdentificationType>
                      <option value="${goodIdentificationType.goodIdentificationTypeId}">${goodIdentificationType.get("description", locale)}</option>
                  </#list>
              </select>

              <label for="goodIdentificationIdValue" class="right-column">${uiLabelMap.ProductGoodIdentification}</label>
              <input id='goodIdentificationIdValue' type='text' name='goodIdentificationIdValue' value='${requestParameters.goodIdentificationIdValue!}'/>
          </#if>

          <label for="productId" class="left-column">${uiLabelMap.ProductProductId}</label>
          <@htmlTemplate.lookupField value='${requestParameters.productId!}' formName="lookuporder" name="productId" id="productId" fieldFormName="LookupProduct"/>

          <label for="internalCode" class="right-column">${uiLabelMap.OrderInternalCode}</label>
          <input id='internalCode' type='text' name='internalCode' value='${requestParameters.internalCode!}'/>

          <label for="inventoryItemId" class="left-column">${uiLabelMap.ProductInventoryItemId}</label>
          <input id='inventoryItemId' type='text' name='inventoryItemId' value='${requestParameters.inventoryItemId!}'/>

          <label for="serialNumber">${uiLabelMap.ProductSerialNumber}</label>
          <input id='serialNumber' type='text' name='serialNumber' value='${requestParameters.serialNumber!}'/>

          <label for="softIdentifier">${uiLabelMap.ProductSoftIdentifier}</label>
          <input id='softIdentifier' type='text' name='softIdentifier' value='${requestParameters.softIdentifier!}'/>

          <label class="left-column">${uiLabelMap.CommonStatus}</label>
          <div class="full-span-field">
              <#list orderStatuses as orderStatus>
                  <label>
                      <input type="checkbox" name="orderStatusId" value="${orderStatus.statusId}" <#if currentStatuses?has_content && currentStatuses.contains(orderStatus.statusId)>checked</#if>/>
                      ${orderStatus.get("description", locale)}
                  </label>
              </#list>
          </div>

          <label for="billingAccountId" class="left-column">${uiLabelMap.AccountingBillingAccount}</label>
          <@htmlTemplate.lookupField value='${requestParameters.billingAccountId!}' formName="lookuporder" name="billingAccountId" id="billingAccountId" fieldFormName="LookupBillingAccount"/>

          <label for="paymentStatusId">${uiLabelMap.AccountingPaymentStatus}</label>
          <select id='paymentStatusId' name="paymentStatusId">
              <option value="">${uiLabelMap.CommonAll}</option>
              <#list paymentStatusList as paymentStatus>
                  <option value="${paymentStatus.statusId}">${paymentStatus.get("description", locale)}</option>
              </#list>
          </select>

          <label for="productStoreId" class="left-column">${uiLabelMap.ProductProductStore}</label>
          <select id='productStoreId' name='productStoreId'>
              <#if currentProductStore?has_content>
                  <option value="${currentProductStore.productStoreId}">${currentProductStore.storeName!}</option>
                  <option value="${currentProductStore.productStoreId}">---</option>
              </#if>
              <option value="">${uiLabelMap.CommonAnyStore}</option>
              <#list productStores as store>
                  <option value="${store.productStoreId}">${store.storeName!}</option>
              </#list>
          </select>

          <label for="salesChannelEnumId">${uiLabelMap.OrderSalesChannel}</label>
          <select id="salesChannelEnumId" name='salesChannelEnumId'>
              <#if currentSalesChannel?has_content>
                  <option value="${currentSalesChannel.enumId}">${currentSalesChannel.get("description", locale)}</option>
                  <option value="${currentSalesChannel.enumId}">---</option>
              </#if>
              <option value="">${uiLabelMap.CommonAnySalesChannel}</option>
              <#list salesChannels as channel>
                  <option value="${channel.enumId}">${channel.get("description", locale)}</option>
              </#list>
          </select>

          <label for="orderWebSiteId">${uiLabelMap.ProductWebSite}</label>
          <select id='orderWebSiteId' name='orderWebSiteId'>
              <#if currentWebSite?has_content>
                  <option value="${currentWebSite.webSiteId}">${currentWebSite.siteName}</option>
                  <option value="${currentWebSite.webSiteId}">---</option>
              </#if>
              <option value="">${uiLabelMap.CommonAnyWebSite}</option>
              <#list webSites as webSite>
                  <option value="${webSite.webSiteId}">${webSite.siteName!}</option>
              </#list>
          </select>

          <label for="shipmentMethod" class="left-column">${uiLabelMap.OrderSelectShippingMethod}</label>
          <select id="shipmentMethod" name="shipmentMethod">
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

          <label for="countryGeoId">${uiLabelMap.OrderShipToCountry}</label>
          <div>
              <select id="countryGeoId" name="countryGeoId">
                  <#if requestParameters.countryGeoId?has_content>
                      <#assign countryGeoId = requestParameters.countryGeoId>
                      <#assign geo = delegator.findOne("Geo", Static["org.apache.ofbiz.base.util.UtilMisc"].toMap("geoId", countryGeoId), true)>
                      <option value="${countryGeoId}" selected="selected">${geo.geoName!}</option>
                      <option value="" >${uiLabelMap.CommonAny}</option>
                  <#else>
                      <option value="" selected="selected">${uiLabelMap.CommonAny}</option>
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
          </div>

          <label for="hasBackOrders">${uiLabelMap.OrderContainsBackOrders}</label>
          <select id="hasBackOrders" name='hasBackOrders'>
              <#if requestParameters.hasBackOrders?has_content>
                  <option value="Y">${uiLabelMap.OrderBackOrders}</option>
                  <option value="Y">---</option>
              </#if>
              <option value="">${uiLabelMap.CommonShowAll}</option>
              <option value="Y">${uiLabelMap.CommonOnly}</option>
          </select>

          <label for="isViewed">${uiLabelMap.OrderViewed}</label>
          <select id="isViewed" name="isViewed">
              <#if requestParameters.isViewed?has_content>
                  <#assign isViewed = requestParameters.isViewed>
                  <option value="${isViewed}"><#if "Y" == isViewed>${uiLabelMap.CommonYes}<#elseif "N" == isViewed>${uiLabelMap.CommonNo}</#if></option>
              </#if>
              <option value=""></option>
              <option value="Y">${uiLabelMap.CommonYes}</option>
              <option value="N">${uiLabelMap.CommonNo}</option>
          </select>

          <label for="gatewayAvsResult">${uiLabelMap.OrderAddressVerification}</label>
          <input id="gatewayAvsResult" type='text' name='gatewayAvsResult' value='${requestParameters.gatewayAvsResult!}'/>

          <label for="gatewayScoreResult">${uiLabelMap.OrderScore}</label>
          <input id="gatewayScoreResult" type='text' name='gatewayScoreResult' value='${requestParameters.gatewayScoreResult!}'/>

          <label class="left-column">Filter</label>
          <div>
              <input type="checkbox" id="filterInventoryProblems" name="filterInventoryProblems" value="Y"
                     <#if "Y" == requestParameters.filterInventoryProblems?default("N")>checked="checked"</#if> />
              <label for="filterInventoryProblems">${uiLabelMap.OrderFilterOn} ${uiLabelMap.OrderFilterInventoryProblems}</label>
          </div>

          <label class="left-column"></label>
          <div>
              <input type="checkbox" id="filterPartiallyReceivedPOs" name="filterPartiallyReceivedPOs" value="Y"
                     <#if "Y" == requestParameters.filterPartiallyReceivedPOs?default("N")>checked="checked"</#if> />
              <label for="filterPartiallyReceivedPOs">${uiLabelMap.OrderFilterOn} ${uiLabelMap.OrderFilterPOs} ${uiLabelMap.OrderFilterPartiallyReceivedPOs}</label>
          </div>

          <label class="left-column"></label>
          <div>
              <input type="checkbox" id="filterPOsOpenPastTheirETA" name="filterPOsOpenPastTheirETA" value="Y"
                     <#if "Y" == requestParameters.filterPOsOpenPastTheirETA?default("N")>checked="checked"</#if> />
              <label for="filterPOsOpenPastTheirETA">${uiLabelMap.OrderFilterOn} ${uiLabelMap.OrderFilterPOs} ${uiLabelMap.OrderFilterPOsOpenPastTheirETA}</label>
          </div>

          <label class="left-column"></label>
          <div>
              <input type="checkbox" id="filterPOsWithRejectedItems" name="filterPOsWithRejectedItems" value="Y"
                     <#if "Y" == requestParameters.filterPOsWithRejectedItems?default("N")>checked="checked"</#if> />
              <label for="filterPOsWithRejectedItems">${uiLabelMap.OrderFilterOn} ${uiLabelMap.OrderFilterPOs} ${uiLabelMap.OrderFilterPOsWithRejectedItems}</label>
          </div>

          <input type="hidden" name="showAll" value="Y"/>
          <div class="full-width-center">
              <input type='submit' value='${uiLabelMap.CommonFind}'/>
          </div>
      </div>

      </#if>
</div>
</form>
<#if requestParameters.hideFields?default("N") != "Y">
<script type="application/javascript">
document.lookuporder.orderId.focus();
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
      <input type="hidden" name="showAll" value="Y"/>
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
           <option value="javascript:void(0);">${uiLabelMap.OrderAnyOrderStatus}</option>
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
        <#if printers?has_content>
        <select name="printerName">
           <option value="javascript:void(0);">${uiLabelMap.CommonPleaseSelectPrinter}</option>
           <#list printers as printer>
           <option value="${printer}">${printer}</option>
           </#list>
        </select>
        </#if>
        <input id="submitButton" type="button" onclick="javascript:runAction();" value="${uiLabelMap.OrderRunAction}" disabled="disabled" />
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
