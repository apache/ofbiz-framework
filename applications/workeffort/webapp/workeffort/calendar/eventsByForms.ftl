<#--
 *  Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
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
 *@author     Johan Isacsson
 *@created    May 19 2003
 *@author     Eric.Barbier@nereide.biz (migration to uiLabelMap)
 *@version    1.0
-->
<table width="100%" border="0" cellpadding="0" cellspacing="0">
    <tr>
        <td width="34%" align="center">
            <div class="tabletext">
                <form action="<@ofbizUrl>${parameters.targetRequestUri}</@ofbizUrl>" name="partyform" method="post">
                    <input type="hidden" name="start" value="${start.time?string("#")}"/>
                    ${uiLabelMap.WorkEffortByPartyId}: 
                    <input type="text" name="partyId" value="${requestParameters.partyId?if_exists}" class="inputBox"/>
                    <a href="javascript:call_fieldlookup2(document.partyform.partyId,'<@ofbizUrl>LookupPartyName</@ofbizUrl>');"><img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt='Click here For Field Lookup'></a>
                    <input type="submit" value="${uiLabelMap.CommonView}" class="smallSubmit"/>
                </form>
            </div>
        </td>
        <td width="32%" align="center">
            <div class="tabletext">
                <form action="<@ofbizUrl>${parameters.targetRequestUri}</@ofbizUrl>" method="post">
                    <input type="hidden" name="start" value="${start.time?string("#")}"/>
                    ${uiLabelMap.WorkEffortByFacility}: 
                    <select name="facilityId" class="selectBox">
                        <option value=""></option>
                        <#list allFacilities as facility>
                            <option value="${facility.facilityId}"<#if requestParameters.facilityId?has_content && requestParameters.facilityId == facility.facilityId>${uiLabelMap.WorkEffortSelected}</#if>>${facility.facilityName}</option>
                        </#list>
                    </select>
                    <input type="submit" value="${uiLabelMap.CommonView}" class="smallSubmit"/>
                </form>
            </div>
        </td>
        <td width="32%" align="center">
            <div class="tabletext">
                <form action="<@ofbizUrl>${parameters.targetRequestUri}</@ofbizUrl>" method="post">
                    <input type="hidden" name="start" value="${start.time?string("#")}"/>
                    ${uiLabelMap.WorkEffortByFixedAsset}: 
                    <select name="fixedAssetId" class="selectBox">
                        <option value=""></option>
                        <#list allFixedAssets as fixedAsset>
                            <option value="${fixedAsset.fixedAssetId}"<#if requestParameters.fixedAssetId?has_content && requestParameters.fixedAssetId == fixedAsset.fixedAssetId>${uiLabelMap.WorkEffortSelected}</#if>>${fixedAsset.fixedAssetId}</option>
                        </#list>
                    </select>
                    <input type="submit" value="${uiLabelMap.CommonView}" class="smallSubmit"/>
                </form>
            </div>
        </td>
    </tr>
</table>
<div class="tabletext">&nbsp;</div>
