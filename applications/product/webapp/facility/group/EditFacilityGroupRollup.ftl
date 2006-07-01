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
 *@author     David E. Jones
 *@author     Brad Steiner
 *@author     Catherine.Heintz@nereide.biz (migration to UiLabel)
 *@version    $Rev$
 *@since      2.2
-->

<div class="head1">${uiLabelMap.ProductRollups} <span class="head2">${uiLabelMap.CommonFor}"${(facilityGroup.facilityGroupName)?if_exists}" [${uiLabelMap.CommonId}:${facilityGroupId?if_exists}]</span></div>
<a href="<@ofbizUrl>EditFacilityGroup</@ofbizUrl>" class="buttontext">[${uiLabelMap.ProductNewGroup}]</a>
<br/>
<br/>

<#if facilityGroup?exists>
<p class="head2">${uiLabelMap.ProductFacilityGroupRollupParentGroups}</p>

<table border="1" cellpadding="2" cellspacing="0">
  <tr>
    <td><div class="tabletext"><b>${uiLabelMap.ProductParentGroupId}</b></div></td>
    <td><div class="tabletext"><b>${uiLabelMap.CommonFromDate}</b></div></td>
    <td align="center"><div class="tabletext"><b>${uiLabelMap.ProductThruDateTimeSequence}</b></div></td>
    <td><div class="tabletext"><b>&nbsp;</b></div></td>
  </tr>
<#if currentGroupRollups?has_content>
  <#list currentGroupRollups as facilityGroupRollup>
    <#assign curGroup = facilityGroupRollup.getRelatedOne("ParentFacilityGroup")?if_exists>
    <tr valign="middle">
      <td><a href="<@ofbizUrl>EditFacilityGroup?facilityGroupId=${(curGroup.facilityGroupId)?if_exists}</@ofbizUrl>" class="buttontext">${(curGroup.facilityGroupName)?if_exists}</a></td>
      <td><div class="tabletext" <#if facilityGroupRollup.fromDate?exists && Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp().before(facilityGroupRollup.fromDate)>style="color: red;"</#if>>${facilityGroupRollup.fromDate}</div></td>
      <td align="center">
        <FORM method="post" action="<@ofbizUrl>updateFacilityGroupToGroup</@ofbizUrl>" name="lineParentForm${facilityGroupRollup_index}">
            <input type="hidden" name="showFacilityGroupId" value="${facilityGroupId}">
            <input type="hidden" name="facilityGroupId" value="${facilityGroupRollup.facilityGroupId}">
            <input type="hidden" name="parentFacilityGroupId" value="${facilityGroupRollup.parentFacilityGroupId}">
            <input type="hidden" name="fromDate" value="${facilityGroupRollup.fromDate.toString()}">
            <input type="text" size="25" name="thruDate" value="${(facilityGroupRollup.thruDate.toString())?if_exists}" class="inputBox" <#if facilityGroupRollup.thruDate?exists && Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp().after(facilityGroupRollup.thruDate)>style="color: red;"</#if>>
            <a href="javascript:call_cal(document.lineParentForm${facilityGroupRollup_index}.thruDate, '${(facilityGroupRollup.thruDate.toString())?default(nowTimestampString)}');"><img src="/images/cal.gif" width="16" height="16" border="0" alt="Calendar"></a>
            <input type="text" size="5" name="sequenceNum" value="${facilityGroupRollup.sequenceNum?if_exists}" class="inputBox">
            <INPUT type="submit" value="${uiLabelMap.CommonUpdate}" style="font-size: x-small;">
        </FORM>
      </td>
      <td>
        <a href="<@ofbizUrl>removeFacilityGroupFromGroup?showFacilityGroupId=${facilityGroupId}&facilityGroupId=${facilityGroupRollup.facilityGroupId}&parentFacilityGroupId=${facilityGroupRollup.parentFacilityGroupId}&fromDate=${(facilityGroupRollup.fromDate.toString())?if_exists}</@ofbizUrl>" class="buttontext">
        [${uiLabelMap.CommonDelete}]</a>
      </td>
    </tr>
  </#list>
<#else>
  <tr valign="middle">
    <td colspan="5"><div class="tabletext">${uiLabelMap.ProductNoParentGroupsFound}.</div></td>
  </tr>
</#if>
</table>
<br/>
<form method="post" action="<@ofbizUrl>addFacilityGroupToGroup</@ofbizUrl>" style="margin: 0;" name="addParentForm">
  <input type="hidden" name="facilityGroupId" value="${facilityGroupId}">
  <input type="hidden" name="showFacilityGroupId" value="${facilityGroupId}">
  <div class="tabletext">${uiLabelMap.CommonAdd} <b>${uiLabelMap.ProductParent}</b> ${uiLabelMap.ProductGroupSelectCategoryFromDate}:</div>
    <select name="parentFacilityGroupId" class="selectBox">
    <#list facilityGroups as curGroup>
      <#if !(facilityGroupId == curGroup.facilityGroupId) && !("_NA_" == curGroup.facilityGroupId)>
        <option value="${curGroup.facilityGroupId}">${curGroup.facilityGroupName?if_exists} [${curGroup.facilityGroupId}]</option>
      </#if>
    </#list>
    </select>
  <input type="text" class="inputBox" size="25" name="fromDate">
  <a href="javascript:call_cal(document.addParentForm.fromDate, '${nowTimestampString}');"><img src="/images/cal.gif" width="16" height="16" border="0" alt="Calendar"></a>
  <input type="submit" value="${uiLabelMap.CommonAdd}">
