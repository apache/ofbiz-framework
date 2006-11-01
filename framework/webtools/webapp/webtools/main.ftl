<#--
Copyright 2001-2006 The Apache Software Foundation

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.
-->

<table border="0" width="100%" cellspacing="0" cellpadding="0" class="boxoutside">
  <tr>
    <td width="100%">
      <table width="100%" border="0" cellspacing="0" cellpadding="0" class="boxtop">
        <tr>
          <td align="left" width="90%" >
            <div class="boxhead">&nbsp;${uiLabelMap.WebtoolsMainPage}</div>
          </td>
          <td align="right" width="10%"><div class="lightbuttontextdisabled">${delegator.getDelegatorName()}</div></td>
        </tr>
      </table>
    </td>
  </tr>
  <tr>
    <td width="100%">
      <table width="100%" border="0" cellspacing="0" cellpadding="0" class="boxbottom">
        <tr>
          <td>
            <#if !userLogin?has_content>
              <div class="tabletext">${uiLabelMap.WebtoolsMessage14}.</div>
              <br/>
            </#if>
            <div class="tabletext">${uiLabelMap.WebtoolsTitle1} ${uiLabelMap.WebtoolsTitle2} ${uiLabelMap.WebtoolsTitle3} ${uiLabelMap.WebtoolsTitle4} ${uiLabelMap.WebtoolsTitle5} ${uiLabelMap.WebtoolsTitle6}.</div>
            <br/>
            <div class="tabletext">${uiLabelMap.WebtoolsMessage13}.</div>
            <#if userLogin?has_content>
            <ul>
                <li><div class="tabletext">${uiLabelMap.WebtoolsCacheDebugTools}</div>
                <ul>
                    <li><a href="<@ofbizUrl>/FindUtilCache</@ofbizUrl>" class="linktext">${uiLabelMap.WebtoolsCacheMaintenance}</a>
                    <li><a href="<@ofbizUrl>/LogConfiguration</@ofbizUrl>" class="linktext">${uiLabelMap.WebtoolsAdjustDebuggingLevels}</a>
                </ul>
              <#if security.hasPermission("ENTITY_MAINT", session)>
                <li><div class="tabletext">${uiLabelMap.WebtoolsEntityEngineTools}</div>
                <ul>
                  <li><a href="<@ofbizUrl>/entitymaint</@ofbizUrl>" class="linktext">${uiLabelMap.WebtoolsEntityDataMaintenance}</a>
                  <li><a href="<@ofbizUrl>/view/entityref</@ofbizUrl>" class="linktext" target="_blank">${uiLabelMap.WebtoolsServiceReference}</a>&nbsp;<a href="<@ofbizUrl>/view/entityref?forstatic=true</@ofbizUrl>" class="linktext" target="_blank">[Static Version]</a>
                  <li><a href="<@ofbizUrl>/EntitySQLProcessor</@ofbizUrl>" class="linktext">${uiLabelMap.PageTitleEntitySQLProcessor}</a>
                  <li><a href="<@ofbizUrl>/EntitySyncStatus</@ofbizUrl>" class="linktext">${uiLabelMap.WebtoolsEntitySyncStatus}</a>
                  <li><a href="<@ofbizUrl>/view/ModelInduceFromDb</@ofbizUrl>" target="_blank" class="linktext">${uiLabelMap.WebtoolsInduceModelXMLFromDatabase}</a><br/>
                  <li><a href="<@ofbizUrl>/view/checkdb</@ofbizUrl>" class="linktext">${uiLabelMap.WebtoolsCheckUpdateDatabase}</a>

                  <!-- want to leave these out because they are only working so-so, and cause people more problems that they solve, IMHO
                  <ul>
                    <li><a href="<@ofbizUrl>/view/EditEntity</@ofbizUrl>" class="linktext" target="_blank">Edit Entity Definitions</a>
                    <li><a href="<@ofbizUrl>/ModelWriter</@ofbizUrl>" class="linktext" target="_blank">Generate Entity Model XML (all in one)</a>
                    <li><a href="<@ofbizUrl>/ModelWriter?savetofile=true</@ofbizUrl>" target="_blank" class="linktext">Save Entity Model XML to Files</a><br/>
                  -->
                  <!-- not working right now anyway
                    <li><a href="<@ofbizUrl>/ModelGroupWriter</@ofbizUrl>" target="_blank" class="linktext">Generate Entity Group XML</a><br/>
                    <li><a href="<@ofbizUrl>/ModelGroupWriter?savetofile=true</@ofbizUrl>" target="_blank" class="linktext">Save Entity Group XML to File</a><br/>
                  </ul>
                  -->
                  <!--
                  <li><a href="<@ofbizUrl>/view/tablesMySql</@ofbizUrl>" class="linktext">MySQL Table Creation SQL</a>
                  <li><a href="<@ofbizUrl>/view/dataMySql</@ofbizUrl>" class="linktext">MySQL Auto Data SQL</a>
                  -->
                </ul>
                <li><div class="tabletext">${uiLabelMap.WebtoolsEntityXMLTools}</div>
                <ul>
                  <li><a href="<@ofbizUrl>/xmldsdump</@ofbizUrl>" class="linktext">${uiLabelMap.PageTitleEntityExport}</a>
                  <li><a href="<@ofbizUrl>/EntityExportAll</@ofbizUrl>" class="linktext">${uiLabelMap.PageTitleEntityExportAll}</a>
                  <li><a href="<@ofbizUrl>/EntityImport</@ofbizUrl>" class="linktext">${uiLabelMap.PageTitleEntityImport}</a>
                  <li><a href="<@ofbizUrl>/EntityImportDir</@ofbizUrl>" class="linktext">${uiLabelMap.PageTitleEntityImportDir}</a>
                </ul>
              </#if>
              <#if security.hasPermission("SERVICE_MAINT", session)>
                <li><div class="tabletext">${uiLabelMap.WebtoolsServiceEngineTools}</div>
                <ul>
                  <li><a href="<@ofbizUrl>/availableServices</@ofbizUrl>" class="linktext">${uiLabelMap.WebtoolsServiceReference}</a>
                  <li><a href="<@ofbizUrl>/scheduleJob</@ofbizUrl>" class="linktext">${uiLabelMap.PageTitleScheduleJob}</a>
                  <li><a href="<@ofbizUrl>/jobList</@ofbizUrl>" class="linktext">${uiLabelMap.PageTitleJobList}</a>
                  <li><a href="<@ofbizUrl>/threadList</@ofbizUrl>" class="linktext">${uiLabelMap.PageTitleThreadList}</a>
                  <li><a href="<@ofbizUrl>/serviceList</@ofbizUrl>" class="linktext">${uiLabelMap.WebtoolsServiceLog}</a>
                </ul>
              </#if>
              <#if security.hasPermission("WORKFLOW_MAINT", session)>
                <li><div class="tabletext">${uiLabelMap.WebtoolsWorkflowEngineTools}</div>
                <ul>
                  <li><a href="<@ofbizUrl>/workflowMonitor</@ofbizUrl>" class="linktext">${uiLabelMap.WebtoolsWorkflowMonitor}</a>
                  <li><a href="<@ofbizUrl>/readxpdl</@ofbizUrl>" class="linktext">${uiLabelMap.WebtoolsReadXPDLFile}</a>
                </ul>
              </#if>
              <#if security.hasPermission("DATAFILE_MAINT", session)>
                <li><div class="tabletext">${uiLabelMap.WebtoolsDataFileTools}</div>
                <ul>
                  <li><a href="<@ofbizUrl>/viewdatafile</@ofbizUrl>" class="linktext">${uiLabelMap.WebtoolsWorkWithDataFiles}</a>
                </ul>
              </#if>
                <li><div class="tabletext">${uiLabelMap.WebtoolsMiscSetupTools}</div>
                <ul>
                  <#if security.hasPermission("PERIOD_MAINT", session)>
                    <li><a href="<@ofbizUrl>/EditCustomTimePeriod</@ofbizUrl>" class="linktext">${uiLabelMap.WebtoolsEditCustomTimePeriods}</a>
                  </#if>
                  <#if security.hasPermission("ENUM_STATUS_MAINT", session)>
                  <!--
                    <li><a href="<@ofbizUrl>/EditEnumerationTypes</@ofbizUrl>" class="linktext">Edit Enumerations</a>
                    <li><a href="<@ofbizUrl>/EditStatusTypes</@ofbizUrl>" class="linktext">Edit Status Options</a>
                  -->
                  </#if>
                </ul>
                <li><div class="tabletext">${uiLabelMap.WebtoolsPerformanceTests}</div>
                <ul>
                  <li><a href="<@ofbizUrl>/EntityPerformanceTest</@ofbizUrl>" class="linktext">${uiLabelMap.WebtoolsEntityEngine}</a>
                </ul>
              <#if security.hasPermission("SERVER_STATS_VIEW", session)>
                <li><div class="tabletext">${uiLabelMap.WebtoolsServerHitStatisticsTools}</div>
                <ul>
                  <li><a href="<@ofbizUrl>/StatsSinceStart</@ofbizUrl>" class="linktext">${uiLabelMap.WebtoolsStatsSinceServerStart}</a>
                </ul>
              </#if>
            </ul>
            </#if>

            <div class="tabletext">${uiLabelMap.WebtoolsNote1}</div>
          </td>
        </tr>
      </table>
    </td>
  </tr>
</table>
