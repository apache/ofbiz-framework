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

<h1>Read XPDL File</h1>
<br />
<p>This page is used to read and import XPDL files into the workflow process repository.</p>
<hr/>
<#if security.hasPermission("WORKFLOW_MAINT", session)>
  <form method="post" action="<@ofbizUrl>readxpdl</@ofbizUrl>">
    <table class="basic-table form-widget-table">
      <tr>
        <td class="label">XPDL Filename or URL</td>
        <td><input name='XPDL_LOCATION' type="text" size='60' value='${parameters.XPDL_LOCATION?if_exists}'></td>
        <td><span class="label">Is URL?</span><input type="checkbox" name='XPDL_IS_URL'<#if parameters.XPDL_IS_URL?has_content> checked="checked"</#if>></td>
      </tr>
      <tr>
        <td class="label">Import/Update to DB?</td>
        <td><input type="checkbox" name='XPDL_IMPORT'></td>
        <td>&nbsp;</td>
      </tr>
      <tr>
        <td>&nbsp;</td>
        <td><input type="submit" value='View'></td>
        <td>&nbsp;</td>
      </tr>
    </table>
  </form>

  <#if parameters.XPDL_LOCATION?has_content>
    <#if messages?has_content>
      <hr/>
      <h1>The following occurred:</h1>
      <div>
        <#list messages as message>
          <p>${message}</p>
        </#list>
      </div>
    </#if>

    <#if toBeStored?has_content>
      <#list toBeStored as entity>
          <pre>${entity}</pre>
      </#list>
      <hr/>
      <div>Read and printed ${toBeStored.size()} entities.</div>
    <#else>
      <div>No entities read.</div>
    </#if>
  </#if>
<#else>
  <h3>You do not have permission to use this page (WORKFLOW_MAINT needed)</h3>
</#if>
