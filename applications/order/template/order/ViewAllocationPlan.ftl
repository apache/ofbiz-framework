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
    function toggleAllItems(master) {
        var form = document.updateAllocationPlanItems;
        var length = form.elements.length;
        for (var i = 0; i < length; i++) {
            var element = form.elements[i];
            if (element.name.match(/rsm_.*/)) {
                element.checked = master.checked;
            }
        }
        if (master.checked) {
            jQuery('#saveItemsButton').attr("href", "javascript:runAction();");
        } else {
            jQuery('#saveItemsButton').attr("href", "javascript: void(0);");
        }
    }

    function runAction() {
        var form = document.updateAllocationPlanItems;
        form.submit();
    }

    function toggleItem() {
        var form = document.updateAllocationPlanItems;
        var length = form.elements.length;
        var isAllSelected = true;
        var isAnyOneSelected = false;
        for (var i = 0; i < length; i++) {
            var element = form.elements[i];
            if (element.name.match(/rsm_.*/)) {
                if (element.checked) {
                    isAnyOneSelected = true;
                } else {
                    isAllSelected = false;
                }
            }
        }
        jQuery('#checkAllItems').attr("checked", isAllSelected);
        if (isAnyOneSelected || isAllSelected) {
            jQuery('#saveItemsButton').attr("href", "javascript:runAction();");
        } else {
            jQuery('#saveItemsButton').attr("href", "javascript: void(0);");
        }
    }

    $(document).ready(function(){
        $(".up,.down").click(function(){
            var rowCount = $('#allocatioPlanItemsTable tr').length;
            var row = $(this).parents("tr:first");
            if ($(this).is(".up")) {
                if (row.index() != 1) {
                    row.insertBefore(row.prev());
                }
            } else {
                row.insertAfter(row.next());
            }

            //run through each row and reassign the priority
            $('#allocatioPlanItemsTable tr').each(function (i, row) {
                if (i != 0) {
                    var prioritySeqInput = $(row).find('.prioritySeqId');
                    prioritySeqInput.attr("value", i);
                }
            });
        });
    });
