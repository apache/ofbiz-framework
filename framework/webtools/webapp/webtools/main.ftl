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
              <div class="tabletext">For something interesting make sure you are logged in, try username:admin, password:ofbiz.</div>
              <br/>
            </#if>
            <div class="tabletext">The purpose of this Web Tools administration package is to contain all of the 
            administration tools that directly relate to the various Core Tool Components. The Core Tool Component layer is
            defined in the architecture documents as the container of all entity definitions shared by the vertical applications that
            are built on top of these entity definitions and the tools surrounding them such as the entity, workflow, and rule engines,
            content and knowledge management, data analysis, and so forth.</div>
            <br/>
            <div class="tabletext">This application is primarily intended for developers and system administrators.</div>
            <#if userLogin?has_content>
            <ul>
                <li><div class="tabletext">Cache &amp; Debug Tools</div>
                <ul>
                    <li><a href="<@ofbizUrl>/FindUtilCache</@ofbizUrl>" class="linktext">Cache Maintenance</a>
                    <li><a href="<@ofbizUrl>/debuglevels</@ofbizUrl>" class="linktext">Adjust Debugging Levels</a>
                </ul>
              <#if security.hasPermission("ENTITY_MAINT", session)>
                <li><div class="tabletext">Entity Engine Tools</div>
                <ul>
                  <li><a href="<@ofbizUrl>/entitymaint</@ofbizUrl>" class="linktext">Entity Data Maintenance</a>
                  <li><a href="<@ofbizUrl>/view/entityref</@ofbizUrl>" class="linktext" target="_blank">Entity Reference</a>&nbsp;<a href="<@ofbizUrl>/view/entityref?forstatic=true</@ofbizUrl>" class="linktext" target="_blank">[Static Version]</a>
                  <li><a href="<@ofbizUrl>/EntitySQLProcessor</@ofbizUrl>" class="linktext">Entity SQL Processor</a>
                  <li><a href="<@ofbizUrl>/EntitySyncStatus</@ofbizUrl>" class="linktext">Entity Sync Status</a>
                  <li><a href="<@ofbizUrl>/view/ModelInduceFromDb</@ofbizUrl>" target="_blank" class="linktext">Induce Model XML from Database</a><br/>
                  <li><a href="<@ofbizUrl>/view/checkdb</@ofbizUrl>" class="linktext">Check/Update Database</a>

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
                <li><div class="tabletext">Entity XML Tools</div>
                <ul>
                  <li><a href="<@ofbizUrl>/xmldsdump</@ofbizUrl>" class="linktext">XML Data Export</a>
                  <li><a href="<@ofbizUrl>/EntityExportAll</@ofbizUrl>" class="linktext">XML Data Export All</a>
                  <li><a href="<@ofbizUrl>/EntityImport</@ofbizUrl>" class="linktext">XML Data Import</a>
                  <li><a href="<@ofbizUrl>/EntityImportDir</@ofbizUrl>" class="linktext">XML Data Import Dir</a>
                </ul>
              </#if>
              <#if security.hasPermission("SERVICE_MAINT", session)>
                <li><div class="tabletext">Service Engine Tools</div>
                <ul>
                  <li><a href="<@ofbizUrl>/availableServices</@ofbizUrl>" class="linktext">Service Reference</a>
                  <li><a href="<@ofbizUrl>/scheduleJob</@ofbizUrl>" class="linktext">Schedule Job</a>
                  <li><a href="<@ofbizUrl>/jobList</@ofbizUrl>" class="linktext">Job List</a>
                  <li><a href="<@ofbizUrl>/threadList</@ofbizUrl>" class="linktext">Thread List</a>
                  <li><a href="<@ofbizUrl>/serviceList</@ofbizUrl>" class="linktext">Service Log</a>
                </ul>
              </#if>
              <#if security.hasPermission("WORKFLOW_MAINT", session)>
                <li><div class="tabletext">Workflow Engine Tools</div>
                <ul>
                  <li><a href="<@ofbizUrl>/workflowMonitor</@ofbizUrl>" class="linktext">Workflow Monitor</a>
                  <li><a href="<@ofbizUrl>/readxpdl</@ofbizUrl>" class="linktext">Read XPDL File</a>
                </ul>
              </#if>
              <#if security.hasPermission("DATAFILE_MAINT", session)>
                <li><div class="tabletext">Data File Tools</div>
                <ul>
                  <li><a href="<@ofbizUrl>/viewdatafile</@ofbizUrl>" class="linktext">Work With Data Files</a>
                </ul>
              </#if>
                <li><div class="tabletext">Misc. Setup Tools</div>
                <ul>
                  <#if security.hasPermission("PERIOD_MAINT", session)>
                    <li><a href="<@ofbizUrl>/EditCustomTimePeriod</@ofbizUrl>" class="linktext">Edit Custom Time Periods</a>
                  </#if>
                  <#if security.hasPermission("ENUM_STATUS_MAINT", session)>
                  <!--
                    <li><a href="<@ofbizUrl>/EditEnumerationTypes</@ofbizUrl>" class="linktext">Edit Enumerations</a>
                    <li><a href="<@ofbizUrl>/EditStatusTypes</@ofbizUrl>" class="linktext">Edit Status Options</a>
                  -->
                  </#if>
                </ul>
                <li><div class="tabletext">Performance Tests</div>
                <ul>
                  <li><a href="<@ofbizUrl>/EntityPerformanceTest</@ofbizUrl>" class="linktext">Entity Engine</a>
                </ul>
              <#if security.hasPermission("SERVER_STATS_VIEW", session)>
                <li><div class="tabletext">Server Hit Statistics Tools</div>
                <ul>
                  <li><a href="<@ofbizUrl>/StatsSinceStart</@ofbizUrl>" class="linktext">Stats Since Server Start</a>
                </ul>
              </#if>
            </ul>
            </#if>

            <div class="tabletext">NOTE: If you have not already run the installation data loading script, from the ofbiz home directory run "ant run-install" or "java -jar ofbiz.jar install"</div>
          </td>
        </tr>
      </table>
    </td>
  </tr>
</table>
