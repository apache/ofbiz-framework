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
      <li class="h3">${uiLabelMap.WebtoolsMainPage}</li>
      <li class="disabled">${delegator.getDelegatorName()}</li>
    </ul>
    <br class="clear"/>
  </div>
  <div class="screenlet-body">
    <#if !userLogin?has_content>
      <div>${uiLabelMap.WebtoolsForSomethingInteresting}.</div>
      <br />
      <div>${uiLabelMap.WebtoolsNoteAntRunInstall}</div>
      <br />
      <div><a href="<@ofbizUrl>checkLogin</@ofbizUrl>">${uiLabelMap.CommonLogin}</a></div>
    </#if>
    <#if userLogin?has_content>
      <ul class="webToolList">
        <li><h3>${uiLabelMap.WebtoolsCacheDebugTools}</h3></li>
        <li><a href="<@ofbizUrl>FindUtilCache</@ofbizUrl>">${uiLabelMap.WebtoolsCacheMaintenance}</a></li>
        <li><a href="<@ofbizUrl>LogConfiguration</@ofbizUrl>">${uiLabelMap.WebtoolsAdjustDebuggingLevels}</a></li>
        <li><a href="<@ofbizUrl>LogView</@ofbizUrl>">${uiLabelMap.WebtoolsViewLog}</a></li>
        <li><a href="<@ofbizUrl>ViewComponents</@ofbizUrl>">${uiLabelMap.WebtoolsViewComponents}</a></li>
        <#if security.hasPermission("ARTIFACT_INFO_VIEW", session)>
          <li><h3>${uiLabelMap.WebtoolsGeneralArtifactInfoTools}</h3></li>
          <li><a href="<@ofbizUrl>ViewComponents</@ofbizUrl>" target="_blank">${uiLabelMap.WebtoolsArtifactInfo}</a></li>
          <li><a href="<@ofbizUrl>entityref</@ofbizUrl>" target="_blank">${uiLabelMap.WebtoolsEntityReference} - ${uiLabelMap.WebtoolsEntityReferenceInteractiveVersion}</a></li>
          <li><a href="<@ofbizUrl>ServiceList</@ofbizUrl>">${uiLabelMap.WebtoolsServiceReference}</a></li>
        </#if>
        <#if security.hasPermission("LABEL_MANAGER_VIEW", session)>
          <li><h3>${uiLabelMap.WebtoolsLabelManager}</h3></li>
          <li><a href="<@ofbizUrl>SearchLabels</@ofbizUrl>">${uiLabelMap.WebtoolsLabelManager}</a></li>
        </#if>
        <#if security.hasPermission("ENTITY_MAINT", session)>
          <li><h3>${uiLabelMap.WebtoolsEntityEngineTools}</h3></li>
          <li><a href="<@ofbizUrl>entitymaint</@ofbizUrl>">${uiLabelMap.WebtoolsEntityDataMaintenance}</a></li>
          <li><a href="<@ofbizUrl>entityref</@ofbizUrl>" target="_blank">${uiLabelMap.WebtoolsEntityReference} - ${uiLabelMap.WebtoolsEntityReferenceInteractiveVersion}</a></li>
          <li><a href="<@ofbizUrl>entityref?forstatic=true</@ofbizUrl>" target="_blank">${uiLabelMap.WebtoolsEntityReference} - ${uiLabelMap.WebtoolsEntityReferenceStaticVersion}</a></li>
          <li><a href="<@ofbizUrl>entityrefReport</@ofbizUrl>" target="_blank">${uiLabelMap.WebtoolsEntityReferencePdf}</a></li>
          <li><a href="<@ofbizUrl>EntitySQLProcessor</@ofbizUrl>">${uiLabelMap.PageTitleEntitySQLProcessor}</a></li>
          <li><a href="<@ofbizUrl>EntitySyncStatus</@ofbizUrl>">${uiLabelMap.WebtoolsEntitySyncStatus}</a></li>
          <li><a href="<@ofbizUrl>view/ModelInduceFromDb</@ofbizUrl>" target="_blank">${uiLabelMap.WebtoolsInduceModelXMLFromDatabase}</a></li>
          <li><a href="<@ofbizUrl>EntityEoModelBundle</@ofbizUrl>">${uiLabelMap.WebtoolsExportEntityEoModelBundle}</a></li>
          <li><a href="<@ofbizUrl>view/checkdb</@ofbizUrl>">${uiLabelMap.WebtoolsCheckUpdateDatabase}</a></li>
          <li><a href="<@ofbizUrl>ConnectionPoolStatus</@ofbizUrl>">${uiLabelMap.ConnectionPoolStatus}</a></li>
          <#-- want to leave these out because they are only working so-so, and cause people more problems that they solve, IMHO
            <li><a href="<@ofbizUrl>view/EditEntity</@ofbizUrl>"  target="_blank">Edit Entity Definitions</a></li>
            <li><a href="<@ofbizUrl>ModelWriter</@ofbizUrl>" target="_blank">Generate Entity Model XML (all in one)</a></li>
            <li><a href="<@ofbizUrl>ModelWriter?savetofile=true</@ofbizUrl>" target="_blank">Save Entity Model XML to Files</a></li>
          -->
          <#-- not working right now anyway
            <li><a href="<@ofbizUrl>ModelGroupWriter</@ofbizUrl>" target="_blank">Generate Entity Group XML</a></li>
            <li><a href="<@ofbizUrl>ModelGroupWriter?savetofile=true</@ofbizUrl>" target="_blank">Save Entity Group XML to File</a></li>
          -->
          <#--
            <li><a href="<@ofbizUrl>view/tablesMySql</@ofbizUrl>">MySQL Table Creation SQL</a></li>
            <li><a href="<@ofbizUrl>view/dataMySql</@ofbizUrl>">MySQL Auto Data SQL</a></li>
          -->
          <li><h3>${uiLabelMap.WebtoolsEntityXMLTools}</h3></li>
          <li><a href="<@ofbizUrl>xmldsdump</@ofbizUrl>">${uiLabelMap.PageTitleEntityExport}</a></li>
          <li><a href="<@ofbizUrl>EntityExportAll</@ofbizUrl>">${uiLabelMap.PageTitleEntityExportAll}</a></li>
          <li><a href="<@ofbizUrl>EntityImport</@ofbizUrl>">${uiLabelMap.PageTitleEntityImport}</a></li>
          <li><a href="<@ofbizUrl>EntityImportDir</@ofbizUrl>">${uiLabelMap.PageTitleEntityImportDir}</a></li>
          <li><a href="<@ofbizUrl>EntityImportReaders</@ofbizUrl>">${uiLabelMap.PageTitleEntityImportReaders}</a></li>

          <li><h3>${uiLabelMap.WebtoolsEntityJSONTools}</h3></li>
          <li><a href="<@ofbizUrl>jsondsdump</@ofbizUrl>">${uiLabelMap.PageTitleEntityExportJson}</a></li>
          <li><a href="<@ofbizUrl>EntityExportAllJson</@ofbizUrl>">${uiLabelMap.PageTitleEntityExportAllJson}</a></li>
          <li><a href="<@ofbizUrl>EntityImportJson</@ofbizUrl>">${uiLabelMap.PageTitleEntityImportJson}</a></li>
          <li><a href="<@ofbizUrl>EntityImportDirJson</@ofbizUrl>">${uiLabelMap.PageTitleEntityImportDirJson}</a></li>
        </#if>
        <#if security.hasPermission("SERVICE_MAINT", session)>
          <li><h3>${uiLabelMap.WebtoolsServiceEngineTools}</h3></li>
          <li><a href="<@ofbizUrl>ServiceList</@ofbizUrl>">${uiLabelMap.WebtoolsServiceReference}</a></li>
          <li><a href="<@ofbizUrl>scheduleJob</@ofbizUrl>">${uiLabelMap.PageTitleScheduleJob}</a></li>
          <li><a href="<@ofbizUrl>runService</@ofbizUrl>">${uiLabelMap.PageTitleRunService}</a></li>
          <li><a href="<@ofbizUrl>FindJob</@ofbizUrl>">${uiLabelMap.PageTitleJobList}</a></li>
          <li><a href="<@ofbizUrl>threadList</@ofbizUrl>">${uiLabelMap.PageTitleThreadList}</a></li>
          <li><a href="<@ofbizUrl>FindJobManagerLock</@ofbizUrl>">${uiLabelMap.PageTitleJobManagerLockList}</a></li>
          <li><a href="<@ofbizUrl>ServiceLog</@ofbizUrl>">${uiLabelMap.WebtoolsServiceLog}</a></li>
        </#if>
        <#if security.hasPermission("DATAFILE_MAINT", session)>
          <li><h3>${uiLabelMap.WebtoolsDataFileTools}</h3></li>
          <li><a href="<@ofbizUrl>viewdatafile</@ofbizUrl>">${uiLabelMap.WebtoolsWorkWithDataFiles}</a></li>
        </#if>
        <li><h3>${uiLabelMap.WebtoolsMiscSetupTools}</h3></li>
        <#if security.hasPermission("PORTALPAGE_ADMIN", session)>
          <li><a href="<@ofbizUrl>FindGeo</@ofbizUrl>">${uiLabelMap.WebtoolsGeoManagement}</a></li>
          <li><a href="<@ofbizUrl>WebtoolsLayoutDemo</@ofbizUrl>">${uiLabelMap.WebtoolsLayoutDemo}</a></li>
        </#if>
        <#if security.hasPermission("ENUM_STATUS_MAINT", session)>
          <#--
          <li><a href="<@ofbizUrl>EditEnumerationTypes</@ofbizUrl>">Edit Enumerations</a></li>
          <li><a href="<@ofbizUrl>EditStatusTypes</@ofbizUrl>">Edit Status Options</a></li>
          -->
        </#if>
        <li><h3>${uiLabelMap.WebtoolsPerformanceTests}</h3></li>
        <li><a href="<@ofbizUrl>EntityPerformanceTest</@ofbizUrl>">${uiLabelMap.WebtoolsEntityEngine}</a></li>
        <#if security.hasPermission("SERVER_STATS_VIEW", session)>
          <li><h3>${uiLabelMap.WebtoolsServerHitStatisticsTools}</h3></li>
          <li><a href="<@ofbizUrl>StatsSinceStart</@ofbizUrl>">${uiLabelMap.WebtoolsStatsSinceServerStart}</a></li>
        </#if>
        <li><h3>${uiLabelMap.WebtoolsCertsX509}</h3></li>
        <li><a href="<@ofbizUrl>myCertificates</@ofbizUrl>">${uiLabelMap.WebtoolsMyCertificates}</a></li>
      </ul>
    </#if>
  </div>
</div>
