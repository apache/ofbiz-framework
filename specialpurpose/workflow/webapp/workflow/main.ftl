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

<#-- TODO: Remove embedded style during UI refactor -->
<style type="text/css">
.webToolList {
list-style-type: none;
margin-left: 1em;
}
.webToolList li {
padding: 0 1em 0 1em;
}
.webToolList h3 {
padding: 1em 0 0 0;
margin-left: -1em;
}
.webToolList li a {
color: #000099;
font-weight: bold;
text-decoration: none;
}
.webToolList li a:hover {
text-decoration: underline;
}
</style>

<div class="screenlet">
  <div class="screenlet-title-bar">
    <ul>
      <li class="h3">Main Page</li>
    </ul>
    <br class="clear"/>
  </div>
  <div class="screenlet-body">
    <#if !userLogin?has_content>
    </#if>
    <#if userLogin?has_content>
      <ul class="webToolList">
        <#if security.hasPermission("WORKFLOW_MAINT", session)>
          <li><h3>${uiLabelMap.WorkflowWorkflowEngineTools}</h3></li>
          <li><a href="<@ofbizUrl>workflowMonitor</@ofbizUrl>">${uiLabelMap.WorkflowWorkflowMonitor}</a></li>
          <li><a href="<@ofbizUrl>readxpdl</@ofbizUrl>">${uiLabelMap.WorkflowReadXPDLFile}</a></li>
        </#if>
      </ul>
    </#if>
  </div>
</div>
<!-- end main.ftl -->