</form>
<br/>
<hr>
<br/>
<p class="head2">${uiLabelMap.ProductGroupRollupChildGroups}</p>

<table border="1" cellpadding="2" cellspacing="0">
  <tr>
    <td><div class="tabletext"><b>${uiLabelMap.ProductChildGroupId}</b></div></td>
    <td><div class="tabletext"><b>${uiLabelMap.CommonFromDate}</b></div></td>
    <td align="center"><div class="tabletext"><b>${uiLabelMap.ProductThruDateTimeSequence}</b></div></td>
    <td><div class="tabletext"><b>&nbsp;</b></div></td>
  </tr>
<#if parentGroupRollups?has_content>
  <#list parentGroupRollups as facilityGroupRollup>
    <#assign curGroup = facilityGroupRollup.getRelatedOne("CurrentFacilityGroup")>
    <tr valign="middle">
      <td><a href="<@ofbizUrl>EditFacilityGroup?facilityGroupId=${(curGroup.facilityGroupId)?if_exists}</@ofbizUrl>" class="buttontext">${(curGroup.facilityGroupName)?if_exists}</a></td>
      <td><div class="tabletext" <#if facilityGroupRollup.fromDate?exists && Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp().before(facilityGroupRollup.fromDate)>style="color: red;"</#if>>${facilityGroupRollup.fromDate}</div></td>
      <td align="center">
        <FORM method="post" action="<@ofbizUrl>updateFacilityGroupToGroup</@ofbizUrl>" name="lineChildForm${facilityGroupRollup_index}">
            <input type="hidden" name="showFacilityGroupId" value="${facilityGroupId}">
            <input type="hidden" name="facilityGroupId" value="${facilityGroupRollup.facilityGroupId}">
            <input type="hidden" name="parentFacilityGroupId" value="${facilityGroupRollup.parentFacilityGroupId}">
            <input type="hidden" name="fromDate" value="${facilityGroupRollup.fromDate.toString()}">
            <input type="text" size="25" name="thruDate" value="${(facilityGroupRollup.thruDate.toString())?if_exists}" class="inputBox" <#if facilityGroupRollup.thruDate?exists && Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp().after(facilityGroupRollup.thruDate)>style="color: red;"</#if>>
            <a href="javascript:call_cal(document.lineChildForm${facilityGroupRollup_index}.thruDate, '${(facilityGroupRollup.thruDate.toString())?default(nowTimestampString)}');"><img src="/images/cal.gif" width="16" height="16" border="0" alt="Calendar"></a>
            <input type="text" size="5" name="sequenceNum" value="${facilityGroupRollup.sequenceNum?if_exists}" class="inputBox">
            <INPUT type="submit" value="${uiLabelMap.CommonUpdate}" style="font-size: x-small;">
        </FORM>
      </td>
      <td>
        <a href="<@ofbizUrl>removeFacilityGroupFromGroup?showFacilityGroupId=${facilityGroupId}&facilityGroupId=${facilityGroupRollup.facilityGroupId}&parentFacilityGroupId=${facilityGroupRollup.parentFacilityGroupId}&fromDate=${(facilityGroupRollup.fromDate.toString())?if_exists}</@ofbizUrl>" class="buttontext">
        [${uiLabelMap.CommonDelete}]</a>
      </td>
    </tr>
  </#list>
<#else>
  <tr valign="middle">
    <td colspan="5"><DIV class="tabletext">${uiLabelMap.ProductNoChildGroupsFound}.</DIV></td>
  </tr>
</#if>
</table>
<br/>
<form method="post" action="<@ofbizUrl>addFacilityGroupToGroup</@ofbizUrl>" style="margin: 0;" name="addChildForm">
  <input type="hidden" name="showFacilityGroupId" value="${facilityGroupId}">
  <input type="hidden" name="parentFacilityGroupId" value="${facilityGroupId}">
  <div class="tabletext">${uiLabelMap.CommonAdd} <b>${uiLabelMap.ProductChild}</b> ${uiLabelMap.ProductGroupSelectGroupFromDate} :</div>
    <select name="facilityGroupId" class="selectBox">
    <#list facilityGroups as curGroup>
      <#if !(facilityGroupId == curGroup.facilityGroupId) && !("_NA_" == curGroup.facilityGroupId)>
        <option value="${curGroup.facilityGroupId}">${curGroup.facilityGroupName?if_exists} [${curGroup.facilityGroupId}]</option>
      </#if>
    </#list>
    </select>
  <input type="text" class="inputBox" size="25" name="fromDate">
  <a href="javascript:call_cal(document.addChildForm.fromDate, '${nowTimestampString}');"><img src="/images/cal.gif" width="16" height="16" border="0" alt="Calendar"></a>
  <input type="submit" value="${uiLabelMap.CommonAdd}">
</form>
</#if>
