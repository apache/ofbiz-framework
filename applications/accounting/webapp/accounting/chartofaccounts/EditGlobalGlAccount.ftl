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

<#assign uiLabelMap = requestAttributes.uiLabelMap>
<#if hasPermission>

<#if glAccountId?has_content>
  <div class='tabContainer'>
  <a href="<@ofbizUrl>EditGlobalGlAccount?glAccountId=${glAccountId}</@ofbizUrl>" class="tabButtonSelected">GL Account</a>
  <a href="<@ofbizUrl>EditGlobalGlAccountOrganizations?glAccountId=${glAccountId}</@ofbizUrl>" class="tabButton">Organizations</a>
  <a href="<@ofbizUrl>EditGlobalGlAccountRoles?glAccountId=${glAccountId}</@ofbizUrl>" class="tabButton">Roles</a>
  </div>
</#if>
<div class="head1">GL Account <span class='head2'><#if (glAccount.accountName)?has_content>"${glAccount.accountName}"</#if> [${uiLabelMap.CommonId}:${glAccountId?if_exists}]</span></div>
<a href="<@ofbizUrl>EditGlobalGlAccount</@ofbizUrl>" class="buttontext">[New Global GL Account]</a>

${editGlAccountWrapper.renderFormString()}

<#else>
  <h3>${uiLabelMap.AccountingViewPermissionError}</h3>
</#if>
