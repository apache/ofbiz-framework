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
      <li class="h3">${uiLabelMap.FindPendingLocations}</li>
    </ul>
  </div>
  <div class="screenlet-body">
    <form name="FindCycleCount" action="<@ofbizUrl>RecordCount</@ofbizUrl>" method="post">
      <fieldset id="findPendingLocation">
        <input type="hidden" name="searchResult" value="Y">
        <table cellspacing="0" class="basic-table">
          <tbody>
            <tr>
              <td style="width: 10%;" align="right">
                <label class="label">${uiLabelMap.FacilityFacility}</label>
              </td>
              <td>
                <select name="facilityId">
                  <#list facilities as facility>
                    <option value="${facility.facilityId!}" <#if parameters.facilityId?has_content && parameters.facilityId == facility.facilityId>selected="selected"<#elseif facilityId?has_content && facilityId == facility.facilityId>selected="selected"</#if>>${facility.facilityName!}</option>
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
                <input type="text" name="areaId" value="${parameters.areaId!}" placeholder="Area Id"/>
                <input type="text" name="aisleId" value="${parameters.aisleId!}" placeholder="Aisle Id"/>
                <input type="text" name="sectionId" value="${parameters.sectionId!}" placeholder="Section Id"/>
                <input type="text" name="levelId" value="${parameters.levelId!}" placeholder="Level Id"/>
                <input type="text" name="positionId" value="${parameters.positionId!}" placeholder="Position Id"/>
              </td>
            </tr>
            <tr>
              <td class="label">${uiLabelMap.LocationNotScanned}</td>
              <td>
                <input type="text" size="4" name="countDays" value="${parameters.countDays!}"/> ${uiLabelMap.CommonDays}
              </td>
            </tr>
            <tr>
              <td class="label">${uiLabelMap.LocationScheduledForScanning}</td>
              <td>
                <input type="text" size="4" name="nextCountDays" value="${parameters.nextCountDays!}"/> ${uiLabelMap.CommonDays}
              </td>
            </tr>
            <tr>
              <td></td>
              <td>
                <input type="submit" name="search" value="${uiLabelMap.CommonFind}"/>
              </td>
            </tr>
          </tbody>
        </table>
      </fieldset>
    </form>
  </div>
</div>