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
<script language="JavaScript" type="text/javascript">
<!-- //
function lookupShipments() {
    shipmentIdValue = document.lookupShipmentForm.shipmentId.value;
    if (shipmentIdValue.length > 1) {
        document.lookupShipmentForm.action = "<@ofbizUrl>ViewShipment</@ofbizUrl>";
    } else {
        document.lookupShipmentForm.action = "<@ofbizUrl>FindShipment</@ofbizUrl>";
    }
    document.lookupShipmentForm.submit();
}
// -->
</script>
<div id="findOrders" class="screenlet">
    <div class="screenlet-title-bar">
        <ul>
            <li class="h3">${uiLabelMap.ProductFindShipmentTitle}</li>
            <#if requestParameters.facilityId?has_content>
                <li><a href="<@ofbizUrl>quickShipOrder?facilityId=${requestParameters.facilityId}</@ofbizUrl>">${uiLabelMap.ProductQuickShipOrder}</a></li>
            </#if>
            <li><a href="<@ofbizUrl>EditShipment</@ofbizUrl>">${uiLabelMap.ProductNewShipment}</a></li>
            <li><a href="javascript:lookupShipments();">${uiLabelMap.ProductFindShipment}</a></li>
        </ul>
        <br class="clear"/>
    </div>
    <div class="screenlet-body">
        <form method="post" name="lookupShipmentForm" action="<@ofbizUrl>FindShipment</@ofbizUrl>">
            <input type="hidden" name="lookupFlag" value="Y" />
            <table cellspacing="0" cellpadding="2" class="basic-table">
              <tr>
                <td width="25%" align="right" class="label">${uiLabelMap.ProductShipmentId}</td>
                <td width="5%">&nbsp;</td>
                <td><input type="text" name="shipmentId" value="${shipmentId!}" /></td>
              </tr>
              <tr>
                <td width="25%" align="right" class="label">${uiLabelMap.ProductShipmentType}</td>
                <td width="5%">&nbsp;</td>
                <td>
                  <select name="shipmentTypeId">
                    <#if currentShipmentType?has_content>
                    <option value="${currentShipmentType.shipmentTypeId}">${currentShipmentType.get("description",locale)}</option>
                    <option value="${currentShipmentType.shipmentTypeId}">---</option>
                    </#if>
                    <option value="">${uiLabelMap.ProductAnyShipmentType}</option>
                    <#list shipmentTypes as shipmentType>
                      <option value="${shipmentType.shipmentTypeId}">${shipmentType.get("description",locale)}</option>
                    </#list>
                  </select>
                </td>
              </tr>
              <tr>
                <td width="25%" align="right" class="label">${uiLabelMap.ProductOriginFacility}</td>
                <td width="5%">&nbsp;</td>
                <td>
                  <select name="originFacilityId">
                    <#if currentOriginFacility?has_content>
                    <option value="${currentOriginFacility.facilityId}">${currentOriginFacility.facilityName} [${currentOriginFacility.facilityId}]</option>
                    <option value="${currentOriginFacility.facilityId}">---</option>
                    </#if>
                    <option value="">${uiLabelMap.ProductAnyFacility}</option>
                    <#list facilities as facility>
                      <option value="${facility.facilityId}">${facility.facilityName} [${facility.facilityId}]</option>
                    </#list>
                  </select>
                </td>
              </tr>
              <tr>
                <td width="25%" align="right" class="label">${uiLabelMap.ProductDestinationFacility}</td>
                <td width="5%">&nbsp;</td>
                <td>
                  <select name="destinationFacilityId">
                    <#if currentDestinationFacility?has_content>
                    <option value="${currentDestinationFacility.facilityId}">${currentDestinationFacility.facilityName} [${currentDestinationFacility.facilityId}]</option>
                    <option value="${currentDestinationFacility.facilityId}">---</option>
                    </#if>
                    <option value="">${uiLabelMap.ProductAnyFacility}</option>
                    <#list facilities as facility>
                      <option value="${facility.facilityId}">${facility.facilityName} [${facility.facilityId}]</option>
                    </#list>
                  </select>
                </td>
              </tr>
              <tr>
                <td width="25%" align="right" class="label">${uiLabelMap.CommonStatus}</td>
                <td width="5%">&nbsp;</td>
                <td>
                  <select name="statusId">
                    <#if currentStatus?has_content>
                    <option value="${currentStatus.statusId}">${currentStatus.get("description",locale)}</option>
                    <option value="${currentStatus.statusId}">---</option>
                    </#if>
                    <option value="">${uiLabelMap.ProductSalesShipmentStatus}</option>
                    <#list shipmentStatuses as shipmentStatus>
                      <option value="${shipmentStatus.statusId}">${shipmentStatus.get("description",locale)}</option>
                    </#list>
                    <option value="">---</option>
                    <option value="">${uiLabelMap.ProductPurchaseShipmentStatus}</option>
                    <#list purchaseShipmentStatuses as shipmentStatus>
                      <option value="${shipmentStatus.statusId}">${shipmentStatus.get("description",locale)}</option>
                    </#list>
                    <option value="">---</option>
                    <option value="">${uiLabelMap.ProductOrderReturnStatus}</option>
                    <#list returnStatuses as returnStatus>
                      <#if returnStatus.statusId != "RETURN_REQUESTED">
                        <option value="${returnStatus.statusId}">${returnStatus.get("description",locale)}</option>
                      </#if>
                    </#list>
                  </select>
                </td>
              </tr>
              <tr>
                <td width="25%" align="right" class="label">${uiLabelMap.ProductDateFilter}</td>
                <td width="5%">&nbsp;</td>
                <td>
                  <table cellspacing="0" class="basic-table">
                    <tr>
                      <td>
                        <@htmlTemplate.renderDateTimeField name="minDate" event="" action="" className="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" value="${requestParameters.minDate!}" size="25" maxlength="30" id="minDate1" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
                        <span class="label">${uiLabelMap.CommonFrom}</span>
                      </td>
                    </tr>
                    <tr>
                      <td>
                        <@htmlTemplate.renderDateTimeField name="maxDate" event="" action="" className="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" value="${requestParameters.maxDate!}" size="25" maxlength="30" id="maxDate1" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
                        <span class="label">${uiLabelMap.CommonThru}</span>
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>
            </table>
        </form>
    </div>
