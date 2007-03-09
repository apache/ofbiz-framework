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

<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
  <fo:layout-master-set>
    <fo:simple-page-master master-name="simple-portrait"
      page-width="8.5in" page-height="11in"
      margin-top="0.3in" margin-bottom="0.3in"
      margin-left="0.4in" margin-right="0.3in">
    <fo:region-body margin-top="1in" margin-bottom="0.5in"/>
    <fo:region-before extent="1in"/>
    <fo:region-after extent="0.5in" />
    </fo:simple-page-master>
  </fo:layout-master-set>

  <fo:page-sequence master-reference="simple-portrait" font-size="12pt">
  <fo:flow flow-name="xsl-region-body">
  
  <#-- Print FixedAsset Information -->
  <fo:table table-layout="fixed" border-style="solid" border-width="1pt">
    <fo:table-column column-width="1.5in"/>
    <fo:table-column column-width="6.5in"/>
    <fo:table-body>
      <fo:table-row>
        <fo:table-cell><fo:block number-columns-spanned="2"  font-weight="bold">${uiLabelMap.AccountingFixedAsset}</fo:block></fo:table-cell>
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
           <fo:table-cell><fo:block>${fixedAsset.fixedAssetName?if_exists}</fo:block></fo:table-cell>
        </fo:table-row>
        </#if>
        <#if fixedAsset.serialNumber?has_content>
        <fo:table-row>
            <fo:table-cell><fo:block>${uiLabelMap.FormFieldTitle_serialNumber}</fo:block></fo:table-cell>
            <fo:table-cell><fo:block font-weight="bold">${fixedAsset.serialNumber?if_exists}</fo:block></fo:table-cell>
        </fo:table-row>
        </#if>
        <#if fixedAsset.locatedAtFacilityId?has_content>
        <fo:table-row>  
            <fo:table-cell><fo:block>${uiLabelMap.FormFieldTitle_locatedAtFacilityId}</fo:block></fo:table-cell>
            <fo:table-cell><fo:block>${maintenance.facilityName?if_exists}</fo:block></fo:table-cell>
        </fo:table-row>
        </#if>
        <#if fixedAssetIdentValue?has_content>
        <fo:table-row>
            <fo:table-cell><fo:block>${uiLabelMap.AccountingFixedAssetIdents}</fo:block></fo:table-cell>
            <fo:table-cell><fo:block>${fixedAssetIdentValue?if_exists}</fo:block></fo:table-cell>
        </fo:table-row>
        </#if>
      </#if>
    </fo:table-body>
  </fo:table>
  <#-- End Print FixedAsset Information -->
  
  <#-- Start Print FixedAsset Maintenance Information -->
  <fo:table table-layout="fixed"  border-style="solid" border-width="1pt">
    <fo:table-column column-width="1.5in"/>
    <fo:table-column column-width="6.5in"/>
    <fo:table-body>
      <fo:table-row>
        <fo:table-cell><fo:block number-columns-spanned="2" font-weight="bold">${uiLabelMap.AccountingFixedAssetMaint}</fo:block></fo:table-cell>
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
          <fo:table-cell><fo:block>${productMaintName?if_exists}</fo:block></fo:table-cell>
        </fo:table-row>
        </#if>
        <#if productMaintTypeDesc?has_content>
        <fo:table-row>
          <fo:table-cell><fo:block>${uiLabelMap.AccountingFixedAssetMaintType}</fo:block></fo:table-cell>
          <fo:table-cell><fo:block font-weight="bold">${productMaintTypeDesc?if_exists}</fo:block></fo:table-cell>
        </fo:table-row>
        </#if>
        <fo:table-row>
          <fo:table-cell><fo:block>${uiLabelMap.AccountingFixedAssetMaintIntervalQuantity}</fo:block></fo:table-cell>
          <fo:table-cell><fo:block font-weight="bold">${fixedAssetMaint.intervalQuantity?if_exists} ${intervalUomDesc?if_exists}</fo:block></fo:table-cell>
        </fo:table-row>
        <#if productMeterTypeDesc?has_content>
        <fo:table-row>
          <fo:table-cell><fo:block>Meter type</fo:block></fo:table-cell>
          <fo:table-cell><fo:block font-weight="bold">${productMeterTypeDesc?if_exists}</fo:block></fo:table-cell>
        </fo:table-row>
        </#if>
      </#if>
    </fo:table-body>
  </fo:table>
<#-- End Print FixedAsset Maintenance Information -->
  
