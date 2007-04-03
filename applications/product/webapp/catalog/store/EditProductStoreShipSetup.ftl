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
<script language="JavaScript" type="text/javascript">
<!--
function setAssocFields(select) {
    var index = select.selectedIndex;
    var opt = select.options[index];
    var optStr = opt.value;
    var optLen = optStr.length;

    var shipmentMethodTypeId = "";
    var sequenceNumber = "";
    var roleTypeId = "";
    var partyId = "";
    var delIdx = 1;

    for (i=0; i<optLen; i++) {
        if (optStr.charAt(i) == '|') {
            delIdx++;
        } else {
            if (delIdx == 1) {
                partyId = partyId + optStr.charAt(i);
            } else if (delIdx == 2) {
                roleTypeId = roleTypeId + optStr.charAt(i);
            } else if (delIdx == 3) {
                shipmentMethodTypeId = shipmentMethodTypeId + optStr.charAt(i);
            } else if (delIdx == 4) {
                sequenceNumber = sequenceNumber + optStr.charAt(i);
            }
        }
    }

    document.addscarr.roleTypeId.value = roleTypeId;
    document.addscarr.partyId.value = partyId;
    document.addscarr.shipmentMethodTypeId.value = shipmentMethodTypeId;
    document.addscarr.sequenceNumber.value = sequenceNumber;
}
// -->
</script>

<#-- Shipping Setup From Catalog->Store->Shipping-->

