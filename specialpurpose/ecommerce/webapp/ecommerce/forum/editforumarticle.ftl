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
    <td>
      <h1>${contentIdTo!}
      </h1>
    </td>
    <td align="right">
    </td>
  </tr>
</table>
<br />
-->

<div class="screenlet">
    
        <div class="boxlink">
            <a href="<@ofbizUrl>editforuminfo?contentId=${contentId!}&amp;nodeTrailCsv=${nodeTrailCsv!}&amp;contentIdTo=${contentIdTo!}</@ofbizUrl>" class="submenutextright">${uiLabelMap.CommonUpdate}</a>
        </div>
        <h3>${uiLabelMap.EcommerceContentInformation}</h3>
   
    <div class="screenlet-body">
  <table>
    <tr><td align="right" nowrap="nowrap"><div class='tabletext'><b>${uiLabelMap.ProductContentId}</b></div></td><td>&nbsp;</td><td><div class='tabletext'>${contentId!}</div></td></tr>
    <tr><td align="right" nowrap="nowrap"><div class='tabletext'><b>${uiLabelMap.EcommerceContentName}</b></div></td><td>&nbsp;</td><td><div class='tabletext'>${contentName!}</div></td></tr>
    <tr><td align="right" nowrap="nowrap"><div class='tabletext'><b>${uiLabelMap.CommonDescription}</b></div></td><td>&nbsp;</td><td><div class='tabletext'>${description!}<div></td></tr>
  </table>
    </div>
</div>

<div class="screenlet">
    
        <div class="boxlink">
            <a href="<@ofbizUrl>editaddimage?contentIdTo=${contentId!}&amp;dataResourceId=${txtDataResourceId!}&amp;mapKey=IMAGE&amp;nodeTrailCsv=${nodeTrailCsv!}</@ofbizUrl>" class="submenutextright">${uiLabelMap.CommonUpdate}</a>
        </div>
        <h3>${uiLabelMap.EcommerceImageInformation}</h3>
    
    <div class="screenlet-body">
  <table>
    <tr><td align="right" nowrap="nowrap"><div class='tabletext'>${uiLabelMap.EcommerceImage}</div></td><td>&nbsp;</td><td><div class='tabletext'>
        <img src="<@ofbizUrl>img?imgId=${imgDataResourceId!}</@ofbizUrl>" alt="" class='cssImgStandard' />
<div></td></tr>
  </table>
    </div>
</div>

<div class="screenlet">
        <div class="boxlink">
            <a href="<@ofbizUrl>editaddforumdata?contentIdTo=${contentId!}&amp;dataResourceId=${txtDataResourceId!}&amp;mapKey=SUMMARY&amp;nodeTrailCsv=${nodeTrailCsv!}</@ofbizUrl>" class="submenutextright">${uiLabelMap.CommonUpdate}</a>
        </div>
        <h3>${uiLabelMap.EcommerceSummaryInformation}</h3>
    <div class="screenlet-body">
  <table>
    <tr><td><div class='tabletext'>${uiLabelMap.ContentSummary}</div></td><td>&nbsp;</td><td><div class='tabletext'>
${summaryData!}
<div></td></tr>
  </table>
    </div>
</div>

<div class="screenlet">
        <div class="boxlink">
            <a href="<@ofbizUrl>editaddforumdata?contentIdTo=${contentId!}&amp;dataResourceId=${txtDataResourceId!}&amp;mapKey=ARTICLE&amp;nodeTrailCsv=${nodeTrailCsv!}</@ofbizUrl>" class="submenutextright">${uiLabelMap.CommonUpdate}</a>
        </div>
        <h3>&nbsp;${uiLabelMap.EcommerceArticleInformation}</h3>
    <div class="screenlet-body">
  <table width="100%" border="0" cellpadding="0" cellspacing='0'>
    <tr><td align="right" nowrap="nowrap"><div class='tabletext'><b>Article</b></div></td><td>&nbsp;</td><td><div class='tabletext'>
${textData!}
<div></td></tr>
  </table>
    </div>
</div>
