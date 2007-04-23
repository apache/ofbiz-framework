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

<#-- TODO: probably some kind of permission checking to see that this userLogin can view such and such reports -->

<div>

<script language="javascript">
function submitReconcile(form) {
  form.action="<@ofbizUrl>ReconcileGlAccountOrganization</@ofbizUrl>";
  form.submit();
}

function submitViewBalance(form) {
  form.action="<@ofbizUrl>viewGlAccountBalance</@ofbizUrl>";
  form.submit();
}
</script>

<#if closedTimePeriods?has_content>
<p>${uiLabelMap.TimePeriodMessage2}:
<ul type="circle">
<#list closedTimePeriods as timePeriod>
<li>${timePeriod.periodName?if_exists} ${timePeriod.periodNum?string("####")} (${timePeriod.fromDate} - ${timePeriod.thruDate})
</#list>
</ul></p>
<p>
<#else>
<p>${uiLabelMap.TimePeriodMessage1}</p>
</#if>

</div>
