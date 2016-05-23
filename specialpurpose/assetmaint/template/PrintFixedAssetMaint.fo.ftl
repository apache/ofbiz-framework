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
<#escape x as x?xml>
  <fo:flow flow-name="xsl-region-body">

  <#-- Print FixedAsset Information -->
  <fo:table table-layout="fixed" width="100%" border-style="solid" border-collapse="collapse" border-width="1pt">
    <fo:table-column column-width="20%"/>
    <fo:table-column column-width="80%"/>
    <fo:table-body>
      <fo:table-row>
        <fo:table-cell number-columns-spanned="2"><fo:block font-weight="bold">${uiLabelMap.AccountingFixedAsset}</fo:block></fo:table-cell>
      </fo:table-row>
      <#if fixedAsset?has_content>
        <fo:table-row>
          <fo:table-cell><fo:block>${uiLabelMap.AccountingFixedAssetId}</fo:block></fo:table-cell>
          <fo:table-cell><fo:block>${fixedAssetId}</fo:block></fo:table-cell>
        </fo:table-row>
        <#if fixedAsset.createdStamp?has_content>
        <fo:table-row>
           <fo:table-cell><fo:block>${uiLabelMap.CommonDate}</fo:block></fo:table-cell>
           <fo:table-cell><fo:block>${fixedAsset.createdStamp?string.short}</fo:block></fo:table-cell>
        </fo:table-row>
        </#if>
        <#if fixedAsset.fixedAssetName?has_content>
        <fo:table-row>
           <fo:table-cell><fo:block>${uiLabelMap.AccountingFixedAssetName}</fo:block></fo:table-cell>
           <fo:table-cell><fo:block>${fixedAsset.fixedAssetName!}</fo:block></fo:table-cell>
        </fo:table-row>
        </#if>
        <#if fixedAsset.serialNumber?has_content>
        <fo:table-row>
            <fo:table-cell><fo:block>${uiLabelMap.FormFieldTitle_serialNumber}</fo:block></fo:table-cell>
            <fo:table-cell><fo:block font-weight="bold">${fixedAsset.serialNumber!}</fo:block></fo:table-cell>
        </fo:table-row>
        </#if>
        <#if fixedAsset.locatedAtFacilityId?has_content>
        <fo:table-row>
            <fo:table-cell><fo:block>${uiLabelMap.FormFieldTitle_locatedAtFacilityId}</fo:block></fo:table-cell>
            <fo:table-cell><fo:block>${maintenance.facilityName!}</fo:block></fo:table-cell>
        </fo:table-row>
        </#if>
        <#if fixedAssetIdentValue?has_content>
        <fo:table-row>
            <fo:table-cell><fo:block>${uiLabelMap.AccountingFixedAssetIdents}</fo:block></fo:table-cell>
            <fo:table-cell><fo:block>${fixedAssetIdentValue!}</fo:block></fo:table-cell>
        </fo:table-row>
        </#if>
      </#if>
    </fo:table-body>
  </fo:table>
  <#-- End Print FixedAsset Information -->

  <#-- Start Print FixedAsset Maintenance Information -->
  <fo:table table-layout="fixed" width="100%" border-style="solid" border-collapse="collapse" border-width="1pt">
    <fo:table-column column-width="20%"/>
    <fo:table-column column-width="80%"/>
    <fo:table-body>
      <fo:table-row>
        <fo:table-cell number-columns-spanned="2"><fo:block font-weight="bold">${uiLabelMap.AccountingFixedAssetMaint}</fo:block></fo:table-cell>
      </fo:table-row>
      <#if fixedAssetMaint?has_content>
        <fo:table-row>
          <fo:table-cell><fo:block>${uiLabelMap.AccountingFixedAssetMaintSeqId}</fo:block></fo:table-cell>
          <fo:table-cell><fo:block>${fixedAssetMaint.maintHistSeqId}</fo:block></fo:table-cell>
        </fo:table-row>
        <fo:table-row>
          <fo:table-cell><fo:block>${uiLabelMap.CommonStatus}</fo:block></fo:table-cell>
          <fo:table-cell><fo:block>${statusItemDesc}</fo:block></fo:table-cell>
        </fo:table-row>
        <#if productMaintName?has_content>
        <fo:table-row>
          <fo:table-cell><fo:block>${uiLabelMap.CommonName}</fo:block></fo:table-cell>
          <fo:table-cell><fo:block>${productMaintName!}</fo:block></fo:table-cell>
        </fo:table-row>
        </#if>
        <#if productMaintTypeDesc?has_content>
        <fo:table-row>
          <fo:table-cell><fo:block>${uiLabelMap.AccountingFixedAssetMaintType}</fo:block></fo:table-cell>
          <fo:table-cell><fo:block font-weight="bold">${productMaintTypeDesc!}</fo:block></fo:table-cell>
        </fo:table-row>
        </#if>
        <fo:table-row>
          <fo:table-cell><fo:block>${uiLabelMap.AccountingFixedAssetMaintIntervalQuantity}</fo:block></fo:table-cell>
          <fo:table-cell><fo:block font-weight="bold">${fixedAssetMaint.intervalQuantity!} ${intervalUomDesc!}</fo:block></fo:table-cell>
        </fo:table-row>
        <#if productMeterTypeDesc?has_content>
        <fo:table-row>
          <fo:table-cell><fo:block>Meter type</fo:block></fo:table-cell>
          <fo:table-cell><fo:block font-weight="bold">${productMeterTypeDesc!}</fo:block></fo:table-cell>
        </fo:table-row>
        </#if>
      </#if>
    </fo:table-body>
  </fo:table>
