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
 *@author     Al Byers (byersa@automationgroups.com)
 *@version    $Rev$
 *@since      3.0
-->


${pages.get(page.getProperty("subMenu"))}

<#if hasPermission?exists && hasPermission>
    <div class="head1">${page.getProperty("entityName")}</div>
    <hr align="left" width="25%"/>
    <#if queryWrapper?exists >
    <div class="head1">Query</div>
    ${queryWrapper.renderFormString()}
    <br/>
    </#if>

    <#if mruWrapper?exists >
    <div class="head1">Select</div>
    ${mruWrapper.renderFormString()}
    <br/>
    </#if>

    <#if editWrapper?exists >
    <div class="head1">Edit</div>
    ${editWrapper.renderFormString()}
    <br/>
    </#if>

    <#if listWrapper?exists >
    ${listWrapper.renderFormString()}
    <br/>
    </#if>

    <#if addWrapper?exists >
    <div class="head1">${singleTitle?default("Create New")}</div>
    ${addWrapper.renderFormString()}
    <br/>
    </#if>

    <#if formWrapper?exists >
    ${formWrapper.renderFormString()}
    <br/>
    </#if>


    <#if uploadWrapper?exists >
    ${uploadWrapper.renderFormString()}
    <br/>
    </#if>

<#else>
 <h3>You do not have permission to view this page. ("CONTENTMGR_VIEW" or "CONTENTMGR_ADMIN" needed)</h3>
</#if>
