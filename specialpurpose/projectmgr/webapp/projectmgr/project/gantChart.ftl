<div align="center">
<table border="0" cellspacing="0" width="882px;">
	<tr>
		<td style="width: 150px; vertical-align: bottom;" align="center"><h2>Name</h2></td>	
		<td style="width: 732px; vertical-align: bottom;" colspan="12" align="center"><h2>Time Line</h2></td>	
	</tr>
	<tr>
		<td style="width: 150px; vertical-align: bottom;">&nbsp;</td>	
		<td style="width: 61px;" align="center" bgcolor="#D3D3D3">January</td>
		<td style="width: 61px;" align="center" bgcolor="#BEBEBE">February</td>
		<td style="width: 61px;" align="center" bgcolor="#D3D3D3">March</td>
		<td style="width: 61px;" align="center" bgcolor="#BEBEBE">April</td>
		<td style="width: 61px;" align="center" bgcolor="#D3D3D3">May</td>
		<td style="width: 61px;" align="center" bgcolor="#BEBEBE">June</td>
		<td style="width: 61px;" align="center" bgcolor="#D3D3D3">July</td>
		<td style="width: 61px;" align="center" bgcolor="#BEBEBE">August</td>
		<td style="width: 61px;" align="center" bgcolor="#D3D3D3">September</td>
		<td style="width: 61px;" align="center" bgcolor="#BEBEBE">October</td>
		<td style="width: 61px;" align="center" bgcolor="#D3D3D3">November</td>
		<td style="width: 61px;" align="center" bgcolor="#BEBEBE">December</td>	
	</tr>
<#assign phasesummary2 = 0/>
<#assign summary2 = 0/>
<#assign day = 24 * 60 * 60 * 1000/>
<#if phaseTaskList?has_content>
<#list phaseTaskList as phases>
<#if phases.workEffortTypeId == "PHASE">
		<#assign phaseId = phases.phaseId/>	
		<#assign phasesStart = Static["org.ofbiz.base.util.UtilDateTime"].toCalendar(phases.estimatedStartDate)/>
		<#assign phasesEnd = Static["org.ofbiz.base.util.UtilDateTime"].toCalendar(phases.estimatedCompletionDate)/>
		<#assign t1 = phasesStart.getTime().getTime()/> 
		<#assign t2 = phasesEnd.getTime().getTime()/>
		<#assign phasesMonth = phases.estimatedStartDate?substring(5,7)?number/>
		<#assign phasesDay = phases.estimatedStartDate?substring(8,10)?number/>
		<#assign phasespacer = ((phasesMonth-1)*60.8)+(phasesDay*2)/>
		<#assign phasesummary = ((t2-t1)/day)*2/>
		<#if (732<(phasespacer+phasesummary))>
			<#assign phasesummary1 = 732-phasespacer/>
			<#assign phasesummary2 = phasesummary-phasesummary1/>
			<#assign phasesummary = phasesummary1/>
		</#if>
		<#assign lastSpacer = 732 - (phasespacer + phasesummary)/>
		
	<tr>
		<td style="width: 150px; vertical-align: bottom;">
			${phases.phaseName?if_exists}
		</td>
		<td colspan="12">
			<img src="/images/spacer.gif" height="15px;" width="${phasespacer}px;"><img src="/images/busy.gif" height="15px;" width="${phasesummary}px;"><img src="/images/spacer.gif" height="15px;" width="${lastSpacer}px;">
		</td>		
	</tr>
	<#if (phasesummary2 != 0)>	
	<tr>		
		<td style="width: 150px; vertical-align: bottom;">
			&nbsp;
		</td>		
		<td colspan="12">
			<img src="/images/busy.gif" height="15px;" width="${phasesummary2}px;">
		</td>		
	</tr>
    </#if>      
</#if>
<#if phases.workEffortTypeId == "TASK">
    <#assign tasks = phases/>      
		<#assign phasesId = tasks.workEffortParentId/>
		<#assign taskId = tasks.taskId/>		
		<#assign taskStart = Static["org.ofbiz.base.util.UtilDateTime"].toCalendar(tasks.estimatedStartDate?if_exists)/>
		<#assign taskEnd = Static["org.ofbiz.base.util.UtilDateTime"].toCalendar(tasks.estimatedCompletionDate?if_exists)/>
		<#assign t3 = taskStart.getTime().getTime()/> 
		<#assign t4 = taskEnd.getTime().getTime()/> 
		<#assign startMonth = tasks.estimatedStartDate?substring(5,7)?number/>
		<#assign startDay = tasks.estimatedStartDate?substring(8,10)?number/>			
		<#assign spacer = ((startMonth-1)*60.8)+(startDay*2)/>
		<#if phasesId==phaseId>
			<#assign summary = ((t4-t3)/day)*2/>	
			<#if (732<(spacer+summary))>
				<#assign summary1 = 732-spacer/>
				<#assign summary2 = summary-summary1/>
				<#assign summary = summary1/>
			</#if>
			<#assign spacer2 = 732 - (spacer + summary)/>
	<tr>
		<td style="width: 150px; vertical-align: bottom;" >
			<a href="/projectmgr/control/taskView?workEffortId=${tasks.taskId}">${tasks.taskName?if_exists}</a>
		</td>
		<td  colspan="12">
			<img src="/images/spacer.gif" height="15px;" width="${spacer}px;"><img src="/images/bluebar.gif" height="15px;" width="${summary}px;"><img src="/images/spacer.gif" height="15px;" width="${spacer2}px;">
		</td>		
	</tr>
	<#if (summary2 != 0)>
	<tr>
		<td style="width: 150px; vertical-align: bottom;" >
			&nbsp;
		</td>
		<td  colspan="12">
			<img src="/images/bluebar.gif" height="15px;" width="${summary2}px;">
		</td>		
	</tr>
	</#if>
	</#if>	
</#if>  
</#list>
</#if>
</table>
</div><br>
<img src="/images/busy.gif" height="15px;" width="30px;"><b> : Phase</b><br>
<img src="/images/bluebar.gif" height="15px;" width="30px;"><b> : Task</b>
