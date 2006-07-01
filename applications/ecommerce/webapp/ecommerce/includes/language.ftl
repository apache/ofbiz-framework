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
 *@author     Andy Zeneski (jaz@ofbiz.org)
 *@version    $Rev$
 *@since      3.0
-->

<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxhead">${uiLabelMap.CommonLanguageTitle}</div>
    </div>
    <div class="screenlet-body" style="text-align: center;">
        <form method="post" name="chooseLanguage" action="<@ofbizUrl>setSessionLocale</@ofbizUrl>" style="margin: 0;">
          <select name="locale" class="selectBox">
            <#assign initialDisplayName = locale.getDisplayName(locale)>
            <#if 18 < initialDisplayName?length>
              <#assign initialDisplayName = initialDisplayName[0..15] + "...">
            </#if>
            <option value="${locale.toString()}">${initialDisplayName}</option>
            <option value="${locale.toString()}">----</option>
            <#list availableLocales as availableLocale>
              <#assign displayName = availableLocale.getDisplayName(locale)>
              <#if 18 < displayName?length>
                <#assign displayName = displayName[0..15] + "...">
              </#if>
              <option value="${availableLocale.toString()}">${displayName}</option>
            </#list>
          </select>
          <div><a href="javascript:document.chooseLanguage.submit()" class="buttontext">${uiLabelMap.CommonChange}</a></div>
        </form>
    </div>
</div>
