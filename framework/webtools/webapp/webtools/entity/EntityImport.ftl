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
 * @author David E. Jones (jonesde@ofbiz.org)
 * @author Jacopo Cappellato (tiz@sastau.it)
-->

<div class="head1">${uiLabelMap.WebtoolsImportToDataSource}</div>
<div>${uiLabelMap.WebtoolsMessage5}.</div>
<hr>
  <div class="head2">${uiLabelMap.WebtoolsImport}:</div>

  <form method="post" action="<@ofbizUrl>entityImport</@ofbizUrl>">
    <div class="tabletext">${uiLabelMap.WebtoolsMessage6}:</div>
    <div><input type="text" class="inputBox" size="60" name="fmfilename" value="${fmfilename?if_exists}"/></div>
    <div class="tabletext">${uiLabelMap.WebtoolsMessage7}:</div>
    <div><input type="text" class="inputBox" size="60" name="filename" value="${filename?if_exists}"/></div>
    <div class="tabletext"><input type="checkbox" name="isUrl" <#if isUrl?exists>"checked"</#if>/>${uiLabelMap.WebtoolsIsURL}</div>
    <div class="tabletext"><input type="checkbox" name="mostlyInserts" <#if mostlyInserts?exists>"checked"</#if>/>${uiLabelMap.WebtoolsMostlyInserts}</div>
    <div class="tabletext"><input type="checkbox" name="maintainTimeStamps" <#if keepStamps?exists>"checked"</#if>/>${uiLabelMap.WebtoolsMaintainTimestamps}</div>
    <div class="tabletext"><input type="checkbox" name="createDummyFks" <#if createDummyFks?exists>"checked"</#if>/>${uiLabelMap.WebtoolsCreateDummyFks}</div>
    <div class="tabletext">${uiLabelMap.WebtoolsTimeoutSeconds}:<input type="text" size="6" value="${txTimeoutStr?default("7200")}" name="txTimeout"/></div>
    <div><input type="submit" value="${uiLabelMap.WebtoolsImportFile}"/></div>
  </form>
  <form method="post" action="<@ofbizUrl>entityImport</@ofbizUrl>">
    <div class="tabletext">${uiLabelMap.WebtoolsMessage4}:</div>
    <textarea class="textAreaBox" rows="20" cols="85" name="fulltext">${fulltext?if_exists}</textarea>
    <br/><input type="submit" value="${uiLabelMap.WebtoolsImportText}"/>
  </form>
  <hr>
  <#if messages?exists>
      <h3>${uiLabelMap.WebtoolsResults}:</h3>
      <#list messages as message>
          <div class="tabletext">${message}</div>
      </#list>
  </#if>
