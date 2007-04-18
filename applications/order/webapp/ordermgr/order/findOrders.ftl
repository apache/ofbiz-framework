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
        if (element.name == "orderIdList") {
            element.checked = master.checked;
            element.disabled = !element.disabled;
        }
    }
}
function setServiceName(selection) {
    document.massOrderChangeForm.action = selection.value;
}
function runAction() {
    var form = document.massOrderChangeForm;
    var orders = form.elements.length;
    for (var i = 0; i < orders; i++) {
        var element = form.elements[i];
        if (element.name == "orderIdList") {
            element.disabled = false;
        }
    }
    form.submit();
}
// -->
</script>

<#if security.hasEntityPermission("ORDERMGR", "_VIEW", session)>
<form method="post" name="lookuporder" action="<@ofbizUrl>searchorders</@ofbizUrl>" onsubmit="javascript:lookupOrders();">
<input type="hidden" name="lookupFlag" value="Y"/>
<input type="hidden" name="hideFields" value="Y"/>
<input type="hidden" name="viewSize" value="${viewSize}"/>
<input type="hidden" name="viewIndex" value="${viewIndex}"/>

<table border="0" width='100%' cellspacing='0' cellpadding='0' class='boxoutside'>
  <tr>
    <td width='100%'>
      <table width='100%' border='0' cellspacing='0' cellpadding='0' class='boxtop'>
        <tr>
          <td><div class='boxhead'>${uiLabelMap.OrderFindOrder}</div></td>
          <td align='right'>
            <div class="tabletext">
              <#if requestParameters.hideFields?default("N") == "Y">
                <a href="<@ofbizUrl>searchorders?hideFields=N&viewSize=${viewSize}&viewIndex=${viewIndex}&${paramList}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonShowLookupFields}</a>
              <#else>
                <#if orderList?exists><a href="<@ofbizUrl>searchorders?hideFields=Y&viewSize=${viewSize}&viewIndex=${viewIndex}&${paramList}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonHideFields}</a></#if>
                <a href="javascript:lookupOrders(true);" class="buttontext">${uiLabelMap.OrderLookupOrder}</a>
                <a href="/partymgr/control/findparty?externalLoginKey=${requestAttributes.externalLoginKey?if_exists}" class="buttontext">${uiLabelMap.PartyLookupParty}</a>
              </#if>
            </div>
          </td>
        </tr>
      </table>
      <#if requestParameters.hideFields?default("N") != "Y">
      <table width='100%' border='0' cellspacing='0' cellpadding='2' class='boxbottom'>
        <tr>
          <td align='center' width='100%'>
            <table border='0' cellspacing='0' cellpadding='2'>
              <tr>
                <td width='25%' align='right'><div class='tableheadtext'>${uiLabelMap.OrderOrderId}</div></td>
                <td width='5%'>&nbsp;</td>
                <td><input type='text' class='inputBox' name='orderId'/></td>
              </tr>
             <tr>
                <td width='25%' align='right'><div class='tableheadtext'>${uiLabelMap.OrderExternalId}</div></td>
                <td width='5%'>&nbsp;</td>
                <td><input type='text' class='inputBox' name='externalId'/></td>
              </tr>
              <tr>
                <td width='25%' align='right'><div class='tableheadtext'>${uiLabelMap.OrderCustomerPo}</div></td>
                <td width='5%'>&nbsp;</td>
                <td><input type='text' class='inputBox' name='correspondingPoId' value='${requestParameters.correspondingPoId?if_exists}'/></td>
              </tr>
              <tr>
                <td width='25%' align='right'><div class='tableheadtext'>${uiLabelMap.OrderInternalCode}</div></td>
                <td width='5%'>&nbsp;</td>
                <td><input type='text' class='inputBox' name='internalCode' value='${requestParameters.internalCode?if_exists}'/></td>
              </tr>
              <tr>
                <td width='25%' align='right'><div class='tableheadtext'>${uiLabelMap.ProductProductId}</div></td>
                <td width='5%'>&nbsp;</td>
                <td><input type='text' class='inputBox' name='productId' value='${requestParameters.productId?if_exists}'/></td>
              </tr>
              <tr>
                <td width='25%' align='right'><div class='tableheadtext'>${uiLabelMap.ProductInventoryItemId}</div></td>
                <td width='5%'>&nbsp;</td>
                <td><input type='text' class='inputBox' name='inventoryItemId' value='${requestParameters.inventoryItemId?if_exists}'/></td>
              </tr>
              <tr>
                <td width='25%' align='right'><div class='tableheadtext'>${uiLabelMap.ProductSerialNumber}</div></td>
                <td width='5%'>&nbsp;</td>
                <td><input type='text' class='inputBox' name='serialNumber' value='${requestParameters.serialNumber?if_exists}'/></td>
              </tr>
              <tr>
                <td width='25%' align='right'><div class='tableheadtext'>${uiLabelMap.ProductSoftIdentifier}</div></td>
                <td width='5%'>&nbsp;</td>
                <td><input type='text' class='inputBox' name='softIdentifier' value='${requestParameters.softIdentifier?if_exists}'/></td>
              </tr>
              <tr>
                <td width='25%' align='right'><div class='tableheadtext'>${uiLabelMap.PartyRoleType}</div></td>
                <td width='5%'>&nbsp;</td>
                <td>
                  <select name='roleTypeId' class='selectBox'>
                    <#if currentRole?has_content>
                    <option value="${currentRole.roleTypeId}">${currentRole.get("description", locale)}</option>
                    <option value="${currentRole.roleTypeId}">---</option>
                    </#if>
                    <option value="">${uiLabelMap.CommonAnyRoleType}</option>
                    <#list roleTypes as roleType>
                      <option value="${roleType.roleTypeId}">${roleType.get("description", locale)}</option>
                    </#list>
                  </select>
                </td>
              </tr>
              <tr>
                <td width='25%' align='right'><div class='tableheadtext'>${uiLabelMap.PartyPartyId}</div></td>
                <td width='5%'>&nbsp;</td>
                <td>
                  <input type='text' class='inputBox' name='partyId' value='${requestParameters.partyId?if_exists}'/>
                  <a href="javascript:call_fieldlookup2(document.lookuporder.partyId,'LookupPartyName');">
                    <img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt='Click here For Field Lookup'/>
                  </a>
                </td>
              </tr>
              <tr>
                <td width='25%' align='right'><div class='tableheadtext'>${uiLabelMap.PartyUserLoginId}</div></td>
                <td width='5%'>&nbsp;</td>
                <td><input type='text' class='inputBox' name='userLoginId' value='${requestParameters.userLoginId?if_exists}'/></td>
              </tr>
              <tr>
                <td width='25%' align='right'><div class='tableheadtext'>${uiLabelMap.OrderOrderType}</div></td>
                <td width='5%'>&nbsp;</td>
                <td>
                  <select name='orderTypeId' class='selectBox'>
                    <#if currentType?has_content>
                    <option value="${currentType.orderTypeId}">${currentType.get("description", locale)}</option>
                    <option value="${currentType.orderTypeId}">---</option>
                    </#if>
                    <option value="">${uiLabelMap.CommonAnyOrderType}</option>
                    <#list orderTypes as orderType>
                      <option value="${orderType.orderTypeId}">${orderType.get("description", locale)}</option>
                    </#list>
                  </select>
                </td>
              </tr>
              <tr>
                <td width='25%' align='right'><div class='tableheadtext'>${uiLabelMap.AccountingBillingAccount}</div>
                <td width='5%'>&nbsp;</td>
                <td><input type='text' class='inputBox' name='billingAccountId' value='${requestParameters.billingAccountId?if_exists}'/></td>
              </tr>
              <tr>
                <td width='25%' align='right'><div class='tableheadtext'>${uiLabelMap.CommonCreatedBy}</div>
                <td width='5%'>&nbsp;</td>
                <td><input type='text' class='inputBox' name='createdBy' value='${requestParameters.createdBy?if_exists}'/></td>
              </tr>
              <tr>
                <td width='25%' align='right'><div class='tableheadtext'>${uiLabelMap.OrderSalesChannel}</div></td>
                <td width='5%'>&nbsp;</td>
                <td>
                  <select name='salesChannelEnumId' class='selectBox'>
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
                <td width='25%' align='right'><div class='tableheadtext'>${uiLabelMap.ProductProductStore}</div></td>
                <td width='5%'>&nbsp;</td>
                <td>
                  <select name='productStoreId' class='selectBox'>
                    <#if currentProductStore?has_content>
                    <option value="${currentProductStore.productStoreId}">${currentProductStore.storeName?if_exists}</option>
                    <option value="${currentProductStore.productStoreId}">---</option>
                    </#if>
                    <option value="">${uiLabelMap.CommonAnyStore}</option>
                    <#list productStores as store>
                      <option value="${store.productStoreId}">${store.storeName?if_exists}</option>
                    </#list>
                  </select>
                </td>
              </tr>
              <tr>
                <td width='25%' align='right'><div class='tableheadtext'>${uiLabelMap.ProductWebSite}</div></td>
                <td width='5%'>&nbsp;</td>
                <td>
                  <select name='orderWebSiteId' class='selectBox'>
                    <#if currentWebSite?has_content>
                    <option value="${currentWebSite.webSiteId}">${currentWebSite.siteName}</option>
                    <option value="${currentWebSite.webSiteId}">---</option>
                    </#if>
                    <option value="">${uiLabelMap.CommonAnyWebSite}</option>
                    <#list webSites as webSite>
                      <option value="${webSite.webSiteId}">${webSite.siteName}</option>
                    </#list>
                  </select>
                </td>
              </tr>
              <tr>
                <td width='25%' align='right'><div class='tableheadtext'>${uiLabelMap.CommonStatus}</div></td>                
                <td width='5%'>&nbsp;</td>
                <td>
                  <select name='orderStatusId' class='selectBox'>
                    <#if currentStatus?has_content>
                    <option value="${currentStatus.statusId}">${currentStatus.get("description", locale)}</option>
                    <option value="${currentStatus.statusId}">---</option>
                    </#if>
                    <option value="">${uiLabelMap.CommonAnyOrderStatus}</option>
                    <#list orderStatuses as orderStatus>
                      <option value="${orderStatus.statusId}">${orderStatus.get("description", locale)}</option>
                    </#list>
                  </select>
                </td>
              </tr>
              <tr>
                <td width='25%' align='right'><div class='tableheadtext'>${uiLabelMap.OrderContainsBackOrders}</div></td>
                <td width='5%'>&nbsp;</td>
                <td>
                  <select name='hasBackOrders' class='selectBox'>
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
                <td width='25%' align='right'>
                  <div class='tableheadtext'>${uiLabelMap.CommonDateFilter}</div>
                </td>
                <td width='5%'>&nbsp;</td>
                <td>
                  <table border='0' cellspacing='0' cellpadding='0'>
                    <tr>
                      <td nowrap>
                        <input type='text' size='25' class='inputBox' name='minDate' value='${requestParameters.minDate?if_exists}'/>
                        <a href="javascript:call_cal(document.lookuporder.minDate,'${fromDateStr}');"><img src='/images/cal.gif' width='16' height='16' border='0' alt='Calendar'/></a>
                        <span class='tabletext'>${uiLabelMap.CommonFrom}</span>
                      </td>
                    </tr>
                    <tr>
                      <td nowrap>
                        <input type='text' size='25' class='inputBox' name='maxDate' value='${requestParameters.maxDate?if_exists}'/>
                        <a href="javascript:call_cal(document.lookuporder.maxDate,'${thruDateStr}');"><img src='/images/cal.gif' width='16' height='16' border='0' alt='Calendar'/></a>
                        <span class='tabletext'>${uiLabelMap.CommonThru}</span>
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>
              <tr>
                <td width='25%' align='right'>
                  <div class='tableheadtext'>${uiLabelMap.OrderFilterOn} ${uiLabelMap.OrderFilterInventoryProblems}</div>
                </td>
                <td width='5%'>&nbsp;</td>
                <td>
                  <table border='0' cellspacing='0' cellpadding='0'>
                    <tr>
                      <td nowrap>
                        <input type="checkbox" name="filterInventoryProblems" value="Y"
                            <#if requestParameters.filterInventoryProblems?default("N") == "Y">checked="checked"</#if> />                    
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>                            
                <td width='25%' align='right'>
                  <div class='tableheadtext'>${uiLabelMap.OrderFilterOn} ${uiLabelMap.OrderFilterPOs} ${uiLabelMap.OrderFilterPartiallyReceivedPOs}</div>
                </td>
                <td width='5%'>&nbsp;</td>
                <td>
                  <table border='0' cellspacing='0' cellpadding='0'>
                    <tr>
                      <td nowrap>
                        <input type="checkbox" name="filterPartiallyReceivedPOs" value="Y"
                            <#if requestParameters.filterPartiallyReceivedPOs?default("N") == "Y">checked="checked"</#if> />                    
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>                            
              <tr>
                <td width='25%' align='right'>
                  <div class='tableheadtext'>${uiLabelMap.OrderFilterOn} ${uiLabelMap.OrderFilterPOs} ${uiLabelMap.OrderFilterPOsOpenPastTheirETA}</div>
                </td>
                <td width='5%'>&nbsp;</td>
                <td>
                  <table border='0' cellspacing='0' cellpadding='0'>
                    <tr>
                      <td nowrap>
                        <input type="checkbox" name="filterPOsOpenPastTheirETA" value="Y"
                            <#if requestParameters.filterPOsOpenPastTheirETA?default("N") == "Y">checked="checked"</#if> />                    
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>                            
              <tr>
                <td width='25%' align='right'>
                  <div class='tableheadtext'>${uiLabelMap.OrderFilterOn} ${uiLabelMap.OrderFilterPOs} ${uiLabelMap.OrderFilterPOsWithRejectedItems}</div>
                </td>
                <td width='5%'>&nbsp;</td>
                <td>
                  <table border='0' cellspacing='0' cellpadding='0'>
                    <tr>
                      <td nowrap>
                        <input type="checkbox" name="filterPOsWithRejectedItems" value="Y"
                            <#if requestParameters.filterPOsWithRejectedItems?default("N") == "Y">checked="checked"</#if> />                    
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>                            
              <tr><td colspan="3"><hr class="sepbar"/></td></tr>
              <tr>
                <td width='25%' align='right'>&nbsp;</td>
                <td width='5%'>&nbsp;</td>
                <td>
                  <div class="tabletext">
                    <input type='checkbox' name='showAll' value='Y' onclick="javascript:lookupOrders(true);"/>&nbsp;${uiLabelMap.CommonShowAllRecords}
                  </div>
                </td>
              </tr>
            </table>
          </td>
        </tr>
      </table>
      </#if>
    </td>
  </tr>
