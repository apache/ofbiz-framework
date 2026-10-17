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
<#if partyTaxAuthInfoAndDetailList??>
    <#list partyTaxAuthInfoAndDetailList as partyTaxAuthInfoAndDetail>
        <div>
            <a href="<@ofbizUrl>deleteCustomerTaxAuthInfo?partyId=${partyId}&amp;taxAuthPartyId=${partyTaxAuthInfoAndDetail.taxAuthPartyId}&amp;taxAuthGeoId=${partyTaxAuthInfoAndDetail.taxAuthGeoId}&amp;fromDate=${partyTaxAuthInfoAndDetail.fromDate}</@ofbizUrl>" class="buttontext">X</a>
            [${partyTaxAuthInfoAndDetail.geoCode}] ${partyTaxAuthInfoAndDetail.geoName} (${partyTaxAuthInfoAndDetail.groupName!}): ${uiLabelMap.PartyTaxId} [${partyTaxAuthInfoAndDetail.partyTaxId?default("N/A")}], ${uiLabelMap.PartyTaxIsExempt} [${partyTaxAuthInfoAndDetail.isExempt?default("N")}]
        </div>
    </#list>
    <div>
        <span>${uiLabelMap.PartyTaxAddInfo}:</span>
        <select name="taxAuthPartyGeoIds">
          <option></option>
          <#list taxAuthorityAndDetailList as taxAuthorityAndDetail>
            <option value="${taxAuthorityAndDetail.taxAuthPartyId}::${taxAuthorityAndDetail.taxAuthGeoId}">[${taxAuthorityAndDetail.geoCode}] ${taxAuthorityAndDetail.geoName} (${taxAuthorityAndDetail.groupName!})</option>
          </#list>
        </select>
        <span>${uiLabelMap.CommonId}: </span><input type="text" name="partyTaxId" size="12" maxlength="40"/>

        <#if "Y" == productStore.showTaxIsExempt?default("Y")>
        <label><span>${uiLabelMap.PartyTaxIsExempt} </span><input type="checkbox" name="isExempt" value="Y"/></label>
        <#else>
        <input type="hidden" name="isExempt" value="N"/>
        </#if>
    </div>
</#if>