<#-- Start Print Maintenance Schedule Information -->
  <fo:table table-layout="fixed"  border-style="solid" border-width="1pt">
    <fo:table-column column-width="2in"/>
    <fo:table-column column-width="2in"/>
    <fo:table-column column-width="2in"/>
    <fo:table-column column-width="2in"/>
    <fo:table-body>
      <fo:table-row>
        <fo:table-cell number-columns-spanned="4" ><fo:block number-columns-spanned="4" font-weight="bold">${uiLabelMap.WorkEffortSummary}</fo:block></fo:table-cell>
      </fo:table-row>
      <#if scheduleWorkEffort?has_content>
        <fo:table-row>
          <fo:table-cell><fo:block>${uiLabelMap.WorkEffortActualStartDate}</fo:block></fo:table-cell>
            <#if scheduleWorkEffort.actualStartDate?has_content>
              <#assign actualStartDate = scheduleWorkEffort.get("actualStartDate")>
                <fo:table-cell><fo:block>${actualStartDate?string.short}</fo:block></fo:table-cell>
            </#if>
          <fo:table-cell><fo:block>${uiLabelMap.WorkEffortActualCompletionDate}</fo:block></fo:table-cell>
            <#if scheduleWorkEffort.actualCompletionDate?has_content>
              <#assign actualCompletionDate = scheduleWorkEffort.get("actualCompletionDate")>
              <fo:table-cell><fo:block>${actualCompletionDate?string.short}</fo:block></fo:table-cell>
            </#if>
        </fo:table-row>
        <#assign workEffortPurposeType = scheduleWorkEffort.getRelatedOne("WorkEffortPurposeType")?if_exists>
        <#if workEffortPurposeType?has_content>
        <fo:table-row>
          <fo:table-cell><fo:block>${uiLabelMap.WorkEffortWorkEffortPurposeTypeId}</fo:block></fo:table-cell>
          <fo:table-cell number-columns-spanned="3"><fo:block>${workEffortPurposeType.workEffortPurposeTypeId}--${workEffortPurposeType.description}</fo:block></fo:table-cell>
        </fo:table-row>
        </#if>
        <fo:table-row>
          <fo:table-cell><fo:block>${uiLabelMap.CommonName}</fo:block></fo:table-cell>
          <fo:table-cell number-columns-spanned="3"><fo:block font-weight="bold">${scheduleWorkEffort.workEffortName?if_exists}</fo:block></fo:table-cell>
        </fo:table-row>
        <fo:table-row>
          <fo:table-cell><fo:block>${uiLabelMap.CommonDescription}</fo:block></fo:table-cell>
          <fo:table-cell number-columns-spanned="3"><fo:block font-weight="bold">${scheduleWorkEffort.description?if_exists}</fo:block></fo:table-cell>
        </fo:table-row>
      </#if>
    </fo:table-body>
  </fo:table>
<#-- End Print Maintenance Schedule Information -->

  <fo:table table-layout="fixed"  border-spacing="3pt" border-style="solid" border-width="1pt">
    <fo:table-column column-width="6.5in"/>
    <fo:table-column column-width="1.5in"/>
    <fo:table-header>
      <fo:table-row>
        <fo:table-cell><fo:block font-weight="bold">${uiLabelMap.FixedAssetMaintItemIssuance}</fo:block></fo:table-cell>
        <fo:table-cell text-align="center"><fo:block font-weight="bold">${uiLabelMap.OrderQuantity}</fo:block></fo:table-cell>
      </fo:table-row>        
    </fo:table-header>
    <fo:table-body>
      <#if itemIssuanceList?has_content>
        <#list itemIssuanceList as itemIssuance>
          <#assign productId = itemIssuance.productId?if_exists>
          <#assign quantity = itemIssuance.quantity?default(0)>
            <fo:table-row>
              <fo:table-cell>
                <fo:block>
                  <#if productId?exists>
                    ${itemIssuance.productId?default("N/A")} - ${itemIssuance.internalName?if_exists} - ${itemIssuance.description?if_exists} - ${itemIssuance.comments?if_exists}
                  </#if>
                </fo:block>
              </fo:table-cell>
              <fo:table-cell text-align="center"><fo:block>${quantity}</fo:block></fo:table-cell>            
            </fo:table-row>
        </#list>
      </#if>
    </fo:table-body>
  </fo:table>

  <fo:table table-layout="fixed"   border-spacing="3pt" border-style="solid" border-width="1pt">
    <fo:table-column column-width="8in"/>
    <fo:table-header>
      <fo:table-row>
        <fo:table-cell><fo:block font-weight="bold">${uiLabelMap.WorkEffortNotes}</fo:block></fo:table-cell>
      </fo:table-row>        
    </fo:table-header>
    <fo:table-body>
      <#if notes?has_content>
        <#list notes as note>
          <fo:table-row>
            <fo:table-cell>
              <fo:block>Auther : ${Static["org.ofbiz.party.party.PartyHelper"].getPartyName(delegator, note.noteParty, true)}</fo:block>
              <fo:block>Date : ${note.noteDateTime?string.short}</fo:block>
              <#escape x as x?html>
                <fo:block>${note.noteInfo?if_exists}</fo:block>
              </#escape> 
            </fo:table-cell>
          </fo:table-row>
        </#list>
      </#if>
    </fo:table-body>
  </fo:table>

  </fo:flow>
  </fo:page-sequence>
</fo:root>

