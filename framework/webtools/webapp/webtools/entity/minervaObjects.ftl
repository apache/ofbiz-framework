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
<h1>Minerva Connection Objects</h1>
<br/>

<div class="button-bar">
    <a href="<@ofbizUrl>minervainfo</@ofbizUrl>" class="smallSubmit">Refresh</a>
</div>

<#assign groups = delegator.getModelGroupReader().getGroupNames(delegator.getDelegatorName())?if_exists/>
<table class="basic-table light-grid hover-bar" cellspacing="0">
    <tr class="header-row">
        <td>Helper Name</td>
        <td>Pool</td>
        <td>Thread</td>
        <td>In Tx</td>
        <td>Timeout</td>
        <td>Object</td>
        <td>Created</td>
        <td>Last Used</td>
        <td>In Use</td>
    </tr>
    <#assign alt_row = false>
    <#if (groups?has_content)>
        <#list groups as group>
            <#assign helper = delegator.getGroupHelperName(group)?if_exists/>
            <#if (helper?has_content)>
                <#assign pooledObjs = Static["org.ofbiz.entity.connection.MinervaConnectionFactory"].getPooledData(helper)?if_exists/>
                <#assign pool = Static["org.ofbiz.entity.connection.MinervaConnectionFactory"].getPoolName(helper)?if_exists/>
                <#if (pooledObjs?has_content)>
                    <#list pooledObjs as obj>
                        <#assign isTx = (obj.getCurrentXid()?has_content)/>
                        <#if ((obj.isInUse())?default(false))>
                            <#assign color = "red"/>
                        <#else>
                            <#assign color = "green"/>
                        </#if>
                        <tr<#if alt_row> class="alternate-row"</#if>>
                            <td>${helper}</td>
                            <td>${pool}</td>
                            <td>${(obj.getThread().getName())?default("n/a")}</td>
                            <td>${(isTx)?default("n/a")?string}</td>
                            <td>${(obj.getTransactionTimeout())?default(-1)?string}</td>
                            <td>${(obj.getObject().toString())?default("n/a")}</td>
                            <td>${obj.getCreationDate()?datetime?default("n/a")?string}</td>
                            <td>${obj.getLastUsedDate()?datetime?default("n/a")?string}</td>
                            <td><font color="${color}">${obj.isInUse()?default(false)?string}</font></td>
                        </tr>
                        <#assign alt_row = !alt_row>
                    </#list>
                </#if>
            </#if>
        </#list>
    <#else>
        <tr><td colspan="6">No helpers found.</td></tr>
    </#if>
</table>