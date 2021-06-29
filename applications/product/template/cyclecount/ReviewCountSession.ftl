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
<script type="text/javascript">
    function rejectCountSession(){
        var checkboxes = document.getElementsByClassName("rowSubmit");
        var checkCondition = true;
        for (var i=0; i<checkboxes.length; i++) {
            if (checkboxes[i].checked) {
                checkCondition = false;
            }
        }
        if (checkCondition) {
            alert("Please select row to proceed");
            return false;
        }
        var retVal = confirm('Are you sure want to reject?');
        if (retVal == null || retVal == "") {
            return false;
        } else {
            document.getElementById("statusId").value = "INV_COUNT_REJECTED";
            return true;
        }
    }
    function getValue(){
        var form = document.selectAllForm;
        var checkboxes = document.getElementsByClassName("rowSubmit");
        var checkCondition = true;
        for (var i=0; i<checkboxes.length; i++) {
            if (checkboxes[i].checked) {
                checkCondition = false;
            }
        }
        if (checkCondition) {
            alert("Please select row to proceed");
            return false;
        }
        var retVal = prompt("Schedule Next Cycle Count in (Days)", "60");
        if (retVal == null || retVal == "") {
            return false;
        } else {
            document.getElementById("nextCountDay").value = retVal;
            return true;
        }
    }
    function toggleInventoryCountList() {
        var form = document.selectAllForm;
        var inventoryCounts = form.elements.length;
        var isAllSelected = true;
        for (var i = 0; i < inventoryCounts; i++) {
            var element = form.elements[i];
            if ("bulkAction" == element.id && !element.checked) {
                isAllSelected = false;
            }
            jQuery('#selectAll').attr("checked", isAllSelected);
        }
    }
    function toggleInventoryCount(master) {
        var form = document.selectAllForm;
        var inventoryCounts = form.elements.length;
        for (var i = 0; i < inventoryCounts; i++) {
            var element = form.elements[i];
            if ("bulkAction" == element.id) {
                element.checked = master.checked;
            }
        }
    }
