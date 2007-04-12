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
<#assign clientCerts = request.getAttribute("javax.servlet.request.X509Certificate")?if_exists/>
<#if (!clientCerts?has_content)>
    <#assign clientCerts = request.getAttribute("javax.net.ssl.peer_certificates")?if_exists/>
</#if>

<#if (isSecure)>
    <#if (clientCerts?has_content)>
        <#list clientCerts as cert>
            <#assign certString = Static["org.ofbiz.base.util.KeyStoreUtil"].certToString(cert)?if_exists>
            <#if (certString?has_content)>            
                <div style="width: 60%">
                    <div><b>Cert: ${cert.getType()} : ${cert.getSubjectX500Principal()}</b></div>
                    <div><b>Serial Number: ${cert.getSerialNumber().toString(16)}</b></div>

                    <textarea class="textBox" rows="4" cols="130">
${certString}

-----BEGIN PUBLIC KEY HEX-----
${Static["org.ofbiz.base.util.KeyStoreUtil"].pemToPkHex(certString)}
-----END PUBLIC KEY HEX-----

                    </textarea>
                </div>
                <br>
            </#if>
        </#list>
    <#else>
         <p>No client certifications found.</p>
    </#if>
<#else>
    <p>request can only obtain certifications when calls through SSL</p>
</#if>