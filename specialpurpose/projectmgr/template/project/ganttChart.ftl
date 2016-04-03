<#-- <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"> -->
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

<div style="position:relative" class="gantt" id="GanttChartDIV"></div>
<script type="text/javascript" language="javascript">
var g = new JSGantt.GanttChart('g',document.getElementById('GanttChartDIV'), 'day');

g.setShowRes(1); // Show/Hide Responsible (0/1)
g.setShowDur(1); // Show/Hide Duration (0/1)
g.setShowComp(1); // Show/Hide % Complete(0/1)

// Parameters             (pID, pName,                  pStart,      pEnd,        pColor,   pLink,          pMile, pRes,  pComp, pGroup, pParent, pOpen)

<#list phaseTaskList as t>
    <#if t.workEffortTypeId == "PHASE">
        g.AddTaskItem(new JSGantt.TaskItem(${t.phaseNr}, "${t.phaseSeqNum!}. ${t.phaseName}", "", "", "00ff00", "", 0, "", 0, 1, 0, 1));
    </#if>
    <#if t.workEffortTypeId == "TASK">
        g.AddTaskItem(new JSGantt.TaskItem(${t.taskNr},"${t.taskSeqNum!}. ${t.taskName}","${StringUtil.wrapString(t.estimatedStartDate)}", "${StringUtil.wrapString(t.estimatedCompletionDate)}","009900", "${t.url}", 0 , "${t.resource!}", ${t.completion!} , 0, ${t.phaseNr}, 1<#if t.preDecessor??>, "${t.preDecessor}"</#if>));
    </#if>
    <#if t.workEffortTypeId == "MILESTONE">
        g.AddTaskItem(new JSGantt.TaskItem(${t.taskNr},"${t.taskName}","${StringUtil.wrapString(t.estimatedStartDate)}", "${StringUtil.wrapString(t.estimatedCompletionDate)}","00ff00", "", 1 , "${t.resource!}", ${t.completion!} , 0,${t.phaseNr}, "", "" ));
    </#if>
</#list>

<#--

TaskItem(pID, pName, pStart, pEnd, pColor, pLink, pMile, pRes, pComp, pGroup, pParent, pOpen, pDepend)
pID: (required) is a unique ID used to identify each row for parent functions and for setting dom id for hiding/showing
pName: (required) is the task Label
pStart: (required) the task start date, can enter empty date ('') for groups
pEnd: (required) the task end date, can enter empty date ('') for groups
pColor: (required) the html color for this task; e.g. '00ff00'
pLink: (optional) any http link navigated to when task bar is clicked.
pMile:(optional) represent a milestone
pRes: (optional) resource name
pComp: (required) completion percent
pGroup: (optional) indicates whether this is a group(parent) - 0=NOT Parent; 1=IS Parent
pParent: (required) identifies a parent pID, this causes this task to be a child of identified task
pOpen: UNUSED - in future can be initially set to close folder when chart is first drawn
pDepend: dependency: need previous task finished.

-->
g.Draw();
g.DrawDependencies();
</script>

