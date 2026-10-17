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

<#-- K8s injected-mode notice: vault-level FETCH events live in the operator log, not here -->
<#if isK8sMode!false>
  <div class="eventMessage">
    <strong>${uiLabelMap.WebtoolsSecretAuditK8sNoticeTitle}:</strong>
    ${uiLabelMap.WebtoolsSecretAuditK8sNoticeBody}
  </div>
</#if>


<form method="get" action="<@ofbizUrl>SecretAuditLog</@ofbizUrl>" class="basic-form">
  <table class="basic-table" cellspacing="0">
    <tbody>
      <tr>
        <td class="label">${uiLabelMap.WebtoolsSecretAuditFilterUser}</td>
        <td><input type="text" name="filterUserLoginId" size="20" value="${filterUserLoginId!}"/></td>
        <td class="label">${uiLabelMap.WebtoolsSecretAuditFilterAction}</td>
        <td>
          <select name="filterAction">
            <option value=""></option>
            <#list ["FETCH","CACHE_HIT","SYNC","STORE","FLUSH_CACHE","RESET_STATS","RELOAD_PROVIDER","TEST_CONNECTION","ROTATION_POLL","UNKNOWN"] as a>
              <option value="${a}"<#if filterAction! == a> selected="selected"</#if>>${a}</option>
            </#list>
          </select>
        </td>
      </tr>
      <tr>
        <td class="label">${uiLabelMap.WebtoolsSecretAuditFilterOutcome}</td>
        <td>
          <select name="filterOutcome">
            <option value=""></option>
            <#list ["SUCCESS","FAILURE","NOT_FOUND","DENIED","FALLBACK_USED","NO_CHANGE"] as o>
              <option value="${o}"<#if filterOutcome! == o> selected="selected"</#if>>${o}</option>
            </#list>
          </select>
        </td>
        <td class="label">${uiLabelMap.WebtoolsSecretAuditFilterKey}</td>
        <td><input type="text" name="filterSecretKeyRef" size="30" value="${filterSecretKeyRef!}"/></td>
      </tr>
      <tr>
        <td class="label">${uiLabelMap.WebtoolsSecretAuditFilterFrom}</td>
        <td><input type="date" name="filterDateFrom" value="${filterDateFrom!}"/></td>
        <td class="label">${uiLabelMap.WebtoolsSecretAuditFilterTo}</td>
        <td><input type="date" name="filterDateTo" value="${filterDateTo!}"/></td>
      </tr>
      <tr>
        <td class="label">${uiLabelMap.WebtoolsSecretAuditFilterProvider}</td>
        <td>
          <#assign providerOptions = [
            {"v": "AwsSecretsManagerProvider",       "l": "AWS"},
            {"v": "AzureKeyVaultSecretsProvider",    "l": "AZURE"},
            {"v": "HashicorpVaultSecretsProvider",   "l": "HASHICORP"},
            {"v": "GcpSecretManagerSecretsProvider", "l": "GCP"},
            {"v": "BitwardenSecretsProvider",        "l": "BITWARDEN"},
            {"v": "OnePasswordSecretsProvider",      "l": "1PASSWORD"},
            {"v": "EnvVarSecretProvider",            "l": "ENV_VAR"},
            {"v": "FileBasedSecretProvider",         "l": "FILE"}
          ]/>
          <select name="filterProviderType">
            <option value=""></option>
            <#list providerOptions as p>
              <option value="${p.v}"<#if filterProviderType! == p.v> selected="selected"</#if>>${p.l}</option>
            </#list>
          </select>
        </td>
        <td class="label">${uiLabelMap.WebtoolsSecretAuditFilterDeployMode}</td>
        <td>
          <select name="filterDeploymentMode">
            <option value=""></option>
            <#list ["DIRECT","K8S_INJECTED"] as d>
              <option value="${d}"<#if filterDeploymentMode! == d> selected="selected"</#if>>${d}</option>
            </#list>
          </select>
        </td>
      </tr>
      <tr>
        <td colspan="4">
          <a href="<@ofbizUrl>SecretAuditLog</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonClear}</a>
          &nbsp;
          <input type="submit" value="${uiLabelMap.CommonFind}" class="smallSubmit"/>
        </td>
      </tr>
    </tbody>
  </table>
