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

<#if assetDepreciationTillDate?has_content && assetNBVAfterDepreciation?has_content>
  <br/><br/>
  <#assign listSize = assetDepreciationTillDate.size()>
  <table width="50%" cellspacing="0" border="1">
    <tr>
      <th>
        Sequence No.
      </th>
      <th>
        Depreciation
      </th>
      <th>
        Sum of Depreciation
      </th>
      <th>
        Net Book Value
      </th>
    </tr>
    <tr>
      <td>
        <#list 1.. listSize as i>
          ${i} <br/>
        </#list>
      </td>
      <td>
        <#list assetDepreciationTillDate as assetDepreciation>
          ${assetDepreciation} <br/>
        </#list>
      </td>
      <td>
        <#assign i = 0/>
        <#assign cumulativeDep = 0/>
        <#list assetDepreciationTillDate as assetDepreciation>
          <#if i <= listSize>
            <#assign cumulativeDep = cumulativeDep + assetDepreciation>
            ${cumulativeDep} <br/>
            <#assign i = i + 1/>
          </#if>
          <#assign cumulativeDep = cumulativeDep>
        </#list>
      </td>
      <td>
        <#list assetNBVAfterDepreciation as assetNBVDepreciation>
          ${assetNBVDepreciation}<br/>
        </#list>
      </td>
    </tr>
  </table>
</#if>