<#-- New Shippment Methods-->
    <div class="head2">${uiLabelMap.ProductStoreShipmentMethodAssociations}</div>
    <table border="1" cellpadding="2" cellspacing="0">
      <tr>
        <td><span class="tableheadtext">${uiLabelMap.ProductShipmentMethodType}</span></td>
        <td><span class="tableheadtext">${uiLabelMap.PartyParty}</span></td>
        <td><span class="tableheadtext">${uiLabelMap.ProductMinSz}</span></td>
        <td><span class="tableheadtext">${uiLabelMap.ProductMaxSz}</span></td>
        <td><span class="tableheadtext">${uiLabelMap.ProductMinWt}</span></td>
        <td><span class="tableheadtext">${uiLabelMap.ProductMaxWt}</span></td>
        <td><span class="tableheadtext">${uiLabelMap.ProductMinTot}</span></td>
        <td><span class="tableheadtext">${uiLabelMap.ProductMaxTot}</span></td>
        <td><span class="tableheadtext">${uiLabelMap.ProductAllowUSPS}</span></td>
        <td><span class="tableheadtext">${uiLabelMap.ProductRequireUSPS}</span></td>
        <td><span class="tableheadtext">${uiLabelMap.ProductAllowCo}</span></td>
        <td><span class="tableheadtext">${uiLabelMap.ProductRequireCo}</span></td>
        <td><span class="tableheadtext">${uiLabelMap.ProductIncFreeship}</span></td>
        <td><span class="tableheadtext">${uiLabelMap.ProductIncGeo}</span></td>
        <td><span class="tableheadtext">${uiLabelMap.ProductExcGeo}</span></td>
        <td><span class="tableheadtext">${uiLabelMap.ProductIncFeature}</span></td>
        <td><span class="tableheadtext">${uiLabelMap.ProductExcFeature}</span></td>
        <td><span class="tableheadtext">${uiLabelMap.ProductSequence}</span></td>
        <td>&nbsp;</td>
      </tr>
      <#if storeShipMethods?has_content>
        <#assign idx = 0>
        <#list storeShipMethods as meth>
          <#assign idx = idx + 1>
          <form name="methUpdate${idx}" method="post" action="<@ofbizUrl>storeUpdateShipMeth</@ofbizUrl>">
            <input type="hidden" name="productStoreShipMethId" value="${meth.productStoreShipMethId}">
            <input type="hidden" name="shipmentMethodTypeId" value="${meth.shipmentMethodTypeId}">
            <input type="hidden" name="partyId" value="${meth.partyId}">
            <input type="hidden" name="roleTypeId" value="${meth.roleTypeId}">
            <input type="hidden" name="productStoreId" value="${meth.productStoreId}">
            <input type="hidden" name="viewProductStoreId" value="${productStoreId}">
            <input type="hidden" name="newShipMethod" value="Y">
            <tr>
              <td><span class="tabletext">${meth.description}</span></td>
              <td><span class="tabletext">${meth.partyId}</span></td>
              <td><span class="tabletext">${meth.minSize?if_exists}</span></td>
              <td><span class="tabletext">${meth.maxSize?if_exists}</span></td>
              <td><span class="tabletext">${meth.minWeight?if_exists}</span></td>
              <td><span class="tabletext">${meth.maxWeight?if_exists}</span></td>
              <td><span class="tabletext">${meth.minTotal?default(0)?string("##0.00")}</span></td>
              <td><span class="tabletext">${meth.maxTotal?default(0)?string("##0.00")}</span></td>
              <td><span class="tabletext">${meth.allowUspsAddr?default("N")}</span></td>
              <td><span class="tabletext">${meth.requireUspsAddr?default("N")}</span></td>
              <td><span class="tabletext">${meth.allowCompanyAddr?default("N")}</span></td>
              <td><span class="tabletext">${meth.requireCompanyAddr?default("N")}</span></td>
              <td><span class="tabletext">${meth.includeNoChargeItems?default("Y")}</span></td>
              <td><span class="tabletext">${meth.includeGeoId?if_exists}</span></td>
              <td><span class="tabletext">${meth.excludeGeoId?if_exists}</span></td>
              <td><span class="tabletext">${meth.includeFeatureGroup?if_exists}</span></td>
              <td><span class="tabletext">${meth.excludeFeatureGroup?if_exists}</span></td>
              <td><input type="text" size="5" class="inputBox" name="sequenceNumber" value="${meth.sequenceNumber?if_exists}"></td>
              <td width='1' align="right">
                <span style="white-space: nowrap;">
                  <a href="javascript:document.methUpdate${idx}.submit();" class="buttontext">[${uiLabelMap.CommonUpdate}]</a>
                  <a href="<@ofbizUrl>storeRemoveShipMeth?viewProductStoreId=${productStoreId}&amp;productStoreId=${meth.productStoreId}&amp;newShipMethod=Y&amp;productStoreShipMethId=${meth.productStoreShipMethId}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonRemove}]</a>
                </span>
              </td>
            </tr>
          </form>
        </#list>
      </#if>
    </table>
    <br/>
    <table cellspacing="2" cellpadding="2">
      <form name="addscarr" method="post" action="<@ofbizUrl>storeCreateShipMeth</@ofbizUrl>">
        <input type="hidden" name="viewProductStoreId" value="${productStoreId}">
        <input type="hidden" name="newShipMethod" value="Y">
        <input type="hidden" name="productStoreId" value="${productStoreId}">
        <input type="hidden" name="shipmentMethodTypeId">
        <input type="hidden" name="roleTypeId">
        <input type="hidden" name="partyId">
        <tr>
          <td align="right"><span class="tableheadtext">${uiLabelMap.ProductCarrierShipmentMethod}</span></td>
          <td>
            <select class="selectBox" name="carrierShipmentString" onchange="javascript:setAssocFields(this);">
              <option>${uiLabelMap.ProductSelectOne}</option>
              <#list shipmentMethods as shipmentMethod>
                <option value="${shipmentMethod.partyId}|${shipmentMethod.roleTypeId}|${shipmentMethod.shipmentMethodTypeId}|${shipmentMethod.sequenceNumber?default(1)}">${shipmentMethod.description} (${shipmentMethod.partyId}/${shipmentMethod.roleTypeId})</option>
              </#list>
            </select> *
          </td>
        </tr>
        <tr>
          <td align="right"><span class="tableheadtext">${uiLabelMap.ProductMinSize}</span></td>
          <td>
            <input type="text" class="inputBox" name="minSize" size="5">
            <span class="tabletext">${uiLabelMap.ProductMinSizeMessage}</span>
          </td>
        </tr>
        <tr>
          <td align="right"><span class="tableheadtext">${uiLabelMap.ProductMaxSize}</span></td>
          <td>
            <input type="text" class="inputBox" name="maxSize" size="5">
            <span class="tabletext">${uiLabelMap.ProductMaxSizeMessage}</span>
          </td>
        </tr>
        <tr>
          <td align="right"><span class="tableheadtext">${uiLabelMap.ProductMinWeight}</span></td>
          <td>
            <input type="text" class="inputBox" name="minWeight" size="5">
            <span class="tabletext">${uiLabelMap.ProductMinWeightMessage}</span>
          </td>
        </tr>
        <tr>
          <td align="right"><span class="tableheadtext">${uiLabelMap.ProductMaxWeight}</span></td>
          <td>
            <input type="text" class="inputBox" name="maxWeight" size="5">
            <span class="tabletext">${uiLabelMap.ProductMaxWeightMessage}</span>
          </td>
        </tr>
        <tr>
          <td align="right"><span class="tableheadtext">${uiLabelMap.ProductMinTotal}</span></td>
          <td>
            <input type="text" class="inputBox" name="minTotal" size="5">
            <span class="tabletext">${uiLabelMap.ProductMinTotalMesssage}</span>
          </td>
        </tr>
        <tr>
          <td align="right"><span class="tableheadtext">${uiLabelMap.ProductMaxTotal}</span></td>
          <td>
            <input type="text" class="inputBox" name="maxTotal" size="5">
            <span class="tabletext">${uiLabelMap.ProductMaxTotalMessage}</span>
          </td>
        </tr>
        <tr>
          <td align="right"><span class="tableheadtext">${uiLabelMap.ProductAllowUSPSAddr}</span></td>
          <td>
            <select name="allowUspsAddr" class="selectBox">
              <option value="N">${uiLabelMap.CommonN}</option>
              <option value="Y">${uiLabelMap.CommonY}</option>
            </select>
          </td>
        </tr>
        <tr>
          <td align="right"><span class="tableheadtext">${uiLabelMap.ProductRequireUSPSAddr}</span></td>
          <td>
            <select name="requireUspsAddr" class="selectBox">
              <option value="N">${uiLabelMap.CommonN}</option>
              <option value="Y">${uiLabelMap.CommonY}</option>
            </select>
            <span class="tabletext">${uiLabelMap.ProductRequireMessage}</span>
          </td>
        </tr>
        <tr>
          <td align="right"><span class="tableheadtext">${uiLabelMap.ProductAllowCoAddr}</span></td>
          <td>
            <select name="allowCompanyAddr" class="selectBox">
              <option value="N">${uiLabelMap.CommonN}</option>
              <option value="Y">${uiLabelMap.CommonY}</option>
            </select>
          </td>
        </tr>
        <tr>
          <td align="right"><span class="tableheadtext">${uiLabelMap.ProductRequireCoAddr}</span></td>
          <td>
            <select name="requireCompanyAddr" class="selectBox">
              <option value="N">${uiLabelMap.CommonN}</option>
              <option value="Y">${uiLabelMap.CommonY}</option>
            </select>
            <span class="tabletext">${uiLabelMap.ProductRequireMessage}</span>
          </td>
        </tr>
        <tr>
          <td align="right"><span class="tableheadtext">${uiLabelMap.PartyCompanyId}</span></td>
          <td>
            <input type="text" class="inputBox" name="companyPartyId" size="20">
            <span class="tabletext">${uiLabelMap.ProductAllowMessage}</span>
          </td>
        </tr>
        <tr>
          <td align="right"><span class="tableheadtext">${uiLabelMap.ProductIncludeFreeship}</span></td>
          <td>
            <select name="includeNoChargeItems" class="selectBox">
              <option value="N">${uiLabelMap.CommonN}</option>
              <option value="Y">${uiLabelMap.CommonY}</option>
            </select>
            <span class="tabletext">${uiLabelMap.ProductIncludeFreeshipMessage}</span>
          </td>
        </tr>
        <tr>
          <td align="right"><span class="tableheadtext">${uiLabelMap.ProductIncludeGeo}</span></td>
          <td>
            <select name="includeGeoId" class="selectBox">
              <option></option>
              <#list geoList as geo>
                <option value="${geo.geoId}">${geo.geoName}</option>
              </#list>
            </select>
            <span class="tabletext">${uiLabelMap.ProductIncludeGeoMessage}</span>
          </td>
        </tr>
        <tr>
          <td align="right"><span class="tableheadtext">${uiLabelMap.ProductExcludeGeo}</span></td>
          <td>
            <select name="excludeGeoId" class="selectBox">
              <option></option>
              <#list geoList as geo>
                <option value="${geo.geoId}">${geo.geoName}</option>
              </#list>
            </select>
            <span class="tabletext">${uiLabelMap.ProductExcludeGeoMessage}</span>
          </td>
        </tr>
        <tr>
          <td align="right"><span class="tableheadtext">${uiLabelMap.ProductIncludeFeature}</span></td>
          <td>
            <input type="text" class="inputBox" name="includeFeatureGroup" size="20">
            <span class="tabletext">${uiLabelMap.ProductIncludeFeatureMessage}</span>
          </td>
        </tr>
        <tr>
          <td align="right"><span class="tableheadtext">${uiLabelMap.ProductExcludeFeature}</span></td>
          <td>
            <input type="text" class="inputBox" name="excludeFeatureGroup" size="20">
            <span class="tabletext">${uiLabelMap.ProductExcludeFeatureMessage}</span>
          </td>
        </tr>
        <tr>
          <td align="right"><span class="tableheadtext">${uiLabelMap.ProductServiceName}</span></td>
          <td>
            <input type="text" class="inputBox" name="serviceName" size="25">
            <span class="tabletext"></span>
          </td>
        </tr>
        <tr>
          <td align="right"><span class="tableheadtext">${uiLabelMap.ProductServiceConfig}</span></td>
          <td>
            <input type="text" class="inputBox" name="configProps" size="25">
            <span class="tabletext"></span>
          </td>
        </tr>
        <tr>
          <td align="right"><span class="tableheadtext">${uiLabelMap.ProductSequence}#</span></td>
          <td>
            <input type="text" class="inputBox" name="sequenceNumber" size="5">
            <span class="tabletext">${uiLabelMap.ProductUsedForDisplayOrdering}</span>
          </td>
        </tr>
        <tr>
          <td>
            <input type="submit" class="smallSubmit" value="${uiLabelMap.CommonAdd}">
          </td>
        </tr>
      </form>
    </table>
    <br/>

    <div class="head2">${uiLabelMap.ProductShipmentMethodType} :</div>
    <table cellspacing="2" cellpadding="2">
      <form name="editmeth" method="post" action="<@ofbizUrl>EditProductStoreShipSetup</@ofbizUrl>">
        <input type="hidden" name="viewProductStoreId" value="${productStoreId}">
        <input type="hidden" name="newShipMethod" value="Y">
        <tr>
          <td align="right"><span class="tableheadtext">${uiLabelMap.ProductSelectToEdit}</span></td>
          <td>
            <select class="selectBox" name="editShipmentMethodTypeId">
              <#list shipmentMethodTypes as shipmentMethodType>
                <option value="${shipmentMethodType.shipmentMethodTypeId}">${shipmentMethodType.description?default(shipmentMethodType.shipmentMethodTypeId)}</option>
              </#list>
            </select>
            <input type="submit" class="smallSubmit" value="${uiLabelMap.CommonEdit}">
          </td>
        </tr>
      </form>
      <#if shipmentMethodType?has_content>
        <#assign webRequest = "/updateShipmentMethodType">
        <#assign buttonText =uiLabelMap.CommonUpdate>
      <#else>
        <#assign webRequest = "/createShipmentMethodType">
        <#assign buttonText = uiLabelMap.CommonCreate>
      </#if>
      <form name="addmeth" method="post" action="<@ofbizUrl>${webRequest}</@ofbizUrl>">
        <input type="hidden" name="viewProductStoreId" value="${productStoreId}">
        <input type="hidden" name="newShipMethod" value="Y">
        <tr>
          <td align="right"><span class="tableheadtext">${uiLabelMap.ProductShipmentMethodType}</span></td>
          <td>
            <#if shipmentMethodType?has_content>
              <div class="tabletext">${shipmentMethodType.shipmentMethodTypeId}</div>
              <input type="hidden" name="shipmentMethodTypeId" value="${shipmentMethodType.shipmentMethodTypeId}">
            <#else>
              <input type="text" class="inputBox" name="shipmentMethodTypeId" size="20"> *</td>
            </#if>
          </td>
        </tr>
        <tr>
          <td align="right"><span class="tableheadtext">${uiLabelMap.ProductDescription}</span></td>
          <td><input type="text" class="inputBox" name="description" size="30" value="${shipmentMethodType.description?if_exists}"> *</td>
        </tr>
        <tr>
          <td>
            <input type="submit" class="smallSubmit" value="${buttonText}">
          </td>
        </tr>
      </form>
    </table>

    <br/>

    <div class="head2">${uiLabelMap.ProductCarrierShipmentMethod} :</div>
    <table cellspacing="2" cellpadding="2">
      <form name="editcarr" method="post" action="<@ofbizUrl>EditProductStoreShipSetup</@ofbizUrl>">
        <input type="hidden" name="viewProductStoreId" value="${productStoreId}">
        <input type="hidden" name="newShipMethod" value="Y">

        <tr>
          <td align="right"><span class="tableheadtext">${uiLabelMap.ProductSelectToEdit}</span></td>
          <td>
            <select class="selectBox" name="editCarrierShipmentMethodId">
              <#list shipmentMethods as shipmentMethod>
                <option value="${shipmentMethod.partyId}|${shipmentMethod.roleTypeId}|${shipmentMethod.shipmentMethodTypeId}">${shipmentMethod.description} (${shipmentMethod.partyId}/${shipmentMethod.roleTypeId})</option>
              </#list>
            </select>
            <input type="submit" class="smallSubmit" value="${uiLabelMap.CommonEdit}">
          </td>
        </tr>
      </form>
      <#if carrierShipmentMethod?has_content>
        <#assign webRequest = "/updateCarrierShipmentMethod">
        <#assign buttonText = uiLabelMap.CommonUpdate>
      <#else>
        <#assign webRequest = "/createCarrierShipmentMethod">
        <#assign buttonText = uiLabelMap.CommonCreate>
      </#if>
      <form name="addcarr" method="post" action="<@ofbizUrl>${webRequest}</@ofbizUrl>">
        <input type="hidden" name="viewProductStoreId" value="${productStoreId}">
        <input type="hidden" name="newShipMethod" value="Y">
       
        <tr>
          <td align="right"><span class="tableheadtext">${uiLabelMap.ProductShipmentMethod}</span></td>
          <td>
            <#if carrierShipmentMethod?has_content>
              <input type="hidden" name="shipmentMethodTypeId" value="${carrierShipmentMethod.shipmentMethodTypeId}">
              <div class="tabletext">${carrierShipmentMethod.shipmentMethodTypeId}</div>
            <#else>
              <select class="selectBox" name="shipmentMethodTypeId">
                <#list shipmentMethodTypes as shipmentMethodType>
                  <option value="${shipmentMethodType.shipmentMethodTypeId}">${shipmentMethodType.description?default(shipmentMethodType.shipmentMethodTypeId)}</option>
                </#list>
              </select> *
            </#if>
          </td>
        </tr>
        <tr>
          <td align="right"><span class="tableheadtext">${uiLabelMap.PartyRoleType}</span></td>
          <td>
            <#if carrierShipmentMethod?has_content>
              <input type="hidden" name="roleTypeId" value="${carrierShipmentMethod.roleTypeId}">
              <div class="tabletext">${carrierShipmentMethod.roleTypeId}</div>
            <#else>
              <select class="selectBox" name="roleTypeId">
                <#list roleTypes as roleType>
                  <option value="${roleType.roleTypeId}" <#if roleType.roleTypeId == "CARRIER" && !carrierShipmentMethod?has_content>${uiLabelMap.ProductSelected}</#if>>${roleType.get("description",locale)?default(roleType.roleTypeId)}</option>
                </#list>
              </select> *
            </#if>
          </td>
        </tr>
        <tr>
          <td align="right"><span class="tableheadtext">${uiLabelMap.PartyParty}</span></td>
          <td>
            <#if carrierShipmentMethod?has_content>
              <input type="hidden" name="partyId" value="${carrierShipmentMethod.partyId}">
              <div class="tabletext">${carrierShipmentMethod.partyId}</div>
            <#else>
              <input type="text" class="inputBox" name="partyId" size="20" value="${carrierShipmentMethod.partyId?if_exists}"> *
            </#if>
          </td>
        </tr>
        <tr>
          <td align="right"><span class="tableheadtext">${uiLabelMap.ProductCarrierServiceCode}</span></td>
          <td><input type="text" class="inputBox" name="carrierServiceCode" size="20" value="${carrierShipmentMethod.carrierServiceCode?if_exists}"></td>
        </tr>
        <tr>
          <td align="right"><span class="tableheadtext">${uiLabelMap.ProductSequence} #</span></td>
          <td>
            <input type="text" class="inputBox" name="sequenceNumber" size="5" value="${carrierShipmentMethod.sequenceNumber?if_exists}">
            <span class="tabletext">${uiLabelMap.ProductUsedForDisplayOrdering}</span>
          </td>
        </tr>
        <tr>
          <td>
            <input type="submit" class="smallSubmit" value="${buttonText}">
          </td>
        </tr>
      </form>
    </table>