<#-- End Print FixedAsset Maintenance Information -->

<#-- Start Print Maintenance Schedule Information -->
  <fo:table table-layout="fixed" width="100%" border-style="solid" border-collapse="collapse" border-width="1pt">
    <fo:table-column column-width="25%"/>
    <fo:table-column column-width="25%"/>
    <fo:table-column column-width="25%"/>
    <fo:table-column column-width="25%"/>
    <fo:table-body>
      <fo:table-row>
        <fo:table-cell number-columns-spanned="4" ><fo:block font-weight="bold">${uiLabelMap.WorkEffortSummary}</fo:block></fo:table-cell>
      </fo:table-row>
      <#if scheduleWorkEffort?has_content>
        <fo:table-row>
          <fo:table-cell><fo:block>${uiLabelMap.FormFieldTitle_actualStartDate}</fo:block></fo:table-cell>
            <#if scheduleWorkEffort.actualStartDate?has_content>
              <#assign actualStartDate = scheduleWorkEffort.get("actualStartDate")>
                <fo:table-cell><fo:block>${actualStartDate?string.short}</fo:block></fo:table-cell>
            <#else>
                <fo:table-cell><fo:block></fo:block></fo:table-cell>
            </#if>
          <fo:table-cell><fo:block>${uiLabelMap.FormFieldTitle_actualCompletionDate}</fo:block></fo:table-cell>
            <#if scheduleWorkEffort.actualCompletionDate?has_content>
              <#assign actualCompletionDate = scheduleWorkEffort.get("actualCompletionDate")>
              <fo:table-cell><fo:block>${actualCompletionDate?string.short}</fo:block></fo:table-cell>
            <#else>
                <fo:table-cell><fo:block></fo:block></fo:table-cell>
            </#if>
        </fo:table-row>
        <#assign workEffortPurposeType = scheduleWorkEffort.getRelatedOne("WorkEffortPurposeType", false)!>
        <#if workEffortPurposeType?has_content>
        <fo:table-row>
          <fo:table-cell><fo:block>${uiLabelMap.FormFieldTitle_workEffortPurposeTypeId}</fo:block></fo:table-cell>
          <fo:table-cell number-columns-spanned="3"><fo:block>${workEffortPurposeType.workEffortPurposeTypeId}--${workEffortPurposeType.description}</fo:block></fo:table-cell>
        </fo:table-row>
        </#if>
        <fo:table-row>
          <fo:table-cell><fo:block>${uiLabelMap.CommonName}</fo:block></fo:table-cell>
          <fo:table-cell number-columns-spanned="3"><fo:block font-weight="bold">${scheduleWorkEffort.workEffortName!}</fo:block></fo:table-cell>
        </fo:table-row>
        <fo:table-row>
          <fo:table-cell><fo:block>${uiLabelMap.CommonDescription}</fo:block></fo:table-cell>
          <fo:table-cell number-columns-spanned="3"><fo:block font-weight="bold">${scheduleWorkEffort.description!}</fo:block></fo:table-cell>
        </fo:table-row>
      </#if>
    </fo:table-body>
  </fo:table>
<#-- End Print Maintenance Schedule Information -->

  <#if itemIssuanceList?has_content>
    <fo:table table-layout="fixed" width="100%" border-style="solid" border-collapse="collapse" border-width="1pt">
      <fo:table-column column-width="20%"/>
      <fo:table-column column-width="80%"/>
      <fo:table-header>
        <fo:table-row>
          <fo:table-cell><fo:block font-weight="bold">${uiLabelMap.AssetMaintItemIssuance}</fo:block></fo:table-cell>
          <fo:table-cell text-align="center"><fo:block font-weight="bold">${uiLabelMap.OrderQuantity}</fo:block></fo:table-cell>
        </fo:table-row>
      </fo:table-header>
      <fo:table-body>
        <#list itemIssuanceList as itemIssuance>
          <#assign productId = itemIssuance.productId!>
          <#assign quantity = itemIssuance.quantity?default(0)>
            <fo:table-row>
              <fo:table-cell>
                <fo:block>
                  <#if productId??>
                    ${itemIssuance.productId?default("N/A")} - ${itemIssuance.internalName!} - ${itemIssuance.description!} - ${itemIssuance.comments!}
                  </#if>
                </fo:block>
              </fo:table-cell>
              <fo:table-cell text-align="center"><fo:block>${quantity}</fo:block></fo:table-cell>
            </fo:table-row>
        </#list>
      </fo:table-body>
    </fo:table>
  </#if>

  <#if notes?has_content>
    <fo:table table-layout="fixed" width="100%" border-style="solid" border-collapse="collapse" border-width="1pt">
      <fo:table-column column-width="100%"/>
      <fo:table-header>
        <fo:table-row>
          <fo:table-cell><fo:block font-weight="bold">${uiLabelMap.WorkEffortNotes}</fo:block></fo:table-cell>
        </fo:table-row>
      </fo:table-header>
      <fo:table-body>
        <#list notes as note>
          <fo:table-row>
            <fo:table-cell>
              <fo:block>Author : ${Static["org.ofbiz.party.party.PartyHelper"].getPartyName(delegator, note.noteParty, true)}</fo:block>
              <fo:block>Date : ${note.noteDateTime?string.short}</fo:block>
              <#escape x as x?html>
                <fo:block>${note.noteInfo!}</fo:block>
              </#escape>
            </fo:table-cell>
          </fo:table-row>
        </#list>
      </fo:table-body>
    </fo:table>
  </#if>

  </fo:flow>
</#escape>


