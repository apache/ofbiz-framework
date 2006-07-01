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
 *@version    $Rev$
 *@since      2.2
-->

<div class="head1">Current Returns</div>
<div><a href="<@ofbizUrl>returnMain</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderReturnCreate}</a></div>

<br/>
<table width="100%" border="0" cellpadding="0" cellspacing="0"> 
  <tr>
    <td><div class="tableheadtext">${uiLabelMap.OrderReturnId} #</div></td>
    <td><div class="tableheadtext">${uiLabelMap.FormFieldTitle_entryDate}</div></td>
    <td><div class="tableheadtext">${uiLabelMap.PartyParty}</div></td>
    <td><div class="tableheadtext">${uiLabelMap.FacilityFacility}</div></td>
    <td><div class="tableheadtext">${uiLabelMap.CommonStatus}</div></td>
  </tr> 
  <tr><td colspan="5"><hr class="sepbar"></td></tr>
  <#list returnList as returnHeader>
  <#assign statusItem = returnHeader.getRelatedOne("StatusItem")>
  <#if returnHeader.destinationFacilityId?exists>
    <#assign facility = returnHeader.getRelatedOne("Facility")>
  </#if>
  <tr>
    <td><a href="<@ofbizUrl>returnMain?returnId=${returnHeader.returnId}</@ofbizUrl>" class="buttontext">${returnHeader.returnId}</a></td>
    <td><div class="tabletext">${returnHeader.entryDate.toString()}</div></td>
    <td>
      <#if returnHeader.fromPartyId?exists>
        <a href="${customerDetailLink}${returnHeader.fromPartyId}${externalKeyParam}" class='buttontext'>${returnHeader.fromPartyId}</a>
      <#else>
        <span class="tabletext">${uiLabelMap.CommonNA}</span>
      </#if>
    </td>
    <td><div class="tabletext"><#if facility?exists>${facility.facilityName?default(facility.facilityId)}<#else>${uiLabelMap.CommonNone}</#if></div></td>
    <td><div class="tabletext">${statusItem.get("description",locale)}</div></td>   
  </tr>
  </#list>
</table>
