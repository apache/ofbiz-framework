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
      <li class="h3">${uiLabelMap.PendingLocations}</li>
      <li style="margin-top: 3px;"><input type="submit" form="selectAllForm" class="link" name="Create Session" value="Create Session"/></li>
    </ul>
  </div>
  <div class="screenlet-body">
    <#if pendingLocations?has_content>
      <#assign commonUrl>
        <@ofbizUrl>RecordCount?</@ofbizUrl>
      </#assign>
      <#if parameters.facilityId?has_content>
        <#assign commonUrl=commonUrl?trim+'facilityId='+parameters.facilityId?default("")+'&amp;'/>
      </#if>
      <#if parameters.locationSeqId?has_content>
        <#assign commonUrl=commonUrl?trim+'locationSeqId='+parameters.locationSeqId?default("")+'&amp;'/>
      </#if>
      <#if parameters.areaId?has_content>
        <#assign commonUrl=commonUrl?trim+'areaId='+parameters.areaId?default("")+'&amp;'/>
      </#if>
      <#if parameters.aisleId?has_content>
        <#assign commonUrl=commonUrl?trim+'aisleId='+parameters.aisleId?default("")+'&amp;'/>
      </#if>
      <#if parameters.sectionId?has_content>
        <#assign commonUrl=commonUrl?trim+'sectionId='+parameters.sectionId?default("")+'&amp;'/>
      </#if>
      <#if parameters.levelId?has_content>
        <#assign commonUrl=commonUrl?trim+'levelId='+parameters.levelId?default("")+'&amp;'/>
      </#if>
      <#if parameters.positionId?has_content>
        <#assign commonUrl=commonUrl?trim+'positionId='+parameters.positionId?default("")+'&amp;'/>
      </#if>
      <#if parameters.countDays?has_content>
        <#assign commonUrl=commonUrl?trim+'countDays='+parameters.countDays?default("")+'&amp;'/>
      </#if>
      <#if parameters.nextCountDays?has_content>
        <#assign commonUrl=commonUrl?trim+'nextCountDays='+parameters.nextCountDays?default("")+'&amp;'/>
      </#if>
      <@htmlTemplate.nextPrev commonUrl=commonUrl ajaxEnabled=false javaScriptEnabled=false paginateStyle="nav-pager" paginateFirstStyle="nav-first" viewIndex=viewIndex highIndex=highIndex listSize=listSize viewSize=viewSize ajaxFirstUrl="" firstUrl="" paginateFirstLabel="" paginatePreviousStyle="nav-previous" ajaxPreviousUrl="" previousUrl="" paginatePreviousLabel="" pageLabel="" ajaxSelectUrl="" selectUrl="" ajaxSelectSizeUrl="" selectSizeUrl="" commonDisplaying=commonDisplaying paginateNextStyle="nav-next" ajaxNextUrl="" nextUrl="" paginateNextLabel="" paginateLastStyle="nav-last" ajaxLastUrl="" lastUrl="" paginateLastLabel="" paginateViewSizeLabel="" />
      <#assign rowCount = 0/>
      <form method="post" id="selectAllForm" action="<@ofbizUrl>createInventoryCountAndAddBulkLocations</@ofbizUrl>" name="selectAllForm">
        <input type="hidden" name="facilityId" value="${facilityId!}"/>
        <table class="basic-table hover-bar" cellspacing="1">
          <thead>
            <tr class="header-row">
              <th>${uiLabelMap.Facility}</th>
              <th>${uiLabelMap.ProductLocation}</th>
              <th>${uiLabelMap.ProductArea}</th>
              <th>${uiLabelMap.ProductAisle}</th>
              <th>${uiLabelMap.ProductSection}</th>
              <th>${uiLabelMap.ProductLevel}</th>
              <th>${uiLabelMap.ProductPosition}</th>
              <th>Last counted on</th>
              <th>Days since last count</th>
              <th>Next count date</th>
              <th>Days to next count</th>
              <th>${uiLabelMap.CommonTotal} ${uiLabelMap.ProductInventoryItems}</th>
              <th><input type="checkbox" title="${uiLabelMap.CommonSelectAll}" name="selectAll" id="selectAll"
                         value="Y" onclick="javascript:toggleAll(this, 'selectAllForm');"/></th>
            </tr>
          </thead>
          <tbody>
            <#assign rowCount = 0/>
            <#list pendingLocations as pendingLocation>
              <tr>
                <td>${pendingLocation.facilityName!}</td>
                <td>
                    <a href="<@ofbizUrl>EditFacilityLocation?facilityId=${pendingLocation.facilityId!}&amp;locationSeqId=${pendingLocation.locationSeqId!}</@ofbizUrl>">${pendingLocation.areaId!} <#if pendingLocation.aisleId?has_content>:${pendingLocation.aisleId!}</#if><#if pendingLocation.sectionId?has_content>:${pendingLocation.sectionId!}</#if><#if pendingLocation.levelId?has_content>:${pendingLocation.levelId!}</#if>:${pendingLocation.positionId!} [${pendingLocation.locationSeqId!}]</a>
                </td>
                <td>${pendingLocation.areaId!}</td>
                <td>${pendingLocation.aisleId!}</td>
                <td>${pendingLocation.sectionId!}</td>
                <td>${pendingLocation.levelId!}</td>
                <td>${pendingLocation.positionId!}</td>
                <td><#if pendingLocation.lastCountDate?has_content>${pendingLocation.lastCountDate?string('MM/dd/yyyy')}</#if></td>
                <td>${pendingLocation.lastCountDay!}</td>
                <td><#if pendingLocation.nextCountDate?has_content>${pendingLocation.nextCountDate?string('MM/dd/yyyy')}</#if></td>
                <td>${pendingLocation.nextCountDay!}</td>
                <td>${pendingLocation.totalInventoryItems!}</td>
                <td>
                  <input type="hidden" name="locationSeqId_o_${rowCount}" value="${pendingLocation.locationSeqId!}"/>
                  <input type="checkbox" class="rowSubmit" name="_rowSubmit_o_${rowCount}" value="Y"
                      onclick="javascript:checkToggle(this, 'selectAllForm');"/>
                </td>
              </tr>
              <#assign rowCount = rowCount + 1>
            </#list>
          </tbody>
        </table>
      </form>
      <@htmlTemplate.nextPrev commonUrl=commonUrl ajaxEnabled=false javaScriptEnabled=false paginateStyle="nav-pager" paginateFirstStyle="nav-first" viewIndex=viewIndex highIndex=highIndex listSize=listSize viewSize=viewSize ajaxFirstUrl="" firstUrl="" paginateFirstLabel="" paginatePreviousStyle="nav-previous" ajaxPreviousUrl="" previousUrl="" paginatePreviousLabel="" pageLabel="" ajaxSelectUrl="" selectUrl="" ajaxSelectSizeUrl="" selectSizeUrl="" commonDisplaying=commonDisplaying paginateNextStyle="nav-next" ajaxNextUrl="" nextUrl="" paginateNextLabel="" paginateLastStyle="nav-last" ajaxLastUrl="" lastUrl="" paginateLastLabel="" paginateViewSizeLabel="" />
    <#else>
      <label>No record found.</label>
    </#if>
  </div>
</div>