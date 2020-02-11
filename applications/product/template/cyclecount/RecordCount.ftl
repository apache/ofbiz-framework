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
    function getValue(){
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
        return true;
    }
    function checkQuantityValue(){
        var quantities = document.getElementsByClassName("quantity");
        var checkCondition = false;
        for (var i=0; i< quantities.length; i++) {
            if (quantities[i] !== null && quantities[i].value === "") {
                checkCondition = true;
                break;
            }
        }
        if (checkCondition) {
            return confirm("One or more items in the session are not counted yet. Do you really want to proceed ?");
        } else {
            return confirm("You can't add/edit after submitting this session. Do you really want to proceed ?");
        }
        return true;
    }
    function cancelSession(){
        return confirm("Do you really want to reject ?");
    }

    function toggleAll(e) {
        var cform = document.selectAllForm;
        var len = cform.elements.length;
        for (var i = 0; i < len; i++) {
            var element = cform.elements[i];
            if (element.name.substring(0, 10) == "_rowSubmit" && element.checked != e.checked) {
                element.click();
            }
        }
    }
</script>

<#if countSessionValue?has_content>
  <div class="screenlet">
    <div class="screenlet-title-bar">
      <ul>
        <#if "INV_COUNT_CREATED" == countSessionValue.statusId>
          <li>
            <form method="post" action="<@ofbizUrl>approveInventoryCountAndItems</@ofbizUrl>">
              <input type="hidden" name="inventoryCountId" value="${inventoryCountId!}"/>
              <input type="submit" class="link" name="submit" value="Submit" onclick="javascript: return checkQuantityValue();"/>
            </form>
          </li>
          <li>
            <form method="post" action="<@ofbizUrl>approveInventoryCountAndItems</@ofbizUrl>">
              <input type="hidden" name="inventoryCountId" value="${inventoryCountId!}"/>
              <input type="hidden" name="statusId" value="INV_COUNT_REJECTED"/>
              <input type="submit" class="link" name="submit" value="Reject" onclick="javascript: return cancelSession();"/>
            </form>
          </li>
        </#if>
        <li class="h3">${uiLabelMap.FacilityRecordCount}</li>
      </ul>
    </div>
    <div class="screenlet-body">
      <table width="100%">
        <tr>
          <td>
            <div class="screenlet-title-bar">
              <ul>
                <li class="h3">Session Details</li>
              </ul>
            </div>
            <div class="screenlet-body">
              <table class="basic-table">
                <tr>
                  <td align="right" class="label">Session:</td>
                  <td>${countSessionValue.inventoryCountId!}</td>
                </tr>
                <tr>
                  <td align="right" class="label">${uiLabelMap.FacilityFacility}:</td>
                  <td>${countSessionValue.facilityName!}</td>
                </tr>
                <tr>
                  <td align="right" class="label">Status:</td>
                  <td>${countSessionValue.statusDescription!}</td>
                </tr>
                <tr>
                  <td align="right" class="label">Scanned By:</td>
                  <td>${countSessionValue.createdBy!}</td>
                </tr>
                <#if "INV_COUNT_CREATED" == countSessionValue.statusId>
                  <tr>
                    <td align="right" class="label">${uiLabelMap.ProductLocation}:</td>
                    <td id="scanLocationLookup">
                      <form method="post" id="addItemFromLocation" action="<@ofbizUrl>addLocationItemsToCycleCount</@ofbizUrl>" name="addItemFromLocation" data-barcode-form="addItemFromLocation">
                        <input type="hidden" name="inventoryCountId" value="${inventoryCountId!}"/>
                        <input type="hidden" name="facilityId" value="${facilityId!}"/>
                        <@htmlTemplate.lookupField value='${parameters.locationSeqId!}' formName="addItemFromLocation" name="locationSeqId" id="locationSeqId" fieldFormName="LookupFacilityLocation?facilityId=${facilityId!}" showDescription="N"/>
                        <input type="submit" class="smallSubmit" name="Add" value="Add"/>
                      </form>
                    </td>
                  </tr>
                </#if>
                <#if "INV_COUNT_CREATED" == countSessionValue.statusId>
                  <tr>
                    <td align="right" class="label">${uiLabelMap.ProductInventoryItem}:</td>
                    <td>
                      <form method="post" id="addItemFromInventoryItem" action="<@ofbizUrl>addInventoryItemToCycleCount</@ofbizUrl>" name="addItemFromLocation" data-barcode-form="addItemFromLocation">
                        <input type="hidden" name="inventoryCountId" value="${inventoryCountId!}"/>
                        <@htmlTemplate.lookupField value='${parameters.inventoryItemId!}' formName="addItemFromInventoryItem" name="inventoryItemId" id="inventoryItemId" fieldFormName="LookupInventoryItem?facilityId=${facilityId!}" showDescription="N"/>
                        <input type="submit" class="smallSubmit" name="Add" value="Add"/>
                      </form>
                    </td>
                  </tr>
                </#if>
              </table>
            </div>
          </td>
        </tr>
      </table>
      <div class="screenlet-title-bar">
        <ul>
          <#if "INV_COUNT_CREATED" == countSessionValue.statusId><li style="margin-top: 3px;"><input type="submit" form="selectAllForm" class="link" name="Save" value="Save" onclick="javascript: return getValue();"/></li></#if>
        <li class="h3">Item Details</li>
        </ul>
      </div>
      <div class="screenlet-body" id="updateCountItem">
        <#assign rowCount = 0/>
        <form method="post" id="selectAllForm" action="<@ofbizUrl>updateCountItemAndCreateCountVariance</@ofbizUrl>" name="selectAllForm">
          <input type="hidden" name="inventoryCountId" value="${inventoryCountId!}"/>
          <input type="hidden" name="_useRowSubmit" value="Y"/>
          <table class="basic-table hover-bar" cellspacing="1">
            <thead>
              <tr class="header-row">
                <th>${uiLabelMap.FormFieldTitle_locationSeqId}</th>
                <th>${uiLabelMap.ProductInventoryItem}</th>
                <th>${uiLabelMap.ProductProductName}</th>
                <th>${uiLabelMap.ProductProductDescription}</th>
                <th>${uiLabelMap.ProductQtyCounted}</th>
                <#if "INV_COUNT_CREATED" == countSessionValue.statusId>
                  <th style="text-align:center;">
                    <input type="checkbox" title="${uiLabelMap.CommonSelectAll}" name="selectAll" id="selectAll" value="Y" onclick="javascript:toggleAll(this, 'selectAllForm');"/>
                  </th>
                </#if>
              </tr>
            </thead>
            <tbody>
              <#list inventoryCountItemAndVariances as inventoryCountItemAndVariance>
                <tr>
                  <td>
                    <a target="_blank" href="<@ofbizUrl>EditFacilityLocation?facilityId=${inventoryCountItemAndVariance.facilityId!}&amp;locationSeqId=${inventoryCountItemAndVariance.locationSeqId!}</@ofbizUrl>">${inventoryCountItemAndVariance.areaId!} <#if inventoryCountItemAndVariance.aisleId?has_content>:${inventoryCountItemAndVariance.aisleId!}</#if><#if inventoryCountItemAndVariance.sectionId?has_content>:${inventoryCountItemAndVariance.sectionId!}</#if><#if inventoryCountItemAndVariance.levelId?has_content>:${inventoryCountItemAndVariance.levelId!}</#if>:${inventoryCountItemAndVariance.positionId!} [${inventoryCountItemAndVariance.locationSeqId!}]</a>
                  </td>
                  <td>
                    <#if inventoryCountItemAndVariance.inventoryItemId?has_content>
                      <a class="smallSubmit" target="_blank" href="/facility/control/EditInventoryItem?inventoryItemId=${inventoryCountItemAndVariance.inventoryItemId!}&amp;facilityId=${inventoryCountItemAndVariance.facilityId!}${StringUtil.wrapString(externalKeyParam!)}">${inventoryCountItemAndVariance.inventoryItemId!}</a>
                    </#if>
                  </td>
                  <td>${inventoryCountItemAndVariance.internalName!}</td>
                  <td>${inventoryCountItemAndVariance.partDescription!}</td>
                  <td>
                    <#if "INV_COUNT_CREATED" == countSessionValue.statusId>
                      <input type="text" id="${inventoryCountItemAndVariance.inventoryItemId!}" class="quantity focusBackToInput" data-focus-back-id="#searchAndFocusInput" name="quantity_o_${rowCount}" data-check-box="#checkBoxRowSubmit_${inventoryCountItemAndVariance_index}" value="${inventoryCountItemAndVariance.quantity!}"/>
                    <#else>
                      ${inventoryCountItemAndVariance.quantity!}
                    </#if>
                  </td>
                  <#if "INV_COUNT_CREATED" == countSessionValue.statusId>
                    <td style="text-align: center;">
                      <input type="hidden" name="inventoryCountItemSeqId_o_${rowCount}" value="${inventoryCountItemAndVariance.inventoryCountItemSeqId!}">
                      <input type="checkbox" id="checkBoxRowSubmit_${inventoryCountItemAndVariance_index}" class="rowSubmit" name="_rowSubmit_o_${rowCount}" value="Y"/>
                    </td>
                  </#if>
                </tr>
                <#assign rowCount = rowCount + 1>
              </#list>
            </tbody>
          </table>
        </form>
      </div>
    </div>
  </div>
<#else>
  ${screens.render("component://product/widget/facility/CycleCountScreens.xml#PendingLocations")}
</#if>