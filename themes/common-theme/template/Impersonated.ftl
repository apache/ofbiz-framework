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

<#assign messageMap = Static["org.apache.ofbiz.base.util.UtilMisc"].toMap("originUserLoginId", impersonator)/>
<#assign impersonationMessage= Static["org.apache.ofbiz.base.util.UtilProperties"].getMessage("SecurityextUiLabels", "loginevents.impersonation_in_process", messageMap, locale)/>
<#assign fromDate = Static["org.apache.ofbiz.base.util.UtilFormatOut"].formatDateTime(impersonationFromDate, "", locale, timeZone)/>
<div id="impersonateMode">
  <div class="content">
      <img src="/images/img/impersonate-ico.png" alt="${uiLabelMap.CommonImpersonateTitle}"/>
      <p class="user">${impersonationMessage!}<p>
      <p>${uiLabelMap.CommonSince} ${fromDate!}</p>
      <p><a id="logout" class="user-pref-btn" href="<@ofbizUrl>logout</@ofbizUrl>">${uiLabelMap.CommonLogout}</a></p>
  </div>
</div>
<script>
    setInterval('window.location.reload()', 30000);
</script>
