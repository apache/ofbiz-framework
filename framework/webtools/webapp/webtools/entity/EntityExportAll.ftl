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
 * @author Brian Johnson (bmj@camfour.com)
 * @author David E. Jones (jonesde@ofbiz.org)
 * @author Jacopo Cappellato (tiz@sastau.it)
-->

<div class="head1">${uiLabelMap.WebtoolsExportFromDataSource}</div>
<div class="tabletext">
    ${uiLabelMap.WebtoolsMessage1}. 
    ${uiLabelMap.WebtoolsMessage2}.
    ${uiLabelMap.WebtoolsMessage3}.
</div>
<hr>
    
<div class="head2">${uiLabelMap.WebtoolsResults}:</div>

<#if results?has_content>
    <#list results as result>
        <div class="tabletext">${result}</div>
    </#list>
</#if>

<hr>

<div class="head2">${uiLabelMap.WebtoolsExport}:</div>
<form method="post" action="<@ofbizUrl>entityExportAll</@ofbizUrl>">
    <div class="tabletext">${uiLabelMap.WebtoolsOutputDirectory}: <input type="text" class="inputBox" size="60" name="outpath" value="${outpath?if_exists}"></div>
    <div class="tabletext">${uiLabelMap.WebtoolsTimeoutSeconds}: <input type="text" size="6" value="${txTimeout?default('7200')}" name="txTimeout"/></div>
    <br/>
    <input type="submit" value="${uiLabelMap.WebtoolsExport}">
</form>
