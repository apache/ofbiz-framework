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

<#assign selected = page.tabButtonItem?default("void")>
<div class="button-bar button-style-1">
  <ul>
    <li<#if selected == "authorizetransactiontab"> class="selected"</#if>><a href="<@ofbizUrl>/AuthorizeTransaction</@ofbizUrl>">${uiLabelMap.AccountingAuthorize}</a></li>
    <li<#if selected == "capturetransactiontab"> class="selected"</#if>><a href="<@ofbizUrl>/CaptureTransaction</@ofbizUrl>">${uiLabelMap.AccountingCapture}</a></li>
    <li<#if selected == "gatewayresponsestab"> class="selected"</#if>><a href="<@ofbizUrl>/FindGatewayResponses</@ofbizUrl>">${uiLabelMap.AccountingGatewayResponses}</a></li>
    <li<#if selected == "manualtransactiontab"> class="selected"</#if>><a href="<@ofbizUrl>/ManualTransaction</@ofbizUrl>">${uiLabelMap.AccountingManualTransaction}</a></li>
  </ul>
  <br class="clear"/>
</div>