</form>

<#-- Export CSV — date range is mandatory; the form carries current filter state -->
<hr/>
<h3>${uiLabelMap.WebtoolsSecretAuditExportTitle}</h3>
<form method="get" action="<@ofbizUrl>exportSecretAuditLog</@ofbizUrl>" class="basic-form">
  <input type="hidden" name="filterUserLoginId"   value="${filterUserLoginId!}"/>
  <input type="hidden" name="filterAction"        value="${filterAction!}"/>
  <input type="hidden" name="filterOutcome"       value="${filterOutcome!}"/>
  <input type="hidden" name="filterSecretKeyRef"  value="${filterSecretKeyRef!}"/>
  <input type="hidden" name="filterProviderType"  value="${filterProviderType!}"/>
  <input type="hidden" name="filterDeploymentMode" value="${filterDeploymentMode!}"/>
  <table class="basic-table" cellspacing="0">
    <tr>
      <td class="label">${uiLabelMap.WebtoolsSecretAuditFilterFrom} <span class="alert">*</span></td>
      <td><input type="date" name="filterDateFrom" value="${filterDateFrom!}" required="required"/></td>
      <td class="label">${uiLabelMap.WebtoolsSecretAuditFilterTo} <span class="alert">*</span></td>
      <td><input type="date" name="filterDateTo" value="${filterDateTo!}" required="required"/></td>
      <td>
        <input type="submit" value="${uiLabelMap.WebtoolsSecretAuditExportButton}" class="smallSubmit"/>
      </td>
    </tr>
  </table>
  <p>${uiLabelMap.WebtoolsSecretAuditExportHint}</p>
</form>

<#if (listSize!0) gt 0>
  <p>
    ${uiLabelMap.WebtoolsSecretAuditShowing} ${lowIndex!0}&ndash;${highIndex!0} ${uiLabelMap.WebtoolsSecretAuditOf} ${listSize!0}
    &nbsp;|&nbsp;
    <#if (viewIndex > 0)>
      <a href="<@ofbizUrl>SecretAuditLog?viewIndex=${viewIndex - 1}&${filterParams}</@ofbizUrl>" class="buttontext">&laquo; ${uiLabelMap.CommonPrevious}</a>
      &nbsp;
    </#if>
    <#if (highIndex < listSize)>
      <a href="<@ofbizUrl>SecretAuditLog?viewIndex=${viewIndex + 1}&${filterParams}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonNext} &raquo;</a>
    </#if>
  </p>

  <table class="basic-table light-grid hover-bar" cellspacing="0">
    <thead>
      <tr class="header-row-2">
        <th>${uiLabelMap.WebtoolsSecretAuditTimestamp}</th>
        <th>${uiLabelMap.WebtoolsSecretAuditUser}</th>
        <th>${uiLabelMap.WebtoolsSecretAuditClientIp}</th>
        <th>${uiLabelMap.WebtoolsSecretAuditAction}</th>
        <th>${uiLabelMap.WebtoolsSecretAuditOutcome}</th>
        <th>${uiLabelMap.WebtoolsSecretAuditErrorCategory}</th>
        <th>${uiLabelMap.WebtoolsSecretAuditKey}</th>
        <th>${uiLabelMap.WebtoolsSecretAuditTarget}</th>
        <th>${uiLabelMap.WebtoolsSecretAuditAccessMode}</th>
        <th>${uiLabelMap.WebtoolsSecretAuditProvider}</th>
        <th>${uiLabelMap.WebtoolsSecretAuditDeployMode}</th>
        <th>${uiLabelMap.WebtoolsSecretAuditResourceId}</th>
        <th>${uiLabelMap.WebtoolsSecretAuditPropertyId}</th>
      </tr>
    </thead>
    <tbody>
      <#list auditRows as row>
        <tr<#if row?index % 2 == 1> class="alternate-row"</#if>>
          <td>${(row.auditTimestamp)!}</td>
          <td>${row.userLoginId!}</td>
          <td>${row.clientIpAddress!}</td>
          <td><strong>${row.action!}</strong></td>
          <td<#if row.outcome == "FAILURE" || row.outcome == "DENIED"> class="alert"</#if>>${row.outcome!}</td>
          <td>${row.errorCategory!}</td>
          <td><code>${row.secretKeyRef!}</code></td>
          <td>${row.secretTarget!}</td>
          <td>${row.accessMode!}</td>
          <td>${row.providerType!}</td>
          <td>${row.deploymentMode!}</td>
          <td>${row.systemResourceId!}</td>
          <td>${row.systemPropertyId!}</td>
        </tr>
      </#list>
    </tbody>
  </table>
