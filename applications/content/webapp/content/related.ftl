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


${pages.get(page.getProperty("subMenu"))}

<div class="head1">${page.getProperty("entityName")}</div>
<br/>
<#if addWrapper?exists >
<br/>
<div class="head1">${singleTitle?default("Create New")}</div>
${addWrapper.renderFormString()}
</#if>
<#if listWrapper?exists >
<br/>
${listWrapper.renderFormString()}
</#if>
<#if editWrapper?exists >
<br/>
<div class="head1">Edit</div>
${editWrapper.renderFormString()}
<br/>
</#if>

<#--
<#if hasPermission>
<#else>
 <h3>You do not have permission to view this page. ("CONTENTMGR_VIEW" or "CONTENTMGR_ADMIN" needed)</h3>
</#if>
-->
