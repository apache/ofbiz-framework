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
<#if (requestParameters.certString?has_content)>
    <#assign cert = Static["org.ofbiz.base.util.KeyStoreUtil"].pemToCert(requestParameters.certString)/>
</#if>
<br/>

<#if (cert?has_content)>
    <div><b>Cert: ${cert.getType()} : ${cert.getSubjectX500Principal()}</b></div>
    <div><b>Name: ${cert.getSubjectX500Principal().getName()}</b></div>
    <div><b>Serial Number: ${cert.getSerialNumber().toString(16)}</b></div>
<#else>
    <h3>Invalid certificate</h3>
</#if>

<br/>
<h1>Save to KeyStore</h1>
<table cellspacing="0" class="basic-table form-widget-table dark-grid">
  <tr class="header-row">
    <td>Component</td>
    <td>Keystore</td>
    <td>Import Issuer</td>
    <td>Key Alias</td>
    <td>&nbsp;</td>
  </tr>
  <#list components as component>
    <#assign keystores = component.getKeystoreInfos()?if_exists/>    
      <#list keystores as store>
        <#if (store.isTrustStore())>
          <tr>
            <form method="post" action="<@ofbizUrl>/importIssuerProvision</@ofbizUrl>">
              <input type="hidden" name="componentName" value="${component.getComponentName()}"/>
              <input type="hidden" name="keystoreName" value="${store.getName()}"/>
              <input type="hidden" name="certString" value="${requestParameters.certString}"/>

              <td>${component.getComponentName()}</td>
              <td>${store.getName()}</td>
              <td align="center"><input type="checkbox" name="importIssuer" value="Y"/>
              <td><input type="text" class="inputBox" name="alias" size="20"/>
              <td align="right"><input type="submit" value="Save"/>
            </form>
          </tr>
        </#if>
      </#list>
  </#list>
</table>