<#--
 *  Copyright (c) 2001 The Open For Business Project - www.ofbiz.org
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
 *@author     thierry.grauss@etu.univ-tours.fr (migration to uiLabelMap)
 *@created    May 10, 2002
 *@version    1.0
-->

<#if security.hasEntityPermission("FACILITY", "_VIEW", session)>

<div class="head1">${uiLabelMap.ProductFacilitiesList}</div>

<div><a href='<@ofbizUrl>EditFacility</@ofbizUrl>' class="buttontext">${uiLabelMap.ProductCreateNewFacility}</a></div>
<br/>
<table border="1" cellpadding='2' cellspacing='0'>
  <tr>
    <td><div class="tabletext"><b>${uiLabelMap.ProductFacilityNameId}</b></div></td>
    <td><div class="tabletext"><b>${uiLabelMap.ProductFacilityType}</b></div></td>
    <td><div class="tabletext"><b>${uiLabelMap.ProductFacilityOwner}</b></div></td>
    <td><div class="tabletext"><b>${uiLabelMap.ProductSqFt}</b></div></td>
    <td><div class="tabletext"><b>${uiLabelMap.ProductDescription}</b></div></td>
    <td><div class="tabletext">&nbsp;</div></td>
  </tr>
<#list facilities as facility>
  <#assign facilityType = facility.getRelatedOne("FacilityType")?if_exists>
  <tr valign="middle">
    <td><div class='tabletext'>&nbsp;${facility.facilityName?if_exists} <a href='<@ofbizUrl>EditFacility?facilityId=${facility.facilityId?if_exists}</@ofbizUrl>' class="buttontext">${facility.facilityId?if_exists}</a></div></td>
    <td><div class='tabletext'>&nbsp;${facilityType.get("description",locale)?if_exists}</div></td>
    <td><div class='tabletext'>&nbsp;${facility.ownerPartyId?if_exists}</div></td>
    <td><div class='tabletext'>&nbsp;${facility.squareFootage?if_exists}</div></td>
    <td><div class='tabletext'>&nbsp;${facility.description?if_exists}</div></td>
    <td>
      <a href='<@ofbizUrl>EditFacility?facilityId=${facility.facilityId?if_exists}</@ofbizUrl>' class="buttontext">${uiLabelMap.CommonEdit}</a>
    </td>
  </tr>
</#list>
</table>
<br/>

<#else>
  <h3>${uiLabelMap.ProductFacilityViewPermissionError}</h3>
</#if>