</table>
<input type="image" src="<@ofbizContentUrl>/images/spacer.gif</@ofbizContentUrl>" onClick="javascript:lookupOrders(true);"/>
</form>
<#if requestParameters.hideFields?default("N") != "Y">
<script language="JavaScript" type="text/javascript">
<!--//
document.lookuporder.orderId.focus();
//-->
</script>
</#if>

<br/>
<form name="massOrderChangeForm" method="post" action="javascript:void();">
<table border="0" width='100%' cellspacing='0' cellpadding='0' class='boxoutside'>
  <tr>
    <td width='100%'>
      <table width='100%' border='0' cellspacing='0' cellpadding='0' class='boxtop'>
        <tr>
          <td width="50%"><div class="boxhead">${uiLabelMap.OrderOrderFound}</div></td>
          <td width="50%">
            <div class="boxhead" align="right">
              <#if (orderList?has_content && 0 < orderList?size)>
                <#if (viewIndex > 1)>
                  <a href="<@ofbizUrl>searchorders?viewSize=${viewSize}&viewIndex=${viewIndex-1}&hideFields=${requestParameters.hideFields?default("N")}&${paramList}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonPrevious}</a>
                <#else>
                  <span class="buttontextdisabled">${uiLabelMap.CommonPrevious}</span>
                </#if>
                <#if (orderListSize > 0)>
                  <span class="submenutextinfo">${lowIndex} - ${highIndex} ${uiLabelMap.CommonOf} ${orderListSize}</span>
                </#if>
                <#if (orderListSize > highIndex)>
                  <a href="<@ofbizUrl>searchorders?viewSize=${viewSize}&viewIndex=${viewIndex+1}&hideFields=${requestParameters.hideFields?default("N")}&${paramList}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonNext}</a>
                <#else>
                  <span class="buttontextdisabled">${uiLabelMap.CommonNext}</span>
                </#if>
              </#if>
              &nbsp;
            </div>
          </td>
        </tr>
      </table>

      <div>&nbsp;</div>
      <div align="right" class="tabletext">
        <input type="hidden" name="orderIdList" value=""/>
        <input type="hidden" name="screenLocation" value="component://order/widget/ordermgr/OrderPrintForms.xml#OrderPDF"/>
        <select name="serviceName" class="selectBox" onchange="javascript:setServiceName(this);">
           <option value="javascript:void();">&nbsp;</option>
           <option value="<@ofbizUrl>massApproveOrders?hideFields=${requestParameters.hideFields?default("N")}${paramList}</@ofbizUrl>">${uiLabelMap.OrderApproveOrder}</option>
           <option value="<@ofbizUrl>massPickOrders?hideFields=${requestParameters.hideFields?default("N")}${paramList}</@ofbizUrl>">${uiLabelMap.OrderPickOrders}</option>
           <option value="<@ofbizUrl>massPrintOrders?hideFields=${requestParameters.hideFields?default('N')}${paramList}</@ofbizUrl>">${uiLabelMap.CommonPrint}</option>
           <option value="<@ofbizUrl>massCreateFileForOrders?hideFields=${requestParameters.hideFields?default('N')}${paramList}</@ofbizUrl>">${uiLabelMap.ContentCreateFile}</option>
        </select>
        <select name="printerName" class="selectBox">
           <option value="javascript:void();">&nbsp;</option>
           <#list printers as printer>
           <option value="${printer}">${printer}</option>
           </#list>
        </select>
        <a href="javascript:runAction();" class="buttontext">${uiLabelMap.OrderRunAction}</a>
      </div>

      <table width='100%' border='0' cellspacing='0' cellpadding='2' class='boxbottom'>
        <tr>
          <td width="1%" align="left">
            <input type="checkbox" name="checkAllOrders" value="1" onchange="javascript:toggleOrderId(this);"/>
          </td>
          <td width="5%" align="left"><div class="tableheadtext">${uiLabelMap.OrderOrderType}</div></td>
          <td width="5%" align="left"><div class="tableheadtext">${uiLabelMap.OrderOrderId}</div></td>
          <td width="20%" align="left"><div class="tableheadtext">${uiLabelMap.PartyName}</div></td>
          <td width="5%" align="right"><div class="tableheadtext">${uiLabelMap.OrderSurvey}</div></td>
          <td width="5%" align="right"><div class="tableheadtext">${uiLabelMap.OrderItemsOrdered}</div></td>
          <td width="5%" align="right"><div class="tableheadtext">${uiLabelMap.OrderItemsBackOrdered}</div></td>
          <td width="5%" align="right"><div class="tableheadtext">${uiLabelMap.OrderItemsReturned}</div></td>
          <td width="10%" align="right"><div class="tableheadtext">${uiLabelMap.OrderRemainingSubTotal}</div></td>
          <td width="10%" align="right"><div class="tableheadtext">${uiLabelMap.OrderOrderTotal}</div></td>
          <td width="5%" align="left"><div class="tableheadtext">&nbsp;</div></td>
            <#if (requestParameters.filterInventoryProblems?default("N") == "Y") || (requestParameters.filterPOsOpenPastTheirETA?default("N") == "Y") || (requestParameters.filterPOsWithRejectedItems?default("N") == "Y") || (requestParameters.filterPartiallyReceivedPOs?default("N") == "Y")> 
              <td width="15%" align="left"><div class="tableheadtext">${uiLabelMap.CommonStatus}</div></td>
              <td width="5%"><div class="tabletext"><b>${uiLabelMap.CommonFilter}</b></div></td>
            <#else>
              <td width="20%" align="left"><div class="tableheadtext">${uiLabelMap.CommonStatus}</div></td>
            </#if>          
          <td width="20%" align="left"><div class="tableheadtext">${uiLabelMap.OrderDate}</div></td>
          <td width="5%" align="left"><div class="tableheadtext">${uiLabelMap.PartyPartyId}</div></td>
          <td width="10%">&nbsp;</td>
        </tr>
        <tr>
          <td colspan='15'><hr class='sepbar'/></td>
        </tr>
        <#if orderList?has_content>
          <#assign rowClass = "viewManyTR2">
          <#list orderList as orderHeader>
            <#assign orh = Static["org.ofbiz.order.order.OrderReadHelper"].getHelper(orderHeader)>
            <#assign statusItem = orderHeader.getRelatedOneCache("StatusItem")>
            <#assign orderType = orderHeader.getRelatedOneCache("OrderType")>
            <#if orderType.orderTypeId == "PURCHASE_ORDER">
              <#assign displayParty = orh.getSupplierAgent()?if_exists>
            <#else>
              <#assign displayParty = orh.getPlacingParty()?if_exists>
            </#if>
            <#assign partyId = displayParty.partyId?default("_NA_")>
            <tr class='${rowClass}'>
              <td>
                 <input type="checkbox" name="orderIdList" value="${orderHeader.orderId}"/>
              </td>
              <td><div class='tabletext'>${orderType.get("description",locale)?default(orderType.orderTypeId?default(""))}</div></td>
              <td><a href="<@ofbizUrl>orderview?orderId=${orderHeader.orderId}</@ofbizUrl>" class='buttontext'>${orderHeader.orderId}</a></td>
              <td>
                <div class="tabletext">
                  <#if displayParty?has_content>
                      <#assign displayPartyNameResult = dispatcher.runSync("getPartyNameForDate", Static["org.ofbiz.base.util.UtilMisc"].toMap("partyId", displayParty.partyId, "compareDate", orderHeader.orderDate, "userLogin", userLogin))/>
                      ${displayPartyNameResult.fullName?default("[${uiLabelMap.OrderPartyNameNotFound}]")}
                  <#else>
                    ${uiLabelMap.CommonNA}
                  </#if>
                </div>
                <#--
                <div class='tabletext'>

                <#if placingParty?has_content>
                  <#assign partyId = placingParty.partyId>
                  <#if placingParty.getEntityName() == "Person">
                    <#if placingParty.lastName?exists>
                      ${placingParty.lastName}<#if placingParty.firstName?exists>, ${placingParty.firstName}</#if>
                    <#else>
                      ${uiLabelMap.CommonNA}
                    </#if>
                  <#else>
                    <#if placingParty.groupName?exists>
                      ${placingParty.groupName}
                    <#else>
                      ${uiLabelMap.CommonNA}
                    </#if>
                  </#if>
                <#else>
                  ${uiLabelMap.CommonNA}
                </#if>
                </div>
                -->
              </td>
              <td align="right"><div class="tabletext">${orh.hasSurvey()?string.number}</div></td>
              <td align="right"><div class="tabletext">${orh.getTotalOrderItemsQuantity()?string.number}</div></td>
              <td align="right"><div class="tabletext">${orh.getOrderBackorderQuantity()?string.number}</div></td>
              <td align="right"><div class="tabletext">${orh.getOrderReturnedQuantity()?string.number}</div></td>
              <td align="right"><div class="tabletext"><@ofbizCurrency amount=orderHeader.remainingSubTotal isoCode=orh.getCurrency()/></div></td>
              <td align="right"><div class="tabletext"><@ofbizCurrency amount=orderHeader.grandTotal isoCode=orh.getCurrency()/></div></td>

              <td>&nbsp;</td>
              <td><div class="tabletext">${statusItem.get("description",locale)?default(statusItem.statusId?default("N/A"))}</div></td>
                                </td>
              <#if (requestParameters.filterInventoryProblems?default("N") == "Y") || (requestParameters.filterPOsOpenPastTheirETA?default("N") == "Y") || (requestParameters.filterPOsWithRejectedItems?default("N") == "Y") || (requestParameters.filterPartiallyReceivedPOs?default("N") == "Y")> 
                  <td class="tabletext">
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
              
              <td><div class="tabletext">${orderHeader.getString("orderDate")}</div></td>
              <td>
                <#if partyId != "_NA_">
                  <a href="${customerDetailLink}${partyId}" class="buttontext">${partyId}</a>
                <#else>
                  <span class='tabletext'>${uiLabelMap.CommonNA}</span>
                </#if>
              </td>
              <td align='right'>
                <a href="<@ofbizUrl>orderview?orderId=${orderHeader.orderId}</@ofbizUrl>" class='buttontext'>${uiLabelMap.CommonView}</a>
              </td>
            </tr>
            <#-- toggle the row color -->
            <#if rowClass == "viewManyTR2">
              <#assign rowClass = "viewManyTR1">
            <#else>
              <#assign rowClass = "viewManyTR2">
            </#if>
          </#list>
        <#else>
          <tr>
            <td colspan='4'><div class='head3'>${uiLabelMap.OrderNoOrderFound}</div></td>
          </tr>
        </#if>
        <#if lookupErrorMessage?exists>
          <tr>
            <td colspan='4'><div class="head3">${lookupErrorMessage}</div></td>
          </tr>
        </#if>
      </table>
    </td>
  </tr>
</table>
</form>

<#else>
  <h3>${uiLabelMap.OrderViewPermissionError}</h3>
</#if>