</script>
<#if security.hasPermission("ALLOCPLAN_VIEW", session)>
  <#if allocationPlanInfo.allocationPlanHeader?has_content>
    <#assign statusItem = delegator.findOne("StatusItem", {"statusId" : allocationPlanInfo.statusId!}, false)!/>
    <#if !editMode?exists>
        <#assign editMode = false/>
    </#if>
    <#-- Overview Section -->
    <div id="allocationPlanOverview" class="screenlet">
      <div class="screenlet-title-bar">
        <ul>
          <li class="h3">${uiLabelMap.OrderOverview} [${uiLabelMap.CommonId}:${allocationPlanInfo.planId!}]</li>
          <#if (allocationPlanInfo.statusId! == "ALLOC_PLAN_CREATED" || allocationPlanInfo.statusId! == "ALLOC_PLAN_APPROVED") && security.hasPermission("ALLOCPLAN_CANCEL", session)>
            <li>
              <a href="javascript:document.CancelPlan.submit()">${uiLabelMap.OrderCancelPlan}</a>
              <form class="basic-form" name="CancelPlan" method="post" action="<@ofbizUrl>changeAllocationPlanStatus/orderview</@ofbizUrl>">
                <input type="hidden" name="planId" value="${allocationPlanInfo.planId!}"/>
                <input type="hidden" name="statusId" value="ALLOC_PLAN_CANCELLED"/>
              </form>
            </li>
          </#if>
          <#if (allocationPlanInfo.statusId! == "ALLOC_PLAN_CREATED") && security.hasPermission("ALLOCPLAN_APPROVE", session)>
            <li>
              <a href="javascript:document.ApprovePlan.submit()">${uiLabelMap.OrderApprovePlan}</a>
              <form class="basic-form" name="ApprovePlan" method="post" action="<@ofbizUrl>changeAllocationPlanStatus/orderview</@ofbizUrl>">
                <input type="hidden" name="planId" value="${allocationPlanInfo.planId!}"/>
                <input type="hidden" name="statusId" value="ALLOC_PLAN_APPROVED"/>
              </form>
            </li>
          </#if>
        </ul>
        <br class="clear"/>
      </div>
      <div class="screenlet-body">
        <table class="basic-table form-table" cellspacing="0">
          <tbody>
            <tr>
              <td align="center"><label><b>${uiLabelMap.CommonName}</b></label></td>
              <td align="left"><a href="<@ofbizUrl controlPath="/ordermgr/control">ViewAllocationPlan?planId=${allocationPlanInfo.planId!}</@ofbizUrl>" title="${allocationPlanInfo.planId!}"> ${allocationPlanInfo.planName!}</a></td>
              <td align="center"><label><b>${uiLabelMap.OrderProduct}</b></label></td>
              <td align="left"><a href="<@ofbizUrl controlPath="/catalog/control">EditProduct?productId=${allocationPlanInfo.productId!}</@ofbizUrl>" title="${allocationPlanInfo.productId!}">${allocationPlanInfo.productName!}</a></td>
              <td align="center"><label><b>${uiLabelMap.CommonCreatedBy}</b></label></td>
              <td align="left">${allocationPlanInfo.createdBy!}</td>
            </tr>
            <tr>
              <td align="center"><label><b>${uiLabelMap.CommonStatus}</b></label></td>
              <td align="left">${statusItem.get("description")!}</td>
              <td align="center"><label><b>${uiLabelMap.ProductAtp}/${uiLabelMap.ProductQoh}</b></label></td>
              <td align="left">${allocationPlanInfo.totalATP!}/${allocationPlanInfo.totalQOH!}</td>
              <td align="center"><label><b>${uiLabelMap.OrderRequestCreatedDate}</b></label></td>
              <td align="left">${allocationPlanInfo.createdDate!}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
    <#-- Summary Section -->
    <div id="allocationPlanSummary" class="screenlet">
      <div class="screenlet-title-bar">
        <ul>
          <li class="h3">${uiLabelMap.CommonSummary}</li>
        </ul>
        <br class="clear"/>
      </div>
      <div class="screenlet-body">
          <table class="basic-table hover-bar" cellspacing='0'>
            <tr class="header-row">
              <td width="20%">${uiLabelMap.OrderOrderingChannel}</td>
              <td align="right" width="16%">${uiLabelMap.OrderOrderedUnits}</td>
              <td align="right" width="16%">${uiLabelMap.OrderOrderedValue}</td>
              <td align="right" width="16%">${uiLabelMap.OrderAllocatedUnits}</td>
              <td align="right" width="16%">${uiLabelMap.OrderAllocatedValue}</td>
              <td align="right" width="16%">${uiLabelMap.OrderAllocation} %</td>
            </tr>
            <#list allocationPlanInfo.summaryMap.keySet() as key>
              <#assign summary = allocationPlanInfo.summaryMap.get(key)/>
              <tr>
                <td>${summary.salesChannel!}</td>
                <td align="right">${summary.orderedQuantity!}</td>
                <td align="right"><@ofbizCurrency amount=summary.orderedValue?default(0.00) isoCode=currencyUomId/></td>
                <td align="right">${summary.allocatedQuantity!}</td>
                <td align="right"><@ofbizCurrency amount=summary.allocatedValue?default(0.00) isoCode=currencyUomId/></td>
                <td align="right">${summary.allocationPercentage!?string("0.####")}</td>
              </tr>
            </#list>
            <tr>
              <td ><b>${uiLabelMap.CommonTotal}</b></td>
              <td align="right"><b>${allocationPlanInfo.orderedQuantityTotal!}</b></td>
              <td align="right"><b><@ofbizCurrency amount=allocationPlanInfo.orderedValueTotal?default(0.00) isoCode=currencyUomId/></td>
              <td align="right"><b>${allocationPlanInfo.allocatedQuantityTotal!}</b></td>
              <td align="right"><b><@ofbizCurrency amount=allocationPlanInfo.allocatedValueTotal?default(0.00) isoCode=currencyUomId/></b></td>
              <td align="right"><b>${allocationPlanInfo.allocationPercentageTotal!?string("0.####")}</b></td>
            </tr>
          </table>
      </div>
    </div>
    <#-- Items Section -->
    <div id="allocationPlanItems" class="screenlet">
      <div class="screenlet-title-bar">
        <ul>
          <li class="h3">${uiLabelMap.CommonItems}</li>
          <#if editMode>
            <li><a href="<@ofbizUrl controlPath="/ordermgr/control">ViewAllocationPlan?planId=${allocationPlanInfo.planId!}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonCancel}</a></li>
            <li><a id="saveItemsButton" href="javascript: void(0);" class="buttontext">${uiLabelMap.CommonSave}</a></li>
          <#elseif allocationPlanInfo.statusId! != "ALLOC_PLAN_COMPLETED" && allocationPlanInfo.statusId! != "ALLOC_PLAN_CANCELLED" && security.hasPermission("ALLOCPLAN_UPDATE", session)>
            <li><a href="<@ofbizUrl controlPath="/ordermgr/control">EditAllocationPlan?planId=${allocationPlanInfo.planId!}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonEdit}</a></li>
          </#if>
        </ul>
        <br class="clear"/>
      </div>
      <div class="screenlet-body">
        <#assign rowCount = 0>
        <table id="allocatioPlanItemsTable" class="basic-table hover-bar" cellspacing='0'>
          <form class="basic-form" name="updateAllocationPlanItems" id="updateAllocationPlanItems" method="post" action="<@ofbizUrl>updateAllocationPlanItems</@ofbizUrl>">
            <input type="hidden" name="planId" value="${allocationPlanInfo.planId!}"/>
            <tr class="header-row">
              <#if editMode>
                <td width="5%"><input type="checkbox" id="checkAllItems" name="checkAllItems" onchange="javascript:toggleAllItems(this);"></td>
              </#if>
              <td width="8%">${uiLabelMap.OrderSalesChannel}</td>
              <td width="8%">${uiLabelMap.OrderCustomer}</td>
              <td width="8%">${uiLabelMap.Status}</td>
              <td width="8%">${uiLabelMap.FormFieldTitle_orderId}</td>
              <td width="8%">${uiLabelMap.FormFieldTitle_orderItemSeqId}</td>
              <td width="10%">${uiLabelMap.FormFieldTitle_estimatedShipDate}</td>
              <td align="right" width="8%">${uiLabelMap.OrderOrdered}</td>
              <td align="right" width="8%">${uiLabelMap.ProductReserved}</td>
              <td align="right" width="8%">${uiLabelMap.OrderExtValue}</td>
              <td align="right" width="8%">${uiLabelMap.OrderAllocated}</td>
              <td align="right" width="8%">${uiLabelMap.OrderAllocation} %</td>
              <#if editMode>
                <td align="right" width="5%">${uiLabelMap.FormFieldTitle_actionEnumId}</td>
              </#if>
            </tr>
            <#list allocationPlanInfo.itemList as item>
              <tr>
                <input type="hidden" name="psim_${item.planItemSeqId}" value="${rowCount+1}" class="prioritySeqId"/>
                <input type="hidden" name="pim_${item.planItemSeqId}" value="${item.productId}"/>
                <input type="hidden" name="oim_${item.planItemSeqId}" value="${item.orderId}"/>
                <input type="hidden" name="oisim_${item.planItemSeqId}" value="${item.orderItemSeqId}"/>
                <#if editMode>
                  <td>
                    <input type="checkbox" name="rsm_${item.planItemSeqId}" value="Y" onchange="javascript:toggleItem();">
                  </td>
                </#if>
                <td>${item.salesChannel!}</td>
                <td><a href="<@ofbizUrl controlPath="/partymgr/control">viewprofile?partyId=${item.partyId!}</@ofbizUrl>" title="${item.partyId!}">${item.partyName!}</a></td>
                <#assign statusItem = delegator.findOne("StatusItem", {"statusId" : item.statusId!}, false)!/>
                <td>${statusItem.get("description")}</td>
                <td><a href="<@ofbizUrl controlPath="/ordermgr/control">orderview?orderId=${item.orderId!}</@ofbizUrl>" title="${item.orderId!}">${item.orderId!}</a></td>
                <td>${item.orderItemSeqId!}</td>
                <td>${item.estimatedShipDate!}</td>
                <td align="right">${item.orderedQuantity!}</td>
                <td align="right">${item.reservedQuantity!}</td>
                <td align="right"><@ofbizCurrency amount=item.orderedValue!?default(0.00) isoCode=currencyUomId/></td>
                <#if editMode>
                  <td><input type="text" name="aqm_${item.planItemSeqId}" value="${item.allocatedQuantity!}"/></td>
                  <td align="right">${item.allocationPercentage!?string("0.####")}</td>
                  <td align="right">
                    <a href="#" class="up"><img src="/images/arrow-single-up-green.png"/></a>
                    <a href="#" class="down"><img src="/images/arrow-single-down-green.png"/></a>
                  </td>
                <#else>
                  <td align="right">${item.allocatedQuantity!}</td>
                  <td align="right">${item.allocationPercentage!?string("0.####")}</td>
                </#if>
              </tr>
              <#assign rowCount = rowCount + 1>
            </#list>
            <tr>
              <#if editMode>
                <td></td>
              </#if>
              <td colspan="6"><b>${uiLabelMap.CommonTotal}</b></td>
              <td align="right"><b>${allocationPlanInfo.orderedQuantityTotal!}</b></td>
              <td align="right"><b>${allocationPlanInfo.reservedQuantityTotal!}</b></td>
              <td align="right"><b><@ofbizCurrency amount=allocationPlanInfo.orderedValueTotal?default(0.00) isoCode=currencyUomId/>
              <td align="right"><b>${allocationPlanInfo.allocatedQuantityTotal!}</b></td>
              <td align="right"><b>${allocationPlanInfo.allocationPercentageTotal!?string("0.####")}</b></td>
              <#if editMode>
                <td></td>
              </#if>
            </tr>
            <input type="hidden" name="_rowCount" value="${rowCount}" />
          </form>
        </table>
      </div>
    </div>
  <#else>
    <b>${uiLabelMap.OrderAllocationPlanNotFound}</b>
  </#if>
<#else>
  ${uiLabelMap.OrderAllocationPlanViewPermissionError}
</#if>