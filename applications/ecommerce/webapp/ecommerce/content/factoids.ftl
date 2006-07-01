<#--
 *  Copyright (c) 2003-2005 The Open For Business Project - www.ofbiz.org
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
 *@author     Al Byers (byersa@automationgroups.com)
 *@version    $Rev$
 *@since      3.1
-->
<#assign factoidRootId = "WebStoreFACTOID"/>

<#-- variable setup and worker calls -->
<#assign curCategoryId = requestAttributes.curCategoryId?if_exists>
<#assign factoidTrailCsv = requestParameters.factoidTrailCsv?if_exists/>
<#assign factoidTrail=[]/>
<#assign firstContentId=""/>
<#if factoidTrailCsv?has_content>
  <#assign factoidTrail=Static["org.ofbiz.base.util.StringUtil"].split(factoidTrailCsv, ",") />
  <#if 0 < factoidTrail?size>
    <#assign firstContentId=factoidTrail[0]?string/>
  </#if>
</#if>

<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxhead">${uiLabelMap.Factoids}</div>
    </div>
    <div class="screenlet-body">
        <#assign count_1=0/>
        <@limitedSubContent subContentId=factoidRootId viewIndex=0 viewSize=9999 orderBy="contentName" contentAssocTypeId="SUBSITE" limitSize="2">
            <div class="browsecategorytext" style="margin-left: 10px">
              -&nbsp;<@renderSubContentCache subContentId=subContentId/>
            </div>
            <#assign count_1=(count_1 + 1)/>
        </@limitedSubContent>
    </div>
</div>
