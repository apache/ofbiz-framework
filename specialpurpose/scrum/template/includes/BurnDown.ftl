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

<#assign sprint = delegator.findOne("WorkEffort", Static["org.apache.ofbiz.base.util.UtilMisc"].toMap("workEffortId", parameters.get("sprintId")), false)! />
<#assign actualStartDay = Static["org.apache.ofbiz.base.util.UtilDateTime"].getDayStart(sprint.actualStartDate, timeZone, locale)/>
<#assign actualCompletionDay = Static["org.apache.ofbiz.base.util.UtilDateTime"].getDayStart(sprint.actualCompletionDate, timeZone, locale)/>
<#assign dayNumber = ((actualCompletionDay.getTime() - actualStartDay.getTime())/1000/60/60/24) + 1/>
<#assign estimatedHrs = sprint.estimatedMilliSeconds/1000/60/60/>
<#assign members = delegator.findByAnd("WorkEffortPartyAssignment", Static["org.apache.ofbiz.base.util.UtilMisc"].toMap("workEffortId", parameters.get("sprintId")), null, false)/>
<#if members.size() &gt; 0 >
    <#assign maxHours = estimatedHrs * members.size()/>
    <div id="params_birtReport" style='display:none'>
        <INPUT type="HIDDEN" name="sprintId" value="${sprint.workEffortId!}"/>
        <INPUT type="HIDDEN" name="actualStartDate" value="${sprint.actualStartDate!}"/>
        <INPUT type="HIDDEN" name="actualCompletionDate" value="${sprint.actualCompletionDate!}"/>
        <INPUT type="HIDDEN" name="dayNumber" value="${dayNumber!}"/>
        <INPUT type="HIDDEN" name="estimatedHrs" value="${estimatedHrs!}"/>
        <INPUT type="HIDDEN" name="maxHours" value="${maxHours!}"/>
    </div>
    <form id="form_birtReport" method="post"></form>
    <script type="text/javascript">
    function loadViewerbirtReport(){
    var formObj = document.getElementById( "form_birtReport" );
    var paramContainer = document.getElementById("params_birtReport");
    var oParams = paramContainer.getElementsByTagName('input');
    if( oParams )
    {
      for( var i=0;i<oParams.length;i++ )  
      {
        var param = document.createElement( "INPUT" );
        param.type = "HIDDEN";
        param.name= oParams[i].name;
        param.value= oParams[i].value;
        formObj.appendChild( param );
      }
    }
    formObj.action = "/birt/preview?__page=2&__report=component://scrum/webapp/scrum/reports/Burndown.rptdesign&__masterpage=true&__format=html";
    formObj.target = "birtReport";
    formObj.submit( );
    }
    
    </script>
    <iframe name="birtReport" frameborder="no"  scrolling = "auto"  style='height:350px;width:100%;' ></iframe>
    <script type="text/javascript">loadViewerbirtReport();</script> 
</#if>
