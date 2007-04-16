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

<#if shipment?exists>
<table width="100%" cellspacing="0" cellpadding="2" border="1">
    <tr>
        <td><div class="tableheadtext">${uiLabelMap.ProductPackage}</div></td>
        <td><div class="tableheadtext">${uiLabelMap.CommonCreated}</div></td>
        <td><div class="tableheadtext">&nbsp;</div></td>
        <td><div class="tableheadtext">&nbsp;</div></td>
        <td><div class="tableheadtext">&nbsp;</div></td>
        <td><div class="tableheadtext">&nbsp;</div></td>
    </tr>
<#list shipmentPackageDatas as shipmentPackageData>
    <#assign shipmentPackage = shipmentPackageData.shipmentPackage>
    <#assign shipmentPackageContents = shipmentPackageData.shipmentPackageContents?if_exists>
    <#assign shipmentPackageRouteSegs = shipmentPackageData.shipmentPackageRouteSegs?if_exists>
    <#assign weightUom = shipmentPackageData.weightUom?if_exists>
    <form action="<@ofbizUrl>updateShipmentPackage</@ofbizUrl>" name="updateShipmentPackageForm${shipmentPackageData_index}">
    <input type="hidden" name="shipmentId" value="${shipmentId}"/>
    <input type="hidden" name="shipmentPackageSeqId" value="${shipmentPackage.shipmentPackageSeqId}"/>
    <tr>
        <td><div class="tabletext">${shipmentPackage.shipmentPackageSeqId}</div></td>
        <td><div class="tabletext">${(shipmentPackage.dateCreated.toString())?if_exists}</div></td>
        <td><span class="tabletext">${uiLabelMap.ProductWeight}:</span><input type="text" size="5" name="weight" value="${shipmentPackage.weight?if_exists}" class="inputBox"/></td>
        <td><span class="tabletext">${uiLabelMap.ProductWeightUnit}:</span>
            <select name="weightUomId" class="selectBox">
                <#if weightUom?has_content>
                    <option value="${weightUom.uomId}">${weightUom.get("description",locale)}</option>
                    <option value="${weightUom.uomId}">---</option>
                <#else>
                    <option value="">&nbsp;</option>
                </#if>
                <#list weightUoms as weightUomOption>
                    <option value="${weightUomOption.uomId}">${weightUomOption.get("description",locale)} [${weightUomOption.abbreviation}]</option>
                </#list>
            </select>
            <span class="tabletext">${uiLabelMap.ProductShipmentBoxType}:</span>
            <select name="shipmentBoxTypeId" class="selectBox">
                <option value="">&nbsp;</option>
                <#list boxTypes as boxType>
                    <option value="${boxType.shipmentBoxTypeId}" <#if shipmentPackage.shipmentBoxTypeId?exists && shipmentPackage.shipmentBoxTypeId == boxType.shipmentBoxTypeId>selected</#if>>${boxType.get("description",locale)}</option>
                </#list>
            </select>
            <br />
            <span class="tabletext">${uiLabelMap.ProductShipmentInsuredValuePackage}:</span>
            <input type="text" size="5" name="insuredValue" value="${shipmentPackage.insuredValue?if_exists}" class="inputBox"/>
        </td>
        <td><a href="javascript:document.updateShipmentPackageForm${shipmentPackageData_index}.submit();" class="buttontext">${uiLabelMap.CommonUpdate}</a></td>
        <td><div class="tabletext"><a href="<@ofbizUrl>deleteShipmentPackage?shipmentId=${shipmentId}&shipmentPackageSeqId=${shipmentPackage.shipmentPackageSeqId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonDelete}</a></div></td>
    </tr>
    </form>
    <#list shipmentPackageContents as shipmentPackageContent>
        <tr>
            <td><div class="tabletext">&nbsp;</div></td>
            <td><div class="tabletext">${uiLabelMap.ProductItem} :${shipmentPackageContent.shipmentItemSeqId}</div></td>
            <td><div class="tabletext">${uiLabelMap.ProductQuantity} :${shipmentPackageContent.quantity?if_exists}</div></td>
            <td><div class="tabletext">&nbsp;</div></td>
            <td><div class="tabletext">&nbsp;</div></td>
            <td><div class="tabletext"><a href="<@ofbizUrl>deleteShipmentPackageContent?shipmentId=${shipmentId}&shipmentPackageSeqId=${shipmentPackageContent.shipmentPackageSeqId}&shipmentItemSeqId=${shipmentPackageContent.shipmentItemSeqId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonDelete}</a></div></td>
        </tr>
    </#list>
    <tr>
        <form action="<@ofbizUrl>createShipmentPackageContent</@ofbizUrl>" name="createShipmentPackageContentForm${shipmentPackageData_index}">
        <input type="hidden" name="shipmentId" value="${shipmentId}"/>
        <input type="hidden" name="shipmentPackageSeqId" value="${shipmentPackage.shipmentPackageSeqId}"/>
        <td><div class="tabletext">&nbsp;</div></td>
        <td>
            <div class="tabletext">${uiLabelMap.ProductAddFromItem} :
            <select name="shipmentItemSeqId" class="selectBox">
                <#list shipmentItems as shipmentItem>
                    <option>${shipmentItem.shipmentItemSeqId}</option>
                </#list>
            </select>
            </div>
        </td>
        <td><div class="tabletext">${uiLabelMap.ProductQuantity} :<input name="quantity" size="5" value="0" class="inputBox"/></div></td>
        <td><div class="tabletext">&nbsp;</div></td>
        <td><a href="javascript:document.createShipmentPackageContentForm${shipmentPackageData_index}.submit()" class="buttontext">${uiLabelMap.CommonAdd}</a></td>
        <td><div class="tabletext">&nbsp;</div></td>
        </form>
    </tr>
    <#list shipmentPackageRouteSegs as shipmentPackageRouteSeg>
        <form action="<@ofbizUrl>updateShipmentPackageRouteSeg</@ofbizUrl>" name="updateShipmentPackageRouteSegForm${shipmentPackageData_index}${shipmentPackageRouteSeg_index}">
        <input type="hidden" name="shipmentId" value="${shipmentId}"/>
        <input type="hidden" name="shipmentRouteSegmentId" value="${shipmentPackageRouteSeg.shipmentRouteSegmentId}"/>
        <input type="hidden" name="shipmentPackageSeqId" value="${shipmentPackageRouteSeg.shipmentPackageSeqId}"/>
        <tr>
            <td><div class="tabletext">&nbsp;</div></td>
            <td><div class="tabletext">${uiLabelMap.ProductRouteSegment} :${shipmentPackageRouteSeg.shipmentRouteSegmentId}</div></td>
            <td><span class="tabletext">${uiLabelMap.ProductTrack} :</span><input type="text" size="22" name="trackingCode" value="${shipmentPackageRouteSeg.trackingCode?if_exists}" class="inputBox"/></td>
            <td><span class="tabletext">${uiLabelMap.ProductBox} :</span><input type="text" size="5" name="boxNumber" value="${shipmentPackageRouteSeg.boxNumber?if_exists}" class="inputBox"/></td>
            <td><a href="javascript:document.updateShipmentPackageRouteSegForm${shipmentPackageData_index}${shipmentPackageRouteSeg_index}.submit();" class="buttontext">${uiLabelMap.CommonUpdate}</a></td>
            <td><div class="tabletext"><a href="<@ofbizUrl>deleteShipmentPackageRouteSeg?shipmentId=${shipmentId}&shipmentPackageSeqId=${shipmentPackageRouteSeg.shipmentPackageSeqId}&shipmentRouteSegmentId=${shipmentPackageRouteSeg.shipmentRouteSegmentId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonDelete}</a></div></td>
        </tr>
        </form>
    </#list>
    <#--
    <tr>
        <form action="<@ofbizUrl>createShipmentPackageRouteSeg</@ofbizUrl>" name="createShipmentPackageRouteSegForm${shipmentPackageData_index}">
        <input type="hidden" name="shipmentId" value="${shipmentId}"/>
        <input type="hidden" name="shipmentPackageSeqId" value="${shipmentPackage.shipmentPackageSeqId}"/>
        <td><div class="tabletext">&nbsp;</div></td>
        <td>
            <div class="tabletext">${uiLabelMap.ProductAddRouteSegmentInfo}:
            <select name="shipmentRouteSegmentId" class="selectBox">
                <#list shipmentRouteSegments as shipmentRouteSegment>
                    <option>${shipmentRouteSegment.shipmentRouteSegmentId}</option>
                </#list>
            </select>
            </div>
        </td>
        <td><span class="tabletext">Track#:</span><input type="text" size="22" name="trackingCode" class="inputBox"/></td>
        <td><span class="tabletext">Box#:</span><input type="text" size="5" name="boxNumber" class="inputBox"/></td>
        <td><a href="javascript:document.createShipmentPackageRouteSegForm${shipmentPackageData_index}.submit()" class="buttontext">${uiLabelMap.CommonAdd}</a></td>
        <td><div class="tabletext">&nbsp;</div></td>
        </form>
    </tr>
    -->
</#list>
<#--
<form action="<@ofbizUrl>createShipmentPackage</@ofbizUrl>" name="createShipmentPackageForm">
    <input type="hidden" name="shipmentId" value="${shipmentId}"/>
    <tr>
        <td><div class="tabletext">${uiLabelMap.ProductNewPackage} :</div></td>
        <td><div class="tabletext">&nbsp;</div></td>
        <td><span class="tabletext">${uiLabelMap.ProductWeight} :</span><input type="text" size="5" name="weight" class="inputBox"/></td>
        <td><span class="tabletext">${uiLabelMap.ProductWeightUnit} :</span>
            <select name="weightUomId" class="selectBox">
                <#list weightUoms as weightUomOption>
                    <option value="${weightUomOption.uomId}">${weightUomOption.get("description",locale)} [${weightUomOption.abbreviation}]</option>
                </#list>
            </select>
        </td>
        <td><a href="javascript:document.createShipmentPackageForm.submit();" class="buttontext">${uiLabelMap.CommonCreate}</a></td>
        <td>&nbsp;</td>
    </tr>
</form>
-->
</table>
<#else>
  <h3>${uiLabelMap.ProductShipmentNotFoundId} : [${shipmentId?if_exists}]</h3>
</#if>
