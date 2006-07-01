<#--
 *  Copyright (c) 2004-2005 The Open For Business Project - www.ofbiz.org
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
 *@since      2.1
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
        <div style="float: right;">
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
        <div style="float: right;">
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
        <div style="float: right;">
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
        <div style="float: right;">
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
