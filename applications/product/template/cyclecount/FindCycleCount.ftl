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
      <li class="h3">${uiLabelMap.FindSession}</li>
    </ul>
  </div>
  <div class="screenlet-body">
    <form name="FindCycleCount" action="<@ofbizUrl>FindCycleCount</@ofbizUrl>" method="post" data-validate-form="true">
      <fieldset id="findCycleCount">
        <input type="hidden" name="searchResult" value="Y">
        <div class="fieldgroup">
          <div class="fieldgroup-body">
            <table cellspacing="0" class="basic-table">
              <tbody>
                <tr>
                  <td style="width: 10%;" align="right">
                    <label class="label">Session ID</label>
                  </td>
                  <td>
                    <input type="text" name="inventoryCountId" value="${parameters.inventoryCountId!}">
                  </td>
                </tr>
                  <tr>
                    <td style="width: 10%;" align="right">
                        <label class="label">${uiLabelMap.FacilityFacility}</label>
                    </td>
                    <td>
                      <select name="facilityId" class="ajaxUpdateArea" data-params="#findCycleCount" data-param-url="AjaxFacilityLocation" updated-area-id="updateFacilityLocation">
                        <#list facilities as facility>
                          <option value="${facility.facilityId!}" <#if facilityId?has_content && facilityId.contains(facility.facilityId)>selected="selected"</#if>>${facility.facilityName!}</option>
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
                        <@htmlTemplate.lookupField value='${parameters.locationSeqId!}' formName="FindCycleCount" name="locationSeqId" id="locationSeqId" fieldFormName="LookupFacilityLocation?facilityId=${facilityId!}"/>
                      </div>
                    </td>
                  </tr>
                  <tr>
                    <td style="width: 10%;" align="right">
                      <label class="label">${uiLabelMap.CommonStatus}</label>
                    </td>
                    <td>
                      <#list statusItems as statusItem>
                        <input type="checkbox" name="statusIds" id="statusId_${statusItem_index}" value="${statusItem.statusId!}" <#if statusIds?has_content && statusIds.contains(statusItem.statusId)>checked="checked"<#elseif parameters.statusIds?has_content && statusItem.statusId == parameters.statusIds>checked="checked"<#elseif !parameters.statusIds?has_content && statusItem.statusId == "INV_COUNT_APPROVED">checked="checked"</#if>/>
                        <label for="statusId_${statusItem_index}">${statusItem.description!}</label>
                      </#list>
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
          </div>
        </div>
      </fieldset>
    </form>
  </div>
</div>