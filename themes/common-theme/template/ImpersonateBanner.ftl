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

<#if parameters.originUserLogin??>
    <a href="#impersonateContent" title="${uiLabelMap.CommonImpersonateTitle}" id="impersonateBtn"><img src="/images/img/impersonate-ico.png" alt="${uiLabelMap.CommonImpersonateTitle}"/></a>
    <div id="impersonateContent">
        <div class="impersonateModal">
            <a href="#" class="btn-close" title="${uiLabelMap.CommonClose}">×</a>
            <h3>${uiLabelMap.CommonImpersonateTitle}</h3>
            <p>${uiLabelMap.CommonImpersonateUserLogin} : <strong>${context.userLogin.userLoginId!}</strong></p>
            <a href="depersonateLogin" class="btn" title="${uiLabelMap.CommonImpersonateStop}">${uiLabelMap.CommonImpersonateStop}</a>
        </div>
    </div>
</#if>
