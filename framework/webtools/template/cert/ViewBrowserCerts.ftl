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

<#assign isSecure = request.isSecure()/>
<#assign clientCerts = request.getAttribute("javax.servlet.request.X509Certificate")!/>
<#if (!clientCerts?has_content)>
    <#assign clientCerts = request.getAttribute("javax.net.ssl.peer_certificates")!/>
</#if>

<div class="screenlet">
  <div class="screenlet-title-bar">
    <h3>${uiLabelMap.WebtoolsCertsX509}</h3>
  </div>
  <#if (isSecure)>
    <#if (clientCerts?has_content)>
      <table class="basic-table">
        <#list clientCerts as cert>
          <#assign certString = Static["org.apache.ofbiz.base.util.KeyStoreUtil"].certToString(cert)!>
          <#if (certString?has_content)>
            <tr>
              <td class="label">${uiLabelMap.WebtoolsCertsCert}</td>
              <td>${cert.getType()} ${cert.getSubjectX500Principal()}</td>
            </tr>
            <tr>
              <td class="label">${uiLabelMap.WebtoolsCertsSerialNum}:</td>
              <td>${cert.getSerialNumber().toString(16)}</td>
            </tr>
            <tr>
              <td>&nbsp;</td>
              <td>
                <textarea rows="4" cols="130">
${certString}

-----BEGIN PUBLIC KEY HEX-----
${Static["org.apache.ofbiz.base.util.KeyStoreUtil"].pemToPkHex(certString)}
-----END PUBLIC KEY HEX-----

                </textarea>
              </td>
            </tr>
          </#if>
        </#list>
      </table>
    <#else>
      <div class="screenlet-body">${uiLabelMap.WebtoolsCertsNotFound}.</div>
    </#if>
  <#else>
    <div class="screenlet-body">${uiLabelMap.WebtoolsCertsRequiresSSL}.</div>
  </#if>
</div>