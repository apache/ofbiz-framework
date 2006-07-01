<#--
 *  Copyright (c) 2003 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a 
 *  copy of this software and associated documentation files (the "Software"), 
 *  to deal in the Software without restriction, including without limitation 
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 *  and/or sell copies of the Software, and to permit persons to whom the 
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included 
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS 
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT 
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR 
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *@author     Andy Zeneski (jaz@ofbiz.org)
 *@author     David E. Jones (jonesde@ofbiz.org)
 *@author     Catherine.Heintz@nereide.biz (migration to UiLabel)
 *@version    $Rev$
 *@since      3.0
-->
<#if shipmentPackageDatas?has_content>
  <br/>
  <table width="100%" cellspacing="0" cellpadding="2" border="1">
    <tr>
      <td><div class="tableheadtext">${uiLabelMap.ProductPackage}</div></td>
      <td><div class="tableheadtext">${uiLabelMap.CommonCreated}</div></td>
      <td><div class="tableheadtext">&nbsp;</div></td>
      <td><div class="tableheadtext">&nbsp;</div></td>
    </tr>
    <#list shipmentPackageDatas as shipmentPackageData>
      <#assign shipmentPackage = shipmentPackageData.shipmentPackage>
      <#assign shipmentPackageContents = shipmentPackageData.shipmentPackageContents?if_exists>
      <#assign shipmentPackageRouteSegs = shipmentPackageData.shipmentPackageRouteSegs?if_exists>
      <#assign weightUom = shipmentPackageData.weightUom?if_exists>
      <tr>
        <td><div class="tabletext">${shipmentPackage.shipmentPackageSeqId}</div></td>
        <td><div class="tabletext">${(shipmentPackage.dateCreated.toString())?if_exists}</div></td>
        <td><span class="tabletext">${uiLabelMap.ProductWeight} : ${shipmentPackage.weight?if_exists}</span></td>
        <td><span class="tabletext">${uiLabelMap.ProductWeightUnit} :${weightUom.description?default(shipmentPackage.weightUomId?if_exists)}</span></td>
      </tr>
      <#list shipmentPackageContents as shipmentPackageContent>
        <tr>
          <td><div class="tabletext">&nbsp;</div></td>
          <td><div class="tabletext">${uiLabelMap.ProductItem} :${shipmentPackageContent.shipmentItemSeqId}</div></td>
          <td><div class="tabletext">${uiLabelMap.ProductQuantity} :${shipmentPackageContent.quantity?if_exists}</div></td>
          <td><div class="tabletext">&nbsp;</div></td>
        </tr>
      </#list>
      <#list shipmentPackageRouteSegs as shipmentPackageRouteSeg>
        <tr>
          <td><div class="tabletext">&nbsp;</div></td>
          <td><div class="tabletext">${uiLabelMap.ProductRouteSegment} :${shipmentPackageRouteSeg.shipmentRouteSegmentId}</div></td>
          <td><span class="tabletext">${uiLabelMap.ProductTracking} : ${shipmentPackageRouteSeg.trackingCode?if_exists}</span></td>
          <td><span class="tabletext">${uiLabelMap.ProductBox} : ${shipmentPackageRouteSeg.boxNumber?if_exists}</span></td>
        </tr>
      </#list>
    </#list>
  </table>
</#if>
