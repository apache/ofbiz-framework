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
<#import "bloglib.ftl" as blog/>
<@blog.renderAncestryPath trail=ancestorList?default([]) endIndexOffset=1/>
<#-- Main Heading -->
<#--
<table width='100%' cellpadding='0' cellspacing='0' border='0'>
  <tr>
    <td align="left">
      <div class="head1">${contentIdTo?if_exists}
      </div>
    </td>
    <td align="right">
    </td>
  </tr>
</table>
<br/>
-->

<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxlink">
            <a href="<@ofbizUrl>editforuminfo?contentId=${contentId?if_exists}&nodeTrailCsv=${nodeTrailCsv?if_exists}&contentIdTo=${contentIdTo?if_exists}</@ofbizUrl>" class="submenutextright">${uiLabelMap.CommonUpdate}</a>
        </div>
        <div class="boxhead">&nbsp;${uiLabelMap.EcommerceContentInformation}</div>
    </div>
    <div class="screenlet-body">
  <table width="100%" border="0" cellpadding="0" cellspacing='0'>
    <tr><td align="right" nowrap><div class='tabletext'><b>${uiLabelMap.ProductContentId}</b></div></td><td>&nbsp;</td><td align="left"><div class='tabletext'>${contentId?if_exists}</div></td></tr>
    <tr><td align="right" nowrap><div class='tabletext'><b>${uiLabelMap.EcommerceContentName}</b></div></td><td>&nbsp;</td><td align="left"><div class='tabletext'>${contentName?if_exists}</div></td></tr>
    <tr><td align="right" nowrap><div class='tabletext'><b>${uiLabelMap.CommonDescription}</b></div></td><td>&nbsp;</td><td align="left"><div class='tabletext'>${description?if_exists}<div></td></tr>
  </table>
    </div>
</div>

<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxlink">
            <a href="<@ofbizUrl>editaddimage?contentIdTo=${contentId?if_exists}&dataResourceId=${txtDataResourceId?if_exists}&mapKey=IMAGE&nodeTrailCsv=${nodeTrailCsv?if_exists}</@ofbizUrl>" class="submenutextright">${uiLabelMap.CommonUpdate}</a>
        </div>
        <div class="boxhead">&nbsp;${uiLabelMap.EcommerceImageInformation}</div>
    </div>
    <div class="screenlet-body">
  <table width="100%" border="0" cellpadding="0" cellspacing='0'>
    <tr><td align="right" nowrap><div class='tabletext'><b>${uiLabelMap.EcommerceImage}</b></div></td><td>&nbsp;</td><td align="left"><div class='tabletext'>
        <img src="<@ofbizUrl>img?imgId=${imgDataResourceId?if_exists}</@ofbizUrl>" />
<div></td></tr>
  </table>
    </div>
</div>

<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxlink">
            <a href="<@ofbizUrl>editaddforumdata?contentIdTo=${contentId?if_exists}&dataResourceId=${txtDataResourceId?if_exists}&mapKey=SUMMARY&nodeTrailCsv=${nodeTrailCsv?if_exists}</@ofbizUrl>" class="submenutextright">${uiLabelMap.CommonUpdate}</a>
        </div>
        <div class="boxhead">&nbsp;${uiLabelMap.EcommerceSummaryInformation}</div>
    </div>
    <div class="screenlet-body">
  <table width="100%" border="0" cellpadding="0" cellspacing='0'>
    <tr><td align="right" nowrap><div class='tabletext'><b>${uiLabelMap.EcommerceSummary}</b></div></td><td>&nbsp;</td><td align="left"><div class='tabletext'>
${summaryData?if_exists}
<div></td></tr>
  </table>
    </div>
</div>

<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxlink">
            <a href="<@ofbizUrl>editaddforumdata?contentIdTo=${contentId?if_exists}&dataResourceId=${txtDataResourceId?if_exists}&mapKey=ARTICLE&nodeTrailCsv=${nodeTrailCsv?if_exists}</@ofbizUrl>" class="submenutextright">${uiLabelMap.CommonUpdate}</a>
        </div>
        <div class="boxhead">&nbsp;${uiLabelMap.EcommerceArticleInformation}</div>
    </div>
    <div class="screenlet-body">
  <table width="100%" border="0" cellpadding="0" cellspacing='0'>
    <tr><td align="right" nowrap><div class='tabletext'><b>Article</b></div></td><td>&nbsp;</td><td align="left"><div class='tabletext'>
${textData?if_exists}
<div></td></tr>
  </table>
    </div>
</div>
