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
<#if tobrowser?exists && tobrowser>
<h1>XML Export from DataSource(s)</h1>
<br />
<p>This page can be used to export data from the database. The exported documents will have a root tag of "&lt;entity-engine-xml&gt;".</p>
<hr/>
<#if security.hasPermission("ENTITY_MAINT", session)>
    <a href="<@ofbizUrl>xmldsrawdump</@ofbizUrl>" class="buttontext" target="_blank">Click Here to Get Data (or save to file)</a>
<#else>
    <div>You do not have permission to use this page (ENTITY_MAINT needed)</div>
</#if>
<#else>
<#macro displayButtonBar>
  <div class="button-bar">
    <input type="submit" value="Export"/>
    <a href="<@ofbizUrl>xmldsdump?checkAll=true</@ofbizUrl>" class="smallSubmit">Check All</a>
    <a href="<@ofbizUrl>xmldsdump</@ofbizUrl>" class="smallSubmit">Un-Check All</a>
  </div>
</#macro>

<h1>XML Export from DataSource(s)</h1>
<br />
<p>This page can be used to export data from the database. The exported documents will have a root tag of "&lt;entity-engine-xml&gt;".</p>

<hr/>

<#if security.hasPermission("ENTITY_MAINT", session)>
  <h2>Results:</h2>
  <#if Static["org.ofbiz.base.util.UtilValidate"].isNotEmpty(parameters.filename) && (numberOfEntities?number > 0)>
    <p>Wrote XML for all data in ${numberOfEntities} entities.</p>
    <p>Wrote ${numberWritten} records to XML file ${parameters.filename}</p>
  <#elseif Static["org.ofbiz.base.util.UtilValidate"].isNotEmpty(parameters.outpath) && (numberOfEntities?number > 0)>
    <#list results as result>
      <p>${result}</p>
    </#list>
  <#else>
    <p>No filename specified or no entity names specified, doing nothing.</p>
  </#if>
    
  <hr/>
    
  <h2>Export:</h2>
  <form method="post" action="<@ofbizUrl>xmldsdump</@ofbizUrl>" name="entityExport">
    <table class="basic-table">
      <tr>
        <td class="label">Output Directory</td>
        <td><input type="text" size="60" name="outpath" value="${parameters.outpath?if_exists}"/></td>
      </tr>
      <tr>
        <td class="label">Max Records Per File</td>
        <td><input type="text" size="10" name="maxrecords"/></td>
      </tr>
      <tr>
        <td class="label">Single Filename</td>
        <td><input type="text" size="60" name="filename" value="${parameters.filename?if_exists}"/></td>
      </tr>
      <tr>
        <td class="label">Records Updated Since</td>
        <td><input type="text" size="25" name="entityFrom" />
        <a href="javascript:call_cal(document.entityExport.entityFrom, null);" title="View Calendar"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="View Calendar"></a></td>
      </tr>
      <tr>
        <td class="label">Records Updated Before&nbsp</td>
        <td><input type="text" size="25" name="entityThru" />
        <a href="javascript:call_cal(document.entityExport.entityThru, null);" title="View Calendar"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="View Calendar"></a></td>
      </tr>
      <tr>
        <td class="label">OR Out to Browser</td>
        <td><input type="checkbox" name="tobrowser"<#if tobrowser?has_content> checked="checked"</#if>></td>
      </tr>
    </table>
    <br/>
    <p>Entity Names:</p>
    <@displayButtonBar/>
      <div>Entity Sync Dump:
        <input name="entitySyncId" size="30" value="${entitySyncId?if_exists}"/>
      </div>
      Pre-configured set:
      <select name="preConfiguredSetName">
        <option value="">None</option>
        <option value="CatalogExport">Catalog Export</option>
        <option value="Product1">Product Part 1</option>
        <option value="Product2">Product Part 2</option>
        <option value="Product3">Product Part 3</option>
        <option value="Product4">Product Part 4</option>
      </select>
      <br/>

      <table>
        <tr>
          <#assign entCount = 0>
          <#assign check = parameters.checkAll?default("false")>
          <#list entityNames as curEntityName>
            <#if entCount % 3 == 0>
              </tr><tr>
            </#if>
            <#assign entCount = entCount + 1>
            <td><input type="checkbox" name="entityName" value="${curEntityName}"<#if check="true"> checked="checked"</#if>/>${curEntityName}</td>
          </#list>
        </tr>
      </table>

      <@displayButtonBar/>
    </form>
<#else>
    <div>You do not have permission to use this page (ENTITY_MAINT needed)</div>
</#if>
</#if>