<#else>
  <p>${uiLabelMap.WebtoolsSecretAuditNoRows}</p>
</#if>

<#-- Retention Settings panel (read-only unless SECRET_MAINT, which is handled server-side) -->
<hr/>
<h3>${uiLabelMap.WebtoolsSecretAuditRetentionTitle}</h3>
<table class="basic-table" cellspacing="0">
  <tbody>
    <tr>
      <td class="label">${uiLabelMap.WebtoolsSecretAuditRetentionDays}</td>
      <td>${retentionDays!365} ${uiLabelMap.WebtoolsSecretAuditRetentionDaysSuffix}</td>
    </tr>
    <tr>
      <td class="label">${uiLabelMap.WebtoolsSecretAuditRetentionBatch}</td>
      <td>${purgeBatchSize!500}</td>
    </tr>
    <tr>
      <td class="label">${uiLabelMap.WebtoolsSecretAuditRetentionNextRun}</td>
      <td>${nextPurgeRun!'(not scheduled or already ran)'}</td>
    </tr>
    <tr>
      <td class="label">${uiLabelMap.WebtoolsSecretAuditRetentionFetchEvents}</td>
      <td>${(fetchEventsEnabled!false)?string('ENABLED','DISABLED (default)')}</td>
    </tr>
    <tr>
      <td class="label">${uiLabelMap.WebtoolsSecretAuditRetentionCacheHits}</td>
      <td>${(cacheHitsEnabled!false)?string('ENABLED','DISABLED (default)')}</td>
    </tr>
    <tr>
      <td class="label">${uiLabelMap.WebtoolsSecretAuditDeployMode}</td>
      <td>${deploymentMode!'DIRECT'}</td>
    </tr>
  </tbody>
</table>
<#if hasSecretMaint!false>
<form method="post" action="<@ofbizUrl>updateSecretAuditRetention</@ofbizUrl>" class="basic-form">
  <table class="basic-table" cellspacing="0">
    <tbody>
      <tr>
        <td class="label">${uiLabelMap.WebtoolsSecretAuditRetentionDays}</td>
        <td>
          <input type="number" name="retentionDays" value="${retentionDays!365}" min="1"/>
          ${uiLabelMap.WebtoolsSecretAuditRetentionDaysSuffix}
        </td>
      </tr>
      <tr>
        <td class="label">${uiLabelMap.WebtoolsSecretAuditRetentionBatch}</td>
        <td><input type="number" name="batchSize" value="${purgeBatchSize!500}" min="1" max="10000"/></td>
      </tr>
      <tr>
        <td></td>
        <td><input type="submit" value="${uiLabelMap.WebtoolsSecretAuditRetentionSave}" class="smallSubmit"/></td>
      </tr>
    </tbody>
  </table>
</form>
<#else>
<p>${uiLabelMap.WebtoolsSecretAuditRetentionHint}</p>
</#if>
