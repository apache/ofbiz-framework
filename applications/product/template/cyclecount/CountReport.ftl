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
    <ul>
      <li class="h3">${uiLabelMap.ProductCountReport}</li>
    </ul>
  </div>
  <#setting number_format=",##0.00;(,##0.00)">
  <div class="screenlet-body">
    <table width="100%">
      <tbody>
        <tr>
          <td width="39%">
            <div class="screenlet">
              <div class="screenlet-title-bar">
                <ul>
                  <li class="h3">${uiLabelMap.CommonSearchOptions}</li>
                </ul>
              </div>
              <div class="screenlet-body">
                <form name="FindCycleCount" action="<@ofbizUrl>CountReport</@ofbizUrl>" method="post" data-validate-form="true">
                  <fieldset id="findPendingLocation">
                    <input type="hidden" name="searchResult" value="Y">
                    <table class="basic-table">
                      <tbody>
                      <tr>
                        <td style="width: 10%;" align="right">
                          <label class="label">${uiLabelMap.FacilityFacility}</label>
                        </td>
                        <td>
                          <select name="facilityIds" class="ajaxUpdateArea" data-params="#findPendingLocation" data-param-url="AjaxFacilityLocation" updated-area-id="updateFacilityLocation">
                            <option>All</option>
                            <#list facilities as facility>
                              <option value="${facility.facilityId!}" <#if facilityIds?has_content && facilityIds.contains(facility.facilityId)>selected="selected"</#if>>${facility.facilityName!}</option>
                            </#list>
                          </select>
                        </td>
                      </tr>
                      <tr>
                        <td style="width: 10%;" align="right">
                          <label class="label" for="locationSeqId">${uiLabelMap.ProductLocation}</label>
                        </td>
                        <td>
                          <div id="updateFacilityLocation">
                          <#if facilityIds?has_content && "All" != parameters.facilityIds>
                            <@htmlTemplate.lookupField value='${parameters.locationSeqId!}' formName="FindCycleCount" name="locationSeqId" id="locationSeqId" fieldFormName="LookupFacilityLocation?facilityId=${parameters.facilityIds!}"/>
                          <#else>
                            <@htmlTemplate.lookupField value='${parameters.locationSeqId!}' formName="FindCycleCount" name="locationSeqId" id="locationSeqId" fieldFormName="LookupFacilityLocation"/>
                          </#if>
                          </div>
                        </td>
                      </tr>
                      <tr>
                        <td class="label">${uiLabelMap.CommonFromDate}</td>
                        <td>
                          <@htmlTemplate.renderDateTimeField name="fromDate" event="" action="" className="${class!}" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" value="${(parameters.fromDate)!}" size="25" maxlength="30" id="fromDate" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
                        </td>
                      </tr>
                      <tr>
                        <td class="label">${uiLabelMap.ToDate}</td>
                        <td>
                          <@htmlTemplate.renderDateTimeField name="toDate" event="" action="" className="${class!}" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" value="${(parameters.toDate)!}" size="25" maxlength="30" id="toDate" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
                        </td>
                      </tr>
                      <tr>
                        <td></td>
                        <td>
                          <input type="submit" name="search" value="${uiLabelMap.CommonSearch}"/>
                        </td>
                      </tr>
                      </tbody>
                    </table>
                  </fieldset>
                </form>
              </div>
            </div>
          </td>
          <td width="1%"></td>
          <td width="60%" style="vertical-align: top;">
            <div class="screenlet">
              <div class="screenlet-title-bar">
                <ul>
                  <li class="h3">${uiLabelMap.CountProgress}</li>
                </ul>
              </div>
              <div class="screenlet-body">
                <#if inventoryCountItemsAndVariances?has_content>
                  <table class="basic-table hover-bar" cellspacing="1">
                    <thead>
                      <tr class="header-row">
                        <th colspan="3" style="text-align: center;">${uiLabelMap.CountProgress}</th>
                        <th colspan="2" style="text-align: center;">${uiLabelMap.TotalProjectedCounts}</th>
                        <th colspan="2" style="text-align: center;">${uiLabelMap.WorkEffortPercentComplete}</th>
                      </tr>
                      <tr class="header-row">
                        <th width="20%">${uiLabelMap.Facility}</th>
                        <th width="15%" class="data-table-right">${uiLabelMap.LocationCounted}</th>
                        <th width="15%" class="data-table-right">${uiLabelMap.InventoryItemsCounted}</th>
                        <th width="12%" class="data-table-right">${uiLabelMap.ProductLocations}</th>
                        <th width="13%" class="data-table-right">${uiLabelMap.ProductInventoryItems}</th>
                        <th width="12%" class="data-table-right">${uiLabelMap.ProductLocations}</th>
                        <th width="13%" class="data-table-right">${uiLabelMap.ProductInventoryItems}</th>
                      </tr>
                    </thead>
                    <tbody>
                      <#list progressList as progress>
                        <tr>
                          <td width="20%">${progress.facilityName!}</td>
                          <td width="15%" class="data-table-right">${progress.countedLocations!}</td>
                          <td width="15%" class="data-table-right">${progress.totalCountedInventoryItems!}</td>
                          <td width="12%" class="data-table-right">${progress.totalLocations!}</td>
                          <td width="13%" class="data-table-right">${progress.totalInventoryItems!}</td>
                          <td width="12%" class="data-table-right">${progress.percentLocationCompleted!}%</td>
                          <td width="13%" class="data-table-right">${progress.percentInventoryItemCompleted!}%</td>
                        </tr>
                      </#list>
                    </tbody>
                    <tfoot>
                      <tr id="sumHeader">
                        <td width="20%">Grand Total</td>
                        <td width="15%" class="data-table-right">${grandTotalLocationCounted!}</td>
                        <td width="15%" class="data-table-right">${grandTotalInventoryItemCounted!}</td>
                        <td width="12%" class="data-table-right">${grandTotalLocationCompleted!}</td>
                        <td width="13%" class="data-table-right">${grandTotalInventoryItemCompleted!}</td>
                        <td width="12%" class="data-table-right">${grandTotalPerLocation!}%</td>
                        <td width="13%" class="data-table-right">${grandTotalPerInventoryItem!}%</td>
                      </tr>
                    </tfoot>
                  </table>
                <#else>
                  <label>No record found.</label>
                </#if>
              </div>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
    <div class="screenlet">
      <div class="screenlet-title-bar">
        <ul>
          <li class="h3">${uiLabelMap.ItemVariance}</li>
        </ul>
      </div>
      <div class="screenlet-body">
        <#if inventoryCountItemsAndVariances?has_content>
          <table class="basic-table hover-bar data-table exportTableToCSV" data-height="450" data-custom-footer-call-back="CycleCount" cellspacing="1">
            <thead>
              <tr class="header-row">
                <th width="10%">${uiLabelMap.Facility}</th>
                <th width="12%">${uiLabelMap.ProductLocation}</th>
                <th width="7%">Counted On</th>
                <th width="7%">Variance Created On</th>
                <th width="7%">${uiLabelMap.ProductInventoryItem}</th>
                <th width="7%">${uiLabelMap.ProductProductName}</th>
                <th width="7%">${uiLabelMap.ProductProductDescription}</th>
                <th width="6%">Qty Counted</th>
                <th width="6%">System QOH</th>
                <th width="6%">${uiLabelMap.FormFieldTitle_unitCost}</th>
                <th width="6%">${uiLabelMap.ProductQtyVariance}</th>
                <th width="6%">Variance Value</th>
                <th width="6%">Inventory Value</th>
                <th with="6%">${uiLabelMap.WebtoolsErrorLogLevel}</th>
              </tr>
            </thead>
            <tbody>
              <#list inventoryCountItemsAndVariances as inventoryCountItemsAndVariance>
                <tr>
                  <td width="10%">${inventoryCountItemsAndVariance.facilityName!}</td>
                  <td width="12%" class="data-table-word-break">
                    <a target="_blank" href="<@ofbizUrl>EditFacilityLocation?facilityId=${inventoryCountItemsAndVariance.facilityId!}&amp;locationSeqId=${inventoryCountItemsAndVariance.locationSeqId!}</@ofbizUrl>"><#if inventoryCountItemsAndVariance.areaId?has_content>${inventoryCountItemsAndVariance.areaId!}:</#if> <#if inventoryCountItemsAndVariance.aisleId?has_content>${inventoryCountItemsAndVariance.aisleId!}:</#if><#if inventoryCountItemsAndVariance.sectionId?has_content>${inventoryCountItemsAndVariance.sectionId!}:</#if><#if inventoryCountItemsAndVariance.levelId?has_content>${inventoryCountItemsAndVariance.levelId!}:</#if><#if inventoryCountItemsAndVariance.positionId?has_content>${inventoryCountItemsAndVariance.positionId!}</#if><br/> [${inventoryCountItemsAndVariance.locationSeqId!}]</a>
                  </td>
                  <td width="7%"><#if inventoryCountItemsAndVariance.createdDate?has_content>${inventoryCountItemsAndVariance.createdDate?string("MM/dd/yyyy")}</#if></td>
                  <td width="7%"><#if inventoryCountItemsAndVariance.varianceCreatedOn?has_content>${inventoryCountItemsAndVariance.varianceCreatedOn?string("MM/dd/yyyy")}</#if></td>
                  <td width="7%">
                    <#if inventoryCountItemsAndVariance.inventoryItemId?has_content>
                      <a class="smallSubmit" target="_blank" href="/facility/control/EditInventoryItem?inventoryItemId=${inventoryCountItemsAndVariance.inventoryItemId!}&amp;facilityId=${inventoryCountItemsAndVariance.facilityId!}${StringUtil.wrapString(externalKeyParam!)}">${inventoryCountItemsAndVariance.inventoryItemId!}</a>
                    </#if>
                  </td>
                  <td width="7%" class="data-table-word-break" title="${inventoryCountItemsAndVariance.internalName!}">
                    <#if inventoryCountItemsAndVariance.internalName?has_content && inventoryCountItemsAndVariance.internalName?length &gt; 20>
                      ${inventoryCountItemsAndVariance.internalName?substring(0,20)}
                    <#else>
                      ${inventoryCountItemsAndVariance.internalName!}
                    </#if>
                  </td>
                  <td width="7%" class="data-table-word-break" title="${inventoryCountItemsAndVariance.partDescription!}">
                    <#if inventoryCountItemsAndVariance.partDescription?has_content && inventoryCountItemsAndVariance.partDescription?length &gt; 20>
                      ${inventoryCountItemsAndVariance.partDescription?substring(0,20)}
                    <#else>
                    ${inventoryCountItemsAndVariance.partDescription!}
                    </#if>
                  </td>
                  <td width="6%" class="data-table-right" <#if inventoryCountItemsAndVariance.quantity?has_content && inventoryCountItemsAndVariance.quantity != 0>title="${inventoryCountItemsAndVariance.quantity!}"</#if>>
                    <div><#if inventoryCountItemsAndVariance.quantity?has_content && inventoryCountItemsAndVariance.quantity &lt; 0> <span class="calculateSum">${inventoryCountItemsAndVariance.quantity!}</span><#else><span class="calculateSum">${inventoryCountItemsAndVariance.quantity!}</span></#if></div>
                  </td>
                  <td width="6%" class="data-table-right" <#if inventoryCountItemsAndVariance.systemQuantityOnHand?has_content && inventoryCountItemsAndVariance.systemQuantityOnHand != 0>title="${inventoryCountItemsAndVariance.systemQuantityOnHand!}"</#if>>
                    <div><#if inventoryCountItemsAndVariance.systemQuantityOnHand?has_content && inventoryCountItemsAndVariance.systemQuantityOnHand &lt; 0> <span class="calculateSum">${inventoryCountItemsAndVariance.systemQuantityOnHand!}</span><#else><span class="calculateSum">${inventoryCountItemsAndVariance.systemQuantityOnHand!}</span></#if></div>
                  </td>
                  <td width="6%" class="data-table-right" <#if inventoryCountItemsAndVariance.unitCost?has_content && inventoryCountItemsAndVariance.unitCost != 0>title="${inventoryCountItemsAndVariance.unitCost!}"</#if>>
                    <div><#if inventoryCountItemsAndVariance.unitCost?has_content && inventoryCountItemsAndVariance.unitCost &lt; 0> <span class="calculateSum"><@ofbizCurrency amount=inventoryCountItemsAndVariance.unitCost isoCode=currencyUomId/></span><#else><span class="calculateSum"><@ofbizCurrency amount=inventoryCountItemsAndVariance.unitCost isoCode=currencyUomId/></span></#if></div>
                  </td>
                  <td width="6%" class="data-table-right" <#if inventoryCountItemsAndVariance.varianceQuantityOnHand?has_content && inventoryCountItemsAndVariance.varianceQuantityOnHand != 0>title="${inventoryCountItemsAndVariance.varianceQuantityOnHand!}"</#if>>
                    <div><#if inventoryCountItemsAndVariance.varianceQuantityOnHand?has_content && inventoryCountItemsAndVariance.varianceQuantityOnHand &lt; 0> <span class="colorVs calculateSum">${inventoryCountItemsAndVariance.varianceQuantityOnHand!}</span><#else><span class="colorMain calculateSum">${inventoryCountItemsAndVariance.varianceQuantityOnHand!}</span></#if></div>
                  </td>
                  <td width="6%" class="data-table-right" <#if inventoryCountItemsAndVariance.costVariance?has_content && inventoryCountItemsAndVariance.costVariance != 0>title="${inventoryCountItemsAndVariance.costVariance!}"</#if>>
                    <div><#if inventoryCountItemsAndVariance.costVariance?has_content && inventoryCountItemsAndVariance.costVariance &lt; 0> <span class="colorVs calculateSum"><@ofbizCurrency amount=inventoryCountItemsAndVariance.costVariance isoCode=currencyUomId/></span><#else><span class="colorMain calculateSum"><@ofbizCurrency amount=inventoryCountItemsAndVariance.costVariance isoCode=currencyUomId/></span></#if></div>
                  </td>
                  <td width="6%" class="data-table-right" <#if inventoryCountItemsAndVariance.totalCost?has_content && inventoryCountItemsAndVariance.totalCost != 0>title="${inventoryCountItemsAndVariance.totalCost!}"</#if>>
                    <div><#if inventoryCountItemsAndVariance.totalCost?has_content && inventoryCountItemsAndVariance.totalCost &lt; 0> <span class="colorVs calculateSum"><@ofbizCurrency amount=inventoryCountItemsAndVariance.totalCost isoCode=currencyUomId/></span><#else><span class="colorMain calculateSum"><@ofbizCurrency amount=inventoryCountItemsAndVariance.totalCost isoCode=currencyUomId/></span></#if></div>
                  </td>
                  <td width="6%" class="data-table-right" <#if inventoryCountItemsAndVariance.error?has_content && inventoryCountItemsAndVariance.error != 0>title="${inventoryCountItemsAndVariance.error!}"</#if>>
                    <div><#if inventoryCountItemsAndVariance.error?has_content && inventoryCountItemsAndVariance.error &lt; 0> <span class="colorVs calculateSum">${inventoryCountItemsAndVariance.error!}%</span><#else><span class="colorMain calculateSum">${inventoryCountItemsAndVariance.error!}%</span></#if></div>
                  </td>
                </tr>
              </#list>
            </tbody>
          </table>
        <#else>
          <label>No record found. Please search result.</label>
        </#if>
      </div>
    </div>
  </div>
</div>
