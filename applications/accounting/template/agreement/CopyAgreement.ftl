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
<#if agreement??>
<div class="screenlet">
  <div class="screenlet-title-bar">
    <ul>
      <li class="h3">${uiLabelMap.PageTitleCopyAgreement}</li>
    </ul>
    <br class="clear"/>
  </div>
  <div class="screenlet-body">
    <form action="<@ofbizUrl>copyAgreement</@ofbizUrl>" method="post">
        <input type="hidden" name="agreementId" value="${agreementId}"/>
        <div>
            <span class="label">${uiLabelMap.AccountingCopyAgreement}</span>
            ${uiLabelMap.AccountingAgreementTerms}&nbsp;<input type="checkbox" name="copyAgreementTerms" value="Y" checked="checked" />
            ${uiLabelMap.ProductProducts}&nbsp;<input type="checkbox" name="copyAgreementProducts" value="Y" checked="checked" />
            ${uiLabelMap.Party}&nbsp;<input type="checkbox" name="copyAgreementParties" value="Y" checked="checked" />
            ${uiLabelMap.ProductFacilities}&nbsp;<input type="checkbox" name="copyAgreementFacilities" value="Y" checked="checked" />
        </div>
        <div class="button-bar">
            <input type="submit" value="${uiLabelMap.CommonCopy}"/>
        </div>
    </form>
  </div>
</div>
</#if>