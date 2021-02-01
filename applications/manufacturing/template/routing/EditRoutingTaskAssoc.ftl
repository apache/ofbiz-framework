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
function copyAndAddRoutingTask() {
    document.addtaskassocform.copyTask.value = "Y";
    document.addtaskassocform.submit();
}
function addRoutingTask() {
    document.addtaskassocform.copyTask.value = "N";
    document.addtaskassocform.submit();
}
</script>

<#if security.hasEntityPermission("MANUFACTURING", "_CREATE", session)>
<form method="post" action="<@ofbizUrl>AddRoutingTaskAssoc</@ofbizUrl>" name="addtaskassocform">
    <input type="hidden" name="workEffortId" value="${workEffortId}"/>
    <input type="hidden" name="workEffortIdFrom" value="${workEffortId}"/>
    <input type="hidden" name="workEffortAssocTypeId" value="ROUTING_COMPONENT"/>
    <input type="hidden" name="copyTask" value="N"/>
    <table class="basic-table" cellspacing="0">
        <tr>
            <th align="right">
                ${uiLabelMap.ManufacturingRoutingTaskId}
            </th>
            <td>
                <@htmlTemplate.lookupField formName="addtaskassocform" name="workEffortIdTo" id="workEffortIdTo" fieldFormName="LookupRoutingTask"/>
            </td>
            <th align="right">
                ${uiLabelMap.CommonFromDate}
            </th>
            <td>
                <@htmlTemplate.renderDateTimeField name="fromDate" event="" action="" className="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" value="${nowTimestamp}" size="25" maxlength="30" id="fromDate_1" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
            </td>
            <td align="center" width="40%">&nbsp;</td>
        </tr>
        <tr>
            <th align="right">
                ${uiLabelMap.CommonSequenceNum}
            </th>
            <td>
                <input type="text" name="sequenceNum" size="10"/>
            </td>
            <th align="right">
                ${uiLabelMap.CommonThruDate}
            </th>
            <td>
                <@htmlTemplate.renderDateTimeField name="thruDate" event="" action="" className="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" value="" size="25" maxlength="30" id="thruDate_1" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
            </td>
            <td>&nbsp;</td>
        </tr>

        <tr>
            <td >&nbsp;</td>
            <td colspan="3">
                <a href="javascript:addRoutingTask();" class="buttontext">${uiLabelMap.ManufacturingAddExistingRoutingTask}</a>
                &nbsp;-&nbsp;
                <a href="javascript:copyAndAddRoutingTask();" class="buttontext">${uiLabelMap.ManufacturingCopyAndAddRoutingTask}</a>
            </td>
            <td>&nbsp;</td>
        </tr>
    </table>
</form>
</#if>