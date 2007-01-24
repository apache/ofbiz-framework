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
<#if agreement?exists>
<form action="<@ofbizUrl>copyAgreement</@ofbizUrl>" method="post" style="margin: 0;">
    <input type="hidden" name="agreementId" value="${agreementId}"/>
    <div class="tabletext">
        <b>${uiLabelMap.AccountingCopyAgreement}:</b>
        ${uiLabelMap.AccountingAgreementTerms}&nbsp;<input type="checkbox" class="checkBox" name="copyAgreementTerms" value="Y" checked/>
        ${uiLabelMap.ProductProducts}&nbsp;<input type="checkbox" class="checkBox" name="copyAgreementProducts" value="Y" checked/>
        ${uiLabelMap.Party}&nbsp;<input type="checkbox" class="checkBox" name="copyAgreementParties" value="Y" checked/>
    </div>
    <input type="submit" class="smallSubmit" value="${uiLabelMap.CommonCopy}"/>
</form>
</#if>