</script>
<#if countSessionValue?has_content>
  <div class="screenlet">
    <div class="screenlet-title-bar">
      <ul>
        <li class="h3">${uiLabelMap.ProductReviewCountSession}</li>
      </ul>
    </div>
    <div class="screenlet-body">
      <table class="basic-table">
        <tr>
          <td style="width: 10%;" align="right" class="label">Session:</td>
          <td>${countSessionValue.inventoryCountId!}</td>
        </tr>
        <tr>
          <td style="width: 10%;" align="right" class="label">Status:</td>
          <td>${countSessionValue.statusDescription!}</td>
        </tr>
        <tr>
          <td style="width: 10%;" align="right" class="label">Created By:</td>
          <td>${countSessionValue.createdBy!}</td>
        </tr>
        <tr>
          <td style="width: 10%;" align="right" class="label">Created On:</td>
          <td><#if countSessionValue.createdDate?has_content>${countSessionValue.createdDate?string("MM/dd/yyyy")}</#if></td>
        </tr>
      </table>
    <div class="screenlet-title-bar">
      <ul>
        <#if "INV_COUNT_APPROVED" == countSessionValue.statusId>
          <li><input type="submit" form="selectAllForm" class="link" name="Reject" value="Reject" onclick="javascript: return rejectCountSession();"/></li>
          <li><input type="submit" form="selectAllForm" class="link" name="Accept" value="Accept" onclick="javascript: return getValue();"/></li>
        </#if>
        <li class="h3">Item Details</li>
      </ul>
    </div>
    <div class="screenlet-body">
      <#assign rowCount = 0>
      <form method="post" id="selectAllForm" action="<@ofbizUrl>acceptCountSessions</@ofbizUrl>" name="selectAllForm">
        <fieldset>
          <input type="hidden" name="_useRowSubmit" value="Y"/>
          <input type="hidden" name="_checkGlobalScope" value="Y" />
          <input type="hidden" id="nextCountDay" name="nextCountDay" value="30"/>
          <input type="hidden" name="inventoryCountId" value="${inventoryCountId!}"/>
          <input type="hidden" id="statusId" name="statusId" value="INV_COUNT_COMPLETED"/>
          <table class="basic-table hover-bar" cellspacing="1">
            <thead>
              <tr class="header-row">
                <th>${uiLabelMap.ProductFacility}</th>
                <th>${uiLabelMap.ProductLocation}</th>
                <th>${uiLabelMap.ProductInventoryItem}</th>
                <th>${uiLabelMap.ProductInternalName}</th>
                <th>${uiLabelMap.ProductQtyCounted}</th>
                <th>Systemic QOH</th>
                <th>${uiLabelMap.ProductQtyVariance}</th>
                <th>Counter User ID</th>
                <th>${uiLabelMap.CommonStatus}</th>
                <th style="text-align:center"><input type="checkbox" title="${uiLabelMap.CommonSelectAll}" <#if "INV_COUNT_APPROVED" != countSessionValue.statusId>disabled="disabled"</#if> name="selectAll" id="selectAll" value="Y" onclick="javascript:toggleInventoryCount(this, 'selectAllForm');"/></th>
              </tr>
            </thead>
            <tbody>
              <#setting number_format=",##0;(,##0)">
              <#list inventoryCountItemAndVariances as inventoryCountItemAndVariance>
                <tr>
                  <td>${inventoryCountItemAndVariance.facilityName!}</td>
                  <td>
                    <a target="_blank" href="<@ofbizUrl>EditFacilityLocation?facilityId=${inventoryCountItemAndVariance.facilityId!}&amp;locationSeqId=${inventoryCountItemAndVariance.locationSeqId!}</@ofbizUrl>">${inventoryCountItemAndVariance.areaId!} <#if inventoryCountItemAndVariance.aisleId?has_content>:${inventoryCountItemAndVariance.aisleId!}</#if><#if inventoryCountItemAndVariance.sectionId?has_content>:${inventoryCountItemAndVariance.sectionId!}</#if><#if inventoryCountItemAndVariance.levelId?has_content>:${inventoryCountItemAndVariance.levelId!}</#if>:${inventoryCountItemAndVariance.positionId!} [${inventoryCountItemAndVariance.locationSeqId!}]</a>
                  </td>
                  <td>
                    <#if inventoryCountItemAndVariance.inventoryItemId?has_content>
                      <a class="smallSubmit" target="_blank" href="/facility/control/EditInventoryItem?inventoryItemId=${inventoryCountItemAndVariance.inventoryItemId!}&amp;facilityId=${inventoryCountItemAndVariance.facilityId!}${StringUtil.wrapString(externalKeyParam!)}">${inventoryCountItemAndVariance.inventoryItemId!}</a>
                    </#if>
                  </td>
                  <td>${inventoryCountItemAndVariance.internalName!}</td>
                  <td><#if inventoryCountItemAndVariance.quantity?has_content && inventoryCountItemAndVariance.quantity &lt; 0><span class="colorVs calculateSum" style="font-weight: bold">${inventoryCountItemAndVariance.quantity!}</span><#else><span class="colorMain" style="font-weight: bold">${inventoryCountItemAndVariance.quantity!}</span></#if></td>
                  <td><#if inventoryCountItemAndVariance.systemQuantityOnHand?has_content && inventoryCountItemAndVariance.systemQuantityOnHand &lt; 0><span class="colorVs calculateSum" style="font-weight: bold">${inventoryCountItemAndVariance.systemQuantityOnHand!}</span><#else><span class="colorMain" style="font-weight: bold">${inventoryCountItemAndVariance.systemQuantityOnHand!}</span></#if></td>
                  <td><#if inventoryCountItemAndVariance.varianceQuantityOnHand?has_content && inventoryCountItemAndVariance.varianceQuantityOnHand &lt; 0><span class="colorVs calculateSum" style="font-weight: bold">${inventoryCountItemAndVariance.varianceQuantityOnHand!}</span><#else><span class="colorMain" style="font-weight: bold">${inventoryCountItemAndVariance.varianceQuantityOnHand!}</span></#if></td>
                  <td>${inventoryCountItemAndVariance.createBy!}</td>
                  <td>${inventoryCountItemAndVariance.statusDescription!}</td>
                  <td style="text-align: center;">
                    <#if "INV_COUNT_APPROVED" == inventoryCountItemAndVariance.itemStatusId>
                      <input type="hidden" name="inventoryCountItemSeqId_o_${rowCount}" value="${inventoryCountItemAndVariance.inventoryCountItemSeqId!}"/>
                      <input type="checkbox" class="rowSubmit" id="bulkAction" name="_rowSubmit_o_${rowCount}" value="Y" onclick="javascript:toggleInventoryCountList(this, 'selectAllForm');"/>
                    </#if>
                  </td>
                </tr>
              <#assign rowCount = rowCount + 1>
            </#list>
            </tbody>
          </table>
        </fieldset>
      </form>
    </div>
    </div>
  </div>
<#else>
  <div class="screenlet">
    <div class="screenlet-title-bar">
      <ul>
        <li class="h3">${uiLabelMap.ProductReviewCountSession}</li>
      </ul>
    </div>
    <div class="screenlet-body">
      ${screens.render("component://product/widget/facility/CycleCountScreens.xml#PendingCycleCountList")}
    </div>
  </div>
</#if>