</div>
<#if shipmentList??>
<div id="findOrders_2" class="screenlet">
    <div class="screenlet-title-bar">
        <ul>
            <li class="h3">${uiLabelMap.ProductShipmentsFound}</li>
            <#if 0 < shipmentList?size>
                <#if (shipmentList?size > highIndex)>
                    <li><a class="nav-next" href="<@ofbizUrl>FindShipment?VIEW_SIZE=${viewSize}&amp;VIEW_INDEX=${viewIndex+1}${paramList}&amp;lookupFlag=Y</@ofbizUrl>">${uiLabelMap.CommonNext}</a></li>
                <#else>
                    <li class="disabled">${uiLabelMap.CommonNext}</li>
                </#if>
                <li>${lowIndex} - ${highIndex} ${uiLabelMap.CommonOf} ${shipmentList?size}</li>
                <#if (viewIndex > 1)>
                    <li><a class="nav-previous" href="<@ofbizUrl>FindShipment?VIEW_SIZE=${viewSize}&amp;VIEW_INDEX=${viewIndex-1}${paramList}&amp;lookupFlag=Y</@ofbizUrl>">${uiLabelMap.CommonPrevious}</a></li>
                <#else>
                    <li class="disabled">${uiLabelMap.CommonPrevious}</li>
                </#if>
            </#if>
        </ul>
        <br class="clear"/>
    </div>
    <div class="screenlet-body">
        <table cellspacing="0" cellpadding="2" class="basic-table hover-bar">
        <tr class="header-row">
          <td width="5%">${uiLabelMap.ProductShipmentId}</td>
          <td width="15%">${uiLabelMap.ProductShipmentType}</td>
          <td width="10%">${uiLabelMap.CommonStatus}</td>
          <td width="25%">${uiLabelMap.ProductOriginFacility}</td>
          <td width="25%">${uiLabelMap.ProductDestFacility}</td>
          <td width="15%">${uiLabelMap.ProductShipDate}</td>
          <td width="5%">&nbsp;</td>
        </tr>
        <#if shipmentList?has_content>
          <#assign alt_row = false>
          <#list shipmentList as shipment>
            <#assign originFacility = delegator.findOne("Facility", Static["org.ofbiz.base.util.UtilMisc"].toMap("facilityId", shipment.originFacilityId), true)! />
            <#assign destinationFacility = delegator.findOne("Facility", Static["org.ofbiz.base.util.UtilMisc"].toMap("facilityId", shipment.destinationFacilityId), true)! />
            <#assign statusItem = delegator.findOne("StatusItem", Static["org.ofbiz.base.util.UtilMisc"].toMap("statusId", shipment.statusId), true)!/>
            <#assign shipmentType = delegator.findOne("ShipmentType", Static["org.ofbiz.base.util.UtilMisc"].toMap("shipmentTypeId", shipment.shipmentTypeId), true)!/>
            <tr valign="middle"<#if alt_row> class="alternate-row"</#if>>
              <td><a href="<@ofbizUrl>ViewShipment?shipmentId=${shipment.shipmentId}</@ofbizUrl>" class="buttontext">${shipment.shipmentId}</a></td>
              <td>${(shipmentType.get("description",locale))?default(shipmentType.shipmentTypeId?default(""))}</td>
              <td>${(statusItem.get("description",locale))?default(statusItem.statusId?default("N/A"))}</td>
              <td>${(originFacility.facilityName)!} [${shipment.originFacilityId!}]</td>
              <td>${(destinationFacility.facilityName)!} [${shipment.destinationFacilityId!}]</td>
              <td><span style="white-space: nowrap;">${(shipment.estimatedShipDate.toString())!}</span></td>
              <td align="right">
                <a href="<@ofbizUrl>ViewShipment?shipmentId=${shipment.shipmentId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonView}</a>
              </td>
            </tr>
            <#-- toggle the row color -->
            <#assign alt_row = !alt_row>
          </#list>
        <#else>
          <tr>
            <td colspan="7"><h3>${uiLabelMap.ProductNoShipmentsFound}.</h3></td>
          </tr>
        </#if>
        </table>
    </div>
</div>
</#if>
