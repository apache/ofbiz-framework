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
<table width="100%" border="0" cellpadding="0" cellspacing="0">
    <tr>
        <td width="34%" align="center">
            <div class="tabletext">
                <form action="<@ofbizUrl>${parameters.targetRequestUri}</@ofbizUrl>" name="partyform" method="post">
                    <input type="hidden" name="start" value="${start.time?string("#")}"/>
                    ${uiLabelMap.WorkEffortByPartyId}: 
                    <input type="text" name="partyId" value="${requestParameters.partyId?if_exists}" class="inputBox"/>
                    <a href="javascript:call_fieldlookup2(document.partyform.partyId,'<@ofbizUrl>LookupPartyName</@ofbizUrl>');"><img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt='Click here For Field Lookup'></a>
                    <input type="submit" value="${uiLabelMap.CommonView}" class="smallSubmit"/>
                </form>
            </div>
        </td>
        <td width="32%" align="center">
            <div class="tabletext">
                <form action="<@ofbizUrl>${parameters.targetRequestUri}</@ofbizUrl>" method="post">
                    <input type="hidden" name="start" value="${start.time?string("#")}"/>
                    ${uiLabelMap.WorkEffortByFacility}: 
                    <select name="facilityId" class="selectBox">
                        <option value=""></option>
                        <#list allFacilities as facility>
                            <option value="${facility.facilityId}"<#if requestParameters.facilityId?has_content && requestParameters.facilityId == facility.facilityId>${uiLabelMap.WorkEffortSelected}</#if>>${facility.facilityName}</option>
                        </#list>
                    </select>
                    <input type="submit" value="${uiLabelMap.CommonView}" class="smallSubmit"/>
                </form>
            </div>
        </td>
        <td width="32%" align="center">
            <div class="tabletext">
                <form action="<@ofbizUrl>${parameters.targetRequestUri}</@ofbizUrl>" method="post">
                    <input type="hidden" name="start" value="${start.time?string("#")}"/>
                    ${uiLabelMap.WorkEffortByFixedAsset}: 
                    <select name="fixedAssetId" class="selectBox">
                        <option value=""></option>
                        <#list allFixedAssets as fixedAsset>
                            <option value="${fixedAsset.fixedAssetId}"<#if requestParameters.fixedAssetId?has_content && requestParameters.fixedAssetId == fixedAsset.fixedAssetId>${uiLabelMap.WorkEffortSelected}</#if>>${fixedAsset.fixedAssetId}</option>
                        </#list>
                    </select>
                    <input type="submit" value="${uiLabelMap.CommonView}" class="smallSubmit"/>
                </form>
            </div>
        </td>
    </tr>
</table>
<div class="tabletext">&nbsp;</div>
