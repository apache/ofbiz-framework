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

<#assign components = Static["org.ofbiz.base.component.ComponentConfig"].getAllComponents()?if_exists/>
<div id="stats-bins-history" class="screenlet">
  <div class="screenlet-title-bar">
    <h3>${uiLabelMap.WebtoolsComponentsLoaded}</h3>
  </div>
  <#if (components?has_content)>
    <table class="basic-table" cellspacing="0">
      <tr class="header-row">
        <td>${uiLabelMap.CommonName}</td>
        <td>${uiLabelMap.WebtoolsComponentsPath}</td>
        <td>${uiLabelMap.CommonEnabled}</td>
        <td colspan="3">${uiLabelMap.WebtoolsComponentsWebApps}</td>
      </tr>
      <#list components as component>
        <#assign webinfos = component.getWebappInfos()?if_exists/>
        <#assign firstRow = true>
        <tr>
          <td>${component.getComponentName()?if_exists}</td>
          <td>${component.getRootLocation()?if_exists}</td>
          <td>${component.enabled()?string?if_exists}</td>
          <#if (webinfos?has_content)>
            <#list webinfos as webinfo>
              <#if firstRow>
                <#assign firstRow = false>
              <#else>
                <tr>
                  <td>&nbsp;</td>
                  <td>&nbsp;</td>
                  <td>&nbsp;</td>
              </#if>
              <td>${webinfo.getName()?if_exists}</td>
              <td>${webinfo.getContextRoot()?if_exists}</td>
              <td>${webinfo.getLocation()?if_exists}</td>
              </tr>
            </#list>
          <#else>
              <td>&nbsp;</td>
              <td>&nbsp;</td>
              <td>&nbsp;</td>
            </tr>
          </#if>
      </#list>
    </table>
  <#else>
    <div class="screenlet-body">${uiLabelMap.WebtoolsComponentsNoComponents}.</div>
  </#if>
</div>
