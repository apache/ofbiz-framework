/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ofbiz.webtools.secret

import org.apache.ofbiz.base.secret.SecretValueResolver
import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.entity.util.EntityUtilProperties

import java.sql.Timestamp

// --- pagination ---
viewIndex = (parameters.viewIndex ?: 0) as int
viewSize  = (parameters.viewSize  ?: 50) as int
if (viewIndex < 0) {
    viewIndex = 0
}
if (viewSize < 1) {
    viewSize = 50
}

// --- filter parameters ---
filterUser         = parameters.filterUserLoginId    ?: ''
filterAction       = parameters.filterAction         ?: ''
filterOutcome      = parameters.filterOutcome        ?: ''
filterKey          = parameters.filterSecretKeyRef   ?: ''
filterProviderType = parameters.filterProviderType   ?: ''
filterDeployMode   = parameters.filterDeploymentMode ?: ''
filterFrom         = parameters.filterDateFrom       ?: ''
filterTo           = parameters.filterDateTo         ?: ''

// --- build conditions ---
conditions = []
if (filterUser) {
    conditions << EntityCondition.makeCondition('userLoginId', EntityOperator.EQUALS, filterUser)
}
if (filterAction) {
    conditions << EntityCondition.makeCondition('action', EntityOperator.EQUALS, filterAction)
}
if (filterOutcome) {
    conditions << EntityCondition.makeCondition('outcome', EntityOperator.EQUALS, filterOutcome)
}
if (filterKey) {
    conditions << EntityCondition.makeCondition('secretKeyRef', EntityOperator.EQUALS, filterKey)
}
if (filterProviderType) {
    conditions << EntityCondition.makeCondition('providerType', EntityOperator.EQUALS, filterProviderType)
}
if (filterDeployMode) {
    conditions << EntityCondition.makeCondition('deploymentMode', EntityOperator.EQUALS, filterDeployMode)
}
if (filterFrom) {
    try {
        conditions << EntityCondition.makeCondition('auditTimestamp',
                EntityOperator.GREATER_THAN_EQUAL_TO, Timestamp.valueOf(filterFrom + ' 00:00:00'))
    } catch (IllegalArgumentException ignore) { }
}
if (filterTo) {
    try {
        conditions << EntityCondition.makeCondition('auditTimestamp',
                EntityOperator.LESS_THAN_EQUAL_TO, Timestamp.valueOf(filterTo + ' 23:59:59'))
    } catch (IllegalArgumentException ignore) { }
}

// --- query ---
where = conditions ? EntityCondition.makeCondition(conditions, EntityOperator.AND) : null

// Returns a fresh, optionally-filtered query each call — avoids passing null to where() in Groovy
Closure filtered = { where ? from('SecretAuditLog').where(where) : from('SecretAuditLog') }

listSize  = filtered().queryCount()
auditRows = filtered().orderBy('-auditTimestamp').offset(viewIndex * viewSize).limit(viewSize).queryList()

highIndex = Math.min((viewIndex + 1) * viewSize, (int) listSize)

// --- URL-encoded filter params for pagination links ---
Closure enc = { String v -> URLEncoder.encode(v ?: '', 'UTF-8') }
filterParams = "filterUserLoginId=${enc(filterUser as String)}" +
        "&filterAction=${enc(filterAction as String)}" +
        "&filterOutcome=${enc(filterOutcome as String)}" +
        "&filterSecretKeyRef=${enc(filterKey as String)}" +
        "&filterProviderType=${enc(filterProviderType as String)}" +
        "&filterDeploymentMode=${enc(filterDeployMode as String)}" +
        "&filterDateFrom=${enc(filterFrom as String)}" +
        "&filterDateTo=${enc(filterTo as String)}" +
        "&viewSize=${viewSize}"

// --- populate context ---
context.putAll([
        auditRows: auditRows,
        listSize: listSize,
        viewIndex: viewIndex,
        viewSize: viewSize,
        lowIndex: listSize > 0 ? viewIndex * viewSize + 1 : 0,
        highIndex: highIndex,
        filterUserLoginId: filterUser,
        filterAction: filterAction,
        filterOutcome: filterOutcome,
        filterSecretKeyRef: filterKey,
        filterProviderType: filterProviderType,
        filterDeploymentMode: filterDeployMode,
        filterDateFrom: filterFrom,
        filterDateTo: filterTo,
        filterParams: filterParams
])

// --- K8s mode notice ---
deploymentMode         = UtilProperties.getPropertyValue('security', 'secret.audit.deployment.mode', 'DIRECT')
context.deploymentMode = deploymentMode
context.isK8sMode      = deploymentMode == 'K8S_INJECTED'

// --- retention settings ---
context.retentionDays  = EntityUtilProperties.getPropertyValue('security', 'secret.audit.retention.days',   delegator) ?: '365'
context.purgeBatchSize = EntityUtilProperties.getPropertyValue('security', 'secret.audit.purge.batch.size', delegator) ?: '500'
context.hasSecretMaint = security.hasPermission('SECRET_MAINT', session)

// --- next scheduled purge run ---
nextJobRun           = from('JobSandbox').where('jobId', 'SECRET_AUDIT_PURGE').queryOne()
context.nextPurgeRun = nextJobRun?.getTimestamp('runTime')

// --- audit phase-2 flags ---
context.fetchEventsEnabled = SecretValueResolver.LOG_FETCH_EVENTS
context.cacheHitsEnabled   = SecretValueResolver.LOG_CACHE_HITS
